package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.sample.MaterialType;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;

import javax.annotation.Nonnull;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class BSPSampleDTO implements SampleData {

    private static final Log logger = LogFactory.getLog(BSPSampleDTO.class);

    public static final String TUMOR_IND = "Tumor";
    public static final String NORMAL_IND = "Normal";

    public static final String FEMALE_IND = "Female";
    public static final String MALE_IND = "Male";

    public static final String ACTIVE_IND = "Active Stock";

    // Regular expression for RIN range patterns used when there isn't a clear RIN reading.  These will look like
    // "1.5 - 2.5" or "3-4".  The actual pattern used here defensively allows for whitespace around the numbers and
    // captures the beginning and ending numbers of the range.  There are optional non-capturing groups for decimal
    // points and digits after the decimal point (corresponding to the "1.5 - 2.5" example above), although examples
    // with decimal points and digits after the decimal points have not been observed in the wild.
    private static final Pattern RIN_RANGE = Pattern.compile("\\s*(\\d+(?:\\.\\d+)?)\\s*-\\s*(\\d+(?:\\.\\d+)?)\\s*");
    public static final String SAMPLE_ID = "sampleId";
    public static final String COLLABORATOR_SAMPLE_ID = "collaboratorSampleId";
    public static final String PATIENT_ID = "patientId";
    public static final String COLLABORATOR_PARTICIPANT_ID = "collaboratorParticipantId";
    public static final String VOLUME = "volume";
    public static final String CONCENTRATION = "concentration";
    public static final String PICO_DATE = "picoDate";
    public static final String TOTAL = "total";
    public static final String HAS_SAMPLE_KIT_UPLOAD_RACKSCAN_MISMATCH = "hasSampleKitUploadRackscanMismatch";
    public static final String COMPLETELY_BILLED = "completelyBilled";
    public static final String PACKAGE_DATE = "packageDate";
    public static final String RECEIPT_DATE = "receiptDate";
    public static final String SUPPORTS_NUMBER_OF_LANES = "supportsNumberOfLanes";
    public static final String JSON_RIN_KEY = "rin";
    public static final String JSON_RQS_KEY = "rqs";

    private final Map<BSPSampleSearchColumn, String> columnToValue;

    // package-local for test
    Map<BSPSampleSearchColumn, String> getColumnToValue() {
        return columnToValue;
    }

    // This is the BSP sample receipt date format string. (ex. 11/18/2010)
    public static final String BSP_DATE_FORMAT_STRING = "MM/dd/yyyy";

    @Override
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
     *
     * @return value at column, or empty string if missing
     */
    @Nonnull
    protected String getValue(BSPSampleSearchColumn column) {
        String value = columnToValue.get(column);
        if (value != null) {
            return value;
        }
        return "";
    }

    private double getDouble(BSPSampleSearchColumn column) {
        String s = getValue(column);
        if (StringUtils.isNotBlank(s)) {
            return Double.parseDouble(s);
        }
        return 0.0;
    }

    private Double getDoubleOrNull(BSPSampleSearchColumn column) {
        String value = getValue(column);
        if (StringUtils.isNotBlank(value)) {
            return Double.parseDouble(value);
        }
        return null;
    }

    /**
     * Returns an array of one double if this is a regular non-range (e.g. "3.14159") or an array of two doubles if
     * this is a range ("2.71828 - 3.14159").
     */
    private double[] getDoubleOrRangeOfDoubles(BSPSampleSearchColumn column) {
        try {
            return new double[]{getDouble(column)};
        } catch (NumberFormatException e) {
            // The data did not parse as a double; how about as a range of doubles?
            Matcher matcher = RIN_RANGE.matcher(getValue(column));
            if (!matcher.matches()) {
                // Not a range either.
                throw new NumberFormatException(getValue(column));
            }
            return new double[]{Double.parseDouble(matcher.group(1)), Double.parseDouble(matcher.group(2))};
        }
    }

    /**
     * Returns true if the sample has a rin score
     * that can be converted to a number.  Otherwise,
     * returns false.
     */
    @Override
    public boolean canRinScoreBeUsedForOnRiskCalculation() {
        boolean isOkay = true;
        try {
            getRin();
        }
        catch(NumberFormatException e) {
            isOkay = false;
        }
        return isOkay;
    }

    private Date getDate(BSPSampleSearchColumn column) {
        String dateString = getValue(column);

        if (StringUtils.isNotBlank(dateString)) {
            try {
                return new SimpleDateFormat(BSP_DATE_FORMAT_STRING).parse(dateString);
            } catch (Exception e) {
                // Fall through to return.
                logger.warn(
                        "column: " + column.columnName() + " for sample: " + getSampleId() +
                        " had a bad value of " + dateString, e);
            }
        }

        return null;
    }

    private boolean getBoolean(BSPSampleSearchColumn column) {
        return Boolean.parseBoolean(getValue(column));
    }

    @Override
    public Date getPicoRunDate() {
        return getDate(BSPSampleSearchColumn.PICO_RUN_DATE);
    }

    /**
     * Returns the unmodified value of the "RIN Number"
     * annotation from BSP.
     * @see #getRin()
     */
    @Override
    public String getRawRin() {
        return getValue(BSPSampleSearchColumn.RIN);
    }

    /**
     * Get the RIN value for the sample. Returns null if no RIN value is set. If the RIN value is expressed as a range,
     * the lower value is returned. Throws NumberFormatException if the value cannot be parsed as a single value or as a
     * range.
     *
     * @return the (lowest) RIN value from BSP; null if no value is set
     * @throws NumberFormatException if the value cannot be parsed as a single value or a range
     *
     * @see #getRawRin() to get the unmodified rin value as a string.
     */
    @Override
    public Double getRin() {
        Double rin = null;
        String rawRin = getRawRin();
        if (StringUtils.isNotBlank(rawRin)) {
            double[] doubles = getDoubleOrRangeOfDoubles(BSPSampleSearchColumn.RIN);
            if (doubles.length == 1) {
                rin = doubles[0];
            } else {
                // The logic for RIN is to take the lower of the two numbers.
                rin = Math.min(doubles[0], doubles[1]);
            }
        }
        return rin;
    }

    @Override
    public Double getRqs() {
        return getDoubleOrNull(BSPSampleSearchColumn.RQS);
    }

    @Override
    public double getVolume() {
        return getDouble(BSPSampleSearchColumn.VOLUME);
    }

    @Override
    public Double getConcentration() {
        return getDouble(BSPSampleSearchColumn.CONCENTRATION);
    }

    @Override
    public String getRootSample() {
        return getValue(BSPSampleSearchColumn.ROOT_SAMPLE);
    }

    @Override
    public String getStockSample() {
        return getValue(BSPSampleSearchColumn.STOCK_SAMPLE);
    }

    @Override
    public String getCollection() {
        return getValue(BSPSampleSearchColumn.COLLECTION);
    }

    @Override
    public String getCollaboratorsSampleName() {
        return getValue(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID);
    }

    @Override
    public String getContainerId() {
        return getValue(BSPSampleSearchColumn.CONTAINER_ID);
    }

    @Override
    public String getPatientId() {
        return getValue(BSPSampleSearchColumn.PARTICIPANT_ID);
    }

    @Override
    public String getOrganism() {
        return getValue(BSPSampleSearchColumn.SPECIES);
    }

    @Override
    public boolean getHasSampleKitUploadRackscanMismatch() {
        return getBoolean(BSPSampleSearchColumn.RACKSCAN_MISMATCH);
    }

    @Override
    public String getSampleLsid() {
        return getValue(BSPSampleSearchColumn.LSID);
    }

    @Override
    public String getCollaboratorParticipantId() {
        return getValue(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID);
    }

    @Override
    public String getMaterialType() {
        return getValue(BSPSampleSearchColumn.MATERIAL_TYPE);
    }

    @Override
    public double getTotal() {
        return getDouble(BSPSampleSearchColumn.TOTAL_DNA);
    }

    @Override
    public String getSampleType() {
        return getValue(BSPSampleSearchColumn.SAMPLE_TYPE);
    }

    @Override
    public String getPrimaryDisease() {
        return getValue(BSPSampleSearchColumn.PRIMARY_DISEASE);
    }

    @Override
    public String getGender() {
        return getValue(BSPSampleSearchColumn.GENDER);
    }

    @Override
    public String getStockType() {
        return getValue(BSPSampleSearchColumn.STOCK_TYPE);
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
    @Override
    public boolean isSampleReceived() {
        try {
            return !getRootSample().equals(getSampleId()) || getReceiptDate() != null;
        } catch (Exception e) {
            //In the case of a parsing exception we assume there is a date, however the date can not be parsed which
            // isn't important for this logic.
            logger.warn("Receipt Date for sample: " + getSampleId() + " was bad", e);
            return true;
        }
    }

    @Override
    public Date getReceiptDate() throws ParseException {
        return getDate(BSPSampleSearchColumn.RECEIPT_DATE);
    }


    @Override
    public boolean isActiveStock() {
        String stockType = getStockType();
        return (stockType != null) && (stockType.equals(ACTIVE_IND));
    }

    @Override
    public String getSampleId() {
        return getValue(BSPSampleSearchColumn.SAMPLE_ID);
    }

    @Override
    public MaterialType getMaterialTypeObject() {
        String materialType = getMaterialType();
        return StringUtils.isBlank(materialType) ? null : new MaterialType(materialType);
    }

    @Override
    public String getCollaboratorName() {
        return getValue(BSPSampleSearchColumn.COLLABORATOR_NAME);
    }

    @Override
    public String getEthnicity() {
        return getValue(BSPSampleSearchColumn.ETHNICITY);
    }

    @Override
    public String getRace() {
        return getValue(BSPSampleSearchColumn.RACE);
    }

    @Override
    public Boolean getFfpeStatus() {
        if (ffpeStatus == FFPEStatus.UNKNOWN) {
            BSPSampleDataFetcher sampleDataFetcher = ServiceAccessUtility.getBean(BSPSampleDataFetcher.class);
            sampleDataFetcher.fetchFFPEDerived(Collections.singletonList(this));
        }
        return ffpeStatus.derived;
    }

    public void setFfpeStatus(Boolean ffpeStatus) {
        this.ffpeStatus = FFPEStatus.fromBoolean(ffpeStatus);
    }

    public List<String> getPlasticBarcodes() {
        if (plasticBarcodes == null) {
            BSPSampleDataFetcher sampleDataFetcher = ServiceAccessUtility.getBean(BSPSampleDataFetcher.class);
            sampleDataFetcher.fetchSamplePlastic(Collections.singletonList(this));
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
