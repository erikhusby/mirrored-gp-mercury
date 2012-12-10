package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.envers.Audited;

/**
 * A ReagentDesign is the name of magical
 * elixirs, such as Baits and CATs, which
 * are ordered from companies like IDT
 * or brewed in-house.
 */
@Entity
@Audited
@Table(schema = "mercury",uniqueConstraints = {@UniqueConstraint(columnNames={"reagentDesign", "reagentType"})})
public class ReagentDesign {

    @Id
    @SequenceGenerator(name = "SEQ_REAGENT_DESIGN", schema = "mercury", sequenceName = "SEQ_REAGENT_DESIGN")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_REAGENT_DESIGN")
    private Long reagentDesignId;

    private String reagentDesign;
    private String targetSetName;
    private String manufacturersName;

    @OneToMany(mappedBy = "reagentDesign")
    private Set<DesignedReagent> designedReagents = new HashSet<DesignedReagent>();

    /** For JPA */
    ReagentDesign() {
    }

    public enum REAGENT_TYPE {
        BAIT,CAT
    }

    @Enumerated(EnumType.STRING)
    private REAGENT_TYPE reagentType;

    /**
     *
     * @param designName     Example: cancer_2000gene_shift170_undercovered
     * @param reagentType
     */
    public ReagentDesign(String designName, REAGENT_TYPE reagentType) {
        if (designName == null) {
             throw new NullPointerException("designName cannot be null."); 
        }
        if (reagentType == null) {
             throw new NullPointerException("reagentType cannot be null.");
        }
        this.reagentDesign = designName;
        this.reagentType = reagentType;
    }

    public REAGENT_TYPE getReagentType() {
        return reagentType;
    }

    public String getDesignName() {
        return reagentDesign;
    }

    public void setDesignName(String designName) {
        this.reagentDesign = designName;
    }

    public String getTargetSetName() {
        return targetSetName;
    }

    /**
     *
     * @param targetSetName      Example: Cancer_2K
     */
    public void setTargetSetName(String targetSetName) {
        this.targetSetName = targetSetName;
    }

    public String getManufacturersName() {
        return manufacturersName;
    }

    public void setManufacturersName(String manufacturersName) {
        this.manufacturersName = manufacturersName;
    }

    public Set<DesignedReagent> getDesignedReagents() {
        return designedReagents;
    }

    public void addDesignedReagent(DesignedReagent designedReagent) {
        this.designedReagents.add(designedReagent);
//        designedReagent.setReagentDesign(this);
    }

    public Long getReagentDesignId() {
        return reagentDesignId;
    }
}
