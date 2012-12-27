package org.broadinstitute.gpinformatics.athena.presentation.orders;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.IOUtils;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderListModel;
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
import org.broadinstitute.gpinformatics.athena.presentation.converter.ProductOrderConverter;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import java.io.*;
import java.util.*;

@ManagedBean
@ViewScoped
public class ProductOrderListBean extends AbstractJsfBean {

    @Inject
    private FacesContext facesContext;

    @Inject
    private ProductOrderConverter orderConverter;

    @Inject
    private BillingLedgerDao billingLedgerDao;

    @Inject
    private BillingSessionDao billingSessionDao;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private ProductOrderListEntryDao productOrderListEntryDao;

    private ProductOrderListEntry[] selectedProductOrders;

    @Inject
    private UserBean userBean;

    private ProductOrderListModel productOrderListModel;


    public void initView() {
        if (productOrderListModel == null) {
            List<ProductOrderListEntry> productOrderListEntries = productOrderListEntryDao.findProductOrderListEntries();
            productOrderListModel = new ProductOrderListModel(productOrderListEntries);
        }
    }


    /**
     * Returns a list of all product orders.
     *
     * @return list of all product orders
     */
    public ProductOrderListModel getAllProductOrders() {
        return productOrderListModel;
    }



    public Set<Product> getSelectedProducts() {
        Set<Product> productSet = new HashSet<Product>();
        for (ProductOrderListEntry productOrder : getSelectedProductOrders()) {
            ProductOrder order = orderConverter.getAsObject(facesContext, null, productOrder.getBusinessKey());
            productSet.add(order.getProduct());
        }

        return productSet;
    }


    private List<String> getSelectedProductOrderBusinessKeys() {

        List<String> businessKeys = new ArrayList<String>();
        for (ProductOrderListEntry productOrderListEntry : getSelectedProductOrders()) {
            businessKeys.add(productOrderListEntry.getBusinessKey());
        }

        return businessKeys;
    }


    public static String getTrackerForOrders(
            List<String> pdoBusinessKeys,
            BSPUserList bspUserList,
            BillingLedgerDao ledgerDao,
            ProductOrderDao productOrderDao) {

        OutputStream outputStream = null;
        File tempFile = null;
        InputStream inputStream = null;

        try {
            String filename =
                    "BillingTracker-" + AbstractSpreadsheetExporter.DATE_FORMAT.format(Calendar.getInstance().getTime());

            tempFile = File.createTempFile(filename, "xls");
            outputStream = new FileOutputStream(tempFile);

            SampleLedgerExporter sampleLedgerExporter = new SampleLedgerExporter(pdoBusinessKeys, bspUserList, productOrderDao);
            sampleLedgerExporter.writeToStream(outputStream);
            IOUtils.closeQuietly(outputStream);
            inputStream = new FileInputStream(tempFile);

            // This copies the inputStream as a faces download with the file name specified.
            AbstractSpreadsheetExporter.copyForDownload(inputStream, filename + ".xls");
        } catch (Exception ex) {
            String message = "Got an exception trying to download the billing tracker: " + ex.getMessage();
            AbstractJsfBean.addMessage(null, FacesMessage.SEVERITY_ERROR, message, message);
        } finally {
            IOUtils.closeQuietly(outputStream);
            IOUtils.closeQuietly(inputStream);
            FileUtils.deleteQuietly(tempFile);
        }

        return null;
    }


    public ProductOrderListEntry[] getSelectedProductOrders() {
        return selectedProductOrders;
    }

    public void setSelectedProductOrders(ProductOrderListEntry[] selectedProductOrders) {
        this.selectedProductOrders = selectedProductOrders;
    }


    public String startBillingSession() {

        Set<BillingLedger> ledgerItems = validateOrderSelection("billing session");
        if (ledgerItems == null) {
            return null;
        }

        if (ledgerItems.isEmpty()) {
            addErrorMessage("There is nothing to bill");
            return null;
        }

        BillingSession session = new BillingSession(userBean.getBspUser().getUserId(), ledgerItems);
        billingSessionDao.persist(session);

        return redirect("/billing/view") + "&billingSession=" + session.getBusinessKey();
    }

    public String downloadBillingTracker() {

        // Do order validation
        Set<BillingLedger> previouslyUpdatedItems = validateOrderSelection("tracker download");
        if (previouslyUpdatedItems == null) {
            return null;
        }

        return getTrackerForOrders(getSelectedProductOrderBusinessKeys(), bspUserList, billingLedgerDao, productOrderDao);
    }    


    private Set<BillingLedger> validateOrderSelection(String validatingFor) {
        if (!userBean.isValidBspUser()) {
            addErrorMessage("A valid bsp user is needed to start a " + validatingFor);
            return null;
        }

        if ((getSelectedProductOrders() == null) || (getSelectedProductOrders().length == 0)) {
            addErrorMessage("Product orders must be selected for a " + validatingFor + " to be started");
            return null;
        }

        // Go through all products and report invalid duplicate price item names.
        Set<Product> products = getSelectedProducts();
        for (Product product : products) {

            String[] duplicatePriceItems = product.getDuplicatePriceItemNames();
            if (duplicatePriceItems != null) {
                addErrorMessage("The Product " + product.getPartNumber() +
                        " has duplicate price items: " + StringUtils.join(duplicatePriceItems, ", "));
                return null;
            }
        }

        // Do not allow the session to start if there are locked out orders.
        Set<BillingLedger> lockedOutOrders = billingLedgerDao.findLockedOutByOrderList(getSelectedProductOrderBusinessKeys());
        if (!lockedOutOrders.isEmpty()) {
            Set<String> lockedOutOrderStrings = new HashSet<String>(lockedOutOrders.size());
            for (BillingLedger ledger : lockedOutOrders) {
                lockedOutOrderStrings.add(ledger.getProductOrderSample().getProductOrder().getTitle());
            }

            String lockedOutString = StringUtils.join(lockedOutOrderStrings.toArray(), ", ");
            addErrorMessage("The following orders are locked out by active billing sessions: " + lockedOutString);
            return null;
        }

        return billingLedgerDao.findWithoutBillingSessionByOrderList(getSelectedProductOrderBusinessKeys());
    }
}
