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

import java.io.IOException;

import org.eclipse.smarthome.core.thing.Thing;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openhab.binding.nestdeviceaccess.internal.nesthelper.NestUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link NestThermostat} is responsible for handling all attributes and features of a Nest Thermostat
 *
 * @author Brian Higginbotham - Initial contribution
 */
public class NestThermostat {

    public NestThermostat(Thing thing) {
        if (thing != null) {
            this.thing = thing;
            nestUtility = new NestUtility(this.thing);
        }
    }

    Thing thing;
    NestUtility nestUtility;

    private final Logger logger = LoggerFactory.getLogger(NestThermostat.class);

    // Thermostat properties
    public String deviceName;
    public String deviceType;
    public String deviceCustomName;
    public int deviceHumidityPercent;
    public String deviceStatus;
    public String deviceFan;
    public String deviceCurrentThermostatMode;
    public String[] deviceAvailableThermostatModes;
    public String deviceThermostatEcoMode;
    public String[] deviceAvailableThermostatEcoModes;
    public double deviceCurrentThermostatEcoHeatCelsius;
    public double deviceCurrentThermostatEcoCoolCelsius;
    public double deviceCurrentThermostatHeatCelsius;
    public double deviceCurrentThermostatCoolCelsius;
    public String deviceThermostatHVACStatus;
    public String deviceTemperatureScaleSetting;
    public double deviceAmbientTemperatureSetting;
    public double deviceTargetTemperature; // settings used to aggregate target setting on heat/cool
    public double deviceMinTemperature; // settings used to aggregate setting for eco and heat-cool
    public double deviceMaxTemperature; // settings used to aggregate setting for eco and heat-cool
    public String[] deviceParentRelations;

    public boolean parseThermostatInfo(String jsonContent) {
        JSONObject jo = new JSONObject(jsonContent);

        deviceParentRelations = new String[2];
        deviceAvailableThermostatEcoModes = new String[2]; // only two known eco modes
        deviceAvailableThermostatModes = new String[4];

        deviceType = jo.getString("type");
        deviceCustomName = jo.getJSONObject("traits").getJSONObject("sdm.devices.traits.Info").getString("customName");
        deviceHumidityPercent = jo.getJSONObject("traits").getJSONObject("sdm.devices.traits.Humidity")
                .getInt("ambientHumidityPercent");
        deviceStatus = jo.getJSONObject("traits").getJSONObject("sdm.devices.traits.Connectivity").getString("status");
        deviceFan = jo.getJSONObject("traits").getJSONObject("sdm.devices.traits.Fan").getString("timerMode");
        deviceCurrentThermostatMode = jo.getJSONObject("traits").getJSONObject("sdm.devices.traits.ThermostatMode")
                .getString("mode");

        JSONArray jaAvailableModes = jo.getJSONObject("traits").getJSONObject("sdm.devices.traits.ThermostatMode")
                .getJSONArray("availableModes");

        for (int nCount = 0; nCount < jaAvailableModes.length(); nCount++) {
            // get Available Modes
            deviceAvailableThermostatModes[nCount] = jaAvailableModes.getString(nCount);
        }

        deviceThermostatEcoMode = jo.getJSONObject("traits").getJSONObject("sdm.devices.traits.ThermostatEco")
                .getString("mode");
        deviceCurrentThermostatEcoHeatCelsius = jo.getJSONObject("traits")
                .getJSONObject("sdm.devices.traits.ThermostatEco").getFloat("heatCelsius");
        deviceCurrentThermostatEcoCoolCelsius = jo.getJSONObject("traits")
                .getJSONObject("sdm.devices.traits.ThermostatEco").getFloat("coolCelsius");

        JSONArray jaAvailableEcoModes = jo.getJSONObject("traits").getJSONObject("sdm.devices.traits.ThermostatEco")
                .getJSONArray("availableModes");

        for (int nCount = 0; nCount < jaAvailableEcoModes.length(); nCount++) {
            // get Available Modes
            deviceAvailableThermostatEcoModes[nCount] = jaAvailableEcoModes.getString(nCount);
        }

        deviceThermostatHVACStatus = jo.getJSONObject("traits").getJSONObject("sdm.devices.traits.ThermostatHvac")
                .getString("status");

        deviceTemperatureScaleSetting = jo.getJSONObject("traits").getJSONObject("sdm.devices.traits.Settings")
                .getString("temperatureScale");

        if (jo.getJSONObject("traits").getJSONObject("sdm.devices.traits.ThermostatTemperatureSetpoint")
                .has("coolCelsius")) {
            deviceCurrentThermostatCoolCelsius = jo.getJSONObject("traits")
                    .getJSONObject("sdm.devices.traits.ThermostatTemperatureSetpoint").getFloat("coolCelsius");
        } // ThermostatMode = Off or Heater
        else {
            deviceCurrentThermostatCoolCelsius = 0;
        }
        if (jo.getJSONObject("traits").getJSONObject("sdm.devices.traits.ThermostatTemperatureSetpoint")
                .has("heatCelsius")) {
            deviceCurrentThermostatHeatCelsius = jo.getJSONObject("traits")
                    .getJSONObject("sdm.devices.traits.ThermostatTemperatureSetpoint").getFloat("heatCelsius");
        } // ThermostatMode = Off or AC
        else {
            deviceCurrentThermostatHeatCelsius = 0;
        }

        deviceAmbientTemperatureSetting = jo.getJSONObject("traits").getJSONObject("sdm.devices.traits.Temperature")
                .getFloat("ambientTemperatureCelsius");

        JSONArray jaParentRelations = jo.getJSONArray("parentRelations");

        for (int nCount = 0; nCount < jaParentRelations.length(); nCount++) {
            // get Available Modes
            deviceParentRelations[0] = jaParentRelations.getJSONObject(nCount).getString("parent");
            deviceParentRelations[1] = jaParentRelations.getJSONObject(nCount).getString("displayName");
            break;
        }
        deviceName = deviceParentRelations[1];

        // last thing is to aggregate temperature settings for ease of use
        if ((deviceCurrentThermostatMode.equalsIgnoreCase("HEAT"))
                && (!deviceThermostatEcoMode.equalsIgnoreCase("MANUAL_ECO"))) {
            deviceTargetTemperature = deviceCurrentThermostatHeatCelsius;
        } else if ((deviceCurrentThermostatMode.equalsIgnoreCase("COOL"))
                && (!deviceThermostatEcoMode.equalsIgnoreCase("MANUAL_ECO"))) {
            deviceTargetTemperature = deviceCurrentThermostatCoolCelsius;
        } else if (deviceThermostatEcoMode.equalsIgnoreCase("MANUAL_ECO")) {
            deviceMinTemperature = deviceCurrentThermostatEcoHeatCelsius;
            deviceMaxTemperature = deviceCurrentThermostatEcoCoolCelsius;
        } else if ((deviceCurrentThermostatMode.equalsIgnoreCase("HEAT-COOL"))
                && (!deviceThermostatEcoMode.equalsIgnoreCase("MANUAL_ECO"))) {
            deviceMinTemperature = deviceCurrentThermostatHeatCelsius;
            deviceMaxTemperature = deviceCurrentThermostatCoolCelsius;
        }

        return (true);
    }

    public boolean setThermostatMode(String setting) throws IOException {
        String jsonContent = "{\"command\" : \"sdm.devices.commands.ThermostatMode.SetMode\",\"params\" : {\"mode\" : \""
                + setting + "\"}}";

        try {
            String jsonResponse = nestUtility.deviceExecuteCommand(thing.getProperties().get("deviceId"),
                    thing.getProperties().get("projectId"), thing.getProperties().get("accessToken"), jsonContent);
            return (true);
        } catch (IOException e) {
            throw new IOException(e.getMessage());
        }
    }

    public boolean setThermostatEcoMode(String setting) throws IOException {
        String jsonContent = "{\"command\" : \"sdm.devices.commands.ThermostatEco.SetMode\",\"params\" : {\"mode\" : \""
                + setting + "\"}}";
        try {
            String jsonResponse = nestUtility.deviceExecuteCommand(thing.getProperties().get("deviceId"),
                    thing.getProperties().get("projectId"), thing.getProperties().get("accessToken"), jsonContent);
            return (true);
        } catch (IOException e) {
            throw new IOException(e.getMessage());
        }
    }

    public boolean setThermostatTargetTemperature(String setting) throws IOException {

        String jsonContent = "";
        double value = Double.parseDouble(setting);

        if (getTemperatureScaleSetting().equalsIgnoreCase("FAHRENHEIT")) {
            value = convertToCelsius(value);
        }

        if (getThermostatMode().equalsIgnoreCase("COOL")) {
            jsonContent = "{\"command\" : \"sdm.devices.commands.ThermostatTemperatureSetpoint.SetCool\",\"params\" : {\"coolCelsius\" : "
                    + String.valueOf(value) + "}}";
        } else if (getThermostatMode().equalsIgnoreCase("HEAT")) {
            jsonContent = "{\"command\" : \"sdm.devices.commands.ThermostatTemperatureSetpoint.SetHeat\",\"params\" : {\"heatCelsius\" : "
                    + String.valueOf(value) + "}}";
        } else {
            // INVALID use case for setThermostatTargetTemperature..
            return (false);
        }

        try {
            String jsonResponse = nestUtility.deviceExecuteCommand(thing.getProperties().get("deviceId"),
                    thing.getProperties().get("projectId"), thing.getProperties().get("accessToken"), jsonContent);
            return (true);
        } catch (IOException e) {
            throw new IOException(e.getMessage());
        }
    }

    public boolean getDevices() throws IOException {
        try {
            String url = "https://smartdevicemanagement.googleapis.com/v1/enterprises/"
                    + thing.getProperties().get("projectId") + "/devices";
            nestUtility.getDeviceInfo(thing.getProperties().get("accessToken"), url);
            return (true);
        } catch (IOException e) {
            throw new IOException(e.getMessage());
        }
    }

    public boolean initializeThermostat() throws IOException {

        return (getDevices());

    }

    public int getCurrentHumidity() {
        return (deviceHumidityPercent);
    }

    public String getDeviceName() {
        return (deviceName);
    }

    public String getCustomName() {
        return (deviceCustomName);
    }

    public String getDeviceStatus() {
        return (deviceStatus);
    }

    public String getDeviceFan() {
        return (deviceFan);
    }

    public String getThermostatMode() {
        return (deviceCurrentThermostatMode);
    }

    public String[] getAvailableThermostatModes() {
        return (deviceAvailableThermostatModes);
    }

    public String getThermostatEcoMode() {
        return (deviceThermostatEcoMode);
    }

    public String[] getAvailableThermostatEcoModes() {
        return (deviceAvailableThermostatEcoModes);
    }

    public double getCurrentThermostatEcoHeatCelsius() {
        if (getTemperatureScaleSetting().equalsIgnoreCase("Fahrenheit")) {
            return (convertToFahrenheit(deviceCurrentThermostatEcoHeatCelsius));
        } else {
            return (deviceCurrentThermostatEcoHeatCelsius);
        }
    }

    public double getCurrentThermostatEcoCoolCelsius() {
        if (getTemperatureScaleSetting().equalsIgnoreCase("Fahrenheit")) {
            return (convertToFahrenheit(deviceCurrentThermostatEcoCoolCelsius));
        } else {
            return (deviceCurrentThermostatEcoCoolCelsius);
        }
    }

    public String getThermostatHVACStatus() {
        return (deviceThermostatHVACStatus);
    }

    public String getTemperatureScaleSetting() {
        return (deviceTemperatureScaleSetting);
    }

    public double getAmbientTemperatureSetting() {
        if (getTemperatureScaleSetting().equalsIgnoreCase("Fahrenheit")) {
            return (convertToFahrenheit(deviceAmbientTemperatureSetting));
        } else {
            return (deviceAmbientTemperatureSetting);
        }
    }

    public double getCurrentTemperatureHeat() {
        if (getTemperatureScaleSetting().equalsIgnoreCase("Fahrenheit")) {
            return (convertToFahrenheit(deviceCurrentThermostatHeatCelsius));
        } else {
            return (deviceCurrentThermostatHeatCelsius);
        }
    }

    public double getCurrentTemperatureCool() {
        if (getTemperatureScaleSetting().equalsIgnoreCase("Fahrenheit")) {
            return (convertToFahrenheit(deviceCurrentThermostatCoolCelsius));
        } else {
            return (deviceCurrentThermostatCoolCelsius);
        }
    }

    public double getTargetTemperature() {
        if (getTemperatureScaleSetting().equalsIgnoreCase("Fahrenheit")) {
            return (convertToFahrenheit(deviceTargetTemperature));
        } else {
            return (deviceTargetTemperature);
        }

    }

    public double[] getMinMaxTemperature() {
        double[] minMaxValue = new double[2];
        if (getTemperatureScaleSetting().equalsIgnoreCase("Fahrenheit")) {

            minMaxValue[0] = convertToFahrenheit(deviceMinTemperature);
            minMaxValue[1] = convertToFahrenheit(deviceMaxTemperature);
            return (minMaxValue);
        } else {
            minMaxValue[0] = deviceMinTemperature;
            minMaxValue[1] = deviceMaxTemperature;
            return (minMaxValue);
        }
    }

    private double convertToFahrenheit(double temperature) {
        return (((temperature / 5) * 9) + 32);
    }

    private double convertToCelsius(double temperature) {
        return ((temperature - 32) * 5 / 9);
    }

    public boolean getThermostatInfo() throws IOException {
        try {
            String jsonContent;
            jsonContent = nestUtility.getDeviceInfo(thing.getProperties().get("accessToken"),
                    "https://smartdevicemanagement.googleapis.com/v1/enterprises/"
                            + thing.getProperties().get("projectId") + "/devices/"
                            + thing.getProperties().get("deviceId"));
            JSONObject jo = new JSONObject(jsonContent);

            return (parseThermostatInfo(jsonContent));

        } catch (IOException e) {
            throw new IOException(e.getMessage());
        }
    }

}
