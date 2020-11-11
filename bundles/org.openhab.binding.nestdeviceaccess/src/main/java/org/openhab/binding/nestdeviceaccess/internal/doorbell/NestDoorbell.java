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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.eclipse.smarthome.core.library.types.RawType;
import org.eclipse.smarthome.core.thing.Thing;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openhab.binding.nestdeviceaccess.internal.nesthelper.NestUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.common.net.HttpHeaders;

/**
 * The {@link NestDoorbell} is responsible for handling all attributes and features of a Nest Doorbell
 *
 * @author Brian Higginbotham - Initial contribution
 */
public class NestDoorbell {

    public NestDoorbell(Thing thing) {
        if (thing != null) {
            this.thing = thing;
            nestUtility = new NestUtility(this.thing);
        }
    }

    public Thing thing;
    public NestUtility nestUtility;

    private final Logger logger = LoggerFactory.getLogger(NestDoorbell.class);

    // Thermostat properties
    public String deviceName;
    public String deviceType;
    public String deviceCustomName;
    public String deviceStatus;
    public Date cameraSoundEventTime;
    private boolean cameraSoundEvent;
    private Date cameraMotionEventTime;
    private boolean cameraMotionEvent;
    private Date cameraPersonEventTime;
    private boolean cameraPersonEvent;
    private Date cameraChimeEventTime;
    private boolean cameraChimeEvent;
    private String streamUrl;
    private String streamToken;
    private Date streamExpiresAt;
    private String streamExtensionToken;
    public int[] deviceMaxImageResolution;
    public int[] deviceMaxVideoResolution;
    public String[] deviceVideoResolution;
    public String[] deviceParentRelations;
    public String[] deviceVideoCodecs;
    public String[] deviceAudioCodecs;
    public String[] deviceSupportedProtocols;

    public boolean parseDoorbellInfo(String jsonContent) {
        JSONObject jo = new JSONObject(jsonContent);

        deviceParentRelations = new String[2];
        deviceType = jo.getString("type");

        if (jo.getJSONObject("traits").getJSONObject("sdm.devices.traits.Info").has("customName")) {
            deviceCustomName = jo.getJSONObject("traits").getJSONObject("sdm.devices.traits.Info")
                    .getString("customName");
        }

        if (jo.getJSONObject("traits").getJSONObject("sdm.devices.traits.CameraLiveStream").has("maxVideoResolution")) {
            deviceMaxVideoResolution = new int[2];

            deviceMaxVideoResolution[0] = jo.getJSONObject("traits")
                    .getJSONObject("sdm.devices.traits.CameraLiveStream").getJSONObject("maxVideoResolution")
                    .getInt("width");
            deviceMaxVideoResolution[1] = jo.getJSONObject("traits")
                    .getJSONObject("sdm.devices.traits.CameraLiveStream").getJSONObject("maxVideoResolution")
                    .getInt("height");
        }

        if (jo.getJSONObject("traits").getJSONObject("sdm.devices.traits.CameraLiveStream").has("videoCodecs")) {
            JSONArray jaVideoCodecs = jo.getJSONObject("traits").getJSONObject("sdm.devices.traits.CameraLiveStream")
                    .getJSONArray("videoCodecs");
            deviceVideoCodecs = new String[jaVideoCodecs.length()];
            for (int nCount = 0; nCount < jaVideoCodecs.length(); nCount++) {
                deviceVideoCodecs[nCount] = jaVideoCodecs.getString(nCount);
            }
        }

        if (jo.getJSONObject("traits").getJSONObject("sdm.devices.traits.CameraLiveStream").has("audioCodecs")) {
            JSONArray jaAudioCodecs = jo.getJSONObject("traits").getJSONObject("sdm.devices.traits.CameraLiveStream")
                    .getJSONArray("audioCodecs");
            deviceAudioCodecs = new String[jaAudioCodecs.length()];
            for (int nCount = 0; nCount < jaAudioCodecs.length(); nCount++) {
                deviceVideoCodecs[nCount] = jaAudioCodecs.getString(nCount);
            }
        }

        if (jo.getJSONObject("traits").getJSONObject("sdm.devices.traits.CameraLiveStream").has("supportedProtocols")) {
            JSONArray jaSupportedProtocols = jo.getJSONObject("traits")
                    .getJSONObject("sdm.devices.traits.CameraLiveStream").getJSONArray("supportedProtocols");
            deviceSupportedProtocols = new String[jaSupportedProtocols.length()];
            for (int nCount = 0; nCount < jaSupportedProtocols.length(); nCount++) {
                deviceVideoCodecs[nCount] = jaSupportedProtocols.getString(nCount);
            }
        }

        if (jo.getJSONObject("traits").getJSONObject("sdm.devices.traits.CameraLiveStream").has("maxImageResolution")) {
            deviceMaxImageResolution = new int[2];
            deviceMaxImageResolution[0] = jo.getJSONObject("traits")
                    .getJSONObject("sdm.devices.traits.CameraLiveStream").getJSONObject("maxImageResolution")
                    .getInt("width");
            deviceMaxImageResolution[1] = jo.getJSONObject("traits")
                    .getJSONObject("sdm.devices.traits.CameraLiveStream").getJSONObject("maxImageResolution")
                    .getInt("height");
        }

        JSONArray jaParentRelations = jo.getJSONArray("parentRelations");

        for (int nCount = 0; nCount < jaParentRelations.length(); nCount++) {
            // get Available Modes
            deviceParentRelations[0] = jaParentRelations.getJSONObject(nCount).getString("parent");
            deviceParentRelations[1] = jaParentRelations.getJSONObject(nCount).getString("displayName");
            break;
        }
        deviceName = deviceParentRelations[1];

        return (true);
    }

    public String getDevices() throws IOException {
        try {
            return (nestUtility.getDevices());
        } catch (IOException e) {
            throw new IOException(e.getMessage());
        }
    }

    public boolean initializeDoorbell() throws IOException {
        return (getDoorbellInfo());
    }

    public boolean setCameraSoundEvent(Date value) {
        cameraSoundEventTime = value;
        cameraSoundEvent = true;
        return (true);
    }

    public boolean getCameraSoundEvent() {
        return (cameraSoundEvent);
    }

    public Date getCameraSoundEventTime() {
        return (cameraSoundEventTime);
    }

    public boolean setCameraPersonEvent(Date value) {
        cameraPersonEventTime = value;
        cameraPersonEvent = true;
        return (true);
    }

    public boolean getCameraPersonEvent() {
        return (cameraPersonEvent);
    }

    public Date getCameraPersonEventTime() {
        return (cameraPersonEventTime);
    }

    public boolean setCameraMotionEvent(Date value) {
        cameraMotionEventTime = value;
        cameraMotionEvent = true;
        return (true);
    }

    public boolean getCameraMotionEvent() {
        return (cameraMotionEvent);
    }

    public Date getCameraMotionEventTime() {
        return (cameraMotionEventTime);
    }

    public boolean setCameraChimeEvent(Date value) {
        cameraChimeEventTime = value;
        cameraChimeEvent = true;
        return (true);
    }

    public boolean getCameraChimeEvent() {
        return (cameraChimeEvent);
    }

    public Date getCameraChimeEventTime() {
        return (cameraChimeEventTime);
    }

    public int[] getMaxImageResolution() {
        return (deviceMaxImageResolution);
    }

    public String[] getVideoCodecs() {
        return (deviceVideoCodecs);
    }

    public String[] getAudioCodecs() {
        return (deviceAudioCodecs);
    }

    public String[] getSupportedProtocols() {
        return (deviceSupportedProtocols);
    }

    public int[] getMaxVideoResolution() {
        return (deviceMaxVideoResolution);
    }

    public String getDeviceName() {
        return (deviceName);
    }

    public String getCustomName() {
        return (deviceCustomName);
    }

    public String getStreamUrl() {
        return (streamUrl);
    }

    public String getStreamToken() {
        return (streamToken);
    }

    public Date getStreamTokenExpiration() {
        return (streamExpiresAt);
    }

    public String getStreamExensionToken() {
        return (streamExtensionToken);
    }

    public boolean isImageValid(Date messageTime) {
        // check if image is stale
        Date currentTime = new Date();
        long diffMilliseconds = currentTime.getTime() - messageTime.getTime();
        long diffSeconds = (diffMilliseconds / 1000) % 60;

        if (diffSeconds <= 30) {
            return (true);
        } else {
            return (false);
        }
    }

    public boolean getCameraLiveStream() throws IOException {
        String requestContent = "{\"command\" : \"sdm.devices.commands.CameraLiveStream.GenerateRtspStream\",\"params\" : {}}";
        try {
            String jsonContent = nestUtility.deviceExecuteCommand(thing.getProperties().get("deviceId"),
                    thing.getProperties().get("projectId"), nestUtility.getAccessToken().getTokenValue(),
                    requestContent);
            JSONObject jo = new JSONObject(jsonContent);
            streamUrl = jo.getJSONObject("results").getJSONObject("streamUrls").getString("rtspUrl");
            streamToken = jo.getJSONObject("results").getString("streamToken");
            DateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            streamExpiresAt = utcFormat.parse(jo.getJSONObject("results").getString("expiresAt"));
            streamExtensionToken = jo.getJSONObject("results").getString("streamExtensionToken");

            return (true);
        } catch (IOException e) {
            throw new IOException(e.getMessage());
        } catch (JSONException e) {
            throw new IOException(e.getMessage());
        } catch (ParseException e) {
            throw new IOException(e.getMessage());
        }
    }

    public RawType getCameraImage(String eventId) throws IOException {
        String requestContent = "{\"command\" : \"sdm.devices.commands.CameraEventImage.GenerateImage\",\"params\" : {\"eventId\" : \""
                + eventId + "\"}}";
        try {
            String jsonContent = nestUtility.deviceExecuteCommand(thing.getProperties().get("deviceId"),
                    thing.getProperties().get("projectId"), nestUtility.getAccessToken().getTokenValue(),
                    requestContent);
            JSONObject jo = new JSONObject(jsonContent);
            String url = jo.getJSONObject("results").getString("url");
            String token = jo.getJSONObject("results").getString("token");
            // fetch image
            HttpTransport transport = new NetHttpTransport();
            HttpRequest request = transport.createRequestFactory().buildGetRequest(new GenericUrl(url + "?width=480"));
            request.getHeaders().set(HttpHeaders.CONTENT_TYPE, "image/jpeg");
            request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Basic " + token);
            HttpResponse response = request.execute();

            ByteArrayOutputStream byteImage = new ByteArrayOutputStream();
            response.download(byteImage);

            RawType image = new RawType(byteImage.toByteArray(), "image/jpeg");
            byteImage.close();
            return (image);
        } catch (IOException e) {
            throw new IOException(e.getMessage());
        } catch (Exception e) {
            // general exception
            throw new IOException(e.getMessage());
        }
    }

    public boolean getDoorbellInfo() throws IOException {
        try {
            String jsonContent;
            jsonContent = nestUtility.getDeviceInfo("https://smartdevicemanagement.googleapis.com/v1/enterprises/"
                    + thing.getProperties().get("projectId") + "/devices/" + thing.getProperties().get("deviceId"));
            return (parseDoorbellInfo(jsonContent));

        } catch (IOException e) {
            throw new IOException(e.getMessage());
        }
    }

}
