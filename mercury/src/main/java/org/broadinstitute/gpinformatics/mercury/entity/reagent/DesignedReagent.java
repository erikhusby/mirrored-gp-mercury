/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2012 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * An instance of a Reagent that is a ReagentDesign (currently either a bait or a CAT).
 */
@Entity
@Audited
public class DesignedReagent extends Reagent {

    // In production code, the reagentDesign will always exist first, so there's no need for cascade, but in tests
    // DesignedReagent and ReagentDesign are created at the same time.
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "REAGENT_DESIGN")
    private ReagentDesign reagentDesign;

    public DesignedReagent(ReagentDesign reagentDesign) {
        // todo jmt what to pass to super?
        super(null, null, null);
        this.reagentDesign = reagentDesign;
        reagentDesign.addDesignedReagent(this);
    }

    /** For JPA */
    protected DesignedReagent() {
    }

    public ReagentDesign getReagentDesign() {
        return reagentDesign;
    }
}

