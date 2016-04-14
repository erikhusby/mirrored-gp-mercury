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
// TODO: Move anything that is needed from SampleLedgerExporter into another class
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.boundary.orders.SampleLedgerExporter;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.StaleLedgerUpdateException;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.cognos.SampleCoverageFirstMetFetcher;
import org.broadinstitute.gpinformatics.infrastructure.cognos.entity.SampleCoverageFirstMet;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

@UrlBinding("/orders/ledger.action")
public class BillingLedgerActionBean extends CoreActionBean {

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

    private String orderId;
    private ProductOrder productOrder;
    private List<PriceItem> priceItems = new ArrayList<>();
    private List<ProductOrderSampleLedgerInfo> productOrderSampleLedgerInfos = new ArrayList<>();
    private Map<String, SampleCoverageFirstMet> coverageFirstMetBySample;
    private List<Long> selectedProductOrderSampleIds;

    /**
     * List of price item names for this product order. This is saved into the form and checked against current data
     * when the form is submitted to make sure that nothing changed between rendering and submitting the form, including
     * the order of the price items. With that guaranteed, we know that the originalQuantities array is reliable.
     */
    private List<String> priceItemNames;

    /**
     * List of sample names for this product order. This is saved into the form and checked against current data when
     * the form is submitted to make sure that nothing changed between rendering and submitting the form.
     */
    private List<String> sampleNames;

    public static class FormData {
        private String sampleName;
        private Date workCompleteDate;
        private Map<Long, ProductOrderSampleQuantity> quantities;

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

        public Map<Long, ProductOrderSampleQuantity> getQuantities() {
            return quantities;
        }

        public void setQuantities(Map<Long, ProductOrderSampleQuantity> quantities) {
            this.quantities = quantities;
        }
    }

    public static class ProductOrderSampleQuantity {
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

    private List<FormData> formData;

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
        priceItemNames = new ArrayList<>();
        for (PriceItem priceItem : priceItems) {
            priceItemNames.add(priceItem.getName());
        }
    }

    @DefaultHandler
    public Resolution view() {
        return new ForwardResolution(BILLING_LEDGER_PAGE);
    }

    @HandlesEvent("ledgerDetails")
    public Resolution ledgerDetails() throws JSONException {
        JSONObject result = new JSONObject();
        for (ProductOrderSampleLedgerInfo info : productOrderSampleLedgerInfos) {
            Integer samplePosition = info.getSample().getSamplePosition();
            result.put(samplePosition.toString(), makeLedgerDetailJson(info));
        }
        return createTextResolution(result.toString());
    }

    @HandlesEvent("updateLedgers")
    public Resolution updateLedgers() {
        Multimap<ProductOrderSample, ProductOrderSample.LedgerUpdate> ledgerUpdates = LinkedListMultimap.create();
        for (int i = 0; i < formData.size(); i++) {
            FormData data = formData.get(i);
            ProductOrderSample productOrderSample = productOrder.getSamples().get(i);
            Map<PriceItem, ProductOrderSample.LedgerQuantities> ledgerQuantitiesMap =
                    productOrderSample.getLedgerQuantities();
            for (Map.Entry<Long, ProductOrderSampleQuantity> entry : data.getQuantities().entrySet()) {
                PriceItem priceItem = priceItemDao.findById(PriceItem.class, entry.getKey());
                ProductOrderSampleQuantity quantities = entry.getValue();
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
        try {
            productOrderEjb.updateSampleLedgers(ledgerUpdates.asMap());
        } catch (StaleLedgerUpdateException e) {
            addGlobalValidationError(e.getMessage());
        }
        return new RedirectResolution("/orders/ledger.action?orderId=" + productOrder.getBusinessKey());
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

    public List<Long> getSelectedProductOrderSampleIds() {
        return selectedProductOrderSampleIds;
    }

    public void setSelectedProductOrderSampleIds(List<Long> selectedProductOrderSampleIds) {
        this.selectedProductOrderSampleIds = selectedProductOrderSampleIds;
    }

    public double[][] getOriginalQuantities() {
        return originalQuantities;
    }

    public void setOriginalQuantities(double[][] originalQuantities) {
        this.originalQuantities = originalQuantities;
    }

    public List<FormData> getFormData() {
        return formData;
    }

    public void setFormData(List<FormData> formData) {
        this.formData = formData;
    }

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
/*
                Double quantity = billedQuantities.get(priceItem);
                if (quantity == null) {
                    billedQuantities.put(priceItem, ledgerEntry.getQuantity());
                } else {
                    billedQuantities.put(priceItem, quantity + ledgerEntry.getQuantity());
                }
*/
            }
            if (coverageFirstMet != null && !primaryBilled) {
                autoFillQuantity = 1;
            }
        }

        public String baseName() {
            return String.format("%d-%s", productOrderSample.getSamplePosition(), productOrderSample.getName());
        }

        public String getDateComplete() {
            Date workCompleteDate = productOrderSample.getWorkCompleteDate();
            if (workCompleteDate == null) {
                workCompleteDate = coverageFirstMet;
            }
            return workCompleteDate != null ? new SimpleDateFormat("MMM d, yyyy").format(workCompleteDate) : null;
        }

        public String baseName(PriceItem priceItem) {
            return String.format("%s-%d", baseName(), priceItem.getPriceItemId());
        }

        public ProductOrderSample getSample() {
            return productOrderSample;
        }

        public Date getCoverageFirstMet() {
            return coverageFirstMet;
        }

        public Map<PriceItem, ProductOrderSample.LedgerQuantities> getLedgerQuantities() {
            return ledgerQuantities;
        }

        public double getBilledForPriceItem(PriceItem priceItem) {
            ProductOrderSample.LedgerQuantities quantities = ledgerQuantities.get(priceItem);
            return quantities != null ? quantities.getBilled() : 0;
        }

        public double getTotalForPriceItem(PriceItem priceItem) {
            ProductOrderSample.LedgerQuantities quantities = ledgerQuantities.get(priceItem);
            return quantities != null ? quantities.getTotal() : 0;
        }

        public ListMultimap<PriceItem, LedgerEntry> getLedgerEntriesByPriceItem() {
            return ledgerEntriesByPriceItem;
        }

        public int getAutoFillQuantity() {
            return autoFillQuantity;
        }
    }
}
