package org.broadinstitute.gpinformatics.athena.entity.project;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * IRBs for a research project
 */
@Entity
public class ResearchProjectIRB {

    @Id
    private Long id;

    @ManyToOne
    private ResearchProject researchProject;

    private String irb;

    public ResearchProjectIRB() { }

    public ResearchProjectIRB(ResearchProject researchProject, String irb) {
        this.researchProject = researchProject;
        this.irb = irb;
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

    public String getIrb() {
        return irb;
    }

    public void setIrb(String irb) {
        this.irb = irb;
    }
}
