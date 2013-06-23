package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.boundary.orders.OrderBillSummaryStat;
import org.broadinstitute.gpinformatics.athena.boundary.orders.SampleLedgerExporter;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.LedgerEntryDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class takes columns from a table parser and pulls out the billing tracker information.
 */
public class BillingTrackerProcessor extends TableProcessor {

    private static final long serialVersionUID = 2769150801705141335L;

    private final ProductOrderDao productOrderDao;
    private final LedgerEntryDao ledgerEntryDao;

    private ProductOrder currentProductOrder = null;
    private Product currentProduct = null;
    private List<ProductOrderSample> currentSamples = new ArrayList<>();
    private int sampleIndexInOrder = 0;
    private List<BillableRef> currentBillableRefs = null;
    private final Map<String, PriceItem> currentPriceItemsByName;
    private final List<ProductOrder> productOrders = new ArrayList<>();

    // This holds the map of charges keyed by PDO name.
    private final Map<String, Map<BillableRef, OrderBillSummaryStat>> chargesMapByPdo = new HashMap<>();

    private List<String> headerValues;

    private final boolean doPersist;

    public Map<String, Map<BillableRef, OrderBillSummaryStat>> getChargesMapByPdo() {
        return chargesMapByPdo;
    }

    public BillingTrackerProcessor(
            String sheetName, LedgerEntryDao ledgerEntryDao, ProductDao productDao, ProductOrderDao productOrderDao,
            PriceItemDao priceItemDao, PriceListCache priceListCache, boolean doPersist) {
        super(sheetName);

        this.productOrderDao = productOrderDao;
        this.ledgerEntryDao = ledgerEntryDao;

        // The current product is the one specified here by the sheet name (the product number).
        currentProduct = productDao.findByPartNumber(sheetName);

        // The price items for the product, in order that they appear in the spreadsheet. Populate the price item cache.
        currentPriceItemsByName = new HashMap<>();
        currentBillableRefs = getBillableRefList(currentProduct, priceItemDao, priceListCache, currentPriceItemsByName);
        for (Product addOn : currentProduct.getAddOns()) {
            currentBillableRefs.addAll(getBillableRefList(addOn, priceItemDao, priceListCache, currentPriceItemsByName));
        }

        this.doPersist = doPersist;
    }

    private List<BillableRef> getBillableRefList(Product product, PriceItemDao priceItemDao,
                                                 PriceListCache priceListCache,
                                                 Map<String, PriceItem> currentPriceItemsByName) {

        List<BillableRef> billableRefs = new ArrayList<>();
        List<PriceItem> priceItems = SampleLedgerExporter.getPriceItems(product, priceItemDao, priceListCache);
        for (PriceItem priceItem : priceItems) {
            currentPriceItemsByName.put(priceItem.getName(), priceItem);
            billableRefs.add(new BillableRef(product.getPartNumber(), priceItem.getName()));
        }

        return billableRefs;
    }

    @Override
    public List<String> getHeaderNames() {
        return headerValues;
    }

    @Override
    public int getNumHeaderRows() {
        return 2;
    }

    @Override
    public void processHeader(List<String> originalHeaders, int row) {
        // The second row is captured in the first row, so can just skip.
        if (row == 1) {
            return;
        }

        // Only the product headers contain square brackets, so if those exist add two items for billed and updated.
        headerValues = new ArrayList<> ();

        Iterator<String> headerIterator = originalHeaders.iterator();
        while (headerIterator.hasNext()) {
            String originalHeader = headerIterator.next();
            if (originalHeader.contains("[")) {
                // The bracket is for price item columns, which are double spans.
                headerValues.add(originalHeader + " " + BillingTrackerHeader.BILLED);
                headerValues.add(originalHeader + " " + BillingTrackerHeader.UPDATE);

                // Bump past the 'blank' column that the double span uses.
                headerIterator.next();
            } else {
                headerValues.add(originalHeader);
            }
        }
    }

    @Override
    public void close() {
        if (doPersist) {
            productOrderDao.persistAll(productOrders);
        }
    }

    @Override
    protected ColumnHeader[] getColumnHeaders() {
        return BillingTrackerHeader.values();
    }

    @Override
    public void processRowDetails(Map<String, String> dataRow, int dataRowIndex) {

        // Increment to start because when there is a new row (and first) the addSummaryToChargesMap will set to 0.
        // All error messages want the right row AND then return, so need this to be incremented.
        sampleIndexInOrder++;

        String rowPdoIdStr = dataRow.get(BillingTrackerHeader.ORDER_ID_HEADING.getText());
        String currentSampleName = dataRow.get(BillingTrackerHeader.SAMPLE_ID_HEADING.getText());

        // Get the stats for the current PDO id. If it does not exist, add the new summary to the map.
        Map<BillableRef, OrderBillSummaryStat> pdoSummaryStatsMap = chargesMapByPdo.get(rowPdoIdStr);
        if (pdoSummaryStatsMap == null) {
            // Update the charges map.
            addSummaryToChargesMap(dataRowIndex, rowPdoIdStr);
            if (getMessages().size() > 0) {
                return;
            }

            // If we are doing the persist, the first step is that we need to clear out all the previously billed stuff.
            // This assumes that validation has been done and that no changes will be done.
            if (doPersist) {
                ProductOrder[] productOrderArray = new ProductOrder[]{currentProductOrder};
                ledgerEntryDao.removeLedgerItemsWithoutBillingSession(productOrderArray);
                ledgerEntryDao.flush();
            }

        }

        // Must have a product order or product at this point. If not, it is an error.
        if ((currentProductOrder == null) || (currentProduct == null)) {
            addDataMessage("Sample " + currentSampleName + " cannot find PDO: " + rowPdoIdStr, dataRowIndex);
            return;
        }

        // Verify the sample information.
        checkSample(dataRowIndex, rowPdoIdStr, currentSampleName,
                dataRow.get(BillingTrackerHeader.AUTO_LEDGER_TIMESTAMP_HEADING.getText()));
        if (getMessages().size() > 0) {
            return;
        }

        // Now that everything else has been set up, parse and process it.
        parseRowForSummaryMap(dataRow, dataRowIndex, rowPdoIdStr);
    }

    private void checkSample(int dataRowIndex, String rowPdoIdStr, String currentSampleName, String autoLedgerString) {

        // Make sure we do not have too many samples at this point.
        if (sampleIndexInOrder >= currentSamples.size()) {
            String error = "Too many samples at: " + currentSampleName + " for order <" +
                    currentProductOrder.getTitle() + " (Id: " + rowPdoIdStr + ")> has only " +
                    currentSamples.size() + " samples.";
            addDataMessage(error, dataRowIndex);
            return;
        }

        // The order in the spreadsheet is the same as returned in the productOrder.
        if (!currentSamples.get(sampleIndexInOrder).getSampleName().equals(currentSampleName)) {
            String error = "Sample " + currentSampleName + " is not in the expected position. " +
                           "The Order <" + currentProductOrder.getTitle() +
                           " (Id: " + rowPdoIdStr + ")> has sample name " +
                           currentSamples.get(sampleIndexInOrder).getSampleName() +
                           " at position: " + sampleIndexInOrder;
            addDataMessage(error, dataRowIndex);
            return;
        }

        // Make sure the product order sample name in the current position matches the sample list.
        ProductOrderSample productOrderSample = currentSamples.get(sampleIndexInOrder);
        if (!productOrderSample.getSampleName().equals(currentSampleName)) {
            String error = "Sample " + currentSampleName +
                           " is in different position than expected. Expected value from Order is " +
                           productOrderSample.getSampleName();
            addDataMessage(error, dataRowIndex);
            return;
        }

        try {
            // Check the latest date on the sample and make sure it is not after the date on the data row.
            Date uploadedTimestamp = (!StringUtils.isBlank(autoLedgerString)) ? DateUtils.parseDate(autoLedgerString) : null;
            Date currentSampleTimestamp = productOrderSample.getLatestAutoLedgerTimestamp();
            if (!areTimestampsEqual(uploadedTimestamp, currentSampleTimestamp)) {
                String error = "Sample " + currentSampleName + " was auto billed after this tracker was downloaded. " +
                               "Download the tracker again to validate " + productOrderSample.getSampleName();
                addDataMessage(error, dataRowIndex);
            }
        } catch (ParseException ex) {
            addDataMessage(ex.getMessage(), dataRowIndex);
        }
    }

    /**
     * Check the spreadsheet timestamp against the ledger's timestamp.
     *
     * @param uploadedTimestamp The spreadsheet timestamp
     * @param currentSampleTimestamp The ledger timestamp on the product order sample
     *
     * @return Are they 'equal' by rules set in the if-else logic documented here.
     */
    private boolean areTimestampsEqual(Date uploadedTimestamp, Date currentSampleTimestamp) {
        boolean timestampsAreEqual;
        if (doPersist) {
            // If persisting, then we have cleared out the ledger entries, so there will be no timestamp to compare
            // and validation has already been done, so the timestamp can be considered equal.
            timestampsAreEqual = true;
        } else if ((uploadedTimestamp == null) || (currentSampleTimestamp == null)) {
            // If either are null, both must be null.
            timestampsAreEqual = uploadedTimestamp == currentSampleTimestamp;
        } else {
            // The start of day values of the timestamps must be equal.
            uploadedTimestamp = DateUtils.getStartOfDay(uploadedTimestamp);
            currentSampleTimestamp = DateUtils.getStartOfDay(currentSampleTimestamp);
            timestampsAreEqual = uploadedTimestamp.equals(currentSampleTimestamp);
        }
        return timestampsAreEqual;
    }

    private void addSummaryToChargesMap(int dataRowIndex, String rowPdoIdStr) {

        Map<BillableRef, OrderBillSummaryStat> pdoSummaryStatsMap = new HashMap<>();
        chargesMapByPdo.put(rowPdoIdStr, pdoSummaryStatsMap);

        // Find the order in the DB.
        currentProductOrder = productOrderDao.findByBusinessKey(rowPdoIdStr);
        if (currentProductOrder != null) {

            sampleIndexInOrder = 0; // Set to 0 at start here so row numbers will be correct.

            // Add to the list of product orders that were processed.
            productOrders.add(currentProductOrder);
            currentSamples = currentProductOrder.getSamples();

            if (currentSamples == null) {
                addDataMessage("Product Order " + rowPdoIdStr + " has no samples.", dataRowIndex);
                return;
            }

            // Add values for each price item.
            for (BillableRef billableRef : currentBillableRefs) {
                pdoSummaryStatsMap.put(billableRef, new OrderBillSummaryStat());
            }
        } else {
            addDataMessage("Product Order " + rowPdoIdStr + " is not found in the database.", dataRowIndex);
        }
    }

    // Error messages.
    private static final String BILLED_IS_SAME =
            "Found a different billed quantity '%f' in the database for sample in %s in %s, price item '%s'. " +
            "The billed quantity in the spreadsheet is '%f', please download a recent copy of the " +
            "BillingTracker spreadsheet.";

    private static final String PREVIOUSLY_BILLED =
            "No billed quantity found in the database for sample %s in %s, price item '%s'. However " +
            "the billed quantity in the spreadsheet is '%f', indicating the Billed column of this spreadsheet has " +
            "accidentally been edited.";

    private static final String SAMPLE_EMPTY_VALUE =
            "Found empty %s value for updated sample %s in %s, price item '%s'";

    private static final String QUOTE_MISMATCH =
            "Found quote ID ''{0}'' for updated sample ''{1}'' in ''{2}'', this differs from quote ''{4}'' " +
            "currently associated with ''{2}''.";

    private static final String BAD_UPDATE_FILE =
            "Found empty %s value for updated sample %s in %s, price item '%s'";

    private static final String NEGATIVE_VALUE =
            "Found negative new quantity '%f' for sample %s in %s, price item '%s'";

    private void parseRowForSummaryMap(Map<String, String> dataRow, int dataRowIndex, String rowPdoIdStr) {
        ProductOrderSample productOrderSample = currentSamples.get(sampleIndexInOrder);

        // Get the stats for the order.
        Map<BillableRef, OrderBillSummaryStat> pdoSummaryStatsMap = chargesMapByPdo.get(rowPdoIdStr);

        Map<PriceItem, ProductOrderSample.LedgerQuantities> billCounts = productOrderSample.getLedgerQuantities();

        for (BillableRef billableRef : currentBillableRefs) {
            PriceItem priceItem = currentPriceItemsByName.get(billableRef.getPriceItemName());

            double trackerBilled = 0;

            String billedKey = BillingTrackerHeader.getPriceItemHeader(billableRef) + " " + BillingTrackerHeader.BILLED;

            String priceItemName = billableRef.getPriceItemName();
            if (!StringUtils.isBlank(dataRow.get(billedKey))) {
                trackerBilled = Double.parseDouble(dataRow.get(billedKey));

                double sampleBilled = 0;
                ProductOrderSample.LedgerQuantities sampleQuantities = billCounts.get(priceItem);
                if (sampleQuantities != null) {
                    sampleBilled = sampleQuantities.getBilled();
                } else {
                    if (trackerBilled != 0) {
                        addDataMessage(String.format(PREVIOUSLY_BILLED, productOrderSample.getSampleKey(),
                                currentProductOrder.getBusinessKey(), priceItemName, trackerBilled), dataRowIndex);
                    }
                }

                // Check billedQuantity in spreadsheet against that which is already billed for this sample and
                // price item, they should match.
                if (!MathUtils.isSame(trackerBilled, sampleBilled)) {
                    addDataMessage(String.format(BILLED_IS_SAME, sampleBilled, productOrderSample.getSampleKey(),
                            currentProductOrder.getBusinessKey(), priceItemName, trackerBilled), dataRowIndex);
                }
            }

            doUpdate(dataRow, dataRowIndex, productOrderSample, pdoSummaryStatsMap, billableRef, priceItem,
                    trackerBilled, priceItemName);
        }
    }

    private void doUpdate(Map<String, String> dataRow, int dataRowIndex, ProductOrderSample productOrderSample,
                          Map<BillableRef, OrderBillSummaryStat> pdoSummaryStatsMap, BillableRef billableRef,
                          PriceItem priceItem, double trackerBilled, String priceItemName) {

        String updateToKey =
                BillingTrackerHeader.getPriceItemHeader(billableRef) + " " + BillingTrackerHeader.UPDATE;
        String updateTo = dataRow.get(updateToKey);

        if (!StringUtils.isBlank(updateTo)) {
            double newQuantity;
            newQuantity = Double.parseDouble(updateTo);
            if (newQuantity < 0) {
                addDataMessage(String.format(NEGATIVE_VALUE, newQuantity, productOrderSample.getSampleKey(),
                        currentProductOrder.getBusinessKey(), priceItemName), dataRowIndex);
            }

            double delta = newQuantity - trackerBilled;

            if (delta != 0) {
                String uploadedQuoteId = dataRow.get(BillingTrackerHeader.QUOTE_ID_HEADING.getText());
                if (StringUtils.isBlank(uploadedQuoteId)) {
                    addDataMessage(
                            String.format(SAMPLE_EMPTY_VALUE, BillingTrackerHeader.QUOTE_ID_HEADING.getText(),
                                    productOrderSample.getSampleKey(), currentProductOrder.getBusinessKey(),
                                    priceItemName), dataRowIndex);
                }

                if (!productOrderSample.getProductOrder().getQuoteId().equals(uploadedQuoteId)) {
                    addDataMessage(MessageFormat.format(QUOTE_MISMATCH, uploadedQuoteId,
                            productOrderSample.getSampleKey(), currentProductOrder.getBusinessKey(),
                            currentProduct.getPartNumber(), productOrderSample.getProductOrder().getQuoteId()),
                            dataRowIndex);
                }

                String workCompleteDateString = dataRow.get(BillingTrackerHeader.WORK_COMPLETE_DATE_HEADING.getText());
                if (StringUtils.isBlank(workCompleteDateString)) {
                    addDataMessage(String.format(BAD_UPDATE_FILE,
                            BillingTrackerHeader.WORK_COMPLETE_DATE_HEADING.getText(),
                            productOrderSample.getSampleKey(), currentProductOrder.getBusinessKey(), priceItemName),
                            dataRowIndex);
                }

                OrderBillSummaryStat orderBillSummaryStat = pdoSummaryStatsMap.get(billableRef);
                if (orderBillSummaryStat == null) {
                    // Create a new stat obj and add it to the map.
                    orderBillSummaryStat = new OrderBillSummaryStat();
                    pdoSummaryStatsMap.put(billableRef, orderBillSummaryStat);
                }

                orderBillSummaryStat.applyDelta(delta);

                try {
                    // If there are no messages AND we are persisting, the do the persist.
                    if (CollectionUtils.isEmpty(getMessages()) && doPersist) {
                        Date workCompleteDate = DateUtils.parseDate(workCompleteDateString);
                        productOrderSample.addLedgerItem(workCompleteDate, priceItem, delta);
                    }
                } catch (ParseException e) {
                    addDataMessage(MessageFormat.format(
                            "Could not persist ledger for updated sample ''{0}'' in PDO ''{1}'' because of " +
                            "error: {4}", productOrderSample.getSampleKey(), currentProductOrder.getBusinessKey(),
                            currentProduct.getPartNumber(), e.getMessage()), dataRowIndex);
                }
            }
        }
    }

    public List<ProductOrder> getUpdatedProductOrders() {
        return productOrders;
    }
}
