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
// TODO: Move anything that is needed from SampleLedgerExporter into another class
import org.broadinstitute.gpinformatics.athena.boundary.orders.SampleLedgerExporter;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.cognos.SampleCoverageFirstMetFetcher;
import org.broadinstitute.gpinformatics.infrastructure.cognos.entity.SampleCoverageFirstMet;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.json.JSONArray;
import org.json.JSONException;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

@UrlBinding("/orders/ledger.action")
public class BillingLedgerActionBean extends CoreActionBean {

    private static final Log logger = LogFactory.getLog(BillingLedgerActionBean.class);

    private static final String BILLING_LEDGER_PAGE = "/orders/ledger.jsp";

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private PriceItemDao priceItemDao;

    @Inject
    private PriceListCache priceListCache;

    @Inject
    private SampleDataFetcher sampleDataFetcher;

    @Inject
    private SampleCoverageFirstMetFetcher sampleCoverageFirstMetFetcher;

    @Inject
    private ProductOrderEjb productOrderEjb;

    /**
     * The ID of the order being billed.
     */
    private String orderId;

    /**
     * The product order instance loaded from the orderId.
     */
    private ProductOrder productOrder;

    /**
     * The price items that are in effect for this product order. This includes the product's primary price item, any
     * configured replacement price items, add-on price items, and any historical price items that this order's samples
     * have been billed for (in the case where the price item for the product or an add-on has been changed while this
     * order was open and being billed).
     */
    private List<PriceItem> priceItems = new ArrayList<>();

    private List<ProductOrderSampleLedgerInfo> productOrderSampleLedgerInfos = new ArrayList<>();
    private Map<String, SampleCoverageFirstMet> coverageFirstMetBySample;

    /**
     * List of sample names for this product order. This is saved into the form and checked against current data when
     * the form is submitted to make sure that nothing changed between rendering and submitting the form.
     */
    private List<String> renderedSampleNames;

    /**
     * List of price item names for this product order. This is saved into the form and checked against current data
     * when the form is submitted to make sure that nothing changed between rendering and submitting the form, including
     * the order of the price items. With that guaranteed, we know that the originalQuantities array is reliable.
     * // TODO: review javadoc
     */
    private List<String> renderedPriceItemNames;

    /**
     * All quantities for all samples for all price items, including the quantities at the time that the page was
     * rendered. Used to verify that there were no significant changes to the data between the time that the page was
     * loaded and ultimately submitted.
     *
     * Stripes will nicely map into this nested data structure using Indexed Properties:
     * https://stripesframework.atlassian.net/wiki/display/STRIPES/Indexed+Properties
     */
    private List<LedgerData> ledgerData;

    /**
     * All quantities for all samples for all price items at the time that the page was rendered. The first dimension is
     * price items. The second dimension is product order samples.
     *
     * It seems that Stripes will do the heavy lifting for an array of doubles, but doesn't do anything useful with
     * nested data structures (maps of arrays, arrays of arrays, etc.). Therefore, there's some extra logic in this
     * action bean and the JSP to handle the price item dimension.
     */
    private double[][] originalQuantities;

    @Before(stages = LifecycleStage.EventHandling)
    public void before() {
        productOrder = productOrderDao.findByBusinessKey(orderId);
        ProductOrder.loadSampleData(productOrder.getSamples(), BSPSampleSearchColumn.BILLING_TRACKER_COLUMNS);

        // Gather metrics and related information
        coverageFirstMetBySample =
                sampleCoverageFirstMetFetcher.getCoverageFirstMetBySampleForPdo(productOrder.getBusinessKey());

        gatherSampleInfo();

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

    private void gatherSampleInfo() {
        productOrderSampleLedgerInfos.clear();
        for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
            SampleCoverageFirstMet sampleCoverageFirstMet =
                    coverageFirstMetBySample.get(productOrderSample.getSampleData().getCollaboratorsSampleName());
            if (sampleCoverageFirstMet == null) {
                sampleCoverageFirstMet = coverageFirstMetBySample.get(productOrderSample.getName());
            }
            ProductOrderSampleLedgerInfo info = new ProductOrderSampleLedgerInfo(productOrderSample,
                    sampleCoverageFirstMet != null ? sampleCoverageFirstMet.getDcfm() : null);
            productOrderSampleLedgerInfos.add(info);
        }
    }

    @Before(stages = LifecycleStage.EventHandling, on = "view")
    public void captureOriginalData() {
        renderedSampleNames = buildSampleNameList();
        renderedPriceItemNames = buildPriceItemNameList();
    }

    private ArrayList<String> buildSampleNameList() {
        ArrayList<String> sampleNames = new ArrayList<>();
        for (ProductOrderSample sample : productOrder.getSamples()) {
            sampleNames.add(sample.getName());
        }
        return sampleNames;
    }

    private ArrayList<String> buildPriceItemNameList() {
        ArrayList<String> priceItemNames = new ArrayList<>();
        for (PriceItem priceItem : priceItems) {
            priceItemNames.add(priceItem.getName());
        }
        return priceItemNames;
    }

    @DefaultHandler
    public Resolution view() {
        return new ForwardResolution(BILLING_LEDGER_PAGE);
    }

    @HandlesEvent("ledgerDetails")
    public Resolution ledgerDetails() throws JSONException {
        JSONArray details = new JSONArray();
        for (ProductOrderSampleLedgerInfo info : productOrderSampleLedgerInfos) {
            details.put(makeLedgerDetailJson(info));
        }
        return createTextResolution(details.toString());
    }

    @HandlesEvent("updateLedgers")
    public Resolution updateLedgers() {
        if (isSubmittedSampleListValid() && isSubmittedPriceItemListValid()) {
            Map<ProductOrderSample, Collection<ProductOrderSample.LedgerUpdate>> ledgerUpdates = buildLedgerUpdates();
            try {
                productOrderEjb.updateSampleLedgers(ledgerUpdates);
            } catch (ValidationException e) {
                logger.error(e);
                addGlobalValidationErrors(e.getValidationMessages());
            }
        }
        return new RedirectResolution("/orders/ledger.action?orderId=" + productOrder.getBusinessKey());
    }

    private boolean isSubmittedSampleListValid() {
        List<String> sampleNames = buildSampleNameList();
        if (!sampleNames.equals(renderedSampleNames)) {
            addGlobalValidationError(
                    "The sample list has changed since loading the page. Changes have not been saved. Please reevaluate your billing decisions and reapply your changes as appropriate.");
            return false;
        }
        return true;
    }

    private boolean isSubmittedPriceItemListValid() {
        List<String> priceItemNames = buildPriceItemNameList();
        if (!priceItemNames.equals(renderedPriceItemNames)) {
            addGlobalValidationError(
                    "The price items in effect for this order's product have changed since loading the page. Changes have not been saved. Please reevaluate your billing decisions and reapply your changes as appropriate.");
            return false;
        }
        return true;
    }

    private Map<ProductOrderSample, Collection<ProductOrderSample.LedgerUpdate>> buildLedgerUpdates() {
        Multimap<ProductOrderSample, ProductOrderSample.LedgerUpdate> ledgerUpdates = LinkedListMultimap.create();
        for (int i = 0; i < ledgerData.size(); i++) {
            LedgerData data = ledgerData.get(i);
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
                if (ledgerUpdate.isChangeIntended()) {
                    ledgerUpdates.put(productOrderSample, ledgerUpdate);
                }
            }
        }
        return ledgerUpdates.asMap();
    }

    private JSONArray makeLedgerDetailJson(ProductOrderSampleLedgerInfo info) throws JSONException {
        JSONArray result = new JSONArray();

        for (LedgerEntry ledgerEntry : info.getSample().getLedgerItems()) {
            result.put(makeLedgerEntryJson(ledgerEntry));
        }
        return result;
    }

    private JSONArray makeLedgerEntryJson(LedgerEntry ledgerEntry) throws JSONException {
        JSONArray result = new JSONArray();

        result.put(ledgerEntry.getPriceItem().getName());
        result.put(ledgerEntry.getQuantity());
        result.put(ledgerEntry.getQuoteId());
//        result.put(DateFormat.getDateInstance(DateFormat.MEDIUM).format(ledgerEntry.getWorkCompleteDate()));
        result.put(new SimpleDateFormat("MMM d, yyyy").format(ledgerEntry.getWorkCompleteDate()));
        BillingSession billingSession = ledgerEntry.getBillingSession();
        result.put(billingSession != null ? billingSession.getBillingSessionId() : null);
        result.put(billingSession != null ?
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

    public List<PriceItem> getPriceItems() {
        return priceItems;
    }

    public List<ProductOrderSampleLedgerInfo> getProductOrderSampleLedgerInfos() {
        return productOrderSampleLedgerInfos;
    }

    public List<LedgerData> getLedgerData() {
        return ledgerData;
    }

    public void setLedgerData(List<LedgerData> ledgerData) {
        this.ledgerData = ledgerData;
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

    /**
     * Data used to render the billing ledger UI.
     */
    public static class ProductOrderSampleLedgerInfo {
        private ProductOrderSample productOrderSample;
        private Date coverageFirstMet;
        private Map<PriceItem, ProductOrderSample.LedgerQuantities> ledgerQuantities;
        private ListMultimap<PriceItem, LedgerEntry> ledgerEntriesByPriceItem = ArrayListMultimap.create();
        private int autoFillQuantity = 0;

        public ProductOrderSampleLedgerInfo(ProductOrderSample productOrderSample, Date coverageFirstMet) {
            this.productOrderSample = productOrderSample;
            this.coverageFirstMet = coverageFirstMet;

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
            if (coverageFirstMet != null && !primaryBilled) {
                autoFillQuantity = 1;
            }
        }

        /**
         * Get the date complete to use for the form. To maintain familiarity with the billing tracker spreadsheet, this
         * will use the work complete date from unbilled ledger entries. If there are none, then DCFM (Date Coverage
         * First Met) will be pre-populated if
         *
         * @return
         */
        public String getDateComplete() {
            Date workCompleteDate = productOrderSample.getWorkCompleteDate();
            if (workCompleteDate == null) {
                workCompleteDate = coverageFirstMet;
            }
            return workCompleteDate != null ? new SimpleDateFormat("MMM d, yyyy").format(workCompleteDate) : null;
        }

        public ProductOrderSample getSample() {
            return productOrderSample;
        }

        public Date getCoverageFirstMet() {
            return coverageFirstMet;
        }

        public double getBilledForPriceItem(PriceItem priceItem) {
            ProductOrderSample.LedgerQuantities quantities = ledgerQuantities.get(priceItem);
            return quantities != null ? quantities.getBilled() : 0;
        }

        public double getTotalForPriceItem(PriceItem priceItem) {
            ProductOrderSample.LedgerQuantities quantities = ledgerQuantities.get(priceItem);
            return quantities != null ? quantities.getTotal() : 0;
        }

        public int getAutoFillQuantity() {
            return autoFillQuantity;
        }
    }

    /**
     *
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

        public void setWorkCompleteDate(Date workCompleteDate) {
            this.workCompleteDate = workCompleteDate;
        }

        public Map<Long, ProductOrderSampleQuantities> getQuantities() {
            return quantities;
        }

        public void setQuantities(Map<Long, ProductOrderSampleQuantities> quantities) {
            this.quantities = quantities;
        }
    }

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
    }
}
