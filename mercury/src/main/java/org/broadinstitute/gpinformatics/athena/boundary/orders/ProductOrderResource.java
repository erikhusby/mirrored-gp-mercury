package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.*;

/**
 * Restful webservice to list product orders.
 */

@Path("productOrders")
@Stateful
@RequestScoped
public class ProductOrderResource {

    @Inject
    private ProductOrderDao productOrderDao;

    @XmlRootElement
    public static class ProductOrderData {
        public final String title;
        public final String id;
        public final String comments;
        public final Date modifiedDate;
        public final ProductOrder.OrderStatus status;
        @XmlElementWrapper
        @XmlElement(name = "sample")
        public final List<String> samples;

        // Required by JAXB.
        ProductOrderData() {
            title = null;
            id = null;
            comments = null;
            modifiedDate = null;
            samples = null;
            status = null;
        }

        public ProductOrderData(ProductOrder productOrder) {
            title = productOrder.getTitle();
            id = productOrder.getJiraTicketKey();
            comments = productOrder.getComments();
            samples = new ArrayList<String>(productOrder.getSamples().size());
            for (ProductOrderSample sample : productOrder.getSamples()) {
                samples.add(sample.getSampleName());
            }
            modifiedDate = productOrder.getModifiedDate();
            status = productOrder.getOrderStatus();
        }
    }

    @XmlRootElement
    public static class ProductOrders {

        @XmlElementWrapper
        @XmlElement(name = "order")
        public final List<ProductOrderData> orders;

        public ProductOrders() {
            orders = Collections.emptyList();
        }

        public ProductOrders(List<ProductOrder> orders) {
            this.orders = new ArrayList<ProductOrderData>(orders.size());
            for (ProductOrder order : orders) {
                this.orders.add(new ProductOrderData(order));
            }
        }
    }

    @GET
    @Path("pdo/{productOrderIds}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public ProductOrders findById(@PathParam("productOrderIds") String productOrderIds) {
        return new ProductOrders(productOrderDao.findListByBusinessKeyList(
                Arrays.asList(productOrderIds.split(","))));
    }

    @GET
    @Path("sample/{sample}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public ProductOrders findBySample(@PathParam("sample") String sampleName) {
        // TODO: find all PDOs by sample.

        return new ProductOrders();
    }

    /**
     * Method to GET the list of research projects. Optionally filter this by the user who created them if the creator
     * param is supplied.
     *
     * @return The research projects that match
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public ProductOrders findAll() {
        return new ProductOrders(productOrderDao.findAll());
    }
}