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

package org.openhab.binding.nestdeviceaccess.internal.doorbell;

import static org.openhab.binding.nestdeviceaccess.internal.nestdeviceaccessBindingConstants.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.RawType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
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
 * The {@link NestDoorbellHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Brian Higginbotham - Initial contribution
 */
@NonNullByDefault
public class NestDoorbellHandler extends BaseThingHandler {

    NestUtility nestUtility = new NestUtility(thing);
    private final Logger logger = LoggerFactory.getLogger(NestDoorbellHandler.class);

    private @NonNullByDefault({}) ScheduledFuture<?> refreshJob;
    private @Nullable nestdeviceaccessConfiguration config;
    private @Nullable Future<?> future;
    NestDoorbell nestDoorbell;
    private @NonNullByDefault({}) RawType soundImage;
    private @NonNullByDefault({}) RawType motionImage;
    private @NonNullByDefault({}) RawType personImage;
    private @NonNullByDefault({}) RawType chimeImage;

    public NestDoorbellHandler(Thing thing) {
        super(thing);
        nestDoorbell = new NestDoorbell(thing); // initialize Doorbell with base properties
    }

    @Nullable
    Subscriber subscriber;

    @Override
    public void dispose() {
        if (refreshJob != null) {
            refreshJob.cancel(true);
        }
        logger.debug("Shutting down the doorbell handler..");
        if (future != null) {
            future.cancel(true);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // logger.debug("handleCommand reporting {},{}", channelUID.getId(), command.toString());
        /*
         * try {
         *
         * if (doorbellName.equals(channelUID.getId())) {
         *
         * if (command instanceof RefreshType) {
         * // TODO: handle data refresh
         *
         * logger.debug("handleCommand reporting {}", command.toString());
         * }
         *
         * // TODO: handle command
         *
         * // Note: if communication with thing fails for some reason,
         * // indicate that by setting the status with detail information:
         * // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
         * // "Could not control device at IP address x.x.x.x");
         * }
         * } catch (IOException e) {
         * logger.debug("handleMessage reporting exception {}", e.getMessage());
         * } catch (InterruptedException e) {
         * // TODO Auto-generated catch block
         * logger.debug("handleMessage reporting exception {}", e.getMessage());
         * }
         */
    }

    void refreshChannels() {
        logger.info("refreshChannels process a timer for Updating thing Channels for:{}", thing.getUID());

        try {
            // refresh status of the device

            if (nestDoorbell.getDoorbellInfo()) {
                /*
                 * State statusState = new StringType(nestThermostat.getDeviceStatus());
                 * updateState("thermostatStatus", statusState);
                 * State nameState = new StringType(nestThermostat.getDeviceName());
                 * updateState("thermostatName", nameState);
                 * State humidityState = new DecimalType(nestThermostat.getCurrentHumidity());
                 * updateState("thermostatHumidityPercent", humidityState);
                 * State ambientState = new DecimalType(nestThermostat.getAmbientTemperatureSetting());
                 * updateState("thermostatAmbientTemperature", ambientState);
                 * State setTempHeatState = new DecimalType(nestThermostat.getCurrentTemperatureHeat());
                 * updateState("thermostatTemperatureHeat", setTempHeatState);
                 * State setTempCoolState = new DecimalType(nestThermostat.getCurrentTemperatureCool());
                 * updateState("thermostatTemperatureCool", setTempCoolState);
                 * State setTargetTempState = new DecimalType(nestThermostat.getTargetTemperature());
                 * updateState("thermostatTargetTemperature", setTargetTempState);
                 * State setMinTempState = new DecimalType(nestThermostat.getMinMaxTemperature()[0]);
                 * updateState("thermostatMinimumTemperature", setMinTempState);
                 * State setMaxTempState = new DecimalType(nestThermostat.getMinMaxTemperature()[1]);
                 * updateState("thermostatMaximumTemperature", setMaxTempState);
                 * State modeState = new StringType(nestThermostat.getThermostatMode());
                 * updateState("thermostatCurrentMode", modeState);
                 * State ecoModeState = new StringType(nestThermostat.getThermostatEcoMode());
                 * updateState("thermostatCurrentEcoMode", ecoModeState);
                 * State scaleSettingState = new StringType(nestThermostat.getTemperatureScaleSetting());
                 * updateState("thermostatScaleSetting", scaleSettingState);
                 */
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
        config.serviceAccountPath = thing.getProperties().get("serviceAccountPath");
        config.subscriptionId = thing.getProperties().get("subscriptionId");
        config.pubsubProjectId = thing.getProperties().get("pubsubProjectId");
        if (thing.getConfiguration().containsKey("refreshInterval")) {
            config.refreshInterval = Integer.parseInt(thing.getProperties().get("refreshInterval").toString());
        } else {
            config.refreshInterval = 300; // default setting
        }

        logger.debug("Start initializing device {}", config.deviceName);

        updateStatus(ThingStatus.UNKNOWN);

        // Example for background initialization:
        scheduler.execute(() -> {
            boolean thingReachable = true; // <background task with long running initialization here>
            try {

                if ((nestUtility.getPubSubProjectId() != null) && (nestUtility.getSubscriptionId() != null)) {
                    logger.debug("starting pubsub doorbell [{}]...", config.deviceName);
                    future = scheduler.submit(() -> {
                        pubSubEventHandler(nestUtility.getPubSubProjectId(), nestUtility.getSubscriptionId());
                    });
                } else {
                    logger.info(
                            "NestDoorBellHandler reporting that PubSub configurations were not defined.. Nest doorbell REQUIRES pubsub for event notifications (i.e. Camera motion and Chime events..) Please follow the README and configure the PubSub topics and configurations...");
                }
                nestDoorbell.initializeDoorbell();
                // when done do:
                thingReachable = true; // No status on doorbell
                if (thingReachable) {
                    updateStatus(ThingStatus.ONLINE);
                } else {
                    updateStatus(ThingStatus.OFFLINE);
                }

            } catch (Exception e) {
                logger.debug("Initialize() caught an exception {} {}", e.getMessage(), e.getStackTrace().toString());
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
        logger.debug("Finished initializing device {}", config.deviceName);
    }

    private boolean updateLiveStreamChannels() throws IOException {

        try {
            nestDoorbell.getCameraLiveStream();
            State streamUrlState = new StringType(nestDoorbell.getStreamUrl());
            updateState(doorbellLiveStreamUrl, streamUrlState);
            State streamTokenState = new StringType(nestDoorbell.getStreamToken());
            updateState(doorbellLiveStreamCurrentToken, streamTokenState);
            State streamExtensionToken = new StringType(nestDoorbell.getStreamExensionToken());
            updateState(doorbellLiveStreamExtensionToken, streamExtensionToken);
            State streamExpirationTime = new StringType(nestDoorbell.getStreamTokenExpiration().toString());
            updateState(doorbellLiveStreamExpirationTime, streamExpirationTime);
            return (true);
        } catch (IOException e) {
            throw new IOException(e.getMessage());
        }

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
            DateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            messageTime = utcFormat.parse(jo.getString("timestamp"));

            if (resourceName.contains(thing.getProperties().get("deviceId"))) {
                // find out if it is an event or trait
                if (jo.getJSONObject("resourceUpdate").has("events")) {

                    eventTrait = jo.getJSONObject("resourceUpdate").getJSONObject("events").names().getString(0);
                    eventSessionId = jo.getJSONObject("resourceUpdate").getJSONObject("events")
                            .getJSONObject(eventTrait).getString("eventSessionId");
                    eventId = jo.getJSONObject("resourceUpdate").getJSONObject("events").getJSONObject(eventTrait)
                            .getString("eventId");

                    if (jo.getJSONObject("resourceUpdate").getJSONObject("events")
                            .has("sdm.devices.events.CameraSound.Sound")) {
                        eventTrait = "sdm.devices.events.CameraSound.Sound";
                        nestDoorbell.setCameraSoundEvent(messageTime);
                        if (nestDoorbell.getCameraSoundEvent()) {
                            updateState(doorbellSoundEvent, OnOffType.ON);
                            State newState = new StringType(messageTime.toString());
                            updateState(doorbellSoundLastEventTime, newState);
                            if (nestDoorbell.isImageValid(messageTime)) {
                                soundImage = nestDoorbell.getCameraImage(eventId);
                                updateState(doorbellEventImage, soundImage);
                                logger.debug("dispatchMessage processed a Sound camera image with data {}", soundImage);
                                updateLiveStreamChannels();
                            }

                        } else {
                            updateState(doorbellSoundEvent, OnOffType.OFF);
                        }
                    }
                    if (jo.getJSONObject("resourceUpdate").getJSONObject("events")
                            .has("sdm.devices.events.CameraPerson.Person")) {
                        eventTrait = "sdm.devices.events.CameraPerson.Person";
                        nestDoorbell.setCameraPersonEvent(messageTime);
                        if (nestDoorbell.getCameraPersonEvent()) {
                            updateState(doorbellPersonEvent, OnOffType.ON);
                            State newState = new StringType(messageTime.toString());
                            updateState(doorbellPersonLastEventTime, newState);
                            if (nestDoorbell.isImageValid(messageTime)) {
                                personImage = nestDoorbell.getCameraImage(eventId);
                                updateState(doorbellEventImage, personImage);
                                logger.debug("dispatchMessage processed a Person camera image with data {}",
                                        personImage);
                                updateLiveStreamChannels();
                            }

                        } else {
                            updateState(doorbellPersonEvent, OnOffType.OFF);
                        }
                    }
                    if (jo.getJSONObject("resourceUpdate").getJSONObject("events")
                            .has("sdm.devices.events.CameraMotion.Motion")) {
                        eventTrait = "sdm.devices.events.CameraMotion.Motion";
                        nestDoorbell.setCameraMotionEvent(messageTime);
                        if (nestDoorbell.getCameraMotionEvent()) {
                            updateState(doorbellMotionEvent, OnOffType.ON);
                            State newState = new StringType(messageTime.toString());
                            updateState(doorbellMotionLastEventTime, newState);
                            if (nestDoorbell.isImageValid(messageTime)) {
                                motionImage = nestDoorbell.getCameraImage(eventId);
                                updateState(doorbellEventImage, motionImage);
                                logger.debug("dispatchMessage processed a Motion camera image with data {}",
                                        motionImage);
                                updateLiveStreamChannels();
                            }
                        } else {
                            updateState(doorbellMotionEvent, OnOffType.OFF);
                        }
                    }
                    if (jo.getJSONObject("resourceUpdate").getJSONObject("events")
                            .has("sdm.devices.events.DoorbellChime.Chime")) {
                        eventTrait = "sdm.devices.events.DoorbellChime.Chime";
                        nestDoorbell.setCameraChimeEvent(messageTime);
                        if (nestDoorbell.getCameraChimeEvent()) {
                            updateState(doorbellChimeEvent, OnOffType.ON);
                            State newState = new StringType(messageTime.toString());
                            updateState(doorbellChimeLastEventTime, newState);
                            if (nestDoorbell.isImageValid(messageTime)) {
                                chimeImage = nestDoorbell.getCameraImage(eventId);
                                updateState(doorbellEventImage, chimeImage);
                                logger.debug("dispatchMessage processed a Chime camera image with data {}", chimeImage);
                                updateLiveStreamChannels();
                            }
                        } else {
                            updateState(doorbellChimeEvent, OnOffType.OFF);
                        }
                    }
                    logger.debug("dispatchMessage processed messageId {} successfully", message.getMessageId());
                    return (true);

                }
            } else {
                logger.debug("dispatchMessage is skipping a message {} because it doesn't belong to this device..",
                        message.getMessageId());
                return (false); // another deviceID
            }

        } catch (JSONException e) {
            logger.debug("dispatchMessage exception {}", e.getMessage());
            return (false);
        } catch (IOException e) {
            throw new IOException(e.getMessage());
        } catch (Exception e) {
            logger.debug("dispatchMessage general exception exception {}", e.getMessage());
            return (false);
        }

        return (false);
    }

    public void pubSubEventHandler(String projectId, String subscriptionId) {
        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId);

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
                    if (e.getMessage().contains("503")) {
                        logger.debug(
                                "The image is no longer valid and we need to acknowledge the message to avoid retries..");
                        consumer.ack();
                    } else {
                        logger.debug("Made it to nack..");
                        consumer.nack();
                    }

                }

            }
        };

        try {

            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(new FileInputStream(nestUtility.getServiceAccountPath()))
                    // GoogleCredentials credentials = GoogleCredentials.create(googleAccessToken)
                    .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/pubsub"));

            CredentialsProvider cred = FixedCredentialsProvider.create(credentials);

            ExecutorProvider executorProvider = InstantiatingExecutorProvider.newBuilder().setExecutorThreadCount(1)
                    .build();
            subscriber = Subscriber.newBuilder(subscriptionName, receiver).setCredentialsProvider(cred)
                    .setExecutorProvider(executorProvider).build();

            // Start the subscriber.
            subscriber.startAsync().awaitRunning();
            // Allow the subscriber to run indefinitely unless an unrecoverable error occurs.
            subscriber.awaitTerminated();
            subscriber.stopAsync(); // end the async thread

        } catch (IllegalStateException e) {
            logger.debug("illegal state exception {}", e.getMessage());

        } catch (FileNotFoundException e) {
            logger.debug("FileNotFound exception {}", e.getMessage());
        } catch (IOException e) {
            logger.debug("IOException exception {}", e.getMessage());
        }
    }

}
