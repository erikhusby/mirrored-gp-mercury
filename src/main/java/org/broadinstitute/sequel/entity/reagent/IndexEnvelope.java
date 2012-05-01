package org.broadinstitute.sequel.entity.reagent;


import org.broadinstitute.sequel.entity.run.SequencingTechnology;
import org.broadinstitute.sequel.entity.vessel.MolecularAppendage;
import org.broadinstitute.sequel.entity.vessel.MolecularEnvelope;

public class IndexEnvelope  extends MolecularEnvelope {

    private String threePrimeSeq;
    private String fivePrimeSeq;
    private String name;

    // todo jmt add technology
    public IndexEnvelope(String threePrimeSeq,
                         String fivePrimeSeq,
                         String name) {
        this.threePrimeSeq = threePrimeSeq;
        this.fivePrimeSeq = fivePrimeSeq;
        this.name = name;
    }
    
    @Override
    public FUNCTIONAL_ROLE getFunctionalRole() {
        return FUNCTIONAL_ROLE.INDEX;
    }

    @Override
    public MolecularAppendage get3PrimeAttachment() {
        return new DNAAppendage(name, SequencingTechnology.TECHNOLOGY_NAME.ILLUMINA_HISEQ, DNAAppendage.FunctionalRole.MOLECULAR_INDEX,
                threePrimeSeq, null);
    }

    @Override
    public MolecularAppendage get5PrimeAttachment() {
        return new DNAAppendage(name, SequencingTechnology.TECHNOLOGY_NAME.ILLUMINA_HISEQ, DNAAppendage.FunctionalRole.MOLECULAR_INDEX,
                null, fivePrimeSeq);
    }

    @Override
    public MolecularEnvelope getContainedEnvelope() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void surroundWith(MolecularEnvelope containingEnvelope) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public boolean contains(MolecularAppendage appendage) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public boolean contains3Prime(MolecularAppendage appendage) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public boolean contains5Prime(MolecularAppendage appendage) {
        throw new RuntimeException("I haven't been written yet.");
    }
}
