package org.broadinstitute.gpinformatics.infrastructure.bsp;

import java.math.BigDecimal;

/**
 * Stubbed version of the set volume and concentration service.<br/>
 * (As of 12/8/2016, not used in alternatives so no CDI requirements)
 */
public class BSPSetVolumeConcentrationStub implements BSPSetVolumeConcentration {

    private static final long serialVersionUID = -4537906882178920633L;

    @Override
    public String setVolumeAndConcentration(String barcode, BigDecimal volume, BigDecimal concentration,
                                            BigDecimal receptacleWeight, TerminateAction terminateAction) {
        return RESULT_OK;
    }
}
