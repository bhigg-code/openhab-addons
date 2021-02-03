package org.openhab.binding.atlona.internal.opus;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.openhab.binding.atlona.internal.handler.AtlonaCapabilities;

/**
 * The capabilities class for the Atlona OPUS line. Each OPUS model differs in the number of (input) HDMI ports, the
 * number of (output) video and audio ports.
 *
 * @author Brian Higginbotham - Initial contribution
 * @author Tim Roberts - Code Adopted from initial
 */
public class AtlonaOpusCapabilities extends AtlonaCapabilities {
    /**
     * Number of output power ports
     */
    private final int nbrPowerPorts;

    /**
     * Number of audio ports
     */
    private final int nbrAudioPorts;

    /**
     * The set of output ports that are HDMI ports
     */
    private final Set<Integer> hdmiPorts;

    /**
     * Constructs the capabilities from the parms
     *
     * @param nbrPowerPorts a greater than 0 number of power ports
     * @param nbrAudioPorts a greater than 0 number of audio ports
     * @param hdmiPorts a non-null, non-empty set of hdmi ports
     */
    public AtlonaOpusCapabilities(int nbrPowerPorts, int nbrAudioPorts, Set<Integer> hdmiPorts) {
        super();

        if (nbrPowerPorts < 1) {
            throw new IllegalArgumentException("nbrPowerPorts must be greater than 0");
        }

        if (nbrAudioPorts < 1) {
            throw new IllegalArgumentException("nbrAudioPorts must be greater than 0");
        }

        if (hdmiPorts == null) {
            throw new IllegalArgumentException("hdmiPorts cannot be null");
        }

        if (hdmiPorts.isEmpty()) {
            throw new IllegalArgumentException("hdmiPorts cannot be empty");
        }

        this.nbrPowerPorts = nbrPowerPorts;
        this.nbrAudioPorts = nbrAudioPorts;
        this.hdmiPorts = Collections.unmodifiableSet(new HashSet<>(hdmiPorts));
    }

    /**
     * Returns the number of power ports
     *
     * @return a greater than 0 number of power ports
     */
    int getNbrPowerPorts() {
        return nbrPowerPorts;
    }

    /**
     * Returns the number of audio ports
     *
     * @return a greater than 0 number of audio ports
     */
    int getNbrAudioPorts() {
        return nbrAudioPorts;
    }

    /**
     * Returns the set of hdmi ports
     *
     * @return a non-null, non-empty immutable set of hdmi ports
     */
    Set<Integer> getHdmiPorts() {
        return hdmiPorts;
    }
}