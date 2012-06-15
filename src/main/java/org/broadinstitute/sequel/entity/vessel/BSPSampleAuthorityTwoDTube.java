package org.broadinstitute.sequel.entity.vessel;

import org.broadinstitute.sequel.boundary.Sample;
import org.broadinstitute.sequel.entity.project.Starter;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.StartingSample;

import java.util.Collection;
import java.util.Set;

/**
 * An aliquot made on behalf of a particular starter.
 */
public class BSPSampleAuthorityTwoDTube extends TwoDBarcodedTube {

    private StartingSample aliquot;


    /**
     * When we receive a BSP export, we must know both what the originating
     * sample in the PASS was and the corresponding aliquot.  Here's
     * where we make that association.
     * @param aliquot
     */
    public BSPSampleAuthorityTwoDTube(StartingSample aliquot) {
        super(aliquot.getLabel());
        this.aliquot = aliquot;
    }

    @Override
    public Set<SampleInstance> getSampleInstances() {
        return aliquot.getSampleInstances();
    }

    @Override
    public boolean isSampleAuthority() {
        return true;
    }
}
