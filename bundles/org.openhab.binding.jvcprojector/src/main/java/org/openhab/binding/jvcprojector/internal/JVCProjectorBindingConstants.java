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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link JVCProjectorBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Brian Higginbotham - Initial contribution
 */
@NonNullByDefault
public class JVCProjectorBindingConstants {

    private static final String BINDING_ID = "jvcprojector";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_NX9 = new ThingTypeUID(BINDING_ID, "jvc-projector-nx9");
    public static final ThingTypeUID THING_TYPE_NX7 = new ThingTypeUID(BINDING_ID, "jvc-projector-nx7");
    public static final ThingTypeUID THING_TYPE_NX5 = new ThingTypeUID(BINDING_ID, "jvc-projector-nx5");
    public static final ThingTypeUID THING_TYPE_GENERIC = new ThingTypeUID(BINDING_ID, "jvc-projector-generic");

    // List of all Channel ids
    public static final String Power = "Power";
    public static final String InputHDMI = "InputHDMI";
    public static final String Model = "Model";
    public static final String LampHours = "LampHours";
    public static final String PictureMode = "PictureMode";

    public static byte[] bControllerValid = new byte[] { (byte) 0x21, (byte) 0x89, (byte) 0x01, (byte) 0x00,
            (byte) 0x00, (byte) 0x0a };
    public static byte[] bControllerValidSuccess = new byte[] { (byte) 0x06, (byte) 0x89, (byte) 0x01, (byte) 0x00,
            (byte) 0x00, (byte) 0x0a };
    public static byte[] bPowerOn = new byte[] { (byte) 0x21, (byte) 0x89, (byte) 0x01, (byte) 0x50, (byte) 0x57,
            (byte) 0x31, (byte) 0x0a };
    public static byte[] bPowerOnSuccess = new byte[] { (byte) 0x06, (byte) 0x89, (byte) 0x01, (byte) 0x50, (byte) 0x57,
            (byte) 0x0a };
    public static byte[] bPowerOff = new byte[] { (byte) 0x21, (byte) 0x89, (byte) 0x01, (byte) 0x50, (byte) 0x57,
            (byte) 0x30, (byte) 0x0a };
    public static byte[] bPowerOffSuccess = new byte[] { (byte) 0x06, (byte) 0x89, (byte) 0x01, (byte) 0x50,
            (byte) 0x57, (byte) 0x0a };
    public static byte[] bPowerStatusCheck = new byte[] { (byte) 0x3F, (byte) 0x89, (byte) 0x01, (byte) 0x50,
            (byte) 0x57, (byte) 0x0a };
    public static byte[] bPowerOnStatusAck = new byte[] { (byte) 0x06, (byte) 0x89, (byte) 0x01, (byte) 0x50,
            (byte) 0x57, (byte) 0x0a };
    public static byte[] bPowerOffResponse = new byte[] { (byte) 0x40, (byte) 0x89, (byte) 0x01, (byte) 0x50,
            (byte) 0x57, (byte) 0x30, (byte) 0x0a };
    public static byte[] bPowerOnResponse = new byte[] { (byte) 0x40, (byte) 0x89, (byte) 0x01, (byte) 0x50,
            (byte) 0x57, (byte) 0x31, (byte) 0x0a };
    public static byte[] bPowerResponseCooling = new byte[] { (byte) 0x40, (byte) 0x89, (byte) 0x01, (byte) 0x50,
            (byte) 0x57, (byte) 0x32, (byte) 0x0a };
    public static byte[] bPowerResponseReserved = new byte[] { (byte) 0x40, (byte) 0x89, (byte) 0x01, (byte) 0x50,
            (byte) 0x57, (byte) 0x33, (byte) 0x0a };
    public static byte[] bPowerResponseEmergency = new byte[] { (byte) 0x40, (byte) 0x89, (byte) 0x01, (byte) 0x50,
            (byte) 0x57, (byte) 0x34, (byte) 0x0a };
    public static byte[] bInputHDMI1 = new byte[] { (byte) 0x21, (byte) 0x89, (byte) 0x01, (byte) 0x49, (byte) 0x50,
            (byte) 0x36, (byte) 0x0a };
    public static byte[] bInputHDMI2 = new byte[] { (byte) 0x21, (byte) 0x89, (byte) 0x01, (byte) 0x49, (byte) 0x50,
            (byte) 0x37, (byte) 0x0a };
    public static byte[] bGetInputStatus = new byte[] { (byte) 0x3F, (byte) 0x89, (byte) 0x01, (byte) 0x49, (byte) 0x50,
            (byte) 0x0a };
    public static byte[] bInputSuccess = new byte[] { (byte) 0x06, (byte) 0x89, (byte) 0x01, (byte) 0x49, (byte) 0x50,
            (byte) 0x0a };
    public static byte[] bGetModelNumber = new byte[] { (byte) 0x3F, (byte) 0x89, (byte) 0x01, (byte) 0x4D, (byte) 0x44,
            (byte) 0x0A };
    public static byte[] bGetModelNumberSuccess = new byte[] { (byte) 0x06, (byte) 0x89, (byte) 0x01, (byte) 0x4D,
            (byte) 0x44, (byte) 0x0A };
    public static byte[] bGetSoftwareVersion = new byte[] { (byte) 0x3F, (byte) 0x89, (byte) 0x01, (byte) 0x49,
            (byte) 0x46, (byte) 0x53, (byte) 0x56, (byte) 0x0A };
    public static byte[] bGetSoftwareVersionSuccess = new byte[] { (byte) 0x06, (byte) 0x89, (byte) 0x01, (byte) 0x49,
            (byte) 0x46, (byte) 0x0A };
    public static byte[] bGetLampHours = new byte[] { (byte) 0x3F, (byte) 0x89, (byte) 0x01, (byte) 0x49, (byte) 0x46,
            (byte) 0x4C, (byte) 0x54, (byte) 0x0A };
    public static byte[] bGetLampHoursSuccess = new byte[] { (byte) 0x06, (byte) 0x89, (byte) 0x01, (byte) 0x49,
            (byte) 0x46, (byte) 0x0A };
    public static byte[] bPictureModeFilm = new byte[] { (byte) 0x21, (byte) 0x89, (byte) 0x01, (byte) 0x50,
            (byte) 0x4D, (byte) 0x50, (byte) 0x4D, (byte) 0x30, (byte) 0x30, (byte) 0x0a };
    public static byte[] bPictureModeCinema = new byte[] { (byte) 0x21, (byte) 0x89, (byte) 0x01, (byte) 0x50,
            (byte) 0x4D, (byte) 0x50, (byte) 0x4D, (byte) 0x30, (byte) 0x31, (byte) 0x0a };
    public static byte[] bPictureModeNatural = new byte[] { (byte) 0x21, (byte) 0x89, (byte) 0x01, (byte) 0x50,
            (byte) 0x4D, (byte) 0x50, (byte) 0x4D, (byte) 0x30, (byte) 0x33, (byte) 0x0a };
    public static byte[] bPictureModeHDR = new byte[] { (byte) 0x21, (byte) 0x89, (byte) 0x01, (byte) 0x50, (byte) 0x4D,
            (byte) 0x50, (byte) 0x4D, (byte) 0x30, (byte) 0x34, (byte) 0x0a };
    public static byte[] bPictureModeHDRAdapt = new byte[] { (byte) 0x21, (byte) 0x89, (byte) 0x01, (byte) 0x50,
            (byte) 0x4D, (byte) 0x50, (byte) 0x4D, (byte) 0x30, (byte) 0x42, (byte) 0x0a };
    public static byte[] bPictureModeTHX = new byte[] { (byte) 0x21, (byte) 0x89, (byte) 0x01, (byte) 0x50, (byte) 0x4D,
            (byte) 0x50, (byte) 0x4D, (byte) 0x30, (byte) 0x36, (byte) 0x0a };
    public static byte[] bPictureModeUser1 = new byte[] { (byte) 0x21, (byte) 0x89, (byte) 0x01, (byte) 0x50,
            (byte) 0x4D, (byte) 0x50, (byte) 0x4D, (byte) 0x30, (byte) 0x43, (byte) 0x0a };
    public static byte[] bPictureModeUser2 = new byte[] { (byte) 0x21, (byte) 0x89, (byte) 0x01, (byte) 0x50,
            (byte) 0x4D, (byte) 0x50, (byte) 0x4D, (byte) 0x30, (byte) 0x44, (byte) 0x0a };
    public static byte[] bPictureModeUser3 = new byte[] { (byte) 0x21, (byte) 0x89, (byte) 0x01, (byte) 0x50,
            (byte) 0x4D, (byte) 0x50, (byte) 0x4D, (byte) 0x30, (byte) 0x45, (byte) 0x0a };
    public static byte[] bPictureModeUser4 = new byte[] { (byte) 0x21, (byte) 0x89, (byte) 0x01, (byte) 0x50,
            (byte) 0x4D, (byte) 0x50, (byte) 0x4D, (byte) 0x30, (byte) 0x46, (byte) 0x0a };
    public static byte[] bPictureModeUser5 = new byte[] { (byte) 0x21, (byte) 0x89, (byte) 0x01, (byte) 0x50,
            (byte) 0x4D, (byte) 0x50, (byte) 0x4D, (byte) 0x31, (byte) 0x30, (byte) 0x0a };
    public static byte[] bPictureModeUser6 = new byte[] { (byte) 0x21, (byte) 0x89, (byte) 0x01, (byte) 0x50,
            (byte) 0x4D, (byte) 0x50, (byte) 0x4D, (byte) 0x31, (byte) 0x31, (byte) 0x0a };
    public static byte[] bPictureModeHLG = new byte[] { (byte) 0x21, (byte) 0x89, (byte) 0x01, (byte) 0x50, (byte) 0x4D,
            (byte) 0x50, (byte) 0x4D, (byte) 0x31, (byte) 0x34, (byte) 0x0a };
    public static byte[] bPictureModeSuccess = new byte[] { (byte) 0x06, (byte) 0x89, (byte) 0x01, (byte) 0x50,
            (byte) 0x4D, (byte) 0x0a };

}
