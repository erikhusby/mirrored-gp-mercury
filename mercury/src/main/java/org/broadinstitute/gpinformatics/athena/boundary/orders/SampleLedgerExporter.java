package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.broadinstitute.gpinformatics.athena.boundary.util.AbstractSpreadsheetExporter;
import org.broadinstitute.gpinformatics.athena.entity.orders.BillableItem;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
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

    private static final String[] FIXED_HEADERS = {
            "Sample ID",
            "Collaborator Sample ID",
            "Product Name",
            "Product Order ID",
            "Product Order Name",
            "Project Manager",
            "Comments",
            "Date Completed",
            "Quote ID"
    };

    /** Count shown when no billing has occurred. */
    private static final String NO_BILL_COUNT = "0";

    private BSPUserList bspUserList;

    public SampleLedgerExporter(ProductOrder[] productOrders, BSPUserList bspUserList) {
        super();

        this.bspUserList = bspUserList;

        for (ProductOrder productOrder : productOrders) {
            if (!orderMap.containsKey(productOrder.getProduct())) {
                orderMap.put(productOrder.getProduct(), new ArrayList<ProductOrder>());
            }

            orderMap.get(productOrder.getProduct()).add(productOrder);
        }
    }

    private String getBspUsername(long id) {
        if (bspUserList == null) {
            return "User id " + id;
        }

        return bspUserList.getById(id).getUsername();
    }

    private List<PriceItem> getPriceItems(Product product) {
        // Create a copy of the product's price items list in order to impose an order on it.
        List<PriceItem> priceItems = new ArrayList<PriceItem>(product.getPriceItems());
        Collections.sort(priceItems, new Comparator<PriceItem>() {
            @Override
            public int compare(PriceItem o1, PriceItem o2) {
                return new CompareToBuilder().append(o1.getPlatform(), o2.getPlatform())
                        .append(o1.getCategory(), o2.getCategory())
                        .append(o1.getName(), o2.getName()).build();
            }
        });

        return priceItems;
    }

    private static Map<PriceItem, BigDecimal> getBillCounts(ProductOrderSample sample) {
        Set<BillableItem> billableItems = sample.getBillableItems();
        if (billableItems.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<PriceItem, BigDecimal> sampleStatus = new HashMap<PriceItem, BigDecimal>();
        for (BillableItem item : billableItems) {
            sampleStatus.put(item.getPriceItem(), item.getCount());
        }
        return sampleStatus;
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
            getWriter().setCurrentSheet(getWorkbook().createSheet(currentProduct.getPartNumber()));

            List<ProductOrder> productOrders = orderMap.get(currentProduct);

            // Write preamble.
            String preambleText = "Orders: ";
            for (ProductOrder productOrder : productOrders) {
                preambleText += productOrder.getJiraTicketKey() + "_" +
                                productOrder.getTitle() + "_" +
                                productOrder.getProduct().getProductName() + "; ";
            }

            getWriter().writePreamble(preambleText);

            // Write headers after placing an extra line
            getWriter().nextRow();
            for (String header : FIXED_HEADERS) {
                getWriter().writeCell(header, getHeaderStyle());
            }

            // Get the ordered price items for the current product
            List<PriceItem> priceItems = getPriceItems(currentProduct);

            for (PriceItem item : priceItems) {
                getWriter().writeCell(item.getCategory() + " - " + item.getName(), getHeaderStyle());
            }

            // Write content.
            for (ProductOrder productOrder : productOrders) {
                for (ProductOrderSample sample : productOrder.getSamples()) {
                    getWriter().nextRow();
                    // sample name
                    getWriter().writeCell(sample.getSampleName());
                    // collaborator sample ID, looks like this is properly initialized
                    getWriter().writeCell(sample.getBspDTO().getCollaboratorsSampleName());
                    // product name
                    getWriter().writeCell(sample.getProductOrder().getProduct().getProductName());
                    // Product Order ID
                    getWriter().writeCell(sample.getProductOrder().getBusinessKey());
                    // Product Order Name (actually this concept is called 'Title' in PDO world)
                    getWriter().writeCell(sample.getProductOrder().getTitle());
                    // Project Manager - need to turn this into a user name
                    getWriter().writeCell(getBspUsername(sample.getProductOrder().getCreatedBy()));
                    // TODO comment - this needs to come from the ledger
                    getWriter().writeCell("comment from ledger");
                    // TODO Work completed date
                    getWriter().writeCell("work completed date from ledger");
                    // Quote ID
                    getWriter().writeCell(sample.getProductOrder().getQuoteId());

                    // per 2012-11-19 meeting not doing this
                    // getWriter().writeCell(sample.getBillingStatus().getDisplayName());

                    Map<PriceItem, BigDecimal> billCounts = getBillCounts(sample);
                    for (PriceItem item : priceItems) {
                        BigDecimal count = billCounts.get(item);
                        if (count != null) {
                            getWriter().writeCell(count.doubleValue());
                        } else {
                            getWriter().writeCell(NO_BILL_COUNT);
                        }
                    }
                }
            }
        }

        getWorkbook().write(out);
    }
}
