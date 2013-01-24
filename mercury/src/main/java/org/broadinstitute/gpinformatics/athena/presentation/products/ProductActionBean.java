package org.broadinstitute.gpinformatics.athena.presentation.products;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductFamilyDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.MaterialTypeTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.PriceItemTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.ProductTokenInput;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPMaterialTypeList;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.json.JSONArray;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * This class supports all the actions done on products
 */
@UrlBinding(ProductActionBean.ACTIONBEAN_URL_BINDING)
public class ProductActionBean extends CoreActionBean {
    public static final String ACTIONBEAN_URL_BINDING = "/products/product.action";
    public static final String PRODUCT_PARAMETER = "product";

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

    @Inject
    private BSPMaterialTypeList materialTypeListCache;

    @Inject
    private ProductTokenInput addOnTokenInput;

    @Inject
    private PriceItemTokenInput priceItemTokenInput;

    @Inject
    private PriceItemTokenInput optionalPriceItemTokenInput;

    @Inject
    private MaterialTypeTokenInput materialTypeTokenInput;

    // Data needed for displaying the view
    private List<ProductFamily> productFamilies;
    private List<Product> allProducts;

    @Validate(required = true, on = {VIEW_ACTION, EDIT_ACTION})
    private String product;

    @ValidateNestedProperties({
        @Validate(field="productFamily.productFamilyId", label="Product Family", required = true, maxlength=255, on={SAVE_ACTION}),
        @Validate(field="productName", required = true, maxlength=255, on={SAVE_ACTION}),
        @Validate(field="partNumber", required = true, maxlength=255, on={SAVE_ACTION}, label="Part Number")
    })
    private Product editProduct;

    private final Log logger = LogFactory.getLog(ProductActionBean.class);

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
    @Before(stages = LifecycleStage.BindingAndValidation, on = {VIEW_ACTION, EDIT_ACTION, CREATE_ACTION, SAVE_ACTION, "addOnsAutocomplete", "materialTypesAutocomplete"})
    public void init() {
        product = getContext().getRequest().getParameter(PRODUCT_PARAMETER);
        if (!StringUtils.isBlank(product)) {
            editProduct = productDao.findByBusinessKey(product);
        } else {
            // This must be a create, so construct a new top level product that has nothing else set
            editProduct = new Product(Product.TOP_LEVEL_PRODUCT);
        }
    }

    /**
     * Need to get this for setting up create and edit and for any errors on save
     */
    @Before(stages = LifecycleStage.BindingAndValidation, on = {CREATE_ACTION, EDIT_ACTION, SAVE_ACTION})
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
    @ValidationMethod(on = SAVE_ACTION)
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
        populateTokenListsFromObjectData();
        return new ForwardResolution(PRODUCT_CREATE_PAGE);
    }

    @HandlesEvent(EDIT_ACTION)
    public Resolution edit() {
        setSubmitString(EDIT_PRODUCT);
        populateTokenListsFromObjectData();
        return new ForwardResolution(PRODUCT_CREATE_PAGE);
    }

    /**
     * For the prepopulate to work on opening create and edit page, we need to take values from the editOrder. After,
     * the pages have the values passed in.
     */
    private void populateTokenListsFromObjectData() {
        addOnTokenInput.setup(editProduct.getAddOnBusinessKeys());
        materialTypeTokenInput.setup(editProduct.getAllowableMaterialTypeNames());
        priceItemTokenInput.setup(editProduct.getPriceItemIds());
        optionalPriceItemTokenInput.setup(editProduct.getPriceItemIds());
    }

    @HandlesEvent("addOnsAutocomplete")
    public Resolution addOnsAutocomplete() throws Exception {
        return createTextResolution(addOnTokenInput.getAddOnsJsonString(editProduct, getQ()));
    }

    @HandlesEvent("priceItemAutocomplete")
    public Resolution priceItemAutocomplete() throws Exception {
        return createTextResolution(priceItemTokenInput.getJsonString(getQ()));
    }

    /* This method retrieves all possible material types */
    @HandlesEvent("materialTypesAutocomplete")
    public Resolution materialTypesAutocomplete() throws Exception {
        return createTextResolution(materialTypeTokenInput.getJsonString(getQ()));
    }

    @HandlesEvent(SAVE_ACTION)
    public Resolution save() {
        populateTokenListFields();

        editProduct.setProductFamily(productFamilyDao.find(editProduct.getProductFamily().getProductFamilyId()));

        productDao.persist(editProduct);
        addMessage("Product \"" + editProduct.getProductName() + "\" has been saved");
        return new RedirectResolution(ProductActionBean.class, VIEW_ACTION).addParameter(PRODUCT_PARAMETER,
                editProduct.getPartNumber());
    }

    private void populateTokenListFields() {
        editProduct.getAddOns().clear();
        editProduct.getAddOns().addAll(addOnTokenInput.getTokenObjects());

        editProduct.setPrimaryPriceItem(priceItemTokenInput.getTokenObject());

        editProduct.getAllowableMaterialTypes().clear();
        editProduct.getAllowableMaterialTypes().addAll(materialTypeTokenInput.getMercuryTokenObjects());

        editProduct.getOptionalPriceItems().clear();
        editProduct.getOptionalPriceItems().addAll(optionalPriceItemTokenInput.getMercuryTokenObjects());
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

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public List<ProductFamily> getProductFamilies() {
        return productFamilies;
    }

    private static List<MaterialType> convertDtosToEntities(
            List<org.broadinstitute.bsp.client.sample.MaterialType> materialTypeDtos) {

        if (materialTypeDtos != null) {
            List<MaterialType> materialTypeEntities = new ArrayList<MaterialType>(materialTypeDtos.size());
            for (org.broadinstitute.bsp.client.sample.MaterialType materialTypeDto : materialTypeDtos) {
                MaterialType materialTypeEntity =
                        new MaterialType(materialTypeDto.getCategory(), materialTypeDto.getName());
                materialTypeEntity.setFullName(materialTypeDto.getFullName());
                materialTypeEntities.add(materialTypeEntity);
            }
            return materialTypeEntities;
        }

        return Collections.emptyList();
    }
    /* This method retrieves any material types already set on the product and
     * returns a JsonArray as a string
     */
    public String getMaterialTypeCompleteData() throws Exception {
        if ((editProduct == null) || (editProduct.getAllowableMaterialTypes() == null)) {
            return "";
        }

        JSONArray itemList = new JSONArray();
        Set<MaterialType> materialTypes =  editProduct.getAllowableMaterialTypes();
        for ( MaterialType materialType : materialTypes ) {
            String idName = materialType.getCategory() + ":" + materialType.getName();
            itemList.put(TokenInput.getJSONObject(idName, idName, false));
        }
        return itemList.toString();
    }

    public MaterialTypeTokenInput getMaterialTypeTokenInput() {
        return materialTypeTokenInput;
    }

    public void setMaterialTypeTokenInput(MaterialTypeTokenInput materialTypeTokenInput) {
        this.materialTypeTokenInput = materialTypeTokenInput;
    }

    public PriceItemTokenInput getOptionalPriceItemTokenInput() {
        return optionalPriceItemTokenInput;
    }

    public void setOptionalPriceItemTokenInput(PriceItemTokenInput optionalPriceItemTokenInput) {
        this.optionalPriceItemTokenInput = optionalPriceItemTokenInput;
    }

    public PriceItemTokenInput getPriceItemTokenInput() {
        return priceItemTokenInput;
    }

    public void setPriceItemTokenInput(PriceItemTokenInput priceItemTokenInput) {
        this.priceItemTokenInput = priceItemTokenInput;
    }

    public ProductTokenInput getAddOnTokenInput() {
        return addOnTokenInput;
    }

    public void setAddOnTokenInput(ProductTokenInput addOnTokenInput) {
        this.addOnTokenInput = addOnTokenInput;
    }
}
