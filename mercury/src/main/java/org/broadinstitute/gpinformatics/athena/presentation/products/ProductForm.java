package org.broadinstitute.gpinformatics.athena.presentation.products;

import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductFamilyDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.*;

@Named
@RequestScoped
public class ProductForm extends AbstractJsfBean  implements Serializable {

    @Inject
    private ProductDao productDao;

    @Inject
    private ProductFamilyDao productFamilyDao;

    @Inject
    private FacesContext facesContext;

    public static final String DEFAULT_WORKFLOW_NAME = "";
    public static final Boolean DEFAULT_TOP_LEVEL = Boolean.TRUE;
    public static final Integer NULL_CYCLE_TIME = 0;



    public static final int ONE_HOUR_IN_SECONDS = 3600;

    // The reason why Product is included in this class as well as some of it's
    // members is because Product is most fiedls in Product are immutable
    // couple with the fact that we can create and edit a Product vi athe UI.
    // Not sure yet whether to add the setters to Product, once we perform
    // edit functionality we'll decide.
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



    public void initForm() {
        // Only initialize the form on postback. Otherwise, we'll leave the form as the user submitted it.
        if (!facesContext.isPostback()) {
            if ((partNumber != null) && !StringUtils.isBlank(partNumber)) {
                product = productDao.findByBusinessKey(partNumber);
                initProductDetailsFromProduct(product);
            }
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
     * Returns a list of SelectItems for all product families
     *
     * @return list of product families
     */
    public List<SelectItem> getProductFamilySelectItems() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        Set<String> aList = new HashSet<String>();
        for (ProductFamily productFamily :  productFamilyDao.findAll()) {
            items.add(new SelectItem(productFamily.getProductFamilyId(), productFamily.getName()));
        }
        return items;
    }

    public List<ProductFamily> getProductFamilies() {
        return  productFamilyDao.findAll();
    }

    public String save() {
        if (getProduct() == null ) {
            return create();
        } else {
            return edit();
        }
    }

    public String create() {

        Product product =  new Product(getProductName(), getProductFamilyById(getProductFamilyId()), getDescription(),
                getPartNumber(), getAvailabilityDate(), getDiscontinuedDate(),
                convertCycleTimeHoursToSeconds(getExpectedCycleTimeHours()),
                convertCycleTimeHoursToSeconds(getGuaranteedCycleTimeHours()),
                getSamplesPerWeek(), getInputRequirements(), getDeliverables(), DEFAULT_TOP_LEVEL,
                DEFAULT_WORKFLOW_NAME);

        try {
            productDao.persist(product);
        } catch (Exception e ) {
            String errorMessage = MessageFormat.format("Unknown exception occurred while persisting Product.", null);
            if (GenericDao.IsConstraintViolationException(e)) {
                errorMessage = MessageFormat.format("The Product Part-Number ''{0}'' is not unique.", getPartNumber());
            }
            addErrorMessage("Product not Created.", errorMessage, errorMessage + ": " + e);
            return "list";
        }

        addInfoMessage("Product created.", "Product " + product.getPartNumber() + " has been created.");
        return redirect("list");
    }

    private ProductFamily getProductFamilyById(Long productFamilyId) {
        return productFamilyDao.find(productFamilyId);
    }

    // TODO under construction not working nor tested.
    public String edit() {
//        String errorMessage = MessageFormat.format("Ability to saving an existing Product is Not yet Implemented.", null);
//        addErrorMessage("Product not Created.", errorMessage, errorMessage + ": " + new RuntimeException("Not yet Implemented"));
//        return "create";
        Product modifiedProduct = updateProductFromProductDetails(getProduct());
        if ( modifiedProduct != null ) {
            productDao.getEntityManager().merge(modifiedProduct);
            addInfoMessage("Product detail updated.", "Product " + modifiedProduct.getPartNumber() + " has been updated.");
//            return redirect("view?product=" + modifiedProduct.getPartNumber().trim());
            return redirect("list");
        }
//        return redirect("view?product="+getProduct().getPartNumber().trim());
        return redirect("list");
    }

    /**
     * Converts cycle times from hours to seconds.
     * @param cycleTimeHours
     * @return the number of seconds.
     */
    public static int convertCycleTimeHoursToSeconds(Integer cycleTimeHours) {
        return ( cycleTimeHours == null ? 0 : cycleTimeHours * ONE_HOUR_IN_SECONDS);
    }


    private Product updateProductFromProductDetails(Product product) {
        boolean hasProductChanged = false;
        Product modifiedProduct = null;
        if ( product != null ) {
            if ( StringUtils.isNotBlank(product.getProductName()) && ! (product.getProductName().equals( productName )) ) {
                hasProductChanged = true;
            }
            if ( StringUtils.isNotBlank(product.getDescription()) && ! (product.getDescription().equals( description )) ) {
                hasProductChanged = true;
            }
            if ( (product.getAvailabilityDate() != null) && ! (product.getAvailabilityDate().equals( availabilityDate )) ) {
                hasProductChanged = true;
            }
            if ( (product.getDiscontinuedDate() != null) && ! (product.getDiscontinuedDate().equals( discontinuedDate )) ) {
                hasProductChanged = true;
            }
            if ( (product.getSamplesPerWeek() != null) && ! (product.getSamplesPerWeek().equals( samplesPerWeek )) ) {
                hasProductChanged = true;
            }
            if ( StringUtils.isNotBlank(product.getInputRequirements()) && ! (product.getInputRequirements().equals( inputRequirements )) ) {
                hasProductChanged = true;
            }
            if ( StringUtils.isNotBlank(product.getDeliverables()) && ! (product.getDeliverables().equals( deliverables )) ) {
                hasProductChanged = true;
            }
            if ( (product.getExpectedCycleTimeSeconds() != null) && ! (product.getExpectedCycleTimeSeconds().equals( expectedCycleTimeHours )) ) {
                hasProductChanged = true;
            }
            if ( (product.getGuaranteedCycleTimeSeconds() != null) && ! (product.getGuaranteedCycleTimeSeconds().equals( guaranteedCycleTimeHours )) ) {
                hasProductChanged = true;
            }
            // create new product if It has changed.
            if ( hasProductChanged ) {
                modifiedProduct =  new Product(getProductName(), getProductFamily(), getDescription(),
                        getPartNumber(), getAvailabilityDate(), getDiscontinuedDate(),
                        ProductForm.convertCycleTimeHoursToSeconds(getExpectedCycleTimeHours()),
                        ProductForm.convertCycleTimeHoursToSeconds(getGuaranteedCycleTimeHours()),
                        getSamplesPerWeek(), getInputRequirements(), getDeliverables(), ProductForm.DEFAULT_TOP_LEVEL,
                        ProductForm.DEFAULT_WORKFLOW_NAME);
            }
        }

        return modifiedProduct;

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
