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

    public ResearchProject getResearchProject() {
        return researchProject;
    }

    public String getFundingId() {
        return fundingId;
    }
}
