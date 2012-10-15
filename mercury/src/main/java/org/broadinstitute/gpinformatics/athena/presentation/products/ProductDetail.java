package org.broadinstitute.gpinformatics.athena.presentation.products;

import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Date;

@Named
@RequestScoped
public class ProductDetail extends AbstractJsfBean {

    @Inject
    private ProductDao productDao;

    private Product product;
    private String productName;
    private ProductFamily productFamily;
    private String description;
    private String partNumber;
    //TODO hmc
    private Date availabilityDate;
    private Date discontinuedDate;
    private Integer expectedCycleTimeSeconds;
    private Integer guaranteedCycleTimeSeconds;
    private Integer samplesPerWeek;
    private String inputRequirements;
    private String deliverables;
    private boolean topLevelProduct;
    private String workflowName;


    public void initEmptyProduct() {
        //TODO hmc
        product =
        new Product("productName", new ProductFamily("ProductFamily"), "description",
                    "partNumber", new Date(), new Date(), 12345678, 123456, 100, "inputRequirements", "deliverables",
                    true, "workflowName");
    }

    public void loadProduct() {
        //TODO hmc
        if ((product == null) && !StringUtils.isBlank(partNumber)) {
            product = productDao.findByPartNumber(partNumber);
        }
    }

    public Product getProduct() {
        return product;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(final String productName) {
        this.productName = productName;
    }

    public ProductFamily getProductFamily() {
        return productFamily;
    }

    public void setProductFamily(final ProductFamily productFamily) {
        this.productFamily = productFamily;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(final String partNumber) {
        this.partNumber = partNumber;
    }

    public Date getAvailabilityDate() {
        return availabilityDate;
    }

    public void setAvailabilityDate(final Date availabilityDate) {
        this.availabilityDate = availabilityDate;
    }

    public Date getDiscontinuedDate() {
        return discontinuedDate;
    }

    public void setDiscontinuedDate(final Date discontinuedDate) {
        this.discontinuedDate = discontinuedDate;
    }

    public Integer getExpectedCycleTimeSeconds() {
        return expectedCycleTimeSeconds;
    }

    public void setExpectedCycleTimeSeconds(final Integer expectedCycleTimeSeconds) {
        this.expectedCycleTimeSeconds = expectedCycleTimeSeconds;
    }

    public Integer getGuaranteedCycleTimeSeconds() {
        return guaranteedCycleTimeSeconds;
    }

    public void setGuaranteedCycleTimeSeconds(final Integer guaranteedCycleTimeSeconds) {
        this.guaranteedCycleTimeSeconds = guaranteedCycleTimeSeconds;
    }

    public Integer getSamplesPerWeek() {
        return samplesPerWeek;
    }

    public void setSamplesPerWeek(final Integer samplesPerWeek) {
        this.samplesPerWeek = samplesPerWeek;
    }

    public String getInputRequirements() {
        return inputRequirements;
    }

    public void setInputRequirements(final String inputRequirements) {
        this.inputRequirements = inputRequirements;
    }

    public String getDeliverables() {
        return deliverables;
    }

    public void setDeliverables(final String deliverables) {
        this.deliverables = deliverables;
    }

    public boolean isTopLevelProduct() {
        return topLevelProduct;
    }

    public void setTopLevelProduct(final boolean topLevelProduct) {
        this.topLevelProduct = topLevelProduct;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(final String workflowName) {
        this.workflowName = workflowName;
    }
}
