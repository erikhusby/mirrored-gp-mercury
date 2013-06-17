package org.broadinstitute.gpinformatics.athena.boundary.billing;

import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.ValidationErrors;
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
    private List<ProductOrderSample> currentSamples = null;
    private int sampleIndexInOrder = 0;
    private List<PriceItem> currentPriceItems = null;
    private final List<ProductOrder> productOrders = new ArrayList<>();

    // The map of charges keyed by PDO name
    private final Map<String, Map<BillableRef, OrderBillSummaryStat>> chargesMapByPdo = new HashMap<>();

    private List<String> headerValues;

    private ValidationErrors validationErrors;

    private final boolean doPersist;

    public Map<String, Map<BillableRef, OrderBillSummaryStat>> getChargesMapByPdo() {
        return chargesMapByPdo;
    }

    public BillingTrackerProcessor(
            String sheetName, LedgerEntryDao ledgerEntryDao, ProductDao productDao, ProductOrderDao productOrderDao,
            PriceItemDao priceItemDao, PriceListCache priceListCache, ValidationErrors validationErrors,
            boolean doPersist) {

        this.productOrderDao = productOrderDao;
        this.ledgerEntryDao = ledgerEntryDao;
        this.validationErrors = validationErrors;

        // The current product is the one specified here by the sheet name (the product number).
        currentProduct = productDao.findByPartNumber(sheetName);

        // The price items for the product, in order that they appear in the spreadsheet.
        currentPriceItems = SampleLedgerExporter.getPriceItems(currentProduct, priceItemDao, priceListCache);

        this.doPersist = doPersist;
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
        for (String originalHeader : originalHeaders) {
            if (originalHeader.contains("[")) {
                headerValues.add(originalHeader + " " + BillingTrackerHeader.BILLED);
                headerValues.add(originalHeader + " " + BillingTrackerHeader.UPDATE);
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
    public void processRow(Map<String, String> dataRow, int dataRowIndex) {

        String rowPdoIdStr = dataRow.get(BillingTrackerHeader.PRODUCT_ORDER_NAME.getText());
        String currentSampleName = dataRow.get(BillingTrackerHeader.SAMPLE_ID_HEADING.getText());

        // Get the stats for the current PDO id. If it does not exist, add the new summary to the map.
        Map<BillableRef, OrderBillSummaryStat> pdoSummaryStatsMap = chargesMapByPdo.get(rowPdoIdStr);
        if (pdoSummaryStatsMap == null) {
            // Update the charges map.
            addSummaryToChargesMap(dataRowIndex, rowPdoIdStr);
            if (!validationErrors.isEmpty()) {
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

        // Verify the sample information.
        checkSample(dataRowIndex, rowPdoIdStr, currentSampleName,
                dataRow.get(BillingTrackerHeader.AUTO_LEDGER_TIMESTAMP_HEADING.getText()));
        if (!validationErrors.isEmpty()) {
            return;
        }

        // Now that everything else has been set up, parse and process it.
        String error = parseRowForSummaryMap(dataRow, rowPdoIdStr);
        if (!StringUtils.isBlank(error)) {
            addError(error);
            return;
        }

        sampleIndexInOrder++;
    }

    private void checkSample(int dataRowIndex, String rowPdoIdStr, String currentSampleName, String autoLedgerString){

        // The order in the spreadsheet is the same as returned in the productOrder.
        if (sampleIndexInOrder >= currentSamples.size()) {
            String error = "Sample " + currentSampleName + " on row " +  (dataRowIndex + 1 ) +
                    " of spreadsheet "  + currentProduct.getPartNumber() +
                    " is not in the expected position. The Order <" + currentProductOrder.getTitle() +
                    " (Id: " + rowPdoIdStr + ")> has only " + currentSamples.size() + " samples.";
            addError(error);
            return;
        }

        // Make sure the product order sample name in the current position matches the sample list
        ProductOrderSample productOrderSample = currentSamples.get(sampleIndexInOrder);
        if (!productOrderSample.getSampleName().equals(currentSampleName)) {
            String error = "Sample " + currentSampleName + " on row " + (dataRowIndex + 1) +
                " of spreadsheet " + currentProduct.getPartNumber() +
                " is in different position than expected. Expected value from Order is "
                + productOrderSample.getSampleName();
            addError(error);
            return;
        }

        try {
            // Check the latest date on the sample and make sure it is not after the date on the data row.
            Date autoLedgerTimestamp = DateUtils.parseDate(autoLedgerString);
            if (autoLedgerTimestamp.after(productOrderSample.getLatestAutoLedgerTimestamp())) {
                String error = "Sample " + currentSampleName + " on row " + (dataRowIndex + 1) +
                               " of spreadsheet " + currentProduct.getPartNumber() +
                               " was auto billed after this tracker was downloaded. Download the tracker again to " +
                               "validate " + productOrderSample.getSampleName();
                addError(error);
            }
        } catch (ParseException ex) {
            addError(ex.getMessage());
        }
    }

    private void addSummaryToChargesMap(int dataRowIndex, String rowPdoIdStr) {

        Map<BillableRef, OrderBillSummaryStat> pdoSummaryStatsMap = new HashMap<>();
        chargesMapByPdo.put(rowPdoIdStr, pdoSummaryStatsMap);

        // Find the order in the DB.
        currentProductOrder = productOrderDao.findByBusinessKey(rowPdoIdStr);
        if (currentProductOrder != null) {

            // Add to the list of product orders that were processed.
            productOrders.add(currentProductOrder);
            currentSamples = currentProductOrder.getSamples();

            if (currentSamples == null) {
                addError("Product Order " + rowPdoIdStr + " on row " + (dataRowIndex + 1) +
                         " of sheet " + currentProduct.getPartNumber() + " has no samples.");
                return;
            }

            // Add values for each price item.
            for (PriceItem priceItem : currentPriceItems) {
                BillableRef billableRef = new BillableRef(currentProduct.getPartNumber(), priceItem.getName());
                pdoSummaryStatsMap.put(billableRef, new OrderBillSummaryStat());
            }

            sampleIndexInOrder = 0;
        } else {
            addError("Product Order " + rowPdoIdStr + " on row " + (dataRowIndex + 1) +
                       " of sheet " + currentProduct.getPartNumber() + " is not found in the database.");
        }
    }

    private void addError(String error) {
        if (validationErrors == null) {
            throw new RuntimeException(error);
        } else {
            validationErrors.addGlobalError(new SimpleError(error));
        }
    }

    private static final String BILLED_IS_SAME =
            "Found a different billed quantity '%f' in the database for sample in %s in %s, price item '%s', in " +
            "Product %s. The billed quantity in the spreadsheet is '%f', please download a recent copy of the " +
            "BillingTracker spreadsheet.";

    private static final String PREVIOUSLY_BILLED =
            "No billed quantity found in the database for sample %s in %s, price item '%s', in Product %s. However " +
            "the billed quantity in the spreadsheet is '%f', indicating the Billed column of this spreadsheet has " +
            "accidentally been edited.";

    private static final String SAMPLE_EMPTY_VALUE =
            "Found empty %s value for updated sample %s in %s, price item '%s', in Product %s";

    private static final String QUOTE_MISMATCH =
            "Found quote ID ''{0}'' for updated sample ''{1}'' in ''{2}'' in Product" +
            " ''{3}'', this differs from quote ''{4}'' currently associated with ''{2}''.";

    private String parseRowForSummaryMap(Map<String, String> dataRow, String rowPdoIdStr) {
        ProductOrderSample productOrderSample = currentSamples.get(sampleIndexInOrder);

        // Get the stats for the order.
        Map<BillableRef, OrderBillSummaryStat> pdoSummaryStatsMap = chargesMapByPdo.get(rowPdoIdStr);

        Map<PriceItem, ProductOrderSample.LedgerQuantities> billCounts = productOrderSample.getLedgerQuantities();

        for (PriceItem priceItem : currentPriceItems) {
            double newQuantity;
            double trackerBilled = 0;
            BillableRef billableRef = new BillableRef(currentProduct.getPartNumber(), priceItem.getName());

            String billedKey = BillingTrackerHeader.getPriceItemHeader(priceItem, currentProduct) + " " +
                            BillingTrackerHeader.BILLED;

            String priceItemName = billableRef.getPriceItemName();
            if (!StringUtils.isBlank(dataRow.get(billedKey))) {
                trackerBilled = Double.parseDouble(dataRow.get(billedKey));

                double sampleBilled = 0;
                ProductOrderSample.LedgerQuantities sampleQuantities = billCounts.get(priceItem);
                if (sampleQuantities != null) {
                    sampleBilled = sampleQuantities.getBilled();
                } else {
                    if (trackerBilled != 0) {
                        return String.format(PREVIOUSLY_BILLED, productOrderSample.getSampleKey(),
                                currentProductOrder.getBusinessKey(), priceItemName, currentProduct.getPartNumber(),
                                trackerBilled );
                    }
                }

                // Check billedQuantity in spreadsheet against that which is already billed for this sample and
                // price item, they should match.
                if (!MathUtils.isSame(trackerBilled, sampleBilled)) {
                    return String.format(BILLED_IS_SAME, sampleBilled, productOrderSample.getSampleKey(),
                            currentProductOrder.getBusinessKey(), priceItemName, currentProduct.getPartNumber(),
                            trackerBilled);
                }
            }

            String updateToKey = BillingTrackerHeader.getPriceItemHeader(priceItem, currentProduct) + " " +
                              BillingTrackerHeader.UPDATE;
            String updateTo = dataRow.get(updateToKey);

            if (!StringUtils.isBlank(updateTo)) {
                newQuantity = Double.parseDouble(updateTo);
                if (newQuantity < 0) {
                    return String.format("Found negative new quantity '%f' for sample %s in %s, price item '%s', in Product %s",
                            newQuantity, productOrderSample.getSampleKey(), currentProductOrder.getBusinessKey(), priceItemName,
                            currentProduct.getPartNumber());
                }

                double delta = newQuantity - trackerBilled;

                if (delta != 0) {
                    String uploadedQuoteId = dataRow.get(BillingTrackerHeader.QUOTE_ID_HEADING.getText());
                    if (StringUtils.isBlank(uploadedQuoteId)) {
                        return String.format(SAMPLE_EMPTY_VALUE, BillingTrackerHeader.QUOTE_ID_HEADING.getText(),
                                productOrderSample.getSampleKey(), currentProductOrder.getBusinessKey(), priceItemName,
                                currentProduct.getPartNumber());
                    }

                    if (!productOrderSample.getProductOrder().getQuoteId().equals(uploadedQuoteId)) {
                        return MessageFormat
                                .format(QUOTE_MISMATCH, uploadedQuoteId, productOrderSample.getSampleKey(),
                                        currentProductOrder.getBusinessKey(), currentProduct.getPartNumber(),
                                        productOrderSample.getProductOrder().getQuoteId());
                    }

                    String workCompleteDateString = dataRow.get(BillingTrackerHeader.WORK_COMPLETE_DATE_HEADING.getText());
                    if (StringUtils.isBlank(workCompleteDateString)) {
                        return String.format("Found empty %s value for updated sample %s in %s, price item '%s', in Product %s",
                                BillingTrackerHeader.WORK_COMPLETE_DATE_HEADING.getText(), productOrderSample.getSampleKey(),
                                currentProductOrder.getBusinessKey(), priceItemName, currentProduct.getPartNumber());
                    }

                    OrderBillSummaryStat orderBillSummaryStat = pdoSummaryStatsMap.get(billableRef);
                    if (orderBillSummaryStat == null) {
                        // Create a new stat obj and add it to the map.
                        orderBillSummaryStat = new OrderBillSummaryStat();
                        pdoSummaryStatsMap.put(billableRef, orderBillSummaryStat);
                    }

                    orderBillSummaryStat.applyDelta(delta);

                    try {
                        if (doPersist) {
                            Date workCompleteDate = DateUtils.parseDate(workCompleteDateString);
                            productOrderSample.addLedgerItem(workCompleteDate, priceItem, delta);
                        }
                    } catch (ParseException e) {
                        return MessageFormat
                                .format("Could not persist ledger for updated sample ''{0}'' in PDO ''{1}'' in Product" +
                                        " ''{3}'' because of error: {4}",
                                        productOrderSample.getSampleKey(), currentProductOrder.getBusinessKey(),
                                        currentProduct.getPartNumber(), e.getMessage());
                    }
                }
            }
        }

        return "";
    }

    public List<ProductOrder> getUpdatedProductOrders() {
        return productOrders;
    }
}
