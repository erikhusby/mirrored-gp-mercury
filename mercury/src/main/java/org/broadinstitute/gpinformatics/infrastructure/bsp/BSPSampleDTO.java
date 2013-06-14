package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.sample.MaterialType;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;

import javax.annotation.Nonnull;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * A class that stores data fetched from BSP. In the case of Plastic Barcodes and FFPE, the data will be retrieved
 * from BSP if it isn't already present.
 * <p/>
 * If a value is missing the following default values are returned, based on the object type:
 * <ul>
 * <li>double - 0</li>
 * <li>String - ""</li>
 * <li>boolean - false</li>
 * </ul>
 */
public class BSPSampleDTO {

    public static final String TUMOR_IND = "Tumor";
    public static final String NORMAL_IND = "Normal";

    public static final String FEMALE_IND = "Female";
    public static final String MALE_IND = "Male";

    public static final String ACTIVE_IND = "Active Stock";

    private final Map<BSPSampleSearchColumn, String> columnToValue;

    //This is the BSP sample receipt date formatter. (ex. 11/18/2010)
    public static final SimpleDateFormat BSP_DATE_FORMAT = new SimpleDateFormat("MM/DD/yyyy");

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

    private Date getDate(BSPSampleSearchColumn column) {
        String dateString = getValue(column);

        if (StringUtils.isNotBlank(dateString)){
            try {
                return BSP_DATE_FORMAT.parse(dateString);
            } catch (Exception e) {
                // Fall through to return.
            }
        }

        return null;

    }

    private boolean getBoolean(BSPSampleSearchColumn column) {
        return Boolean.parseBoolean(getValue(column));
    }

    public Date getPicoRunDate() {
        return getDate(BSPSampleSearchColumn.PICO_RUN_DATE);
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

    /**
     * This method returns true when the sample is received using the following logic:
     * <ol>
     * <li>If the sample id is not the root sample we are not a root sample and therefore received.
     * Otherwise we are a root sample and need to check condition 2.</li>
     * <li>If we are a root sample and we have a receipt date then we have been received.</li>
     * </ol>
     *
     * @return A boolean that determines if this sample has been received or not.
     */
    public boolean isSampleReceived() {
        try {
            return !getRootSample().equals(getSampleId()) || getReceiptDate() != null;
        } catch (ParseException e) {
            //In the case of a parsing exception we assume there is a date, however the date can not be parsed which isn't important for this logic.
            return true;
        }
    }

    public Date getReceiptDate() throws ParseException {
        return getDate(BSPSampleSearchColumn.RECEIPT_DATE);
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
            plasticBarcodes = new ArrayList<>();
        }
        plasticBarcodes.add(barcode);
    }

    /**
     * Finds the most appropriate label/barcode for a Mercury LabVessel. BSPSampleDTO has a notion of potentially having
     * more than one "plastic barcode". Not all receptacles in BSP have a manufacturer barcode, in which case the
     * barcode on the label (which resolves to the sample ID) is the best we can do. This utility looks through all of
     * the candidate barcodes and chooses the "best" fit for a Mercury LabVessel, preferring something that does not
     * look like a BSP sample ID.
     *
     * @return the appropriate barcode for a Mercury LabVessel
     */
    public String getBarcodeForLabVessel() {
        String manufacturerBarcode = null;
        String bspLabelBarcode = null;

        List<String> barcodes = getPlasticBarcodes();
        if (barcodes != null) {
            for (String barcode : barcodes) {

                // plasticBarcodes shouldn't contain nulls, but the code for that seems to be in flux at the moment -BPR
                if (barcode != null) {
                    if (BSPUtil.isInBspFormat(barcode)) {
                        bspLabelBarcode = barcode;
                    } else {
                        manufacturerBarcode = barcode;
                    }
                }
            }
        }

        // Prefers the manufacturer's barcode i.e. not the SM-id.
        if (manufacturerBarcode != null) {
            return manufacturerBarcode;
        } else {
            return bspLabelBarcode;
        }
    }
}
