package org.broadinstitute.sequel.entity.reagent;


import org.broadinstitute.sequel.entity.vessel.MolecularAppendage;

public interface DNAAppendage extends MolecularAppendage {

    /**
     * Does this belong up at MolecularEnvelope?
     * Do we thig of the role of the envelope
     * differently from its component 3' and 5'
     * ends?
     */
    public enum FunctionalRole {
        SEQUENCING_PRIMER,
        ADAPTOR,
        MOLECULAR_INDEX,
        AMPLIFICATION_PRIMER
    }

    public DNAAppendage.FunctionalRole getFunctionalRole();

    public String get3To5Sequence();

    public String get5To3Sequence();


}
