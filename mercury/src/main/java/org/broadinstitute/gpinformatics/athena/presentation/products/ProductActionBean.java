package org.broadinstitute.gpinformatics.athena.presentation.products;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationErrors;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductFamilyDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

/**
 * This class supports all the actions done on products
 */
@UrlBinding("/products/product.action")
public class ProductActionBean extends CoreActionBean{
    private Log logger = LogFactory.getLog(ProductActionBean.class);

    private static final String CREATE_PAGE = "/products/create.jsp";
    private static final String LIST_PAGE = "/products/list.jsp";
    private static final String VIEW_PAGE = "/products/view.jsp";

    private static final String GLOBAL_MESSAGE_PRICE_ITEMS_NOT_ON_PRICE_LIST =
            "One or more price items associated with this product do not appear on the current quote server price list.  " +
                    "These price items have been temporarily removed from this product; hit Save to remove these price items permanently or Cancel to abort.";

    @Inject
    private ProductFamilyDao productFamilyDao;

    @Inject
    private PriceItemDao priceItemDao;

    // The complete price list from the quote server
    @Inject
    private PriceListCache priceListCache;

    @Inject
    private ProductDao productDao;

    public static final String DEFAULT_WORKFLOW_NAME = "";
    public static final Boolean DEFAULT_TOP_LEVEL = Boolean.TRUE;
    private static final int ONE_DAY_IN_SECONDS = 60 * 60 * 24;

    // Data needed for displaying the view
    private List<ProductFamily> productFamilies;
    private List<Product> allProducts;

    @Validate(required = true, on = {"view", "edit"})
    private String productKey;
    private Product product;

    private Product editProduct;

    /**
     * maps between athena price item and the quote server version of PriceItem
     *
     * @param entity The entity The price item entity
     *
     * @return The quote servers price item that corresponds
     */
    private PriceItem getQuotePriceItem(org.broadinstitute.gpinformatics.athena.entity.products.PriceItem entity) {
        return new PriceItem(entity.getQuoteServerId(), entity.getPlatform(), entity.getCategory(), entity.getName());
    }

    /**
     * maps between quote server version of PriceItem and athena price item
     *
     * @param quotePriceItem The athena price item
     *
     * @return The athena price item
     */
    private org.broadinstitute.gpinformatics.athena.entity.products.PriceItem getPriceItem(PriceItem quotePriceItem) {
        return new org.broadinstitute.gpinformatics.athena.entity.products.PriceItem(
            quotePriceItem.getId(), quotePriceItem.getPlatformName(), quotePriceItem.getCategoryName(), quotePriceItem.getName());
    }

    /**
     * Initialize the product with the passed in key for display in the form
     */
    @Before(stages = LifecycleStage.BindingAndValidation, on = {"view", "edit"})
    public void init() {
        product = productDao.findByBusinessKey(productKey);
        productFamilies = productFamilyDao.findAll();
        Collections.sort(productFamilies);
    }

    @Before(stages = LifecycleStage.BindingAndValidation, on = {"list"})
    public void listInit() {
        allProducts = productDao.findAll(Product.class);
    }

    /**
     * Validate information on the product being edited or created
     */
    @ValidationMethod(on = {"createForm", "editForm"})
    public void validatePriceItems(ValidationErrors errors) {
        String[] duplicatePriceItems = editProduct.getDuplicatePriceItemNames();
        if (duplicatePriceItems != null) {
            errors.addGlobalError(new SimpleError("Cannot save with duplicate price items: " + StringUtils.join(duplicatePriceItems, ", ")));
        }

        // Is there a product with the part number already?
        Product existingProduct = productDao.findByPartNumber(editProduct.getPartNumber());
        if (existingProduct != null && ! existingProduct.getProductId().equals(product.getProductId())) {
            errors.addGlobalError(new SimpleError("Part number '" + editProduct.getPartNumber() + "' is already in use"));
        }

        if ((editProduct.getAvailabilityDate() != null) &&
            (editProduct.getDiscontinuedDate() != null) &&
            (editProduct.getAvailabilityDate().after(product.getDiscontinuedDate()))) {
            errors.addGlobalError(new SimpleError("Availability date must precede discontinued date."));
        }
    }

    @DefaultHandler
    @HandlesEvent("list")
    public Resolution list() {
        return new ForwardResolution(LIST_PAGE);
    }

    @HandlesEvent(value = "save")
    public Resolution save() {
        try {
            productDao.persist(editProduct);
        } catch (Exception e ) {
            addGlobalValidationError(e.getMessage());
            return null;
        }

        addMessage("Product \"" + product.getProductName() + "\" has been created");
        return new RedirectResolution(VIEW_PAGE).addParameter("product", editProduct.getProductId());
    }

    private String addProductParam() {
        return "&product=" + product.getBusinessKey();
    }

    /**
     * convert a quote price item to an athena price item
     *
     * @param quotePriceItem The quote server's price item
     *
     * @return The athena price item
     */
    private org.broadinstitute.gpinformatics.athena.entity.products.PriceItem findEntity(PriceItem quotePriceItem) {
        org.broadinstitute.gpinformatics.athena.entity.products.PriceItem entity =
            priceItemDao.find(
                quotePriceItem.getPlatformName(), quotePriceItem.getCategoryName(), quotePriceItem.getName());

        if (entity == null) {
            entity = getPriceItem(quotePriceItem);
        }

        return entity;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(final Product product) {
        this.product = product;
    }

    public Integer getExpectedCycleTimeDays() {
        return convertCycleTimeSecondsToDays(product.getExpectedCycleTimeSeconds()) ;
    }
    public void setExpectedCycleTimeDays(final Integer expectedCycleTimeDays) {
        product.setExpectedCycleTimeSeconds(convertCycleTimeDaysToSeconds(expectedCycleTimeDays));
    }

    public Integer getGuaranteedCycleTimeDays() {
        return convertCycleTimeSecondsToDays(product.getGuaranteedCycleTimeSeconds()) ;
    }
    public void setGuaranteedCycleTimeDays(final Integer guaranteedCycleTimeDays) {
        product.setGuaranteedCycleTimeSeconds(convertCycleTimeDaysToSeconds(guaranteedCycleTimeDays));
    }

    /**
     * Converts cycle times from days to seconds.
     * @return the number of seconds.
     */
    public static int convertCycleTimeDaysToSeconds(Integer cycleTimeDays) {
        return (cycleTimeDays == null) ? 0 : cycleTimeDays * ONE_DAY_IN_SECONDS;
    }

    /**
     * Converts cycle times from seconds to days.
     * This method rounds down to the nearest day
     *
     * @param cycleTimeSeconds The cycle time in seconds
     *
     * @return the number of days.
     */
    public static Integer convertCycleTimeSecondsToDays(Integer cycleTimeSeconds) {
        Integer cycleTimeDays = null;
        if ((cycleTimeSeconds != null) && cycleTimeSeconds >= ONE_DAY_IN_SECONDS) {
            cycleTimeDays =  (cycleTimeSeconds - (cycleTimeSeconds % ONE_DAY_IN_SECONDS)) / ONE_DAY_IN_SECONDS;
        }
        return cycleTimeDays;
    }

    public Product getEditProduct() {
        return editProduct;
    }

    public void setEditProduct(Product product) {
        this.editProduct = product;
    }

    public List<Product> getAllProducts() {
        return allProducts;
    }
}
