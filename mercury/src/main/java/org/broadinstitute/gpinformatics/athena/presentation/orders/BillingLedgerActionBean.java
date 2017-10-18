package org.broadinstitute.gpinformatics.athena.presentation.orders;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.boundary.orders.SampleLedgerExporter;
import org.broadinstitute.gpinformatics.athena.boundary.products.InvalidProductException;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderListEntryDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderListEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.cognos.SampleCoverageFirstMetFetcher;
import org.broadinstitute.gpinformatics.infrastructure.cognos.entity.SampleCoverageFirstMet;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPInterfaceException;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.json.JSONArray;
import org.json.JSONException;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

// TODO: Move anything that is needed from SampleLedgerExporter into another class

@UrlBinding("/orders/ledger.action")
public class BillingLedgerActionBean extends CoreActionBean {

    /**
     * Date format used for displaying DCFM and the value of Date Complete inputs.
     */
    public static final String DATE_FORMAT = "MMM d, yyyy";

    private static final Log logger = LogFactory.getLog(BillingLedgerActionBean.class);

    private static final String BILLING_LEDGER_PAGE = "/orders/ledger.jsp";

    @Inject
    private ProductOrderEjb productOrderEjb;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ProductOrderListEntryDao productOrderListEntryDao;

    @Inject
    private PriceItemDao priceItemDao;

    @Inject
    private PriceListCache priceListCache;

    @Inject
    private SampleCoverageFirstMetFetcher sampleCoverageFirstMetFetcher;

    /**
     * The ID of the order being billed.
     */
    private String orderId;

    /**
     * The product order instance loaded from the orderId.
     */
    private ProductOrder productOrder;

    /**
     * Provides information about the billing state of the product order.
     */
    private ProductOrderListEntry productOrderListEntry;

    /**
     * The price items that are in effect for this product order.
     *
     * @see BillingLedgerActionBean#gatherPriceItems
     */
    private List<PriceItem> priceItems = new ArrayList<>();

    /**
     * List of sample names for this product order. This is saved into the form and checked against current data when
     * the form is submitted to make sure that nothing changed between rendering and submitting the form. Used for both
     * rendering the JSP and capturing the values submitted from hidden elements in the form.
     */
    private List<String> renderedSampleNames;

    /**
     * List of price item names for this product order. This is saved into the form and checked against current data
     * when the form is submitted to make sure that nothing changed between rendering and submitting the form. Used for
     * both rendering the JSP and capturing the values submitted from hidden elements in the form.
     */
    private List<String> renderedPriceItemNames;

    /**
     * Information used to render the ledger table and form inputs. Effectively the "output" of the action bean, or
     * "input" to the JSP, whichever way you want to look at it.
     *
     * This is ONLY used for rendering the page, not for capturing the data submitted by the form.
     */
    private List<ProductOrderSampleLedgerInfo> productOrderSampleLedgerInfos = new ArrayList<>();

    /**
     * All quantities for all samples for all price items, including the quantities at the time that the page was
     * rendered. Used to verify that there were no significant changes to the data between the time that the page was
     * loaded and ultimately submitted.
     *
     * This is ONLY used for capturing the data submitted by the form, not for rendering the page.
     *
     * Stripes will nicely map into this nested data structure using Indexed Properties:
     * https://stripesframework.atlassian.net/wiki/display/STRIPES/Indexed+Properties
     */
    private List<LedgerData> ledgerData;

    /**
     * Load the product order, price items, sample data, and various other information based on the orderId parameter.
     * All of this data is needed for rendering the billing ledger UI.
     */
    @Before(stages = LifecycleStage.EventHandling, on = { "view", "updateLedgers" })
    public void loadAllDataForView() {
        loadProductOrder();
        gatherPriceItems();

        /*
         * When processing a form submit ("updateLedgers" action), this will only be used if rendering an error.
         * However, it is important to gather this data now, especially ProductOrderSample.getLedgerQuantities(),
         * because the results can be side-effected by changes made to managed entities but not committed due to a
         * subsequent error from the EJB method.
         */
        loadViewData();
    }

    /**
     * Load the product order based on the orderId request parameter. This is all of the data that is needed for loading
     * the ledger details (from an AJAX request).
     */
    @Before(stages = LifecycleStage.EventHandling, on = { "ledgerDetails" })
    public void loadProductOrder() { productOrder = productOrderDao.findByBusinessKey(orderId);
    }

    /**
     * Render the billing ledger UI.
     */
    @DefaultHandler
    public Resolution view() {
        return new ForwardResolution(BILLING_LEDGER_PAGE);
    }

    /**
     * Load the ledger details and return them as JSON data to be loaded by the page via AJAX.
     *
     * @return a resolution containing the ledger details serialized as JSON
     * @throws JSONException
     */
    @HandlesEvent("ledgerDetails")
    public Resolution ledgerDetails() throws JSONException {
        JSONArray details = new JSONArray();
        for (ProductOrderSample sample : productOrder.getSamples()) {
            details.put(makeLedgerDetailJson(sample));
        }
        return createTextResolution(details.toString());
    }

    /**
     * Apply billing ledger updates.
     */
    @HandlesEvent("updateLedgers")
    public Resolution updateLedgers() {
        if (isSubmittedSampleListValid() && isSubmittedPriceItemListValid()) {
            Map<ProductOrderSample, Collection<ProductOrderSample.LedgerUpdate>> ledgerUpdates = buildLedgerUpdates();
            try {
                productOrderEjb.updateSampleLedgers(ledgerUpdates);
            } catch (ValidationException e) {
                logger.error(e);
                addGlobalValidationErrors(e.getValidationMessages());
            } catch ( QuoteNotFoundException | QuoteServerException  otherEs) {
                logger.error(otherEs);
                addGlobalValidationError(otherEs.getMessage());
            } catch (SAPInterfaceException sie) {
                String errorMessage = "While updating this order in SAP an error occurred:  ";
                logger.error(sie);
                addGlobalValidationError(errorMessage + sie.getMessage());
            } catch (InvalidProductException ipe) {
                String errorMessage = "Either the product or one of the selected add-ons for this order is associated "
                                      + "with an invalid Price item.  Please set the correct price item before "
                                      + "continuing for this will cause issues when billing: ";
                logger.error(ipe);
                addGlobalValidationError(errorMessage+ipe.getMessage());
            }
        }

        if (hasErrors()) {
            return new ForwardResolution(BILLING_LEDGER_PAGE);
        } else {
            return new RedirectResolution("/orders/ledger.action?orderId=" + productOrder.getBusinessKey());
        }
    }

    /**
     * Gather all price items associated with a product order. This includes the product's primary price item, any
     * configured replacement price items, add-on price items, and any "historical" price items (ones that have been
     * billed for this PDO but are otherwise no longer related).
     */
    private void gatherPriceItems() {
        // Collect primary and replacement price items
        priceItems.addAll(SampleLedgerExporter.getPriceItems(productOrder.getProduct(), priceItemDao, priceListCache));

        // Collect historical price items (need add-ons to do that)
        List<Product> addOns = new ArrayList<>();
        for (ProductOrderAddOn productOrderAddOn : productOrder.getAddOns()) {
            addOns.add(productOrderAddOn.getAddOn());
        }
        Collections.sort(addOns);
        priceItems.addAll(SampleLedgerExporter
                .getHistoricalPriceItems(Collections.singletonList(productOrder), priceItems, addOns, priceItemDao,
                        priceListCache));

        // Collect add-on price items
        for (Product addOn : addOns) {
            priceItems.add(addOn.getPrimaryPriceItem());
        }
    }

    private void loadViewData() {
        // Load sample data
        ProductOrder.loadSampleData(productOrder.getSamples(), BSPSampleSearchColumn.BILLING_TRACKER_COLUMNS);

        // Load billing status information
        productOrderListEntry = productOrder.isDraft() ? ProductOrderListEntry.createDummy() :
                productOrderListEntryDao.findSingle(productOrder.getJiraTicketKey());

        // Gather metrics and related information
        productOrderSampleLedgerInfos = gatherSampleInfo();

        // Capture data to render into the form to verify that nothing changed when the form is submitted
        renderedSampleNames = buildSampleNameList();
        renderedPriceItemNames = buildPriceItemNameList();
    }

    /**
     * Gather information about samples needed to render the billing ledger UI. This includes Date Coverage First Met
     * (DCFM) and ledger quantities for all applicable price items.
     *
     * @return a list of sample information in the same order as the product order samples
     */
    private List<ProductOrderSampleLedgerInfo> gatherSampleInfo() {
        List<ProductOrderSampleLedgerInfo> infos = new ArrayList<>();

        Map<String, SampleCoverageFirstMet> coverageFirstMetBySample =
                sampleCoverageFirstMetFetcher.getCoverageFirstMetBySampleForPdo(productOrder.getBusinessKey());

        for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
            SampleCoverageFirstMet sampleCoverageFirstMet =
                    coverageFirstMetBySample.get(productOrderSample.getSampleData().getCollaboratorsSampleName());
            if (sampleCoverageFirstMet == null) {
                sampleCoverageFirstMet = coverageFirstMetBySample.get(productOrderSample.getName());
            }
            ProductOrderSampleLedgerInfo info = new ProductOrderSampleLedgerInfo(productOrderSample,
                    sampleCoverageFirstMet != null ? sampleCoverageFirstMet.getDcfm() : null);
            infos.add(info);
        }

        return infos;
    }

    /**
     * Build a list of all of the sample names on this product order. This is done both when rendering the form and when
     * the form is submitted. The results are compared and an error is thrown if they are different, indicating that a
     * change has been made that may make it unsafe to apply the requested ledger updates.
     *
     * @return a list of sample names on the product order
     */
    private ArrayList<String> buildSampleNameList() {
        ArrayList<String> sampleNames = new ArrayList<>();
        for (ProductOrderSample sample : productOrder.getSamples()) {
            sampleNames.add(sample.getName());
        }
        return sampleNames;
    }

    /**
     * Build a list of all of the price item names for this product order.  This is done both when rendering the form
     * and when the form is submitted. The results are compared and an error is thrown if they are different, indicating
     * that a change has been made that may make it unsafe to apply the requested ledger updates.
     *
     * @return a list of the price item names for the product order
     */
    private ArrayList<String> buildPriceItemNameList() {
        ArrayList<String> priceItemNames = new ArrayList<>();
        for (PriceItem priceItem : priceItems) {
            priceItemNames.add(priceItem.getName());
        }
        return priceItemNames;
    }

    /**
     * Validation method that compares the current list of sample names with the list of sample names as it was when the
     * billing ledger UI was rendered. Adds a validation error and returns false if there is an inconsistency.
     *
     * @return true if the sample names have not changed; false otherwise
     */
    private boolean isSubmittedSampleListValid() {
        List<String> sampleNames = buildSampleNameList();
        if (!sampleNames.equals(renderedSampleNames)) {
            addGlobalValidationError(
                    "The sample list has changed since loading the page. Changes have not been saved. Please reevaluate your billing decisions and reapply your changes as appropriate.");
            return false;
        }
        return true;
    }

    /**
     * Validation method that compares the current list of price item names with the list of price item names as it was
     * when the billing ledger UI was rendered. Adds a validation error and returns false if there is an inconsistency.
     *
     * @return true if the price item names have not changed; false otherwise
     */
    private boolean isSubmittedPriceItemListValid() {
        List<String> priceItemNames = buildPriceItemNameList();
        if (!priceItemNames.equals(renderedPriceItemNames)) {
            addGlobalValidationError(
                    "The price items in effect for this order's product have changed since loading the page. Changes have not been saved. Please reevaluate your billing decisions and reapply your changes as appropriate.");
            return false;
        }
        return true;
    }

    /**
     * Gathers all of the submitted data into a collection of LedgerUpdate instances representing the requested changes
     * to apply. Also gathers the current quantities to be compared with the originals to detect changes made by another
     * user or process.
     *
     * @return a map of LedgerUpdate instances by ProductOrderSample to be applied
     */
    private Map<ProductOrderSample, Collection<ProductOrderSample.LedgerUpdate>> buildLedgerUpdates() {
        Multimap<ProductOrderSample, ProductOrderSample.LedgerUpdate> ledgerUpdates = LinkedListMultimap.create();
        for (int i = 0; i < ledgerData.size(); i++) {
            LedgerData data = ledgerData.get(i);
            if (data != null) {
                ProductOrderSample productOrderSample = productOrder.getSamples().get(i);
                Map<PriceItem, ProductOrderSample.LedgerQuantities> ledgerQuantitiesMap =
                        productOrderSample.getLedgerQuantities();
                for (Map.Entry<Long, ProductOrderSampleQuantities> entry : data.getQuantities().entrySet()) {
                    PriceItem priceItem = priceItemDao.findById(PriceItem.class, entry.getKey());
                    ProductOrderSampleQuantities quantities = entry.getValue();
                    ProductOrderSample.LedgerQuantities ledgerQuantities = ledgerQuantitiesMap.get(priceItem);
                    double currentQuantity = ledgerQuantities != null ? ledgerQuantities.getTotal() : 0;
                    ProductOrderSample.LedgerUpdate ledgerUpdate =
                            new ProductOrderSample.LedgerUpdate(productOrderSample.getSampleKey(), priceItem,
                                    quantities.originalQuantity, currentQuantity, quantities.submittedQuantity,
                                    data.getWorkCompleteDate());
                    ledgerUpdates.put(productOrderSample, ledgerUpdate);
                }
            }
        }
        return ledgerUpdates.asMap();
    }

    /**
     * Builds a JSON array of ledger entry data for a product order sample.
     *
     * @param sample    the sample to return ledger data for
     * @return and array of all of the ledger entry data as a JSONArray
     */
    private JSONArray makeLedgerDetailJson(ProductOrderSample sample) {
        JSONArray result = new JSONArray();

        for (LedgerEntry ledgerEntry : sample.getLedgerItems()) {
            result.put(makeLedgerEntryJson(ledgerEntry));
        }
        return result;
    }

    /**
     * Builds a JSON array of information about a single ledger entry.
     *
     * @param ledgerEntry    the ledger entry to serialize
     * @return the ledger entry data as a JSONArray
     */
    private JSONArray makeLedgerEntryJson(LedgerEntry ledgerEntry) {
        JSONArray result = new JSONArray();

        result.put(ledgerEntry.getPriceItem().getName());
        result.put(Double.valueOf(ledgerEntry.getQuantity()));
        result.put(ledgerEntry.getQuoteId());
        result.put(new SimpleDateFormat("MMM d, yyyy").format(ledgerEntry.getWorkCompleteDate()));
        BillingSession billingSession = ledgerEntry.getBillingSession();
        result.put(billingSession != null ? billingSession.getBillingSessionId() : null);
        result.put(billingSession != null && billingSession.getBilledDate() != null ?
                new SimpleDateFormat("MMM d, yyyy HH:mm:ss a").format(billingSession.getBilledDate()) : null);
        result.put(ledgerEntry.getBillingMessage());

        return result;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public ProductOrder getProductOrder() {
        return productOrder;
    }

    public ProductOrderListEntry getProductOrderListEntry() {
        return productOrderListEntry;
    }

    public List<PriceItem> getPriceItems() {
        return priceItems;
    }

    public List<String> getRenderedSampleNames() {
        return renderedSampleNames;
    }

    public void setRenderedSampleNames(List<String> renderedSampleNames) {
        this.renderedSampleNames = renderedSampleNames;
    }

    public List<String> getRenderedPriceItemNames() {
        return renderedPriceItemNames;
    }

    public void setRenderedPriceItemNames(List<String> renderedPriceItemNames) {
        this.renderedPriceItemNames = renderedPriceItemNames;
    }

    public List<ProductOrderSampleLedgerInfo> getProductOrderSampleLedgerInfos() {
        return productOrderSampleLedgerInfos;
    }

    public List<LedgerData> getLedgerData() {
        return ledgerData;
    }

    public List<LedgerData> getLd(){
        return getLedgerData();
    }

    public void setLd(List<LedgerData> ledgerData){
        setLedgerData(ledgerData);
    }

    public void setLedgerData(List<LedgerData> ledgerData) {
        this.ledgerData = ledgerData;
    }

    /**
     * Return the date 3 months prior to now as a number representing number of milliseconds since January 1, 1970,
     * 00:00:00 GMT. The intent is to pass this to <code>new Date()</code> in JavaScript to perform date range checks.
     *
     * @return the date 3 months ago as milliseconds since January 1, 1970, 00:00:00 GMT
     */
    public long getThreeMonthsAgo() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -3);
        return calendar.getTime().getTime();
    }

    /**
     * Data used to render the billing ledger UI.
     */
    public static class ProductOrderSampleLedgerInfo {
        private ProductOrderSample productOrderSample;
        private Date coverageFirstMet;
        private Date workCompleteDate;
        private Map<PriceItem, ProductOrderSample.LedgerQuantities> ledgerQuantities;
        private ListMultimap<PriceItem, LedgerEntry> ledgerEntriesByPriceItem = ArrayListMultimap.create();
        private int autoFillQuantity = 0;

        public ProductOrderSampleLedgerInfo(ProductOrderSample productOrderSample, Date coverageFirstMet) {
            this.productOrderSample = productOrderSample;
            this.coverageFirstMet = coverageFirstMet;

            // Capture this value now so that it is not affected by later changes in the entity graph.
            workCompleteDate = productOrderSample.getWorkCompleteDate();
            if (workCompleteDate == null) {
                workCompleteDate = coverageFirstMet;
            }

            ledgerQuantities = productOrderSample.getLedgerQuantities();

            boolean primaryBilled = false;
            for (LedgerEntry ledgerEntry : productOrderSample.getLedgerItems()) {
                PriceItem priceItem = ledgerEntry.getPriceItem();
                ledgerEntriesByPriceItem.get(priceItem).add(ledgerEntry);
                LedgerEntry.PriceItemType priceItemType = ledgerEntry.getPriceItemType();
                if (priceItemType == LedgerEntry.PriceItemType.PRIMARY_PRICE_ITEM
                    || priceItemType == LedgerEntry.PriceItemType.REPLACEMENT_PRICE_ITEM) {
                    primaryBilled = true;
                }
            }

            // If coverage has been met and the sample hasn't yet been billed, indicate an auto-fill quantity of 1
            if (coverageFirstMet != null && !primaryBilled) {
                autoFillQuantity = 1;
            }
        }

        public ProductOrderSample getSample() {
            return productOrderSample;
        }

        public Date getCoverageFirstMet() {
            return coverageFirstMet;
        }

        /**
         * Get the date complete to use for the form. To maintain familiarity with the billing tracker spreadsheet, this
         * will use the work complete date from unbilled ledger entries. If there are none and coverage has been met,
         * DCFM (Date Coverage First Met) will be pre-populated.
         *
         * @return the date complete to populate in the UI
         */
        public String getDateCompleteFormatted() {
            return workCompleteDate != null ? new SimpleDateFormat("MMM d, yyyy").format(workCompleteDate) : null;
        }

        public double getTotalForPriceItem(PriceItem priceItem) {
            ProductOrderSample.LedgerQuantities quantities = ledgerQuantities.get(priceItem);
            return quantities != null ? quantities.getTotal() : 0;
        }

        public double getBilledForPriceItem(PriceItem priceItem) {
            ProductOrderSample.LedgerQuantities quantities = ledgerQuantities.get(priceItem);
            return quantities != null ? quantities.getBilled() : 0;
        }

        public int getAutoFillQuantity() {
            return autoFillQuantity;
        }
    }

    /**
     * Data posted from the billing ledger UI.
     */
    public static class LedgerData {
        private String sampleName;
        private Date workCompleteDate;
        private Map<Long, ProductOrderSampleQuantities> quantities;

        public String getSampleName() {
            return sampleName;
        }

        public void setSampleName(String sampleName) {
            this.sampleName = sampleName;
        }

        public Date getWorkCompleteDate() {
            return workCompleteDate;
        }
        public Date getWcd() {
            return getWorkCompleteDate();
        }
        public void setWcd(Date d){
            setWorkCompleteDate(d);
        }
        public String getCompleteDateFormatted() {
            return workCompleteDate != null ? new SimpleDateFormat(DATE_FORMAT).format(workCompleteDate) : null;
        }

        public String getCdf(){
            return getCompleteDateFormatted();
        }

        public String getSn(){
            return getSampleName();
        }


        public void setSn(String sn) {
            setSampleName(sn);
        }

        public void setWorkCompleteDate(Date workCompleteDate) {
            this.workCompleteDate = workCompleteDate;
        }

        public Map<Long, ProductOrderSampleQuantities> getQuantities() {
            return quantities;
        }

        public void setQuantities(Map<Long, ProductOrderSampleQuantities> quantities) {
            this.quantities = quantities;
        }

        public Map<Long, ProductOrderSampleQuantities> getQ() {
            return getQuantities();
        }

        public void setQ(
            Map<Long, ProductOrderSampleQuantities> q) {
            setQuantities(q);
        }
    }

    /**
     * Quantity data for a single price item posted from the billing ledger UI.
     */
    public static class ProductOrderSampleQuantities {
        private double originalQuantity;
        private double submittedQuantity;

        public double getOriginalQuantity() {
            return originalQuantity;
        }

        public void setOriginalQuantity(double originalQuantity) {
            this.originalQuantity = originalQuantity;
        }

        public double getSubmittedQuantity() {
            return submittedQuantity;
        }

        public void setSubmittedQuantity(double submittedQuantity) {
            this.submittedQuantity = submittedQuantity;
        }

        public double getOc() {
            return originalQuantity;
        }

        public double getSq() {
            return submittedQuantity;
        }

        public void setOc(double oc) {
            setOriginalQuantity(oc);
        }

        public void setSq(double sq) {
            setSubmittedQuantity(sq);
        }
    }
}
