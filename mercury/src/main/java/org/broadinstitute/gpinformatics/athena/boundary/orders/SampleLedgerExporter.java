package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingTrackerUtils;
import org.broadinstitute.gpinformatics.athena.boundary.util.AbstractSpreadsheetExporter;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * This class creates a spreadsheet version of a product order's sample billing status, also called the sample
 * billing ledger.  The exported spreadsheet will later be imported after the user has updated the sample
 * billing information.
 *
 * A future version of this class will probably support both export and import, since there will be some common
 * code & data structures.
 */
public class SampleLedgerExporter extends AbstractSpreadsheetExporter {

    // Each worksheet is a different product, so distribute the list of orders by product
    private final Map<Product, List<ProductOrder>> orderMap = new HashMap<Product, List<ProductOrder>>();

    private static final int FIXED_HEADER_WIDTH = 259 * 15;
    private static final int VALUE_WIDTH = 259 * 25;
    private static final int ERRORS_WIDTH = 259 * 100;
    private static final int COMMENTS_WIDTH = 259 * 60;

    private final BSPUserList bspUserList;
    private final PriceListCache priceListCache;

    public SampleLedgerExporter(BSPUserList bspUserList, PriceListCache priceListCache, List<ProductOrder> productOrders) {
        this.bspUserList = bspUserList;
        this.priceListCache = priceListCache;

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

    private static Date getWorkCompleteDate(Set<LedgerEntry> ledgerEntries, ProductOrderSample productOrderSample) {
        if (ledgerEntries == null) {
            return null;
        }

        // Very simple logic that for now rolls up all work complete dates and assumes they are the same across
        // all price items on the PDO sample.
        for (LedgerEntry ledgerEntry : ledgerEntries) {
            if (!ledgerEntry.isBilled() && ledgerEntry.getProductOrderSample().equals(productOrderSample)) {
                return ledgerEntry.getWorkCompleteDate();
            }
        }

        return null;
    }

    public static List<PriceItem> getPriceItems(Product product, PriceListCache priceItemListCache) {
        // Create a copy of the product's price items list in order to impose an order on it.
        List<PriceItem> allPriceItems = new ArrayList<PriceItem>(product.getOptionalPriceItems(priceItemListCache));
        Collections.sort(allPriceItems, new Comparator<PriceItem>() {
            @Override
            public int compare(PriceItem o1, PriceItem o2) {
                return new CompareToBuilder().append(o1.getPlatform(), o2.getPlatform())
                        .append(o1.getCategory(), o2.getCategory())
                        .append(o1.getName(), o2.getName()).build();
            }
        });

        // primary price item always goes first
        allPriceItems.add(0, product.getPrimaryPriceItem());

        return allPriceItems;
    }

    private void writePriceItemProductHeader(PriceItem priceItem, Product product) {
        getWriter().writeCell(priceItem.getName() + " [" + product.getPartNumber() + "]", 2, getPriceItemProductHeaderStyle());
    }

    private void writeBillAndNewHeaders() {
        getWriter().writeCell("Billed", getBilledAmountsHeaderStyle());
        getWriter().setColumnWidth(VALUE_WIDTH);
        getWriter().writeCell("Update Quantity To", getBilledAmountsHeaderStyle());
        getWriter().setColumnWidth(VALUE_WIDTH);
    }

    /**
     * Write out the spreadsheet contents to a stream.  The output is in native excel format.
     * @param out the stream to write to
     * @throws IOException if the stream can't be written to
     */
    public void writeToStream(OutputStream out) throws IOException {

        // Order Products by part number so the tabs will have a predictable order.  The orderMap HashMap could have
        // been made a TreeMap to achieve the same effect
        List<Product> productsSortedByPartNumber = new ArrayList<Product>(orderMap.keySet());
        Collections.sort(productsSortedByPartNumber);

        // Go through each product
        for (Product currentProduct : productsSortedByPartNumber) {

            // per 2012-11-19 conversation with Alex and Hugh, Excel does not give us enough characters in a tab
            // name to allow for the product name in all cases, so use just the part number
            getWriter().createSheet(currentProduct.getPartNumber());

            List<ProductOrder> productOrders = orderMap.get(currentProduct);

            // Write headers after placing an extra line
            getWriter().nextRow();
            for (String header : BillingTrackerUtils.FIXED_HEADERS) {
                getWriter().writeCell(header, getFixedHeaderStyle());
                getWriter().setColumnWidth(FIXED_HEADER_WIDTH);
            }

            // Get the ordered price items for the current product, add the spanning price item + product headers
            List<PriceItem> sortedPriceItems = getPriceItems(currentProduct, priceListCache);

            // add on products
            List<Product> sortedAddOns = new ArrayList<Product>(currentProduct.getAddOns());
            Collections.sort(sortedAddOns);

            writeHeaders(currentProduct, sortedPriceItems, sortedAddOns);

            // Write content.
            int sortOrder = 1;
            for (ProductOrder productOrder : productOrders) {
                productOrder.loadBspData();
                for (ProductOrderSample sample : productOrder.getSamples()) {
                    writeRow(sortedPriceItems, sortedAddOns, sample, sortOrder++);
                }
            }
        }

        getWorkbook().write(out);
    }

    private void writeRow(List<PriceItem> sortedPriceItems, List<Product> sortedAddOns, ProductOrderSample sample, int sortOrder) {
        getWriter().nextRow();

        // sample name
        getWriter().writeCell(sample.getSampleName());

        // collaborator sample ID, looks like this is properly initialized
        getWriter().writeCell(sample.getBspDTO().getCollaboratorsSampleName());

        // Material type from BSP (GPLIM-422)
        getWriter().writeCell(sample.getBspDTO().getMaterialType());

        // Risk Information (GPLIM-660)
        String riskString = sample.getRiskString();
        if (StringUtils.isBlank(riskString)) {
            getWriter().nextCell();
        } else {
            getWriter().writeCell(riskString, getRiskStyle());
        }

        // Sample Status
        ProductOrderSample.DeliveryStatus status = sample.getDeliveryStatus();
        getWriter().writeCell(status.getDisplayName());

        // product name
        getWriter().writeCell(sample.getProductOrder().getProduct().getProductName());

        // Product Order ID
        getWriter().writeCell(sample.getProductOrder().getBusinessKey());

        // Product Order Name (actually this concept is called 'Title' in PDO world)
        getWriter().writeCell(sample.getProductOrder().getTitle());

        // Project Manager - need to turn this into a user name
        getWriter().writeCell(getBspFullName(sample.getProductOrder().getCreatedBy()));

        // Per 2012-11-20 HipChat discussion with Hugh and Alex we will not try to store comment
        // Per 2012-11-20 HipChat discussion with Howie this might be used as a read-only field for
        // advisory info.  If someone has time to write this useful info this column can go back in
        // getWriter().writeCell("Useful info about the billing history " + sample.getSampleName());

        // work complete date is the date of any ledger items that are ready to be billed
        getWriter().writeCell(getWorkCompleteDate(sample.getBillableLedgerItems(), sample), getDateStyle());

        // Quote ID
        getWriter().writeCell(sample.getProductOrder().getQuoteId());

        // sort order to be able to reconstruct the originally sorted sample list
        getWriter().writeCell(sortOrder);

        // per 2012-11-19 meeting not doing this
        // getWriter().writeCell(sample.getBillingStatus().getDisplayName());

        Map<PriceItem, ProductOrderSample.LedgerQuantities> billCounts = sample.getLedgerQuantities();

        // write out for the price item columns
        for (PriceItem item : sortedPriceItems) {
            writeCountsForPriceItems(billCounts, item);
        }

        // And for add-ons
        for (Product addOn : sortedAddOns) {
            List<PriceItem> sortedAddOnPriceItems = getPriceItems(addOn, priceListCache);
            for (PriceItem item : sortedAddOnPriceItems) {
                writeCountsForPriceItems(billCounts, item);
            }
        }

        // write the comments
        String theComment = "";
        if (!StringUtils.isBlank(sample.getProductOrder().getComments())) {
            theComment += sample.getProductOrder().getComments();
        }

        if (!StringUtils.isBlank(sample.getSampleComment())) {
            theComment += "--" + sample.getSampleComment();
        }
        getWriter().writeCell(theComment);

        // Any messages for items that are not billed yet
        String billingError = sample.getUnbilledLedgerItemMessages();

        if (StringUtils.isBlank(billingError)) {
            // no value, so just empty a blank line
            getWriter().nextCell();
        } else if (billingError.equals(BillingSession.SUCCESS)) {
            // Give the success message with no special style
            getWriter().writeCell(billingError);
        } else {
            // Only use error style when there is an error in the string
            getWriter().writeCell(billingError, getErrorMessageStyle());
        }

        if (status == ProductOrderSample.DeliveryStatus.ABANDONED) {
            // Set the row style last, so all columns are affected.
            getWriter().setRowStyle(getAbandonedStyle());
        }
    }

    private static String getBillingError(Set<LedgerEntry> billableItems) {
        Set<String> errors = new HashSet<String>();

        // Collect all unique errors
        for (LedgerEntry ledger : billableItems) {
            if (!StringUtils.isBlank(ledger.getBillingMessage())) {
                errors.add(ledger.getBillingMessage());
            }
        }

        return StringUtils.join(errors.iterator(), ", ");
    }

    private void writeHeaders(Product currentProduct, List<PriceItem> sortedPriceItems, List<Product> sortedAddOns) {
        for (PriceItem priceItem : sortedPriceItems) {
            writePriceItemProductHeader(priceItem, currentProduct);
        }

        // Repeat the process for add ons
        for (Product addOn : sortedAddOns) {
            List<PriceItem> sortedAddOnPriceItems = getPriceItems(addOn, priceListCache);
            for (PriceItem priceItem : sortedAddOnPriceItems) {
                writePriceItemProductHeader(priceItem, addOn);
            }
        }

        getWriter().writeCell("Comments", getFixedHeaderStyle());
        getWriter().setColumnWidth(COMMENTS_WIDTH);
        getWriter().writeCell("Billing Errors", getFixedHeaderStyle());
        getWriter().setColumnWidth(ERRORS_WIDTH);

        writeAllBillAndNewHeaders(currentProduct.getOptionalPriceItems(priceListCache), currentProduct.getAddOns());
    }

    private void writeAllBillAndNewHeaders(List<PriceItem> priceItems, Set<Product> addOns) {
        // The new row
        getWriter().nextRow();

        // The empty fixed headers
        writeEmptyFixedHeaders();

        // primary price item for main product
        writeBillAndNewHeaders();
        for (PriceItem priceItem : priceItems) {
            writeBillAndNewHeaders();
        }

        for (Product addOn : addOns) {
            // primary price item for this add-on
            writeBillAndNewHeaders();

            for (PriceItem priceItem : addOn.getOptionalPriceItems(priceListCache)) {
                writeBillAndNewHeaders();
            }
        }

        // GPLIM-491 freeze the first two rows so sort doesn't disturb them
        getWriter().createFreezePane(0, 2);
    }

    private void writeEmptyFixedHeaders() {
        // Write blank secondary header line for fixed columns, with default styling.
        for (String header : BillingTrackerUtils.FIXED_HEADERS) {
            getWriter().writeCell(" ");
        }
    }

    /**
     * Write out the two count columns for the specified price item.
     *
     * @param billCounts All the counts for this PDO sample
     * @param item The price item to look up
     */
    private void writeCountsForPriceItems(Map<PriceItem, ProductOrderSample.LedgerQuantities> billCounts, PriceItem item) {
        ProductOrderSample.LedgerQuantities quantities = billCounts.get(item);
        if (quantities != null) {
            // If the entry for billed is 0, then don't highlight it, but show a light yellow for anything with values
            if (quantities.getBilled() == 0.0) {
                getWriter().writeCell(quantities.getBilled());
            } else {
                getWriter().writeCell(quantities.getBilled(), getBilledAmountStyle());
            }

            // If the entry represents a change, then highlight it with a light yellow
            if (quantities.getBilled() == quantities.getUploaded()) {
                getWriter().writeCell(quantities.getUploaded());
            } else {
                getWriter().writeCell(quantities.getUploaded(), getBilledAmountStyle());
            }
        } else {
            // write nothing for billed and new
            getWriter().writeCell(ProductOrderSample.NO_BILL_COUNT);
            getWriter().writeCell(ProductOrderSample.NO_BILL_COUNT);
        }
    }
}
