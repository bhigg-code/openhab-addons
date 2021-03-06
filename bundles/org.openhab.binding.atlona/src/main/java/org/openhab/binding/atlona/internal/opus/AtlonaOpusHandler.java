package org.openhab.binding.atlona.internal.opus;

import java.io.IOException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.atlona.internal.AtlonaHandlerCallback;
import org.openhab.binding.atlona.internal.StatefulHandlerCallback;
import org.openhab.binding.atlona.internal.handler.AtlonaHandler;
import org.openhab.binding.atlona.internal.net.SocketChannelSession;
import org.openhab.binding.atlona.internal.net.SocketSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AtlonaOpusHandler extends AtlonaHandler<AtlonaOpusCapabilities> {

    private final Logger logger = LoggerFactory.getLogger(AtlonaOpusHandler.class);

    /**
     * The {@link AtlonaOpusPortocolHandler} protocol handler
     */
    private AtlonaOpusProtocolHandler atlonaHandler;

    /**
     * The {@link SocketSession} telnet session to the switch. Will be null if not connected.
     */
    private SocketSession session;

    /**
     * The polling job to poll the actual state from the {@link #session}
     */
    private ScheduledFuture<?> polling;

    /**
     * The retry connection event
     */
    private ScheduledFuture<?> retryConnection;

    /**
     * The ping event
     */
    private ScheduledFuture<?> ping;

    // List of all the groups patterns we recognize
    private static final Pattern GROUP_PRIMARY_PATTERN = Pattern.compile("^" + AtlonaOpusConstants.GROUP_PRIMARY + "$");
    private static final Pattern GROUP_PORT_PATTERN = Pattern
            .compile("^" + AtlonaOpusConstants.GROUP_PORT + "(\\d{1,2})$");
    private static final Pattern GROUP_MIRROR_PATTERN = Pattern
            .compile("^" + AtlonaOpusConstants.GROUP_MIRROR + "(\\d{1,2})$");
    private static final Pattern GROUP_VOLUME_PATTERN = Pattern
            .compile("^" + AtlonaOpusConstants.GROUP_VOLUME + "(\\d{1,2})$");

    // List of preset commands we recognize
    private static final Pattern CMD_PRESETSAVE = Pattern
            .compile("^" + AtlonaOpusConstants.CMD_PRESETSAVE + "(\\d{1,2})$");
    private static final Pattern CMD_PRESETRECALL = Pattern
            .compile("^" + AtlonaOpusConstants.CMD_PRESETRECALL + "(\\d{1,2})$");
    private static final Pattern CMD_PRESETCLEAR = Pattern
            .compile("^" + AtlonaOpusConstants.CMD_PRESETCLEAR + "(\\d{1,2})$");

    // List of matrix commands we recognize
    private static final Pattern CMD_MATRIXRESET = Pattern.compile("^" + AtlonaOpusConstants.CMD_MATRIXRESET + "$");
    private static final Pattern CMD_MATRIXRESETPORTS = Pattern
            .compile("^" + AtlonaOpusConstants.CMD_MATRIXRESETPORTS + "$");
    private static final Pattern CMD_MATRIXPORTALL = Pattern
            .compile("^" + AtlonaOpusConstants.CMD_MATRIXPORTALL + "(\\d{1,2})$");

    /**
     * Constructs the handler from the {@link org.eclipse.smarthome.core.thing.Thing} with the number of power ports and
     * audio ports the switch supports.
     *
     * @param thing a non-null {@link org.eclipse.smarthome.core.thing.Thing} the handler is for
     * @param capabilities a non-null {@link org.openhab.binding.atlona.internal.pro3.AtlonaOpusCapabilities}
     */
    public AtlonaOpusHandler(Thing thing, AtlonaOpusCapabilities capabilities) {
        super(thing, capabilities);

        if (thing == null) {
            throw new IllegalArgumentException("thing cannot be null");
        }

    }

    /**
     * {@inheritDoc}
     *
     * Handles commands to specific channels. This implementation will offload much of its work to the
     * {@link AtlonaOpusProtocolHandler}. Basically we validate the type of command for the channel then call the
     * {@link AtlonaOpusProtocolHandler} to handle the actual protocol. Special use case is the {@link RefreshType}
     * where we call {{@link #handleRefresh(String)} to handle a refresh of the specific channel (which in turn calls
     * {@link AtlonaOpusProtocolHandler} to handle the actual refresh
     */
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            handleRefresh(channelUID);
            return;
        }

        final String group = channelUID.getGroupId().toLowerCase();
        final String id = channelUID.getIdWithoutGroup().toLowerCase();

        Matcher m;
        if ((m = GROUP_PRIMARY_PATTERN.matcher(group)).matches()) {
            switch (id) {
                case AtlonaOpusConstants.CHANNEL_POWER:
                    if (command instanceof OnOffType) {
                        final boolean makeOn = ((OnOffType) command) == OnOffType.ON;
                        atlonaHandler.setPower(makeOn);
                    } else {
                        logger.debug("Received a POWER channel command with a non OnOffType: {}", command);
                    }

                    break;

                case AtlonaOpusConstants.CHANNEL_PANELLOCK:
                    if (command instanceof OnOffType) {
                        final boolean makeOn = ((OnOffType) command) == OnOffType.ON;
                        atlonaHandler.setPanelLock(makeOn);
                    } else {
                        logger.debug("Received a PANELLOCK channel command with a non OnOffType: {}", command);
                    }
                    break;

                case AtlonaOpusConstants.CHANNEL_IRENABLE:
                    if (command instanceof OnOffType) {
                        final boolean makeOn = ((OnOffType) command) == OnOffType.ON;
                        atlonaHandler.setIrOn(makeOn);
                    } else {
                        logger.debug("Received a IRLOCK channel command with a non OnOffType: {}", command);
                    }

                    break;
                case AtlonaOpusConstants.CHANNEL_MATRIXCMDS:
                    if (command instanceof StringType) {
                        final String matrixCmd = command.toString();
                        Matcher cmd;
                        try {
                            if ((cmd = CMD_MATRIXRESET.matcher(matrixCmd)).matches()) {
                                atlonaHandler.resetMatrix();
                            } else if ((cmd = CMD_MATRIXRESETPORTS.matcher(matrixCmd)).matches()) {
                                atlonaHandler.resetAllPorts();
                            } else if ((cmd = CMD_MATRIXPORTALL.matcher(matrixCmd)).matches()) {
                                if (cmd.groupCount() == 1) {
                                    final int portNbr = Integer.parseInt(cmd.group(1));
                                    atlonaHandler.setPortAll(portNbr);
                                } else {
                                    logger.debug("Unknown matirx set port command: '{}'", matrixCmd);
                                }

                            } else {
                                logger.debug("Unknown matrix command: '{}'", cmd);
                            }
                        } catch (NumberFormatException e) {
                            logger.debug("Could not parse the port number from the command: '{}'", matrixCmd);
                        }
                    }
                    break;
                case AtlonaOpusConstants.CHANNEL_PRESETCMDS:
                    if (command instanceof StringType) {
                        final String presetCmd = command.toString();
                        Matcher cmd;
                        try {
                            if ((cmd = CMD_PRESETSAVE.matcher(presetCmd)).matches()) {
                                if (cmd.groupCount() == 1) {
                                    final int presetNbr = Integer.parseInt(cmd.group(1));
                                    atlonaHandler.saveIoSettings(presetNbr);
                                } else {
                                    logger.debug("Unknown preset save command: '{}'", presetCmd);
                                }
                            } else if ((cmd = CMD_PRESETRECALL.matcher(presetCmd)).matches()) {
                                if (cmd.groupCount() == 1) {
                                    final int presetNbr = Integer.parseInt(cmd.group(1));
                                    atlonaHandler.recallIoSettings(presetNbr);
                                } else {
                                    logger.debug("Unknown preset recall command: '{}'", presetCmd);
                                }
                            } else if ((cmd = CMD_PRESETCLEAR.matcher(presetCmd)).matches()) {
                                if (cmd.groupCount() == 1) {
                                    final int presetNbr = Integer.parseInt(cmd.group(1));
                                    atlonaHandler.clearIoSettings(presetNbr);
                                } else {
                                    logger.debug("Unknown preset clear command: '{}'", presetCmd);
                                }

                            } else {
                                logger.debug("Unknown preset command: '{}'", cmd);
                            }
                        } catch (NumberFormatException e) {
                            logger.debug("Could not parse the preset number from the command: '{}'", presetCmd);
                        }
                    }
                    break;

                default:
                    logger.debug("Unknown/Unsupported Primary Channel: {}", channelUID.getAsString());
                    break;
            }
        } else if ((m = GROUP_PORT_PATTERN.matcher(group)).matches()) {
            if (m.groupCount() == 1) {
                try {
                    final int portNbr = Integer.parseInt(m.group(1));

                    switch (id) {
                        case AtlonaOpusConstants.CHANNEL_PORTOUTPUT:
                            if (command instanceof DecimalType) {
                                final int inpNbr = ((DecimalType) command).intValue();
                                atlonaHandler.setPortSwitch(inpNbr, portNbr);
                            } else {
                                logger.debug("Received a PORTOUTPUT channel command with a non DecimalType: {}",
                                        command);
                            }

                            break;

                        case AtlonaOpusConstants.CHANNEL_PORTPOWER:
                            if (command instanceof OnOffType) {
                                final boolean makeOn = ((OnOffType) command) == OnOffType.ON;
                                atlonaHandler.setPortPower(portNbr, makeOn);
                            } else {
                                logger.debug("Received a PORTPOWER channel command with a non OnOffType: {}", command);
                            }
                            break;
                        default:
                            logger.debug("Unknown/Unsupported Port Channel: {}", channelUID.getAsString());
                            break;
                    }
                } catch (NumberFormatException e) {
                    logger.debug("Bad Port Channel (can't parse the port nbr): {}", channelUID.getAsString());
                }
            }
        } else if ((m = GROUP_MIRROR_PATTERN.matcher(group)).matches()) {
            if (m.groupCount() == 1) {
                try {
                    final int hdmiPortNbr = Integer.parseInt(m.group(1));

                    switch (id) {
                        case AtlonaOpusConstants.CHANNEL_PORTMIRROR:
                            if (command instanceof DecimalType) {
                                final int outPortNbr = ((DecimalType) command).intValue();
                                if (outPortNbr <= 0) {
                                    atlonaHandler.removePortMirror(hdmiPortNbr);
                                } else {
                                    atlonaHandler.setPortMirror(hdmiPortNbr, outPortNbr);
                                }
                            } else {
                                logger.debug("Received a PORTMIRROR channel command with a non DecimalType: {}",
                                        command);
                            }

                            break;
                        case AtlonaOpusConstants.CHANNEL_PORTMIRRORENABLED:
                            if (command instanceof OnOffType) {
                                if (command == OnOffType.ON) {
                                    final StatefulHandlerCallback callback = (StatefulHandlerCallback) atlonaHandler
                                            .getCallback();
                                    final State state = callback.getState(AtlonaOpusConstants.CHANNEL_PORTMIRROR);
                                    int outPortNbr = 1;
                                    if (state != null && state instanceof DecimalType) {
                                        outPortNbr = ((DecimalType) state).intValue();
                                    }
                                    atlonaHandler.setPortMirror(hdmiPortNbr, outPortNbr);
                                } else {
                                    atlonaHandler.removePortMirror(hdmiPortNbr);
                                }
                            } else {
                                logger.debug("Received a PORTMIRROR channel command with a non DecimalType: {}",
                                        command);
                            }

                            break;
                        default:
                            logger.debug("Unknown/Unsupported Mirror Channel: {}", channelUID.getAsString());
                            break;
                    }
                } catch (NumberFormatException e) {
                    logger.debug("Bad Mirror Channel (can't parse the port nbr): {}", channelUID.getAsString());
                }
            }
        } else if ((m = GROUP_VOLUME_PATTERN.matcher(group)).matches()) {
            if (m.groupCount() == 1) {
                try {
                    final int portNbr = Integer.parseInt(m.group(1));

                    switch (id) {
                        case AtlonaOpusConstants.CHANNEL_VOLUME_MUTE:
                            if (command instanceof OnOffType) {
                                atlonaHandler.setVolumeMute(portNbr, ((OnOffType) command) == OnOffType.ON);
                            } else {
                                logger.debug("Received a VOLUME MUTE channel command with a non OnOffType: {}",
                                        command);
                            }

                            break;
                        case AtlonaOpusConstants.CHANNEL_VOLUME:
                            if (command instanceof DecimalType) {
                                final double level = ((DecimalType) command).doubleValue();
                                atlonaHandler.setVolume(portNbr, level);
                            } else {
                                logger.debug("Received a VOLUME channel command with a non DecimalType: {}", command);
                            }
                            break;

                        default:
                            logger.debug("Unknown/Unsupported Volume Channel: {}", channelUID.getAsString());
                            break;
                    }
                } catch (NumberFormatException e) {
                    logger.debug("Bad Volume Channel (can't parse the port nbr): {}", channelUID.getAsString());
                }
            }
        } else {
            logger.debug("Unknown/Unsupported Channel: {}", channelUID.getAsString());
        }
    }

    /**
     * Method that handles the {@link RefreshType} command specifically. Calls the {@link AtlonaOpusPortocolHandler} to
     * handle the actual refresh based on the channel id.
     *
     * @param id a non-null, possibly empty channel id to refresh
     */
    private void handleRefresh(ChannelUID channelUID) {
        if (getThing().getStatus() != ThingStatus.ONLINE) {
            return;
        }

        final String group = channelUID.getGroupId().toLowerCase();
        final String id = channelUID.getIdWithoutGroup().toLowerCase();
        final StatefulHandlerCallback callback = (StatefulHandlerCallback) atlonaHandler.getCallback();

        Matcher m;
        if ((m = GROUP_PRIMARY_PATTERN.matcher(group)).matches()) {
            switch (id) {
                case AtlonaOpusConstants.CHANNEL_POWER:
                    callback.removeState(AtlonaOpusUtilities.createChannelID(group, id));
                    atlonaHandler.refreshPower();
                    break;

                default:
                    break;
            }

        } else if ((m = GROUP_PORT_PATTERN.matcher(group)).matches()) {
            if (m.groupCount() == 1) {
                try {
                    final int portNbr = Integer.parseInt(m.group(1));
                    callback.removeState(AtlonaOpusUtilities.createChannelID(group, portNbr, id));

                    switch (id) {
                        case AtlonaOpusConstants.CHANNEL_PORTOUTPUT:
                            atlonaHandler.refreshPortStatus(portNbr);
                            break;

                        case AtlonaOpusConstants.CHANNEL_PORTPOWER:
                            atlonaHandler.refreshPortPower(portNbr);
                            break;
                        default:
                            break;
                    }
                } catch (NumberFormatException e) {
                    logger.debug("Bad Port Channel (can't parse the port nbr): {}", channelUID.getAsString());
                }

            }
        } else if ((m = GROUP_MIRROR_PATTERN.matcher(group)).matches()) {
            if (m.groupCount() == 1) {
                try {
                    final int hdmiPortNbr = Integer.parseInt(m.group(1));
                    callback.removeState(AtlonaOpusUtilities.createChannelID(group, hdmiPortNbr, id));
                    atlonaHandler.refreshPortMirror(hdmiPortNbr);
                } catch (NumberFormatException e) {
                    logger.debug("Bad Mirror Channel (can't parse the port nbr): {}", channelUID.getAsString());
                }

            }
        } else if ((m = GROUP_VOLUME_PATTERN.matcher(group)).matches()) {
            if (m.groupCount() == 1) {
                try {
                    final int portNbr = Integer.parseInt(m.group(1));
                    callback.removeState(AtlonaOpusUtilities.createChannelID(group, portNbr, id));

                    switch (id) {
                        case AtlonaOpusConstants.CHANNEL_VOLUME_MUTE:
                            atlonaHandler.refreshVolumeMute(portNbr);
                            break;
                        case AtlonaOpusConstants.CHANNEL_VOLUME:
                            atlonaHandler.refreshVolumeStatus(portNbr);
                            break;

                        default:
                            break;
                    }
                } catch (NumberFormatException e) {
                    logger.debug("Bad Volume Channel (can't parse the port nbr): {}", channelUID.getAsString());
                }

            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * Initializes the handler. This initialization will read/validate the configuration, then will create the
     * {@link SocketSession}, initialize the {@link AtlonaOpusPortocolHandler} and will attempt to connect to the switch
     * (via {{@link #retryConnect()}.
     */
    @Override
    public void initialize() {
        final AtlonaOpusConfig config = getAtlonaConfig();

        if (config == null) {
            return;
        }

        if (config.getIpAddress() == null || config.getIpAddress().trim().length() == 0) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "IP Address of Atlona Opus is missing from configuration");
            return;
        }

        session = new SocketChannelSession(config.getIpAddress(), 23);
        atlonaHandler = new AtlonaOpusProtocolHandler(session, config, getCapabilities(),
                new StatefulHandlerCallback(new AtlonaHandlerCallback() {
                    @Override
                    public void stateChanged(String channelId, State state) {
                        updateState(channelId, state);
                    }

                    @Override
                    public void statusChanged(ThingStatus status, ThingStatusDetail detail, String msg) {
                        updateStatus(status, detail, msg);

                        if (status != ThingStatus.ONLINE) {
                            disconnect(true);
                        }
                    }

                    @Override
                    public void setProperty(String propertyName, String propertyValue) {
                        getThing().setProperty(propertyName, propertyValue);
                    }
                }));

        // Try initial connection in a scheduled task
        this.scheduler.schedule(this::connect, 1, TimeUnit.SECONDS);
    }

    /**
     * Attempts to connect to the switch. If successfully connect, the {@link AtlonaOpusPortocolHandler#login()} will be
     * called to log into the switch (if needed). Once completed, a polling job will be created to poll the switch's
     * actual state and a ping job to ping the server. If a connection cannot be established (or login failed), the
     * connection attempt will be retried later (via {@link #retryConnect()})
     */
    private void connect() {
        String response = "Server is offline - will try to reconnect later";
        try {
            // clear listeners to avoid any 'old' listener from handling initial messages
            session.clearListeners();
            session.connect();

            response = atlonaHandler.login();
            if (response == null) {
                final AtlonaOpusConfig config = getAtlonaConfig();
                if (config != null) {
                    polling = this.scheduler.scheduleWithFixedDelay(() -> {
                        final ThingStatus status = getThing().getStatus();
                        if (status == ThingStatus.ONLINE) {
                            if (session.isConnected()) {
                                atlonaHandler.refreshAll();
                            } else {
                                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                                        "Atlona OPUS has disconnected. Will try to reconnect later.");
                            }
                        } else if (status == ThingStatus.OFFLINE) {
                            disconnect(true);
                        }
                    }, config.getPolling(), config.getPolling(), TimeUnit.SECONDS);

                    ping = this.scheduler.scheduleWithFixedDelay(() -> {
                        final ThingStatus status = getThing().getStatus();
                        if (status == ThingStatus.ONLINE) {
                            if (session.isConnected()) {
                                atlonaHandler.ping();
                            }
                        }
                    }, config.getPing(), config.getPing(), TimeUnit.SECONDS);

                    updateStatus(ThingStatus.ONLINE);
                    return;
                }
            }

        } catch (Exception e) {
            // do nothing
        }

        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, response);
        retryConnect();
    }

    /**
     * Attempts to disconnect from the session and will optionally retry the connection attempt. The {@link #polling}
     * will be cancelled, the {@link #ping} will be cancelled and both set to null then the {@link #session} will be
     * disconnected.
     *
     * @param retryConnection true to retry connection attempts after the disconnect
     */
    private void disconnect(boolean retryConnection) {
        // Cancel polling
        if (polling != null) {
            polling.cancel(true);
            polling = null;
        }

        // Cancel ping
        if (ping != null) {
            ping.cancel(true);
            ping = null;
        }

        try {
            session.disconnect();
        } catch (IOException e) {
            // ignore - we don't care
        }

        if (retryConnection) {
            retryConnect();
        }
    }

    /**
     * Retries the connection attempt - schedules a job in {@link AtlonaOpusConfig#getRetryPolling()} seconds to call
     * the
     * {@link #connect()} method. If a retry attempt is pending, the request is ignored.
     */
    private void retryConnect() {
        if (retryConnection == null) {
            final AtlonaOpusConfig config = getAtlonaConfig();
            if (config != null) {
                logger.info("Will try to reconnect in {} seconds", config.getRetryPolling());
                retryConnection = this.scheduler.schedule(() -> {
                    retryConnection = null;
                    connect();
                }, config.getRetryPolling(), TimeUnit.SECONDS);
            }
        } else {
            logger.debug("RetryConnection called when a retry connection is pending - ignoring request");
        }
    }

    /**
     * Simple gets the {@link AtlonaOpusConfig} from the {@link Thing} and will set the status to offline if not found.
     *
     * @return a possible null {@link AtlonaOpusConfig}
     */
    private AtlonaOpusConfig getAtlonaConfig() {
        final AtlonaOpusConfig config = getThing().getConfiguration().as(AtlonaOpusConfig.class);

        if (config == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);
        }

        return config;
    }

    /**
     * {@inheritDoc}
     *
     * Disposes of the handler. Will simply call {@link #disconnect(boolean)} to disconnect and NOT retry the
     * connection
     */
    @Override
    public void dispose() {
        disconnect(false);
    }

}
