package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.broadinstitute.gpinformatics.infrastructure.common.BusinessKeyComparator;

/**
 * Denormalized data to feed to the upload preview table
 */
public class UploadPreviewData implements Comparable<UploadPreviewData> {

    private final String orderJiraKey;

    private final String productPartNumber;

    // we actually don't have title in the spreadsheet, but the upload handler could use a dao to look it up
    private String title;

    private final String priceItemName;

    private final double totalCredits;

    private final double totalCharges;

    private static final BusinessKeyComparator businessKeyComparator = new BusinessKeyComparator();

    public UploadPreviewData(String orderJiraKey, String productPartNumber, String priceItemName, double totalCredits, double totalCharges) {
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

    public double getTotalCredits() {
        return totalCredits;
    }

    public double getTotalCharges() {
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
