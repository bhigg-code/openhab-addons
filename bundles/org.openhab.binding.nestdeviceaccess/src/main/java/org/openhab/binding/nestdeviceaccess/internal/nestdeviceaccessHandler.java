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
package org.openhab.binding.nestdeviceaccess.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.nestdeviceaccess.internal.nesthelper.NestUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link nestdeviceaccessHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Brian Higginbotham - Initial contribution
 */
@NonNullByDefault
public class nestdeviceaccessHandler extends BaseThingHandler {

    NestUtility nestUtility = new NestUtility(thing);
    private final Logger logger = LoggerFactory.getLogger(nestdeviceaccessHandler.class);

    private @Nullable nestdeviceaccessConfiguration config;

    public nestdeviceaccessHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (nestdeviceaccessBindingConstants.thermostatName.equals(channelUID.getId())) {
            if (command instanceof RefreshType) {
                // TODO: handle data refresh
            }

            // TODO: handle command

            // Note: if communication with thing fails for some reason,
            // indicate that by setting the status with detail information:
            // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
            // "Could not control device at IP address x.x.x.x");
        }
    }

    @Override
    public void initialize() {
        logger.debug("Start initializing!");
        config = getConfigAs(nestdeviceaccessConfiguration.class);
        config.projectId = thing.getConfiguration().get("projectId").toString();
        config.clientId = thing.getConfiguration().get("clientId").toString();
        config.clientSecret = thing.getConfiguration().get("clientSecret").toString();
        config.authorizationToken = thing.getConfiguration().get("authorizationToken").toString();
        config.accessToken = thing.getConfiguration().get("accessToken").toString();
        config.refreshToken = thing.getConfiguration().get("refreshToken").toString();
        config.accessTokenExpiresIn = thing.getConfiguration().get("accessTokenExpiresIn").toString();
        // verify accesstoken with simple get request
        /*
         * try {
         * if (nestUtility.getStructures(config.projectId, config.accessToken) == 401) {
         * // get access token refreshed
         * logger.debug("initialize() reporting access token is expired..");
         * if (nestUtility.refreshAccessToken(config.refreshToken, config.clientId, config.clientSecret)) {
         * logger.debug("initialize() reporting access token refresh successful..");
         * if (nestUtility.getStructures(config.projectId, config.accessToken) == 200) {
         * config.accessToken = nestUtility.accessToken;
         * config.accessTokenExpiresIn = nestUtility.accessTokenExpiresIn;
         * logger.debug("initialize() reporting call to getStructures successful");
         * }
         * }
         * }
         *
         * } catch (IOException e) {
         * logger.debug("initialize() {}", e.getMessage());
         * }
         *
         * if (thing.getConfiguration().get("accessToken") != null) {
         * config.accessToken = thing.getConfiguration().get("accessToken").toString();
         * }
         * if (thing.getConfiguration().get("refreshToken") != null) {
         * config.refreshToken = thing.getConfiguration().get("refreshToken").toString();
         * }
         */
        // TODO: Initialize the handler.
        // The framework requires you to return from this method quickly. Also, before leaving this method a thing
        // status from one of ONLINE, OFFLINE or UNKNOWN must be set. This might already be the real thing status in
        // case you can decide it directly.
        // In case you can not decide the thing status directly (e.g. for long running connection handshake using WAN
        // access or similar) you should set status UNKNOWN here and then decide the real status asynchronously in the
        // background.

        // set the thing status to UNKNOWN temporarily and let the background task decide for the real status.
        // the framework is then able to reuse the resources from the thing handler initialization.
        // we set this upfront to reliably check status updates in unit tests.

        updateStatus(ThingStatus.UNKNOWN);

        // Example for background initialization:
        scheduler.execute(() -> {
            boolean thingReachable = true; // <background task with long running initialization here>
            /*
             * try {
             * logger.debug("initialize executing...");
             * if ((config.accessToken == null) && (config.refreshToken == null)) {
             * logger.debug("initialize getting access token...");
             * nestUtility.requestAccessToken(config.clientId, config.clientSecret, config.authorizationToken);
             * } else {
             * logger.info("Initialize reporting a preset access {} and refresh {} token..", config.accessToken,
             * config.refreshToken);
             * thing.getConfiguration().put("accessToken", config.accessToken);
             * thing.getConfiguration().put("refreshToken", config.refreshToken);
             * }
             *
             * } catch (IOException e) {
             * logger.debug("Initialize() reporting {}", e.getMessage());
             * }
             */
            // when done do:
            if (thingReachable) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE);
            }

        });

        // logger.debug("Finished initializing!");

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
    }
}
