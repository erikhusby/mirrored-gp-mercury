package org.broadinstitute.gpinformatics.mercury.entity.labevent;

/**
 * When something goes wrong in the lab,
 * we fail it.  DNA quant out of range.
 * Flowcell density to high/low.
 */
public interface Failure {
    
    public enum FailureType {
        TOO_DENSE,
        TOO_SPARSE,
        QUANT_TOO_HIGH,
        QUANT_TOO_LOW,
        NOT_ENOUGH_VOLUME,
        INSERT_TOO_LARGE,
        INSERT_TOO_SMALL
    }

    /**
     * The person who makes the failure
     * diagnosis may have something to say
     * about it.
     * @return
     */
    public String getTechnicianComments();

}
