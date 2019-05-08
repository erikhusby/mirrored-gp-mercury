package org.broadinstitute.gpinformatics.athena.presentation.products;

import com.itextpdf.text.DocumentException;
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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.products.ProductEjb;
import org.broadinstitute.gpinformatics.athena.boundary.products.ProductPdfFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductFamilyDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.athena.entity.products.Operator;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.products.Product_;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.presentation.DisplayableItem;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.PriceItemTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.ProductTokenInput;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.jpa.BusinessObject;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPProductPriceCache;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.AnalysisTypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

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
    public static final String PUBLISH_TO_SAP = "publishToSap";
    public static final String OPEN_RISK_SUGGESTIONS = "openRiskSuggestedValues";

    public static final String PRODUCT_CREATE_PAGE = "/products/create.jsp";
    public static final String PRODUCT_LIST_PAGE = "/products/list.jsp";
    public static final String PRODUCT_VIEW_PAGE = "/products/view.jsp";
    private static final String DOWNLOAD_PRODUCT_LIST = "downloadProductDescriptions";
    private static final String PUBLISH_PRODUCTS_TO_SAP = "publishProductsToSap";
    private static final String RISK_CRITERIA_SUGGESTED_VALUES = "risk_criteria_suggested_values.jsp";

    @Inject
    private ProductFamilyDao productFamilyDao;

    @Inject
    private ProductEjb productEjb;

    private ProductDao productDao;

    @Inject
    private ProductTokenInput addOnTokenInput;

    @Inject
    private PriceItemTokenInput priceItemTokenInput;

    @Inject
    private PriceItemTokenInput externalPriceItemTokenInput;

    @Inject
    private PriceListCache priceListCache;

    @Inject
    private AnalysisTypeDao analysisTypeDao;

    @Inject
    private ReagentDesignDao reagentDesignDao;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private WorkflowConfig workflowConfig;

    @Inject
    private SAPProductPriceCache productPriceCache;

    // Data needed for displaying the view.
    private List<ProductFamily> productFamilies;
    private List<Product> allProducts;

    private List<String> selectedProductPartNumbers;
    private List<Product> selectedProducts;

    private List<String> criteriaSelectionValues = new ArrayList<>();


    @Validate(required = true, on = {VIEW_ACTION, EDIT_ACTION})
    private String product;

    private String[] criteria = new String[0];
    private String[] operators = new String[0];
    private String[] values = new String[0];

    // Genotyping chip name, pdo substring, chip technology for the current product.
    private List<Triple<String, String, String>> genotypingChipInfo = new ArrayList<>();
    private String[] genotypingChipTechnologies = new String[0];
    private String[] genotypingChipNames = new String[0];
    private String[] genotypingChipPdoSubstrings = new String[0];

    // Map of chip technology to chip names, for populating UI dropdowns.
    private Map<String, SortedSet<String>> availableChipTechnologyAndChipNames = new HashMap<>();

    @Validate(required = true, on = {SAVE_ACTION})
    private Long productFamilyId;

    private String controlsProject;

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

    private boolean productUsedInOrders = false;
    private List<String> suggestedValueSelections = new ArrayList();

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

    private String criteriaIndex;
    private String criteriaLabel;
    private String criteriaOp;
    private String currentCriteriaChoices;

    /**
     * Initialize the product with the passed in key for display in the form.
     */
    @Before(stages = LifecycleStage.BindingAndValidation, on = {"addOnsAutocomplete"})
    public void addOnsAutocompleteBindingAndValidation() {
        initProduct();
    }

    @Before(stages = LifecycleStage.BindingAndValidation, on = {VIEW_ACTION, PUBLISH_TO_SAP})
    public void viewBindingAndValidation() {
        initProduct();
        initGenotypingInfo();
    }

    @Before(stages = LifecycleStage.BindingAndValidation, on = {EDIT_ACTION, CREATE_ACTION})
    public void editCreateBindingAndValidation() {
        initProduct();
        initGenotypingInfo();
        setupFamilies();
    }

    @Before(stages = LifecycleStage.BindingAndValidation, on = {SAVE_ACTION})
    public void saveBindingAndValidation() {
        initProduct();
        setupFamilies();
    }

    private void initProduct() {
        product = getContext().getRequest().getParameter(PRODUCT_PARAMETER);
        if (!StringUtils.isBlank(product)) {
            editProduct = productDao.findByBusinessKey(product);
        } else {
            // This must be a create, so construct a new top level product that has nothing else set
            editProduct = new Product(Product.TOP_LEVEL_PRODUCT);
        }
        productPriceCache.refreshCache();
        availableChipTechnologyAndChipNames = productEjb.findChipFamiliesAndNames();
    }

    private void initGenotypingInfo() {
        genotypingChipInfo = productEjb.getCurrentMappedGenotypingChips(editProduct.getPartNumber());
    }

    @Before(stages = LifecycleStage.BindingAndValidation, on = {CREATE_ACTION, EDIT_ACTION, SAVE_ACTION})
    private void setupFamilies() {
        productFamilies = productFamilyDao.findAll();
        Collections.sort(productFamilies);
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

        if (editProduct.getPositiveControlResearchProject() == null ||
                !editProduct.getPositiveControlResearchProject().getBusinessKey().equals(controlsProject)) {
            editProduct.setPositiveControlResearchProject(controlsProject == null ? null :
                    researchProjectDao.findByBusinessKey(controlsProject));
        }
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
            if (editProduct.getPositiveControlResearchProject() != null) {
                controlsProject = editProduct.getPositiveControlResearchProject().getBusinessKey();
            }

            List<ProductOrder> productOrderList = null;
            if (editProduct.getProductId() != null) {
                productOrderList = productDao.findList(ProductOrder.class, ProductOrder_.product, editProduct);
            }
            productUsedInOrders = !CollectionUtils.isEmpty(productOrderList);
        }
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = LIST_ACTION)
    public void allProductsInit() {
        allProducts = productDao.findProducts(availability, TopLevelOnly.NO, IncludePDMOnly.YES);

        for (Product product: allProducts) {
            Optional <PriceItem> primaryPriceItem = Optional.ofNullable(product.getPrimaryPriceItem());
            primaryPriceItem.ifPresent(priceItem -> {
                final QuotePriceItem quotePriceItem = priceListCache.findByKeyFields(priceItem);
                if (quotePriceItem != null) {
                    priceItem.setPrice(quotePriceItem.getPrice());
                    priceItem.setUnits(quotePriceItem.getUnit());

                }
            });
            Product.setMaterialOnProduct(product, productPriceCache);
        }
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

        if (editProduct.getPartNumber().length() > Product.MAX_PART_NUMBER_LENGTH) {
            addValidationError("partNumber",
                "Part number '" + editProduct.getPartNumber() + "' is larger than maximum allowed size of "
                + Product.MAX_PART_NUMBER_LENGTH + " characters.'");
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

        checkValidCriteria();

        // Strips off "item" prefix.
        List<String> nakedPdoSubstrings = new ArrayList<>();
        for (String prefixedString : genotypingChipPdoSubstrings) {
            String nakedString = StringUtils.trimToNull(StringUtils.substringAfter(prefixedString, " "));
            // pdoSubstring must be unique since product part number + pdoSubstring must form a unique lookup key.
            if (nakedPdoSubstrings.contains(nakedString)) {
                addGlobalValidationError("Cannot have duplicate genotyping chip product order name substrings: " +
                                         nakedString);
            }
            nakedPdoSubstrings.add(nakedString);
        }
        genotypingChipPdoSubstrings = nakedPdoSubstrings.toArray(new String[0]);

        // Repacks genotyping parameters into a list for persistence and to redisplay in case of validation failure.
        genotypingChipInfo.clear();
        for (int i = 0; i < genotypingChipTechnologies.length; ++i) {
            genotypingChipInfo.add(
                    Triple.of(genotypingChipTechnologies[i], genotypingChipNames[i], genotypingChipPdoSubstrings[i]));
        }

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


    @HandlesEvent("externalPriceItemAutocomplete")
    public Resolution externalPriceItemAutocomplete() throws Exception {
        return createTextResolution(externalPriceItemTokenInput.getExternalJsonString(getQ()));
    }


    @HandlesEvent(SAVE_ACTION)
    public Resolution save() {
        // Sets paired end non-null when sequencing params are present.
        if (StringUtils.isNotBlank(editProduct.getAggregationDataType())) {
            editProduct.setPairedEndRead(editProduct.getPairedEndRead());
        }
        productEjb.saveProduct(editProduct, addOnTokenInput, priceItemTokenInput, allLengthsMatch(),
                criteria, operators, values, genotypingChipInfo, externalPriceItemTokenInput);
        addMessage("Product \"" + editProduct.getProductName() + "\" has been saved");
            try {
                productEjb.publishProductToSAP(editProduct);
            } catch (SAPIntegrationException e) {
                addGlobalValidationError("Unable to update the product in SAP. " + e.getMessage());
            }

        return new RedirectResolution(ProductActionBean.class, VIEW_ACTION).addParameter(PRODUCT_PARAMETER,
                editProduct.getPartNumber());
    }

    @HandlesEvent(PUBLISH_PRODUCTS_TO_SAP)
    public Resolution publishProductsToSap() {
        if(CollectionUtils.isEmpty(selectedProductPartNumbers)) {
            addGlobalValidationError("Select at least one product when publishing products in bulk.");
        } else {
            selectedProducts =
                    productDao.findListByList(Product.class, Product_.partNumber, selectedProductPartNumbers);
            try {
                productEjb.publishProductsToSAP(selectedProducts);
            } catch (ValidationException e) {
                addGlobalValidationError("Unable to publish some of the products to SAP. " + e.getMessage("<br/>"));
            }
        }
        return new RedirectResolution(ProductActionBean.class,LIST_ACTION);
    }

    @HandlesEvent(PUBLISH_TO_SAP)
    public Resolution publishToSap() {
        try {
            productEjb.publishProductToSAP(editProduct);
            addMessage("Product \"" + editProduct.getProductName() + "\" Successfully published to SAP");
        } catch (SAPIntegrationException e) {
            addGlobalValidationError("Unable to publish the product to SAP. " + e.getMessage());
        }

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

    @HandlesEvent(OPEN_RISK_SUGGESTIONS)
    public Resolution openRiskSuggestedValues() throws Exception {
        RiskCriterion.RiskCriteriaType criterion = RiskCriterion.RiskCriteriaType.findByLabel(criteriaLabel);
        Optional<String> optionalCriterion = Optional.ofNullable(currentCriteriaChoices);
        optionalCriterion.ifPresent(s -> suggestedValueSelections =
                Arrays.stream(s.split(",")).map(String::trim).collect(Collectors.toList()));

        if(CollectionUtils.isNotEmpty(criterion.getSuggestedValues())) {
            criteriaSelectionValues.addAll(criterion.getSuggestedValues());
        }
        return new ForwardResolution(RISK_CRITERIA_SUGGESTED_VALUES);
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
                    IncludePDMOnly.toIncludePDMOnly(userBean.isPDMUser(), userBean.isGPPMUser()));
        }
        return productDownloadList;
    }

    public Product getEditProduct() {
        return editProduct;
    }

    public boolean isProductNameSet() {
        return StringUtils.isNotBlank(editProduct.getPartNumber());
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

    public PriceItemTokenInput getPriceItemTokenInput() {
        return priceItemTokenInput;
    }

    public void setPriceItemTokenInput(PriceItemTokenInput priceItemTokenInput) {
        this.priceItemTokenInput = priceItemTokenInput;
    }

    public PriceItemTokenInput getExternalPriceItemTokenInput() {
        return externalPriceItemTokenInput;
    }

    public void setExternalPriceItemTokenInput(
            PriceItemTokenInput externalPriceItemTokenInput) {
        this.externalPriceItemTokenInput = externalPriceItemTokenInput;
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

    public Collection<QuotePriceItem> getExternalReplacementPriceItems() {
        return priceListCache.getReplacementPriceItems(editProduct.getExternalPriceItem());
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
     * Get the list of research projects for controls.
     *
     * @return List of strings representing research project names
     */
    public Collection<DisplayableItem> getControlsProjects() {
        List<ResearchProject> researchProjects = researchProjectDao.findLikeTitle("Control");
        Collections.sort(researchProjects, new Comparator<ResearchProject>() {
            @Override
            public int compare(ResearchProject o1, ResearchProject o2) {
                return o1.getJiraTicketKey().compareTo(o2.getJiraTicketKey());
            }
        });
        Collection<DisplayableItem> displayableItems = new ArrayList<>(researchProjects.size());

        for (BusinessObject item : researchProjects) {
            displayableItems.add(new DisplayableItem(item.getBusinessKey(),
                    item.getBusinessKey() + " - " + item.getName()));
        }
        return displayableItems;
    }

    /**
     * Get the list of workflows.
     *
     * @return all workflows
     */
    public Set<String> getAvailableWorkflows() {
        Set<String> workflows = new TreeSet<>();
        List<ProductWorkflowDef> productWorkflowDefs = workflowConfig.getProductWorkflowDefs();
        for (ProductWorkflowDef productWorkflowDef : productWorkflowDefs) {
            workflows.add(productWorkflowDef.getName());
        }
        return workflows;
    }

    public boolean productInSAP(String partNumber, SapIntegrationClientImpl.SAPCompanyConfiguration companyCode) {

        return productPriceCache.findByPartNumber(partNumber, companyCode) != null;
    }

    public ProductDao.Availability getAvailability() {
        return availability;
    }

    public void setAvailability(ProductDao.Availability availability) {
        this.availability = availability;
    }

    public String getControlsProject() {
        return controlsProject;
    }

    public void setControlsProject(String controlsProject) {
        this.controlsProject = controlsProject;
    }

    public String[] getGenotypingChipTechnologies() {
        return genotypingChipTechnologies;
    }

    public void setGenotypingChipTechnologies(String[] genotypingChipTechnologies) {
        this.genotypingChipTechnologies = genotypingChipTechnologies;
    }

    public String[] getGenotypingChipNames() {
        return genotypingChipNames;
    }

    public void setGenotypingChipNames(String[] genotypingChipNames) {
        this.genotypingChipNames = genotypingChipNames;
    }

    public String[] getGenotypingChipPdoSubstrings() {
        return genotypingChipPdoSubstrings;
    }

    public void setGenotypingChipPdoSubstrings(String[] genotypingChipPdoSubstrings) {
        this.genotypingChipPdoSubstrings = genotypingChipPdoSubstrings;
    }

    public List<Triple<String, String, String>> getGenotypingChipInfo() {
        return genotypingChipInfo;
    }

    public void setGenotypingChipInfo(List<Triple<String, String, String>> genotypingChipInfo) {
        this.genotypingChipInfo = genotypingChipInfo;
    }

    public Map<String, SortedSet<String>> getAvailableChipTechnologyAndChipNames() {
        return availableChipTechnologyAndChipNames;
    }

    public String getPublishSAPAction() {
        return PUBLISH_TO_SAP;
    }


    public List<String> getSelectedProductPartNumbers() {
        return selectedProductPartNumbers;
    }

    public void setSelectedProductPartNumbers(List<String> selectedProductPartNumbers) {
        this.selectedProductPartNumbers = selectedProductPartNumbers;
    }

    public SAPProductPriceCache getProductPriceCache() {
        return productPriceCache;
    }

    public boolean isProductUsedInOrders() {
        return productUsedInOrders;
    }


    public List<String> getCriteriaSelectionValues() {
        return criteriaSelectionValues;
    }

    public void setCriteriaSelectionValues(List<String> criteriaSelectionValues) {
        this.criteriaSelectionValues = criteriaSelectionValues;
    }

    public String getCriteriaIndex() {
        return criteriaIndex;
    }

    public void setCriteriaIndex(String criteriaIndex) {
        this.criteriaIndex = criteriaIndex;
    }

    public String getCriteriaLabel() {
        return criteriaLabel;
    }

    public void setCriteriaLabel(String criteriaLabel) {
        this.criteriaLabel = criteriaLabel;
    }

    public String getCriteriaOp() {
        return criteriaOp;
    }

    public void setCriteriaOp(String criteriaOp) {
        this.criteriaOp = criteriaOp;
    }

    public String getCurrentCriteriaChoices() {
        return currentCriteriaChoices;
    }

    public void setCurrentCriteriaChoices(String currentCriteriaChoices) {
        this.currentCriteriaChoices = currentCriteriaChoices;
    }


    public List<String> getSuggestedValueSelections() {
        return suggestedValueSelections;
    }

    public void setSuggestedValueSelections(List<String> suggestedValueSelections) {
        this.suggestedValueSelections = suggestedValueSelections;
    }

    @Inject
    protected void setProductDao(ProductDao productDao) {
        this.productDao = productDao;
    }
}
