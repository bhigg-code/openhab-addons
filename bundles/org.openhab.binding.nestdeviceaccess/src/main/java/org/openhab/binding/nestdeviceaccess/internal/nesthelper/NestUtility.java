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
package org.openhab.binding.nestdeviceaccess.internal.nesthelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.smarthome.core.thing.Thing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.auth.oauth2.AuthorizationCodeTokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.common.net.HttpHeaders;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;

/**
 * The {@link NestUtility} is general utility class to help with all Nest SDM APIs
 *
 * @author Brian Higginbotham - Initial contribution
 */
public class NestUtility {

    public NestUtility(Thing thing) {
        if (thing != null) {
            this.thing = thing;
            deviceId = this.thing.getProperties().get("deviceId");
            clientId = this.thing.getProperties().get("clientId");
            clientSecret = this.thing.getProperties().get("clientSecret");
            projectId = this.thing.getProperties().get("projectId");
            refreshToken = this.thing.getProperties().get("refreshToken");
            accessToken = this.thing.getProperties().get("accessToken");
        }
    }

    public NestUtility(String projectId, String clientId, String clientSecret, String refreshToken,
            String accessToken) {
        // secondary constructor for null thing and discovery service/non-basehandlers
        if (thing == null) {
            this.projectId = projectId;
            this.clientId = clientId;
            this.clientSecret = clientSecret;
            this.refreshToken = refreshToken;
            this.accessToken = accessToken;
        }
    }

    Thing thing;

    private final static Logger logger = LoggerFactory.getLogger(NestUtility.class);

    private String deviceId;
    private String clientId;
    private String clientSecret;
    private String projectId;
    private String refreshToken;
    private String accessToken;

    // helper local vars. Will set global properies file

    public String deviceExecuteCommand(String deviceId, String projectId, String accessToken, String requestBody)
            throws IOException {

        try {
            HttpTransport transport = new NetHttpTransport();
            HttpRequest request = transport.createRequestFactory().buildPostRequest(
                    new GenericUrl("https://smartdevicemanagement.googleapis.com/v1/enterprises/" + projectId
                            + "/devices/" + deviceId + ":executeCommand"),
                    ByteArrayContent.fromString("application/json", requestBody));
            request.getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json");
            request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

            HttpResponse response = request.execute();
            return (convertStreamtoString(response.getContent()));
        } catch (IOException e) {
            int statusCode = Integer.parseInt(e.getMessage().substring(0, 3));
            if (statusCode == 401) {
                // get access token refreshed
                logger.debug("deviceExecuteCommand reporting access token is expired..");
                accessToken = refreshAccessToken(refreshToken, clientId, clientSecret);
                logger.debug("deviceExecuteCommand reporting access token refresh successful..");
                return (deviceExecuteCommand(deviceId, projectId, accessToken, requestBody)); // returns error code in
                                                                                              // string
                // format. Did this to
                // commoditize the
                // return value for successful JSON response
            }
            throw new IOException(e.getMessage());
        }

    }

    public String getDeviceInfo(String accessToken, String url) throws IOException {
        try {
            HttpTransport transport = new NetHttpTransport();
            HttpRequest request = transport.createRequestFactory().buildGetRequest(new GenericUrl(url));

            request.getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json");
            request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            HttpResponse response = request.execute();

            return (convertStreamtoString(response.getContent()));
        } catch (IOException e) {
            int statusCode = Integer.parseInt(e.getMessage().substring(0, 3));
            if (statusCode == 401) {
                // get access token refreshed
                logger.debug("deviceGetInfo reporting access token is expired..");
                accessToken = refreshAccessToken(refreshToken, clientId, clientSecret);
                logger.debug("deviceGetInfo reporting access token refresh successful..");

                return (getDeviceInfo(accessToken, url)); // returns error code in string
                                                          // format. Did this to
                // commoditize the
                // return value for successful JSON response
            }
            throw new IOException(e.getMessage()); // never should get here unless there is a problem
        }
    }

    public String refreshAccessToken(String refreshToken, String clientId, String clientSecret) throws IOException {
        try {
            TokenResponse response = new GoogleRefreshTokenRequest(new NetHttpTransport(), new JacksonFactory(),
                    refreshToken, clientId, clientSecret).execute();
            logger.info("Access Token: {}", response.getAccessToken());
            logger.info("Refresh Token Lifespan: {}", response.getExpiresInSeconds());
            accessToken = response.getAccessToken();

            if (thing != null) {
                thing.setProperty("accessTokenExpiresIn", response.getExpiresInSeconds().toString());
                thing.setProperty("accessToken", accessToken);
            }
            return (accessToken);
        } catch (TokenResponseException e) {
            logger.debug("refreshAccessToken threw an exception {}", e.getDetails().getError());
            if (e.getDetails().getErrorDescription() != null) {
                logger.debug("refreshAccessToken threw further description {}", e.getDetails().getErrorDescription());
            }
            throw new IOException(e.getMessage());
        }
    }

    public String[] requestAccessToken(String clientId, String clientSecret, String authorizationToken)
            throws IOException {
        try {
            String[] tokens = new String[2];
            TokenResponse response = new AuthorizationCodeTokenRequest(new NetHttpTransport(), new JacksonFactory(),
                    new GenericUrl("https://www.googleapis.com/oauth2/v4/token"), authorizationToken)
                            .setRedirectUri("https://www.google.com")
                            .setClientAuthentication(new BasicAuthentication(clientId, clientSecret)).execute();
            logger.info("Access Token: {}", response.getAccessToken());
            logger.info("Refresh Token: {}", response.getRefreshToken());
            logger.info("Refresh Token Lifespan: {}", response.getExpiresInSeconds());
            accessToken = response.getAccessToken();
            refreshToken = response.getRefreshToken();
            tokens[0] = accessToken;
            tokens[1] = refreshToken;
            if (thing != null) {
                thing.setProperty("accessToken", response.getAccessToken());
                thing.setProperty("refreshToken", response.getRefreshToken());
                thing.setProperty("accessTokenExpiresIn", response.getExpiresInSeconds().toString());
            }
            return (tokens);
        } catch (TokenResponseException e) {
            throw new IOException(e.getMessage());
        }
    }

    /* Took function from online stackoverflow article... credit: Zapnologica */
    private String convertStreamtoString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = "";
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            is.close();
            return sb.toString();
        } catch (IOException e) {
            throw new IOException(e.getMessage());
        }

    }

    public String getDevices(String projectId, String accessToken) throws IOException {

        try {
            HttpTransport transport = new NetHttpTransport();
            HttpRequest request = transport.createRequestFactory().buildGetRequest(new GenericUrl(
                    "https://smartdevicemanagement.googleapis.com/v1/enterprises/" + projectId + "/devices"));

            request.getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json");
            request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            HttpResponse response = request.execute();

            return (convertStreamtoString(response.getContent()));

        } catch (IOException e) {
            logger.debug("getDevices returning exception {}", e.getMessage());
            return ((e.getMessage().substring(0, 3)));
        }

    }

    public static void pubSubEventHandler(String projectId, String subscriptionId) {
        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId);

        // Instantiate an asynchronous message receiver.
        MessageReceiver receiver = (PubsubMessage message, AckReplyConsumer consumer) -> {
            // Handle incoming message, then ack the received message.
            logger.debug("Id: {}", message.getMessageId());
            logger.debug("Data: {}", message.getData().toStringUtf8());

            consumer.ack();
        };

        Subscriber subscriber = null;
        try {
            logger.debug("We are starting up the sub... subName {}", subscriptionName);
            subscriber = Subscriber.newBuilder(subscriptionName, receiver).build();
            // Start the subscriber.
            logger.debug("Got the subscriber.. {}", subscriber.getSubscriptionNameString());
            subscriber.startAsync().awaitRunning();
            logger.debug("Listening for messages on {}", subscriptionName.toString());
            // Allow the subscriber to run for 30s unless an unrecoverable error occurs.
            subscriber.awaitTerminated(30, TimeUnit.SECONDS);
        } catch (IllegalStateException e) {
            logger.debug("illegal state exception {}", e.getMessage());

        } catch (TimeoutException timeoutException) {
            // Shut down the subscriber after 30s. Stop receiving messages.
            logger.debug("Timedout exception {}", timeoutException.getMessage());
            subscriber.stopAsync();
        }
    }

    /*
     * public int getStructures(String projectId, String accessToken) throws IOException {
     *
     * try {
     * HttpTransport transport = new NetHttpTransport();
     * HttpRequest request = transport.createRequestFactory().buildGetRequest(new GenericUrl(
     * "https://smartdevicemanagement.googleapis.com/v1/enterprises/" + projectId + "/structures"));
     *
     * request.getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json");
     * request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
     * HttpResponse response = request.execute();
     *
     * JSONObject jo = new JSONObject(convertStreamtoString(response.getContent()));
     * JSONArray ja = jo.getJSONArray("structures");
     * // allocate structures array
     * structureId = new String[ja.length()];
     * structureName = new String[ja.length()];
     *
     * for (int i = 0; i < ja.length(); i++) {
     * structureName[i] = ja.getJSONObject(i).getJSONObject("traits")
     * .getJSONObject("sdm.structures.traits.Info").getString("customName");
     * String temp = ja.getJSONObject(i).getString("name");
     * structureId[i] = temp.substring(temp.lastIndexOf("/") + 1, temp.length());
     * logger.debug("getStructures reporting id {} and name {}", structureId[i], structureName[i]);
     * }
     *
     * response.getContent().close();
     * return (response.getStatusCode());
     * } catch (IOException e) {
     * logger.debug("getStructures returning exception {}", e.getMessage());
     * return (Integer.parseInt((e.getMessage().substring(0, 3))));
     * }
     * }
     */
}
