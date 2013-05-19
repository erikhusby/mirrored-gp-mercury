package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;

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

    /**
     * Return one or more PDOs by ID.
     *
     * @param productOrderIds one or more PDO IDs, separated by commas
     * @param includeSamples if true, include each PDO's sample list in the result
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