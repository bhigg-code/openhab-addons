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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.Semaphore;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class acts as and interface to the physical device.
 *
 * @author Christian Fischer - Initial contribution
 * @author Hilbrand Bouwkamp - Reorganized code an put connection in single class
 * @author Brian Higginbotham - Modified to handle Telnet
 */
@NonNullByDefault
public class JVCProjectorConnection extends Thread {

    private static final int JVC_PROJECTOR_PORT = 20554;
    private static final int SOCKET_TIMEOUT_MILLISECONDS = 5000;
    private final Logger logger = LoggerFactory.getLogger(JVCProjectorConnection.class);
    private @Nullable String ipAddress;
    private Semaphore wSem = new Semaphore(1);
    private int[] nExpectedBytes;
    private byte[] aCommand;
    public String[] strReturnValues;

    /**
     * Initializes a connection to the given ip address.
     *
     * @param ipAddress ip address of the connection
     */
    public JVCProjectorConnection(@Nullable final String ipAddress, byte[] command, int[] expectedBytes) {
        this.ipAddress = ipAddress;
        this.nExpectedBytes = expectedBytes;
        this.aCommand = command;
        this.strReturnValues = new String[expectedBytes.length];
    }

    /**
     * Set the ip address to connect to.
     *
     * @param ipAddress The ip address to connect to
     */
    public void setIpAddress(final String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String HextoAscii(String command) {

        StringBuilder output = new StringBuilder("");

        for (int i = 0; i < command.length(); i += 2) {
            String str = command.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }
        return output.toString();
    }

    /*
     * Coverts asciitohex
     *
     */
    public String AsciitoHex(String command) {

        char[] chars = command.toCharArray();

        StringBuilder hex = new StringBuilder();

        for (char ch : chars) {
            hex.append(Integer.toHexString(ch));
        }

        return hex.toString();
    }

    @Override
    public void run() {

        try {
            wSem.acquire(); // set the gate to avoid concurrency issues with socket
            // retry for socket due to JVC interface not closing down properly
            int nCount = 0;
            do {
                strReturnValues = this.sendCommand();
                if (strReturnValues[0].contains("Connection refused")
                        || (strReturnValues[0].contains("Read timed out"))) {
                    logger.debug("sendCommand is processing a retry for connection due to a refused connection {}",
                            nCount);
                    Thread.sleep(1000);
                } else {
                    logger.debug("The command was successfully sent {}, {}..", aCommand, strReturnValues[0]);
                    break;
                }
                nCount++;
            } while ((nCount < 10) && (((strReturnValues[0].contains("Read timed out"))
                    || (strReturnValues[0].contains("Connection refused")))));
            wSem.release();
        } catch (InterruptedException e) {
            logger.debug("run reporting exception {}", e.getMessage());
        } catch (IOException e) {
            logger.debug("run reporting exception {}", e.getMessage());
        }
    }

    private boolean initializeProjectorCommands(Socket socket, OutputStreamWriter out) throws IOException {

        try {
            String[] strResponse = new String[3];
            int[] nValue = new int[1];
            nValue[0] = "PJ_OK".length();
            strResponse = readReturnValue(socket, nValue);

            if (strResponse[0].contentEquals("PJ_OK")) {
                logger.debug("Projector responded to initial Socket Connection.. PJ_OK");
                char[] csend = new char[256];
                csend = "PJREQ".toCharArray();
                out.write(csend);
                out.flush();
                nValue[0] = "PJREQ".length();
                strResponse = readReturnValue(socket, nValue);
                if (strResponse[0].contentEquals("PJACK")) {
                    logger.debug("Projector responded to initial PJREQ with PJACK..");
                    return true;
                } else {
                    logger.trace("initializeProjectorCommands incorrect response received from Projector on PJREQ {}",
                            strResponse);
                }
            } else {
                logger.trace(
                        "initializeProjectorCommands incorrect response received from Projector on socket connect {}",
                        strResponse);
                return false;
            }
        } // end try
        catch (IOException e) {
            logger.debug("initialProjectorCommands returning exception {}", e.getMessage());
            throw new IOException(e.getMessage());
        }
        return false;
    }

    /**
     * Sends the command, and looks for a number of expected responses as defined by nExpectedResponses
     *
     * @bCommand an array of commands to send to target device
     * @nExpectedResponses an integer number of expected responses from commands
     * @return an array of string responses
     * @throws IOException exception in case device not reachable or an error occurred
     */
    public synchronized String[] sendCommand() throws IOException {
        String[] strResponse = new String[nExpectedBytes.length];

        try (Socket socket = createSocket();
                final OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream())) {
            // Send initializeProjectorCommands
            if (initializeProjectorCommands(socket, out)) {

                DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
                dout.write(aCommand);
                out.flush();
                strResponse = readReturnValue(socket, nExpectedBytes);
                socket.close();
                return strResponse;
            } // end if initializeprojectorcommands successful
            else {
                logger.debug("sendCommand failed to Initialize the projector..");

                socket.close();
                return strResponse;
            } // end initializeprojectorcommands NOT successful
        } catch (IOException e) {
            logger.debug("sendCommand returning an exception {}", e.getMessage());
            strResponse[0] = e.getMessage();
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Reads results from the projector.
     *
     * @param socket socket to read result from
     * @param nExpectedBytes that defines how many responses we should receive from the command
     * @return String array with results
     * @throws IOException exception in case device not reachable
     */
    private String[] readReturnValue(final Socket socket, int[] nExpectedBytes) throws IOException {
        String[] strArray = new String[nExpectedBytes.length];
        int nInterval = 0;
        try {
            InputStreamReader in = new InputStreamReader(socket.getInputStream());
            byte[] bt = new byte[32];
            int ret;
            char[] cbuf = new char[32];

            while (nInterval < nExpectedBytes.length) {
                ret = socket.getInputStream().read(bt, 0, nExpectedBytes[nInterval]);
                char cresp[] = new char[nExpectedBytes[nInterval] + 1];
                // Convert response to unsigned byte
                for (int n = 0; n < ret; n++) {
                    cresp[n] = (char) Byte.toUnsignedInt(bt[n]);
                }
                String strResponse = String.copyValueOf(cresp, 0, ret);
                strArray[nInterval] = strResponse;

                nInterval++;
            }

            return strArray;
        } catch (IOException e) {
            strArray[nInterval++] = e.getMessage();
            return (strArray);
        }
    }

    /**
     * Wrapper around socket creation to make mocking possible.
     *
     * @return new Socket instance
     * @throws UnknownHostException exception in case the host could not be determined
     * @throws IOException exception in case device not reachable
     */
    protected Socket createSocket() throws UnknownHostException, IOException {
        if (ipAddress == null) {
            throw new IOException("Ip address not set. Wait for discovery or manually trigger discovery process.");
        }
        final Socket socket = new Socket(ipAddress, JVC_PROJECTOR_PORT);
        socket.setSoTimeout(SOCKET_TIMEOUT_MILLISECONDS);
        return socket;

    }
}
