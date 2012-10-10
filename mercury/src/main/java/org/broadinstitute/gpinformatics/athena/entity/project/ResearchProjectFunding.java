package org.broadinstitute.gpinformatics.athena.entity.project;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Index;

import javax.persistence.*;

/**
 * The funding for research projects
 */
@Entity
public class ResearchProjectFunding {

    @Id
    @SequenceGenerator(name="seq_rp_funding_index", sequenceName="seq_rp_funding_index", allocationSize = 1)
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="seq_rp_funding_index")
    private Long id;

    @ManyToOne
    @Index(name = "ix_funding_project")
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

    @Override
    public boolean equals(Object other) {
        if ( (this == other ) ) {
            return true;
        }

        if ( !(other instanceof ResearchProjectFunding) ) {
            return false;
        }

        ResearchProjectFunding castOther = (ResearchProjectFunding) other;
        return new EqualsBuilder()
                .append(getFundingId(), castOther.getFundingId())
                .append(getResearchProject(), castOther.getResearchProject()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getFundingId()).append(getResearchProject()).toHashCode();
    }
}
