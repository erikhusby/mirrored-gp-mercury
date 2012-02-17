package org.broadinstitute.sequel.entity.labevent;

import org.broadinstitute.sequel.entity.vessel.MolecularState;
import org.broadinstitute.sequel.entity.vessel.MolecularState;

/**
 * Properties common to all events of a particular message type
 */
public class LabEventType {
    private final boolean expectedEmptySources;

    private final boolean expectedEmptyTargets;

    private final MolecularState.DNA_OR_RNA nucleicAcidType;

    private final MolecularState.STRANDEDNESS targetStrand;

    /**
     * One attempt at trying to make a very generic
     * {@link LabEvent} to handle lots of different
     * {@link LabEventName event names}
     * @param expectSourcesEmpty
     * @param expectTargetsEmpty
     * @param targetStrand if null, inherit the same {@link MolecularState.STRANDEDNESS strand}
     *                     from the {@link LabEvent#getSourceLabVessels()}
     * @param nucleicAcid if null, inherit the same {@link MolecularState.DNA_OR_RNA nucleic acid}
     *                     from the {@link LabEvent#getSourceLabVessels() sources}
     */
    public LabEventType(boolean expectSourcesEmpty,
            boolean expectTargetsEmpty,
            MolecularState.STRANDEDNESS targetStrand,
            MolecularState.DNA_OR_RNA nucleicAcid) {
        this.expectedEmptySources = expectSourcesEmpty;
        this.expectedEmptyTargets = expectTargetsEmpty;
        this.nucleicAcidType = nucleicAcid;
        this.targetStrand = targetStrand;
    }

    public boolean isExpectedEmptySources() {
        return expectedEmptySources;
    }

    public boolean isExpectedEmptyTargets() {
        return expectedEmptyTargets;
    }

    public MolecularState.DNA_OR_RNA getNucleicAcidType() {
        return nucleicAcidType;
    }

    public MolecularState.STRANDEDNESS getTargetStrand() {
        return targetStrand;
    }
}
