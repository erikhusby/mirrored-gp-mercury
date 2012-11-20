package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.broadinstitute.gpinformatics.athena.boundary.util.AbstractSpreadsheetExporter;
import org.broadinstitute.gpinformatics.athena.entity.orders.BillableItem;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;

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

    private static final String[] FIXED_HEADERS = { "Sample ID", "Billing Status"};

    /** Count shown when no billing has occurred. */
    private static final String NO_BILL_COUNT = "0";

    public SampleLedgerExporter(ProductOrder[] productOrders) {
        super();

        for (ProductOrder productOrder : productOrders) {
            if (!orderMap.containsKey(productOrder.getProduct())) {
                orderMap.put(productOrder.getProduct(), new ArrayList<ProductOrder>());
            }

            orderMap.get(productOrder.getProduct()).add(productOrder);
        }
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

        // Go through each product
        for (Product currentProduct : orderMap.keySet()) {

            getWriter().setCurrentSheet(
                getWorkbook().createSheet(currentProduct.getPartNumber()));

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
                    getWriter().writeCell(sample.getSampleName());
                    getWriter().writeCell(sample.getBillingStatus().getDisplayName());

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
