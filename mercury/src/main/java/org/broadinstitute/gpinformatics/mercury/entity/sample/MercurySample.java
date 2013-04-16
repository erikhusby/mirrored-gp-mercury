package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.*;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.util.Arrays;
import java.util.regex.Pattern;

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

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private RapSheet rapSheet;

    @Transient
    private BSPSampleDTO bspSampleDTO;

    @Transient
    private boolean hasBspDTOBeenInitialized;

    public static final Pattern BSP_SAMPLE_NAME_PATTERN = Pattern.compile("SM-[A-Z1-9]{4,6}");

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

    public ReworkEntry reworkSample(ReworkReason reworkReason, ReworkLevel reworkLevel, LabEvent labEvent,
                                    LabEventType reworkStep, LabVessel labVessel, VesselPosition vesselPosition,
                                    String comment) {
        final ReworkEntry reworkEntry =
                getRapSheet().addRework(reworkReason, reworkLevel, reworkStep, vesselPosition, this);
        LabVesselComment reworkComment =
                new LabVesselComment<ReworkEntry>(labEvent, labVessel, comment, Arrays.asList(reworkEntry));
        reworkEntry.setLabVesselComment(reworkComment);
        return reworkEntry;
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

    public String getSampleKey() {
        return sampleKey;
    }

    public boolean isInBspFormat() {
        return isInBspFormat(sampleKey);
    }

    public static boolean isInBspFormat(@Nonnull String sampleName) {
        return BSP_SAMPLE_NAME_PATTERN.matcher(sampleName).matches();
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
