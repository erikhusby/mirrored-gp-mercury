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

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement
public class BioProject implements Serializable {
    private static final long serialVersionUID = 2014072901l;
    private String accession;
    private String alias;
    private String projectName;

    public BioProject() {
    }

    public BioProject(String accession, String alias, String projectName) {
        this.accession = accession;
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

        BioProject that = (BioProject) o;

        if (!accession.equals(that.accession)) {
            return false;
        }
        if (!alias.equals(that.alias)) {
            return false;
        }
        if (!projectName.equals(that.projectName)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = accession.hashCode();
        result = 31 * result + alias.hashCode();
        result = 31 * result + projectName.hashCode();
        return result;
    }
}
