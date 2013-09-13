package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
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

    /**
     * This is really a list of sample IDs.
     */
    private List<String> samples;

    private String aggregationDataType;
    private String researchProjectId;
    private String productName;
    private String quoteId;
    public String username;

    @SuppressWarnings("UnusedDeclaration")
    /** Required by JAXB. */
    ProductOrderData() {
    }

    /**
     * Constructor with the {@link ProductOrder} passed in for initialization.
     *
     * @param productOrder the {@link ProductOrder}
     */
    public  ProductOrderData(@Nonnull ProductOrder productOrder) {
        title = productOrder.getTitle();

        if (productOrder.getProductOrderId() != null) {
            id = productOrder.getProductOrderId().toString();
        }

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

        samples = getSampleList(productOrder.getSamples()); // TODO: Confirm this is sample ID not name

        if (productOrder.getResearchProject() != null) {
            researchProjectId = productOrder.getResearchProject().getResearchProjectId().toString();
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
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Date getPlacedDate() {
        return placedDate;
    }

    public void setPlacedDate(Date placedDate) {
        this.placedDate = placedDate;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @XmlElementWrapper
    @XmlElement(name = "sampleId")
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
        return aggregationDataType;
    }

    public void setResearchProjectId(String researchProjectId) {
        this.researchProjectId = researchProjectId;
    }

    public String getResearchProjectId() {
        return researchProjectId;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductName() {
        return productName;
    }

    public void setQuoteId(String quoteId) {
        this.quoteId = quoteId;
    }

    public String getQuoteId() {
        return quoteId;
    }

    public String getUsername() {
        return username;
    }

    public void setUserName(String username) {
        this.username = username;
    }


}
