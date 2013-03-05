package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
        public final String product;
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
            product = null;
        }

        public ProductOrderData(ProductOrder productOrder, boolean includeSamples) {
            title = productOrder.getTitle();
            id = productOrder.getBusinessKey();
            comments = productOrder.getComments();
            modifiedDate = productOrder.getModifiedDate();
            product = productOrder.getProduct().getPartNumber();
            status = productOrder.getOrderStatus();
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

    @XmlRootElement
    public static class ProductOrders {

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

    /**
     * Return one or more PDOs by ID.
     *
     * @param productOrderIds one or more PDO IDs, separated by commas
     * @param includeSamples if true, include each PDO's sample list in the reulst
     * @return the products
     */
    @GET
    @Path("pdo/{productOrderIds}")
    @Produces(MediaType.APPLICATION_XML)
    public ProductOrders findByIds(@PathParam("productOrderIds") String productOrderIds,
                                   @DefaultValue("false") @QueryParam("includeSamples") boolean includeSamples) {
        return new ProductOrders(productOrderDao.findListByBusinessKeyList(
                Arrays.asList(productOrderIds.split(","))), includeSamples);
    }

    /**
     * Method to GET the list of product orders. Optionally filter by a list of PDOs, or by a modify date.
     * <p/>
     * Only one of productOrderIds, modifiedAfter, or sampleIds can be used at a time. If no filter is provided,
     * all product orders are returned.
     *
     * @param productOrderIds one or more PDO IDs, separated by commas
     * @param modifiedAfter date to search to find PDOs modified after
     * @param sampleIds one or more sample IDs, separated by commas
     * @param includeSamples if true, include each PDO's sample list in the result
     * @return The product orders that match
     */
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public ProductOrders findOrders(@QueryParam("withId") String productOrderIds,
                                    @QueryParam("modifiedAfter") Date modifiedAfter,
                                    @QueryParam("withSample") String sampleIds,
                                    @DefaultValue("false") @QueryParam("includeSamples") boolean includeSamples) {
        List<ProductOrder> orders;
        if (!StringUtils.isEmpty(productOrderIds)) {
            orders = productOrderDao.findListByBusinessKeyList(Arrays.asList(productOrderIds.split(",")));
        } else if (sampleIds != null) {
            orders = productOrderDao.findBySampleBarcodes(sampleIds.split(","));
        } else if (modifiedAfter != null) {
            orders = productOrderDao.findModifiedAfter(modifiedAfter);
        } else {
            orders = productOrderDao.findAll();
        }
        return new ProductOrders(orders, includeSamples);
    }
}