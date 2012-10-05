package org.broadinstitute.gpinformatics.mercury.entity;

/**
 * An identifier for a ProductOrder in Athena.
 */
public class ProductOrderId {

    private String name;

    public ProductOrderId(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
