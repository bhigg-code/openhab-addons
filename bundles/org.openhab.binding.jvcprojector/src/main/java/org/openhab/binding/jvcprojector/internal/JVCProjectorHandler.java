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
package org.openhab.binding.jvcprojector.internal;

import static org.openhab.binding.jvcprojector.internal.JVCProjectorBindingConstants.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link JVCProjectorHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Brian Higginbotham - Initial contribution
 */
@NonNullByDefault
public class JVCProjectorHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(JVCProjectorHandler.class);

    private @NonNullByDefault({}) JVCProjectorConfiguration config;
    private @NonNullByDefault({}) JVCProjectorConnection connection;
    private @NonNullByDefault({}) ScheduledFuture<?> refreshJob;
    private final Map<String, String> stateMap = Collections.synchronizedMap(new HashMap<>());
    private @NonNullByDefault({}) String[] gstrArray;

    public JVCProjectorHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void dispose() {
        if (refreshJob != null) {
            refreshJob.cancel(true);
        }
    }

    private void setGlobalStringArray(String[] strReturnValue) {
        gstrArray = strReturnValue.clone();
    }

    public boolean CheckPowerStatus(ChannelUID channelUID) {

        int[] nValue = new int[2];
        nValue[0] = 6; // ACK response length
        nValue[1] = 7; // Response to a power command
        // String[] strReturnValues = new String[nValue.length];

        if (dispatchCommand(JVCProjectorBindingConstants.bPowerStatusCheck,
                JVCProjectorBindingConstants.bPowerOnStatusAck, nValue)) {
            for (int nInt = 1; nInt < gstrArray.length; nInt++) {
                if (gstrArray[nInt].length() == JVCProjectorBindingConstants.bPowerOnResponse.length) {
                    if (CompareByteToStringUnsigned(JVCProjectorBindingConstants.bPowerOnResponse, gstrArray[nInt])) {
                        logger.info("CheckPowerStatus returning that the Projector power is currently ON");
                        updateState(channelUID, OnOffType.ON);
                        return (true);
                    }
                    if (CompareByteToStringUnsigned(JVCProjectorBindingConstants.bPowerOffResponse, gstrArray[nInt])) {
                        logger.info("CheckPowerStatus returning that the Projector power is currently OFF");
                        updateState(channelUID, OnOffType.OFF);
                        return (false);
                    }
                    if (CompareByteToStringUnsigned(JVCProjectorBindingConstants.bPowerResponseCooling,
                            gstrArray[nInt])) {
                        logger.info(
                                "CheckPowerStatus returning that the Projector power is turning Off in COOLING mode");
                        updateState(channelUID, OnOffType.OFF);
                        return (false);
                    }
                    if (CompareByteToStringUnsigned(JVCProjectorBindingConstants.bPowerResponseReserved,
                            gstrArray[nInt])) {
                        logger.info(
                                "CheckPowerStatus returning that the Projector power is turning on in RESERVED mode");
                        updateState(channelUID, OnOffType.ON);
                        return (false);
                    }
                    if (CompareByteToStringUnsigned(JVCProjectorBindingConstants.bPowerResponseEmergency,
                            gstrArray[nInt])) {
                        logger.info(
                                "CheckPowerStatus returning that the Projector power is an EMERGENCY mode when power ON. Check Status");
                        updateState(channelUID, OnOffType.OFF);
                        return (false);
                    }
                } else {
                    logger.debug("CheckPowerStatus failed to compare PowerOnResponse on PowerStatus command..");
                    return (false);
                }
            } // end for
        }
        logger.debug("CheckPowerStatus reporting the command send FAILED for a power status check..");
        return (false);
    }

    private String getInputStatus() {
        int[] nValue = new int[2];

        nValue[0] = 6; // Expected response length from Get Lamp Hours Command
        nValue[1] = 7; // expected 7 bytes coming back from get input status
        String strInput = new String("ERROR");

        if (dispatchCommand(JVCProjectorBindingConstants.bGetInputStatus, JVCProjectorBindingConstants.bInputSuccess,
                nValue)) {
            // get Input
            if (gstrArray[1].charAt(5) == 54) {
                strInput = "HDMI1";
                State newState = new StringType(strInput);
                updateState("InputHDMI", newState);
            }
            if (gstrArray[1].charAt(5) == 55) {
                strInput = "HDMI2";
                State newState = new StringType(strInput);
                updateState("InputHDMI", newState);
            }

            logger.info("{} reporting Input is currently set to  {}", thing.getUID().toString(), strInput);
            return (strInput);
        } else {
            logger.debug("{} FAILED to get Input Status back from thing.", thing.getUID().toString());
            return ("Error");
        }
    }

    private void getChannelState(ChannelUID channelUID) {

        if (JVCProjectorBindingConstants.Power.equals(channelUID.getId())) {
            logger.debug("getChannelState Processing Power Refresh status for {}", channelUID.getId());
            CheckPowerStatus(channelUID);
        } else if (JVCProjectorBindingConstants.InputHDMI.equals(channelUID.getId())) {
            logger.debug("getChannelState Processing Input Refresh status for {}", channelUID.getId());
            getInputStatus();
        } else {
            logger.debug("getChannelState reporting unknown refresh command for channel {}", channelUID.getId());
        }
    }

    private boolean dispatchCommand(byte[] bCommand, byte[] bResponse, int[] nValue) {

        try {
            JVCProjectorConnection connection = new JVCProjectorConnection(config.ipaddress, bCommand, nValue);
            connection.start();
            connection.join();
            setGlobalStringArray(connection.strReturnValues); // sets array for value in class CheckPowerStatus
            for (int nInt = 0; nInt < connection.strReturnValues.length; nInt++) {
                if (connection.strReturnValues[nInt].length() == bResponse.length) {
                    if (CompareByteToStringUnsigned(bResponse, connection.strReturnValues[nInt])) {
                        return (true);
                    } else {
                        return (false);
                    }
                } else {
                    logger.debug(
                            "dispatchCommand received an INVALID String. More than likely due to a read time out or failure in the socket.. Will return false for processing the command..{} {} length [{}] length [{}]",
                            bCommand, connection.strReturnValues, connection.strReturnValues.length, bCommand.length);
                    return (false);
                }
            } // end for
            return (false);
        } catch (InterruptedException e) {
            logger.debug("dispatchCommand returning an exception {}", e.getMessage());
            return (false);
        }
    }

    private String getLampHours(Thing thing) {
        int[] nValue = new int[2];

        nValue[0] = 6; // Expected response length from Get Lamp Hours Command
        nValue[1] = 10; // expected 10 bytes coming back from the get lamp hours command
        if (dispatchCommand(JVCProjectorBindingConstants.bGetLampHours,
                JVCProjectorBindingConstants.bGetLampHoursSuccess, nValue)) {
            // get software version
            String strLampHours = gstrArray[1].substring(5, 9);
            State newState = new StringType(String.format("%dh", Integer.parseInt(strLampHours, 16)));
            updateState("LampHours", newState);
            // thing.setProperty("PROPERTY_FIRMWARE_VERSION", strVersion);
            logger.info("{} reporting Lamp Hours at {}", thing.getUID().toString(),
                    String.format("%dh", Integer.parseInt(strLampHours, 16)));
            return (String.format("%dh", Integer.parseInt(strLampHours, 16)));
        } else {
            logger.debug("{} FAILED to get Lamp Hours back from thing..", thing.getUID().toString());
            return ("Error");
        }
    }

    private String getSoftwareVersion(Thing thing) {
        int[] nValue = new int[2];

        nValue[0] = 6; // Expected response length from software version Command
        nValue[1] = 12; // expected 12 bytes coming back from the software version command
        if (dispatchCommand(JVCProjectorBindingConstants.bGetSoftwareVersion,
                JVCProjectorBindingConstants.bGetSoftwareVersionSuccess, nValue)) {
            // get software version
            String strVersion = "v" + gstrArray[1].substring(6, 7) + "." + gstrArray[1].substring(7, 10);
            thing.setProperty("PROPERTY_FIRMWARE_VERSION", strVersion);
            State newState = new StringType(strVersion);
            updateState("Firmware", newState);
            logger.info("{} reporting a Firmware Version {}", thing.getUID().toString(), strVersion);
            return (strVersion);
        } else {
            logger.debug("{} FAILED to get Firmware Version back from thing..", thing.getUID().toString());
            return ("Error");
        }
    }

    private String getModelInformation(Thing thing) {
        int[] nValue = new int[2];

        nValue[0] = 6; // Expected response length from Model Status Command
        nValue[1] = 20; // expected 14 bytes coming back from the model command
        if (dispatchCommand(JVCProjectorBindingConstants.bGetModelNumber,
                JVCProjectorBindingConstants.bGetModelNumberSuccess, nValue)) {
            State newState;
            switch (gstrArray[1].charAt(18)) {
                case '1':
                    logger.info("{} reporting a jvc-projector-nx9 model inventory", thing.getUID().toString());
                    thing.setProperty("PROPERTY_MODEL_ID", THING_TYPE_NX9.toString());
                    newState = new StringType("JVC DLA-NX9");
                    updateState("Model", newState);
                    return (THING_TYPE_NX9.toString());
                case '2':
                    logger.info("{} reporting a jvc-projector-nx7 model inventory", thing.getUID().toString());
                    thing.setProperty("PROPERTY_MODEL_ID", THING_TYPE_NX7.toString());
                    newState = new StringType("JVC DLA-NX7");
                    updateState("Model", newState);
                    return (THING_TYPE_NX7.toString());
                case '3':
                    logger.info("{} reporting a jvc-projector-nx5 model inventory", thing.getUID().toString());
                    thing.setProperty("PROPERTY_MODEL_ID", THING_TYPE_NX7.toString());
                    newState = new StringType("JVC DLA-NX5");
                    updateState("Model", newState);
                    return (THING_TYPE_NX7.toString());
                default:
                    logger.debug("Failed to identify model from string {}..Setting to Generic ID", gstrArray[1]);
                    newState = new StringType("JVC DLA-GENERIC");
                    updateState("Model", newState);
                    return (THING_TYPE_GENERIC.toString());
            }
        } else {
            logger.debug("{} FAILED to get model back from thing..", thing.getUID().toString());
            return ("Error");
        }
    }

    private boolean setPictureMode(String strMode) {

        int nValue[] = new int[1];

        switch (strMode) {
            case "Film":
                nValue[0] = 6; // expected 6 bytes from a successful Picture Mode command
                if (dispatchCommand(JVCProjectorBindingConstants.bPictureModeFilm,
                        JVCProjectorBindingConstants.bPictureModeSuccess, nValue)) {
                    State newState = new StringType("Film");
                    updateState("PictureMode", newState);
                    return (true);
                } else {
                    break;
                }
            case "Cinema":
                nValue[0] = 6; // expected 6 bytes from a successful Picture Mode command
                if (dispatchCommand(JVCProjectorBindingConstants.bPictureModeCinema,
                        JVCProjectorBindingConstants.bPictureModeSuccess, nValue)) {
                    State newState = new StringType("Cinema");
                    updateState("PictureMode", newState);
                    return (true);
                } else {
                    break;
                }
            case "Natural":
                nValue[0] = 6; // expected 6 bytes from a successful Picture Mode command
                if (dispatchCommand(JVCProjectorBindingConstants.bPictureModeNatural,
                        JVCProjectorBindingConstants.bPictureModeSuccess, nValue)) {
                    State newState = new StringType("Natural");
                    updateState("PictureMode", newState);
                    return (true);
                } else {
                    break;
                }
            case "HDR10":
                nValue[0] = 6; // expected 6 bytes from a successful Picture Mode command
                if (dispatchCommand(JVCProjectorBindingConstants.bPictureModeHDR,
                        JVCProjectorBindingConstants.bPictureModeSuccess, nValue)) {
                    State newState = new StringType("HDR10");
                    updateState("PictureMode", newState);
                    return (true);
                } else {
                    break;
                }
            case "HDR10Adapt":
                nValue[0] = 6; // expected 6 bytes from a successful Picture Mode command
                if (dispatchCommand(JVCProjectorBindingConstants.bPictureModeHDRAdapt,
                        JVCProjectorBindingConstants.bPictureModeSuccess, nValue)) {
                    State newState = new StringType("HDR10Adapt");
                    updateState("PictureMode", newState);
                    return (true);
                } else {
                    break;
                }
            case "THX":
                nValue[0] = 6; // expected 6 bytes from a successful Picture Mode command
                if (dispatchCommand(JVCProjectorBindingConstants.bPictureModeTHX,
                        JVCProjectorBindingConstants.bPictureModeSuccess, nValue)) {
                    State newState = new StringType("THX");
                    updateState("PictureMode", newState);
                    return (true);
                } else {
                    break;
                }

            case "User1":
                nValue[0] = 6; // expected 6 bytes from a successful Picture Mode command
                if (dispatchCommand(JVCProjectorBindingConstants.bPictureModeUser1,
                        JVCProjectorBindingConstants.bPictureModeSuccess, nValue)) {
                    State newState = new StringType("User1");
                    updateState("PictureMode", newState);
                    return (true);
                } else {
                    break;
                }

            case "User2":
                nValue[0] = 6; // expected 6 bytes from a successful Picture Mode command
                if (dispatchCommand(JVCProjectorBindingConstants.bPictureModeUser2,
                        JVCProjectorBindingConstants.bPictureModeSuccess, nValue)) {
                    State newState = new StringType("User2");
                    updateState("PictureMode", newState);
                    return (true);
                } else {
                    break;
                }

            case "User3":
                nValue[0] = 6; // expected 6 bytes from a successful Picture Mode command
                if (dispatchCommand(JVCProjectorBindingConstants.bPictureModeUser3,
                        JVCProjectorBindingConstants.bPictureModeSuccess, nValue)) {
                    State newState = new StringType("User3");
                    updateState("PictureMode", newState);
                    return (true);
                } else {
                    break;
                }

            case "User4":
                nValue[0] = 6; // expected 6 bytes from a successful Picture Mode command
                if (dispatchCommand(JVCProjectorBindingConstants.bPictureModeUser4,
                        JVCProjectorBindingConstants.bPictureModeSuccess, nValue)) {
                    State newState = new StringType("User4");
                    updateState("PictureMode", newState);
                    return (true);
                } else {
                    break;
                }

            case "User5":
                nValue[0] = 6; // expected 6 bytes from a successful Picture Mode command
                if (dispatchCommand(JVCProjectorBindingConstants.bPictureModeUser5,
                        JVCProjectorBindingConstants.bPictureModeSuccess, nValue)) {
                    State newState = new StringType("User5");
                    updateState("PictureMode", newState);
                    return (true);
                } else {
                    break;
                }

            case "User6":
                nValue[0] = 6; // expected 6 bytes from a successful Picture Mode command
                if (dispatchCommand(JVCProjectorBindingConstants.bPictureModeUser6,
                        JVCProjectorBindingConstants.bPictureModeSuccess, nValue)) {
                    State newState = new StringType("User6");
                    updateState("PictureMode", newState);
                    return (true);
                }
            case "HLG":
                nValue[0] = 6; // expected 6 bytes from a successful Picture Mode command
                if (dispatchCommand(JVCProjectorBindingConstants.bPictureModeHLG,
                        JVCProjectorBindingConstants.bPictureModeSuccess, nValue)) {
                    State newState = new StringType("HLG");
                    updateState("PictureMode", newState);
                    return (true);
                } else {
                    break;
                }

            default:
                logger.info("{} FAILED to set the Picture Mode..", thing.getUID().toString());
                return (false);
        }
        logger.info("{} FAILED to setPictureMode for mode {}..", thing.getUID().toString(), strMode);
        return (false);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        config = getConfigAs(JVCProjectorConfiguration.class);
        config.ipaddress = thing.getConfiguration().get("IPAddress").toString();
        int[] nValue = new int[1];

        if (Power.equals(channelUID.getId())) {
            if (command.toString().equals("ON")) {
                nValue[0] = 6; // Expected response length from Power on

                if (dispatchCommand(JVCProjectorBindingConstants.bPowerOn, JVCProjectorBindingConstants.bPowerOnSuccess,
                        nValue)) {
                    logger.info("{} turned the Power ON..", thing.getUID().toString());
                    // Possible set item power to state ON
                    updateState(channelUID, OnOffType.ON);
                } else {
                    logger.debug("{} FAILED to turn the Power ON..", thing.getUID().toString());
                }
            } // end Command Power On
            else if (command.toString().equals("OFF")) {
                nValue[0] = 6; // Expected response length from Power on

                if (dispatchCommand(JVCProjectorBindingConstants.bPowerOff,
                        JVCProjectorBindingConstants.bPowerOffSuccess, nValue)) {
                    logger.info("{} successfully turned the Power OFF..", thing.getUID().toString());
                    // Possible set item power to state ON
                    updateState(channelUID, OnOffType.OFF);
                } else {
                    logger.info("{} FAILED to turn the Power OFF..", thing.getUID().toString());
                }
            } // end Command Power Off
        } // Power command
        else if (JVCProjectorBindingConstants.InputHDMI.equals(channelUID.getId())) {
            if (command.toFullString().equals("HDMI1")) {
                nValue[0] = 6; // Expected response length from Power on

                if (dispatchCommand(JVCProjectorBindingConstants.bInputHDMI1,
                        JVCProjectorBindingConstants.bInputSuccess, nValue)) {
                    State newState = new StringType("HDMI1");
                    updateState("InputHDMI", newState);
                    logger.info("{} successfully changed to INPUT HDMI1..", thing.getUID().toString());
                } else {
                    logger.debug("{} FAILED to change to INPUT HDMI1..", thing.getUID().toString());
                }

            } else if (command.toString().equals("HDMI2")) {
                nValue[0] = 6; // Expected response length from Power on

                if (dispatchCommand(JVCProjectorBindingConstants.bInputHDMI2,
                        JVCProjectorBindingConstants.bInputSuccess, nValue)) {
                    State newState = new StringType("HDMI2");
                    updateState("InputHDMI", newState);
                    logger.info("{} successfully changed to INPUT HDMI2..", thing.getUID().toString());
                } else {
                    logger.debug("{} FAILED to change to INPUT HDMI2..", thing.getUID().toString());
                }
            }
        } else if (JVCProjectorBindingConstants.PictureMode.contentEquals(channelUID.getId())) {
            if (!(command.toFullString().contains("REFRESH"))) {

                if (setPictureMode(command.toFullString())) {
                    logger.info("Successfully changed Picture Mode to {}", command.toFullString());
                } else {
                    logger.debug("Failed to change Picture Mode to {}", command.toFullString());
                }
            }
        }

        else if (JVCProjectorBindingConstants.Model.contentEquals(channelUID.getId()))

        {
            getModelInformation(thing);
        } else {
            logger.debug("{} processing unknown command {}", thing.getUID().toString(), command);
        }
    }

    public boolean getProjectorStatus(Thing thing) {
        logger.debug("getProjectorStatus is refreshing state for the Projector {}", thing.getUID());
        int[] nValue = new int[1];
        nValue[0] = 6; // Expected response length from Power on

        if (dispatchCommand(JVCProjectorBindingConstants.bControllerValid,
                JVCProjectorBindingConstants.bControllerValidSuccess, nValue)) {
            logger.info("getProjectorStatus reporting the Projector THING is ONLINE");
            updateStatus(ThingStatus.ONLINE);
            return (true);
        } else {
            logger.info("getProjectorStatus reporting the Projector THING is Offline");
            updateStatus(ThingStatus.OFFLINE);
            return (false);
        }
    }

    void refreshChannels() {
        logger.info("refreshChannels process a timer for Updating thing Channels for:{}", thing.getUID());
        // Let's refresh the projector status first
        if (getProjectorStatus(thing)) {
            getThing().getChannels().forEach(channel -> getChannelState(channel.getUID()));
            getModelInformation(thing);
            if (CheckPowerStatus(thing.getChannel(Power).getUID())) {
                getSoftwareVersion(thing);
                getLampHours(thing);
                getInputStatus();
            }
        } else {
            logger.debug("refreshChannels reporting that the projector is not online.. Skipping refresh cycle..");
        }

    }

    public boolean CompareByteToStringUnsigned(byte[] bArray, String strArray) {

        boolean bFlag = false;
        for (int n = 0; n < bArray.length; n++) {
            if (Byte.toUnsignedInt(bArray[n]) == strArray.charAt(n)) {
                bFlag = true;
            } else {
                bFlag = false;
                break;
            }
        }
        return (bFlag);
    }

    @SuppressWarnings("null")
    @Override
    public void initialize() {
        config = getConfigAs(JVCProjectorConfiguration.class);
        config.ipaddress = thing.getConfiguration().get("IPAddress").toString();
        config.refresh = Integer.parseInt(thing.getConfiguration().get("Refresh").toString());

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
        if (StringUtil.isBlank(config.ipaddress) && StringUtil.isBlank(config.deviceid)) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "No ip address or deviceid configured.");
            return;
        }
        // set to unknown by default to avoid bad state
        updateStatus(ThingStatus.UNKNOWN);

        // Example for background initialization:
        scheduler.execute(() -> {

            getProjectorStatus(thing);
            getModelInformation(thing);
            if (CheckPowerStatus(thing.getChannel(Power).getUID())) {
                getSoftwareVersion(thing);
            }
        });
        if (refreshJob == null || refreshJob.isCancelled()) {
            refreshJob = scheduler.scheduleWithFixedDelay(this::refreshChannels, 0, config.refresh, TimeUnit.SECONDS);
        }
        logger.debug("Finished initializing!");

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
    }
}
