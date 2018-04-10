package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.hibernate.envers.Audited;

import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * Represents a transfer between positions in two vessel containers
 * todo jmt if the JAXB references multiple source racks, but transfers out of only one, there is no association between lab vessel and lab event.
 */
@Entity
@Audited
public class CherryPickTransfer extends VesselTransfer {

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "SOURCE_VESSEL")
    private LabVessel sourceVessel;


    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "TARGET_VESSEL")
    private LabVessel targetVessel;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "LAB_EVENT")
    private LabEvent labEvent;

    @Enumerated(EnumType.STRING)
    private VesselPosition sourcePosition;

    /** Typically a RackOfTubes. */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "ANCILLARY_SOURCE_VESSEL")
    private LabVessel ancillarySourceVessel;

    @Enumerated(EnumType.STRING)
    private VesselPosition targetPosition;

    /** Typically a RackOfTubes. */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "ANCILLARY_TARGET_VESSEL")
    private LabVessel ancillaryTargetVessel;

    public CherryPickTransfer(
            VesselContainer<?> sourceVesselContainer,
            VesselPosition sourcePosition,
            @Nullable LabVessel ancillarySourceVessel,
            VesselContainer<?> targetVesselContainer,
            VesselPosition targetPosition,
            @Nullable LabVessel ancillaryTargetVessel,
            LabEvent labEvent) {
        if (sourceVesselContainer == null) {
            throw new RuntimeException("sourceVesselContainer must not be null");
        }
        if (sourcePosition == null) {
            throw new RuntimeException("sourcePosition must not be null");
        }
        this.ancillarySourceVessel = ancillarySourceVessel;
        if (targetVesselContainer == null) {
            throw new RuntimeException("targetVesselContainer must not be null");
        }
        if (targetPosition == null) {
            throw new RuntimeException("targetPosition must not be null");
        }
        this.ancillaryTargetVessel = ancillaryTargetVessel;
        this.labEvent = labEvent;
        sourceVessel = sourceVesselContainer.getEmbedder();
        sourceVesselContainer.getCherryPickTransfersFrom().add(this);
        this.sourcePosition = sourcePosition;
        targetVessel = targetVesselContainer.getEmbedder();
        targetVesselContainer.getCherryPickTransfersTo().add(this);
        this.targetPosition = targetPosition;
    }

    protected CherryPickTransfer() {
    }

    /**
     * Added to support Hibernate envers not properly instantiating VesselContainer.embedder. <br />
     * Preference should be getSourceVesselContainer()
     */
    public LabVessel getSourceVessel(){
        return sourceVessel;
    }

    /**
     * Added to support Hibernate envers not properly instantiating VesselContainer.embedder. <br />
     * Preference should be getTargetVesselContainer()
     */
    public LabVessel getTargetVessel(){
        return targetVessel;
    }

    public VesselContainer<?> getSourceVesselContainer() {
        return sourceVessel.getContainerRole();
    }

    public VesselPosition getSourcePosition() {
        return sourcePosition;
    }

    /**
     * For fixups only.
     */
    void setSourcePosition(VesselPosition sourcePosition) {
        this.sourcePosition = sourcePosition;
    }

    @Nullable
    public LabVessel getAncillarySourceVessel() {
        return ancillarySourceVessel;
    }

    public VesselContainer<?> getTargetVesselContainer() {
        return targetVessel.getContainerRole();
    }

    public VesselPosition getTargetPosition() {
        return targetPosition;
    }

    /**
     * For fixups only.
     */
    void setTargetPosition(VesselPosition targetPosition) {
        this.targetPosition = targetPosition;
    }

    @Nullable
    public LabVessel getAncillaryTargetVessel() {
        return ancillaryTargetVessel;
    }

    public LabEvent getLabEvent() {
        return labEvent;
    }

    /**
     * Constructs a string this is likely to be unique for this transfer
     * @return concatenation of key fields
     */
    public String getKey() {
        return getVesselTransferId() + "|" + sourceVessel.getLabel() + "|" + sourcePosition + "|" +
               targetVessel.getLabel() + "|" + targetPosition;
    }

    void clearLabEvent() {
        labEvent = null;
    }
}
