package org.broadinstitute.gpinformatics.infrastructure.bsp;

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


    private final String patientId;

    private final String stockSample;

    private final String rootSample;

    private final String aliquotSample;

    private final String collaboratorsSampleName;

    private final String collection;

    private final double volume;

    private final double concentration;

    private final String organism;

    private final String stockAtExport;

    private final Boolean positiveControl;

    private final Boolean negativeControl;

    private final String sampleLsid;

    private final String collaboratorParticipantId;

    private final String materialType;

    private final double total;

    private final String sampleType;

    private final String primaryDisease;

    private final String gender;

    private final String stockType;

    private final String fingerprint;

    private final String containerId;

    private final String sampleId;

    /**
     * Use this when no valid DTO is present, to avoid null checks
     */
    public static final BSPSampleDTO DUMMY =
            new BSPSampleDTO("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "");

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
                        String gender, String stockType, String fingerprint, String sampleId) {
        this.containerId = containerId;
        this.stockSample = stockSample;
        this.rootSample = rootSample;
        this.aliquotSample = aliquotSample;
        this.patientId = patientId;
        this.collaboratorsSampleName = collaboratorsSampleName;
        this.collection = collection;

        this.volume = safeParseDouble(volume);
        this.concentration = safeParseDouble(concentration);
        this.organism = organism;
        this.sampleLsid = sampleLsid;
        this.collaboratorParticipantId = collaboratorParticipantId;
        this.materialType = materialType;
        this.total = safeParseDouble(total);
        this.sampleType = sampleType;
        this.primaryDisease = primaryDisease;
        this.gender = gender;
        this.stockType = stockType;
        this.fingerprint = fingerprint;
        stockAtExport = null;
        positiveControl = false;
        negativeControl = false;
        this.sampleId = sampleId;
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
}
