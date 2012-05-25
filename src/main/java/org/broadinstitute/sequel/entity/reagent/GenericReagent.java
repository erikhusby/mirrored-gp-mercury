package org.broadinstitute.sequel.entity.reagent;

import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.MolecularEnvelope;
import org.broadinstitute.sequel.entity.vessel.MolecularStateRange;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import java.util.Set;

/**
 * A Generic reagent that doesn't change the molecular envelope
 */
@Entity
public class GenericReagent extends Reagent {

    @ManyToMany
    private Set<LabVessel> containers;

    public GenericReagent(String reagentName, String lot, MolecularEnvelope molecularEnvelope) {
        super(reagentName, lot, molecularEnvelope);
    }

    protected GenericReagent() {
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
