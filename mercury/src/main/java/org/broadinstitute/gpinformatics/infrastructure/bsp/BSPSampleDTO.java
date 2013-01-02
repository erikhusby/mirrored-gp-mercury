package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.google.common.base.FinalizablePhantomReference;
import org.apache.commons.lang3.StringUtils;

/**
 * A simple DTO for fetching commonly used data from BSP.
 */
public class BSPSampleDTO {

    public static final String TUMOR_IND = "Tumor";
    public static final String NORMAL_IND = "Normal";

    public static final String FEMALE_IND = "Female";
    public static final String MALE_IND = "Male";

    public static final String ACTIVE_IND = "Active Stock";


    private String patientId;

    private String stockSample;

    private String rootSample;

    private String aliquotSample;

    private String collaboratorsSampleName;

    private String collection;

    private double volume;

    private double concentration;

    private String organism;

    private String stockAtExport;

    private Boolean positiveControl;

    private Boolean negativeControl;

    private String sampleLsid;

    private String collaboratorParticipantId;

    private String materialType;

    private double total;

    private String sampleType;

    private String primaryDisease;

    private String gender;

    private String stockType;

    private String fingerprint;

    private String containerId;

    private String sampleId;

    private String collaboratorName;

    private String race;

    private String population;

    /**
     * Use this when no valid DTO is present, to avoid null checks
     */
    public static final BSPSampleDTO DUMMY =
            new BSPSampleDTO("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "","","","");

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

    public BSPSampleDTO(String containerId, String stockSample, String rootSample, String aliquotSample,
                        String patientId, String organism, String collaboratorsSampleName, String collection,
                        String volume, String concentration, String sampleLsid, String collaboratorParticipantId,
                        String materialType, String total, String sampleType, String primaryDisease,
                        String gender, String stockType, String fingerprint, String sampleId,String collaboratorName,
                        String race,String population) {
        this(primaryDisease,sampleLsid,materialType,collaboratorsSampleName,organism,patientId);
        this.containerId = containerId;
        this.stockSample = stockSample;
        this.rootSample = rootSample;
        this.aliquotSample = aliquotSample;
        this.collaboratorsSampleName = collaboratorsSampleName;
        this.collection = collection;
        this.volume = safeParseDouble(volume);
        this.concentration = safeParseDouble(concentration);
        this.total = safeParseDouble(total);
        this.sampleType = sampleType;
        this.gender = gender;
        this.stockType = stockType;
        this.fingerprint = fingerprint;
        stockAtExport = null;
        positiveControl = false;
        negativeControl = false;
        this.sampleId = sampleId;
        this.collaboratorName = collaboratorName;
        this.collaboratorParticipantId = collaboratorParticipantId;
        this.race = race;
        this.population = population;
    }

    /**
     * Useful for tests
     * @param primaryDisease
     * @param lsid
     */
    public BSPSampleDTO(String primaryDisease,
                        String lsid,
                        String materialType,
                        String collaboratorsSampleName,
                        String organism,
                        String patientId) {
        this.primaryDisease = primaryDisease;
        this.sampleLsid = lsid;
        this.materialType = materialType;
        this.collaboratorsSampleName = collaboratorsSampleName;
        this.organism = organism;
        this.patientId = patientId;
    }

    public double getVolume() {
        return volume;
    }

    public double getConcentration() {
        return concentration;
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

    public double getTotal() {
        return total;
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

    public String getCollaboratorName() {
        return collaboratorName;
    }

    public String getPopulation() {
        return population;
    }

    public String getRace() {
        return race;
    }
}
