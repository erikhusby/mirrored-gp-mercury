package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFColor;
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
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.tableau.TableauConfig;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
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
 * <p/>
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
    private final SampleDataFetcher sampleDataFetcher;
    private final AppConfig appConfig;
    private final TableauConfig tableauConfig;
    private final Map<Product, List<SampleLedgerRow>> sampleRowData;
    private final Map<ProductOrder, Map<String, WorkCompleteMessage>> workCompleteMessageCache = new HashMap<>();

    public SampleLedgerExporter(
            PriceItemDao priceItemDao,
            PriceListCache priceListCache,
            List<ProductOrder> productOrders,
            WorkCompleteMessageDao workCompleteMessageDao,
            SampleDataFetcher sampleDataFetcher,
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

        // First add the primary price item.
        allPriceItems.add(product.getPrimaryPriceItem());

        // Now add the replacement price items.
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

    private void writePriceItemProductHeaders(PriceItem priceItem, Product product, XSSFColor color) {
        getWriter().writeCell(BillingTrackerHeader.getPriceItemNameHeader(priceItem), getWrappedHeaderStyle(color));
        getWriter().setColumnWidth(VALUE_WIDTH);
        getWriter().writeCell(BillingTrackerHeader.getPriceItemPartNumberHeader(product), getWrappedHeaderStyle(color));
        getWriter().setColumnWidth(VALUE_WIDTH);
    }

    private void writeHistoricalPriceItemProductHeader(PriceItem priceItem, XSSFColor color) {
        getWriter().writeCell(BillingTrackerHeader.getHistoricalPriceItemNameHeader(priceItem),
                getWrappedHeaderStyle(color));
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

        // Loop through all the products, creating a new sheet for each one.
        for (Product currentProduct : productsSortedByPartNumber) {

            // Excel sheet names are limited to 31 characters, so we use the product's part number instead of its name.
            getWriter().createSheet(currentProduct.getPartNumber());

            // Need to create a new row for the headers.
            getWriter().nextRow();

            List<ProductOrder> productOrders = orderMap.get(currentProduct);
            for (BillingTrackerHeader header : BillingTrackerHeader.values()) {
                if (header.shouldShow(currentProduct)) {
                    getWriter().writeHeaderCell(header.getText());
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
                writeRow(sortedPriceItems, sortedAddOns, historicalPriceItems, sampleLedgerRow.getProductOrderSample(),
                        sortOrder++, sampleLedgerRow);
            }
        }

        getWorkbook().write(out);
    }

    private SortedSet<PriceItem> getHistoricalPriceItems(List<ProductOrder> productOrders, List<PriceItem> priceItems,
                                                         List<Product> addOns) {
        return getHistoricalPriceItems(productOrders, priceItems, addOns, priceItemDao, priceListCache);
    }

    // TODO: this is static; move it somewhere else
    public static SortedSet<PriceItem> getHistoricalPriceItems(List<ProductOrder> productOrders,
                                                               List<PriceItem> priceItems, List<Product> addOns,
                                                               PriceItemDao priceItemDao,
                                                               PriceListCache priceListCache) {
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

    /**
     * Write a row of data for a single sample.
     * <p/>
     * Currently, the data comes from many different places. In order to improve testability, the data is in the
     * process of being consolidated into a {@link SampleLedgerRow} object, which will make test setup easier.
     * <p/>
     * Testing this method is a little difficult. Ideally, its inputs would be simple to make it straightforward to test
     * different scenarios by verifying interactions with the writer. Another idea is to move the responsibility into
     * {@link SampleLedgerRow}. See Phil Shapiro's code review
     * <a href="https://crucible.broadinstitute.org/cru/GPI-748#c32936">comment</a>.
     *
     * @param sortedPriceItems     primary and alternative price items that apply to the product ordered
     * @param sortedAddOns         add-on products to provided price item columns for
     * @param historicalPriceItems price items that have ledger entries for this sample but are not primary, alternative, or add-on price items
     * @param sample               the product order sample for the row
     * @param sortOrder            the value for the sort order column, for re-sorting the tracker before uploading
     * @param sampleLedgerRow      sample data to be written
     */
    void writeRow(List<PriceItem> sortedPriceItems, List<Product> sortedAddOns,
                  Collection<PriceItem> historicalPriceItems, ProductOrderSample sample, int sortOrder,
                  SampleLedgerRow sampleLedgerRow) {
        SampleLedgerSpreadSheetWriter writer = getWriter();
        ProductOrder productOrder = sample.getProductOrder();
        Product product = productOrder.getProduct();
        SampleData sampleData = sample.getSampleData();

        writer.nextRow();

        // sample name.
        writer.writeCell(sample.getSampleKey());

        // collaborator sample ID, looks like this is properly initialized.
        writer.writeCell(sampleData != null ? sampleData.getCollaboratorsSampleName() : "");

        // Risk Information.
        String riskString = sample.getRiskString();
        if (StringUtils.isBlank(riskString)) {
            writer.nextCell();
        } else {
            writer.writeCell(riskString, getRiskStyle());
        }

        // Sample Delivery Status.
        writer.writeCell(sample.getDeliveryStatus().getDisplayName());

        // product name.
        writer.writeCell(product.getProductName());

        // Product Order ID.
        writer.writeCellLink(productOrder.getJiraTicketKey(),
                ProductOrderActionBean.getProductOrderLink(productOrder, appConfig));

        // Lane Count
        if (BillingTrackerHeader.LANE_COUNT.shouldShow(product)) {
            writer.writeCell(productOrder.getLaneCount());
        }

        // Auto bill date is the date of any ledger items were auto billed by external messages.
        writer.writeCell(sample.getLatestAutoLedgerTimestamp(), getDateStyle());

        // Work complete date is the date of any ledger items that are ready to be billed.
        writer.writeCell(sample.getWorkCompleteDate(), getDateStyle());

        // Quote ID.
        writer.writeCell(productOrder.getQuoteId());

        // The sort order column allows users to reconstruct the original spreadsheet row order.
        writer.writeCell(sortOrder);

        // The ledger amounts.
        Map<PriceItem, ProductOrderSample.LedgerQuantities> billCounts = sample.getLedgerQuantities();

        // Write out the price item columns.
        for (PriceItem priceItem : historicalPriceItems) {
            writeCountForHistoricalPriceItem(billCounts, priceItem);
        }

        for (PriceItem item : sortedPriceItems) {
            writeCountsForPriceItems(billCounts, item);
        }

        // And for add-ons.
        for (Product addOn : sortedAddOns) {
            for (PriceItem item : getPriceItems(addOn, priceItemDao, priceListCache)) {
                writeCountsForPriceItems(billCounts, item);
            }
        }

        // Any messages for items that are not billed yet.
        String billingError = sample.getUnbilledLedgerItemMessages();

        if (StringUtils.isBlank(billingError)) {
            writer.nextCell();
        } else if (billingError.endsWith(BillingSession.SUCCESS)) {
            // Give the success message with no special style.
            writer.writeCell(billingError);
        } else {
            // Only use error style when there is an error in the string.
            writer.writeCell(billingError, getErrorMessageStyle());
        }

        if (sample.getDeliveryStatus() == ProductOrderSample.DeliveryStatus.ABANDONED) {
            // Set the row style last, so all columns are affected.
            writer.setRowStyle(getAbandonedStyle());
        }
    }

    @SuppressWarnings("MagicNumber")
    private static final XSSFColor HISTORICAL_PRICE_ITEM_COLOR = new XSSFColor(new Color(204, 204, 204));

    private static class PriceItemColor {

        @SuppressWarnings("MagicNumber")
        private static final XSSFColor[] COLORS = {
                new XSSFColor(new Color(255, 255, 204)),
                new XSSFColor(new Color(255, 204, 255)),
                new XSSFColor(new Color(204, 236, 255)),
                new XSSFColor(new Color(204, 255, 204)),
                new XSSFColor(new Color(204, 153, 255)),
                new XSSFColor(new Color(255, 102, 204)),
                new XSSFColor(new Color(255, 255, 153)),
                new XSSFColor(new Color(0, 255, 255)),
                new XSSFColor(new Color(102, 255, 153))
        };

        private int index;

        private XSSFColor getNext() {
            return COLORS[index++ % COLORS.length];
        }
    }

    private void writeHeaders(Product currentProduct, List<PriceItem> sortedPriceItems, List<Product> sortedAddOns,
                              Collection<PriceItem> historicalPriceItems) {
        for (PriceItem priceItem : historicalPriceItems) {
            writeHistoricalPriceItemProductHeader(priceItem, HISTORICAL_PRICE_ITEM_COLOR);
        }

        PriceItemColor itemColor = new PriceItemColor();

        for (PriceItem priceItem : sortedPriceItems) {
            writePriceItemProductHeaders(priceItem, currentProduct, itemColor.getNext());
        }

        // Repeat the process for add ons
        for (Product addOn : sortedAddOns) {
            List<PriceItem> sortedAddOnPriceItems = getPriceItems(addOn, priceItemDao, priceListCache);
            for (PriceItem priceItem : sortedAddOnPriceItems) {
                writePriceItemProductHeaders(priceItem, addOn, itemColor.getNext());
            }
        }

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
            if (quantities.getBilled() == 0.0) {
                getWriter().writeCell(quantities.getBilled());
            } else {
                // Show a light yellow for anything with non-zero values.
                getWriter().writeCell(quantities.getBilled(), getBilledAmountStyle());
            }

            if (MathUtils.isSame(quantities.getBilled(), quantities.getTotal())) {
                getWriter().writeCell(quantities.getTotal());
            } else {
                // If the entry represents a change, then highlight it with a light yellow.
                getWriter().writeCell(quantities.getTotal(), getBilledAmountStyle());
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
