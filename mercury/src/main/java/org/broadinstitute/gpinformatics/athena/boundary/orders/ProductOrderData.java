package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.boundary.products.InvalidProductException;
import org.broadinstitute.gpinformatics.athena.boundary.projects.ApplicationValidationException;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.LongDateTimeAdapter;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService;
import org.broadinstitute.sap.entity.quote.SapQuote;
import org.broadinstitute.sap.services.SAPIntegrationException;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * JAXB DTO representing a single Product Order.
 */
@SuppressWarnings("UnusedDeclaration")
@XmlRootElement
public class ProductOrderData {
    public static final boolean INCLUDE_SAMPLES = true;

    private String title;
    private String id;
    private String comments;
    private Date createdDate;
    private Date placedDate;
    private Date modifiedDate;
    private String status;
    private String aggregationDataType;
    private String researchProjectId;
    private String productPartNumber;
    private String quoteId;
    private String username;
    private String requisitionName;
    private String productOrderKey;
    private String workRequestId;
    private int riskNotCalculatedCount;
    private String genoChipType;

    /** Even if includeSamples == false this will still contain the number of samples in the PDO. */
    private int numberOfSamples;

    // These are "in" parameters only, used to create a PDO with sample kits.
    private List<ProductOrderKitDetailData> kitDetailData = new ArrayList<>();
    private long siteId;

    /**
     * This is really a list of sample IDs.
     */
    private List<String> samples = new ArrayList<>();

    /** Also required by JAXB. */
    public ProductOrderData() {
    }

    /**
     * Constructor with the {@link ProductOrder} passed in for initialization. This constructor defaults the include
     * flag to true because if we don't have samples, including will just include nothing.
     *
     * @param productOrder the {@link ProductOrder}
     */
    public ProductOrderData(@Nonnull ProductOrder productOrder) {
        this(productOrder, INCLUDE_SAMPLES);
    }

    /**
     * Constructor with the {@link ProductOrder} passed in for initialization.
     *
     * @param productOrder the {@link ProductOrder}
     * @param includeSamples if true, include the PDO samples
     */
    public ProductOrderData(@Nonnull ProductOrder productOrder, boolean includeSamples) {
        title = productOrder.getTitle();

        // This duplicates productOrderKey; need to remove this field completely.
        id = productOrder.getBusinessKey();
        productOrderKey = productOrder.getBusinessKey();
        comments = productOrder.getComments();
        placedDate = productOrder.getPlacedDate();
        modifiedDate = productOrder.getModifiedDate();
        quoteId = productOrder.getQuoteId();
        status = productOrder.getOrderStatus().name();
        requisitionName = productOrder.getRequisitionName();
        createdDate = productOrder.getCreatedDate();

        Product product = productOrder.getProduct();
        if (product != null) {
            productPartNumber = product.getPartNumber();
            aggregationDataType = product.getPipelineDataTypeString();
        }


        if (productOrder.getResearchProject() != null) {
            researchProjectId = productOrder.getResearchProject().getBusinessKey();
        }

        if (productOrder.getProductOrderKit() != null) {
            workRequestId = productOrder.getProductOrderKit().getWorkRequestId();
        }

        if (includeSamples) {
            samples = getSampleList(productOrder.getSamples());
            for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
                if (productOrderSample.getRiskItems().isEmpty()) {
                    riskNotCalculatedCount++;
                }
            }
        } else {
            // Explicit set of null into a List<String> field, this duplicates what the existing code was doing when
            // includeSamples = false.  Is the JAXB behavior with an empty List undesirable?
            samples = null;
        }

        numberOfSamples = productOrder.getSampleCount();
    }

    private static List<String> getSampleList(List<ProductOrderSample> productOrderSamples) {
        List<String> sampleIdList = new ArrayList<>();
        for (ProductOrderSample productOrderSample : productOrderSamples) {
            sampleIdList.add(productOrderSample.getSampleKey());
        }

        return sampleIdList;
    }

    public String getTitle() {
        return (title == null) ? "" : title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getId() {
        return (id == null) ? "" : id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getComments() {
        return (comments == null) ? "" : comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    @XmlJavaTypeAdapter(LongDateTimeAdapter.class)
    public Date getPlacedDate() {
        return placedDate;
    }

    public void setPlacedDate(Date placedDate) {
        this.placedDate = placedDate;
    }

    @XmlJavaTypeAdapter(LongDateTimeAdapter.class)
    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public String getStatus() {
        return (status == null) ? "" : status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * We return an empty array and not a CollectionUtils.emptyList() because JaxRS wants to be able to add to the
     * list and it uses this method internally when reconstructing the object.  For this reason we MUST have it
     * return the empty ArrayList that it mutable.
     *
     * @return a mutable {@link List} of samples
     */
    @XmlElementWrapper
    public List<String> getSamples() {
        return samples;
    }

    /**
     * Really passing in a list of sample IDs.
     *
     * @param samples the list of sample IDs
     */
    public void setSamples(List<String> samples) {
        this.samples = samples;
    }

    public void setAggregationDataType(String aggregationDataType) {
        this.aggregationDataType = aggregationDataType;
    }

    public String getAggregationDataType() {
        return (aggregationDataType == null) ? "" : aggregationDataType;
    }

    public void setResearchProjectId(String researchProjectId) {
        this.researchProjectId = researchProjectId;
    }

    public String getResearchProjectId() {
        return (researchProjectId == null) ? "" : researchProjectId;
    }


    public void setWorkRequestId(String workRequestId) {
        this.workRequestId = workRequestId;
    }

    public String getWorkRequestId() {
        return (workRequestId == null) ? "" : workRequestId;
    }

    public void setProductPartNumber(String productPartNumber) {
        this.productPartNumber = productPartNumber;
    }

    public String getProductPartNumber() {
        return (productPartNumber == null) ? "" : productPartNumber;
    }

    public void setQuoteId(String quoteId) {
        this.quoteId = quoteId;
    }

    public String getQuoteId() {
        return (quoteId == null) ? "" : quoteId;
    }

    public String getUsername() {
        return (username == null) ? "" : username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRequisitionName() {
        return (requisitionName == null) ? "" : requisitionName;
    }

    public void setRequisitionName(String requisitionName) {
        this.requisitionName = requisitionName;
    }

    public String getProductOrderKey() {
        return productOrderKey;
    }

    public void setProductOrderKey(String productOrderKey) {
        this.productOrderKey = productOrderKey;
    }

    /**
     * Create a ProductOrder from this ProductOrderData
     *
     * @return the populated {@link ProductOrder}
     */
    public ProductOrder toProductOrder(ProductOrderDao productOrderDao, ResearchProjectDao researchProjectDao,
                                       ProductDao productDao, SapIntegrationService sapIntegrationService)
            throws DuplicateTitleException, NoSamplesException, ApplicationValidationException,
            InvalidProductException {

        // Make sure the title/name is supplied and unique
        if (StringUtils.isBlank(title)) {
            throw new ApplicationValidationException("Title required for Product Order");
        }

        // Make sure the title is unique
        if (productOrderDao.findByTitle(title) != null) {
            throw new DuplicateTitleException();
        }

        ProductOrder productOrder = new ProductOrder(title, comments, quoteId);

        // Find the product by the product part number.
        if (!StringUtils.isBlank(productPartNumber)) {
            productOrder.setProduct(productDao.findByPartNumber(productPartNumber));
        }

        if (StringUtils.isNumeric(quoteId)) {
            try {
                SapQuote sapQuote = sapIntegrationService.findSapQuote(quoteId);
                final ProductOrder.OrderAccessType orderType =
                        ProductOrder.OrderAccessType.fromSalesOrg(sapQuote.getQuoteHeader().getSalesOrganization());

                if((productOrder.getProduct().isExternalProduct() || productOrder.getProduct().isClinicalProduct()) &&
                   orderType != ProductOrder.OrderAccessType.COMMERCIAL) {
                    throw new InvalidProductException("Broad PI Engaged quotes cannot be used for Commercial or Clinical Products");
                } else {

                    productOrder.setOrderType(orderType);
                }
            } catch (SAPIntegrationException e) {
                throw new InvalidProductException( "Unable to get Quote information from SAP",e);
            }
        } else {
            if(productOrder.getProduct().isLLCProduct()) {
                productOrder.setOrderType(ProductOrder.OrderAccessType.COMMERCIAL);
            } else {
                productOrder.setOrderType(ProductOrder.OrderAccessType.BROAD_PI_ENGAGED_WORK);
            }
        }
        if (!StringUtils.isBlank(researchProjectId)) {
            ResearchProject researchProject =
                    researchProjectDao.findByBusinessKey(researchProjectId);

            // Make sure the required research project is present.
            if (researchProject == null) {
                throw new ApplicationValidationException(
                        "The required research project is not associated to the product order");
            }

            productOrder.setResearchProject(researchProject);
        }

        // Find and add the product order samples.
        List<ProductOrderSample> productOrderSamples = new ArrayList<>(samples.size());
        for (String sample : samples) {
            productOrderSamples.add(new ProductOrderSample(sample));
        }

        productOrder.addSamples(productOrderSamples);

        // Set the requisition name so one can look up the requisition in the Portal.
        productOrder.setRequisitionName(requisitionName);

        return productOrder;
    }

    public int getNumberOfSamples() {
        return numberOfSamples;
    }

    public void setNumberOfSamples(int numberOfSamples) {
        this.numberOfSamples = numberOfSamples;
    }

    public int getRiskNotCalculatedCount() {
        return riskNotCalculatedCount;
    }

    public void setRiskNotCalculatedCount(int riskNotCalculatedCount) {
        this.riskNotCalculatedCount = riskNotCalculatedCount;
    }

    public List<ProductOrderKitDetailData> getKitDetailData() {
        return kitDetailData;
    }

    public void setKitDetailData(List<ProductOrderKitDetailData> kitDetailData) {
        this.kitDetailData = kitDetailData;
    }

    public long getSiteId() {
        return siteId;
    }

    public void setSiteId(long siteId) {
        this.siteId = siteId;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public String getGenoChipType() {
        return genoChipType;
    }

    public void setGenoChipType(String genoChipType) {
        this.genoChipType = genoChipType;
    }
}
