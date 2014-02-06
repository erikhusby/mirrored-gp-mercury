package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

import javax.enterprise.inject.Alternative;
import java.math.BigDecimal;

/**
 * Stubbed version of the set volume and concentration service.
 */
@Stub
@Alternative
public class BSPSetVolumeConcentrationStub implements BSPSetVolumeConcentration {

    private static final long serialVersionUID = -4537906882178920633L;

    @Override
    public String setVolumeAndConcentration(String barcode, BigDecimal volume, BigDecimal concentration,
            BigDecimal receptacleWeight) {
        return RESULT_OK;
    }
}
