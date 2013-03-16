package org.broadinstitute.gpinformatics.athena.presentation.products;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidateNestedProperties;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductFamilyDao;
import org.broadinstitute.gpinformatics.athena.entity.products.*;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.MaterialTypeTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.PriceItemTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.ProductTokenInput;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

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

    // Risk criteria
    private String[] criteria = new String[0];
    private String[] operators = new String[0];
    private String[] values = new String[0];

    @ValidateNestedProperties({
        @Validate(field="productFamily.productFamilyId", required = true, maxlength=255, on={SAVE_ACTION}, label="Product Family"),
        @Validate(field="productName", required = true, maxlength=255, on={SAVE_ACTION}, label = "Product Name"),
        @Validate(field="partNumber", required = true, maxlength=255, on={SAVE_ACTION}, label="Part Number"),
        @Validate(field="description", required = true, maxlength = 2000, on={SAVE_ACTION}, label = "Description"),
        @Validate(field="availabilityDate", required = true, on={SAVE_ACTION}, label = "Availability Date")
    })
    private Product editProduct;

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
    public void validatePriceItems() {
        String[] duplicatePriceItems = editProduct.getDuplicatePriceItemNames();
        if (duplicatePriceItems != null) {
            addGlobalValidationError("Cannot save with duplicate price items: " + StringUtils.join(duplicatePriceItems, ", "));
        }

        // check for existing name for create or name change on edit
        if ((editProduct.getOriginalPartNumber() == null) ||
            (!editProduct.getPartNumber().equalsIgnoreCase(editProduct.getOriginalPartNumber()))) {

            Product existingProduct = productDao.findByPartNumber(editProduct.getPartNumber());
            if (existingProduct != null && ! existingProduct.getProductId().equals(editProduct.getProductId())) {
                addValidationError("partNumber", "Part number '" + editProduct.getPartNumber() + "' is already in use");
            }
        }

        // Check that the dates are consistent
        if ((editProduct.getAvailabilityDate() != null) &&
            (editProduct.getDiscontinuedDate() != null) &&
            (editProduct.getAvailabilityDate().after(editProduct.getDiscontinuedDate()))) {
            addGlobalValidationError("Availability date must precede discontinued date.");
        }

        if (priceItemTokenInput.getMercuryTokenObject() == null) {
            addValidationError("token-input-primaryPriceItem", "Primary price item is required");
        }

        // Ensure that numeric criteria have valid data.
        int matchingValueIndex = 0;
        for (String criterion : criteria) {
            RiskCriterion.RiskCriteriaType type = RiskCriterion.RiskCriteriaType.findByLabel(criterion);
            if (type.getOperatorType() == Operator.OperatorType.NUMERIC) {
                try {
                    Double.parseDouble(values[matchingValueIndex]);
                } catch (NumberFormatException e) {
                    addGlobalValidationError("Not a valid number for risk calculation: {2}", values[matchingValueIndex]);
                }
            }

            // Only increment the matching value if it is not boolean or if this is old style boolean where all indexes match
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
        optionalPriceItemTokenInput.setup(PriceItem.getPriceItemKeys(editProduct.getOptionalPriceItems()));
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

        // If all lengths match, just send it
        if (allLengthsMatch()) {
            editProduct.updateRiskCriteria(criteria, operators, values);
        } else {
            // Otherwise, there must be a boolean and we need to make them synchronized
            String[] fullOperators = new String[criteria.length];
            String[] fullValues = new String[criteria.length];

            // insert the operators and values for booleans, otherwise, use the next item
            int fullPosition = 0;
            int originalPosition = 0;
            for (String criterion : criteria) {
                RiskCriterion.RiskCriteriaType type = RiskCriterion.RiskCriteriaType.findByLabel(criterion);
                if (type.getOperatorType() == Operator.OperatorType.BOOLEAN) {
                    fullOperators[fullPosition] = type.getOperators().get(0).getLabel();
                    fullValues[fullPosition] = "true";
                } else {
                    fullOperators[fullPosition] = operators[originalPosition];
                    fullValues[fullPosition] = values[originalPosition];

                    // Only increment original position for values that are not boolean
                    originalPosition++;
                }

                // Always increment full position
                fullPosition++;
            }

            editProduct.updateRiskCriteria(criteria, fullOperators, fullValues);
        }

        productDao.persist(editProduct);
        addMessage("Product \"" + editProduct.getProductName() + "\" has been saved");
        return new RedirectResolution(ProductActionBean.class, VIEW_ACTION).addParameter(PRODUCT_PARAMETER,
                editProduct.getPartNumber());
    }

    private void populateTokenListFields() {
        editProduct.getAddOns().clear();
        editProduct.getAddOns().addAll(addOnTokenInput.getTokenObjects());

        editProduct.setPrimaryPriceItem(priceItemTokenInput.getMercuryTokenObject());

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
}
