package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalSamplesRequest implements Serializable  {

    private List<ExternalSampleContents> externalSampleContents;
    private String username;
    private String collection;
    private long collectionId;
    private long siteId;
    private String labelFormat;
    private String formatType;
    private String shippingMethodType;
    private String shippingNotes;
    private String trackingNumber;
    private String conditionShipment;
    private String materialType;
    private String originalMaterialType;
    private String receptacleType;

    public ExternalSamplesRequest() {
    }

    public ExternalSamplesRequest(List<ExternalSampleContents> externalSampleContents, String username) {
        this.externalSampleContents = externalSampleContents;
        this.username = username;
    }

    public List<ExternalSampleContents> getExternalSampleContents() {
        return externalSampleContents;
    }

    public void setExternalSampleContents(List<ExternalSampleContents> externalSampleContents) {
        this.externalSampleContents = externalSampleContents;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public long getSiteId() {
        return siteId;
    }

    public void setSiteId(long siteId) {
        this.siteId = siteId;
    }

    public String getShippingMethodType() {
        return shippingMethodType;
    }

    public void setShippingMethodType(String shippingMethodType) {
        this.shippingMethodType = shippingMethodType;
    }

    public String getShippingNotes() {
        return shippingNotes;
    }

    public void setShippingNotes(String shippingNotes) {
        this.shippingNotes = shippingNotes;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getConditionShipment() {
        return conditionShipment;
    }

    public void setConditionShipment(String conditionShipment) {
        this.conditionShipment = conditionShipment;
    }

    public String getLabelFormat() {
        return labelFormat;
    }

    public void setLabelFormat(String labelFormat) {
        this.labelFormat = labelFormat;
    }

    public String getFormatType() {
        return formatType;
    }

    public void setFormatType(String formatType) {
        this.formatType = formatType;
    }

    public long getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(long collectionId) {
        this.collectionId = collectionId;
    }

    public String getMaterialType() {
        return materialType;
    }

    public void setMaterialType(String materialType) {
        this.materialType = materialType;
    }

    public String getOriginalMaterialType() {
        return originalMaterialType;
    }

    public void setOriginalMaterialType(String originalMaterialType) {
        this.originalMaterialType = originalMaterialType;
    }

    public String getReceptacleType() {
        return receptacleType;
    }

    public void setReceptacleType(String receptacleType) {
        this.receptacleType = receptacleType;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExternalSampleContents implements Serializable {
        /**
         * This can be either the Collaborator Sample ID or Collaborator Participant ID depending on the receipt process.
         */
        private String collaboratorId;

        private float volume;

        private String container;

        private String position;

        private String organism;

        private long organismId;

        private String receptacleType;

        private String labelFormat;

        private String materialType;

        private String originalMaterialType;

        private String formatType;

        private String scanType;

        private String barcode;

        public ExternalSampleContents() {
        }

        public String getCollaboratorId() {
            return collaboratorId;
        }

        public void setCollaboratorId(String collaboratorId) {
            this.collaboratorId = collaboratorId;
        }

        public float getVolume() {
            return volume;
        }

        public void setVolume(float volume) {
            this.volume = volume;
        }

        public String getContainer() {
            return container;
        }

        public void setContainer(String container) {
            this.container = container;
        }

        public String getPosition() {
            return position;
        }

        public void setPosition(String position) {
            this.position = position;
        }

        public String getOrganism() {
            return organism;
        }

        public void setOrganism(String organism) {
            this.organism = organism;
        }

        public long getOrganismId() {
            return organismId;
        }

        public void setOrganismId(long organismId) {
            this.organismId = organismId;
        }

        public String getReceptacleType() {
            return receptacleType;
        }

        public void setReceptacleType(String receptacleType) {
            this.receptacleType = receptacleType;
        }

        public String getLabelFormat() {
            return labelFormat;
        }

        public void setLabelFormat(String labelFormat) {
            this.labelFormat = labelFormat;
        }

        public String getMaterialType() {
            return materialType;
        }

        public void setMaterialType(String materialType) {
            this.materialType = materialType;
        }

        public String getOriginalMaterialType() {
            return originalMaterialType;
        }

        public void setOriginalMaterialType(String originalMaterialType) {
            this.originalMaterialType = originalMaterialType;
        }

        public String getFormatType() {
            return formatType;
        }

        public void setFormatType(String formatType) {
            this.formatType = formatType;
        }

        public String getScanType() {
            return scanType;
        }

        public String getBarcode() {
            return barcode;
        }

        public void setBarcode(String barcode) {
            this.barcode = barcode;
        }
    }
}
