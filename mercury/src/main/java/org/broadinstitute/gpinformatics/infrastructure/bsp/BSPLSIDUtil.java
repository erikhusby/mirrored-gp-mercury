package org.broadinstitute.gpinformatics.infrastructure.bsp;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Some utilities for working with BSP LSIDs.
 */
public class BSPLSIDUtil {

    public static final String BSP_LSID_FRAGMENT = ":bsp.";

    public static boolean isBspLsid(String id) {
        return id.contains(BSP_LSID_FRAGMENT);
    }

    public static String lsidToBareId(String lsid) {
        return lsid.substring(lsid.lastIndexOf(':') + 1);
    }


    /**
     * Return a mapping from each LSID in the input collection to a bare Sample ID
     * (ID without an SM- or SP- prefix) suitable for feeding to the runSampleSearch method
     *
     * @param lsids LSIDs to map
     *
     * @return mapping from LSIDs to samples
     */
    public static Map<String, String> lsidsToBareIds(Collection<String> lsids) {
        Map<String, String> ret = new HashMap<>();

        for (String lsid : lsids) {
            ret.put(lsid, lsidToBareId(lsid));
        }

        return ret;
    }

}
