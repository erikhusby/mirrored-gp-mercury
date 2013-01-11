package org.broadinstitute.gpinformatics.athena.presentation.orders;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.IOUtils;
import org.broadinstitute.gpinformatics.athena.boundary.orders.SampleLedgerExporter;
import org.broadinstitute.gpinformatics.athena.boundary.util.AbstractSpreadsheetExporter;
import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingLedgerDao;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderListEntryDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedger;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderListEntry;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.presentation.billing.BillingSessionActionBean;
import org.broadinstitute.gpinformatics.athena.presentation.links.BspLink;
import org.broadinstitute.gpinformatics.athena.presentation.links.JiraLink;
import org.broadinstitute.gpinformatics.athena.presentation.links.QuoteLink;
import org.broadinstitute.gpinformatics.athena.presentation.products.ProductActionBean;
import org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

/**
 * This handles all the needed interface processing elements
 */
@UrlBinding(ProductOrderActionBean.ACTIONBEAN_URL_BINDING)
public class ProductOrderActionBean extends CoreActionBean {
    public static final String ACTIONBEAN_URL_BINDING = "/orders/order.action";
    public static final String PRODUCT_ORDER_PARAMETER = "productOrder";

    private static final String CURRENT_OBJECT = "Product Order";
    public static final String CREATE_ORDER = CoreActionBean.CREATE + CURRENT_OBJECT;
    public static final String EDIT_ORDER = CoreActionBean.EDIT + CURRENT_OBJECT;

    private static final String ORDER_CREATE_PAGE = "/orders/create.jsp";
    private static final String ORDER_LIST_PAGE = "/orders/list.jsp";
    private static final String ORDER_VIEW_PAGE = "/orders/view.jsp";

    @Inject
    private QuoteService quoteService;

    @Inject
    private ProductOrderListEntryDao orderListEntryDao;

    @Inject
    private JiraLink jiraLink;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private QuoteLink quoteLink;

    @Inject
    private BspLink bspLink;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private ResearchProjectDao projectDao;

    @Inject
    private BillingSessionDao billingSessionDao;

    @Inject
    private BillingLedgerDao billingLedgerDao;

    private List<ProductOrderListEntry> allProductOrders;

    @Validate(required = true, on = {VIEW_ACTION, EDIT_ACTION})
    private String productOrder;

    @ValidateNestedProperties({
        @Validate(field="comments", maxlength=2000, on={SAVE_ACTION}),
        @Validate(field="title", required = true, maxlength=255, on={SAVE_ACTION}, label = "Name")
    })
    private ProductOrder editOrder;

    private List<String> selectedProductOrderBusinessKeys;
    private List<ProductOrder> selectedProductOrders;

    // For the Add-ons update we need the product title
    @Validate(required = true, on = {"getAddOns"})
    private String product;

    private List<String> addOnKeys = new ArrayList<String> ();

    // The token input autocomplete backing objects
    private String researchProjectList;
    private String productList;

    /**
     * Initialize the product with the passed in key for display in the form
     */
    @Before(stages = LifecycleStage.BindingAndValidation, on = {VIEW_ACTION, EDIT_ACTION, "downloadBillingTracker", SAVE_ACTION, "placeOrder"})
    public void init() {
        productOrder = getContext().getRequest().getParameter(PRODUCT_ORDER_PARAMETER);
        if (!StringUtils.isBlank(productOrder)) {
            editOrder = productOrderDao.findByBusinessKey(productOrder);
        } else {
            editOrder = new ProductOrder(getUserBean().getBspUser());
        }
    }

    @ValidationMethod(on = SAVE_ACTION)
    public void uniqueNameValidation(ValidationErrors errors) {
        // If the research project has no original title, then it was not fetched from hibernate, so this is a create
        // OR if this was fetched and the title has been changed
        if ((editOrder.getOriginalTitle() == null) ||
                (!editOrder.getTitle().equalsIgnoreCase(editOrder.getOriginalTitle()))) {

            // Check if there is an existing research project and error out if it already exists
            ProductOrder existingOrder = productOrderDao.findByTitle(editOrder.getTitle());
            if (existingOrder != null) {
                errors.add("title", new SimpleError("A product order already exists with this name."));
            }
        }
    }

    @ValidationMethod(on = "placeOrder")
    public void validateOrderPlacement(ValidationErrors errors) throws Exception {
        if (editOrder.getSamples().isEmpty()) {
            errors.addGlobalError(new SimpleError("Order does not have any samples"));
        }

        if (editOrder.getResearchProject() == null) {
            errors.addGlobalError(new SimpleError("Cannot place order '" + editOrder.getBusinessKey() + "' because it does not have a research project"));
        }

        if (editOrder.getQuoteId() == null) {
            errors.addGlobalError(new SimpleError("Cannot place order '" + editOrder.getBusinessKey() + "' because it does not have a quote specified"));
        }

        if (editOrder.getProduct() == null) {
            errors.addGlobalError(new SimpleError("Cannot place order '" + editOrder.getBusinessKey() + "' because it does not have a product"));
        }

        if (editOrder.getProduct() == null) {
            errors.addGlobalError(new SimpleError("Cannot place order '" + editOrder.getCount() + "' because it does not have a specified number of lanes"));
        }

        try {
            quoteService.getQuoteByAlphaId(editOrder.getQuoteId());
        } catch (QuoteServerException ex) {
            errors.addGlobalError(new SimpleError("The quote id " + editOrder.getQuoteId() + " is not valid: " + ex.getMessage()));
        } catch (QuoteNotFoundException ex) {
            errors.addGlobalError(new SimpleError("The quote id " + editOrder.getQuoteId() + " is not found"));
        }
    }

    @ValidationMethod(on = {"startBilling", "downloadBillingTracker"})
    public void validateOrderSelection(ValidationErrors errors) {
        if (!getUserBean().isValidBspUser()) {
            errors.addGlobalError(new SimpleError("This requires that you be a valid bsp user "));
        }

        if ((selectedProductOrderBusinessKeys == null) || selectedProductOrderBusinessKeys.isEmpty()) {
            errors.addGlobalError(new SimpleError("Please select at least one product order"));
        } else {
            Set<Product> products = new HashSet<Product> ();
            selectedProductOrders =
                productOrderDao.findListByBusinessKeyList(
                    selectedProductOrderBusinessKeys,
                    ProductOrderDao.FetchSpec.Product,
                    ProductOrderDao.FetchSpec.ResearchProject,
                    ProductOrderDao.FetchSpec.Samples);

            for (ProductOrder order : selectedProductOrders) {
                products.add(order.getProduct());
            }

            // Go through each products and report invalid duplicate price item names
            for (Product product : products) {
                String[] duplicatePriceItems = product.getDuplicatePriceItemNames();
                if (duplicatePriceItems != null) {
                    errors.addGlobalError(new SimpleError(
                        "The Product " + product.getPartNumber() + " has duplicate price items: " +
                        StringUtils.join(duplicatePriceItems, ", ")));
                }
            }

            // If there are locked out orders, then do not allow the session to start
            Set<BillingLedger> lockedOutOrders = billingLedgerDao.findLockedOutByOrderList(getSelectedProductOrderBusinessKeys());
            if (!lockedOutOrders.isEmpty()) {
                Set<String> lockedOutOrderStrings = new HashSet<String>(lockedOutOrders.size());
                for (BillingLedger ledger : lockedOutOrders) {
                    lockedOutOrderStrings.add(ledger.getProductOrderSample().getProductOrder().getTitle());
                }

                String lockedOutString = StringUtils.join(lockedOutOrderStrings.toArray(), ", ");

                errors.addGlobalError(new SimpleError(
                    "The following orders are locked out by active billing sessions: " + lockedOutString));
            }
        }

        // If there are errors, will reload the page, so need to fetch the list
        if (errors.size() > 0) {
            listInit();
        }
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = {LIST_ACTION})
    public void listInit() {
        allProductOrders = orderListEntryDao.findProductOrderListEntries();
    }

    @DefaultHandler
    @HandlesEvent(LIST_ACTION)
    public Resolution list() {
        return new ForwardResolution(ORDER_LIST_PAGE);
    }

    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(ORDER_VIEW_PAGE);
    }

    @HandlesEvent(CREATE_ACTION)
    public Resolution create() {
        setSubmitString(CREATE_ORDER);
        return new ForwardResolution(ORDER_CREATE_PAGE);
    }

    @HandlesEvent(EDIT_ACTION)
    public Resolution edit() {
        setSubmitString(EDIT_ORDER);
        return new ForwardResolution(ORDER_CREATE_PAGE);
    }

    @HandlesEvent("placeOrder")
    public Resolution placeOrder() {
        try {
            setUserInfo();
            editOrder.submitProductOrder();
            editOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);

            // save it!
            productOrderDao.persist(editOrder);
        } catch (Exception e ) {
            addGlobalValidationError(e.getMessage());
            return getContext().getSourcePageResolution();
        }

        addMessage("Product Order \"" + editOrder.getTitle() + "\" has been placed");
        return new RedirectResolution(ProductOrderActionBean.class, VIEW_ACTION).addParameter(PRODUCT_ORDER_PARAMETER, editOrder.getBusinessKey());
    }

    private void setUserInfo() {
        if (editOrder.getCreatedBy() == -1) {
            editOrder.setCreatedBy(getUserBean().getBspUser().getUserId());
        }
        editOrder.setModifiedBy(getUserBean().getBspUser().getUserId());
    }

    @HandlesEvent(SAVE_ACTION)
    public Resolution save() throws Exception {

        // Update the modified by (and created by if it was invalid when created
        setUserInfo();

        // set the project, product and addOns for the order
        ResearchProject project = projectDao.findByBusinessKey(researchProjectList);
        Product product = productDao.findByPartNumber(productList);
        List<Product> addOnProducts = productDao.findByPartNumbers(addOnKeys);
        editOrder.updateData(project, product, addOnProducts);

        if (editOrder.isDraft()) {
            editOrder.setOrderStatus(ProductOrder.OrderStatus.Draft);
        }

        // save it!
        productOrderDao.persist(editOrder);

        addMessage("Product Order \"" + editOrder.getTitle() + "\" has been saved.");
        return new RedirectResolution(ProductOrderActionBean.class, VIEW_ACTION).addParameter(PRODUCT_ORDER_PARAMETER, editOrder.getBusinessKey());
    }

    @HandlesEvent("downloadBillingTracker")
    public Resolution downloadBillingTracker() {
        return ProductOrderActionBean.getTrackerForOrders(this, selectedProductOrders, bspUserList);
    }

    @HandlesEvent("startBilling")
    public Resolution startBilling() {
        Set<BillingLedger> ledgerItems =
                billingLedgerDao.findWithoutBillingSessionByOrderList(getSelectedProductOrderBusinessKeys());
        if ((ledgerItems == null) || (ledgerItems.isEmpty())) {
            addGlobalValidationError("There is nothing to bill");
            return new RedirectResolution(ProductOrderActionBean.class, LIST_ACTION);
        }

        BillingSession session = new BillingSession(getUserBean().getBspUser().getUserId(), ledgerItems);
        billingSessionDao.persist(session);

        return new RedirectResolution(BillingSessionActionBean.class, VIEW_ACTION)
                .addParameter("sessionKey", session.getBusinessKey());
    }

    @HandlesEvent("getAddOns")
    public Resolution getAddOns() throws Exception {
        JSONArray itemList = new JSONArray();

        Product product = productDao.findByBusinessKey(this.product);
        for (Product addOn : product.getAddOns()) {
            JSONObject item = new JSONObject();
            item.put("key", addOn.getBusinessKey());
            item.put("value", addOn.getProductName());

            itemList.put(item);
        }

        return new StreamingResolution("text", new StringReader(itemList.toString()));
    }

    public List<String> getSelectedProductOrderBusinessKeys() {
        return selectedProductOrderBusinessKeys;
    }

    public void setSelectedProductOrderBusinessKeys(List<String> selectedProductOrderBusinessKeys) {
        this.selectedProductOrderBusinessKeys = selectedProductOrderBusinessKeys;
    }

    public List<ProductOrderListEntry> getAllProductOrders() {
        return allProductOrders;
    }

    public String getJiraUrl() {
        return jiraLink.browseUrl();
    }

    public ProductOrder getEditOrder() {
        return editOrder;
    }

    public void setEditOrder(ProductOrder order) {
        this.editOrder = order;
    }

    public String getQuoteUrl() {
        return quoteLink.quoteUrl(editOrder.getQuoteId());
    }

    public String getEditOrderSampleSearchUrl() {
        return bspLink.sampleSearchUrl();
    }

    public static Resolution getTrackerForOrders(
        final CoreActionBean actionBean,
        List<ProductOrder> productOrderList,
        BSPUserList bspUserList) {

        OutputStream outputStream = null;

        try {
            String filename =
                    "BillingTracker-" + AbstractSpreadsheetExporter.DATE_FORMAT.format(Calendar.getInstance().getTime());

            final File tempFile = File.createTempFile(filename, ".xls");
            outputStream = new FileOutputStream(tempFile);

            SampleLedgerExporter sampleLedgerExporter = new SampleLedgerExporter(productOrderList);
            sampleLedgerExporter.writeToStream(outputStream);
            IOUtils.closeQuietly(outputStream);

            return new Resolution() {
                @Override
                public void execute(HttpServletRequest request, HttpServletResponse response)
                        throws Exception {
                    InputStream inputStream = new FileInputStream(tempFile);

                    try {
                        actionBean.setFileDownloadHeaders("application/excel", tempFile.getName());
                        IOUtils.copy(inputStream, actionBean.getContext().getResponse().getOutputStream());
                    } finally {
                        IOUtils.closeQuietly(inputStream);
                        FileUtils.deleteQuietly(tempFile);
                    }
                }
            };
        } catch (Exception ex) {
            actionBean.addGlobalValidationError("Got an exception trying to download the billing tracker: " + ex.getMessage());
            return actionBean.getContext().getSourcePageResolution();
        } finally {
            IOUtils.closeQuietly(outputStream);
        }
    }

    public String getProductOrder() {
        return productOrder;
    }

    public void setProductOrder(String productOrder) {
        this.productOrder = productOrder;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getResearchProjectList() {
        return researchProjectList;
    }

    public void setResearchProjectList(String researchProjectList) {
        this.researchProjectList = researchProjectList;
    }

    public String getProductList() {
        return productList;
    }

    public void setProductList(String productList) {
        this.productList = productList;
    }

    public String getProjectCompleteData() throws Exception {
        if ((editOrder == null) || (editOrder.getResearchProject() == null)) {
            return "";
        }

        return ResearchProjectActionBean.getAutoCompleteJsonString(Collections.singletonList(editOrder.getResearchProject()));
    }

    public String getProductCompleteData() throws Exception {
        if ((editOrder == null) || (editOrder.getProduct() == null)) {
            return "";
        }

        return ProductActionBean.getAutoCompleteJsonString(Collections.singletonList(editOrder.getProduct()));
    }

    public List<String> getAddOnKeys() {
        return addOnKeys;
    }

    public void setAddOnKeys(List<String> addOnKeys) {
        this.addOnKeys = addOnKeys;
    }

    public String getSaveButtonText() {
        return ((editOrder == null) || editOrder.isDraft()) ? "Save Draft" : "Save";
    }
}
