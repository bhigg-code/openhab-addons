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

package org.openhab.binding.nestdeviceaccess.internal.thermostat;

import static org.openhab.binding.nestdeviceaccess.internal.nestdeviceaccessBindingConstants.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openhab.binding.nestdeviceaccess.internal.nestdeviceaccessConfiguration;
import org.openhab.binding.nestdeviceaccess.internal.nesthelper.NestUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.core.InstantiatingExecutorProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.common.collect.Lists;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;

/**
 * The {@link NestThermostatHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Brian Higginbotham - Initial contribution
 */
@NonNullByDefault
public class NestThermostatHandler extends BaseThingHandler {

    // nestUtility class
    NestUtility nestUtility = new NestUtility(thing);
    private final Logger logger = LoggerFactory.getLogger(NestThermostatHandler.class);

    private @NonNullByDefault({}) ScheduledFuture<?> refreshJob;
    private @Nullable Future<?> future;
    private @Nullable nestdeviceaccessConfiguration config;
    NestThermostat nestThermostat;

    public NestThermostatHandler(Thing thing) {
        super(thing);
        nestThermostat = new NestThermostat(thing); // initialize thermostat with base properties
    }

    @Override
    public void dispose() {
        if (refreshJob != null) {
            refreshJob.cancel(true);
        }
        if (future != null) {
            future.cancel(true);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // logger.debug("handleCommand reporting {},{}", channelUID.getId(), command.toString());

        try {

            if (thermostatName.equals(channelUID.getId())) {

                if (command instanceof RefreshType) {
                    // logger.debug("handleCommand reporting {}", command.toString());
                }

                // TODO: handle command

                // Note: if communication with thing fails for some reason,
                // indicate that by setting the status with detail information:
                // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                // "Could not control device at IP address x.x.x.x");
            } else if (thermostatCurrentMode.equals(channelUID.getId())) {
                if (command.toString() != "REFRESH") {
                    if (nestThermostat.setThermostatMode(command.toString())) {
                        logger.info("Thermostat: {} set command {} to {}", thing.getProperties().get("deviceName"),
                                channelUID.toString(), command.toString());
                        Thread.sleep(3000); // set up to handle delay with changing mode and current settings being
                                            // updated
                        refreshChannels();
                    }
                }
            } else if (thermostatCurrentEcoMode.equals(channelUID.getId())) {
                if (command.toString() != "REFRESH") {
                    if (nestThermostat.setThermostatEcoMode(command.toString())) {
                        logger.info("Thermostat: {} set command {} to {}", thing.getProperties().get("deviceName"),
                                channelUID.toString(), command.toString());
                        Thread.sleep(3000); // set up to handle delay with changing mode and current settings being
                                            // updated
                        refreshChannels();
                    }
                }
            } else if (thermostatTargetTemperature.equals(channelUID.getId())) {
                if (command.toString() != "REFRESH") {
                    if (nestThermostat.setThermostatTargetTemperature(Double.parseDouble(command.toString()), 0, 0,
                            false)) {
                        logger.info("Thermostat: {} set command {} to {}", thing.getProperties().get("deviceName"),
                                channelUID.toString(), command.toString());
                        refreshChannels();
                    }
                }
            } else if (thermostatMinimumTemperature.equals(channelUID.getId())) {
                if (command.toString() != "REFRESH") {
                    logger.debug("thermostatMinTemp {}", nestThermostat.getMinMaxTemperature()[1]);
                    if (nestThermostat.setThermostatTargetTemperature(0, Double.parseDouble(command.toString()),
                            nestThermostat.getMinMaxTemperature()[1], true)) {
                        logger.info("Thermostat: {} set command {} to {}", thing.getProperties().get("deviceName"),
                                channelUID.toString(), command.toString());
                        refreshChannels();
                    }
                }
            } else if (thermostatMaximumTemperature.equals(channelUID.getId())) {
                if (command.toString() != "REFRESH") {
                    if (nestThermostat.setThermostatTargetTemperature(0, nestThermostat.getMinMaxTemperature()[0],
                            Double.parseDouble(command.toString()), true)) {
                        logger.info("Thermostat: {} set command {} to {}", thing.getProperties().get("deviceName"),
                                channelUID.toString(), command.toString());
                        refreshChannels();
                    }
                }
            }
        } catch (IOException e) {
            logger.debug("handleMessage reporting exception {}", e.getMessage());
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            logger.debug("handleMessage reporting exception {}", e.getMessage());
        }
    }

    void refreshChannels() {
        logger.info("refreshChannels process a timer for Updating thing Channels for:{}", thing.getUID());

        try {
            // refresh status of the device
            if (nestThermostat.getThermostatInfo()) {
                State statusState = new StringType(nestThermostat.getDeviceStatus());
                updateState("thermostatStatus", statusState);
                State nameState = new StringType(nestThermostat.getDeviceName());
                updateState("thermostatName", nameState);
                State nameHVACState = new StringType(nestThermostat.getThermostatHVACStatus());
                updateState(thermostatHVACStatus, nameHVACState);
                State humidityState = new DecimalType(nestThermostat.getCurrentHumidity());
                updateState("thermostatHumidityPercent", humidityState);
                State ambientState = new DecimalType(nestThermostat.getAmbientTemperatureSetting());
                updateState("thermostatAmbientTemperature", ambientState);
                State setTempHeatState = new DecimalType(nestThermostat.getCurrentTemperatureHeat());
                updateState("thermostatTemperatureHeat", setTempHeatState);
                State setTempCoolState = new DecimalType(nestThermostat.getCurrentTemperatureCool());
                updateState("thermostatTemperatureCool", setTempCoolState);
                State setTargetTempState = new DecimalType(nestThermostat.getTargetTemperature());
                updateState("thermostatTargetTemperature", setTargetTempState);
                State setMinTempState = new DecimalType(nestThermostat.getMinMaxTemperature()[0]);
                updateState("thermostatMinimumTemperature", setMinTempState);
                State setMaxTempState = new DecimalType(nestThermostat.getMinMaxTemperature()[1]);
                updateState("thermostatMaximumTemperature", setMaxTempState);
                State modeState = new StringType(nestThermostat.getThermostatMode());
                updateState("thermostatCurrentMode", modeState);
                State ecoModeState = new StringType(nestThermostat.getThermostatEcoMode());
                updateState("thermostatCurrentEcoMode", ecoModeState);
                State scaleSettingState = new StringType(nestThermostat.getTemperatureScaleSetting());
                updateState("thermostatScaleSetting", scaleSettingState);

            }
        } catch (IOException e) {
            logger.debug("refreshChannels() reporting exception {}", e.getMessage());
        }
    }

    @Override
    public void initialize() {
        config = getConfigAs(nestdeviceaccessConfiguration.class);
        config.projectId = thing.getProperties().get("projectId");
        config.clientId = thing.getProperties().get("clientId");
        config.clientSecret = thing.getProperties().get("clientSecret");
        config.accessToken = thing.getProperties().get("accessToken");
        config.refreshToken = thing.getProperties().get("refreshToken");
        config.deviceId = thing.getProperties().get("deviceId");
        config.deviceName = thing.getProperties().get("deviceName");

        if (thing.getConfiguration().containsKey("refreshInterval")) {
            config.refreshInterval = Integer.parseInt(thing.getConfiguration().get("refreshInterval").toString());
        } else {
            config.refreshInterval = 300; // default setting
        }

        updateStatus(ThingStatus.UNKNOWN);

        // Example for background initialization:
        scheduler.execute(() -> {
            boolean thingReachable = true; // <background task with long running initialization here>
            try {
                logger.debug("Start initializing device [{}]", config.deviceName);

                if ((nestUtility.getPubSubProjectId() != null) && (nestUtility.getSubscriptionId() != null)) {
                    logger.debug("starting pubsub thermostat [{}]...", config.deviceName);

                    future = scheduler.submit(() -> {
                        pubSubEventHandler(nestUtility.getPubSubProjectId(), nestUtility.getSubscriptionId());
                    });
                }
                nestThermostat.initializeThermostat();

                thingReachable = nestThermostat.getDeviceStatus().equalsIgnoreCase("ONLINE");
                if (thingReachable) {
                    updateStatus(ThingStatus.ONLINE);
                } else {
                    updateStatus(ThingStatus.OFFLINE);
                }
                logger.debug("Finished initializing device [{}]", config.deviceName);
            } catch (Exception e) {
                logger.debug("Initialize() caught an exception {}", e.getMessage());
            }
        });

        if (refreshJob == null || refreshJob.isCancelled()) {
            refreshJob = scheduler.scheduleWithFixedDelay(this::refreshChannels, 0, config.refreshInterval,
                    TimeUnit.SECONDS);
        }
        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");

    }

    public boolean dispatchMessage(PubsubMessage message) throws IOException {

        String messageEventId;
        String userId;
        String resourceName;
        String eventTrait;
        String eventSessionId;
        String eventId;
        Date messageTime;

        try {
            // Let's find out what type of message we are dealing with by parsing important artifacts

            JSONObject jo = new JSONObject(message.getData().toStringUtf8());

            messageEventId = jo.getString("eventId").toString();
            userId = jo.getString("userId");
            resourceName = jo.getJSONObject("resourceUpdate").getString("name");
            logger.debug("dispatchMessage processing\ndeviceId [{}]\nname [{}]\nmessageId [{}]",
                    thing.getProperties().get("deviceId"),
                    resourceName.substring(resourceName.lastIndexOf("/") + 1, resourceName.length()),
                    message.getMessageId());

            if (resourceName.contains(thing.getProperties().get("deviceId"))) {
                logger.debug("dispatchMessage found device match for message [{}]",
                        thing.getProperties().get("deviceName"));
                if (jo.getJSONObject("resourceUpdate").has("traits")) {

                    if (jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                            .has("sdm.devices.traits.Temperature")) {

                        if (jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                .getJSONObject("sdm.devices.traits.Temperature").has("ambientTemperatureCelsius")) {

                            State ambientState = new DecimalType(
                                    nestThermostat.setAmbientTemperatureSetting(jo.getJSONObject("resourceUpdate")
                                            .getJSONObject("traits").getJSONObject("sdm.devices.traits.Temperature")
                                            .getFloat("ambientTemperatureCelsius")));

                            updateState(thermostatAmbientTemperature, ambientState);
                        }
                    }

                    if (jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                            .has("sdm.devices.traits.ThermostatTemperatureSetpoint")) {

                        if ((jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                .getJSONObject("sdm.devices.traits.ThermostatTemperatureSetpoint").has("heatCelsius"))
                                && (jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                        .getJSONObject("sdm.devices.traits.ThermostatTemperatureSetpoint")
                                        .has("coolCelsius"))
                                && (jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                        .has("sdm.devices.traits.ThermostatMode"))) {
                            // heatCelsius and coolCelsius only exist in two modes,ThermostatEco and HeatCool.
                            // Check for heatCool since Eco mode is under another trait
                            if (jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                    .getJSONObject("sdm.devices.traits.ThermostatMode").getString("mode")
                                    .equalsIgnoreCase("heatcool")) {
                                // found heatCool. Let's extract data and store
                                double[] thermostatMinMax = new double[2];

                                thermostatMinMax[0] = jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                        .getJSONObject("sdm.devices.traits.ThermostatTemperatureSetpoint")
                                        .getFloat("heatCelsius");
                                thermostatMinMax[1] = jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                        .getJSONObject("sdm.devices.traits.ThermostatTemperatureSetpoint")
                                        .getFloat("coolCelsius");

                                nestThermostat.setMinMaxTemperatureValue(thermostatMinMax);
                                State setMinTempState = new DecimalType(nestThermostat.getMinMaxTemperature()[0]);
                                updateState(thermostatMinimumTemperature, setMinTempState);

                                State setMaxTempState = new DecimalType(nestThermostat.getMinMaxTemperature()[1]);
                                updateState(thermostatMaximumTemperature, setMaxTempState);
                            }
                        } else {
                            if ((jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                    .getJSONObject("sdm.devices.traits.ThermostatTemperatureSetpoint")
                                    .has("heatCelsius"))
                                    && (jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                            .getJSONObject("sdm.devices.traits.ThermostatTemperatureSetpoint")
                                            .has("coolCelsius"))
                                    && ((jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                            .has("sdm.devices.traits.ThermostatMode")) == false)) {

                                logger.debug(
                                        "dispatchMessage found a thermostat value without a mode.. We'll set the minmaxvalue");
                                // found heatCool. Let's extract data and store
                                double[] thermostatMinMax = new double[2];

                                thermostatMinMax[0] = jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                        .getJSONObject("sdm.devices.traits.ThermostatTemperatureSetpoint")
                                        .getFloat("heatCelsius");
                                thermostatMinMax[1] = jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                        .getJSONObject("sdm.devices.traits.ThermostatTemperatureSetpoint")
                                        .getFloat("coolCelsius");

                                nestThermostat.setMinMaxTemperatureValue(thermostatMinMax);

                                State setMinTempState = new DecimalType(nestThermostat.getMinMaxTemperature()[0]);
                                updateState(thermostatMinimumTemperature, setMinTempState);

                                State setMaxTempState = new DecimalType(nestThermostat.getMinMaxTemperature()[1]);
                                updateState(thermostatMaximumTemperature, setMaxTempState);
                            }
                        }
                    }

                    if (jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                            .has("sdm.devices.traits.ThermostatMode")) {
                        if (jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                .getJSONObject("sdm.devices.traits.ThermostatMode").has("mode")) {

                            State modeState = new StringType(nestThermostat
                                    .setThermostatModeValue(jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                            .getJSONObject("sdm.devices.traits.ThermostatMode").getString("mode")));
                            updateState(thermostatCurrentMode, modeState);
                        }

                        if (jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                .getJSONObject("sdm.devices.traits.ThermostatMode").has("availableModes")) {
                            JSONArray jaAvailableModes = jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                    .getJSONObject("sdm.devices.traits.ThermostatMode").getJSONArray("availableModes");
                            String[] deviceAvailableThermostatModes = new String[jaAvailableModes.length()];
                            for (int nCount = 0; nCount < jaAvailableModes.length(); nCount++) {
                                // get Available Modes
                                deviceAvailableThermostatModes[nCount] = jaAvailableModes.getString(nCount);
                            }
                            nestThermostat.setAvailableThermostatModes(deviceAvailableThermostatModes);
                        }
                    }

                    if (jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                            .has("sdm.devices.traits.ThermostatEcoMode")) {
                        if (jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                .getJSONObject("sdm.devices.traits.ThermostatEcoMode").has("mode")) {

                            State ecoModeState = new StringType(nestThermostat.setThermostatEcoModeValue(
                                    jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                            .getJSONObject("sdm.devices.traits.ThermostatEcoMode").getString("mode")));
                            updateState(thermostatCurrentEcoMode, ecoModeState);
                        }

                        if (jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                .getJSONObject("sdm.devices.traits.ThermostatEcoMode").has("availableModes")) {
                            JSONArray jaAvailableModes = jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                    .getJSONObject("sdm.devices.traits.ThermostatEcoMode")
                                    .getJSONArray("availableModes");
                            String[] deviceAvailableThermostatEcoModes = new String[jaAvailableModes.length()];
                            for (int nCount = 0; nCount < jaAvailableModes.length(); nCount++) {
                                // get Available Modes
                                deviceAvailableThermostatEcoModes[nCount] = jaAvailableModes.getString(nCount);
                            }
                            nestThermostat.setAvailableThermostatEcoModes(deviceAvailableThermostatEcoModes);
                        }
                        if ((jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                .getJSONObject("sdm.devices.traits.ThermostatEcoMode").has("heatCelsius"))
                                && (jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                        .getJSONObject("sdm.devices.traits.ThermostatEcoMode").has("coolCelsius"))
                                && (jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                        .getJSONObject("sdm.devices.traits.ThermostatEcoMode").has("mode"))) {

                            // If Mode is active, we set to this value for our aggregated minmaxvalue
                            if ((jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                    .getJSONObject("sdm.devices.traits.ThermostatEcoMode").has("coolCelsius"))
                                    && (jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                            .getJSONObject("sdm.devices.traits.ThermostatEcoMode").getString("mode"))
                                                    .equalsIgnoreCase("manual_eco")) {
                                double[] thermostatMinMax = new double[2];

                                thermostatMinMax[0] = jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                        .getJSONObject("sdm.devices.traits.ThermostatEcoMode").getFloat("heatCelsius");
                                thermostatMinMax[1] = jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                        .getJSONObject("sdm.devices.traits.ThermostatEcoMode").getFloat("coolCelsius");
                                logger.debug("We should not be here for ECO mode settings..");
                                nestThermostat.setMinMaxTemperatureValue(thermostatMinMax);
                                State setMinTempState = new DecimalType(thermostatMinMax[0]);
                                updateState(thermostatMinimumTemperature, setMinTempState);

                                State setMaxTempState = new DecimalType(thermostatMinMax[1]);
                                updateState(thermostatMaximumTemperature, setMaxTempState);
                            }
                        }
                    }

                    if (jo.getJSONObject("resourceUpdate").getJSONObject("traits").has("sdm.devices.traits.Fan")) {
                        if (jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                .getJSONObject("sdm.devices.traits.Fan").has("timerMode")) {
                            State fanState = new StringType(nestThermostat
                                    .setDeviceFan(jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                            .getJSONObject("sdm.devices.traits.Fan").getString("timerMode")));
                            updateState(thermostatFanMode, fanState);
                        }
                    }
                    if (jo.getJSONObject("resourceUpdate").getJSONObject("traits").has("sdm.devices.traits.Humidity")) {
                        // get Humidity properties and set them
                        if (jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                .getJSONObject("sdm.devices.traits.Humidity").has("ambientHumidityPercent")) {
                            State humidityState = new DecimalType(nestThermostat.setHumidityPercent(jo
                                    .getJSONObject("resourceUpdate").getJSONObject("traits")
                                    .getJSONObject("sdm.devices.traits.Humidity").getInt("ambientHumidityPercent")));
                            updateState(thermostatAmbientHumidityPercent, humidityState);
                        }
                    }

                    if (jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                            .has("sdm.devices.traits.ThermostatHvac")) {
                        // get Humidity properties and set them
                        if (jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                .getJSONObject("sdm.devices.traits.ThermostatHvac").has("status")) {
                            State HVACStatusState = new DecimalType(nestThermostat
                                    .setHVACStatus(jo.getJSONObject("resourceUpdate").getJSONObject("traits")
                                            .getJSONObject("sdm.devices.traits.ThermostatHvac").getString("status")));
                            updateState(thermostatHVACStatus, HVACStatusState);
                        }
                    }

                }
                logger.debug("dispatchMessage processed messageId [{}] successfully for device [{}]",
                        message.getMessageId(), thing.getProperties().get("deviceName"));
                return (true);
            } else {
                return (false);
            }

        } catch (JSONException e) {
            logger.debug("dispatchMessage exception {}", e.getMessage());
            return (false);
        } catch (Exception e) {
            logger.debug("dispatchMessage general exception exception {}", e.getMessage());
            return (false);
        }

    }

    public void pubSubEventHandler(String projectId, String subscriptionId) {
        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId);

        Subscriber subscriber = null;
        // Instantiate an asynchronous message receiver.
        // ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);
        MessageReceiver receiver = new MessageReceiver() {
            @Override
            public void receiveMessage(@Nullable PubsubMessage message, final @Nullable AckReplyConsumer consumer) {
                logger.debug("got message: MessageId {}", message.getMessageId());
                try {
                    if (dispatchMessage(message)) {
                        consumer.ack();
                    } else {
                        consumer.nack();
                        logger.debug("messageReceiver NACK message {}", message.getData().toStringUtf8());
                    }
                } catch (IOException e) {
                    logger.debug("receiveMessage threw Exception {}", e.getMessage());
                    consumer.nack();
                }

            }
        };

        try {

            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(new FileInputStream(nestUtility.getServiceAccountPath()))
                    // GoogleCredentials credentials = GoogleCredentials.create(googleAccessToken)
                    .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/pubsub"));

            CredentialsProvider cred = FixedCredentialsProvider.create(credentials);

            ExecutorProvider executorProvider = InstantiatingExecutorProvider.newBuilder().setExecutorThreadCount(5)
                    .build();
            subscriber = Subscriber.newBuilder(subscriptionName, receiver).setCredentialsProvider(cred)
                    .setExecutorProvider(executorProvider).build();
            // Start the subscriber.
            subscriber.startAsync().awaitRunning();
            // Allow the subscriber to run indefinitely unless an unrecoverable error occurs.
            subscriber.awaitTerminated();
            subscriber.stopAsync().awaitTerminated(); // end the async thread
        } catch (IllegalStateException e) {
            logger.debug("illegal state exception {}", e.getMessage());

        } catch (FileNotFoundException e) {
            logger.debug("FileNotFound exception {}", e.getMessage());
        } catch (IOException e) {
            logger.debug("IOException exception {}", e.getMessage());
        }
    }

}
