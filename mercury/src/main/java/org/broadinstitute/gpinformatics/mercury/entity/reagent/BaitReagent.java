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

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Reagent for BaitDesigns.
 * Ponds are then hybridized to RNA "Baits" which are complementary to genomic regions of interest.
 * These baits are single stranded and biotinylated.
 */
@Entity
@Audited
@Table(schema = "mercury")
// todo jmt rename to DesignedReagent
public class BaitReagent extends Reagent {
    private String targetSet;

    @ManyToOne(fetch = FetchType.LAZY)
    private ReagentDesign reagentDesign;

    /**
     * Construct a BaitDesign
     *
     * @param designName     Example: cancer_2000gene_shift170_undercovered
     * @param targetSet      Example: Cancer_2K
     * @param manufacturerId 123465
     */
    public BaitReagent(ReagentDesign reagentDesign) {
        // todo jmt what to pass to super?
        super(null, null);
        this.reagentDesign = reagentDesign;
    }

    BaitReagent() {
    }

    public String getTargetSet() {
        return targetSet;
    }

    public void setTargetSet(String targetSet) {
        this.targetSet = targetSet;
    }

    public ReagentDesign getReagentDesign() {
        return reagentDesign;
    }
}

