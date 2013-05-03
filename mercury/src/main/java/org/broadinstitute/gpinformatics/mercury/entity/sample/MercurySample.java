package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUtil;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.RapSheet;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

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
    @SuppressWarnings("UnusedDeclaration")
    private Long mercurySampleId;

    @Index(name = "ix_ms_sample_key")
    private String sampleKey;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    private RapSheet rapSheet;

    @Transient
    private BSPSampleDTO bspSampleDTO;

    @Transient
    private boolean hasBspDTOBeenInitialized;

    /**
     * For JPA
     */
    protected MercurySample() {
    }

    public MercurySample(String sampleKey) {
        this.sampleKey = sampleKey;
    }

    public MercurySample(String sampleKey, BSPSampleDTO bspSampleDTO) {
        this.sampleKey = sampleKey;
        this.bspSampleDTO = bspSampleDTO;
        hasBspDTOBeenInitialized = true;
    }

    public RapSheet getRapSheet() {
        if (rapSheet == null) {
            rapSheet = new RapSheet(this);
        }
        return rapSheet;
    }

    public void setRapSheet(RapSheet rapSheet) {
        this.rapSheet = rapSheet;
    }

    public String getSampleKey() {
        return sampleKey;
    }

    public boolean isInBspFormat() {
        return BSPUtil.isInBspFormat(sampleKey);
    }

    public BSPSampleDTO getBspSampleDTO() {
        if (!hasBspDTOBeenInitialized) {
            if (isInBspFormat()) {
                BSPSampleDataFetcher bspSampleDataFetcher = ServiceAccessUtility.getBean(BSPSampleDataFetcher.class);
                bspSampleDTO = bspSampleDataFetcher.fetchSingleSampleFromBSP(getSampleKey());
                if (bspSampleDTO == null) {
                    // No BSP sample exists with this name, but we still need a semblance of a BSP DTO.
                    bspSampleDTO = new BSPSampleDTO();
                }
            }

            hasBspDTOBeenInitialized = true;
        }

        return bspSampleDTO;
    }

    public void setBspSampleDTO(BSPSampleDTO bspSampleDTO) {
        hasBspDTOBeenInitialized = true;
        this.bspSampleDTO = bspSampleDTO;
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

        return new EqualsBuilder().append(getSampleKey(), that.getSampleKey()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getSampleKey()).toHashCode();
    }
}
