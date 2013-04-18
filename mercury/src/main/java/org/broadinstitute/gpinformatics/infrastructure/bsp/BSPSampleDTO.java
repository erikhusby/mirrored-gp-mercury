package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.sample.MaterialType;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A class that stores data fetched from BSP. In the case of Plastic Barcodes and FFPE, the data will be retrieved
 * from BSP if it isn't already present.
 * <p/>
 * If a value is missing the following default values are returned, based on the object type:
 * <ul>
 *     <li>double - 0</li>
 *     <li>String - ""</li>
 *     <li>boolean - false</li>
 * </ul>
 */
public class BSPSampleDTO {

    public static final String TUMOR_IND = "Tumor";
    public static final String NORMAL_IND = "Normal";

    public static final String FEMALE_IND = "Female";
    public static final String MALE_IND = "Male";

    public static final String ACTIVE_IND = "Active Stock";

    private final Map<BSPSampleSearchColumn, String> columnToValue;

    public boolean hasData() {
        return !columnToValue.isEmpty();
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

    /**
     * This constructor creates a dto with no values. This is mainly for tests that don't care about the DTO
     */
    @SuppressWarnings("unchecked")
    public BSPSampleDTO() {
        columnToValue = Collections.emptyMap();
    }

    /**
     * A constructor based upon the BSP sample search result data.  This would need to be updated is the BSP Sample Search gets changed.
     *
     * @param dataMap The BSP Sample Search results mapped by the columns
     */
    @SuppressWarnings("unchecked")
    public BSPSampleDTO(Map<BSPSampleSearchColumn, String> dataMap) {
        columnToValue = dataMap;
    }

    /**
     * Use these methods to access the data, so missing elements are returned as empty strings, which is what
     * the clients of this API expect.
     *
     * @param column column to look up
     * @return value at column, or empty string if missing
     */
    @Nonnull
    private String getValue(BSPSampleSearchColumn column) {
        String value = columnToValue.get(column);
        if (value != null) {
            return value;
        }
        return "";
    }

    private double getDouble(BSPSampleSearchColumn column) {
        String s = getValue(column);
        if (!StringUtils.isBlank(s)) {
            try {
                return Double.parseDouble(s);
            } catch (Exception e) {
                // Fall through to return.
            }
        }
        return 0;
    }

    private boolean getBoolean(BSPSampleSearchColumn column) {
        return Boolean.parseBoolean(getValue(column));
    }

    public double getRin() {
        return getDouble(BSPSampleSearchColumn.RIN);
    }

    public double getVolume() {
        return getDouble(BSPSampleSearchColumn.VOLUME);
    }

    public double getConcentration() {
        return getDouble(BSPSampleSearchColumn.CONCENTRATION);
    }

    public String getRootSample() {
        return getValue(BSPSampleSearchColumn.ROOT_SAMPLE);
    }

    public String getStockSample() {
        return getValue(BSPSampleSearchColumn.STOCK_SAMPLE);
    }

    public String getCollection() {
        return getValue(BSPSampleSearchColumn.COLLECTION);
    }

    public String getCollaboratorsSampleName() {
        return getValue(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID);
    }

    public String getContainerId() {
        return getValue(BSPSampleSearchColumn.CONTAINER_ID);
    }

    public String getPatientId() {
        return getValue(BSPSampleSearchColumn.PARTICIPANT_ID);
    }

    public String getOrganism() {
        return getValue(BSPSampleSearchColumn.SPECIES);
    }

    public boolean getHasSampleKitUploadRackscanMismatch() {
        return getBoolean(BSPSampleSearchColumn.RACKSCAN_MISMATCH);
    }

    public String getSampleLsid() {
        return getValue(BSPSampleSearchColumn.LSID);
    }

    public String getCollaboratorParticipantId() {
        return getValue(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID);
    }

    public String getMaterialType() {
        return getValue(BSPSampleSearchColumn.MATERIAL_TYPE);
    }

    public double getTotal() {
        return getDouble(BSPSampleSearchColumn.TOTAL_DNA);
    }

    public String getSampleType() {
        return getValue(BSPSampleSearchColumn.SAMPLE_TYPE);
    }

    public String getPrimaryDisease() {
        return getValue(BSPSampleSearchColumn.PRIMARY_DISEASE);
    }

    public String getGender() {
        return getValue(BSPSampleSearchColumn.GENDER);
    }

    public String getStockType() {
        return getValue(BSPSampleSearchColumn.STOCK_TYPE);
    }

    public String getFingerprint() {
        return getValue(BSPSampleSearchColumn.FINGERPRINT);
    }

    public boolean isSampleReceived() {
        return !StringUtils.isBlank(getValue(BSPSampleSearchColumn.ROOT_SAMPLE));
    }

    public boolean isActiveStock() {
        String stockType = getStockType();
        return (stockType != null) && (stockType.equals(ACTIVE_IND));
    }

    public boolean getHasFingerprint() {
        return !StringUtils.isBlank(getFingerprint());
    }

    public String getSampleId() {
        return getValue(BSPSampleSearchColumn.SAMPLE_ID);
    }

    public MaterialType getMaterialTypeObject() {
        String materialType = getMaterialType();
        return StringUtils.isBlank(materialType) ? null : new MaterialType(materialType);
    }

    public String getCollaboratorName() {
        return getValue(BSPSampleSearchColumn.COLLABORATOR_NAME);
    }

    public String getEthnicity() {
        return getValue(BSPSampleSearchColumn.ETHNICITY);
    }

    public String getRace() {
        return getValue(BSPSampleSearchColumn.RACE);
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
