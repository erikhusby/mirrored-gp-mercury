package org.broadinstitute.gpinformatics.athena.boundary.orders;

import edu.mit.broad.bsp.core.datavo.workrequest.items.kit.MaterialInfo;
import edu.mit.broad.bsp.core.datavo.workrequest.items.kit.PostReceiveOption;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.collection.SampleCollection;
import org.broadinstitute.bsp.client.sample.MaterialInfoDto;
import org.broadinstitute.bsp.client.site.BspSiteManager;
import org.broadinstitute.bsp.client.site.Site;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequest;
import org.broadinstitute.bsp.client.workrequest.kit.KitTypeAllowanceSpecification;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingEjb;
import org.broadinstitute.gpinformatics.athena.boundary.products.InvalidProductException;
import org.broadinstitute.gpinformatics.athena.boundary.products.ProductEjb;
import org.broadinstitute.gpinformatics.athena.boundary.projects.ApplicationValidationException;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductOrderJiraUtil;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.control.dao.work.WorkCompleteMessageDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKit;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKitDetail;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.work.WorkCompleteMessage;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPGroupCollectionList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSiteList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactory;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.KitType;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService;
import org.broadinstitute.gpinformatics.infrastructure.security.Role;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ParentVesselBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.LabVesselFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

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
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
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
    // ID for species: Human is 1 in the database
    private static final long HUMAN = 1;

    private static final ProductOrderDao.FetchSpec[] INCLUDE_SAMPLES_FETCH_SPECS =
            new ProductOrderDao.FetchSpec[]{ProductOrderDao.FetchSpec.SAMPLES, ProductOrderDao.FetchSpec.RISK_ITEMS,
                    ProductOrderDao.FetchSpec.PRODUCT_ORDER_KIT};

    // Include product order kit so getting the work request ID doesn't invoke a lazy load.
    private static final ProductOrderDao.FetchSpec DEFAULT_FETCH_SPECS = ProductOrderDao.FetchSpec.PRODUCT_ORDER_KIT;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ProductOrderEjb productOrderEjb;

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

    @Inject
    private BSPGroupCollectionList bspGroupCollectionList;

    @Inject
    private BSPSiteList bspSiteList;

    @Inject
    private BillingEjb billingEjb;

    @Inject
    private WorkCompleteMessageDao workCompleteMessageDao;

    @Inject
    private UserBean userBean;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private ProductOrderJiraUtil productOrderJiraUtil;

    @Inject
    private ProductEjb productEjb;

    @Inject
    private SapIntegrationService sapService;

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
     * @param productOrderData The document for the construction of the new {@link ProductOrder}. This MUST include the
     *                         site id even though it is optional for the ProductOrderData to support the other create
     *                         method here.
     *
     * @return the reference for the newly created {@link ProductOrder}
     */
    @POST
    @Path("createWithKitRequest")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    public ProductOrderData createWithKitRequest(@Nonnull ProductOrderData productOrderData)
            throws DuplicateTitleException, ApplicationValidationException, NoSamplesException,
            WorkRequestCreationException, InvalidProductException {

        userBean.login(productOrderData.getUsername());
        if(userBean.getBspUser() == UserBean.UNKNOWN) {
            throw new ResourceException("A valid Username is required to complete this request",
                    Response.Status.UNAUTHORIZED);
        }
        ProductOrder productOrder = productOrderEjb.createProductOrder(productOrderData);

        ResearchProject researchProject =
                researchProjectDao.findByBusinessKey(productOrderData.getResearchProjectId());

        SampleCollection sampleCollection = getFirstCollection(researchProject);

        // If we have a valid site, we can set up the product order kit.
        Site site = bspSiteList.getById(productOrderData.getSiteId());
        if (site != null) {
            boolean isExomeExpress = productOrder.getProduct().isExomeExpress();
            productOrder.setProductOrderKit(createOrderKit(isExomeExpress,
                    productOrderData.getKitDetailData(), sampleCollection, site));
        }

        // Send the kit information to BSP and assign the work request id.
        MessageCollection messageCollection = new MessageCollection();

        try {
            productOrderEjb.submitSampleKitRequest(productOrder, messageCollection);
            addProjectManagersToJIRA(productOrder, productOrder.getResearchProject().getProjectManagers());
        } catch (Exception ex) {
            throw new WorkRequestCreationException(ex);
        }

        return new ProductOrderData(productOrder);
    }

    /**
     * Add all the PMs to the order in JIRA, so they're notified when the issue is modified.
     */
    private void addProjectManagersToJIRA(@Nonnull ProductOrder productOrder, Long[] projectManagers)
            throws IOException {
        // Convert IDs to Users.
        List<BspUser> managers = new ArrayList<>(projectManagers.length);
        for (Long projectManager : projectManagers) {
            BspUser user = bspUserList.getById(projectManager);
            if (user != null) {
                managers.add(user);
            } else {
                log.error("Unexpected null BSPUser for project manager ID " + projectManager
                          + " from research project " + productOrder.getResearchProject().getBusinessKey());
            }
        }
        if (!managers.isEmpty()) {
            productOrderJiraUtil.setJiraPMsField(productOrder, managers);
        }
    }

    private ProductOrderKit createOrderKit(boolean isExomeExpress, List<ProductOrderKitDetailData> kitDetailsData,
                                          SampleCollection sampleCollection, Site site) {

        List<ProductOrderKitDetail> kitDetails = new ArrayList<>(kitDetailsData.size());
        for (ProductOrderKitDetailData kitDetailData : kitDetailsData) {
            SampleKitWorkRequest.MoleculeType moleculeType = kitDetailData.getMoleculeType();

            MaterialInfoDto materialInfoDto = createMaterialInfoDTO(kitDetailData.getMaterialInfo(), moleculeType);

            kitDetails.add(createKitDetail(kitDetailData.getNumberOfSamples(), moleculeType, materialInfoDto));
        }
        return new ProductOrderKit(sampleCollection, site, kitDetails, isExomeExpress);
    }

    private ProductOrderKitDetail createKitDetail(long numberOfSamples, SampleKitWorkRequest.MoleculeType moleculeType,
                                                  MaterialInfoDto materialInfo) {
        Set<PostReceiveOption> postReceiveOptions = EnumSet.noneOf(PostReceiveOption.class);

        if (moleculeType == SampleKitWorkRequest.MoleculeType.DNA) {
            postReceiveOptions.add(PostReceiveOption.PICO_RECEIVED);
        } else {
            postReceiveOptions.add(PostReceiveOption.BIOANALYZER);
        }

        return new ProductOrderKitDetail(numberOfSamples, KitType.DNA_MATRIX, HUMAN, materialInfo, postReceiveOptions);
    }

    private MaterialInfoDto createMaterialInfoDTO(MaterialInfo materialInfo,
                                                  SampleKitWorkRequest.MoleculeType moleculeType) {
        KitTypeAllowanceSpecification kitType;
        if (moleculeType == SampleKitWorkRequest.MoleculeType.DNA) {
            kitType = KitTypeAllowanceSpecification.DNA_MATRIX_KIT;
        } else {
            kitType = KitTypeAllowanceSpecification.RNA_MATRIX_KIT;
        }
        return new MaterialInfoDto(kitType.getText(), materialInfo.getText());
    }

    /**
     * Return the information on the newly created {@link ProductOrder} that has Pending status.
     * <p/>
     * It would be nice to only allow Project Managers and Administrators to create PDOs.  Use same {@link Role} names
     * as defined in the class (although I can't seem to be able to use the enum for the annotation.
     *
     * @param productOrderData the data for the construction of the new ProductOrder
     *
     * @return the data from the newly created ProductOrder
     */
    @POST
    @Path("create")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    public ProductOrderData create(@Nonnull ProductOrderData productOrderData)
            throws DuplicateTitleException, NoSamplesException, ApplicationValidationException,
            InvalidProductException {

        userBean.login(productOrderData.getUsername());
        if(userBean.getBspUser() == UserBean.UNKNOWN) {
            throw new ResourceException("A valid Username is required to complete this request",
                    Response.Status.UNAUTHORIZED);
        }
        return new ProductOrderData(productOrderEjb.createProductOrder(productOrderData), true);
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

        userBean.login(addSamplesToPdoBean.getUsername());
        if(userBean.getBspUser() == UserBean.UNKNOWN) {
            throw new ResourceException("A valid Username is required to complete this request",
                    Response.Status.UNAUTHORIZED);
        }

        ProductOrder order = productOrderDao.findByBusinessKey(pdoKey);
        if (order == null) {
            throw new ApplicationValidationException("No product order found for id: " + pdoKey);
        }

        // Create the mercury vessel for the plate and each tube in the plate.
        BspUser bspUser = userBean.getBspUser();
        if (bspUser == null) {
            throw new ApplicationValidationException(
                    "User: " + addSamplesToPdoBean.getUsername() + " is not found in Mercury");
        }

        Date currentDate = new Date();
        // Process is only interested in the primary vessels
        List<LabVessel> vessels = labVesselFactory.buildLabVessels(
                addSamplesToPdoBean.parentVesselBeans, bspUser.getUsername(), currentDate, LabEventType.SAMPLE_PACKAGE,
                MercurySample.MetadataSource.BSP).getLeft();
        labVesselDao.persistAll(vessels);

        // Get all the sample ids
        List<ProductOrderSample> samplesToAdd = new ArrayList<>(addSamplesToPdoBean.getParentVesselBeans().size());
        for (ParentVesselBean parentVesselBean : addSamplesToPdoBean.getParentVesselBeans()) {
            samplesToAdd.add(new ProductOrderSample(parentVesselBean.getSampleId()));
        }

        try {
            // Add the samples.
            productOrderEjb.addSamples(pdoKey, samplesToAdd, MessageReporter.UNUSED);

            // If the PDO is not a sample initiation PDO, but DOES have a sample initiation add on, then add a new
            // auto-billing message so that the billing will happen at the appropriate lock out time.
            if (!order.isSampleInitiation() && (order.hasSampleInitiationAddOn())) {
                for (ProductOrderSample sample : samplesToAdd) {
                    // Sample Initiation products do not have any 'risk' so there does not have to be any message data.
                    // The sample's business key IS the aliquot Id since that is the sample on the order.
                    Map<String, Object> dataMap = Collections.emptyMap();
                    WorkCompleteMessage workComplete =
                            new WorkCompleteMessage(pdoKey, sample.getBusinessKey(), Product.SAMPLE_INITIATION_PART_NUMBER, currentDate, dataMap);
                    workCompleteMessageDao.persist(workComplete);
                }
            }
        } catch (Exception e) {
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
                    productOrderDao.findListByBusinessKeys(businessKeyList, INCLUDE_SAMPLES_FETCH_SPECS) :
                    productOrderDao.findListByBusinessKeys(businessKeyList, DEFAULT_FETCH_SPECS);
        } else if (sampleIds != null) {
            // There isn't currently a fetchspec version of findBySampleBarcodes so this will take the singleton select
            // hit.
            orders = productOrderDao.findBySampleBarcodes(sampleIds.split(","));
        } else if (modifiedAfter != null) {
            // There isn't currently a fetchspec version of findModifiedAfter so this will take the singleton select hit.
            orders = productOrderDao.findModifiedAfter(modifiedAfter);
        } else {
            orders = includeSamples ?
                    productOrderDao.findAll(INCLUDE_SAMPLES_FETCH_SPECS) :
                    productOrderDao.findAll(DEFAULT_FETCH_SPECS);
        }

        // Filter draft orders.
        Iterator<ProductOrder> iterator = orders.iterator();
        while (iterator.hasNext()) {
            ProductOrder productOrder = iterator.next();
            if (productOrder.isDraft()) {
                iterator.remove();
            }
        }

        return buildProductOrdersFromList(orders, includeSamples);
    }

    private ProductOrders buildProductOrdersFromList(@Nonnull List<ProductOrder> productOrderList,
                                                     boolean includeSamples) {
        List<ProductOrderData> productOrderDataList = new ArrayList<>(productOrderList.size());

        for (ProductOrder productOrder : productOrderList) {
            // Adds genotyping chip info.
            ProductOrderData productOrderData = new ProductOrderData(productOrder, includeSamples);
            Date effectiveDate =  productOrder.getCreatedDate();
            productOrderData.setGenoChipType(productEjb.getGenotypingChip(productOrder, effectiveDate).getRight());

            productOrderDataList.add(productOrderData);
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
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
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

    private Site getSiteId(Long collectionId)
            throws ApplicationValidationException {
        // Get the first site Id. If there is not one, give an error.
        BspSiteManager siteManager = bspManagerFactory.createSiteManager();
        List<Site> sites = siteManager.getApplicableSites(collectionId).getResult();
        if (CollectionUtils.isEmpty(sites)) {
            throw new ApplicationValidationException(
                    "Could not find a site for the collection on this research project.");
        }

        return sites.get(0);
    }

    /**
     * @return the first (and only) cohort on a product order
     */

    private SampleCollection getFirstCollection(ResearchProject researchProject) {
        return bspGroupCollectionList.getById(researchProject.getCohorts()[0].getDatabaseId());
    }
}
