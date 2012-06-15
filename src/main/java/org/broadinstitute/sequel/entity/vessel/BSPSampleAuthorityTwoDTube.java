package org.broadinstitute.sequel.entity.vessel;

import org.broadinstitute.sequel.boundary.Sample;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.StartingSample;

import java.util.Collection;
import java.util.Set;

/**
 * An aliquot from BSP, made on behalf of a particular
 * sample in a particular pass (see {@link org.broadinstitute.sequel.entity.bsp.BSPSample}).
 */
public class BSPSampleAuthorityTwoDTube extends TwoDBarcodedTube {

    private StartingSample aliquot;

    private Sample passSample;

    /**
     * When we receive a BSP export, we must know both what the originating
     * sample in the PASS was and the corresponding aliquot.  Here's
     * where we make that association.
     * @param passSample
     * @param aliquot
     */
    public BSPSampleAuthorityTwoDTube(Sample passSample,
                                      StartingSample aliquot) {
        super(aliquot.getLabel());
        this.passSample = passSample;
        this.aliquot = aliquot;
    }

    public Sample getPassSample() {
        return passSample;
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
