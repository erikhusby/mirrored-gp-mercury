package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * A control that represents a Unique Molecular Identifier
 */
@Entity
@Audited
public class UMIReagent extends Reagent {

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "UNIQUE_MOLECULAR_IDENTIFIER")
    private UniqueMolecularIdentifier uniqueMolecularIdentifier;

    public UMIReagent(UniqueMolecularIdentifier uniqueMolecularIdentifier) {
        super(null, null, null);
        this.uniqueMolecularIdentifier = uniqueMolecularIdentifier;
    }

    /** For JPA */
    protected UMIReagent() {
    }

    public UniqueMolecularIdentifier getUniqueMolecularIdentifier() {
        return uniqueMolecularIdentifier;
    }
}
