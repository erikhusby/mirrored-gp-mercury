package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainerEmbedder;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Represents a transfer between two sections.
 */
@Entity
@Audited
@Table(schema = "mercury")
@SuppressWarnings("rawtypes")
public class SectionTransfer extends VesselTransfer {

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private LabVessel sourceVessel;

    @Enumerated(EnumType.STRING)
    private SBSSection sourceSection;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private LabVessel targetVessel;

    @Enumerated(EnumType.STRING)
    private SBSSection targetSection;

    @Index(name = "ix_st_lab_event")
    @ManyToOne(fetch = FetchType.LAZY)
    private LabEvent labEvent;

    public SectionTransfer(VesselContainer sourceVesselContainer, SBSSection sourceSection,
            VesselContainer targetVesselContainer, SBSSection targetSection, LabEvent labEvent) {
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

    public void applyTransfer() {
        // todo jmt remove this method?
/*
        List<VesselPosition> wells = this.sourceSection.getWells();
        for (int wellIndex = 0; wellIndex < wells.size(); wellIndex++) {
            VesselPosition sourceVesselPosition = wells.get(wellIndex);
            VesselPosition targetVesselPosition = this.targetSection.getWells().get(wellIndex);
            if (!this.getSourceVesselContainer().getContainedVessels().isEmpty()) {
                LabVessel sourceWell = this.getSourceVesselContainer().getVesselAtPosition(sourceVesselPosition.toString());
                if (sourceWell != null) {
                    Collection<Reagent> reagents = sourceWell.getReagentContents();
                    // todo jmt is it necessary to copy the reagent into the target, or would it be better to navigate?
                    for (Reagent reagent : reagents) {
                        LabVessel plateWell = this.getTargetVesselContainer().getVesselAtPosition(targetVesselPosition.toString());
                        if (plateWell == null) {
                            plateWell = new PlateWell((StaticPlate) this.getTargetVesselContainer().getEmbedder(), targetVesselPosition);
                            this.getTargetVesselContainer().addContainedVessel(plateWell, targetVesselPosition.toString());
                        }
                        plateWell.applyReagent(reagent);
                    }
                }
            }
        }
*/
/*
        LabVessel sourceVessel = this.getSourceVesselContainer().getEmbedder();
        // todo jmt, rather than checking for incoming transfers, check for other position maps?
        if(Hibernate.getClass(this.getSourceVesselContainer().getEmbedder()).equals(TubeFormation.class) && !sourceVessel.getTransfersTo().isEmpty() ||
                this.sourceSection != this.targetSection) {
            List<VesselPosition> positions = this.sourceSection.getWells();
            for (int wellIndex = 0; wellIndex < positions.size(); wellIndex++) {
                VesselPosition sourceVesselPosition = positions.get(wellIndex);
                VesselPosition targetVesselPosition = this.targetSection.getWells().get(wellIndex);
                if (this.getSourceVesselContainer().getContainedVessels().isEmpty()) {
                    throw new RuntimeException("Vessel " + sourceVessel.getLabel()  + " has contained vessels");
                }
                LabVessel sourceContainedVessel = this.getSourceVesselContainer().getVesselAtPosition(sourceVesselPosition.toString());
                if (sourceContainedVessel != null) {
                    LabVessel targetContainedVessel = this.getTargetVesselContainer().getVesselAtPosition(
                            targetVesselPosition.toString());
                }
            }
        }
*/
    }

    public LabEvent getLabEvent() {
        return labEvent;
    }
}
