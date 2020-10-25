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

/**
 * The {@link JVCProjectorConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Brian Higginbotham - Initial contribution
 */
public class JVCProjectorConfiguration {

    /**
     * Sample configuration parameter. Replace with your own.
     */
    public String ipaddress;
    public String deviceid;

    /**
     * Refresh rate for the device in seconds.
     */
    public int refresh;
}
