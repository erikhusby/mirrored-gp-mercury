package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * JAXB DTO representing a single Product Order.
 */
@XmlRootElement
public class ProductOrderData {
    public final String title;
    public final String id;
    public final String comments;
    public final Date modifiedDate;
    public final String product;
    public final String status;
    @XmlElementWrapper
    @XmlElement(name = "sample")
    public final List<String> samples;

    @SuppressWarnings("UnusedDeclaration")
    // Required by JAXB.
    ProductOrderData() {
        title = null;
        id = null;
        comments = null;
        modifiedDate = null;
        samples = null;
        status = null;
        product = null;
    }

    public ProductOrderData(ProductOrder productOrder, boolean includeSamples) {
        title = productOrder.getTitle();
        id = productOrder.getBusinessKey();
        comments = productOrder.getComments();
        modifiedDate = productOrder.getModifiedDate();
        product = productOrder.getProduct().getPartNumber();
        status = productOrder.getOrderStatus().name();
        if (includeSamples) {
            samples = new ArrayList<String>(productOrder.getSamples().size());
            for (ProductOrderSample sample : productOrder.getSamples()) {
                samples.add(sample.getSampleName());
            }
        } else {
            samples = null;
        }
    }
}
