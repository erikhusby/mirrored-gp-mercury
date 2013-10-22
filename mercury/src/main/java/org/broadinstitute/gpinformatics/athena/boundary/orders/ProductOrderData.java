package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.LongDateTimeAdapter;

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
    private String requisitionKey;
    private String requisitionName;
    private String productOrderKey;

    /**
     * This is really a list of sample IDs.
     */
    private List<String> samples;

    @SuppressWarnings("UnusedDeclaration")
    /** Required by JAXB. */
    ProductOrderData() {
    }

    /**
     * Constructor with the {@link ProductOrder} passed in for initialization.
     *
     * @param productOrder the {@link ProductOrder}
     */
    public ProductOrderData(@Nonnull ProductOrder productOrder) {
        title = productOrder.getTitle();

        if (productOrder.getProductOrderId() != null) {
            id = productOrder.getProductOrderId().toString();
        }

        productOrderKey = productOrder.getBusinessKey();
        comments = productOrder.getComments();
        placedDate = productOrder.getPlacedDate();
        modifiedDate = productOrder.getModifiedDate();
        quoteId = productOrder.getQuoteId();
        status = productOrder.getOrderStatus().name();
        aggregationDataType = null;  // productOrder.?

        if (productOrder.getProduct() != null) {
            product = productOrder.getProduct().getBusinessKey();
            productName = productOrder.getProduct().getProductName();
        }

        samples = getSampleList(productOrder.getSamples());

        if (productOrder.getResearchProject() != null) {
            researchProjectId = productOrder.getResearchProject().getBusinessKey();
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

    public String getRequisitionKey() {
        if (requisitionKey == null) {
            return "";
        }

        return requisitionKey;
    }

    public void setRequisitionKey(String requisitionKey) {
        this.requisitionKey = requisitionKey;
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
}
