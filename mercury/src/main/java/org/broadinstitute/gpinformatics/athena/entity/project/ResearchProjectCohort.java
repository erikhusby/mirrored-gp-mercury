package org.broadinstitute.gpinformatics.athena.entity.project;

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
    private ResearchProject researchProject;

    // The BSP cohort Identifier
    private String cohortId;

    protected ResearchProjectCohort() { }

    ResearchProjectCohort(ResearchProject researchProject, String cohortId) {
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
}

