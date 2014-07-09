package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import java.util.Date;
import java.util.Set;

/**
 * A Generic reagent that doesn't change the molecular envelope
 */
@Entity
@Audited
public class GenericReagent extends Reagent {

    @ManyToMany(mappedBy = "reagentContents")
    private Set<LabVessel> containers;

    public GenericReagent(String reagentName,
                          String lot, /*, MolecularEnvelope molecularEnvelope*/
                          Date expiration) {
        super(reagentName, lot, expiration);
    }

    protected GenericReagent() {
    }

}
