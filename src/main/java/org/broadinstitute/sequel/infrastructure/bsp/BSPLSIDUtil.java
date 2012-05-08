package org.broadinstitute.sequel.infrastructure.bsp;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Some utilities for parsing lsids
 */
public class BSPLSIDUtil {

    /**
     * Return a mapping from each LSID in the input collection to a bare Sample ID
     * (ID without an SM- or SP- prefix) suitable for feeding to the runSampleSearch method
     *
     * @param lsids
     *
     * @return mapping from LSIDs to samples
     *
     */
    public static Map<String, String> lsidsToBareIds(Collection<String> lsids) {
        Map<String, String> ret = new HashMap<String, String>();

        for (String lsid : lsids) {
            String [] chunks = lsid.split(":");
            ret.put(lsid, chunks[chunks.length-1]);
        }

        return ret;

    }
}
