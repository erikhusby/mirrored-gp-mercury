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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;


@JsonPropertyOrder(alphabetic = true)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown=true)
public class BioProjects implements Serializable {
    @JsonProperty
    private List<BioProject> bioprojects;

    public BioProjects() {
    }

    public BioProjects(BioProject ... bioProjects) {
        this.bioprojects = Arrays.asList(bioProjects);
    }

    public List<BioProject> getBioprojects() {
        return bioprojects;
    }

    public void setBioprojects(List<BioProject>  bioprojects) {
        this.bioprojects = bioprojects;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BioProjects that = OrmUtil.proxySafeCast(o, BioProjects.class);
               return new EqualsBuilder().append(this.bioprojects, that.bioprojects).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(bioprojects).hashCode();
    }
}
