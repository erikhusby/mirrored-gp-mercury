package org.broadinstitute.gpinformatics.infrastructure.bsp;

import java.io.Serializable;

/**
 * A simple DTO for fetching commonly used
 * data from BSP.
 */
public class BSPSampleDTO implements Serializable {

    public final static String TUMOR_IND = "Tumor";
    public final static String NORMAL_IND = "Normal";

    public final static String FEMALE_IND = "FEMALE";
    public final static String MALE_IND = "MALE";

    public final static String ACTIVE_IND = "Active Stock";


    private final String patientId;

    private final String stockSample;

    private final String rootSample;

    private final String collaboratorsSampleName;

    private final String collection;

    private final String volume;

    private final String concentration;

    private final String organism;

    private final String stockAtExport;

    private final Boolean positiveControl;

    private final Boolean negativeControl;

    private final String sampleLsid;

    private final String collaboratorParticipantId;

    private final String materialType;

    private final String total;

    private final String sampleType;

    private final String primaryDisease;

    private final String gender;

    private final String stockType;

    private final String fingerprint;


    // collaborator?
    // species vs organism?
    // strain?
    // tissueType?

    public BSPSampleDTO(String containerId, String stockSample, String rootSample, String aliquotSample,
                        String patientId, String organism, String collaboratorsSampleName, String collection,
                        String volume, String concentration, String sampleLsid, String collaboratorParticipantIdIn,
                        String materialTypeIn, String totalIn, String sampleTypeIn, String primaryDiseaseIn,
                        String genderIn, String stockTypeIn, String fingerprintIn) {
        this.stockSample = stockSample;
        this.rootSample = rootSample;
        this.patientId = patientId;
        this.collaboratorsSampleName = collaboratorsSampleName;
        this.collection = collection;
        this.volume = volume;
        this.concentration = concentration;
        this.organism = organism;
        this.sampleLsid = sampleLsid;
        this.collaboratorParticipantId = collaboratorParticipantIdIn;
        this.materialType = materialTypeIn;
        this.total = totalIn;
        this.sampleType = sampleTypeIn;
        this.primaryDisease = primaryDiseaseIn;
        this.gender = genderIn;
        this.stockType = stockTypeIn;
        this.fingerprint = fingerprintIn;
        this.stockAtExport = null;
        this.positiveControl = false;
        this.negativeControl = false;
    }

/*
    public BSPSampleDTO(String containerId,
                        String stockSample,
                        String rootSample,
                        String aliquotSample,
                        String patientId,
                        String organism,
                        String collaboratorsSampleName,
                        String collection,
                        String volume,
                        String concentration,
                        String stockAtExport,
                        Boolean positiveControl,
                        Boolean negativeControl) {
        this.stockSample = stockSample;
        this.rootSample = rootSample;
        this.patientId = patientId;
        this.collaboratorsSampleName = collaboratorsSampleName;
        this.collection = collection;
        this.volume = volume;
        this.concentration = concentration;
        this.organism = organism;
        this.stockAtExport = stockAtExport;
        this.positiveControl = positiveControl;
        this.negativeControl = negativeControl;
    }
*/

    public String getVolume() {
        // todo strongly type, figure out units
        return volume;
    }

    public String getConcentration() {
        // todo strongly type, figure out units
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
     * Gets the name that the collaborator
     * gave to this sample.
     *
     * @return
     */
    public String getCollaboratorsSampleName() {
        return collaboratorsSampleName;
    }

    public String getContainerId() {
        throw new RuntimeException("not implemented yet.");
    }

    public String getPatientId() {
        return patientId;
    }

    public String getOrganism() {
        return organism;
    }

    public String getStockAtExport() {
        return stockAtExport;
    }

    public Boolean isPositiveControl() {
        return positiveControl;
    }

    public Boolean isNegativeControl() {
        return negativeControl;
    }

    public String getSampleLsid() {
        return sampleLsid;
    }

    public Boolean getPositiveControl() {
        return positiveControl;
    }

    public Boolean getNegativeControl() {
        return negativeControl;
    }

    public String getCollaboratorParticipantId() {
        return collaboratorParticipantId;
    }

    public String getMaterialType() {
        return materialType;
    }

    public String getTotal() {
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

}
