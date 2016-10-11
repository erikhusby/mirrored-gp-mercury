package org.broadinstitute.gpinformatics.infrastructure.cognos.entity;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 *
 */
@Entity
@Table(schema = "COGNOS", name = "ORSP_PROJECT_CONSENT")
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
