package org.broadinstitute.sequel.entity.reagent;


import org.broadinstitute.sequel.entity.reagent.Reagent;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.reagent.Reagent;
import org.broadinstitute.sequel.entity.vessel.MolecularEnvelope;
import org.broadinstitute.sequel.entity.vessel.MolecularStateRange;

import javax.persistence.Entity;

@Entity
public class MolecularIndexReagent extends Reagent {

    public MolecularIndexReagent(MolecularEnvelope envelope) {
        super(null, null, envelope);
    }

    public MolecularIndexReagent() {
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
