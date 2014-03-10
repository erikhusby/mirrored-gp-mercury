package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingTrackerHeader;
import org.broadinstitute.gpinformatics.athena.boundary.util.AbstractSpreadsheetExporter;
import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.control.dao.work.WorkCompleteMessageDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.work.WorkCompleteMessage;
import org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPLSIDUtil;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUtil;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class creates a spreadsheet version of a product order's sample billing status, also called the sample
 * billing ledger.  The exported spreadsheet will later be imported after the user has updated the sample
 * billing information.
 *
 * A future version of this class will probably support both export and import, since there will be some common
 * code & data structures.
 */
public class SampleLedgerExporter extends AbstractSpreadsheetExporter {

    // Each worksheet is a different product, so distribute the list of orders by product.
    private final Map<Product, List<ProductOrder>> orderMap = new HashMap<>();

    private static final int FIXED_HEADER_WIDTH = 259 * 15;
    private static final int VALUE_WIDTH = 259 * 25;
    private static final int ERRORS_WIDTH = 259 * 100;
    private static final int COMMENTS_WIDTH = 259 * 60;

    private final PriceItemDao priceItemDao;
    private final BSPUserList bspUserList;
    private final PriceListCache priceListCache;
    private final WorkCompleteMessageDao workCompleteMessageDao;
    private final BSPSampleDataFetcher sampleDataFetcher;
    private final AppConfig appConfig;
    private final TableauConfig tableauConfig;

    public SampleLedgerExporter(
            PriceItemDao priceItemDao,
            BSPUserList bspUserList,
            PriceListCache priceListCache,
            List<ProductOrder> productOrders,
            WorkCompleteMessageDao workCompleteMessageDao,
            BSPSampleDataFetcher sampleDataFetcher,
            AppConfig appConfig,
            TableauConfig tableauConfig) {

        this.priceItemDao = priceItemDao;
        this.bspUserList = bspUserList;
        this.priceListCache = priceListCache;
        this.workCompleteMessageDao = workCompleteMessageDao;
        this.sampleDataFetcher = sampleDataFetcher;
        this.appConfig = appConfig;
        this.tableauConfig = tableauConfig;

        for (ProductOrder productOrder : productOrders) {
            if (!orderMap.containsKey(productOrder.getProduct())) {
                orderMap.put(productOrder.getProduct(), new ArrayList<ProductOrder>());
            }

            orderMap.get(productOrder.getProduct()).add(productOrder);
        }
    }

    private String getBspFullName(long id) {
        if (bspUserList == null) {
            return "User id " + id;
        }

        BspUser user = bspUserList.getById(id);
        if (user == null) {
            return "User id " + id;
        }

        return user.getFullName();
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
                    getWriter().writeCell(header.getText(), getWrappedHeaderStyle(
                            new byte[]{(byte) 204, (byte) 204, (byte) 255}, header == BillingTrackerHeader.TABLEAU_LINK));
                    if (header == BillingTrackerHeader.TABLEAU_LINK) {
                        getWriter().setColumnWidth(900);
                    } else {
                        getWriter().setColumnWidth(FIXED_HEADER_WIDTH);
                    }
                }
            }

            // Get the ordered price items for the current product, add the spanning price item + product headers.
            List<PriceItem> sortedPriceItems = getPriceItems(currentProduct, priceItemDao, priceListCache);

            // add on products.
            List<Product> sortedAddOns = new ArrayList<>(currentProduct.getAddOns());
            Collections.sort(sortedAddOns);

            // Increase the row height to make room for long headers that wrap to multiple lines
            getWriter().setRowHeight((short) (getWriter().getCurrentSheet().getDefaultRowHeight() * 4));
            writeHeaders(currentProduct, sortedPriceItems, sortedAddOns);

            // Freeze the first row to make it persistent when scrolling.
            getWriter().createFreezePane(0, 1);

            // Write content.
            int sortOrder = 1;
            for (ProductOrder productOrder : productOrders) {
                productOrder.loadBspData();
                Map<String, WorkCompleteMessage> workCompleteMessageBySample =
                        getWorkCompleteMessageBySample(productOrder);
                for (ProductOrderSample sample : productOrder.getSamples()) {
                    writeRow(sortedPriceItems, sortedAddOns, sample, sortOrder++, workCompleteMessageBySample);
                }
            }
        }

        getWorkbook().write(out);
    }

    private Map<String, WorkCompleteMessage> getWorkCompleteMessageBySample(ProductOrder productOrder) {
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

        Map<String, WorkCompleteMessage> workCompleteMessageBySample = new HashMap<>();
        for (WorkCompleteMessage workCompleteMessage : workCompleteMessages) {
            String aliquotId = workCompleteMessage.getAliquotId();
            if (BSPLSIDUtil.isBspLsid(aliquotId)) {
                aliquotId = "SM-" + BSPLSIDUtil.lsidToBareId(aliquotId);
                workCompleteMessageBySample.put(aliquotId, workCompleteMessage);
                String stockId = stockIdByAliquotId.get(aliquotId);
                if (stockId == null) {
                    throw new RuntimeException("Couldn't find a stock sample for aliquot: " + aliquotId);
                }
                workCompleteMessageBySample.put(stockId, workCompleteMessage);
            }
        }
        return workCompleteMessageBySample;
    }

    private void writeRow(List<PriceItem> sortedPriceItems, List<Product> sortedAddOns, ProductOrderSample sample,
                          int sortOrder, Map<String, WorkCompleteMessage> workCompleteMessageBySample) {
        getWriter().nextRow();

        // sample name.
        getWriter().writeCell(sample.getName());

        // collaborator sample ID, looks like this is properly initialized.
        getWriter().writeCell(sample.getBspSampleDTO().getCollaboratorsSampleName());

        // Material type.
        getWriter().writeCell(sample.getBspSampleDTO().getMaterialType());

        // Risk Information.
        String riskString = sample.getRiskString();
        if (StringUtils.isBlank(riskString)) {
            getWriter().nextCell();
        } else {
            getWriter().writeCell(riskString, getRiskStyle());
        }

        // Sample Status.
        ProductOrderSample.DeliveryStatus status = sample.getDeliveryStatus();
        getWriter().writeCell(status.getDisplayName());

        Product product = sample.getProductOrder().getProduct();
        // product name.
        getWriter().writeCell(product.getProductName());

        // Product Order ID.
        String pdoKey = sample.getProductOrder().getBusinessKey();
        getWriter().writeCellLink(pdoKey, ProductOrderActionBean.getProductOrderLink(pdoKey, appConfig));

        // Product Order Name (actually this concept is called 'Title' in PDO world).
        getWriter().writeCell(sample.getProductOrder().getTitle());

        // Project Manager - need to turn this into a user name.
        getWriter().writeCell(getBspFullName(sample.getProductOrder().getCreatedBy()));

        // Lane Count
        if (BillingTrackerHeader.LANE_COUNT.shouldShow(product)) {
            getWriter().writeCell(sample.getProductOrder().getLaneCount());
        }

        // auto bill date is the date of any ledger items were auto billed by external messages.
        getWriter().writeCell(sample.getLatestAutoLedgerTimestamp(), getDateStyle());

        // work complete date is the date of any ledger items that are ready to be billed.
        getWriter().writeCell(sample.getWorkCompleteDate(), getDateStyle());

        // work complete message data
        WorkCompleteMessage workCompleteMessage = workCompleteMessageBySample.get(sample.getSampleKey());

        // BSP receipt data
        Date receiptDate = null;
        Date metadataUploadDate = null;
        if (workCompleteMessage != null) {
            receiptDate = workCompleteMessage.getReceiptDate();
            metadataUploadDate = workCompleteMessage.getMetadataUploadDate();
        }

        // BSP receipt properties
        if (BillingTrackerHeader.RECEIPT_DATE.shouldShow(product)) {
            if (receiptDate != null) {
                getWriter().writeCell(receiptDate, getDateStyle());
            } else {
                getWriter().writeCell("");
            }
        }

        if (BillingTrackerHeader.METADATA_UPLOAD_DATE.shouldShow(product)) {
            if (metadataUploadDate != null) {
                getWriter().writeCell(metadataUploadDate, getDateStyle());
            } else {
                getWriter().writeCell("");
            }
        }

        // Picard metrics
        BigInteger pfReads = null;
        BigInteger pfAlignedGb = null;
        BigInteger pfReadsAlignedInPairs = null;
        Double percentCoverageAt20x = null;
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
        if (BillingTrackerHeader.PERCENT_COVERAGE_AT_20X.shouldShow(product)) {
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

        if (status == ProductOrderSample.DeliveryStatus.ABANDONED) {
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

    private void writeHeaders(Product currentProduct, List<PriceItem> sortedPriceItems, List<Product> sortedAddOns) {
        int colorIndex = 0;

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
            // write nothing for billed and new.
            getWriter().writeCell(ProductOrderSample.NO_BILL_COUNT);
            getWriter().writeCell(ProductOrderSample.NO_BILL_COUNT);
        }
    }
}
