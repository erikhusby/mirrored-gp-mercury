package org.broadinstitute.gpinformatics.athena.entity.project;

import javax.persistence.*;

/**
 * The funding for research projects
 */
@Entity
@Table(schema = "athena")
public class ResearchProjectFunding {

    // todo jmt why is allocationSize 1?
    @Id
    @SequenceGenerator(name="seq_rp_funding_index", schema = "athena", sequenceName="seq_rp_funding_index", allocationSize = 1)
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="seq_rp_funding_index")
    private Long id;

    @ManyToOne
    private ResearchProject researchProject;

    // A funding Identifier
    private String fundingId;

    public ResearchProjectFunding() { }

    public ResearchProjectFunding(ResearchProject researchProject, String fundingId) {
        this.researchProject = researchProject;
        this.fundingId =fundingId;
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

    public String getFundingId() {
        return fundingId;
    }

    public void setFundingId(String fundingId) {
        this.fundingId = fundingId;
    }
}
