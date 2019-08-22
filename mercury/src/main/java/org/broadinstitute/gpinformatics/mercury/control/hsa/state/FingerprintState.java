package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

@Entity
@Audited
public class FingerprintState extends State {

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST}, optional = false)
    private MercurySample mercurySample;

    public FingerprintState(MercurySample mercurySample) {
        this.mercurySample = mercurySample;
    }

    public FingerprintState() {
    }

    public MercurySample getMercurySample() {
        return mercurySample;
    }

    public void setMercurySample(MercurySample mercurySample) {
        this.mercurySample = mercurySample;
    }
}
