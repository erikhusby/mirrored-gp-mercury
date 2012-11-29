package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * A ReagentDesign is the name of magical
 * elixirs, such as Baits and CATs, which
 * are ordered from companies like IDT
 * or brewed in-house.
 */
@Entity
@Audited
@Table(schema = "mercury")
public class ReagentDesign {

    @Id
    @SequenceGenerator(name = "SEQ_REAGENT_DESIGN", schema = "mercury", sequenceName = "SEQ_REAGENT_DESIGN")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_REAGENT_DESIGN")
    private Long reagentDesignId;

    // name, manufacturer's ID, target set name
    private String reagentDesign;
    private String targetSetName;
    private String manufacturersName;

    /** For JPA */
    ReagentDesign() {
    }

    public enum REAGENT_TYPE {
        BAIT,CAT
    }

    @Enumerated(EnumType.STRING)
    private REAGENT_TYPE reagent_type;
    
    public ReagentDesign(String designName, REAGENT_TYPE reagentType) {
        if (designName == null) {
             throw new NullPointerException("designName cannot be null."); 
        }
        if (reagentType == null) {
             throw new NullPointerException("reagentType cannot be null.");
        }
        this.reagentDesign = designName;
        this.reagent_type = reagentType;
    }

    public REAGENT_TYPE getReagentType() {
        return reagent_type;
    }

    public String getDesignName() {
        return reagentDesign;
    }

    public String getTargetSetName() {
        return targetSetName;
    }

    public void setTargetSetName(String targetSetName) {
        this.targetSetName = targetSetName;
    }

    public String getManufacturersName() {
        return manufacturersName;
    }

    public void setManufacturersName(String manufacturersName) {
        this.manufacturersName = manufacturersName;
    }
}
