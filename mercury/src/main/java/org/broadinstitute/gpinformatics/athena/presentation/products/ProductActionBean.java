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
import org.broadinstitute.gpinformatics.infrastructure.AutoCompleteToken;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPMaterialTypeList;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.json.JSONArray;
import org.json.JSONException;

import javax.inject.Inject;
import java.io.StringReader;
import java.util.*;

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

    // Data needed for displaying the view
    private List<ProductFamily> productFamilies;
    private List<Product> allProducts;

    @Validate(required = true, on = {VIEW_ACTION, EDIT_ACTION})
    private String product;

    @ValidateNestedProperties({
        @Validate(field="productFamily.productFamilyId", label="Product Family", required = true, maxlength=255, on={SAVE_ACTION}),
        @Validate(field="productName", required = true, maxlength=255, on={SAVE_ACTION})
    })
    private Product editProduct;


    // These are the fields for catching the input tokens
    @Validate(required = true, on = {SAVE_ACTION})
    private String primaryPriceItemList = "";

    private String optionalPriceItemsList = "";

    private String addOnList = "";

    private String materialTypeList = "";

    private Log logger = LogFactory.getLog(ProductActionBean.class);


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
    @Before(stages = LifecycleStage.BindingAndValidation, on = {VIEW_ACTION, EDIT_ACTION, SAVE_ACTION, "addOnsAutocomplete", "materialTypesAutocomplete"})
    public void init() {
        product = getContext().getRequest().getParameter(PRODUCT_PARAMETER);
        if (!StringUtils.isBlank(product)) {
            editProduct = productDao.findByBusinessKey(product);
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

    /* This method retrieves all possible material types */
    @HandlesEvent("materialTypesAutocomplete")
    public Resolution materialTypesAutocomplete() throws Exception {
        JSONArray itemList = new JSONArray();
        if ( materialTypeListCache != null ) {
            for (org.broadinstitute.bsp.client.sample.MaterialType bspMaterialType :  materialTypeListCache.find( getQ() )) {
                itemList.put(new AutoCompleteToken(bspMaterialType.getFullName(), bspMaterialType.getFullName(), false)
                        .getJSONObject());
            }
        } else {
             logger.error("Material Types Cache not available (in null) on ProductActionBean.");
        }
        return new StreamingResolution("text", new StringReader(itemList.toString()));
    }

    @HandlesEvent(value = SAVE_ACTION)
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
        editProduct.getAddOns().addAll(getAddOns());

        editProduct.setPrimaryPriceItem(getPrimaryPriceItem());

        editProduct.getAllowableMaterialTypes().clear();
        editProduct.getAllowableMaterialTypes().addAll( getMaterialTypes() );

        editProduct.getOptionalPriceItems().clear();
        editProduct.getOptionalPriceItems().addAll(getOptionalPriceItems());
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

    public List<Product> getAddOns() {
        if ((addOnList == null) || (addOnList.isEmpty())) {
            return Collections.emptyList();
        }

        List<String> addOnIdList = Arrays.asList(addOnList.split(","));
        return productDao.findByPartNumbers(addOnIdList);
    }

    public org.broadinstitute.gpinformatics.athena.entity.products.PriceItem getPrimaryPriceItem() {
        PriceItem priceItem = priceListCache.findById(Long.valueOf(primaryPriceItemList));

        org.broadinstitute.gpinformatics.athena.entity.products.PriceItem entity =
                priceItemDao.find(priceItem.getPlatformName(), priceItem.getCategoryName(), priceItem.getName());

        // If we don't have this price item, this will add it.
        if (entity == null) {
            entity = new org.broadinstitute.gpinformatics.athena.entity.products.PriceItem(
                priceItem.getId(), priceItem.getPlatformName(), priceItem.getCategoryName(), priceItem.getName());
        }

        return entity;
    }

    public List<MaterialType> getMaterialTypes() {
        if (StringUtils.isBlank(materialTypeList)) {
            return Collections.emptyList();
        }

        // Split up the comma separted MaterialTypes Ids into a List
        List<String> materialTypeIds = Arrays.asList(materialTypeList.split(","));

        //Get the list of DTOs from the cache.
        List<org.broadinstitute.bsp.client.sample.MaterialType> materialTypeDtoList =
                materialTypeListCache.getByFullNames( materialTypeIds );

        // Convert the Dtos to Entities.
        List<MaterialType> materialTypeEntities = convertDtosToEntities(materialTypeDtoList);

        return materialTypeEntities;
    }


    private List<MaterialType> convertDtosToEntities(
            List<org.broadinstitute.bsp.client.sample.MaterialType> materialTypeDtos) {

        List<MaterialType>  materialTypeEntities =
                new ArrayList<MaterialType>();

        if ( materialTypeDtos != null ) {
            for ( org.broadinstitute.bsp.client.sample.MaterialType materialTypeDto : materialTypeDtos ) {
                MaterialType materialTypeEntity =
                        new MaterialType( materialTypeDto.getCategory(), materialTypeDto.getName() );
                    materialTypeEntity.setFullName( materialTypeDto.getFullName() );
                materialTypeEntities.add( materialTypeEntity );
            }
        }

        return materialTypeEntities;
    }



    public List<org.broadinstitute.gpinformatics.athena.entity.products.PriceItem> getOptionalPriceItems() {

        List<org.broadinstitute.gpinformatics.athena.entity.products.PriceItem> optionalPriceItems =
                new ArrayList<org.broadinstitute.gpinformatics.athena.entity.products.PriceItem>();

        if ( ! StringUtils.isBlank(optionalPriceItemsList)) {

            List<String> priceItemIdList = Arrays.asList(optionalPriceItemsList.split(","));

            for (String priceItemId : priceItemIdList) {

                PriceItem priceItem = priceListCache.findById(Long.valueOf(priceItemId));

                org.broadinstitute.gpinformatics.athena.entity.products.PriceItem entity =
                        priceItemDao.find(priceItem.getPlatformName(), priceItem.getCategoryName(), priceItem.getName());

                // If we don't have this price item, this will add it.
                if (entity == null) {
                    entity = new org.broadinstitute.gpinformatics.athena.entity.products.PriceItem(
                            priceItem.getId(), priceItem.getPlatformName(), priceItem.getCategoryName(), priceItem.getName());
                }

                optionalPriceItems.add(entity);
            }
        }

        return optionalPriceItems;
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

    public String getPrimaryPriceItemCompleteData() throws Exception {
        if ((editProduct == null) || (editProduct.getPrimaryPriceItem() == null)) {
            return "";
        }

        JSONArray itemList = new JSONArray();
        PriceItem priceItem = priceListCache.findByConcatenatedKey(editProduct.getPrimaryPriceItem().getConcatenatedKey());
        if (priceItem == null) {
            return "invalid key: " + editProduct.getPrimaryPriceItem().getConcatenatedKey();
        }

        String quotePriceItemId = priceItem.getId();
        itemList.put(new AutoCompleteToken(quotePriceItemId, editProduct.getPrimaryPriceItem().getDisplayName(), false).getJSONObject());

        return itemList.toString();
    }


    public String getOptionalPriceItemsCompleteData() throws Exception {
        if ((editProduct == null) || (editProduct.getOptionalPriceItems() == null) || (editProduct.getOptionalPriceItems().isEmpty())) {
            return "";
        }

        JSONArray itemList = new JSONArray();

        for (org.broadinstitute.gpinformatics.athena.entity.products.PriceItem priceItemEntity : editProduct.getOptionalPriceItems()) {
            PriceItem priceItem = priceListCache.findByConcatenatedKey(priceItemEntity.getConcatenatedKey());
            if (priceItem == null) {
                return "invalid key: " + priceItemEntity.getConcatenatedKey();
            }
            String quotePriceItemId = priceItem.getId();
            itemList.put(new AutoCompleteToken(quotePriceItemId, priceItemEntity.getDisplayName(), false).getJSONObject());
        }

        return itemList.toString();
    }

    public String getPrimaryPriceItemList() {
        return primaryPriceItemList;
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
            itemList.put(new AutoCompleteToken(idName, idName, false).getJSONObject());
        }
        return itemList.toString();
    }

    public void setPrimaryPriceItemList(String primaryPriceItemList) {
        this.primaryPriceItemList = primaryPriceItemList;
    }

    public String getOptionalPriceItemsList() {
        return optionalPriceItemsList;
    }

    public void setOptionalPriceItemsList(String optionalPriceItemsList) {
        this.optionalPriceItemsList = optionalPriceItemsList;
    }

    public String getAddOnList() {
        return addOnList;
    }

    public void setAddOnList(String addOnList) {
        this.addOnList = addOnList;
    }

    public String getMaterialTypeList() {
        return materialTypeList;
    }

    public void setMaterialTypeList(String materialTypeList) {
        this.materialTypeList = materialTypeList;
    }
}
