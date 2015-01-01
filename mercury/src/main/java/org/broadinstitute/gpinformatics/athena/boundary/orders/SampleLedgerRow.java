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

    private final ProductOrderSample productOrderSample;
    private final String projectManagerName;

    public SampleLedgerRow(ProductOrderSample productOrderSample, String projectManagerName) {
        this.productOrderSample = productOrderSample;
        this.projectManagerName = projectManagerName;
    }

    public ProductOrderSample getProductOrderSample() {
        return productOrderSample;
    }

    public String getSampleId() {
        return productOrderSample.getSampleKey();
    }

    public String getCollaboratorSampleId() {
        return productOrderSample.getSampleData().getCollaboratorsSampleName();
    }

    public String getMaterialType() {
        return productOrderSample.getSampleData().getMaterialType();
    }

    public String getRiskText() {
        return productOrderSample.getRiskString();
    }

    public String getDeliveryStatus() {
        return productOrderSample.getDeliveryStatus().getDisplayName();
    }

    public String getProductName() {
        return productOrderSample.getProductOrder().getProduct().getProductName();
    }

    public String getProductOrderKey() {
        return productOrderSample.getProductOrder().getBusinessKey();
    }

    public String getProductOrderTitle() {
        return productOrderSample.getProductOrder().getTitle();
    }

    public String getProjectManagerName() {
        return projectManagerName;
    }

    public int getNumberOfLanes() {
        return productOrderSample.getProductOrder().getLaneCount();
    }

    public Date getAutoLedgerDate() {
        return productOrderSample.getLatestAutoLedgerTimestamp();
    }

    public Date getWorkCompleteDate() {
        return productOrderSample.getWorkCompleteDate();
    }
}
