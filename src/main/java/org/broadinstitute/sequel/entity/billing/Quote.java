package org.broadinstitute.sequel.entity.billing;

/**
 * A quote, as per the quote server
 */
public interface Quote {

    public String getFundingSource();

    public String getCostObject();

    public String getPurchaseOrder();

    public void addWorkItem();

    public void updateWorkItem();

}
