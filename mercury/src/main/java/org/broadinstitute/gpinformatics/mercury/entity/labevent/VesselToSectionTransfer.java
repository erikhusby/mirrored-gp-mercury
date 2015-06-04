package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.annotation.Nullable;
import javax.persistence.*;

/**
 * Represents a transfer from a tube to all positions (wells) in a (plate) section
 */
@Entity
@Audited
public class VesselToSectionTransfer extends VesselTransfer {

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private LabVessel sourceVessel;

    @Enumerated(EnumType.STRING)
    private SBSSection targetSection;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private LabVessel targetVessel;

    /** Typically a RackOfTubes. */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private LabVessel ancillaryTargetVessel;

    @Index(name = "ix_vtst_lab_event")
    @ManyToOne
    private LabEvent labEvent;

    public VesselToSectionTransfer(LabVessel sourceVessel, SBSSection targetSection,
            VesselContainer<?> targetVesselContainer, LabVessel ancillaryTargetVessel, LabEvent labEvent) {
        this.sourceVessel = sourceVessel;
        this.targetSection = targetSection;
        this.ancillaryTargetVessel = ancillaryTargetVessel;
        this.labEvent = labEvent;
        this.targetVessel = targetVesselContainer.getEmbedder();
        sourceVessel.getVesselToSectionTransfersThisAsSource().add(this);
        targetVessel.getContainerRole().getVesselToSectionTransfersTo().add(this);
    }

    protected VesselToSectionTransfer() {
    }

    public LabVessel getSourceVessel() {
        return sourceVessel;
    }

    /** For fixups only */
    void setSourceVessel(LabVessel sourceVessel) {
        this.sourceVessel = sourceVessel;
    }

    public SBSSection getTargetSection() {
        return targetSection;
    }

    public VesselContainer<?> getTargetVesselContainer() {
        return targetVessel.getContainerRole();
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
        return sourceVessel.getLabel() + "|" + targetVessel.getLabel() + "|" +
               targetSection.getSectionName();
    }

}
