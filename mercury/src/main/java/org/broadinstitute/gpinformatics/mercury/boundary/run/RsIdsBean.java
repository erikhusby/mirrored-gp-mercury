package org.broadinstitute.gpinformatics.mercury.boundary.run;

import java.util.List;

/**
 * JAXB DTO to return list of RSIDs in a fingerprint
 */
public class RsIdsBean {
    private List<String> rsids;

    /** For JAXB. */
    public RsIdsBean() {
    }

    public RsIdsBean(List<String> rsids) {
        this.rsids = rsids;
    }

    public List<String> getRsids() {
        return rsids;
    }
}
