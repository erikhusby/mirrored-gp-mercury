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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class OrspProjectConsentKey implements Serializable {

    /*
     * Even though "project_key" should be the default, this fails to deploy unless it explicitly matches the
     * @JoinColumn mapping for OrspProjectConsent.orspProject.
     */
    @Column(name = "project_key")
    private String projectKey;
    @Column(name = "sample_collection_key")
    private String sampleCollection;
    private String consentKey;

    private OrspProjectConsentKey() {}

    public OrspProjectConsentKey(String projectKey, String sampleCollection, String consentKey) {
        this.projectKey = projectKey;
        this.sampleCollection = sampleCollection;
        this.consentKey = consentKey;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public String getSampleCollection() {
        return sampleCollection;
    }

    public String getConsentKey() {
        return consentKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        OrspProjectConsentKey that = (OrspProjectConsentKey) o;

        return new EqualsBuilder()
                .append(projectKey, that.projectKey)
                .append(sampleCollection, that.sampleCollection)
                .append(consentKey, that.consentKey)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(projectKey)
                .append(sampleCollection)
                .append(consentKey)
                .toHashCode();
    }
}
