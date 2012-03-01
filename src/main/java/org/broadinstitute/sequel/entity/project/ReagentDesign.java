package org.broadinstitute.sequel.entity.project;

/**
 * A ReagentDesign is the name of magical
 * elixers, such as Baits and CATs, which
 * are ordered from companies like IDT
 * or brewed in-house.
 */
public class ReagentDesign {
    
    private String reagentDesign;
    
    public ReagentDesign(String designName) {
        if (designName == null) {
             throw new NullPointerException("designName cannot be null."); 
        }
        this.reagentDesign = reagentDesign;
    }
    
    public String getDesignName() {
        return reagentDesign;
    }
}
