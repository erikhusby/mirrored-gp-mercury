package org.broadinstitute.sequel.entity.reagent;


import org.broadinstitute.sequel.entity.reagent.Reagent;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.reagent.Reagent;
import org.broadinstitute.sequel.entity.vessel.MolecularEnvelope;
import org.broadinstitute.sequel.entity.vessel.MolecularStateRange;

public class MolecularIndexReagent implements Reagent {

    private MolecularEnvelope envelopeDelta;

    public MolecularIndexReagent(MolecularEnvelope envelope) {
        this.envelopeDelta = envelope;
    }

    @Override
    public MolecularEnvelope getMolecularEnvelopeDelta() {
        return envelopeDelta;
    }

    @Override
    public String getReagentName() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public String getLot() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Iterable<LabVessel> getContainers() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void addToContainer(LabVessel container) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Iterable<LabVessel> getContainers(MolecularStateRange molecularStateRange) {
        throw new RuntimeException("I haven't been written yet.");
    }
}
