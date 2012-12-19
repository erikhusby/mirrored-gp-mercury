package org.broadinstitute.gpinformatics.athena.presentation.orders;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import org.apache.commons.io.FileUtils;
import org.apache.poi.util.IOUtils;
import org.broadinstitute.gpinformatics.athena.boundary.orders.SampleLedgerExporter;
import org.broadinstitute.gpinformatics.athena.boundary.util.AbstractSpreadsheetExporter;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderListEntryDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderListEntry;
import org.broadinstitute.gpinformatics.athena.presentation.links.BspLink;
import org.broadinstitute.gpinformatics.athena.presentation.links.JiraLink;
import org.broadinstitute.gpinformatics.athena.presentation.links.QuoteLink;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Calendar;
import java.util.List;

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

    private List<ProductOrderListEntry> allProductOrders;

    @Validate(required = true, on = {"view", "edit"})
    private String orderKey;

    ProductOrder editOrder;

    private List<String> selectedProductOrderBusinessKeys;

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

    @HandlesEvent(value = "save")
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
        List<String> productOrderBusinessKeys = getSelectedProductOrderBusinessKeys();
        Resolution downloadResolution =
            ProductOrderActionBean.getTrackerForOrders(
                this, productOrderDao, productOrderBusinessKeys, bspUserList);

        // If there is no file to download, just pass on the errors
        if (downloadResolution == null) {
            return new ForwardResolution(ORDER_VIEW_PAGE);
        }

        // Do the download
        return downloadResolution;
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
        ProductOrderDao productOrderDao,
        List<String> pdoBusinessKeys,
        BSPUserList bspUserList) {

        OutputStream outputStream = null;

        try {
            String filename =
                    "BillingTracker-" + AbstractSpreadsheetExporter.DATE_FORMAT.format(Calendar.getInstance().getTime());

            final File tempFile = File.createTempFile(filename, "xls");
            outputStream = new FileOutputStream(tempFile);

            SampleLedgerExporter sampleLedgerExporter = new SampleLedgerExporter(pdoBusinessKeys, bspUserList, productOrderDao);
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
}
