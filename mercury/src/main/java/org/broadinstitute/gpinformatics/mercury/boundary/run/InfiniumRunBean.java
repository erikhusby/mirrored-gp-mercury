package org.broadinstitute.gpinformatics.mercury.boundary.run;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

/**
 * Bean returned by InfiniumRunResource.
 */
// todo jmt XSD?
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class InfiniumRunBean {

    private String redIDatPath;
    private String greenIDatPath;
    private String chipManifestPath;
    private String beadPoolManifestPath;
    private String clusterFilePath;
    private String zCallThresholdsPath;
    private String sampleId;
    private String collaboratorSampleId;
    private String sampleLsid;
    private String gender;
    private String participantId;
    private String researchProjectId;
    private boolean positiveControl;
    private boolean negativeControl;
    private String callRateThreshold;
    private String genderClusterFile;
    private String collaboratorParticipantId;
    private String productOrderId;
    private String productName;
    private String productFamily;
    private String productPartNumber;
    private String labBatch;
    private Date scanDate;
    private String scannerName;
    private String csvBeadPoolManifestPath;
    private String regulatoryDesignation;
    // todo jmt this or something more REST-like?
    private String error;

    /** For JAXB. */
    public InfiniumRunBean() {
    }

    public InfiniumRunBean(String redIDatPath, String greenIDatPath, String chipManifestPath,
            String beadPoolManifestPath, String clusterFilePath, String zCallThresholdsPath,
            String sampleId, String collaboratorSampleId, String sampleLsid, String gender, String participantId,
            String researchProjectId, boolean positiveControl, boolean negativeControl,
            String callRateThreshold, String genderClusterFile, String collaboratorParticpantId,
            String productOrderId, String productName, String productFamily, String productPartNumber,
            String labBatch, Date scanDate, String scannerName, String csvBeadPoolManifestPath,
            String regulatoryDesignation) {
        this.redIDatPath = redIDatPath;
        this.greenIDatPath = greenIDatPath;
        this.chipManifestPath = chipManifestPath;
        this.beadPoolManifestPath = beadPoolManifestPath;
        this.clusterFilePath = clusterFilePath;
        this.zCallThresholdsPath = zCallThresholdsPath;
        this.collaboratorSampleId = collaboratorSampleId;
        this.sampleId = sampleId;
        this.sampleLsid = sampleLsid;
        this.gender = gender;
        this.participantId = participantId;
        this.researchProjectId = researchProjectId;
        this.positiveControl = positiveControl;
        this.negativeControl = negativeControl;
        this.callRateThreshold = callRateThreshold;
        this.genderClusterFile = genderClusterFile;
        this.collaboratorParticipantId = collaboratorParticpantId;
        this.productOrderId = productOrderId;
        this.productName = productName;
        this.productFamily = productFamily;
        this.productPartNumber = productPartNumber;
        this.labBatch = labBatch;
        this.scanDate = scanDate;
        this.scannerName = scannerName;
        this.csvBeadPoolManifestPath = csvBeadPoolManifestPath;
        this.regulatoryDesignation = regulatoryDesignation;
    }

    public String getRedIDatPath() {
        return redIDatPath;
    }

    public String getGreenIDatPath() {
        return greenIDatPath;
    }

    public String getChipManifestPath() {
        return chipManifestPath;
    }

    public String getBeadPoolManifestPath() {
        return beadPoolManifestPath;
    }

    public String getClusterFilePath() {
        return clusterFilePath;
    }

    public String getzCallThresholdsPath() {
        return zCallThresholdsPath;
    }

    public String getSampleId() {
        return sampleId;
    }

    public String getCollaboratorSampleId() {
        return collaboratorSampleId;
    }

    public String getSampleLsid() {
        return sampleLsid;
    }

    public String getGender() {
        return gender;
    }

    public String getParticipantId() {
        return participantId;
    }

    public String getResearchProjectId() {
        return researchProjectId;
    }

    public Boolean isPositiveControl() {
        return positiveControl;
    }

    public Boolean isNegativeControl() {
        return negativeControl;
    }

    public String getCallRateThreshold() {
        return callRateThreshold;
    }

    public String getGenderClusterFile() {
        return genderClusterFile;
    }

    public String getCollaboratorParticipantId() {
        return collaboratorParticipantId;
    }

    public String getProductOrderId() {
        return productOrderId;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductFamily() {
        return productFamily;
    }

    public String getProductPartNumber() {
        return productPartNumber;
    }

    public String getLabBatch() {
        return labBatch;
    }

    public Date getScanDate() {
        return scanDate;
    }

    public String getScannerName() {
        return scannerName;
    }

    public String getCsvBeadPoolManifestPath() {
        return csvBeadPoolManifestPath;
    }

    public String getRegulatoryDesignation() {
        return regulatoryDesignation;
    }
}
