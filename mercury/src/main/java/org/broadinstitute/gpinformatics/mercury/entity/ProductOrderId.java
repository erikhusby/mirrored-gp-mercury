package org.broadinstitute.gpinformatics.mercury.entity;

/**
 * An identifier for a ProductOrder in Athena.
 */
public class ProductOrderId {

    private String name;


    // not sure if this should be name, id, "barcode", jira ticket
    // it's whatever the business key is in athena for a product order
    public ProductOrderId(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
