package org.broadinstitute.gpinformatics.infrastructure.bsp;


public enum BSPSampleSearchColumn {
    SAMPLE_ID("Sample ID"),
    PARTICIPANT_ID("Participant ID(s)"),
    COLLABORATOR_SAMPLE_ID("Collaborator Sample ID"),
    SPECIES("Species"),
    COLLABORATOR_PARTICIPANT_ID("Collaborator Participant ID"),
    MATERIAL_TYPE("Material Type"),
    VOLUME("Vol(uL)"),
    CONCENTRATION("Conc(ng/uL)"),
    TOTAL_DNA("Total ug"),
    SAMPLE_TYPE("Sample Type"),
    PRIMARY_DISEASE("Primary Disease"),
    GENDER("Gender"),
    STOCK_TYPE("Stock Type"),
    FINGERPRINT("Fingerprint"),
    // ALERT("!", "Alert"),
    // MESSAGE("message");

    LSID("Sample LSID"),
    ROOT_SAMPLE("Root Sample(s)"),
    COLLECTION("Collection"),
    STOCK_SAMPLE("Stock Sample"),
    PARENT_SAMPLES("Parent Sample(s)"),
    CONTAINER_ID("Container"),
    CONTAINER_NAME("Container Name")
    ;
    
    private final String columnName;
    
    
    
    public String columnName() { return columnName; }
    
    BSPSampleSearchColumn(String name) {
        this.columnName = name;
    }
    


}
