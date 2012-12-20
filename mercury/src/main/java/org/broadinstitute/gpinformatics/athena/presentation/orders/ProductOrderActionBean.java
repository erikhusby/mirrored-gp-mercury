package org.broadinstitute.gpinformatics.athena.presentation.orders;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.IOUtils;
import org.broadinstitute.gpinformatics.athena.boundary.orders.SampleLedgerExporter;
import org.broadinstitute.gpinformatics.athena.boundary.util.AbstractSpreadsheetExporter;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingLedgerDao;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderListEntryDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedger;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderListEntry;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.presentation.billing.BillingSessionActionBean;
import org.broadinstitute.gpinformatics.athena.presentation.links.BspLink;
import org.broadinstitute.gpinformatics.athena.presentation.links.JiraLink;
import org.broadinstitute.gpinformatics.athena.presentation.links.QuoteLink;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao.FetchSpec.*;

/**
 * This handles all the needed interface processing elements
 */
@UrlBinding("/orders/order.action")
public class ProductOrderActionBean extends CoreActionBean {

    private static final String CREATE_ORDER = CoreActionBean.CREATE + "New Product Order";
    private static final String EDIT_ORDER = CoreActionBean.EDIT + "Product Order: ";

    private static final String ORDER_CREATE_PAGE = "/orders/create.jsp";
    private static final String ORDER_LIST_PAGE = "/orders/list.jsp";
    private static final String ORDER_VIEW_PAGE = "/orders/view.jsp";

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
    private BillingSessionDao billingSessionDao;

    @Inject
    private BillingLedgerDao billingLedgerDao;

    @Inject
    private UserBean userBean;

    private List<ProductOrderListEntry> allProductOrders;

    @Validate(required = true, on={"view", "edit", "save"})
    private String orderKey;

    @ValidateNestedProperties({
            @Validate(field="productOrderId", required=true, on="edit"),
            @Validate(field="comments", maxlength=2000, on={"edit", "view"})
    })
    private ProductOrder editOrder;

    private List<String> selectedProductOrderBusinessKeys;
    private List<ProductOrder> selectedProductOrders;

    /**
     * Initialize the product with the passed in key for display in the form
     */
    @Before(stages = LifecycleStage.BindingAndValidation, on = {"view", "edit", "downloadBillingTracker", "save", "addOnsAutocomplete"})
    public void init() {
        orderKey = getContext().getRequest().getParameter("orderKey");
        if (orderKey != null) {
            editOrder = productOrderDao.findByBusinessKey(orderKey);
        }
    }

    @ValidationMethod(on = {"startBilling", "downloadBillingTracker"})
    public void validateOrderSelection(ValidationErrors errors) {
        if (!userBean.isValidBspUser()) {
            errors.addGlobalError(new SimpleError("This requires that you be a valid bsp user "));
            return;
        }

        if ((selectedProductOrderBusinessKeys == null) || selectedProductOrderBusinessKeys.isEmpty()) {
            errors.addGlobalError(new SimpleError("Please select at least one product order"));
            return;
        }

        Set<Product> products = new HashSet<Product> ();
        selectedProductOrders =
            productOrderDao.findListByBusinessKeyList(selectedProductOrderBusinessKeys, Product, ResearchProject, Samples);
        for (ProductOrder order : selectedProductOrders) {
            products.add(order.getProduct());
        }

        // Go through each products and report invalid duplicate price item names
        for (Product product : products) {
            String[] duplicatePriceItems = product.getDuplicatePriceItemNames();
            if (duplicatePriceItems != null) {
                errors.addGlobalError(new SimpleError(
                    "The Product " + product.getPartNumber() + " has duplicate price items: " + StringUtils.join(duplicatePriceItems, ", ")));
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

    @After(stages = LifecycleStage.BindingAndValidation, on = {"list"})
    public void listInit() {
        allProductOrders = orderListEntryDao.findProductOrderListEntries();
    }

    @DefaultHandler
    @HandlesEvent("list")
    public Resolution list() {
        return new ForwardResolution(ORDER_LIST_PAGE);
    }

    @HandlesEvent("view")
    public Resolution view() {
        return new ForwardResolution(ORDER_VIEW_PAGE);
    }

    @HandlesEvent("create")
    public Resolution create() {
        setSubmitString(CREATE_ORDER);
        return new ForwardResolution(ORDER_CREATE_PAGE);
    }

    @HandlesEvent("edit")
    public Resolution edit() {
        setSubmitString(EDIT_ORDER);
        return new ForwardResolution(ORDER_CREATE_PAGE);
    }

    @HandlesEvent("save")
    public Resolution save() {
        try {
            productOrderDao.persist(editOrder);
        } catch (Exception e ) {
            addGlobalValidationError(e.getMessage());
            return null;
        }

        addMessage("Product Order \"" + editOrder.getTitle() + "\" has been created");
        return new RedirectResolution(ORDER_VIEW_PAGE).addParameter("order", editOrder.getBusinessKey());
    }

    @HandlesEvent("downloadBillingTracker")
    public Resolution downloadBillingTracker() {
        Resolution downloadResolution =
            ProductOrderActionBean.getTrackerForOrders(this, selectedProductOrders, bspUserList);

        // If there is no file to download, just pass on the errors
        if (downloadResolution == null) {
            return new ForwardResolution(ORDER_VIEW_PAGE);
        }

        // Do the download
        return downloadResolution;
    }

    @HandlesEvent("startBilling")
    public Resolution startBilling() {
        Set<BillingLedger> ledgerItems =
                billingLedgerDao.findWithoutBillingSessionByOrderList(getSelectedProductOrderBusinessKeys());
        if ((ledgerItems == null) || (ledgerItems.isEmpty())) {
            addGlobalValidationError("There is nothing to bill");
            return list();
        }

        BillingSession session = new BillingSession(userBean.getBspUser().getUserId(), ledgerItems);
        billingSessionDao.persist(session);

        return new RedirectResolution(BillingSessionActionBean.class)
                .addParameter("view", "")
                .addParameter("billingSession=", session.getBusinessKey());
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

    public String getProductKey() {
        return orderKey;
    }

    public void setProductKey(String orderKey) {
        this.orderKey = orderKey;
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

            final File tempFile = File.createTempFile(filename, "xls");
            outputStream = new FileOutputStream(tempFile);

            SampleLedgerExporter sampleLedgerExporter = new SampleLedgerExporter(productOrderList, bspUserList);
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
        } finally {
            IOUtils.closeQuietly(outputStream);
        }

        return null;
    }

    public String getOrderKey() {
        return orderKey;
    }

    public void setOrderKey(String orderKey) {
        this.orderKey = orderKey;
    }
}
