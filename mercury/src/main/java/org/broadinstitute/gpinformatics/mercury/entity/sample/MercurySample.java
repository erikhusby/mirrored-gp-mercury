package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.mercury.entity.rework.RapSheet;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.persistence.*;

/**
 * Represents Mercury's view of a sample.  Sample information is held in another system (initially Athena),
 * this entity just holds a key to that system's representation.
 */
@Entity
@Audited
@Table(schema = "mercury")
public class MercurySample {

    @Id
    @SequenceGenerator(name = "SEQ_MERCURY_SAMPLE", schema = "mercury", sequenceName = "SEQ_MERCURY_SAMPLE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MERCURY_SAMPLE")
    private Long mercurySampleId;

    private String productOrderKey;

    @Index(name = "ix_ms_sample_key")
    private String sampleKey;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private RapSheet rapSheet;

    @Transient
    private BSPSampleDTO bspSampleDTO;


    public MercurySample(String productOrderKey, String sampleKey) {
        this.productOrderKey = productOrderKey;
        this.sampleKey = sampleKey;
    }


    public MercurySample(String productOrderKey, String sampleKey, BSPSampleDTO bspSampleDTO) {
        this.productOrderKey = productOrderKey;
        this.sampleKey = sampleKey;
        this.bspSampleDTO = bspSampleDTO;
    }

    public RapSheet getRapSheet() {
        if (rapSheet == null) {
            rapSheet = new RapSheet();
        }
        return rapSheet;
    }

    public void setRapSheet(RapSheet rapSheet) {
        this.rapSheet = rapSheet;
    }

    /**
     * For JPA
     */
    MercurySample() {
    }

    public String getProductOrderKey() {
        return productOrderKey;
    }

    public String getSampleKey() {
        return sampleKey;
    }

    public BSPSampleDTO getBspSampleDTO() {
        return bspSampleDTO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof MercurySample)) {
            return false;
        }

        MercurySample that = (MercurySample) o;

        return new EqualsBuilder().append(getProductOrderKey(), that.getProductOrderKey()).
                append(getSampleKey(), that.getSampleKey()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getProductOrderKey()).append(getSampleKey()).toHashCode();
    }
}
