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
 * Represents a transfer from a tube to all positions (wells) in a (plate) section
 */
@Entity
@Audited
public class VesselToSectionTransfer extends VesselTransfer {

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
    private SBSSection targetSection;

    /** Typically a RackOfTubes. */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "ANCILLARY_TARGET_VESSEL")
    private LabVessel ancillaryTargetVessel;

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

    /**
     * Added to support Hibernate envers not properly instantiating VesselContainer.embedder. <br />
     * Preference should be getTargetVesselContainer()
     */
    public LabVessel getTargetVessel(){
        return targetVessel;
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
