package org.broadinstitute.gpinformatics.athena.presentation.products;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationErrors;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductFamilyDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.AutoCompleteToken;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.json.JSONArray;

import javax.inject.Inject;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;

/**
 * This class supports all the actions done on products
 */
@UrlBinding("/products/product.action")
public class ProductActionBean extends CoreActionBean{
    private static final String CREATE = "Create New Product";
    private static final String EDIT = "Edit Product: ";

    public static final String PROUDCT_CREATE_PAGE = "/products/create.jsp";
    public static final String PRODUCT_LIST_PAGE = "/products/list.jsp";
    public static final String PRODUCT_VIEW_PAGE = "/products/view.jsp";

    @Inject
    private ProductFamilyDao productFamilyDao;

    @Inject
    private ProductDao productDao;

    // Data needed for displaying the view
    private List<ProductFamily> productFamilies;
    private List<Product> allProducts;

    @Validate(required = true, on = {"view", "edit"})
    private String productKey;

    private Product editProduct;

    private String submitString = CREATE;

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
    @Before(stages = LifecycleStage.BindingAndValidation, on = {"view", "edit", "save", "addOnsAutocomplete"})
    public void init() {
        productKey = getContext().getRequest().getParameter("productKey");
        if (productKey == null) {
            editProduct = productDao.findByBusinessKey(productKey);
        }
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = {"create", "edit"})
    public void setupFamilies() {
        productFamilies = productFamilyDao.findAll();
        Collections.sort(productFamilies);
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = {"list"})
    public void listInit() {
        allProducts = productDao.findAll(Product.class);
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = {"save", "addOnsAutocomplete"})
    public void setupOptionalProductKey() {
        if (productKey == null) {
            editProduct = productDao.findByBusinessKey(productKey);
        }
    }

    /**
     * Validate information on the product being edited or created
     */
    @ValidationMethod(on = {"save"})
    public void validatePriceItems(ValidationErrors errors) {
        String[] duplicatePriceItems = editProduct.getDuplicatePriceItemNames();
        if (duplicatePriceItems != null) {
            errors.addGlobalError(new SimpleError("Cannot save with duplicate price items: " + StringUtils.join(duplicatePriceItems, ", ")));
        }

        // Is there a product with the part number already?
        Product existingProduct = productDao.findByPartNumber(editProduct.getPartNumber());
        if (existingProduct != null && ! existingProduct.getProductId().equals(editProduct.getProductId())) {
            errors.addGlobalError(new SimpleError("Part number '" + editProduct.getPartNumber() + "' is already in use"));
        }

        if ((editProduct.getAvailabilityDate() != null) &&
            (editProduct.getDiscontinuedDate() != null) &&
            (editProduct.getAvailabilityDate().after(editProduct.getDiscontinuedDate()))) {
            errors.addGlobalError(new SimpleError("Availability date must precede discontinued date."));
        }
    }

    @DefaultHandler
    @HandlesEvent("list")
    public Resolution list() {
        return new ForwardResolution(PRODUCT_LIST_PAGE);
    }

    @HandlesEvent("view")
    public Resolution view() {
        return new ForwardResolution(PRODUCT_VIEW_PAGE);
    }

    @HandlesEvent("create")
    public Resolution create() {
        submitString = CREATE;
        return new ForwardResolution(PROUDCT_CREATE_PAGE);
    }

    @HandlesEvent("edit")
    public Resolution edit() {
        submitString = EDIT;
        return new ForwardResolution(PROUDCT_CREATE_PAGE);
    }

    @HandlesEvent("addOnsAutocomplete")
    public Resolution addOnsAutocomplete() throws Exception {
        List<Product> addOns = productDao.searchProductsForAddonsInProductEdit(editProduct, getQ());

        JSONArray itemList = new JSONArray();
        for (Product addOn : addOns) {
            itemList.put(new AutoCompleteToken(addOn.getBusinessKey(), addOn.getProductLabel(), false).getJSONObject());
        }

        return new StreamingResolution("text", new StringReader(itemList.toString()));
    }

    @HandlesEvent(value = "save")
    public Resolution save() {
        try {
            productDao.persist(editProduct);
        } catch (Exception e ) {
            addGlobalValidationError(e.getMessage());
            return null;
        }

        addMessage("Product \"" + editProduct.getProductName() + "\" has been created");
        return new RedirectResolution(PRODUCT_VIEW_PAGE).addParameter("product", editProduct.getBusinessKey());
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

    public String getSubmitString() {
        return submitString;
    }

    public boolean isCreating() {
        return CREATE.equals(submitString);
    }
}
