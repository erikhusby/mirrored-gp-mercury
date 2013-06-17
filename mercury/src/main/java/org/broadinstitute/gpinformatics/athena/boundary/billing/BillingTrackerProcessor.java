package org.broadinstitute.gpinformatics.athena.boundary.billing;

import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.ValidationErrors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.boundary.orders.OrderBillSummaryStat;
import org.broadinstitute.gpinformatics.athena.boundary.orders.SampleLedgerExporter;
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

    private ProductOrder currentProductOrder = null;
    private Product currentProduct = null;
    private List<ProductOrderSample> currentSamples = null;
    private int sampleIndexInOrder = 0;
    private List<PriceItem> currentPriceItems = null;

    // The map of charges keyed by PDO name
    private final Map<String, Map<BillableRef, OrderBillSummaryStat>> chargesMapByPdo = new HashMap<>();

    private List<String> headerValues;

    private ValidationErrors validationErrors;

    public Map<String, Map<BillableRef, OrderBillSummaryStat>> getChargesMapByPdo() {
        return chargesMapByPdo;
    }

    public BillingTrackerProcessor(
            String sheetName, ProductDao productDao, ProductOrderDao productOrderDao, PriceItemDao priceItemDao,
            PriceListCache priceListCache, ValidationErrors validationErrors) {

        this.productOrderDao = productOrderDao;
        this.validationErrors = validationErrors;

        // The current product is the one specified here by the sheet name (the product number).
        currentProduct = productDao.findByPartNumber(sheetName);

        // The price items for the product, in order that they appear in the spreadsheet.
        currentPriceItems = SampleLedgerExporter.getPriceItems(currentProduct, priceItemDao, priceListCache);
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
    protected ColumnHeader[] getColumnHeaders() {
        return BillingTrackerHeader.values();
    }

    @Override
    public void processRow(Map<String, String> dataRow, int dataRowIndex) {

        String rowPdoIdStr = dataRow.get(BillingTrackerHeader.PRODUCT_ORDER_NAME.getText());
        String currentSampleName = dataRow.get(BillingTrackerHeader.SAMPLE_ID_HEADING.getText());

        Map<BillableRef, OrderBillSummaryStat> pdoSummaryStatsMap =
                getBillableRefOrderBillSummaryStatMap(dataRowIndex, rowPdoIdStr);
        if (!validationErrors.isEmpty()) {
            return;
        }

        // There must always be a sample and a product order, so go to the next line, if this does not get one.
        if ((CollectionUtils.isEmpty(currentSamples)) || (currentProduct == null)) {
            return;
        }

        checkSample(dataRowIndex, rowPdoIdStr, currentSampleName,
                dataRow.get(BillingTrackerHeader.AUTO_LEDGER_TIMESTAMP_HEADING.getText()));
        if (!validationErrors.isEmpty()) {
            return;
        }

        String error = parseRowForSummaryMap(dataRow, pdoSummaryStatsMap);
        if (!StringUtils.isBlank(error)) {
            addError(error);
            return;
        }

        chargesMapByPdo.put(rowPdoIdStr, pdoSummaryStatsMap);

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

    private Map<BillableRef, OrderBillSummaryStat> getBillableRefOrderBillSummaryStatMap(
            int dataRowIndex, String rowPdoIdStr) {

        // Get the stats for the current PDO id.
        Map<BillableRef, OrderBillSummaryStat> pdoSummaryStatsMap = chargesMapByPdo.get(rowPdoIdStr);

        // For a newly found PDO Id, create a new map for it and add it to the sheet summary map. This relies on the
        // fae that each order will reqpeat until all samples are complete, so it is new series of samples whenever
        // the map for the PDO is not found.
        if (pdoSummaryStatsMap == null) {
            pdoSummaryStatsMap = new HashMap<>();
            chargesMapByPdo.put(rowPdoIdStr, pdoSummaryStatsMap);

            // Find the order in the DB.
            currentProductOrder = productOrderDao.findByBusinessKey(rowPdoIdStr);
            if (currentProductOrder != null) {
                currentSamples = currentProductOrder.getSamples();

                for (PriceItem priceItem : currentPriceItems) {
                    BillableRef billableRef = new BillableRef(currentProduct.getPartNumber(), priceItem.getName());
                    chargesMapByPdo.get(rowPdoIdStr).put(billableRef, new OrderBillSummaryStat());
                }

                sampleIndexInOrder = 0;
            } else {
                addError("Product Order " + rowPdoIdStr + " on row " + (dataRowIndex + 1) +
                           " of sheet " + currentProduct.getPartNumber() + " is not found in the database.");
            }
        }

        return pdoSummaryStatsMap;
    }

    private void addError(String error) {
        if (validationErrors == null) {
            throw new RuntimeException(error);
        } else {
            validationErrors.addGlobalError(new SimpleError(error));
        }
    }

    private String parseRowForSummaryMap(
            Map<String,String> dataRow, Map<BillableRef,OrderBillSummaryStat> pdoSummaryStatsMap) {

        ProductOrderSample productOrderSample = currentSamples.get(sampleIndexInOrder);
        Map<PriceItem, ProductOrderSample.LedgerQuantities> billCounts = productOrderSample.getLedgerQuantities();

        for (PriceItem priceItem : currentPriceItems) {
            double newQuantity;
            double previouslyBilledQuantity = 0;
            BillableRef billableRef = new BillableRef(currentProduct.getPartNumber(), priceItem.getName());

            String billedKey = BillingTrackerHeader.getPriceItemHeader(priceItem, currentProduct) + " " +
                            BillingTrackerHeader.BILLED;
            String billed = dataRow.get(billedKey);

            String priceItemName = billableRef.getPriceItemName();
            if (!StringUtils.isBlank(billed)) {
                previouslyBilledQuantity = Double.parseDouble(billed);

                // Check billedQuantity parsed against that which is already billed for this sample and PriceItem, they
                // should match.
                ProductOrderSample.LedgerQuantities quantities = billCounts.get(priceItem);
                if ((quantities != null) && !MathUtils.isSame(quantities.getBilled(), previouslyBilledQuantity)) {
                    return String.format(
                            "Found a different billed quantity '%f' in the database for sample in %s in %s, " +
                            "price item '%s', in Product %s. The billed quantity in the spreadsheet is " +
                            "'%f', please download a recent copy of the BillingTracker spreadsheet.",
                            quantities.getBilled(), productOrderSample.getSampleKey(), currentProductOrder.getBusinessKey(),
                            priceItemName, currentProduct.getPartNumber(), previouslyBilledQuantity);
                }

                if (quantities == null && previouslyBilledQuantity != 0) {
                    return String.format("No billed quantity found in the database for sample %s in %s, price item " +
                                         "'%s', in Product %s. However the billed quantity in the spreadsheet " +
                                         "is '%f', indicating the Billed column of this spreadsheet has accidentally " +
                                         "been edited.",
                            productOrderSample.getSampleKey(), currentProductOrder.getBusinessKey(), priceItemName,
                            currentProduct.getPartNumber(), previouslyBilledQuantity );
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

                double delta = newQuantity - previouslyBilledQuantity;

                if (delta != 0) {
                    String uploadedQuoteId = dataRow.get(BillingTrackerHeader.QUOTE_ID_HEADING.getText());
                    if (StringUtils.isBlank(uploadedQuoteId)) {
                        return String.format("Found empty %s value for updated sample %s in %s, price item '%s', in Product %s",
                                BillingTrackerHeader.QUOTE_ID_HEADING.getText(), productOrderSample.getSampleKey(),
                                currentProductOrder.getBusinessKey(), priceItemName,
                                currentProduct.getPartNumber());
                    }

                    if (!productOrderSample.getProductOrder().getQuoteId().equals(uploadedQuoteId)) {
                        return MessageFormat
                                .format("Found quote ID ''{0}'' for updated sample ''{1}'' in ''{2}'' in Product" +
                                        " ''{3}'', this differs from quote ''{4}'' currently associated with ''{2}''.",
                                        uploadedQuoteId, productOrderSample.getSampleKey(),
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
                }
            }
        }

        return "";
    }

}
