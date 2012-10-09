package org.broadinstitute.gpinformatics.athena.entity.project;

import org.hibernate.envers.Audited;

import javax.persistence.*;

/**
 * IRBs for a research project
 */
@Entity
@Audited
@Table(schema = "athena")
public class ResearchProjectIRB {

    public enum IrbType {
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
    }

    @Id
    @SequenceGenerator(name="seq_rp_irb_index", schema = "athena", sequenceName="seq_rp_irb_index")
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="seq_rp_irb_index")
    private Long researchProjectIRBId;

    @ManyToOne
    private ResearchProject researchProject;

    private String irb;
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
}
