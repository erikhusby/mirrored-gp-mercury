package org.broadinstitute.sequel.entity.labevent;

import org.broadinstitute.sequel.entity.reagent.Reagent;
import org.broadinstitute.sequel.entity.vessel.AbstractLabVessel;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.PlateWell;
import org.broadinstitute.sequel.entity.vessel.RackOfTubes;
import org.broadinstitute.sequel.entity.vessel.SBSSection;
import org.broadinstitute.sequel.entity.vessel.StaticPlate;
import org.broadinstitute.sequel.entity.vessel.VesselContainer;
import org.broadinstitute.sequel.entity.vessel.WellName;

import java.util.Collection;
import java.util.List;

/**
 * Represents a transfer between two sections.
 */
public class SectionTransfer {
    private VesselContainer sourceVesselContainer;
    private SBSSection sourceSection;
    private VesselContainer targetVesselContainer;
    private SBSSection targetSection;

    public SectionTransfer(VesselContainer sourceVesselContainer, SBSSection sourceSection,
            VesselContainer targetVesselContainer, SBSSection targetSection) {
        this.sourceVesselContainer = sourceVesselContainer;
        this.sourceSection = sourceSection;
        this.targetVesselContainer = targetVesselContainer;
        this.targetSection = targetSection;
    }

    public VesselContainer getSourceVesselContainer() {
        return this.sourceVesselContainer;
    }

    public void setSourceVesselContainer(VesselContainer sourceVesselContainer) {
        this.sourceVesselContainer = sourceVesselContainer;
    }

    public SBSSection getSourceSection() {
        return this.sourceSection;
    }

    public void setSourceSection(SBSSection sourceSection) {
        this.sourceSection = sourceSection;
    }

    public VesselContainer getTargetVesselContainer() {
        return this.targetVesselContainer;
    }

    public void setTargetVesselContainer(VesselContainer targetVesselContainer) {
        this.targetVesselContainer = targetVesselContainer;
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
            if (!this.sourceVesselContainer.getContainedVessels().isEmpty()) {
                LabVessel sourceWell = this.sourceVesselContainer.getVesselAtPosition(sourceWellName.getWellName());
                if (sourceWell != null) {
                    Collection<Reagent> reagents = sourceWell.getReagentContents();
                    // todo jmt is it necessary to copy the reagent into the target, or would it be better to navigate?
                    for (Reagent reagent : reagents) {
                        LabVessel plateWell = this.targetVesselContainer.getVesselAtPosition(targetWellName.getWellName());
                        if (plateWell == null) {
                            plateWell = new PlateWell((StaticPlate) this.targetVesselContainer, targetWellName);
                            this.targetVesselContainer.addContainedVessel(plateWell, targetWellName.getWellName());
                        }
                        plateWell.applyReagent(reagent);
                    }
                }
            }
        }
        // if source container is mutable, or sections are different: establish authorities at contained vessel level, based on section
        LabVessel sourceVessel = (LabVessel) this.sourceVesselContainer;
        // todo jmt, rather than checking for incoming transfers, check for other position maps?
        if((this.sourceVesselContainer instanceof RackOfTubes && !sourceVessel.getTransfersTo().isEmpty()) || this.sourceSection != this.targetSection) {
            List<WellName> positions = this.sourceSection.getWells();
            for (int wellIndex = 0; wellIndex < positions.size(); wellIndex++) {
                WellName sourceWellName = positions.get(wellIndex);
                WellName targetWellName = this.targetSection.getWells().get(wellIndex);
                if (this.sourceVesselContainer.getContainedVessels().isEmpty()) {
                    throw new RuntimeException("Vessel " + sourceVessel.getLabel()  + " has contained vessels");
                }
                LabVessel sourceContainedVessel = this.sourceVesselContainer.getVesselAtPosition(sourceWellName.getWellName());
                if (sourceContainedVessel != null) {
                    AbstractLabVessel targetContainedVessel = (AbstractLabVessel) this.targetVesselContainer.getVesselAtPosition(
                            targetWellName.getWellName());
                    if(targetContainedVessel != null) {
                        targetContainedVessel.getSampleSheetAuthorities().add(sourceContainedVessel);
                    }
                }
            }
        } else {
            if (((AbstractLabVessel) this.sourceVesselContainer).getSampleSheetAuthorities().isEmpty()) {
                if(sourceVessel.getReagentContents().isEmpty()) {
                    ((AbstractLabVessel)this.targetVesselContainer).getSampleSheetAuthorities().add(sourceVessel);
                }
            } else {
                ((AbstractLabVessel)this.targetVesselContainer).getSampleSheetAuthorities().addAll(
                        ((AbstractLabVessel) this.sourceVesselContainer).getSampleSheetAuthorities());
            }
        }

        // if destination is empty and source and destination sections are identical
        // else
        //   establish sample authorities at child vessel level
        // do re-arrays mess this up?  Look at Sage.  Primary should be positionMap, not rackOfTubes
        // if the source is a rack, a re-array is possible
    }
}
