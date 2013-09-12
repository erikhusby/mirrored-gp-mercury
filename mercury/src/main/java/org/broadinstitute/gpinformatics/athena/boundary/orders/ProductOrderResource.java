package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.security.Role;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;

import javax.annotation.Nonnull;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.net.URI;
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

    @Inject
    ProductOrderEjb productOrderEjb;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    ResearchProjectDao researchProjectDao;

    @Context
    private UriInfo uriInfo;

    /**
     * Return the information on the newly created {@link ProductOrder} that has Draft status.
     * <p/>
     * It would be nice to only allow Project Managers and Administrators to create PDOs.  Use same {@link Role} names
     * as defined in the class (although I can't seem to be able to use the enum for the annotation.
     *
     * @param productOrderJaxB the document for the construction of the new {@link ProductOrder}
     *
     * @return the reference for the newly created {@link ProductOrder}
     *
     * @throws DuplicateTitleException
     * @throws NoSamplesException
     * @throws QuoteNotFoundException
     */
    @POST
    @Path("create")
    //@RolesAllowed("Mercury-ProjectManagers, Mercury-Administrators")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    public ProductOrderData create(@Nonnull ProductOrderData productOrderJaxB)
            throws DuplicateTitleException, NoSamplesException, QuoteNotFoundException {
        if (productOrderJaxB == null) {
            throw new InformaticsServiceException(("No data found to define the new Product Order"));
        }

        ProductOrder productOrder = convert(productOrderJaxB);

        // Figure out who called this so we can record the owner.
        BspUser user = bspUserList.getByUsername(productOrderJaxB.getUsername());
        if (user != null) {
            productOrder.setCreatedBy(user.getUserId());
        }

        productOrder.prepareToSave(user, ProductOrder.IS_CREATING);
        productOrder.setOrderStatus(ProductOrder.OrderStatus.Draft);

        // Not supplying samples and add-ons at this point, just saving what we defined above.
        productOrderDao.persist(productOrder);

        /*
        // If returning the URI to the resource we made, then return type needs to be Response.
        URI productOrderUri =
                uriInfo.getAbsolutePathBuilder().path(productOrder.getProductOrderId().toString()).build();
        return Response.created(productOrderUri).build();
        */

        ProductOrderData newProductOrderData = new ProductOrderData(productOrder);
        return newProductOrderData;
    }

    /**
     * Try to convert the JAXB XML data into a {@link ProductOrder}.
     *
     * @param productOrderData The JAXB XML element
     *
     * @return the populated {@link ProductOrder}
     */
    private ProductOrder convert(ProductOrderData productOrderData) {
        //QName qname = new QName("http://mercury.broadinstitute.org/Mercury", "productOrder");
        //JAXBElement<ProductOrder> productOrderJaxB = new JAXBElement(qname, ProductOrderData.class, productOrderData);
        //return productOrderJaxB.getValue();

        ProductOrder productOrder = new ProductOrder();
        productOrder.setTitle(productOrderData.getTitle());
        productOrder.setComments(productOrderData.getComments());
        productOrder.setQuoteId(productOrderData.getQuoteId());

        if (productOrderData.getResearchProjectId() != null) {
            ResearchProject researchProject =
                    researchProjectDao.findByBusinessKey(productOrderData.getResearchProjectId());
            if (researchProject != null) {
                productOrder.setResearchProject(researchProject);
            }
        }

        return productOrder;
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
}