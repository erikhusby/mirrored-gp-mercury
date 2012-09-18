package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.sample.StartingSample;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import java.util.Set;

/**
 * An aliquot made on behalf of a particular starter.
 */
@Entity
@Audited
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
        aliquot.setBspSampleAuthorityTwoDTube(this);
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

    public StartingSample getAliquot() {
        return aliquot;
    }
}
