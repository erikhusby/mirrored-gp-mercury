package org.broadinstitute.gpinformatics.athena.boundary.orders;

/**
 * Simple bean representing a row of data for the billing tracker.
 */
public class SampleLedgerRow {

    private String sampleId;
    private String collaboratorSampleId;
    private String materialType;
    private String riskText;
    private String deliveryStatus;
    private String productName;
    private String productOrderKey;
    private String productOrderTitle;
    private String projectManagerName;
    private Double numberOfLanes;

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getCollaboratorSampleId() {
        return collaboratorSampleId;
    }

    public void setCollaboratorSampleId(String collaboratorSampleId) {
        this.collaboratorSampleId = collaboratorSampleId;
    }

    public String getMaterialType() {
        return materialType;
    }

    public void setMaterialType(String materialType) {
        this.materialType = materialType;
    }

    public String getRiskText() {
        return riskText;
    }

    public void setRiskText(String riskText) {
        this.riskText = riskText;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductOrderKey() {
        return productOrderKey;
    }

    public void setProductOrderKey(String productOrderKey) {
        this.productOrderKey = productOrderKey;
    }

    public String getProductOrderTitle() {
        return productOrderTitle;
    }

    public void setProductOrderTitle(String productOrderTitle) {
        this.productOrderTitle = productOrderTitle;
    }

    public String getProjectManagerName() {
        return projectManagerName;
    }

    public void setProjectManagerName(String projectManagerName) {
        this.projectManagerName = projectManagerName;
    }

    public Double getNumberOfLanes() {
        return numberOfLanes;
    }

    public void setNumberOfLanes(Double numberOfLanes) {
        this.numberOfLanes = numberOfLanes;
    }
}
