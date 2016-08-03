package org.broadinstitute.gpinformatics.athena.entity.project;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * This holds the cohorts for a research project
 */
@Entity
@Audited
@Table(name = "RESEARCH_PROJECT_COHORT", schema = "athena")
public class ResearchProjectCohort {

    public static final int COHORT_PREFIX_LENGTH = 3;
    @Id
    @SequenceGenerator(name="seq_rp_cohort_index", schema = "athena", sequenceName="seq_rp_cohort_index")
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="seq_rp_cohort_index")
    private Long researchProjectCohortId;

    /**
     * This is eager fetched because this class' whole purpose is to bridge a specific person and project. If you
     * ever only need the ID, you should write a specific projection query in the DAO
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @Index(name = "ix_cohort_project")
    @JoinColumn(name = "RESEARCH_PROJECT")
    private ResearchProject researchProject;

    // The BSP cohort Identifier
    @Column(name = "COHORT_ID")
    private String cohortId;

    protected ResearchProjectCohort() { }

    public Long getDatabaseId() {
        return Long.parseLong(cohortId.substring(COHORT_PREFIX_LENGTH));
    }

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

