package org.broadinstitute.sequel.entity.reagent;


import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.MolecularEnvelope;
import org.broadinstitute.sequel.entity.vessel.MolecularStateRange;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

@Entity
public class MolecularIndexReagent extends Reagent {

    @ManyToOne(fetch = FetchType.LAZY)
    private MolecularIndexingScheme molecularIndexingScheme;

    public MolecularIndexReagent(MolecularIndexingScheme molecularIndexingScheme) {
        super(null, null, null);
        this.molecularIndexingScheme = molecularIndexingScheme;
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

    public MolecularIndexingScheme getMolecularIndexingScheme() {
        return molecularIndexingScheme;
    }
}
