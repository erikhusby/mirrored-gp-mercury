package org.broadinstitute.gpinformatics.mercury.boundary.run;

/**
 * JAXB DTO to return list of RSIDs in a fingerprint
 */
public class RsIdsBean {
    private String[] rsids;

    /** For JAXB. */
    public RsIdsBean() {
    }

    public RsIdsBean(String[] rsids) {
        this.rsids = rsids;
    }

    public String[] getRsids() {
        return rsids;
    }
}
