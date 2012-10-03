package org.broadinstitute.gpinformatics.athena.entity.project;

import javax.persistence.*;

/**
 * IRBs for a research project
 */
@Entity
public class ResearchProjectIRB {

    @Id
    @SequenceGenerator(name="seq_rp_irb_index", sequenceName="seq_rp_irb_index", allocationSize = 1)
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="seq_rp_irb_index")
    private Long id;

    @ManyToOne
    private ResearchProject researchProject;

    private String irb;

    protected ResearchProjectIRB() { }

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
