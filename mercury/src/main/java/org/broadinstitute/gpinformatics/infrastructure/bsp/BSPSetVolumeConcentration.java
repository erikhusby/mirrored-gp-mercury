package org.broadinstitute.gpinformatics.infrastructure.bsp;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * This interface provides the methods needed to set volume and concentration on a sample, but barcode. This is so
 * that we can test with STUBs and have the real implementation for the container.
 */
public interface BSPSetVolumeConcentration extends Serializable {
    // value for a successful call to the web service
    String RESULT_OK = "OK";
    // The success message from BSP starts with this.
    String VALID_COMMUNICATION_PREFIX = "updated volume and concentration for";

    /**
     * The actual code to set the volume and concentration.
     *
     * @param barcode           The barcode for the sample being set.
     * @param volume            The volume value to set.
     * @param concentration     The concentration value to set.
     * @param terminateDepleted Whether to terminate the sample if it is depleted.
     *
     * @return OK or error message
     */
    String setVolumeAndConcentration(String barcode, BigDecimal volume, BigDecimal concentration,
                                     BigDecimal receptacleWeight, Boolean terminateDepleted);
}
