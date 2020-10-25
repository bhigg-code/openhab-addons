/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.nestdeviceaccess.internal.discovery;

import static org.openhab.binding.nestdeviceaccess.internal.nestdeviceaccessBindingConstants.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openhab.binding.nestdeviceaccess.internal.nesthelper.NestUtility;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link nestdeviceaccessDiscovery} is discovering devices from the SDM API
 *
 * @author Brian Higginbotham - Initial contribution
 */

@Component(service = DiscoveryService.class, immediate = true, configurationPid = "org.openhab.nestdeviceaccess")
public class nestdeviceaccessDiscovery extends AbstractDiscoveryService {

    NestUtility nestUtility;
    private final Logger logger = LoggerFactory.getLogger(nestdeviceaccessDiscovery.class);
    private ExecutorService executorService;
    private boolean scanning;
    private static final int TIMEOUT = 1000;

    private String projectId;
    private String clientSecret;
    private String clientId;
    private String refreshToken;
    private String authorizationToken;
    private String accessToken;
    private boolean initialize;

    @Activate
    protected void activate(ComponentContext context) {
        Dictionary<String, Object> properties = context.getProperties();
        projectId = properties.get("projectId").toString();
        clientSecret = properties.get("clientSecret").toString();
        clientId = properties.get("clientId").toString();
        refreshToken = properties.get("refreshToken").toString();
        authorizationToken = properties.get("authorizationToken").toString();

        if (((projectId == null) || (clientId == null) || (clientSecret == null) || (refreshToken == null))
                && (authorizationToken == null)) {
            logger.debug(
                    "activate in the Discovery service does NOT enough data to initiliaze.. projectId {}\nclientId {}\nclientSecret{}\nrefreshToken {}\nauthorizationToken {}",
                    projectId, clientId, clientSecret, refreshToken, authorizationToken);
            initialize = false;
        } else {
            initialize = true;
        }
    }

    private void addThing(String devicesType[], String devicesId[], String devicesName[], String devicesStatus[]) {

        ThingTypeUID typeId = null;

        // Let's check type
        for (int i = 0; i < devicesType.length; i++) {
            if (devicesStatus[i].equalsIgnoreCase("online")) {
                switch (devicesType[i]) {
                    case "sdm.devices.types.THERMOSTAT":
                        typeId = THING_TYPE_THERMOSTAT;
                        ThingUID deviceThing = new ThingUID(typeId, devicesId[i]);
                        Map<String, Object> properties = new HashMap<>(8);
                        properties.put("deviceId", devicesId[i]);
                        properties.put("deviceName", devicesName[i]);
                        properties.put("refreshToken", refreshToken);
                        properties.put("clientId", clientId);
                        properties.put("clientSecret", clientSecret);
                        properties.put("accessToken", accessToken);
                        properties.put("projectId", projectId);
                        DiscoveryResult result = DiscoveryResultBuilder.create(deviceThing).withProperties(properties)
                                .withLabel("Nest " + devicesName[i] + " Thermostat").build();
                        thingDiscovered(result);
                        logger.info("nestdeviceaccessDiscovery adding Thermostat: [{}] to inbox", devicesName[i]);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * Starts the scan. This discovery will:
     *
     */
    @Override
    protected void startScan() {

        if (executorService != null) {
            stopScan();
        }
        scanning = true;
        logger.debug("Starting Discovery NestDeviceAccess");

        try {

            nestUtility = new NestUtility(projectId, clientId, clientSecret, refreshToken, accessToken);
            if ((authorizationToken != null) && (refreshToken == null)) {
                // initial authorization request. We need to get a refresh token and then
                logger.debug("Initial Access Token being retrieved...");
                String[] tokens = new String[2];
                tokens = nestUtility.requestAccessToken(clientId, clientSecret, authorizationToken);
                tokens[0] = accessToken;
                tokens[1] = refreshToken;
            }

            // accessToken is typically stale.. Getting fresh on initialization
            // TODO: Check access token for refresh
            accessToken = nestUtility.refreshAccessToken(refreshToken, clientId, clientSecret);
            String url = "https://smartdevicemanagement.googleapis.com/v1/enterprises/" + projectId + "/devices";
            // get devices for discovery
            String jsonContent = nestUtility.getDeviceInfo(accessToken, url);

            JSONObject jo = new JSONObject(jsonContent);
            JSONArray ja = jo.getJSONArray("devices");

            String[][] devicesParentRelations = new String[ja.length()][2];
            String[] devicesType = new String[ja.length()];
            String[] devicesName = new String[ja.length()];
            String[] devicesId = new String[ja.length()];
            String[] devicesStatus = new String[ja.length()];

            for (int i = 0; i < ja.length(); i++) {
                devicesName[i] = ja.getJSONObject(i).getString("name");
                devicesId[i] = devicesName[i].substring(devicesName[i].lastIndexOf("/") + 1, devicesName[i].length());
                devicesType[i] = ja.getJSONObject(i).getString("type");
                devicesStatus[i] = ja.getJSONObject(i).getJSONObject("traits")
                        .getJSONObject("sdm.devices.traits.Connectivity").getString("status");
                JSONArray jaParentRelations = ja.getJSONObject(i).getJSONArray("parentRelations");

                for (int nCount = 0; nCount < jaParentRelations.length(); nCount++) {
                    // get Available Modes
                    devicesParentRelations[i][0] = jaParentRelations.getJSONObject(nCount).getString("parent");
                    devicesParentRelations[i][1] = jaParentRelations.getJSONObject(nCount).getString("displayName");
                    break;
                }
                devicesName[i] = devicesParentRelations[i][1]; // DisplayName
            }

            addThing(devicesType, devicesId, devicesName, devicesStatus);

        } catch (IOException e) {
            logger.debug("discovery reporting exception {}", e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     *
     * Stops the discovery scan. We set {@link #scanning} to false (allowing the listening threads to end naturally
     * within {@link #TIMEOUT) * 5 time then shutdown the {@link #executorService}
     */
    @Override
    protected synchronized void stopScan() {
        super.stopScan();
        /*
         * if (executorService == null) {
         * return;
         * }
         *
         * scanning = false;
         *
         * try {
         * executorService.awaitTermination(TIMEOUT * 5, TimeUnit.MILLISECONDS);
         * } catch (InterruptedException e) {
         * }
         * executorService.shutdown();
         * executorService = null;
         */
    }

    /**
     * Constructs the discovery class using the thing IDs that we can discover.
     */
    public nestdeviceaccessDiscovery() {
        super(Collections.unmodifiableSet(
                Stream.of(THING_TYPE_GENERIC, THING_TYPE_THERMOSTAT).collect(Collectors.toSet())), 30, false);
        logger.debug("nestdeviceaccessDiscovery constructor..");
    }
}
