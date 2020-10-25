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
package org.openhab.binding.jvcprojector.internal.discovery;

import static org.openhab.binding.jvcprojector.internal.JVCProjectorBindingConstants.THING_TYPE_NX7;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link jvcprojectorDiscovery} is for handling discovery for the JVC Projector
 *
 * @author Brian Higginbotham - Initial contribution
 */

@Component(service = DiscoveryService.class, immediate = true, configurationPid = "discovery.jvcprojector")
public class jvcprojectorDiscovery extends AbstractDiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(jvcprojectorDiscovery.class);
    private ExecutorService executorService;
    private boolean scanning;
    private static final int TIMEOUT = 1000;

    /**
     * {@inheritDoc}
     *
     * Starts the scan. This discovery will:
     *
     */
    @Override
    protected void startScan() {

        if (executorService != null) {
            stopScan();
        }
        scanning = true;
        logger.debug("Starting Discovery jvcprojector 2");
    }

    /**
     * {@inheritDoc}
     *
     * Stops the discovery scan. We set {@link #scanning} to false (allowing the listening threads to end naturally
     * within {@link #TIMEOUT) * 5 time then shutdown the {@link #executorService}
     */
    @Override
    protected synchronized void stopScan() {
        super.stopScan();
        /*
         * if (executorService == null) {
         * return;
         * }
         *
         * scanning = false;
         *
         * try {
         * executorService.awaitTermination(TIMEOUT * 5, TimeUnit.MILLISECONDS);
         * } catch (InterruptedException e) {
         * }
         * executorService.shutdown();
         * executorService = null;
         */
    }

    /**
     * Constructs the discovery class using the thing IDs that we can discover.
     */
    public jvcprojectorDiscovery() {
        super(Collections.unmodifiableSet(Stream.of(THING_TYPE_NX7).collect(Collectors.toSet())), 30, false);
        logger.debug("jvcprojectorDiscovery constructor...");
    }
}
