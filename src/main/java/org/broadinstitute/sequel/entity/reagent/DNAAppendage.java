package org.broadinstitute.sequel.entity.reagent;


import org.broadinstitute.sequel.entity.run.SequencingTechnology;
import org.broadinstitute.sequel.entity.vessel.MolecularAppendage;

public class DNAAppendage implements MolecularAppendage {
    
    private String appendageName;
    private SequencingTechnology sequencingTechnology;
    private FunctionalRole functionalRole;
    private String threeToFiveSequence;
    private String fiveToThreeSequence;

    public DNAAppendage(String appendageName, SequencingTechnology sequencingTechnology, FunctionalRole functionalRole,
            String threeToFiveSequence, String fiveToThreeSequence) {
        this.appendageName = appendageName;
        this.sequencingTechnology = sequencingTechnology;
        this.functionalRole = functionalRole;
        this.threeToFiveSequence = threeToFiveSequence;
        this.fiveToThreeSequence = fiveToThreeSequence;
    }

    @Override
    public String getAppendageName() {
        return appendageName;
    }

    @Override
    public SequencingTechnology getSequencingTechnology() {
        return sequencingTechnology;
    }

    /**
     * Does this belong up at MolecularEnvelope?
     * Do we think of the role of the envelope
     * differently from its component 3' and 5'
     * ends?
     */
    public enum FunctionalRole {
        SEQUENCING_PRIMER,
        ADAPTOR,
        MOLECULAR_INDEX,
        AMPLIFICATION_PRIMER
    }

    public DNAAppendage.FunctionalRole getFunctionalRole() {
        return functionalRole;
    }

    public String get3To5Sequence(){
        return threeToFiveSequence;
    }

    public String get5To3Sequence(){
        return fiveToThreeSequence;
    }
}
