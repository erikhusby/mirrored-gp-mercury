package org.broadinstitute.gpinformatics.infrastructure.bsp;


public enum BSPSampleSearchColumn {
    SAMPLE_ID("Sample ID", 18),
    PARTICIPANT_ID("Participant ID(s)", 0),  // aka Patient ID
    COLLABORATOR_SAMPLE_ID("Collaborator Sample ID", 3),
    SPECIES("Species", null),
    COLLABORATOR_PARTICIPANT_ID("Collaborator Participant ID", 9),
    MATERIAL_TYPE("Material Type", 10),
    VOLUME("Vol(uL)", 5),
    CONCENTRATION("Conc(ng/uL)" ,6),
    TOTAL_DNA("Total ug", 11),
    SAMPLE_TYPE("Sample Type", 12),
    PRIMARY_DISEASE("Primary Disease", 13),
    GENDER("Gender", 14),
    STOCK_TYPE("Stock Type", 15),
    FINGERPRINT("Fingerprint", 16),
    LSID("Sample LSID", 8),
    ROOT_SAMPLE("Root Sample(s)", 1),
    COLLECTION("Collection", 4),
    STOCK_SAMPLE("Stock Sample", 2),
    PARENT_SAMPLES("Parent Sample(s)", null),
    CONTAINER_ID("Container", 17),
    CONTAINER_NAME("Container Name", null),
    COLLABORATOR_NAME("Site PI", 19) ,

    // todo arz db free testing of race and ethnicity
    RACE("Race", null),
    ETHNICITY("Ethnicity", 20),

    RACKSCAN_MISMATCH("Sample Kit Data Upload/Rackscan Mismatch", 22);

    private final String columnName;

    private final Integer columnNumber;

    public String columnName() { return columnName; }
    public Integer columnNumber() { return columnNumber; }

    BSPSampleSearchColumn(String name, Integer column) {
        this.columnName = name;
        this.columnNumber = column;
    }
}
