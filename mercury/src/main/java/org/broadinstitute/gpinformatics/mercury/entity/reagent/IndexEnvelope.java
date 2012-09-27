package org.broadinstitute.gpinformatics.mercury.entity.reagent;


import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingTechnology;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MolecularAppendage;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MolecularEnvelope;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;

@Entity
@Audited
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

    protected IndexEnvelope() {
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
