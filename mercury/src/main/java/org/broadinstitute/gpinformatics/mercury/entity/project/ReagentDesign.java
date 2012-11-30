package org.broadinstitute.gpinformatics.mercury.entity.project;

/**
 * A ReagentDesign is the name of magical
 * elixirs, such as Baits and CATs, which
 * are ordered from companies like IDT
 * or brewed in-house.
 */
public class ReagentDesign {
    
    private String reagentDesign;
// cusgom amplicon tube
    public enum REAGENT_TYPE {
        BAIT,CAT
    }

    private REAGENT_TYPE reagent_type;
    
    public ReagentDesign(String designName,REAGENT_TYPE reagent_type) {
        if (designName == null) {
             throw new NullPointerException("designName cannot be null."); 
        }
        if (reagent_type == null) {
             throw new NullPointerException("reagent_type cannot be null.");
        }
        this.reagentDesign = designName;
        this.reagent_type = reagent_type;
    }

    public REAGENT_TYPE getReagentType() {
        return reagent_type;
    }

    public String getDesignName() {
        return reagentDesign;
    }
}
