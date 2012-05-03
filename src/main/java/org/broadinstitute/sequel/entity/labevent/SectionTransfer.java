package org.broadinstitute.sequel.entity.labevent;

import org.broadinstitute.sequel.entity.reagent.Reagent;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.PlateWell;
import org.broadinstitute.sequel.entity.vessel.RackOfTubes;
import org.broadinstitute.sequel.entity.vessel.SBSSection;
import org.broadinstitute.sequel.entity.vessel.StaticPlate;
import org.broadinstitute.sequel.entity.vessel.VesselContainer;
import org.broadinstitute.sequel.entity.vessel.VesselContainerEmbedder;
import org.broadinstitute.sequel.entity.vessel.WellName;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import java.util.Collection;
import java.util.List;

/**
 * Represents a transfer between two sections.
 */
@Entity
@SuppressWarnings("rawtypes")
public class SectionTransfer {
    @Id
    @SequenceGenerator(name = "SEQ_SECTION_TRANSFER", sequenceName = "SEQ_SECTION_TRANSFER")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SECTION_TRANSFER")
    private Long sectionTransferId;

    @ManyToOne(fetch = FetchType.LAZY)
    private LabVessel sourceVessel;
    private SBSSection sourceSection;
    @ManyToOne(fetch = FetchType.LAZY)
    private LabVessel targetVessel;
    private SBSSection targetSection;
    @ManyToOne
    private AbstractLabEvent labEvent;

    public SectionTransfer(VesselContainer sourceVesselContainer, SBSSection sourceSection,
            VesselContainer targetVesselContainer, SBSSection targetSection) {
        this.sourceVessel = sourceVesselContainer.getEmbedder();
        this.sourceSection = sourceSection;
        this.targetVessel = targetVesselContainer.getEmbedder();
        this.targetSection = targetSection;
    }

    protected SectionTransfer() {
    }

    public VesselContainer getSourceVesselContainer() {
        return ((VesselContainerEmbedder)this.sourceVessel).getVesselContainer();
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
        return ((VesselContainerEmbedder) this.targetVessel).getVesselContainer();
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
        List<WellName> wells = this.sourceSection.getWells();
        for (int wellIndex = 0; wellIndex < wells.size(); wellIndex++) {
            WellName sourceWellName = wells.get(wellIndex);
            WellName targetWellName = this.targetSection.getWells().get(wellIndex);
            if (!this.getSourceVesselContainer().getContainedVessels().isEmpty()) {
                LabVessel sourceWell = this.getSourceVesselContainer().getVesselAtPosition(sourceWellName.getWellName());
                if (sourceWell != null) {
                    Collection<Reagent> reagents = sourceWell.getReagentContents();
                    // todo jmt is it necessary to copy the reagent into the target, or would it be better to navigate?
                    for (Reagent reagent : reagents) {
                        LabVessel plateWell = this.getTargetVesselContainer().getVesselAtPosition(targetWellName.getWellName());
                        if (plateWell == null) {
                            plateWell = new PlateWell((StaticPlate) this.getTargetVesselContainer().getEmbedder(), targetWellName);
                            this.getTargetVesselContainer().addContainedVessel(plateWell, targetWellName.getWellName());
                        }
                        plateWell.applyReagent(reagent);
                    }
                }
            }
        }
        // if source container is mutable, or sections are different: establish authorities at contained vessel level, based on section
        LabVessel sourceVessel = this.getSourceVesselContainer().getEmbedder();
        // todo jmt, rather than checking for incoming transfers, check for other position maps?
        if(this.getSourceVesselContainer().getEmbedder() instanceof RackOfTubes && !sourceVessel.getTransfersTo().isEmpty() ||
                this.sourceSection != this.targetSection) {
            List<WellName> positions = this.sourceSection.getWells();
            for (int wellIndex = 0; wellIndex < positions.size(); wellIndex++) {
                WellName sourceWellName = positions.get(wellIndex);
                WellName targetWellName = this.targetSection.getWells().get(wellIndex);
                if (this.getSourceVesselContainer().getContainedVessels().isEmpty()) {
                    throw new RuntimeException("Vessel " + sourceVessel.getLabel()  + " has contained vessels");
                }
                LabVessel sourceContainedVessel = this.getSourceVesselContainer().getVesselAtPosition(sourceWellName.getWellName());
                if (sourceContainedVessel != null) {
                    LabVessel targetContainedVessel = this.getTargetVesselContainer().getVesselAtPosition(
                            targetWellName.getWellName());
                }
            }
        }

        // if destination is empty and source and destination sections are identical
        // else
        //   establish sample authorities at child vessel level
        // do re-arrays mess this up?  Look at Sage.  Primary should be positionMap, not rackOfTubes
        // if the source is a rack, a re-array is possible
    }
}
