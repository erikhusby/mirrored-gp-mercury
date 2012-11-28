package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.broadinstitute.gpinformatics.infrastructure.common.BusinessKeyComparator;

/**
 * Denormalized data to feed to the upload preview table
 */
public class UploadPreviewData implements Comparable<UploadPreviewData> {

    private String orderJiraKey;

    private String productPartNumber;

    // we actually don't have title in the spreadsheet, but the upload handler could use a dao to look it up
    private String title;

    private String priceItemName;

    private Double totalCredits;

    private Double totalCharges;

    private static final BusinessKeyComparator businessKeyComparator = new BusinessKeyComparator();

    public UploadPreviewData(String orderJiraKey, String productPartNumber, String priceItemName, Double totalCredits, Double totalCharges) {
        this.orderJiraKey = orderJiraKey;
        this.productPartNumber = productPartNumber;
        this.priceItemName = priceItemName;
        this.totalCredits = totalCredits;
        this.totalCharges = totalCharges;
    }

    public String getOrderJiraKey() {
        return orderJiraKey;
    }

    public String getProductPartNumber() {
        return productPartNumber;
    }

    public String getPriceItemName() {
        return priceItemName;
    }

    public Double getTotalCredits() {
        return totalCredits;
    }

    public Double getTotalCharges() {
        return totalCharges;
    }

    @Override
    public int compareTo(UploadPreviewData uploadPreviewData) {
        CompareToBuilder builder = new CompareToBuilder();
        builder.append(getOrderJiraKey(), uploadPreviewData.getOrderJiraKey());
        builder.append(getProductPartNumber(), uploadPreviewData.getProductPartNumber());
        builder.append(getPriceItemName(), uploadPreviewData.getPriceItemName());
        return builder.build();
    }
}
