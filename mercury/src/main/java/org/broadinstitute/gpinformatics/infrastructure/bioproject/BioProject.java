/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.bioproject;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.Serializable;

// setting the access order to alphabetical helps the tests pass more reliably.
@JsonPropertyOrder(alphabetic = true)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown=true)
public class BioProject implements Serializable {
    private static final long serialVersionUID = 2014072901l;
    @JsonProperty
    private String accession;
    @JsonProperty
    private String alias;
    @JsonProperty
    private String projectName;

    public BioProject() {
    }

    public BioProject(String accession) {
        this.accession = accession;
    }

    public BioProject(String accession, String alias, String projectName) {
        this(accession);
        this.alias = alias;
        this.projectName = projectName;
    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BioProject that = OrmUtil.proxySafeCast(o, BioProject.class);
        return new EqualsBuilder()
                .append(this.accession, that.accession)
                .append(this.alias, that.alias)
                .append(this.projectName, that.projectName).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(accession).append(alias).append(projectName).hashCode();
    }
}
