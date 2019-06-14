package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DDPKitInfo {

    // For convenience, set during receive step
    private String manufacturerBarcode;

    private String collaboratorParticipantId;

    private String collaboratorSampleId;

    private Long organismClassificationId;

    private String sampleCollectionBarcode;

    private String gender;

    private String materialInfo;

    private String receptacleName;

    public DDPKitInfo() {
    }

    public String getCollaboratorParticipantId() {
        return collaboratorParticipantId;
    }

    public void setCollaboratorParticipantId(String collaboratorParticipantId) {
        this.collaboratorParticipantId = collaboratorParticipantId;
    }

    public String getCollaboratorSampleId() {
        return collaboratorSampleId;
    }

    public void setCollaboratorSampleId(String collaboratorSampleId) {
        this.collaboratorSampleId = collaboratorSampleId;
    }

    public Long getOrganismClassificationId() {
        return organismClassificationId;
    }

    public void setOrganismClassificationId(Long organismClassificationId) {
        this.organismClassificationId = organismClassificationId;
    }

    public String getSampleCollectionBarcode() {
        return sampleCollectionBarcode;
    }

    public void setSampleCollectionBarcode(String sampleCollectionBarcode) {
        this.sampleCollectionBarcode = sampleCollectionBarcode;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getMaterialInfo() {
        return materialInfo;
    }

    public void setMaterialInfo(String materialInfo) {
        this.materialInfo = materialInfo;
    }

    public String getReceptacleName() {
        return receptacleName;
    }

    public void setReceptacleName(String receptacleName) {
        this.receptacleName = receptacleName;
    }

    public String getManufacturerBarcode() {
        return manufacturerBarcode;
    }

    public void setManufacturerBarcode(String manufacturerBarcode) {
        this.manufacturerBarcode = manufacturerBarcode;
    }
}
