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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link JVCProjectorHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Brian Higginbotham - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.jvcprojector", service = ThingHandlerFactory.class)
public class JVCProjectorHandlerFactory extends BaseThingHandlerFactory {

    private final Logger logger = LoggerFactory.getLogger(JVCProjectorHandlerFactory.class);
    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.unmodifiableSet(
            Stream.of(THING_TYPE_NX5, THING_TYPE_NX7, THING_TYPE_NX9, THING_TYPE_GENERIC).collect(Collectors.toSet()));

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        logger.debug("Start supportThing {}...", thingTypeUID);
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);

    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        logger.debug("createHandler.. ConfigIP {}", thing.getConfiguration().get("IPAddress"));
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        // todo.. Assign custom JVC projector instances

        if (THING_TYPE_GENERIC.equals(thingTypeUID)) {
            return new JVCProjectorHandler(thing);
        }

        return null;
    }
}
