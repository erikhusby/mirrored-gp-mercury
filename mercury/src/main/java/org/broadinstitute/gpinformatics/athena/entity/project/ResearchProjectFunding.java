package org.broadinstitute.gpinformatics.athena.entity.project;

import org.hibernate.envers.Audited;

import javax.persistence.*;

/**
 * The funding for research projects
 */
@Entity
@Audited
@Table(schema = "athena")
public class ResearchProjectFunding {

    @Id
    @SequenceGenerator(name="seq_rp_funding_index", schema = "athena", sequenceName="seq_rp_funding_index")
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="seq_rp_funding_index")
    private Long researchProjectFundingId;

    @ManyToOne
    private ResearchProject researchProject;

    // A funding Identifier
    private String fundingId;

    public ResearchProjectFunding() { }

    public ResearchProjectFunding(ResearchProject researchProject, String fundingId) {
        this.researchProject = researchProject;
        this.fundingId =fundingId;
    }

    public Long getResearchProjectFundingId() {
        return researchProjectFundingId;
    }

    public ResearchProject getResearchProject() {
        return researchProject;
    }

    public String getFundingId() {
        return fundingId;
    }
}
