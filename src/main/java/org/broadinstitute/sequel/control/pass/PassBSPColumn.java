package org.broadinstitute.sequel.control.pass;


import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleSearchResultFormatter;

public enum PassBSPColumn {

    SAMPLE_ID(BSPSampleSearchColumn.SAMPLE_ID, "sampleID"),
    PARTICIPANT_ID(BSPSampleSearchColumn.PARTICIPANT_ID, "participantId"),
    COLLABORATOR_SAMPLE_ID(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, "collaboratorSampleId"),
    COLLABORATOR_PARTICIPANT_ID(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, "collaboratorParticipantId"),
    MATERIAL_TYPE(BSPSampleSearchColumn.MATERIAL_TYPE, "materialType"),
    VOLUME(BSPSampleSearchColumn.VOLUME, "volume", BSPSampleSearchResultFormatter.TWO_DECIMAL_PLACES),
    CONCENTRATION(BSPSampleSearchColumn.CONCENTRATION, "concentration", BSPSampleSearchResultFormatter.TWO_DECIMAL_PLACES),
    TOTAL_DNA(BSPSampleSearchColumn.TOTAL_DNA, "totalDNA", BSPSampleSearchResultFormatter.UG_TO_NG_WITH_TWO_DECIMAL_PLACES),
    SAMPLE_TYPE(BSPSampleSearchColumn.SAMPLE_TYPE, "sampleType"),
    PRIMARY_DISEASE(BSPSampleSearchColumn.PRIMARY_DISEASE, "primaryDisease"),
    GENDER(BSPSampleSearchColumn.GENDER, "gender"),
    STOCK_TYPE(BSPSampleSearchColumn.STOCK_TYPE, "stockType"),
    FINGERPRINT(BSPSampleSearchColumn.FINGERPRINT, "fingerprinted", BSPSampleSearchResultFormatter.FINGERPRINT);

    private BSPSampleSearchColumn bspColumn;

    private String propertyName;

    private BSPSampleSearchResultFormatter formatter;

    private PassBSPColumn(BSPSampleSearchColumn bspColumn, String propertyName) {
        this(bspColumn, propertyName, BSPSampleSearchResultFormatter.NONE);
    }

    private PassBSPColumn(BSPSampleSearchColumn bspColumn, String propertyName, BSPSampleSearchResultFormatter formatter) {
        this.bspColumn = bspColumn;
        this.propertyName = propertyName;
        this.formatter = formatter;
    }

    public BSPSampleSearchColumn getBspColumn() {
        return bspColumn;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public BSPSampleSearchResultFormatter getFormatter() {
        return formatter;
    }

    public String format(String data) {
        return formatter.format(data);
    }

}
