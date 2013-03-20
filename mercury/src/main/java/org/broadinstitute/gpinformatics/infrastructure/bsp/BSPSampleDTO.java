package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.sample.MaterialType;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A simple DTO for fetching commonly used data from BSP.
 */
public class BSPSampleDTO {

    public static final String TUMOR_IND = "Tumor";
    public static final String NORMAL_IND = "Normal";

    public static final String FEMALE_IND = "Female";
    public static final String MALE_IND = "Male";

    public static final String ACTIVE_IND = "Active Stock";

    private final String patientId;

    private String stockSample;

    private String rootSample;

    private String aliquotSample;

    /**
     * These are all the same, but sometimes called different names -- collaboratorsSampleName, collaboratorsSampleID and collaboratorSampleName.
     */
    private final String collaboratorsSampleName;

    private String collection;

    private double volume;

    private double concentration;

    private final String organism;

    private String stockAtExport;

    private Boolean positiveControl;

    private Boolean negativeControl;

    private String sampleKitUploadRackscanMismatch;

    private String sampleLsid;

    private String collaboratorParticipantId;

    private String materialType;

    private double total;

    private double rin;

    private String sampleType;

    private final String primaryDisease;

    private String gender;

    private String stockType;

    private String fingerprint;

    private String containerId;

    private String sampleId;

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

    private String collaboratorName;

    /**
     * Race and Ethnicity are used interchangeably.
     */
    private String race;

    private String population;

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
    public BSPSampleDTO(String[] bspColumns) {

        patientId = getValue(bspColumns, BSPSampleSearchColumn.PARTICIPANT_ID.columnNumber());
        organism = getValue(bspColumns, 7);
        primaryDisease = getValue(bspColumns, BSPSampleSearchColumn.PRIMARY_DISEASE.columnNumber());
        collaboratorsSampleName = getValue(bspColumns, BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID.columnNumber());
        sampleLsid = getValue(bspColumns, BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID.columnNumber());
        rootSample = getValue(bspColumns, BSPSampleSearchColumn.ROOT_SAMPLE.columnNumber());
        stockSample = getValue(bspColumns, BSPSampleSearchColumn.STOCK_SAMPLE.columnNumber());
        collection = getValue(bspColumns, BSPSampleSearchColumn.COLLECTION.columnNumber());
        volume = safeParseDouble(getValue(bspColumns, BSPSampleSearchColumn.VOLUME.columnNumber()));
        concentration = safeParseDouble(getValue(bspColumns, BSPSampleSearchColumn.CONCENTRATION.columnNumber()));
        collaboratorParticipantId = getValue(bspColumns, BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID.columnNumber());
        materialType = getValue(bspColumns, BSPSampleSearchColumn.MATERIAL_TYPE.columnNumber());
        total = safeParseDouble(getValue(bspColumns, BSPSampleSearchColumn.TOTAL_DNA.columnNumber()));
        sampleType = getValue(bspColumns, BSPSampleSearchColumn.SAMPLE_TYPE.columnNumber());
        gender = getValue(bspColumns, BSPSampleSearchColumn.GENDER.columnNumber());
        stockType = getValue(bspColumns, BSPSampleSearchColumn.STOCK_TYPE.columnNumber());
        fingerprint = getValue(bspColumns, BSPSampleSearchColumn.FINGERPRINT.columnNumber());
        containerId = getValue(bspColumns, BSPSampleSearchColumn.CONTAINER_ID.columnNumber());
        sampleId = getValue(bspColumns, BSPSampleSearchColumn.SAMPLE_ID.columnNumber());
        collaboratorName = getValue(bspColumns, BSPSampleSearchColumn.COLLABORATOR_NAME.columnNumber());
        population = getValue(bspColumns, 21);
        sampleKitUploadRackscanMismatch = getValue(bspColumns, BSPSampleSearchColumn.RACKSCAN_MISMATCH.columnNumber());
        rin = safeParseDouble(getValue(bspColumns, BSPSampleSearchColumn.RIN.columnNumber()));

        // Race and Ethnicity are used interchangeably.
        race = getValue(bspColumns, BSPSampleSearchColumn.ETHNICITY.columnNumber());

    }

    /**
     * Use this constructor for DatabaseFree tests with a non-UNKNOWN ffpeStatus value.
     */
    public BSPSampleDTO(String containerId, String stockSample, String rootSample, String aliquotSample,
                        String patientId, String organism, String collaboratorsSampleName, String collection,
                        String volume, String concentration, String sampleLsid, String collaboratorParticipantId,
                        String materialType, String total, String sampleType, String primaryDisease,
                        String gender, String stockType, String fingerprint, String sampleId, String collaboratorName,
                        String race, String population, String sampleKitUploadRackscanMismatch, @Nonnull FFPEStatus ffpeStatus) {
        this(primaryDisease, sampleLsid, materialType, collaboratorsSampleName, organism, patientId);
        this.containerId = containerId;
        this.stockSample = stockSample;
        this.rootSample = rootSample;
        this.aliquotSample = aliquotSample;
        this.collection = collection;

        this.volume = safeParseDouble(volume);
        this.concentration = safeParseDouble(concentration);
        this.collaboratorParticipantId = collaboratorParticipantId;
        this.total = safeParseDouble(total);
        this.sampleType = sampleType;
        this.gender = gender;
        this.stockType = stockType;
        this.fingerprint = fingerprint;
        this.sampleId = sampleId;
        this.collaboratorName = collaboratorName;
        this.race = race;
        this.population = population;
        this.ffpeStatus = ffpeStatus;
        this.sampleKitUploadRackscanMismatch = sampleKitUploadRackscanMismatch;

        rin = 0.0;
        stockAtExport = null;
        positiveControl = false;
        negativeControl = false;
    }

    /**
     * Useful for tests
     *
     * @param primaryDisease
     * @param lsid
     */
    public BSPSampleDTO(String primaryDisease,
                        String lsid,
                        String materialType,
                        String collaboratorsSampleName,
                        String organism,
                        String patientId) {
        this.primaryDisease = trim(primaryDisease);
        this.sampleLsid = trim(lsid);
        this.materialType = trim(materialType);
        this.collaboratorsSampleName = trim(collaboratorsSampleName);
        this.organism = trim(organism);
        this.patientId = trim(patientId);

        // Need to set this explicitly to be sure we don't trigger a lazy load in a DBFree test context
        this.ffpeStatus = FFPEStatus.NOT_DERIVED;
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
        return rin;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public double getConcentration() {
        return concentration;
    }

    public void setConcentration(double concentration) {
        this.concentration = concentration;
    }

    public String getRootSample() {
        return rootSample;
    }

    public String getStockSample() {
        return stockSample;
    }

    /**
     * Returns the name of the BSP collection
     * in which this sample resides
     *
     * @return
     */
    public String getCollection() {
        return collection;
    }

    /**
     * @return the name that the collaborator gave to this sample.
     */
    public String getCollaboratorsSampleName() {
        return collaboratorsSampleName;
    }

    public String getContainerId() {
        return containerId;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getOrganism() {
        return organism;
    }

    public String getStockAtExport() {
        throw new RuntimeException("not implemented yet.");
//        return stockAtExport;
    }

    public boolean isPositiveControl() {
        throw new RuntimeException("not implemented yet.");
//        return positiveControl;
    }

    public boolean isNegativeControl() {
        throw new RuntimeException("not implemented yet.");

//        return negativeControl;
    }

    public boolean getHasSampleKitUploadRackscanMismatch() {
        if (sampleKitUploadRackscanMismatch == null) {
            return false;
        }

        return sampleKitUploadRackscanMismatch.equalsIgnoreCase("true");
    }

    public String getSampleLsid() {
        return sampleLsid;
    }

    public boolean getPositiveControl() {
        throw new RuntimeException("not implemented yet.");
    }

    public boolean getNegativeControl() {
        throw new RuntimeException("not implemented yet.");
    }

    public String getCollaboratorParticipantId() {
        return collaboratorParticipantId;
    }

    public String getMaterialType() {
        return materialType;
    }

    public void setMaterialType(String materialType) {
        this.materialType = materialType;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getSampleType() {
        return sampleType;
    }

    public String getPrimaryDisease() {
        return primaryDisease;
    }

    public String getGender() {
        return gender;
    }

    public String getStockType() {
        return stockType;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public boolean isTumor() {
        return TUMOR_IND.equals(sampleType);
    }

    public boolean isSampleReceived() {
        return !StringUtils.isBlank(rootSample);
    }

    public boolean isActiveStock() {
        return (stockType != null) && (stockType.equals(ACTIVE_IND));
    }

    public boolean getHasFingerprint() {
        return !StringUtils.isBlank(fingerprint);
    }

    public String getAliquotSample() {
        return aliquotSample;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public MaterialType getMaterialTypeObject() {
        if (StringUtils.isBlank(materialType)) {
            return null;
        }
        return new MaterialType(materialType);
    }

    public String getCollaboratorName() {
        return collaboratorName;
    }

    public String getPopulation() {
        return population;
    }

    public String getRace() {
        return race;
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
