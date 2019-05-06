package org.broadinstitute.gpinformatics.mercury.control.hsa.state;

import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Audited
@Table(schema = "mercury")
public class AlignmentState extends State {

    @ManyToOne
    @JoinColumn(name = "MERCURY_SAMPLE")
    private MercurySample mercurySample;

    public AlignmentState() {
    }

    public AlignmentState(String name, MercurySample mercurySample) {
        super(name);
        this.mercurySample = mercurySample;
    }

    public MercurySample getMercurySample() {
        return mercurySample;
    }

    public void setMercurySample(MercurySample mercurySample) {
        this.mercurySample = mercurySample;
    }
}
