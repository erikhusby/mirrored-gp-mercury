package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.broadinstitute.gpinformatics.infrastructure.common.BusinessKeyComparator;

/**
 * Denormalized data to feed to the upload preview table
 */
@Deprecated
public class UploadPreviewData implements Comparable<UploadPreviewData> {

    private final String orderJiraKey;

    private final String productPartNumber;

    // we actually don't have title in the spreadsheet, but the upload handler could use a dao to look it up
    private String title;

    private final String priceItemName;

    private final double totalNewCredits;

    private final double totalNewCharges;

    private static final BusinessKeyComparator businessKeyComparator = new BusinessKeyComparator();

    public UploadPreviewData(String orderJiraKey, String productPartNumber, String priceItemName, double totalNewCharges, double totalNewCredits) {
        this.orderJiraKey = orderJiraKey;
        this.productPartNumber = productPartNumber;
        this.priceItemName = priceItemName;
        this.totalNewCredits = totalNewCredits;
        this.totalNewCharges = totalNewCharges;
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

    public double getTotalNewCredits() {
        return totalNewCredits;
    }

    public double getTotalNewCharges() {
        return totalNewCharges;
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
