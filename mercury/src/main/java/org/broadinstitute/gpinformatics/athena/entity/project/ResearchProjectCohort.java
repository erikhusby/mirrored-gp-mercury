package org.broadinstitute.gpinformatics.athena.entity.project;

import javax.persistence.*;

/**
 * This holds the cohorts for a research project
 */
@Entity
public class ResearchProjectCohort {

    @Id
    @SequenceGenerator(name="seq_rp_cohort_index", sequenceName="seq_rp_cohort_index", allocationSize = 1)
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="seq_rp_cohort_index")
    private Long id;

    @ManyToOne
    private ResearchProject researchProject;

    // The BSP cohort Identifier
    private String cohortId;


    protected ResearchProjectCohort() { }

    ResearchProjectCohort(ResearchProject researchProject, String cohortId) {
        this.researchProject = researchProject;
        this.cohortId = cohortId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ResearchProject getResearchProject() {
        return researchProject;
    }

    public void setResearchProject(ResearchProject researchProject) {
        this.researchProject = researchProject;
    }

    public String getCohortId() {
        return cohortId;
    }

    public void setCohortId(String cohortId) {
        this.cohortId = cohortId;
    }
}

