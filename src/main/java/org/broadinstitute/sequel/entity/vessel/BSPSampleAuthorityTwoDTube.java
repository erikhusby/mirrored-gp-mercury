package org.broadinstitute.sequel.entity.vessel;

import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.StartingSample;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import java.util.Set;

/**
 * An aliquot made on behalf of a particular starter.
 */
@Entity
public class BSPSampleAuthorityTwoDTube extends TwoDBarcodedTube {

    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
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

    protected BSPSampleAuthorityTwoDTube() {
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
