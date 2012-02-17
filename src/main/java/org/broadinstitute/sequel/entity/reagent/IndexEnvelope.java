package org.broadinstitute.sequel.entity.reagent;


import org.broadinstitute.sequel.entity.vessel.MolecularAppendage;
import org.broadinstitute.sequel.entity.vessel.MolecularEnvelope;

public class IndexEnvelope  implements MolecularEnvelope {

    public IndexEnvelope(String threePrimeSeq,
                         String fivePrimeSeq,
                         String name) {

    }
    
    @Override
    public FUNCTIONAL_ROLE getFunctionalRole() {
        return FUNCTIONAL_ROLE.INDEX;
    }

    @Override
    public MolecularAppendage get3PrimeAttachment() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public MolecularAppendage get5PrimeAttachment() {
        throw new RuntimeException("I haven't been written yet.");
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
