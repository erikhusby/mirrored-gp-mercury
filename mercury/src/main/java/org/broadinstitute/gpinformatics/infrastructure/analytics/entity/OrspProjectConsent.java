/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2018 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.analytics.entity;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 *
 */
@Entity
@Table(schema = "ANALYTICS", name = "ORSP_SAMPLE_STAR")
public class OrspProjectConsent {

    @EmbeddedId
    private OrspProjectConsentKey key;

    /*
     * For these many-to-one associations, insertable and updatable are false so that Hibernate treats this mapping as
     * read-only and uses the keys from the composite key instead of from the related entities. (Bauer, King, Gregory.
     * 2016. Java Persistence with Hibernate, Second Edition. Manning Publications. Ch. 8.3.2, p. 195)
     */

    @ManyToOne
    @JoinColumn(name="project_key", insertable = false, updatable = false)
    private OrspProject orspProject;

    public OrspProjectConsentKey getKey() {
        return key;
    }

    public OrspProject getOrspProject() {
        return orspProject;
    }
}
