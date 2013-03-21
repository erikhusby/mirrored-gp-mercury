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
    LSID("Sample LSID"),
    ROOT_SAMPLE("Root Sample(s)"),
    COLLECTION("Collection"),
    STOCK_SAMPLE("Stock Sample"),
    PARENT_SAMPLES("Parent Sample(s)"),
    CONTAINER_ID("Container"),
    CONTAINER_NAME("Container Name"),
    COLLABORATOR_NAME("Site PI"),
    RACE("Race"),
    ETHNICITY("Ethnicity"),
    RACKSCAN_MISMATCH("Sample Kit Data Upload/Rackscan Mismatch"),
    RIN("RIN Number");

    private final String columnName;
    public String columnName() { return columnName; }

    BSPSampleSearchColumn(String name) {
        this.columnName = name;
    }

    public static final BSPSampleSearchColumn[] PDO_SEARCH = {
        BSPSampleSearchColumn.PARTICIPANT_ID,
        BSPSampleSearchColumn.ROOT_SAMPLE,
        BSPSampleSearchColumn.STOCK_SAMPLE,
        BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID,
        BSPSampleSearchColumn.COLLECTION,
        BSPSampleSearchColumn.VOLUME,
        BSPSampleSearchColumn.CONCENTRATION,
        BSPSampleSearchColumn.SPECIES,
        BSPSampleSearchColumn.LSID,
        BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID,
        BSPSampleSearchColumn.MATERIAL_TYPE,
        BSPSampleSearchColumn.TOTAL_DNA,
        BSPSampleSearchColumn.SAMPLE_TYPE,
        BSPSampleSearchColumn.PRIMARY_DISEASE,
        BSPSampleSearchColumn.GENDER,
        BSPSampleSearchColumn.STOCK_TYPE,
        BSPSampleSearchColumn.FINGERPRINT,
        BSPSampleSearchColumn.CONTAINER_ID,
        BSPSampleSearchColumn.SAMPLE_ID,
        BSPSampleSearchColumn.COLLABORATOR_NAME,
        BSPSampleSearchColumn.ETHNICITY,
        BSPSampleSearchColumn.RACE,
        BSPSampleSearchColumn.RACKSCAN_MISMATCH,
        BSPSampleSearchColumn.RIN,
    };
}
