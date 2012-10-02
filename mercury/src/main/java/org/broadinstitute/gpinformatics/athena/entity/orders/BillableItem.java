package org.broadinstitute.gpinformatics.athena.entity.orders;

/**
 * Class to contain billing info. It encapsulates
 * the productName, a billing status and the number of times to bill for this billable item.
 *
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 10/1/12
 * Time: 11:31 AM
 */
public class BillableItem {

    private String productName;
    private BillingStatus billingStatus = BillingStatus.NotYetBilled;
    private Integer count;


    public BillableItem(final String productName, final BillingStatus billingStatus, final Integer count) {
        this.productName = productName;
        this.billingStatus = billingStatus;
        this.count = count;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(final String productName) {
        this.productName = productName;
    }

    public BillingStatus getBillingStatus() {
        return billingStatus;
    }

    public void setBillingStatus(final BillingStatus billingStatus) {
        this.billingStatus = billingStatus;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(final Integer count) {
        this.count = count;
    }
}
