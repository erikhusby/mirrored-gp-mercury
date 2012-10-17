package org.broadinstitute.gpinformatics.athena.entity.project;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Index;

import org.hibernate.envers.Audited;

import javax.persistence.*;

/**
 * This holds the cohorts for a research project
 */
@Entity
@Audited
@Table(schema = "athena")
public class ResearchProjectCohort {

    @Id
    @SequenceGenerator(name="seq_rp_cohort_index", schema = "athena", sequenceName="seq_rp_cohort_index")
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="seq_rp_cohort_index")
    private Long researchProjectCohortId;

    @ManyToOne
    @Index(name = "ix_cohort_project")
    private ResearchProject researchProject;

    // The BSP cohort Identifier
    private String cohortId;

    protected ResearchProjectCohort() { }

    public ResearchProjectCohort(ResearchProject researchProject, String cohortId) {
        this.researchProject = researchProject;
        this.cohortId = cohortId;
    }

    public Long getResearchProjectCohortId() {
        return researchProjectCohortId;
    }

    public ResearchProject getResearchProject() {
        return researchProject;
    }

    public String getCohortId() {
        return cohortId;
    }

    @Override
    public boolean equals(Object other) {
        if ( (this == other ) ) {
            return true;
        }

        if ( !(other instanceof ResearchProjectCohort) ) {
            return false;
        }

        ResearchProjectCohort castOther = (ResearchProjectCohort) other;
        return new EqualsBuilder()
                .append(getCohortId(), castOther.getCohortId())
                .append(getResearchProject(), castOther.getResearchProject()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getCohortId()).append(getResearchProject()).toHashCode();
    }
}

