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
 * The funding for research projects
 */
@Entity
@Audited
@Table(name = "RESEARCH_PROJECT_FUNDING", schema = "athena")
public class ResearchProjectFunding {

    @Id
    @SequenceGenerator(name="seq_rp_funding_index", schema = "athena", sequenceName="seq_rp_funding_index")
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="seq_rp_funding_index")
    private Long researchProjectFundingId;

    /**
     * This is eager fetched because this class' whole purpose is to bridge a specific person and project. If you
     * ever only need the ID, you should write a specific projection query in the DAO
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @Index(name = "ix_funding_project")
    @JoinColumn(name = "RESEARCH_PROJECT")
    private ResearchProject researchProject;

    // A funding Identifier
    @Column(name = "FUNDING_ID", nullable = false)
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
