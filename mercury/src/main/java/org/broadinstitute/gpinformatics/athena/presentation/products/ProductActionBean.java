package org.broadinstitute.gpinformatics.athena.presentation.products;

import com.lowagie.text.DocumentException;
import net.sourceforge.stripes.action.After;
import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidateNestedProperties;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.products.ProductEjb;
import org.broadinstitute.gpinformatics.athena.boundary.products.ProductPdfFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductFamilyDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Operator;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.athena.presentation.DisplayableItem;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.MaterialTypeTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.PriceItemTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.ProductTokenInput;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.AnalysisTypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao.IncludePDMOnly;
import static org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao.TopLevelOnly;

/**
 * This class supports all the actions done on products.
 */
@UrlBinding(ProductActionBean.ACTIONBEAN_URL_BINDING)
public class ProductActionBean extends CoreActionBean {
    private static final Log log = LogFactory.getLog(ProductActionBean.class);
    public static final String ACTIONBEAN_URL_BINDING = "/products/product.action";
    public static final String PRODUCT_PARAMETER = "product";

    public static final String PRODUCT_STRING = "Product";
    public static final String CREATE_PRODUCT = CoreActionBean.CREATE + PRODUCT_STRING;
    private static final String EDIT_PRODUCT = CoreActionBean.EDIT + PRODUCT_STRING;

    public static final String PRODUCT_CREATE_PAGE = "/products/create.jsp";
    public static final String PRODUCT_LIST_PAGE = "/products/list.jsp";
    public static final String PRODUCT_VIEW_PAGE = "/products/view.jsp";
    private static final String DOWNLOAD_PRODUCT_LIST = "downloadProductDescriptions";

    @Inject
    private ProductFamilyDao productFamilyDao;

    @Inject
    private ProductEjb productEjb;

    @Inject
    private ProductDao productDao;

    @Inject
    private ProductTokenInput addOnTokenInput;

    @Inject
    private PriceItemTokenInput priceItemTokenInput;

    @Inject
    private PriceListCache priceListCache;

    @Inject
    private MaterialTypeTokenInput materialTypeTokenInput;

    @Inject
    private AnalysisTypeDao analysisTypeDao;

    @Inject
    private ReagentDesignDao reagentDesignDao;

    // Data needed for displaying the view.
    private List<ProductFamily> productFamilies;
    private List<Product> allProducts;

    @Validate(required = true, on = {VIEW_ACTION, EDIT_ACTION})
    private String product;

    private String[] criteria = new String[0];
    private String[] operators = new String[0];
    private String[] values = new String[0];

    @Validate(required = true, on = {SAVE_ACTION})
    private Long productFamilyId;

    @ValidateNestedProperties({
            @Validate(field = "productName", required = true, maxlength = 255, on = {SAVE_ACTION},
                    label = "Product Name"),
            @Validate(field = "partNumber", required = true, maxlength = 255, on = {SAVE_ACTION},
                    label = "Part Number"),
            @Validate(field = "description", required = true, maxlength = 2000, on = {SAVE_ACTION},
                    label = "Description"),
            @Validate(field = "availabilityDate", required = true, on = {SAVE_ACTION}, label = "Availability Date")
    })
    private Product editProduct;

    public ProductActionBean() {
        super(CREATE_PRODUCT, EDIT_PRODUCT, PRODUCT_PARAMETER);
    }

    public ProductActionBean(UserBean userBean, ProductDao productDao) {
        super(CREATE_PRODUCT, EDIT_PRODUCT, PRODUCT_PARAMETER);
        this.userBean = userBean;
        this.productDao = productDao;
    }

    private String q;

    private ProductDao.Availability availability = ProductDao.Availability.CURRENT;

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    /**
     * Initialize the product with the passed in key for display in the form.
     */
    @Before(stages = LifecycleStage.BindingAndValidation,
            on = {VIEW_ACTION, EDIT_ACTION, CREATE_ACTION, SAVE_ACTION, "addOnsAutocomplete",
                    "materialTypesAutocomplete"})
    public void init() {
        product = getContext().getRequest().getParameter(PRODUCT_PARAMETER);
        if (!StringUtils.isBlank(product)) {
            editProduct = productDao.findByBusinessKey(product);
        } else {
            // This must be a create, so construct a new top level product that has nothing else set
            editProduct = new Product(Product.TOP_LEVEL_PRODUCT);
        }
    }

    @Before(stages = LifecycleStage.CustomValidation, on = SAVE_ACTION)
    public void initAfterValidation() {
        // We set product family here because we need it to validate the risk criteria.
        if (productFamilyId == null) {
            // by returning here, built-in validation of the required productFamilyId
            // will tell the user that they need to select a product family
            return;
        }
        if ((editProduct.getProductFamily() == null) || !productFamilyId.equals(
                editProduct.getProductFamily().getProductFamilyId())) {
            editProduct.setProductFamily(productFamilyDao.find(productFamilyId));
        }
    }

    /**
     * Need to get this for setting up create and edit and for any errors on save.
     */
    @Before(stages = LifecycleStage.BindingAndValidation, on = {CREATE_ACTION, EDIT_ACTION, SAVE_ACTION})
    public void setupFamilies() {
        productFamilies = productFamilyDao.findAll();
        Collections.sort(productFamilies);
    }

    /**
     * If the product exists then set any value that needs to be set up for display.
     */
    @After(stages = LifecycleStage.EventHandling, on = {CREATE_ACTION, EDIT_ACTION})
    public void populateFamilyId() {
        if (editProduct != null) {
            if (editProduct.getProductFamily() != null) {
                productFamilyId = editProduct.getProductFamily().getProductFamilyId();
            }
        }
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = LIST_ACTION)
    public void allProductsInit() {
        allProducts = productDao.findProducts(availability, TopLevelOnly.NO, IncludePDMOnly.YES);
    }

    /**
     * Validate information on the product being edited or created.
     */
    @ValidationMethod(on = SAVE_ACTION)
    public void validateForSave() {
        String[] duplicatePriceItems = editProduct.getDuplicatePriceItemNames();
        if (duplicatePriceItems != null) {
            addGlobalValidationError(
                    "Cannot save with duplicate price items: " + StringUtils.join(duplicatePriceItems, ", "));
        }

        // check for existing name for create or name change on edit.
        if ((editProduct.getOriginalPartNumber() == null) ||
            (!editProduct.getPartNumber().equalsIgnoreCase(editProduct.getOriginalPartNumber()))) {

            Product existingProduct = productDao.findByPartNumber(editProduct.getPartNumber());
            if (existingProduct != null && !existingProduct.getProductId().equals(editProduct.getProductId())) {
                addValidationError("partNumber", "Part number '" + editProduct.getPartNumber() + "' is already in use");
            }
        }

        // Check that the dates are consistent.
        if ((editProduct.getAvailabilityDate() != null) &&
            (editProduct.getDiscontinuedDate() != null) &&
            (editProduct.getAvailabilityDate().after(editProduct.getDiscontinuedDate()))) {
            addGlobalValidationError("Availability date must precede discontinued date.");
        }

        if (priceItemTokenInput.getItem() == null) {
            addValidationError("token-input-primaryPriceItem", "Primary price item is required");
        }

        checkValidCriteria();
    }

    private void checkValidCriteria() {
        // Ensure that numeric criteria have valid data, and that the product supports the requested criteria.
        int matchingValueIndex = 0;
        for (String criterion : criteria) {
            RiskCriterion.RiskCriteriaType type = RiskCriterion.RiskCriteriaType.findByLabel(criterion);
            if (type.getOperatorType() == Operator.OperatorType.NUMERIC) {
                if (values == null || matchingValueIndex >= values.length || values[matchingValueIndex] == null) {
                    addGlobalValidationError("Need to provide a value for risk criterion ''{2}''", criterion);
                } else {
                    try {
                        Double.parseDouble(values[matchingValueIndex]);
                    } catch (NumberFormatException e) {
                        addGlobalValidationError("Not a valid number for risk calculation: ''{2}''",
                                values[matchingValueIndex]);
                    }
                }
            }

            // If this is a RIN criterion and the product does not support RIN, give an error. This was still parsed
            // and validated as any other criterion.
            if ((type == RiskCriterion.RiskCriteriaType.RIN) && !editProduct.isSupportsRin()) {
                addGlobalValidationError("Cannot add a RIN criterion for product: {2} of family {3}",
                        editProduct.getDisplayName(), editProduct.getProductFamily().getName());
            }

            // If requesting pico age but does not support it, error it out.
            if ((type == RiskCriterion.RiskCriteriaType.PICO_AGE) && !editProduct.isSupportsPico()) {
                addGlobalValidationError("Cannot add Pico age criterion for product: {2} of family {3}",
                        editProduct.getDisplayName(), editProduct.getProductFamily().getName());
            }

            // Only increment the matching value if it is not boolean or if this is old style boolean where all indexes match.
            if ((type.getOperatorType() != Operator.OperatorType.BOOLEAN) || allLengthsMatch()) {
                matchingValueIndex++;
            }
        }
    }

    /**
     * @return There was a period where all lengths always matched because of using hidden fields for booleans, but this
     * was too error prone. Support both by checking all lengths here.
     */
    private boolean allLengthsMatch() {
        return (operators.length == criteria.length) && (criteria.length == values.length);
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

        PriceItem primaryPriceItem = editProduct.getPrimaryPriceItem();
        if (primaryPriceItem != null) {
            priceItemTokenInput.setup(PriceItem.getPriceItemKeys(Collections.singletonList(primaryPriceItem)));
        }
    }

    @HandlesEvent("addOnsAutocomplete")
    public Resolution addOnsAutocomplete() throws Exception {
        return createTextResolution(addOnTokenInput.getAddOnsJsonString(editProduct, getQ()));
    }

    @HandlesEvent("priceItemAutocomplete")
    public Resolution priceItemAutocomplete() throws Exception {
        return createTextResolution(priceItemTokenInput.getJsonString(getQ()));
    }

    /**
     * This method retrieves all possible material types.
     *
     * @return The autocomplete text.
     * @throws Exception Any errors.
     */
    @HandlesEvent("materialTypesAutocomplete")
    public Resolution materialTypesAutocomplete() throws Exception {
        return createTextResolution(materialTypeTokenInput.getJsonString(getQ()));
    }

    @HandlesEvent(SAVE_ACTION)
    public Resolution save() {
        productEjb.saveProduct(
                editProduct, addOnTokenInput, priceItemTokenInput, materialTypeTokenInput,
                allLengthsMatch(), criteria, operators, values);
        addMessage("Product \"" + editProduct.getProductName() + "\" has been saved");
        return new RedirectResolution(ProductActionBean.class, VIEW_ACTION).addParameter(PRODUCT_PARAMETER,
                editProduct.getPartNumber());
    }

    @HandlesEvent(DOWNLOAD_PRODUCT_LIST)
    public Resolution downloadProductDescriptions() {
        final List<Product> productDownloadList = getProductsForPdfDownload();
        String fileName = getPdfFilename(productDownloadList);

        if (productDownloadList.isEmpty()) {
            addGlobalValidationError("Could not create PDF file. No products found.");
            return getSourcePageResolution();
        }
        return new StreamingResolution("application/pdf") {
            @Override
            public void stream(final HttpServletResponse response) {
                Collections.sort(productDownloadList, Product.BY_FAMILY_THEN_PRODUCT_NAME);
                try {
                    setAttachment(true);
                    ProductPdfFactory.toPdf(response.getOutputStream(),
                            productDownloadList.toArray(new Product[productDownloadList.size()]));
                } catch (IOException | DocumentException e) {
                    String errorMessage = "Error generating PDF file.";
                    log.error(errorMessage, e);
                    addGlobalValidationError(errorMessage);
                }
            }
        }.setFilename(fileName);
    }

    public static String getPdfFilename(List<Product> productList) {
        String fileName = "Product Descriptions.pdf";
        if (productList.size() == 1) {
            fileName = productList.iterator().next().getName() + ".pdf";
        }
        return fileName;

    }

    protected List<Product> getProductsForPdfDownload() {
        List<Product> productDownloadList;
        if (!(editProduct == null || StringUtils.isBlank(editProduct.getPartNumber()))) {
            Product downloadProduct = productDao.findByPartNumber(editProduct.getPartNumber());
            productDownloadList = Arrays.asList(downloadProduct);
        } else {
            productDownloadList = productDao.findProducts(ProductDao.Availability.CURRENT, TopLevelOnly.NO,
                    IncludePDMOnly.toIncludePDMOnly(userBean.isPDMUser()));
        }
        return productDownloadList;
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

    public MaterialTypeTokenInput getMaterialTypeTokenInput() {
        return materialTypeTokenInput;
    }

    public void setMaterialTypeTokenInput(MaterialTypeTokenInput materialTypeTokenInput) {
        this.materialTypeTokenInput = materialTypeTokenInput;
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

    public List<Operator> getGetRequirementOperators() {
        return Operator.findOperatorsByType(Operator.OperatorType.NUMERIC);
    }

    public RiskCriterion.RiskCriteriaType[] getCriteriaTypes() {
        return RiskCriterion.RiskCriteriaType.values();
    }

    public String[] getValues() {
        return values;
    }

    public void setValues(String[] values) {
        this.values = values;
    }

    public String[] getOperators() {
        return operators;
    }

    public void setOperators(String[] operators) {
        this.operators = operators;
    }

    public String[] getCriteria() {
        return criteria;
    }

    public void setCriteria(String[] criteria) {
        this.criteria = criteria;
    }

    public Long getProductFamilyId() {
        return productFamilyId;
    }

    public void setProductFamilyId(Long productFamilyId) {
        this.productFamilyId = productFamilyId;
    }

    public Collection<QuotePriceItem> getReplacementPriceItems() {
        return priceListCache.getReplacementPriceItems(editProduct);
    }

    /**
     * @return Show the create title if this is a developer or PDM.
     */
    @Override
    public boolean isCreateAllowed() {
        return isEditAllowed();
    }

    /**
     * @return Show the edit title if this is a developer or PDM.
     */
    @Override
    public boolean isEditAllowed() {
        return getUserBean().isDeveloperUser() || getUserBean().isPDMUser();
    }

    /**
     * Get the list of available reagent designs.
     *
     * @return List of strings representing the reagent designs
     */
    public Collection<DisplayableItem> getReagentDesigns() {
        return makeDisplayableItemCollection(reagentDesignDao.findAll());
    }

    /**
     * Get the reagent design.
     *
     * @param businessKey the businessKey
     *
     * @return UI helper object {@link DisplayableItem} representing the reagent design
     */
    public DisplayableItem getReagentDesign(String businessKey) {
        return getDisplayableItemInfo(businessKey, reagentDesignDao);
    }

    /**
     * Get the analysis type.
     *
     * @param businessKey the businessKey
     *
     * @return UI helper object {@link DisplayableItem} representing the analysis type
     */
    public DisplayableItem getAnalysisType(String businessKey) {
        return getDisplayableItemInfo(businessKey, analysisTypeDao);
    }

    /**
     * Get the list of available analysis types.
     *
     * @return List of strings representing the analysis types
     */
    public Collection<DisplayableItem> getAnalysisTypes() {
        return makeDisplayableItemCollection(analysisTypeDao.findAll());
    }

    /**
     * Get the list of workflows with NONE removed.
     *
     * @return The workflows
     */
    public List<Workflow> getVisibleWorkflowList() {
        return Workflow.getVisibleWorkflowList();
    }

    public Workflow getWorkflowNone() {
        return Workflow.NONE;
    }

    public ProductDao.Availability getAvailability() {
        return availability;
    }

    public void setAvailability(ProductDao.Availability availability) {
        this.availability = availability;
    }
}
