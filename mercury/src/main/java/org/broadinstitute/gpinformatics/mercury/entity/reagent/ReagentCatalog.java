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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Catalog for Reagents
 */
@Entity
@Audited
@Table(schema = "mercury")
public abstract class ReagentCatalog {
    @Id
    @SequenceGenerator(name = "SEQ_REAGENT_CATALOG", schema = "mercury", sequenceName = "SEQ_REAGENT_CATALOG")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_REAGENT_CATALOG")
    private Long reagentCatalogId;

    @ManyToOne(fetch = FetchType.LAZY)
    private Reagent reagent;

    protected ReagentCatalog(Reagent reagent) {
        this.reagent = reagent;
    }

    protected ReagentCatalog() {
    }
}
