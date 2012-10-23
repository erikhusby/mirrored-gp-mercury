package org.broadinstitute.gpinformatics.athena.presentation.products;

import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Named
@RequestScoped
public class ProductDetail extends AbstractJsfBean {

    public static final int ONE_HOUR_IN_SECONDS = 3600;

    @Inject
    private ProductDao productDao;

    @Inject
    private PriceListCache priceListCache;

    private Product product;
    private String productName;
    private ProductFamily productFamily;
    private Long productFamilyId;
    private String partNumber;
    private String description;
    private Date availabilityDate;
    private Date discontinuedDate;
    private Integer samplesPerWeek;
    private String inputRequirements;
    private String deliverables;
    private Integer expectedCycleTimeHours;
    private Integer guaranteedCycleTimeHours;


    public List<PriceItem> getFirst5PriceItems() {
        return new ArrayList<PriceItem>(priceListCache.getPriceItems()).subList(0, 5);
    }


    public void initEmptyProduct() {
        if ((partNumber != null) && !StringUtils.isBlank(partNumber)) {
            product = productDao.findByBusinessKey(partNumber);
            initProductDetailsFromProduct(product);
        }
    }

    private void initProductDetailsFromProduct(Product product) {
        if ( product != null ) {
            productName = product.getProductName();
            partNumber = product.getPartNumber();
            productFamily = product.getProductFamily();
            description = product.getDescription();
            availabilityDate = product.getAvailabilityDate();
            discontinuedDate = product.getDiscontinuedDate();
            samplesPerWeek = product.getSamplesPerWeek();
            inputRequirements = product.getInputRequirements();
            deliverables = product.getDeliverables();
            expectedCycleTimeHours =convertCycleTimeSecondsToHours(product.getExpectedCycleTimeSeconds());
            guaranteedCycleTimeHours =convertCycleTimeSecondsToHours(product.getGuaranteedCycleTimeSeconds());
        }
    }

    /**
     * Converts cycle times from seconds to hours.
     * This method rounds down to the nearest hour
     * @param cycleTimeSeconds
     * @return the number of hours.
     */
    private int convertCycleTimeSecondsToHours(Integer cycleTimeSeconds) {
        Integer cycleTimeHours = 0;
        if ((cycleTimeSeconds != null) && cycleTimeSeconds >= ONE_HOUR_IN_SECONDS ) {
            cycleTimeHours =  (cycleTimeSeconds - (cycleTimeSeconds % ONE_HOUR_IN_SECONDS)) / ONE_HOUR_IN_SECONDS;
        }
        return cycleTimeHours;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(final Product product) {
        this.product = product;
        initProductDetailsFromProduct(product);
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
    public Long getProductFamilyId() {
        return productFamilyId;
    }

    public void setProductFamilyId(final Long productFamilyId) {
        this.productFamilyId = productFamilyId;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(final String partNumber) {
        this.partNumber = partNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
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


    public Integer getExpectedCycleTimeHours() {
        return expectedCycleTimeHours;
    }
    public void setExpectedCycleTimeHours(final Integer expectedCycleTimeHours) {
        this.expectedCycleTimeHours = expectedCycleTimeHours;
    }

    public Integer getGuaranteedCycleTimeHours() {
        return guaranteedCycleTimeHours;
    }
    public void setGuaranteedCycleTimeHours(final Integer guaranteedCycleTimeHours) {
        this.guaranteedCycleTimeHours = guaranteedCycleTimeHours;
    }


}
