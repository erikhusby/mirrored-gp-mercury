package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.sample.MaterialType;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;

import java.util.*;

/**
 * A simple DTO for fetching commonly used data from BSP.
 */
public class BSPSampleDTO {

    public static final String TUMOR_IND = "Tumor";
    public static final String NORMAL_IND = "Normal";

    public static final String FEMALE_IND = "Female";
    public static final String MALE_IND = "Male";

    public static final String ACTIVE_IND = "Active Stock";

    private final Map<BSPSampleSearchColumn, String> columnToValue;

    public boolean hasNoColumns() {
        return columnToValue.isEmpty();
    }

    public enum FFPEStatus {
        DERIVED(true),
        NOT_DERIVED(false),
        UNKNOWN(null);

        Boolean derived;

        FFPEStatus(Boolean derived) {
            this.derived = derived;
        }

        static FFPEStatus fromBoolean(Boolean bool) {
            for (FFPEStatus ffpeStatus : values()) {
                if (ffpeStatus.derived == bool) {
                    return ffpeStatus;
                }
            }
            throw new RuntimeException("Cannot map FFPEStatus to Boolean");
        }
    }

    private FFPEStatus ffpeStatus = FFPEStatus.UNKNOWN;

    private List<String> plasticBarcodes;

    // collaborator?
    // species vs organism?
    // strain?
    // tissueType?

    private static double safeParseDouble(String s) {
        try {
            if (!StringUtils.isBlank(s)) {
                return Double.parseDouble(s);
            }
        } catch (Exception e) {
            // fall through.
        }
        return 0;
    }


    /**
     * A constructor based upon the BSP sample search result data.  This would need to be updated is the BSP Sample Search gets changed.
     *
     * @param dataMap The BSP Sample Search results mapped by the columns
     */
    public BSPSampleDTO(Map<BSPSampleSearchColumn, String> dataMap) {
        columnToValue = dataMap;
    }

    /**
     * Trim off any empty white space after the string value.
     *
     * @param value The string to trim
     * @return Trimmed value
     */
    private static String trim(String value) {
        if (value != null) {
            if (value.trim().length() > 0) {
                return value.trim();
            }
        }
        return null;
    }

    public double getRin() {
        return safeParseDouble(columnToValue.get(BSPSampleSearchColumn.RIN));
    }

    public double getVolume() {
        return safeParseDouble(columnToValue.get(BSPSampleSearchColumn.VOLUME));
    }

    public double getConcentration() {
        return safeParseDouble(columnToValue.get(BSPSampleSearchColumn.CONCENTRATION));
    }

    public String getRootSample() {
        return columnToValue.get(BSPSampleSearchColumn.ROOT_SAMPLE);
    }

    public String getStockSample() {
        return columnToValue.get(BSPSampleSearchColumn.STOCK_SAMPLE);
    }

    public String getCollection() {
        return columnToValue.get(BSPSampleSearchColumn.COLLECTION);
    }

    public String getCollaboratorsSampleName() {
        return columnToValue.get(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID);
    }

    public String getContainerId() {
        return columnToValue.get(BSPSampleSearchColumn.CONTAINER_ID);
    }

    public String getPatientId() {
        return columnToValue.get(BSPSampleSearchColumn.PARTICIPANT_ID);
    }

    public String getOrganism() {
        return columnToValue.get(BSPSampleSearchColumn.SPECIES);
    }

    public boolean getHasSampleKitUploadRackscanMismatch() {
        return Boolean.parseBoolean(columnToValue.get(BSPSampleSearchColumn.RACKSCAN_MISMATCH));
    }

    public String getSampleLsid() {
        return columnToValue.get(BSPSampleSearchColumn.LSID);
    }

    public String getCollaboratorParticipantId() {
        return columnToValue.get(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID);
    }

    public String getMaterialType() {
        return columnToValue.get(BSPSampleSearchColumn.MATERIAL_TYPE);
    }

    public double getTotal() {
        return safeParseDouble(columnToValue.get(BSPSampleSearchColumn.TOTAL_DNA));
    }

    public String getSampleType() {
        return columnToValue.get(BSPSampleSearchColumn.SAMPLE_TYPE);
    }

    public String getPrimaryDisease() {
        return columnToValue.get(BSPSampleSearchColumn.PRIMARY_DISEASE);
    }

    public String getGender() {
        return columnToValue.get(BSPSampleSearchColumn.GENDER);
    }

    public String getStockType() {
        return columnToValue.get(BSPSampleSearchColumn.STOCK_TYPE);
    }

    public String getFingerprint() {
        return columnToValue.get(BSPSampleSearchColumn.FINGERPRINT);
    }

    public boolean isSampleReceived() {
        return !StringUtils.isBlank(columnToValue.get(BSPSampleSearchColumn.ROOT_SAMPLE));
    }

    public boolean isActiveStock() {
        String stockType = getStockType();
        return (stockType != null) && (stockType.equals(ACTIVE_IND));
    }

    public boolean getHasFingerprint() {
        return !StringUtils.isBlank(getFingerprint());
    }

    public String getSampleId() {
        return columnToValue.get(BSPSampleSearchColumn.SAMPLE_ID);
    }

    public MaterialType getMaterialTypeObject() {
        String materialType = getMaterialType();
        return (StringUtils.isBlank(materialType)) ? null : new MaterialType(materialType);
    }

    public String getCollaboratorName() {
        return columnToValue.get(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID);
    }

    public String getEthnicity() {
        return columnToValue.get(BSPSampleSearchColumn.ETHNICITY);
    }

    public String getRace() {
        return columnToValue.get(BSPSampleSearchColumn.RACE);
    }

    public Boolean getFfpeStatus() {
        if (ffpeStatus == FFPEStatus.UNKNOWN) {
            BSPSampleDataFetcher bspSampleDataFetcher = ServiceAccessUtility.getBean(BSPSampleDataFetcher.class);
            bspSampleDataFetcher.fetchFFPEDerived(Collections.singletonList(this));
        }
        return ffpeStatus.derived;
    }

    public void setFfpeStatus(Boolean ffpeStatus) {
        this.ffpeStatus = FFPEStatus.fromBoolean(ffpeStatus);
    }

    public List<String> getPlasticBarcodes() {
        if (plasticBarcodes == null) {
            BSPSampleDataFetcher bspSampleDataFetcher = ServiceAccessUtility.getBean(BSPSampleDataFetcher.class);
            bspSampleDataFetcher.fetchSamplePlastic(Collections.singletonList(this));
        }
        return plasticBarcodes;
    }

    public void addPlastic(String barcode) {
        if (plasticBarcodes == null) {
            plasticBarcodes = new ArrayList<String>();
        }
        plasticBarcodes.add(barcode);
    }
}
