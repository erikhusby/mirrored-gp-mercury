package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.sample.MaterialType;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;

import javax.annotation.Nonnull;
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

    private final Map<BSPSampleSearchColumn, String> columnToValue = new HashMap<BSPSampleSearchColumn, String>();

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

    /**
     * Use this when no valid DTO is present, to avoid null checks.
     */
    public static final BSPSampleDTO DUMMY = createDummy();

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

    public static BSPSampleDTO createConcentrationSampleDummy(String concentration, String sampleId) {
        return new BSPSampleDTO("", "", "", "", "", "", "", "", "", concentration, "", "", "", "", "", "", "", "", "", sampleId, "", "", "", "", FFPEStatus.NOT_DERIVED);
    }

    public static BSPSampleDTO createVolumeSampleDummy(String volume, String sampleId) {
        return new BSPSampleDTO("", "", "", "", "", "", "", "", volume, "", "", "", "", "", "", "", "", "", "", sampleId, "", "", "", "", FFPEStatus.NOT_DERIVED);
    }

    public static BSPSampleDTO createTotalDNASampleDummy(String totalDNA, String sampleId) {
        return new BSPSampleDTO("", "", "", "", "", "", "", "", "", "", "", "", "", totalDNA, "", "", "", "", "", sampleId, "", "", "", "", FFPEStatus.NOT_DERIVED);
    }

    public static BSPSampleDTO createMaterialTypeDummy(String materialType) {
        return new BSPSampleDTO("", "", materialType, "", "", "");
    }

    public static BSPSampleDTO createMaterialTypeSampleDummy(String materialType, String sampleId) {
        return new BSPSampleDTO("", "", "", "", "", "", "", "", "", "", "", "", materialType, "", "", "", "", "", "", sampleId, "", "", "", "", FFPEStatus.NOT_DERIVED);
    }

    public static BSPSampleDTO createDummy() {
        return new BSPSampleDTO("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", FFPEStatus.NOT_DERIVED);
    }

    private String getValue(String[] bspColumns, int columnNumber) {
        if (bspColumns.length <= columnNumber) {
            return null;
        }

        return trim(bspColumns[columnNumber]);
    }

    /**
     * A constructor based upon the BSP sample search result data.  This would need to be updated is the BSP Sample Search gets changed.
     *
     * @param bspColumns The BSP Sample Search ws data
     */
    public BSPSampleDTO(String[] bspColumns, BSPSampleSearchColumn[] searchColumns) {

        if (searchColumns.length != bspColumns.length) {
            throw new IllegalArgumentException("The columns returned are not the same length as the actual columns searched");
        }

        columnToValue.clear();
        for (int i=0; i<searchColumns.length; i++) {
            columnToValue.put(searchColumns[i], trim(bspColumns[i]));
        }
    }

    public BSPSampleDTO(String primaryDisease,
                        String lsid,
                        String materialType,
                        String collaboratorsSampleName,
                        String organism,
                        String patientId) {

        columnToValue.clear();

        columnToValue.put(BSPSampleSearchColumn.PRIMARY_DISEASE, trim(primaryDisease));
        columnToValue.put(BSPSampleSearchColumn.LSID, trim(lsid));
        columnToValue.put(BSPSampleSearchColumn.MATERIAL_TYPE, trim(materialType));
        columnToValue.put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, trim(collaboratorsSampleName));
        columnToValue.put(BSPSampleSearchColumn.SPECIES, trim(organism));
        columnToValue.put(BSPSampleSearchColumn.PARTICIPANT_ID, trim(patientId));

        // Need to set this explicitly to be sure we don't trigger a lazy load in a DBFree test context
        this.ffpeStatus = FFPEStatus.NOT_DERIVED;
    }

    public BSPSampleDTO(String containerId, String stockSample, String rootSample, String aliquotSample,
                        String patientId, String organism, String collaboratorsSampleName, String collection,
                        String volume, String concentration, String sampleLsid, String collaboratorParticipantId,
                        String materialType, String total, String sampleType, String primaryDisease,
                        String gender, String stockType, String fingerprint, String sampleId, String collaboratorName,
                        String race, String population, String sampleKitUploadRackscanMismatch, @Nonnull FFPEStatus ffpeStatus) {

        this(primaryDisease, sampleLsid, materialType, collaboratorsSampleName, organism, patientId);

        columnToValue.put(BSPSampleSearchColumn.CONTAINER_ID, trim(containerId));
        columnToValue.put(BSPSampleSearchColumn.STOCK_SAMPLE, trim(stockSample));
        columnToValue.put(BSPSampleSearchColumn.ROOT_SAMPLE, trim(rootSample));
        columnToValue.put(BSPSampleSearchColumn.SAMPLE_ID, trim(aliquotSample));
        columnToValue.put(BSPSampleSearchColumn.COLLECTION, trim(collection));
        columnToValue.put(BSPSampleSearchColumn.STOCK_TYPE, trim(stockType));
        columnToValue.put(BSPSampleSearchColumn.VOLUME, trim(volume));
        columnToValue.put(BSPSampleSearchColumn.CONCENTRATION, trim(concentration));
        columnToValue.put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, trim(collaboratorParticipantId));
        columnToValue.put(BSPSampleSearchColumn.TOTAL_DNA, trim(total));
        columnToValue.put(BSPSampleSearchColumn.SAMPLE_TYPE, trim(sampleType));
        columnToValue.put(BSPSampleSearchColumn.GENDER, trim(gender));
        columnToValue.put(BSPSampleSearchColumn.FINGERPRINT, trim(fingerprint));
        columnToValue.put(BSPSampleSearchColumn.SAMPLE_ID, trim(sampleId));
        columnToValue.put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, trim(collaboratorName));
        columnToValue.put(BSPSampleSearchColumn.ETHNICITY, trim(race));
        columnToValue.put(BSPSampleSearchColumn.RACKSCAN_MISMATCH, trim(sampleKitUploadRackscanMismatch));
        columnToValue.put(BSPSampleSearchColumn.RIN, "0.0");

        this.ffpeStatus = ffpeStatus;
    }

    /**
     * Use this constructor for real code, FFPE status will be fetched only on demand.
     */
    public BSPSampleDTO(String containerId, String stockSample, String rootSample, String aliquotSample,
                        String patientId, String organism, String collaboratorsSampleName, String collection,
                        String volume, String concentration, String sampleLsid, String collaboratorParticipantId,
                        String materialType, String total, String sampleType, String primaryDisease,
                        String gender, String stockType, String fingerprint, String sampleId, String collaboratorName,
                        String race, String population, String sampleKitUploadRackscanMismatch) {
        this(containerId, stockSample, rootSample, aliquotSample, patientId, organism, collaboratorsSampleName, collection,
                volume, concentration, sampleLsid, collaboratorParticipantId, materialType, total, sampleType, primaryDisease,
                gender, stockType, fingerprint, sampleId, collaboratorName, race, population, sampleKitUploadRackscanMismatch, FFPEStatus.UNKNOWN);
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

    public String getPopulation() {
        return columnToValue.get(BSPSampleSearchColumn.ETHNICITY);
    }

    public String getRace() {
        return columnToValue.get(BSPSampleSearchColumn.ETHNICITY);
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
