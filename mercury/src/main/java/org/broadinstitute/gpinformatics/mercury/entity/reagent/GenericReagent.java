package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MolecularEnvelope;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MolecularStateRange;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import java.util.Set;

/**
 * A Generic reagent that doesn't change the molecular envelope
 */
@Entity
@Audited
@Table(schema = "mercury")
public class GenericReagent extends Reagent {

    @ManyToMany
    @JoinTable(schema = "mercury")
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
