package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;

import java.util.Date;

/**
 * Simple bean representing a row of data for the billing tracker.
 * <p>
 * The eventual goal of this bean is to hold all of the data needed by {@link SampleLedgerExporter} to write a row to
 * the spreadsheet. Doing so eliminates dependencies from SampleLedgerExporter, thereby making it easier to test. The
 * responsibility for gathering the data is gradually being migrated to {@link SampleLedgerExporterFactory}.
 */
public class SampleLedgerRow {

    /**
     * This is a temporary property to be removed once all of the individual pieces of data have their own properties.
     * Having this here avoids having to iterate across two parallel collections in {@link SampleLedgerExporter}.
     */
    private final ProductOrderSample productOrderSample;

    private final String sampleId;
    private final String collaboratorSampleId;
    private final String materialType;
    private final String riskText;
    private final String deliveryStatus;
    private final String productName;
    private final String productOrderKey;
    private final String productOrderTitle;
    private final String projectManagerName;
    private final Integer numberOfLanes;
    private final Date autoLedgerDate;
    private final Date workCompleteDate;

    public SampleLedgerRow(ProductOrderSample productOrderSample, String projectManagerName) {

        /*
         * Temporarily retain a reference to the productOrderSample until all of the individual pieces of data have
         * their own properties on SampleLedgerRow.
         */
        this.productOrderSample = productOrderSample;

        sampleId = productOrderSample.getSampleKey();
        collaboratorSampleId = productOrderSample.getSampleData().getCollaboratorsSampleName();
        materialType = productOrderSample.getSampleData().getMaterialType();
        riskText = productOrderSample.getRiskString();
        deliveryStatus = productOrderSample.getDeliveryStatus().getDisplayName();
        productName = productOrderSample.getProductOrder().getProduct().getProductName();
        productOrderKey = productOrderSample.getProductOrder().getBusinessKey();
        productOrderTitle = productOrderSample.getProductOrder().getTitle();
        this.projectManagerName = projectManagerName;
        numberOfLanes = productOrderSample.getProductOrder().getLaneCount();
        autoLedgerDate = productOrderSample.getLatestAutoLedgerTimestamp();
        workCompleteDate = productOrderSample.getWorkCompleteDate();
    }

    public SampleLedgerRow(ProductOrderSample productOrderSample, String sampleId, String collaboratorSampleId,
                           String materialType, String riskText, String deliveryStatus, String productName,
                           String productOrderKey, String productOrderTitle, String projectManagerName,
                           Integer numberOfLanes, Date autoLedgerDate, Date workCompleteDate) {
        this.productOrderSample = productOrderSample;
        this.sampleId = sampleId;
        this.collaboratorSampleId = collaboratorSampleId;
        this.materialType = materialType;
        this.riskText = riskText;
        this.deliveryStatus = deliveryStatus;
        this.productName = productName;
        this.productOrderKey = productOrderKey;
        this.productOrderTitle = productOrderTitle;
        this.projectManagerName = projectManagerName;
        this.numberOfLanes = numberOfLanes;
        this.autoLedgerDate = autoLedgerDate;
        this.workCompleteDate = workCompleteDate;
    }

    public ProductOrderSample getProductOrderSample() {
        return productOrderSample;
    }

    public String getSampleId() {
        return sampleId;
    }

    public String getCollaboratorSampleId() {
        return collaboratorSampleId;
    }

    public String getMaterialType() {
        return materialType;
    }

    public String getRiskText() {
        return riskText;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductOrderKey() {
        return productOrderKey;
    }

    public String getProductOrderTitle() {
        return productOrderTitle;
    }

    public String getProjectManagerName() {
        return projectManagerName;
    }

    public Integer getNumberOfLanes() {
        return numberOfLanes;
    }

    public Date getAutoLedgerDate() {
        return autoLedgerDate;
    }

    public Date getWorkCompleteDate() {
        return workCompleteDate;
    }
}
