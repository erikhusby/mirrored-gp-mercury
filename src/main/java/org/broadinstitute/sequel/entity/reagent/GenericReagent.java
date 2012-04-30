package org.broadinstitute.sequel.entity.reagent;

import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.MolecularEnvelope;
import org.broadinstitute.sequel.entity.vessel.MolecularStateRange;

import java.util.Set;

/**
 * A Generic reagent that doesn't change the molecular envelope
 */
public class GenericReagent extends Reagent {

    // todo jmt should this be in a reagent type class?
    private MolecularEnvelope molecularEnvelope;
    private String reagentName;
    private String lot;
    private Set<LabVessel> containers;

    public GenericReagent(String reagentName, String lot) {
        this.reagentName = reagentName;
        this.lot = lot;
    }

    @Override
    public MolecularEnvelope getMolecularEnvelopeDelta() {
        return molecularEnvelope;
    }

    @Override
    public String getReagentName() {
        return reagentName;
    }

    @Override
    public String getLot() {
        return lot;
    }

    @Override
    public Iterable<LabVessel> getContainers() {
        return containers;
    }

    @Override
    public Iterable<LabVessel> getContainers(MolecularStateRange molecularStateRange) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addToContainer(LabVessel container) {
        container.addReagent(this);
        this.containers.add(container);
    }
}
