package org.broadinstitute.gpinformatics.athena.boundary.orders;

import edu.mit.broad.bsp.core.datavo.workrequest.items.kit.PostReceiveOption;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.sample.MaterialInfoDto;
import org.broadinstitute.bsp.client.site.BspSiteManager;
import org.broadinstitute.bsp.client.site.Site;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequest;
import org.broadinstitute.bsp.client.workrequest.kit.KitTypeAllowanceSpecification;
import org.broadinstitute.gpinformatics.athena.boundary.projects.ApplicationValidationException;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductOrderJiraUtil;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.control.dao.samples.MaterialTypeDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKit;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKitDetail;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactory;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.KitType;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.NoJiraTransitionException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.security.Role;
import org.broadinstitute.gpinformatics.mercury.boundary.BucketException;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ParentVesselBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.LabVesselFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Restful webservice to list and create product orders.
 */
@Path("productOrders")
@Stateful
@RequestScoped
public class ProductOrderResource {
    private static final Log log = LogFactory.getLog(ProductOrderResource.class);

    private static final String SAMPLES_ADDED_RESPONSE = "Samples added";

    private static final String PDO_SAMPLE_STATUS = "pdoSampleStatus";

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private MaterialTypeDao materialTypeDao;

    @Inject
    private ProductOrderEjb productOrderEjb;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private LabVesselFactory labVesselFactory;

    @Inject
    private ProductOrderSampleDao pdoSampleDao;

    @Inject
    private JiraService jiraService;

    @Inject
    private BSPManagerFactory bspManagerFactory;

    /**
     * Should be used only by test code
     */
    void setProductOrderSampleDao(ProductOrderSampleDao pdoSampleDao) {
        this.pdoSampleDao = pdoSampleDao;
    }

    /**
     * Return the information on the newly created {@link ProductOrder} that has Draft status.
     * <p/>
     * It would be nice to only allow Project Managers and Administrators to create PDOs.  Use same {@link Role} names
     * as defined in the class (although I can't seem to be able to use the enum for the annotation.
     *
     * @param productOrderJaxB the document for the construction of the new {@link ProductOrder}
     *
     * @return the reference for the newly created {@link ProductOrder}
     */

    @POST
    @Path("createWithKitRequest")
    //@RolesAllowed("Mercury-ProjectManagers, Mercury-Administrators")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    public ProductOrderData createWithKitRequest(@Nonnull ProductOrderData productOrderJaxB)
            throws DuplicateTitleException, ApplicationValidationException, QuoteNotFoundException, NoSamplesException {
        ProductOrder productOrder = createProductOrder(productOrderJaxB);

        String categoryName = "";
        String materialTypeName = "";

        ResearchProject researchProject =
                researchProjectDao.findByBusinessKey(productOrderJaxB.getResearchProjectKey());
        MaterialType materialType = materialTypeDao.find(categoryName, materialTypeName);
        SampleKitWorkRequest.MoleculeType moleculeType = productOrderJaxB.getMoleculeType();

        MaterialInfoDto materialInfoDto = createMaterialInfoDTO(materialType, moleculeType);
        ProductOrderKitDetail kitDetail =
                createKitDetail(productOrderJaxB.getNumberOfSamples(), moleculeType, materialInfoDto);
        productOrder.setProductOrderKit(createProductOrderKit(researchProject, kitDetail));

        MessageCollection messageCollection = new MessageCollection();
        productOrderEjb.placeProductOrder(productOrder.getProductOrderId(), productOrder.getBusinessKey(),
                messageCollection);
        return new ProductOrderData(productOrder);
    }

    private ProductOrderKit createProductOrderKit(ResearchProject researchProject, ProductOrderKitDetail kitDetail)
            throws ApplicationValidationException {
        // Get the collection id from the research project so that we can get the site id.
        Long sampleCollectionId = getFirstCollectionId(researchProject);
        Long siteId = getSiteId(sampleCollectionId);
        if (siteId == null) {
            return null;
        }

        kitDetail.setProductOrderKitDetailId(kitDetail.getProductOrderKitDetailId());
        return new ProductOrderKit(sampleCollectionId, siteId, kitDetail);
    }

    private ProductOrderKitDetail createKitDetail(long numberOfSamples, SampleKitWorkRequest.MoleculeType moleculeType,
                                                  MaterialInfoDto materialInfoDto) {
        ProductOrderKitDetail kitDetail =
                new ProductOrderKitDetail(numberOfSamples, KitType.DNA_MATRIX, 87L, materialInfoDto);

        kitDetail.getPostReceiveOptions().add(PostReceiveOption.FLUIDIGM_FINGERPRINTING);
        if (moleculeType == SampleKitWorkRequest.MoleculeType.DNA) {
            kitDetail.getPostReceiveOptions().add(PostReceiveOption.PICO_RECEIVED);
        } else {
            kitDetail.getPostReceiveOptions().add(PostReceiveOption.BIOANALYZER);
        }
        return kitDetail;
    }

    private MaterialInfoDto createMaterialInfoDTO(MaterialType materialType,
                                                  SampleKitWorkRequest.MoleculeType moleculeType) {
        MaterialInfoDto materialInfoDto;
        if (moleculeType == SampleKitWorkRequest.MoleculeType.DNA) {
            materialInfoDto = new MaterialInfoDto(
                    KitTypeAllowanceSpecification.DNA_MATRIX_KIT.getText(), materialType.getName());
        } else {
            materialInfoDto = new MaterialInfoDto(
                    KitTypeAllowanceSpecification.RNA_MATRIX_KIT.getText(), materialType.getName());
        }
        return materialInfoDto;
    }

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
        ProductOrder productOrder = createProductOrder(productOrderJaxB);
        return new ProductOrderData(productOrder, true);
    }

    private ProductOrder createProductOrder(ProductOrderData productOrderJaxB)
            throws DuplicateTitleException, NoSamplesException, QuoteNotFoundException, ApplicationValidationException {
        ProductOrder productOrder = productOrderJaxB.convert(productOrderDao, researchProjectDao, productDao);

        // Figure out who called this so we can record the owner.
        BspUser user = bspUserList.getByUsername(productOrderJaxB.getUsername());
        if (user == null) {
            throw new ApplicationValidationException(
                    "Problem creating the product order, cannot find the user " + productOrderJaxB.getUsername());
        }

        try {
            productOrder.setCreatedBy(user.getUserId());
            productOrder.prepareToSave(user, ProductOrder.SaveType.CREATING);
            ProductOrderJiraUtil.placeOrder(productOrder, jiraService);
            productOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);

            // Not supplying add-ons at this point, just saving what we defined above and then flushing to make sure
            // any DB constraints have been enforced.
            productOrderDao.persist(productOrder);
            productOrderDao.flush();

            // Set the requisition name on the Jira referenced PDO.
            productOrderEjb.updateJiraIssue(productOrder);
        } catch (Exception e) {
            log.error(
                    user.getUsername() + " had a problem placing their product order " + productOrder.getBusinessKey(),
                    e);
            throw new ApplicationValidationException("Cannot create the product order - " + e.getMessage());
        }

        log.info(user.getUsername() + " created product order " + productOrder.getBusinessKey()
                 + " with an order status of " + productOrder.getOrderStatus().getDisplayName() + " that includes "
                 + productOrder.getSamples().size() + " samples");
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
     * <p/>
     * Method to add a set of samples to a PDO.
     * <p/>
     * <p/>
     * This will add all the sample ids to the specified PDO in the same order sent in.
     *
     * @param addSamplesToPdoBean the PDO and list of samples to add
     */
    @POST
    public String addSamplesToPdo(AddSamplesToPdoBean addSamplesToPdoBean) throws ApplicationValidationException {

        String pdoKey = addSamplesToPdoBean.getPdo();
        if (StringUtils.isBlank(pdoKey)) {
            throw new ApplicationValidationException("No product order id specified");
        }

        if (CollectionUtils.isEmpty(addSamplesToPdoBean.getParentVesselBeans())) {
            throw new ApplicationValidationException("No sample ids specified");
        }

        ProductOrder order = productOrderDao.findByBusinessKey(pdoKey);
        if (order == null) {
            throw new ApplicationValidationException("No product order found for id: " + pdoKey);
        }

        // Create the mercury vessel for the plate and each tube in the plate.
        BspUser bspUser = bspUserList.getByUsername(addSamplesToPdoBean.getUsername());
        if (bspUser == null) {
            throw new ApplicationValidationException(
                    "User: " + addSamplesToPdoBean.getUsername() + " is not found in Mercury");
        }

        List<LabVessel> vessels = labVesselFactory.buildLabVessels(
                addSamplesToPdoBean.parentVesselBeans, bspUser.getUsername(), new Date(), LabEventType.SAMPLE_PACKAGE);
        labVesselDao.persistAll(vessels);

        // Get all the sample ids
        List<ProductOrderSample> samplesToAdd = new ArrayList<>(addSamplesToPdoBean.getParentVesselBeans().size());
        for (ParentVesselBean parentVesselBean : addSamplesToPdoBean.getParentVesselBeans()) {
            samplesToAdd.add(new ProductOrderSample(parentVesselBean.getSampleId()));
        }

        try {
            // Add the samples.
            productOrderEjb.addSamples(bspUser, pdoKey, samplesToAdd, MessageReporter.UNUSED);
        } catch (ProductOrderEjb.NoSuchPDOException | IOException | NoJiraTransitionException | BucketException e) {
            throw new ApplicationValidationException("Could not add samples due to error: " + e);
        }

        return SAMPLES_ADDED_RESPONSE;
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
            productOrderDataList.add(new ProductOrderData(productOrder, includeSamples));
        }

        return new ProductOrders(productOrderDataList);
    }

    /**
     * Web service that takes in a list of PDOSamples and updates key information about them. Primarily it marks which
     * ones have had their primary price item billed, and whether or not the sample is at risk.
     * The initial client is Manhattan (see BSP-811). Whether or not the primary price item is billed is used by
     * Manhattan to determine whether data generation is complete for a given PDO sample.  Put another way, Manhattan
     * polls this service to see which PDO samples it should start assembling and analyzing.
     *
     * @see PDOSamples
     * @see PDOSample
     */
    @POST
    @Path(PDO_SAMPLE_STATUS)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public PDOSamples getPdoSampleStatus(PDOSamples pdoSamplePairs) {
        PDOSamples pdoSamplesResult = new PDOSamples();
        if (pdoSamplePairs != null) {
            try {
                List<ProductOrderSample> allPdoSamples = new ArrayList<>();
                Map<String, Set<String>> pdoToSamples = pdoSamplePairs.convertPdoSamplePairsListToMap();
                for (Map.Entry<String, Set<String>> pdoKeyToSamplesList : pdoToSamples.entrySet()) {
                    String pdoKey = pdoKeyToSamplesList.getKey();
                    Set<String> sampleNames = pdoKeyToSamplesList.getValue();
                    List<ProductOrderSample> pdoSamples = pdoSampleDao.findByOrderKeyAndSampleNames(pdoKey,
                            sampleNames);
                    allPdoSamples.addAll(pdoSamples);
                }
                pdoSamplesResult = pdoSamplePairs.buildOutputPDOSamplePairsFromInputAndQueryResults(allPdoSamples);
            } catch (Throwable t) {
                log.error("Failed to return PDO/Sample billing information.", t);
                pdoSamplesResult.addError(t.getMessage());
            }
        }
        return pdoSamplesResult;
    }

    private Long getSiteId(Long collectionId)
            throws ApplicationValidationException {
        // Get the first site Id. If there is not one, give an error.
        BspSiteManager siteManager = bspManagerFactory.createSiteManager();
        List<Site> sites = siteManager.getApplicableSites(collectionId).getResult();
        if (CollectionUtils.isEmpty(sites)) {
            throw new ApplicationValidationException(
                    "Could not find a site for the collection on this research project.");
        }

        return sites.get(0).getId();
    }

    /**
     * @return the first (and only) cohort on a product order
     */

    private long getFirstCollectionId(ResearchProject researchProject) {
        return researchProject.getCohorts()[0].getDatabaseId();
    }
}
