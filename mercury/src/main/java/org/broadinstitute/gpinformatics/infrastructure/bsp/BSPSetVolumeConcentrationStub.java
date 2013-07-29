package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.athena.entity.project.Cohort;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

import javax.enterprise.inject.Alternative;
import java.util.HashSet;
import java.util.Set;

/**
 * Stubbed version of the cohort service.
 */
@Stub
@Alternative
public class BSPSetVolumeConcentrationStub implements BSPSetVolumeConcentration {

    private static final long serialVersionUID = -4537906882178920633L;

    private String[] result;

    @Override
    public void setVolumeAndConcentration(String barcode, double volume, double concentration) {

        result = new String[0];
        result[0] = "updated volume and concentration for STUB-1234";
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
