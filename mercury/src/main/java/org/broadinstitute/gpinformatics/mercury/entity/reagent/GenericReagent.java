package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MolecularEnvelope;
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

}
