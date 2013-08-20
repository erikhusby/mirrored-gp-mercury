package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;

import javax.annotation.Nonnull;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
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


    private ProductOrders buildProductOrdersFromList(@Nonnull List<ProductOrder> productOrderList,
                                                     boolean includeSamples) {

        List<ProductOrderData> productOrderDataList = new ArrayList<>(productOrderList.size());

        for (ProductOrder productOrder : productOrderList) {
            ProductOrderData productOrderData = new ProductOrderData();
            productOrderData.setTitle(productOrder.getTitle());
            productOrderData.setId(productOrder.getBusinessKey());
            productOrderData.setComments(productOrder.getComments());
            productOrderData.setPlacedDate(productOrder.getPlacedDate());
            productOrderData.setModifiedDate(productOrder.getModifiedDate());
            productOrderData.setProduct(productOrder.getProduct().getPartNumber());
            productOrderData.setProductName(productOrder.getProduct().getName());
            productOrderData.setStatus(productOrder.getOrderStatus().name());
            productOrderData.setAggregationDataType(productOrder.getProduct().getAggregationDataType());
            productOrderData.setResearchProjectId(productOrder.getResearchProject().getBusinessKey());
            productOrderData.setQuoteId(productOrder.getQuoteId());

            if (includeSamples) {
                List<String> sampleNames = new ArrayList<>(productOrder.getSamples().size());
                for (ProductOrderSample sample : productOrder.getSamples()) {
                    sampleNames.add(sample.getSampleName());
                }
                productOrderData.setSamples(sampleNames);
            } else {
                // Explicit set of null into a List<String> field, this duplicates what the existing code was doing when
                // includeSamples = false.  Is the JAXB behavior with an empty List undesirable?
                productOrderData.setSamples(null);
            }

            productOrderDataList.add(productOrderData);
        }

        return new ProductOrders(productOrderDataList);
    }

    /**
     * Return one or more PDOs by ID.
     *
     * @param productOrderIds one or more PDO IDs, separated by commas
     * @param includeSamples  if true, include each PDO's sample list in the result
     *
     * @return the products
     */
    @GET
    @Path("pdo/{productOrderIds}")
    @Produces(MediaType.APPLICATION_XML)
    public ProductOrders findByIds(@PathParam("productOrderIds") String productOrderIds,
                                   @DefaultValue("false") @QueryParam("includeSamples") boolean includeSamples) {

        List<String> businessKeyList = Arrays.asList(productOrderIds.split(","));

        List<ProductOrder> productOrderList = includeSamples ?
                // Do use the FetchSpec for samples if we are to include sample data as otherwise this data would
                // be singleton selected.
                productOrderDao.findListByBusinessKeyList(businessKeyList, ProductOrderDao.FetchSpec.Samples) :
                productOrderDao.findListByBusinessKeyList(businessKeyList);

        return buildProductOrdersFromList(productOrderList, includeSamples);
    }

    /**
     * Method to GET the list of product orders. Optionally filter by a list of PDOs, or by a modify date.
     * <p/>
     * Only one of productOrderIds, modifiedAfter, or sampleIds can be used at a time. If no filter is provided,
     * all product orders are returned.
     *
     * @param productOrderIds one or more PDO IDs, separated by commas
     * @param modifiedAfter   date to search to find PDOs modified after
     * @param sampleIds       one or more sample IDs, separated by commas
     * @param includeSamples  if true, include each PDO's sample list in the result
     *
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
            List<String> businessKeyList = Arrays.asList(productOrderIds.split(","));
            orders = includeSamples ?
                    productOrderDao.findListByBusinessKeyList(businessKeyList, ProductOrderDao.FetchSpec.Samples) :
                    productOrderDao.findListByBusinessKeyList(businessKeyList);
        } else if (sampleIds != null) {
            // There isn't currently a fetchspec version of findBySampleBarcodes so this will take the singleton select
            // hit.
            orders = productOrderDao.findBySampleBarcodes(sampleIds.split(","));
        } else if (modifiedAfter != null) {
            // There isn't currently a fetchspec version of findModifiedAfter so this will take the singleton select hit.
            orders = productOrderDao.findModifiedAfter(modifiedAfter);
        } else {
            orders = includeSamples ?
                    productOrderDao.findAll(ProductOrderDao.FetchSpec.Samples) :
                    productOrderDao.findAll();
        }

        // Filter draft orders.
        Iterator<ProductOrder> iterator = orders.iterator();
        while (iterator.hasNext()) {
            ProductOrder productOrder = iterator.next();
            if (productOrder.getOrderStatus() == ProductOrder.OrderStatus.Draft) {
                iterator.remove();
            }
        }

        return buildProductOrdersFromList(orders, includeSamples);
    }
}