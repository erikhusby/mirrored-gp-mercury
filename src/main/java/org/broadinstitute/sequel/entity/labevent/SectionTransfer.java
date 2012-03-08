package org.broadinstitute.sequel.entity.labevent;

import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.SBSSection;

/**
 * Represents a transfer between two sections.
 */
public class SectionTransfer {
    private LabVessel sourceVessel;
    private SBSSection sourceSection;
    private LabVessel targetVessel;
    private SBSSection targetSection;

    public SectionTransfer(LabVessel sourceVessel, SBSSection sourceSection, LabVessel targetVessel, SBSSection targetSection) {
        this.sourceVessel = sourceVessel;
        this.sourceSection = sourceSection;
        this.targetVessel = targetVessel;
        this.targetSection = targetSection;
    }

    public LabVessel getSourceVessel() {
        return this.sourceVessel;
    }

    public void setSourceVessel(LabVessel sourceVessel) {
        this.sourceVessel = sourceVessel;
    }

    public SBSSection getSourceSection() {
        return this.sourceSection;
    }

    public void setSourceSection(SBSSection sourceSection) {
        this.sourceSection = sourceSection;
    }

    public LabVessel getTargetVessel() {
        return this.targetVessel;
    }

    public void setTargetVessel(LabVessel targetVessel) {
        this.targetVessel = targetVessel;
    }

    public SBSSection getTargetSection() {
        return this.targetSection;
    }

    public void setTargetSection(SBSSection targetSection) {
        this.targetSection = targetSection;
    }
}
