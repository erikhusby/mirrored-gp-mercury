package org.broadinstitute.gpinformatics.mercury.entity.reagent;


import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Audited
@Table(schema = "mercury")
public class MolecularIndexReagent extends Reagent {

    @ManyToOne(fetch = FetchType.LAZY)
    private MolecularIndexingScheme molecularIndexingScheme;

    public MolecularIndexReagent(MolecularIndexingScheme molecularIndexingScheme) {
        // todo jmt what to pass to super?
        super(null, null);
        this.molecularIndexingScheme = molecularIndexingScheme;
    }

    public MolecularIndexReagent() {
    }

    public MolecularIndexingScheme getMolecularIndexingScheme() {
        return molecularIndexingScheme;
    }
}
