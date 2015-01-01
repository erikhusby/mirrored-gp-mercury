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

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;
public class BioProject implements Serializable {
    private static final long serialVersionUID = 2014072901l;
    private String accession;
    private String alias;
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

    @XmlElement(required = false)
    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @XmlElement(required = false)
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
