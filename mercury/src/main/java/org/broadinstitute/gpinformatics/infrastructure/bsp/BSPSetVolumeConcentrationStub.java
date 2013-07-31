package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

import javax.enterprise.inject.Alternative;

/**
 * Stubbed version of the set volume and concentration service.
 */
@Stub
@Alternative
public class BSPSetVolumeConcentrationStub implements BSPSetVolumeConcentration {

    private static final long serialVersionUID = -4537906882178920633L;

    private String[] result;

    @Override
    public void setVolumeAndConcentration(String barcode, double volume, double concentration) {
        result = new String[] { VALID_COMMUNICATION_PREFIX + " " + barcode };
    }

    @Override
    public String[] getResult() {
        return result;
    }

    @Override
    public boolean isValidResult() {
        return result[0].startsWith(BSPSetVolumeConcentration.VALID_COMMUNICATION_PREFIX);
    }
}
