package org.broadinstitute.gpinformatics.athena.boundary.orders;

import edu.mit.broad.bsp.core.datavo.workrequest.items.kit.MaterialInfo;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequest;
import org.broadinstitute.gpinformatics.athena.boundary.projects.ApplicationValidationException;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.LongDateTimeAdapter;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.security.ApplicationInstance;

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
    private Date placedDate;
    private Date modifiedDate;
    private String product;
    private String status;
    private String aggregationDataType;
    private String researchProjectId;
    private String productName;
    private String quoteId;
    private String username;
    private String requisitionName;
    private String productOrderKey;
    private int numberOfSamples;
    private SampleKitWorkRequest.MoleculeType moleculeType;
    private MaterialInfo materialInfo;

    /**
     * This is really a list of sample IDs.
     */
    private List<String> samples;
    private String materialType;

    @SuppressWarnings("UnusedDeclaration")
    /** Required by JAXB. */
    ProductOrderData() {
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

        Product product = productOrder.getProduct();
        if (product != null) {
            this.product = product.getBusinessKey();
            productName = product.getProductName();
            aggregationDataType = product.getAggregationDataType();
        }


        if (productOrder.getResearchProject() != null) {
            researchProjectId = productOrder.getResearchProject().getBusinessKey();
        }

        if (includeSamples) {
            samples = getSampleList(productOrder.getSamples());
        } else {
            // Explicit set of null into a List<String> field, this duplicates what the existing code was doing when
            // includeSamples = false.  Is the JAXB behavior with an empty List undesirable?
            samples = null;
        }
    }

    private static List<String> getSampleList(List<ProductOrderSample> productOrderSamples) {
        List<String> sampleIdList = new ArrayList<>();
        for (ProductOrderSample productOrderSample : productOrderSamples) {
            sampleIdList.add(productOrderSample.getSampleKey());
        }

        return sampleIdList;
    }

    public String getTitle() {
        if (title == null) {
            return "";
        }

        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getId() {
        if (id == null) {
            return "";
        }

        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getComments() {
        if (comments == null) {
            return "";
        }

        return comments;
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

    public String getProduct() {
        if (product == null) {
            return "";
        }

        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getStatus() {
        if (status == null) {
            return "";
        }

        return status;
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
        if (samples == null) {
            return new ArrayList<>();
        }

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
        if (aggregationDataType == null) {
            return "";
        }

        return aggregationDataType;
    }

    public void setResearchProjectId(String researchProjectId) {
        this.researchProjectId = researchProjectId;
    }

    public String getResearchProjectId() {
        if (researchProjectId == null) {
            return "";
        }

        return researchProjectId;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductName() {
        if (productName == null) {
            return "";
        }

        return productName;
    }

    public void setQuoteId(String quoteId) {
        this.quoteId = quoteId;
    }

    public String getQuoteId() {
        if (quoteId == null) {
            return "";
        }

        return quoteId;
    }

    public String getUsername() {
        if (username == null) {
            return "";
        }

        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRequisitionName() {
        if (requisitionName == null) {
            return "";
        }

        return requisitionName;
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
     * Try to convert the JAXB XML data into a {@link ProductOrder} and do some validation while converting.
     *
     * @return the populated {@link ProductOrder}
     */
    public ProductOrder convert(ProductOrderDao productOrderDao, ResearchProjectDao researchProjectDao,
                                 ProductDao productDao)
            throws DuplicateTitleException, NoSamplesException, QuoteNotFoundException, ApplicationValidationException {

        ProductOrder productOrder = new ProductOrder();

        // Make sure the title/name is supplied and unique
        if (StringUtils.isBlank(getTitle())) {
            throw new ApplicationValidationException("Title required for Product Order");
        }

        // Make sure the title
        if (productOrderDao.findByTitle(getTitle()) != null) {
            throw new DuplicateTitleException();
        }

        productOrder.setTitle(getTitle());
        productOrder.setComments(getComments());
        productOrder.setQuoteId(getQuoteId());

        // Find the product by the product name.
        if (!StringUtils.isBlank(getProductName())) {
            Product product = productDao.findByName(getProductName());
            productOrder.setProduct(product);
        }

        if (!StringUtils.isBlank(getResearchProjectId())) {
            ResearchProject researchProject =
                    researchProjectDao.findByBusinessKey(getResearchProjectId());

            // Make sure the required research project is present.
            if (researchProject == null) {
                throw new ApplicationValidationException(
                        "The required research project is not associated to the product order");
            }

            productOrder.setResearchProject(researchProject);
        }

        // Find and add the product order samples.
        List<ProductOrderSample> productOrderSamples = new ArrayList<>();
        for (String sample : getSamples()) {
            productOrderSamples.add(new ProductOrderSample(sample));
        }

        // Make sure the required sample(s) are present FOR CLIA. For others, adding later is valid.
        if (productOrderSamples.isEmpty() && ApplicationInstance.CRSP.isCurrent()) {
            throw new NoSamplesException();
        }

        productOrder.addSamples(productOrderSamples);

        // Set the requisition name so one can look up the requisition in the Portal.
        productOrder.setRequisitionName(getRequisitionName());

        return productOrder;
    }

    public int getNumberOfSamples() {
        return numberOfSamples;
    }

    public void setNumberOfSamples(int numberOfSamples) {
        this.numberOfSamples = numberOfSamples;
    }

    public SampleKitWorkRequest.MoleculeType getMoleculeType() {
        return moleculeType;
    }

    public void setMoleculeType(SampleKitWorkRequest.MoleculeType moleculeType) {
        this.moleculeType = moleculeType;
    }

    public MaterialInfo getMaterialInfo() {
        return materialInfo;
    }

    public void setMaterialInfo(MaterialInfo materialInfo) {
        this.materialInfo = materialInfo;
    }

    public String getMaterialType() {
        return materialType;
    }

    public void setMaterialType(String materialType) {
        this.materialType = materialType;
    }
}
