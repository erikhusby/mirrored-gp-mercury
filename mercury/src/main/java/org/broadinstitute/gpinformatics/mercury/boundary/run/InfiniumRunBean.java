package org.broadinstitute.gpinformatics.mercury.boundary.run;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

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
    private String collaboratorSampleId;
    private String sampleLsid;
    private String gender;
    // todo jmt this or something more REST-like?
    private String error;

    /** For JAXB. */
    public InfiniumRunBean() {
    }

    public InfiniumRunBean(String redIDatPath, String greenIDatPath, String chipManifestPath,
            String beadPoolManifestPath, String clusterFilePath, String zCallThresholdsPath,
            String collaboratorSampleId, String sampleLsid, String gender) {
        this.redIDatPath = redIDatPath;
        this.greenIDatPath = greenIDatPath;
        this.chipManifestPath = chipManifestPath;
        this.beadPoolManifestPath = beadPoolManifestPath;
        this.clusterFilePath = clusterFilePath;
        this.zCallThresholdsPath = zCallThresholdsPath;
        this.collaboratorSampleId = collaboratorSampleId;
        this.sampleLsid = sampleLsid;
        this.gender = gender;
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

    public String getCollaboratorSampleId() {
        return collaboratorSampleId;
    }

    public String getSampleLsid() {
        return sampleLsid;
    }

    public String getGender() {
        return gender;
    }
}
