package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.annotation.Nullable;
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
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private LabEvent labEvent;

    public SectionTransfer(VesselContainer sourceVesselContainer, SBSSection sourceSection, LabVessel ancillarySourceVessel,
            VesselContainer targetVesselContainer, SBSSection targetSection, LabVessel ancillaryTargetVessel, LabEvent labEvent) {
        this.ancillarySourceVessel = ancillarySourceVessel;
        this.ancillaryTargetVessel = ancillaryTargetVessel;
        this.labEvent = labEvent;
        sourceVessel = sourceVesselContainer.getEmbedder();
        sourceVesselContainer.getSectionTransfersFrom().add(this);
        this.sourceSection = sourceSection;
        targetVessel = targetVesselContainer.getEmbedder();
        targetVesselContainer.getSectionTransfersTo().add(this);
        // In case we're adding molecular indexes, clear the cached sample instances.
        targetVesselContainer.clearCaches();
        this.targetSection = targetSection;
        if (sourceSection.getWells().size() != targetSection.getWells().size()) {
            throw new RuntimeException("For transfer to " + targetVessel.getLabel() + ", " +
                    sourceSection.getSectionName() + " is not the same size as " + targetSection.getSectionName());
        }
    }

    protected SectionTransfer() {
    }

    public VesselContainer getSourceVesselContainer() {
        return sourceVessel.getContainerRole();
    }

    public void setSourceVesselContainer(VesselContainer sourceVesselContainer) {
        sourceVessel = sourceVesselContainer.getEmbedder();
    }

    public SBSSection getSourceSection() {
        return sourceSection;
    }

    public void setSourceSection(SBSSection sourceSection) {
        this.sourceSection = sourceSection;
    }

    @Nullable
    public LabVessel getAncillarySourceVessel() {
        return ancillarySourceVessel;
    }

    public VesselContainer getTargetVesselContainer() {
        return targetVessel.getContainerRole();
    }

    public void setTargetVesselContainer(VesselContainer targetVesselContainer) {
        targetVessel = targetVesselContainer.getEmbedder();
    }

    public SBSSection getTargetSection() {
        return targetSection;
    }

    public void setTargetSection(SBSSection targetSection) {
        this.targetSection = targetSection;
    }

    @Nullable
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
        return getVesselTransferId() + "|" + sourceVessel.getLabel() + "|" + sourceSection.getSectionName() + "|" +
               targetVessel.getLabel() + "|" + targetSection.getSectionName();
    }
}
