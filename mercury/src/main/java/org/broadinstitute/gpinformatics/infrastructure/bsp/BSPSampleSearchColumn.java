package org.broadinstitute.gpinformatics.infrastructure.bsp;


import java.util.HashMap;
import java.util.Map;

public enum BSPSampleSearchColumn {
    SAMPLE_ID("Sample ID"),
    SAMPLE_KIT("Sample Kit"),
    SAMPLE_STATUS("Sample Status"),
    PARTICIPANT_ID("Participant ID(s)"),
    COLLABORATOR_SAMPLE_ID("Collaborator Sample ID"),
    SPECIES("Species"),
    COLLABORATOR_PARTICIPANT_ID("Collaborator Participant ID"),
    COLLABORATOR_FAMILY_ID("Collaborator Family ID"),
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
    BSP_COLLECTION_BARCODE("BSP Collection Barcode"),
    BSP_COLLECTION_NAME("BSP Collection Name"),
    STOCK_SAMPLE("Stock Sample"),
    PARENT_SAMPLES("Parent Sample(s)"),
    CONTAINER_ID("Container"),
    CONTAINER_NAME("Container Name"),
    COLLABORATOR_NAME("Site PI"),
    RACE("Race"),
    ETHNICITY("Ethnicity"),
    RACKSCAN_MISMATCH("Sample Kit Data Upload/Rackscan Mismatch"),
    RIN("RIN Number"),
    RQS("RNA Quality Score (RQS)"),
    PICO_RUN_DATE("Pico Run Date"),
    RECEIPT_DATE("Receipt Date"),
    ORIGINAL_MATERIAL_TYPE("Original Material Type"),
    DV200("DV200"),
    RECEPTACLE_TYPE("Receptacle Type");

    private static final Map<String, BSPSampleSearchColumn> MAP_NAME_TO_COLUMN =
            new HashMap<>(BSPSampleSearchColumn.values().length);
    private final String columnName;
    public String columnName() { return columnName; }

    BSPSampleSearchColumn(String name) {
        this.columnName = name;
    }

    public static final BSPSampleSearchColumn[] PDO_SEARCH_COLUMNS = {
        BSPSampleSearchColumn.PARTICIPANT_ID,
        BSPSampleSearchColumn.ROOT_SAMPLE,
        BSPSampleSearchColumn.STOCK_SAMPLE,
        BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID,
        BSPSampleSearchColumn.COLLECTION,
        BSPSampleSearchColumn.BSP_COLLECTION_BARCODE,
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
        BSPSampleSearchColumn.CONTAINER_ID,
        BSPSampleSearchColumn.SAMPLE_ID,
        BSPSampleSearchColumn.COLLABORATOR_NAME,
        BSPSampleSearchColumn.ETHNICITY,
        BSPSampleSearchColumn.RACE,
        BSPSampleSearchColumn.RACKSCAN_MISMATCH,
        BSPSampleSearchColumn.RIN,
        BSPSampleSearchColumn.RQS,
        BSPSampleSearchColumn.PICO_RUN_DATE,
        BSPSampleSearchColumn.RECEIPT_DATE,
        BSPSampleSearchColumn.DV200
    };

    public static final BSPSampleSearchColumn[] BILLING_TRACKER_COLUMNS = {
            COLLABORATOR_SAMPLE_ID
    };

    public static final BSPSampleSearchColumn[] QUANT_UPLOAD_COLUMNS = {
            COLLABORATOR_PARTICIPANT_ID,
            ORIGINAL_MATERIAL_TYPE
    };

    public static final BSPSampleSearchColumn[] QUANT_DATA_COLUMNS = {
            VOLUME, PICO_RUN_DATE, CONCENTRATION, TOTAL_DNA
    };

    public static final BSPSampleSearchColumn[] BUCKET_PAGE_COLUMNS = {
            COLLABORATOR_SAMPLE_ID, MATERIAL_TYPE, RECEIPT_DATE, ROOT_SAMPLE
    };

    public static final BSPSampleSearchColumn[] EXTERNAL_LIBRARY_COLUMNS = {
        BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID,
        BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID,
        BSPSampleSearchColumn.GENDER,
        BSPSampleSearchColumn.SPECIES,
    };

    public static boolean isQuantColumn(BSPSampleSearchColumn bspSampleSearchColumn) {
        for (BSPSampleSearchColumn quantDataColumn : QUANT_DATA_COLUMNS) {
            if (quantDataColumn.equals(bspSampleSearchColumn)) {
                return true;
            }
        }
        return false;
    }

    static {
        for (BSPSampleSearchColumn column : BSPSampleSearchColumn.values()) {
            MAP_NAME_TO_COLUMN.put(column.columnName(), column);
        }
    }

    public static BSPSampleSearchColumn getByName(String displayName) {
        return MAP_NAME_TO_COLUMN.get(displayName);
    }
}
