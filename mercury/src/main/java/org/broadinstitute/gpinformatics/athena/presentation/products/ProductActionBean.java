package org.broadinstitute.gpinformatics.athena.presentation.products;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.*;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductFamilyDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.AutoCompleteToken;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.json.JSONArray;
import org.json.JSONException;

import javax.inject.Inject;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This class supports all the actions done on products
 */
@UrlBinding("/products/product.action")
public class ProductActionBean extends CoreActionBean {

    private static final String CREATE_PRODUCT = CoreActionBean.CREATE + " New Product";
    private static final String EDIT_PRODUCT = CoreActionBean.EDIT + " Product: ";

    public static final String PRODUCT_CREATE_PAGE = "/products/create.jsp";
    public static final String PRODUCT_LIST_PAGE = "/products/list.jsp";
    public static final String PRODUCT_VIEW_PAGE = "/products/view.jsp";

    @Inject
    private ProductFamilyDao productFamilyDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private PriceItemDao priceItemDao;

    @Inject
    private PriceListCache priceListCache;

    // Data needed for displaying the view
    private List<ProductFamily> productFamilies;
    private List<Product> allProducts;

    @Validate(required = true, on = {VIEW_ACTION, EDIT_ACTION})
    private String productKey;

    @ValidateNestedProperties({
        @Validate(field="productFamily.productFamilyId", required = true, maxlength=255, on={SAVE_ACTION}),
        @Validate(field="productName", required = true, maxlength=255, on={SAVE_ACTION})
    })
    private Product editProduct;


    // These are the fields for catching the input tokens
    @Validate(required = true, on = {SAVE_ACTION})
    private String priceItemList = "";

    private String addOnList = "";

    // The search query
    private String q;

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    /**
     * Initialize the product with the passed in key for display in the form
     */
    @Before(stages = LifecycleStage.BindingAndValidation, on = {VIEW_ACTION, EDIT_ACTION, SAVE_ACTION, "addOnsAutocomplete"})
    public void init() {
        productKey = getContext().getRequest().getParameter("productKey");
        if (!StringUtils.isBlank(productKey)) {
            editProduct = productDao.findByBusinessKey(productKey);
        } else {
            // This must be a create, so construct a new top level product that has nothing else set
            editProduct = new Product(Product.TOP_LEVEL_PRODUCT);
        }
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = {CREATE_ACTION, EDIT_ACTION})
    public void setupFamilies() {
        productFamilies = productFamilyDao.findAll();
        Collections.sort(productFamilies);
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = {LIST_ACTION})
    public void listInit() {
        allProducts = productDao.findAll(Product.class);
    }

    /**
     * Validate information on the product being edited or created
     */
    @ValidationMethod(on = {SAVE_ACTION})
    public void validatePriceItems(ValidationErrors errors) {
        String[] duplicatePriceItems = editProduct.getDuplicatePriceItemNames();
        if (duplicatePriceItems != null) {
            errors.addGlobalError(new SimpleError("Cannot save with duplicate price items: " + StringUtils.join(duplicatePriceItems, ", ")));
        }

        // check for existing name for create or name change on edit
        if ((editProduct.getOriginalPartNumber() == null) ||
            (!editProduct.getPartNumber().equalsIgnoreCase(editProduct.getOriginalPartNumber()))) {

            Product existingProduct = productDao.findByPartNumber(editProduct.getPartNumber());
            if (existingProduct != null && ! existingProduct.getProductId().equals(editProduct.getProductId())) {
                errors.add("partNumber", new SimpleError("Part number '" + editProduct.getPartNumber() + "' is already in use"));
            }
        }

        // Check that the dates are consistent
        if ((editProduct.getAvailabilityDate() != null) &&
            (editProduct.getDiscontinuedDate() != null) &&
            (editProduct.getAvailabilityDate().after(editProduct.getDiscontinuedDate()))) {
            errors.addGlobalError(new SimpleError("Availability date must precede discontinued date."));
        }
    }

    @DefaultHandler
    @HandlesEvent(LIST_ACTION)
    public Resolution list() {
        return new ForwardResolution(PRODUCT_LIST_PAGE);
    }

    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(PRODUCT_VIEW_PAGE);
    }

    @HandlesEvent(CREATE_ACTION)
    public Resolution create() {
        setSubmitString(CREATE_PRODUCT);
        return new ForwardResolution(PRODUCT_CREATE_PAGE);
    }

    @HandlesEvent(EDIT_ACTION)
    public Resolution edit() {
        setSubmitString(EDIT_PRODUCT);
        return new ForwardResolution(PRODUCT_CREATE_PAGE);
    }

    @HandlesEvent("autocomplete")
    public Resolution autocomplete() throws Exception {
        List<Product> products = productDao.searchProducts(getQ());

        String completeString = getAutoCompleteJsonString(products);
        return new StreamingResolution("text", new StringReader(completeString));
    }

    public static String getAutoCompleteJsonString(Collection<Product> products) throws JSONException {
        JSONArray itemList = new JSONArray();
        for (Product product : products) {
            itemList.put(new AutoCompleteToken(product.getBusinessKey(), product.getProductName(), false).getJSONObject());
        }

        return itemList.toString();
    }

    @HandlesEvent("addOnsAutocomplete")
    public Resolution addOnsAutocomplete() throws Exception {
        List<Product> addOns = productDao.searchProductsForAddonsInProductEdit(editProduct, getQ());

        JSONArray itemList = new JSONArray();
        for (Product addOn : addOns) {
            itemList.put(new AutoCompleteToken(addOn.getBusinessKey(), addOn.getProductName(), false).getJSONObject());
        }

        return new StreamingResolution("text", new StringReader(itemList.toString()));
    }

    @HandlesEvent("priceItemAutocomplete")
    public Resolution priceItemAutocomplete() throws Exception {
        List<PriceItem> priceItems = priceListCache.searchPriceItems(getQ());

        JSONArray itemList = new JSONArray();
        for (PriceItem priceItem : priceItems) {
            itemList.put(new AutoCompleteToken(priceItem.getId(), priceItem.getName(), false).getJSONObject());
        }

        return new StreamingResolution("text", new StringReader(itemList.toString()));
    }

    @HandlesEvent(value = SAVE_ACTION)
    public Resolution save() {
        populateTokenListFields();

        editProduct.setProductFamily(productFamilyDao.find(editProduct.getProductFamily().getProductFamilyId()));

        productDao.persist(editProduct);
        addMessage("Product \"" + editProduct.getProductName() + "\" has been created");
        return new RedirectResolution(ProductActionBean.class, VIEW_ACTION).addParameter("productKey", editProduct.getPartNumber());
    }

    private void populateTokenListFields() {
        editProduct.getAddOns().clear();
        editProduct.getAddOns().addAll(getAddOns());

        editProduct.setPrimaryPriceItem(getPriceItem());
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

    public String getProductKey() {
        return productKey;
    }

    public void setProductKey(String productKey) {
        this.productKey = productKey;
    }

    public List<ProductFamily> getProductFamilies() {
        return productFamilies;
    }

    public List<Product> getAddOns() {
        if ((addOnList == null) || (addOnList.isEmpty())) {
            return Collections.emptyList();
        }

        List<String> addOnIdList = Arrays.asList(addOnList.split(","));
        return productDao.findByPartNumbers(addOnIdList);
    }

    public org.broadinstitute.gpinformatics.athena.entity.products.PriceItem getPriceItem() {
        PriceItem priceItem = priceListCache.findById(Long.valueOf(priceItemList));

        org.broadinstitute.gpinformatics.athena.entity.products.PriceItem entity =
                priceItemDao.find(priceItem.getPlatformName(), priceItem.getCategoryName(), priceItem.getName());

        // If we don't have this price item, this will add it.
        if (entity == null) {
            entity = new org.broadinstitute.gpinformatics.athena.entity.products.PriceItem(
                priceItem.getId(), priceItem.getPlatformName(), priceItem.getCategoryName(), priceItem.getName());
        }

        return entity;
    }

    public String getAddOnCompleteData() throws Exception {
        if (editProduct == null) {
            return "";
        }

        JSONArray itemList = new JSONArray();
        for (Product product : editProduct.getAddOns()) {
            itemList.put(new AutoCompleteToken(product.getBusinessKey(), product.getDisplayName(), false).getJSONObject());
        }

        return itemList.toString();
    }

    public String getPriceItemCompleteData() throws Exception {
        if ((editProduct == null) || (editProduct.getPrimaryPriceItem() == null)) {
            return "";
        }

        JSONArray itemList = new JSONArray();
        String quotePriceItemId = priceListCache.findByConcatenatedKey(editProduct.getPrimaryPriceItem().getConcatenatedKey()).getId();
        itemList.put(new AutoCompleteToken(quotePriceItemId, editProduct.getPrimaryPriceItem().getDisplayName(), false).getJSONObject());

        return itemList.toString();
    }

    public String getPriceItemList() {
        return priceItemList;
    }

    public void setPriceItemList(String priceItemList) {
        this.priceItemList = priceItemList;
    }

    public String getAddOnList() {
        return addOnList;
    }

    public void setAddOnList(String addOnList) {
        this.addOnList = addOnList;
    }
}
