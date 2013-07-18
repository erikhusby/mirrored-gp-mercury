package org.broadinstitute.gpinformatics.athena.boundary.orders;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
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
    private Date modifiedDate;
    private String product;
    private String status;
    private List<String> samples;
    private String aggregationDataType;

    @SuppressWarnings("UnusedDeclaration")
    /** Required by JAXB. */
    ProductOrderData() {
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
}
