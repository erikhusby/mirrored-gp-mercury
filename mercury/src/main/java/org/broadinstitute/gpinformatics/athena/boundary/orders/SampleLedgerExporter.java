package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingTrackerHeader;
import org.broadinstitute.gpinformatics.athena.boundary.util.AbstractSpreadsheetExporter;
import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.control.dao.work.WorkCompleteMessageDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.work.WorkCompleteMessage;
import org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPLSIDUtil;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.tableau.TableauConfig;
import org.broadinstitute.gpinformatics.mercury.presentation.TableauRedirectActionBean;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This class creates a spreadsheet version of a product order's sample billing status, also called the sample
 * billing ledger.  The exported spreadsheet will later be imported after the user has updated the sample
 * billing information.
 *
 * A future version of this class will probably support both export and import, since there will be some common
 * code & data structures.
 */
public class SampleLedgerExporter extends AbstractSpreadsheetExporter<SampleLedgerSpreadSheetWriter> {

    // Each worksheet is a different product, so distribute the list of orders by product.
    private final Map<Product, List<ProductOrder>> orderMap = new HashMap<>();

    private static final int FIXED_HEADER_WIDTH = 259 * 15;
    private static final int VALUE_WIDTH = 259 * 25;
    private static final int ERRORS_WIDTH = 259 * 100;
    private static final int COMMENTS_WIDTH = 259 * 60;

    private final PriceItemDao priceItemDao;
    private final PriceListCache priceListCache;
    private final WorkCompleteMessageDao workCompleteMessageDao;
    private final BSPSampleDataFetcher sampleDataFetcher;
    private final AppConfig appConfig;
    private final TableauConfig tableauConfig;
    private final Map<Product, List<SampleLedgerRow>> sampleRowData;
    private final Map<ProductOrder, Map<String, WorkCompleteMessage>> workCompleteMessageCache = new HashMap<>();

    public SampleLedgerExporter(
            PriceItemDao priceItemDao,
            PriceListCache priceListCache,
            List<ProductOrder> productOrders,
            WorkCompleteMessageDao workCompleteMessageDao,
            BSPSampleDataFetcher sampleDataFetcher,
            AppConfig appConfig,
            TableauConfig tableauConfig,
            SampleLedgerSpreadSheetWriter writer,
            Map<Product, List<SampleLedgerRow>> sampleRowData) {
        super(writer);

        this.priceItemDao = priceItemDao;
        this.priceListCache = priceListCache;
        this.workCompleteMessageDao = workCompleteMessageDao;
        this.sampleDataFetcher = sampleDataFetcher;
        this.appConfig = appConfig;
        this.tableauConfig = tableauConfig;
        this.sampleRowData = sampleRowData;

        for (ProductOrder productOrder : productOrders) {
            if (!orderMap.containsKey(productOrder.getProduct())) {
                orderMap.put(productOrder.getProduct(), new ArrayList<ProductOrder>());
            }

            orderMap.get(productOrder.getProduct()).add(productOrder);
        }
    }

    /**
     * This gets the product price item and then its list of optional price items. If the price items are not
     * yet stored in the PRICE_ITEM table, it will create it there.
     *
     * @param product The product.
     * @param priceItemDao The DAO to use for saving any new price item.
     * @param priceItemListCache The price list cache.
     *
     * @return The real price item objects.
     */
    public static List<PriceItem> getPriceItems(Product product, PriceItemDao priceItemDao, PriceListCache priceItemListCache) {

        List<PriceItem> allPriceItems = new ArrayList<>();

        // First add the primary price item
        allPriceItems.add(product.getPrimaryPriceItem());

        // Get the replacement items from the quote cache.
        Collection<QuotePriceItem> quotePriceItems =
            priceItemListCache.getReplacementPriceItems(product.getPrimaryPriceItem());

        // Now add the replacement items as mercury price item objects.
        for (QuotePriceItem quotePriceItem : quotePriceItems) {
            // Find the price item object.
            PriceItem priceItem =
                priceItemDao.find(
                    quotePriceItem.getPlatformName(), quotePriceItem.getCategoryName(), quotePriceItem.getName());

            // If it does not exist create it.
            if (priceItem == null) {
                priceItem = new PriceItem(quotePriceItem.getId(), quotePriceItem.getPlatformName(),
                                          quotePriceItem.getCategoryName(), quotePriceItem.getName());
                priceItemDao.persist(priceItem);
            }

            allPriceItems.add(priceItem);
        }

        return allPriceItems;
    }

    private void writePriceItemProductHeaders(PriceItem priceItem, Product product, byte[] rgbColor) {
        getWriter().writeCell(BillingTrackerHeader.getPriceItemNameHeader(priceItem), getWrappedHeaderStyle(rgbColor));
        getWriter().setColumnWidth(VALUE_WIDTH);
        getWriter().writeCell(BillingTrackerHeader.getPriceItemPartNumberHeader(product), getWrappedHeaderStyle(
                rgbColor));
        getWriter().setColumnWidth(VALUE_WIDTH);
    }

    private void writeHistoricalPriceItemProductHeader(PriceItem priceItem, byte[] rgbColor) {
        getWriter().writeCell(BillingTrackerHeader.getHistoricalPriceItemNameHeader(priceItem),
                getWrappedHeaderStyle(rgbColor));
        getWriter().setColumnWidth(VALUE_WIDTH);
    }

    /**
     * Write out the spreadsheet contents to a stream.  The output is in native excel format.
     *
     * @param out the stream to write to.
     * @throws IOException if the stream can't be written to.
     */
    public void writeToStream(OutputStream out) throws IOException {

        // Order Products by part number so the tabs will have a predictable order.  The orderMap HashMap could have
        // been made a TreeMap to achieve the same effect.
        List<Product> productsSortedByPartNumber = new ArrayList<>(orderMap.keySet());
        Collections.sort(productsSortedByPartNumber);

        // Go through each product
        for (Product currentProduct : productsSortedByPartNumber) {

            // per 2012-11-19 conversation with Alex and Hugh, Excel does not give us enough characters in a tab
            // name to allow for the product name in all cases, so use just the part number.
            getWriter().createSheet(currentProduct.getPartNumber());

            List<ProductOrder> productOrders = orderMap.get(currentProduct);

            // Write headers after placing an extra line
            getWriter().nextRow();
            for (BillingTrackerHeader header : BillingTrackerHeader.values()) {
                if (header.shouldShow(currentProduct)) {
                    getWriter().writeHeaderCell(header.getText(), header == BillingTrackerHeader.TABLEAU_LINK);
                }
            }

            // Get the ordered price items for the current product, add the spanning price item + product headers.
            List<PriceItem> sortedPriceItems = getPriceItems(currentProduct, priceItemDao, priceListCache);

            // add on products.
            List<Product> sortedAddOns = new ArrayList<>(currentProduct.getAddOns());
            Collections.sort(sortedAddOns);

            // historical price items
            SortedSet<PriceItem> historicalPriceItems =
                    getHistoricalPriceItems(productOrders, sortedPriceItems, sortedAddOns);

            writeHeaders(currentProduct, sortedPriceItems, sortedAddOns, historicalPriceItems);

            // Freeze the first row to make it persistent when scrolling.
            getWriter().createFreezePane(0, 1);

            // Write content.
            int sortOrder = 1;
            List<SampleLedgerRow> sampleLedgerRows = sampleRowData.get(currentProduct);
            for (SampleLedgerRow sampleLedgerRow : sampleLedgerRows) {
                ProductOrder productOrder = sampleLedgerRow.getProductOrderSample().getProductOrder();
                productOrder.loadBspData();
                writeRow(sortedPriceItems, sortedAddOns, historicalPriceItems, sampleLedgerRow.getProductOrderSample(),
                        sortOrder++, getWorkCompleteMessageBySample(productOrder), sampleLedgerRow);
            }
        }

        getWorkbook().write(out);
    }

    private SortedSet<PriceItem> getHistoricalPriceItems(List<ProductOrder> productOrders, List<PriceItem> priceItems,
                                                         List<Product> addOns) {
        Set<PriceItem> addOnPriceItems = new HashSet<>();
        for (Product addOn : addOns) {
            addOnPriceItems.addAll(getPriceItems(addOn, priceItemDao, priceListCache));
        }

        SortedSet<PriceItem> historicalPriceItems = new TreeSet<>();
        for (ProductOrder productOrder : productOrders) {
            for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
                for (LedgerEntry ledgerEntry : productOrderSample.getLedgerItems()) {
                    PriceItem priceItem = ledgerEntry.getPriceItem();
                    if (!priceItems.contains(priceItem) && !addOnPriceItems
                            .contains(priceItem)) {
                        historicalPriceItems.add(priceItem);
                    }
                }
            }
        }
        return historicalPriceItems;
    }

    private Map<String, WorkCompleteMessage> getWorkCompleteMessageBySample(ProductOrder productOrder) {
        Map<String, WorkCompleteMessage> workCompleteMessageBySample = workCompleteMessageCache.get(productOrder);
        if (workCompleteMessageBySample == null) {
            workCompleteMessageBySample = new HashMap<>();
            workCompleteMessageCache.put(productOrder, workCompleteMessageBySample);

            List<WorkCompleteMessage> workCompleteMessages = workCompleteMessageDao.findByPDO(
                    productOrder.getBusinessKey());

            List<String> aliquotIds = new ArrayList<>();
            for (WorkCompleteMessage workCompleteMessage : workCompleteMessages) {
                String aliquotId = workCompleteMessage.getAliquotId();
                if (BSPLSIDUtil.isBspLsid(aliquotId)) {
                    aliquotId = BSPLSIDUtil.lsidToBareId(aliquotId);
                    aliquotIds.add(aliquotId);
                }
            }
            Map<String, String> stockIdByAliquotId = sampleDataFetcher.getStockIdByAliquotId(aliquotIds);

            for (WorkCompleteMessage workCompleteMessage : workCompleteMessages) {
                String aliquotId = workCompleteMessage.getAliquotId();
                if (BSPLSIDUtil.isBspLsid(aliquotId)) {
                    aliquotId = "SM-" + BSPLSIDUtil.lsidToBareId(aliquotId);
                    String stockId = stockIdByAliquotId.get(aliquotId);
                    if (stockId != null) {
                        workCompleteMessageBySample.put(stockId, workCompleteMessage);
                    } else {
                        /*
                         * This isn't necessarily a useful thing to do, but it may work in some cases. While it may not
                         * be tremendously useful, it's better than doing nothing or throwing an exception.
                         */
                        workCompleteMessageBySample.put(aliquotId, workCompleteMessage);
                    }
                }
            }
        }
        return workCompleteMessageBySample;
    }

    /**
     * Write a row of data for a single sample.
     *
     * Currently, the data comes from many different places. In order to improve testability, the data is in the
     * process of being consolidated into a {@link SampleLedgerRow} object, which will make test setup easier.
     *
     * Testing this method is a little difficult. Ideally, its inputs would be simple to make it straightforward to test
     * different scenarios by verifying interactions with the writer. Another idea is to move the responsibility into
     * {@link SampleLedgerRow}. See Phil Shapiro's code review
     * <a href="https://crucible.broadinstitute.org/cru/GPI-748#c32936">comment</a>.
     *
     * @param sortedPriceItems               primary and alternative price items that apply to the product ordered
     * @param sortedAddOns                   add-on products to provided price item columns for
     * @param historicalPriceItems           price items that have ledger entries for this sample but are not primary, alternative, or add-on price items
     * @param sample                         the product order sample for the row
     * @param sortOrder                      the value for the sort order column, for re-sorting the tracker before uploading
     * @param workCompleteMessageBySample    any work complete messages for the product order, by sample
     * @param sampleData                     simple bean of sample data to be written
     */
    private void writeRow(List<PriceItem> sortedPriceItems, List<Product> sortedAddOns,
                          Collection<PriceItem> historicalPriceItems, ProductOrderSample sample,
                          int sortOrder, Map<String, WorkCompleteMessage> workCompleteMessageBySample,
                          SampleLedgerRow sampleData) {
        getWriter().nextRow();

        // sample name.
        getWriter().writeCell(sampleData.getSampleId());

        // collaborator sample ID, looks like this is properly initialized.
        getWriter().writeCell(sampleData.getCollaboratorSampleId());

        // Material type.
        getWriter().writeCell(sampleData.getMaterialType());

        // Risk Information.
        String riskString = sampleData.getRiskText();
        if (StringUtils.isBlank(riskString)) {
            getWriter().nextCell();
        } else {
            getWriter().writeCell(riskString, getRiskStyle());
        }

        // Sample Delivery Status.
        String deliveryStatus = sampleData.getDeliveryStatus();
        getWriter().writeCell(deliveryStatus);

        // product name.
        getWriter().writeCell(sampleData.getProductName());

        // Product Order ID.
        String pdoKey = sampleData.getProductOrderKey();
        getWriter().writeCellLink(pdoKey, ProductOrderActionBean.getProductOrderLink(pdoKey, appConfig));

        // Product Order Name (actually this concept is called 'Title' in PDO world).
        getWriter().writeCell(sampleData.getProductOrderTitle());

        // Project Manager - need to turn this into a user name.
        getWriter().writeCell(sampleData.getProjectManagerName());

        // Lane Count
        if (BillingTrackerHeader.LANE_COUNT.shouldShow(sample.getProductOrder().getProduct())) {
            getWriter().writeCell(sampleData.getNumberOfLanes());
        }

        // auto bill date is the date of any ledger items were auto billed by external messages.
        getWriter().writeCell(sampleData.getAutoLedgerDate(), getDateStyle());

        // work complete date is the date of any ledger items that are ready to be billed.
        getWriter().writeCell(sampleData.getWorkCompleteDate(), getDateStyle());

        // Picard metrics
        BigInteger pfReads = null;
        BigInteger pfAlignedGb = null;
        BigInteger pfReadsAlignedInPairs = null;
        Double percentCoverageAt20x = null;
        WorkCompleteMessage workCompleteMessage = workCompleteMessageBySample.get(sample.getSampleKey());
        if (workCompleteMessage != null) {
            pfReads = workCompleteMessage.getPfReads();
            pfAlignedGb = workCompleteMessage.getAlignedGb();
            pfReadsAlignedInPairs = workCompleteMessage.getPfReadsAlignedInPairs();
            percentCoverageAt20x = workCompleteMessage.getPercentCoverageAt20X();
        }

        // PF Reads
        if (pfReads != null) {
            getWriter().writeCell(pfReads.doubleValue());
        } else {
            getWriter().writeCell("");
        }

        // PF Aligned GB
        if (pfAlignedGb != null) {
            getWriter().writeCell(pfAlignedGb.doubleValue());
        } else {
            getWriter().writeCell("");
        }

        // PF Reads Aligned in Pairs
        if (pfReadsAlignedInPairs != null) {
            getWriter().writeCell(pfReadsAlignedInPairs.doubleValue());
        } else {
            getWriter().writeCell("");
        }

        // % coverage at 20x
        if (BillingTrackerHeader.PERCENT_COVERAGE_AT_20X.shouldShow(sample.getProductOrder().getProduct())) {
            if (percentCoverageAt20x != null) {
                getWriter().writeCell(percentCoverageAt20x, getPercentageStyle());
            } else {
                getWriter().writeCell("");
            }
        }

        // Tableau link
        getWriter().writeCellLink(TableauRedirectActionBean.getPdoSequencingSampleDashboardUrl(pdoKey, tableauConfig),
                TableauRedirectActionBean.getPdoSequencingSamplesDashboardRedirectUrl(pdoKey, appConfig));

        // Quote ID.
        getWriter().writeCell(sample.getProductOrder().getQuoteId());

        // sort order to be able to reconstruct the originally sorted sample list.
        getWriter().writeCell(sortOrder);

        // The ledger amounts.
        Map<PriceItem, ProductOrderSample.LedgerQuantities> billCounts = sample.getLedgerQuantities();

        // write out for the price item columns.
        for (PriceItem priceItem : historicalPriceItems) {
            writeCountForHistoricalPriceItem(billCounts, priceItem);
        }

        for (PriceItem item : sortedPriceItems) {
            writeCountsForPriceItems(billCounts, item);
        }

        // And for add-ons.
        for (Product addOn : sortedAddOns) {
            List<PriceItem> sortedAddOnPriceItems = getPriceItems(addOn, priceItemDao, priceListCache);
            for (PriceItem item : sortedAddOnPriceItems) {
                writeCountsForPriceItems(billCounts, item);
            }
        }

        // write the comments.
        String theComment = "";
        if (!StringUtils.isBlank(sample.getProductOrder().getComments())) {
            theComment += sample.getProductOrder().getComments();
        }

        if (!StringUtils.isBlank(sample.getSampleComment())) {
            theComment += "--" + sample.getSampleComment();
        }
        getWriter().writeCell(theComment);

        // Any messages for items that are not billed yet.
        String billingError = sample.getUnbilledLedgerItemMessages();

        if (StringUtils.isBlank(billingError)) {
            // no value, so just empty a blank line.
            getWriter().nextCell();
        } else if (billingError.endsWith(BillingSession.SUCCESS)) {
            // Give the success message with no special style.
            getWriter().writeCell(billingError);
        } else {
            // Only use error style when there is an error in the string.
            getWriter().writeCell(billingError, getErrorMessageStyle());
        }

        if (deliveryStatus.equals(ProductOrderSample.DeliveryStatus.ABANDONED.getDisplayName())) {
            // Set the row style last, so all columns are affected.
            getWriter().setRowStyle(getAbandonedStyle());
        }
    }

    private static byte[][] PRICE_ITEM_COLORS = {
            { (byte) 255, (byte) 255, (byte) 204 },
            { (byte) 255, (byte) 204, (byte) 255 },
            { (byte) 204, (byte) 236, (byte) 255 },
            { (byte) 204, (byte) 255, (byte) 204 },
            { (byte) 204, (byte) 153, (byte) 255 },
            { (byte) 255, (byte) 102, (byte) 204 },
            { (byte) 255, (byte) 255, (byte) 153 },
            { (byte)   0, (byte) 255, (byte) 255 },
            { (byte) 102, (byte) 255, (byte) 153 },
    };

    private void writeHeaders(Product currentProduct, List<PriceItem> sortedPriceItems, List<Product> sortedAddOns,
                              Collection<PriceItem> historicalPriceItems) {
        int colorIndex = 0;

        for (PriceItem priceItem : historicalPriceItems) {
            writeHistoricalPriceItemProductHeader(priceItem, new byte[]{(byte) 204, (byte) 204, (byte) 204});
        }

        for (PriceItem priceItem : sortedPriceItems) {
            writePriceItemProductHeaders(priceItem, currentProduct,
                    PRICE_ITEM_COLORS[colorIndex++ % PRICE_ITEM_COLORS.length]);
        }

        // Repeat the process for add ons
        for (Product addOn : sortedAddOns) {
            List<PriceItem> sortedAddOnPriceItems = getPriceItems(addOn, priceItemDao, priceListCache);
            for (PriceItem priceItem : sortedAddOnPriceItems) {
                writePriceItemProductHeaders(priceItem, addOn,
                        PRICE_ITEM_COLORS[colorIndex++ % PRICE_ITEM_COLORS.length]);
            }
        }

        getWriter().writeCell("Comments", getFixedHeaderStyle());
        getWriter().setColumnWidth(COMMENTS_WIDTH);
        getWriter().writeCell("Billing Errors", getFixedHeaderStyle());
        getWriter().setColumnWidth(ERRORS_WIDTH);
    }

    /**
     * Write out the two count columns for the specified price item.
     *
     * @param billCounts All the counts for this PDO sample.
     * @param item The price item to look up.
     */
    private void writeCountsForPriceItems(Map<PriceItem, ProductOrderSample.LedgerQuantities> billCounts, PriceItem item) {
        ProductOrderSample.LedgerQuantities quantities = billCounts.get(item);
        if (quantities != null) {
            // If the entry for billed is 0, then don't highlight it, but show a light yellow for anything with values.
            if (quantities.getBilled() == 0.0) {
                getWriter().writeCell(quantities.getBilled());
            } else {
                getWriter().writeCell(quantities.getBilled(), getBilledAmountStyle());
            }

            // If the entry represents a change, then highlight it with a light yellow.
            if (MathUtils.isSame(quantities.getBilled(), quantities.getUploaded())) {
                getWriter().writeCell(quantities.getUploaded());
            } else {
                getWriter().writeCell(quantities.getUploaded(), getBilledAmountStyle());
            }
        } else {
            // Write nothing for billed and new.
            getWriter().writeCell(ProductOrderSample.NO_BILL_COUNT);
            getWriter().writeCell(ProductOrderSample.NO_BILL_COUNT);
        }
    }

    private void writeCountForHistoricalPriceItem(Map<PriceItem, ProductOrderSample.LedgerQuantities> billCounts,
                                                  PriceItem item) {
        ProductOrderSample.LedgerQuantities quantities = billCounts.get(item);
        if (quantities != null) {
            // If the entry for billed is 0, then don't highlight it, but show a light yellow for anything with values.
            if (quantities.getBilled() == 0.0) {
                getWriter().writeCell(quantities.getBilled());
            } else {
                getWriter().writeHistoricalBilledAmount(quantities.getBilled());
            }
        } else {
            // Write nothing for billed.
            getWriter().writeCell(ProductOrderSample.NO_BILL_COUNT);
        }
    }
}
