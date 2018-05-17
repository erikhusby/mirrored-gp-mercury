package org.broadinstitute.gpinformatics.athena.entity.project;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.presentation.Displayable;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * IRBs for a research project
 */
@Entity
@Audited
@Table(name = "RESEARCH_PROJECTIRB", schema = "athena")
public class ResearchProjectIRB {

    public enum IrbType implements Displayable {
        PARTNERS("IRB from Partners"),
        FARBER("IRB from Dana Farber"),
        MIT("IRB from MIT COUHES"),
        BROAD("IACUC from Broad"),
        OTHER("External");

        private String displayName;

        IrbType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getName() {
            return name();
        }

        public static IrbType findByDisplayName(String typeString) {
            for (IrbType type : values()) {
                if (type.displayName.equals(typeString)) {
                    return type;
                }
            }

            return null;
        }
    }

    @Id
    @SequenceGenerator(name="seq_rp_irb_index", schema = "athena", sequenceName="seq_rp_irb_index")
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="seq_rp_irb_index")
    private Long researchProjectIRBId;

    /**
     * This is eager fetched because this class' whole purpose is to bridge a specific person and project. If you
     * ever only need the ID, you should write a specific projection query in the DAO
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @Index(name = "ix_irb_project")
    @JoinColumn(name = "RESEARCH_PROJECT")
    private ResearchProject researchProject;

    @Column(name = "IRB")
    private String irb;

    @Column(name = "IRB_TYPE")
    @Enumerated(EnumType.STRING)
    private IrbType irbType;

    protected ResearchProjectIRB() { }

    public ResearchProjectIRB(ResearchProject researchProject, IrbType irbType, String irb) {
        this.researchProject = researchProject;
        this.irbType = irbType;
        this.irb = irb;
    }

    public Long getResearchProjectIRBId() {
        return researchProjectIRBId;
    }

    public ResearchProject getResearchProject() {
        return researchProject;
    }

    public String getIrb() {
        return irb;
    }

    public IrbType getIrbType() {
        return irbType;
    }

    @Override
    public boolean equals(Object other) {
        if ( (this == other ) ) {
            return true;
        }

        if (!(other instanceof ResearchProjectIRB)) {
            return false;
        }

        ResearchProjectIRB castOther = (ResearchProjectIRB) other;
        return new EqualsBuilder()
                .append(getIrb(), castOther.getIrb())
                .append(getIrbType(), castOther.getIrbType())
                .append(getResearchProject(), castOther.getResearchProject()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getIrb()).append(getIrbType()).append(getResearchProject()).toHashCode();
    }
}
