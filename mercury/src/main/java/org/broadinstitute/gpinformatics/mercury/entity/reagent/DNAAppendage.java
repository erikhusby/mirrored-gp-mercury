package org.broadinstitute.gpinformatics.mercury.entity.reagent;


import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingTechnology;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MolecularAppendage;

public class DNAAppendage implements MolecularAppendage {
    
    private String appendageName;
    private SequencingTechnology.TECHNOLOGY_NAME sequencingTechnology;
    private FunctionalRole functionalRole;
    private String threeToFiveSequence;
    private String fiveToThreeSequence;

    public DNAAppendage(String appendageName, SequencingTechnology.TECHNOLOGY_NAME sequencingTechnology,
            FunctionalRole functionalRole, String threeToFiveSequence, String fiveToThreeSequence) {
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
    public SequencingTechnology.TECHNOLOGY_NAME getSequencingTechnology() {
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
