package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;


/**
 * JAXB DTO representing a collection of zero or more Product Orders.
 */
@XmlRootElement
public class ProductOrders {

    @XmlElement(name = "order")
    public final List<ProductOrderData> orders;

    public ProductOrders() {
        orders = null;
    }

    public ProductOrders(List<ProductOrder> orders, boolean includeSamples) {
        this.orders = new ArrayList<ProductOrderData>(orders.size());
        for (ProductOrder order : orders) {
            // Result doesn't include Draft orders.
            if (!order.isDraft()) {
                this.orders.add(new ProductOrderData(order, includeSamples));
            }
        }
    }
}
