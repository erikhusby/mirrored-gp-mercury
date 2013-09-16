package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.LongDateTimeAdapter;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.text.SimpleDateFormat;
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
        this.title = productOrder.getTitle();

        if (productOrder.getProductOrderId() != null) {
            this.id = productOrder.getProductOrderId().toString();
        }

        this.comments = productOrder.getComments();
        this.placedDate = productOrder.getPlacedDate();
        this.modifiedDate = productOrder.getModifiedDate();

        if (productOrder.getProduct() != null) {
            // TODO: How is this different than productName?
            this.product = productOrder.getProduct().getProductName();
        }
        this.status = productOrder.getOrderStatus().name();

        this.samples = getSampleList(productOrder.getSamples()); // TODO: Confirm this is sample ID not name

        this.aggregationDataType = null;  // productOrder.?

        if (productOrder.getResearchProject() != null) {
            this.researchProjectId = productOrder.getResearchProject().getResearchProjectId().toString();
        }

        if (productOrder.getProduct() != null) {
            this.productName = productOrder.getProduct().getProductName();
        }

        this.quoteId = productOrder.getQuoteId();
    }

    private static List<String> getSampleList(List<ProductOrderSample> productOrderSamples) {
        List<String> sampleIdList = new ArrayList<>();
        for (ProductOrderSample productOrderSample : productOrderSamples) {
            sampleIdList.add(productOrderSample.getProductOrderSampleId().toString());
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
    @XmlElement(name = "sample")
    public List<String> getSamples() {
        return samples;
    }

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
