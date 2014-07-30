package org.broadinstitute.gpinformatics.athena.boundary.orders;

import java.util.Date;

/**
 * Simple bean representing a row of data for the billing tracker.
 * <p>
 * The eventual goal of this bean is to hold all of the data needed by {@link SampleLedgerExporter} to write a row to the
 * spreadsheet. Doing so eliminates dependencies from SampleLedgerExporter, thereby making it easier to test. The
 * responsibility for gathering the data is gradually being migrated to {@link SampleLedgerExporterFactory}.
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
    private Integer numberOfLanes;
    private Date autoLedgerDate;
    private Date workCompleteDate;

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

    public Integer getNumberOfLanes() {
        return numberOfLanes;
    }

    public void setNumberOfLanes(Integer numberOfLanes) {
        this.numberOfLanes = numberOfLanes;
    }

    public Date getAutoLedgerDate() {
        return autoLedgerDate;
    }

    public void setAutoLedgerDate(Date autoLedgerDate) {
        this.autoLedgerDate = autoLedgerDate;
    }

    public Date getWorkCompleteDate() {
        return workCompleteDate;
    }

    public void setWorkCompleteDate(Date workCompleteDate) {
        this.workCompleteDate = workCompleteDate;
    }
}
