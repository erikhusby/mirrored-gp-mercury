package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.projects.ApplicationValidationException;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.security.Role;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;

import javax.annotation.Nonnull;
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
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Restful webservice to list and create product orders.
 */
@Path("productOrders")
@Stateful
@RequestScoped
public class ProductOrderResource {
    final private static Log log = LogFactory.getLog(ProductOrderResource.class);

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    ProductOrderEjb productOrderEjb;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    ResearchProjectDao researchProjectDao;

    @Inject
    ProductDao productDao;

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
            throws DuplicateTitleException, NoSamplesException, QuoteNotFoundException, ApplicationValidationException {
        ProductOrder productOrder = convert(productOrderJaxB);

        // Figure out who called this so we can record the owner.
        BspUser user = bspUserList.getByUsername(productOrderJaxB.getUsername());
        if (user == null) {
            throw new ApplicationValidationException(
                    "Problem creating the product order, cannot find the user " + productOrderJaxB.getUsername());
        }

        try {
            productOrder.setCreatedBy(user.getUserId());
            productOrder.prepareToSave(user, ProductOrder.SaveType.CREATING);
            productOrder.placeOrder();
            productOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);

            // Not supplying add-ons at this point, just saving what we defined above and then flushing to make sure
            // any DB constraints have been enforced.
            productOrderDao.persist(productOrder);
            productOrderDao.flush();
        } catch (Exception e) {
            log.error(
                    user.getUsername() + " had a problem placing their product order " + productOrder.getBusinessKey(),
                    e);
            throw new ApplicationValidationException("Cannot create the product order - " + e.getMessage());
        }

        log.info(user.getUsername() + " created product order " + productOrder.getBusinessKey()
                 + " with an order status of " + productOrder.getOrderStatus().getDisplayName() + " that includes "
                 + productOrder.getSamples().size() + " samples");

        return new ProductOrderData(productOrder);
    }

    /**
     * Try to convert the JAXB XML data into a {@link ProductOrder} and do some validation while converting.
     *
     * @param productOrderData The JAXB XML element
     *
     * @return the populated {@link ProductOrder}
     */
    private ProductOrder convert(ProductOrderData productOrderData)
            throws DuplicateTitleException, NoSamplesException, QuoteNotFoundException, ApplicationValidationException {
        if (productOrderData == null) {
            throw new InformaticsServiceException(("No data found to define the new product order"));
        }

        ProductOrder productOrder = new ProductOrder();

        // Make sure the title/name is supplied and unique
        if (StringUtils.isBlank(productOrderData.getTitle())) {
            throw new ApplicationValidationException("Title required for Product Order");
        }

        // Make sure the title
        if (productOrderDao.findByTitle(productOrderData.getTitle()) != null) {
            throw new DuplicateTitleException();
        }

        productOrder.setTitle(productOrderData.getTitle());
        productOrder.setComments(productOrderData.getComments());
        productOrder.setQuoteId(productOrderData.getQuoteId());

        // Find the product by the product name.
        if (!StringUtils.isBlank(productOrderData.getProductName())) {
            Product product = productDao.findByName(productOrderData.getProductName());
            productOrder.setProduct(product);
        }

        if (!StringUtils.isBlank(productOrderData.getResearchProjectKey())) {
            ResearchProject researchProject =
                    researchProjectDao.findByBusinessKey(productOrderData.getResearchProjectKey());

            // Make sure the required research project is present.
            if (researchProject == null) {
                throw new ApplicationValidationException(
                        "The required research project is not associated to the product order");
            }

            productOrder.setResearchProject(researchProject);
        }

        // Find and add the product order samples.
        List<ProductOrderSample> productOrderSamples = new ArrayList<>();
        for (String sample : productOrderData.getSamples()) {
            productOrderSamples.add(new ProductOrderSample(sample));
        }

        // Make sure the required sample(s) are present.
        if (productOrderSamples.isEmpty()) {
            throw new NoSamplesException();
        }

        productOrder.addSamples(productOrderSamples);

        // Set the requisition key so one can look up the requisition in the Portal.
        productOrder.setRequisitionKey(productOrderData.getRequisitionKey());

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
                productOrderDao.findListByBusinessKeys(businessKeyList, ProductOrderDao.FetchSpec.SAMPLES) :
                productOrderDao.findListByBusinessKeys(businessKeyList);

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
                    productOrderDao.findListByBusinessKeys(businessKeyList, ProductOrderDao.FetchSpec.SAMPLES) :
                    productOrderDao.findListByBusinessKeys(businessKeyList);
        } else if (sampleIds != null) {
            // There isn't currently a fetchspec version of findBySampleBarcodes so this will take the singleton select
            // hit.
            orders = productOrderDao.findBySampleBarcodes(sampleIds.split(","));
        } else if (modifiedAfter != null) {
            // There isn't currently a fetchspec version of findModifiedAfter so this will take the singleton select hit.
            orders = productOrderDao.findModifiedAfter(modifiedAfter);
        } else {
            orders = includeSamples ?
                    productOrderDao.findAll(ProductOrderDao.FetchSpec.SAMPLES) :
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
            productOrderData.setResearchProjectKey(productOrder.getResearchProject().getBusinessKey());
            productOrderData.setQuoteId(productOrder.getQuoteId());

            if (includeSamples) {
                List<String> sampleNames = new ArrayList<>(productOrder.getSamples().size());
                for (ProductOrderSample sample : productOrder.getSamples()) {
                    sampleNames.add(sample.getName());
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