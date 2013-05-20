package org.broadinstitute.gpinformatics.athena.boundary.orders;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;


/**
 * JAXB DTO representing a collection of zero or more Product Orders.
 */
@SuppressWarnings("UnusedDeclaration")
@XmlRootElement
public class ProductOrders {

    @XmlElement(name = "order")
    public List<ProductOrderData> orders = new ArrayList<ProductOrderData>();

    /** For JAXB. */
    public ProductOrders() {
    }

    public ProductOrders(List<ProductOrderData> productOrderDataList) {
        orders = productOrderDataList;
    }

    public List<ProductOrderData> getOrders() {
        return orders;
    }

    public void setOrders(List<ProductOrderData> orders) {
        this.orders = orders;
    }
}
