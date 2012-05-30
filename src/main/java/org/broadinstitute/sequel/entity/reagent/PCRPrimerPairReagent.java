package org.broadinstitute.sequel.entity.reagent;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.MolecularEnvelope;
import org.broadinstitute.sequel.entity.vessel.MolecularStateRange;

import javax.persistence.Entity;

@Entity
public class PCRPrimerPairReagent extends Reagent {

    private static Log gLog = LogFactory.getLog(PCRPrimerPairReagent.class);

    public PrimerPair getPrimerPair() {
        throw new RuntimeException("Method not yet implemented.");
    }

    @Override
    public Iterable<LabVessel> getContainers() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Iterable<LabVessel> getContainers(MolecularStateRange molecularStateRange) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void addToContainer(LabVessel container) {
        throw new RuntimeException("I haven't been written yet.");
    }
}
