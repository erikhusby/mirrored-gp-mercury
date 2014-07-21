package org.broadinstitute.gpinformatics.mercury.entity.reagent;


import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

@Entity
@Audited
public class MolecularIndexReagent extends Reagent {

    @ManyToOne(fetch = FetchType.LAZY)
    @BatchSize(size = 500)
    private MolecularIndexingScheme molecularIndexingScheme;

    public MolecularIndexReagent(MolecularIndexingScheme molecularIndexingScheme) {
        // todo jmt what to pass to super?
        super(null, null, null);
        this.molecularIndexingScheme = molecularIndexingScheme;
    }

    public MolecularIndexReagent() {
    }

    public MolecularIndexingScheme getMolecularIndexingScheme() {
        return molecularIndexingScheme;
    }
}
