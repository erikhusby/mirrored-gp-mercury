package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
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
 * Represents a transfer between two sections.
 */
@Entity
@Audited
@SuppressWarnings("rawtypes")
public class SectionTransfer extends VesselTransfer {

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
    private SBSSection sourceSection;

    /** Typically a RackOfTubes. */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "ANCILLARY_SOURCE_VESSEL")
    private LabVessel ancillarySourceVessel;

    @Enumerated(EnumType.STRING)
    private SBSSection targetSection;

    /** Typically a RackOfTubes. */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "ANCILLARY_TARGET_VESSEL")
    private LabVessel ancillaryTargetVessel;

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

    public VesselContainer<?> getTargetVesselContainer() {
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
