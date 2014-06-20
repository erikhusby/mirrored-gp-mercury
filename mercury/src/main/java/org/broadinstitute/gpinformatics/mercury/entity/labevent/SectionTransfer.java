package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.persistence.*;

/**
 * Represents a transfer between two sections.
 */
@Entity
@Audited
@SuppressWarnings("rawtypes")
public class SectionTransfer extends VesselTransfer {

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private LabVessel sourceVessel;

    @Enumerated(EnumType.STRING)
    private SBSSection sourceSection;

    /** Typically a RackOfTubes. */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private LabVessel ancillarySourceVessel;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private LabVessel targetVessel;

    @Enumerated(EnumType.STRING)
    private SBSSection targetSection;

    /** Typically a RackOfTubes. */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private LabVessel ancillaryTargetVessel;

    @Index(name = "ix_st_lab_event")
    @ManyToOne(fetch = FetchType.LAZY)
    private LabEvent labEvent;

    public SectionTransfer(VesselContainer sourceVesselContainer, SBSSection sourceSection, LabVessel ancillarySourceVessel,
            VesselContainer targetVesselContainer, SBSSection targetSection, LabVessel ancillaryTargetVessel, LabEvent labEvent) {
        this.ancillarySourceVessel = ancillarySourceVessel;
        this.ancillaryTargetVessel = ancillaryTargetVessel;
        this.labEvent = labEvent;
        this.sourceVessel = sourceVesselContainer.getEmbedder();
        sourceVesselContainer.getSectionTransfersFrom().add(this);
        this.sourceSection = sourceSection;
        this.targetVessel = targetVesselContainer.getEmbedder();
        targetVesselContainer.getSectionTransfersTo().add(this);
        this.targetSection = targetSection;
    }

    protected SectionTransfer() {
    }

    public VesselContainer getSourceVesselContainer() {
        return this.sourceVessel.getContainerRole();
    }

    public void setSourceVesselContainer(VesselContainer sourceVesselContainer) {
        this.sourceVessel = sourceVesselContainer.getEmbedder();
    }

    public SBSSection getSourceSection() {
        return this.sourceSection;
    }

    public void setSourceSection(SBSSection sourceSection) {
        this.sourceSection = sourceSection;
    }

    public LabVessel getAncillarySourceVessel() {
        return ancillarySourceVessel;
    }

    public VesselContainer getTargetVesselContainer() {
        return this.targetVessel.getContainerRole();
    }

    public void setTargetVesselContainer(VesselContainer targetVesselContainer) {
        this.targetVessel = targetVesselContainer.getEmbedder();
    }

    public SBSSection getTargetSection() {
        return this.targetSection;
    }

    public void setTargetSection(SBSSection targetSection) {
        this.targetSection = targetSection;
    }

    public LabVessel getAncillaryTargetVessel() {
        return ancillaryTargetVessel;
    }

    public LabEvent getLabEvent() {
        return labEvent;
    }

    /**
     * Constructs a String that is likely to be unique for this transfer
     * @return concatenation of critical fields
     */
    public String getKey() {
        // todo jmt this hides duplicates
        return sourceVessel.getLabel() + "|" + sourceSection.getSectionName() + "|" + targetVessel.getLabel() + "|" +
                targetSection.getSectionName();
    }
}
