package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.billing.ProductLedgerIndex;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This is the information needed to import a quantity of some price item on a quote.
 */
public class QuoteImportInfo {

    /**
     * What the heck is this complicated structure? It is used to take in ledger items and bucket them
     * in a way that makes getQuoteImportItems easy later. The buckets (the map keys) are:
     * <ul>
     * <li>Quote Id (the string that is the main key).</li>
     * <li>Price Item & Product (all the info to separate a call to the quote server by primary price or add on).</li>
     * <li>Bill Date (this separates the counts by billing period. The get period will do the logic of
     * placing into an appropriate chunk).</li>
     * </ul>
     * All this maps to:
     * <p/>
     *      Billing Ledger - We keep the whole ledger so we can place any errors on each item AND it has the count
     */
    private final Map<String, Map<ProductOrder, Map<ProductLedgerIndex, Map<Date, Map<String, List<LedgerEntry>>>>>> quantitiesByQuotePriceItem =
            new HashMap<>();

    /**
     * Take the ledger item and bucket it into the nasty structure we use here.
     *
     * @param ledger The single, ledger item
     */
    public void addQuantity(LedgerEntry ledger) {

        // Get the appropriate quote id (the one we will bill or the one we did bill depending on state).
        String quoteId = getLedgerQuoteId(ledger);

        // The price item on the ledger entry.
        Optional<PriceItem> priceItem = Optional.ofNullable(ledger.getPriceItem());

        Product product;
        if(ledger.getProductOrderSample().getProductOrder().hasSapQuote()) {
            product = ledger.getProduct();
        } else {
            product = ledger.getProductOrderSample().getProductForPriceItem(priceItem.orElse(null));
        }

        // If we have not seen the quote yet, create the map entry for it.
        if (!quantitiesByQuotePriceItem.containsKey(quoteId)) {
            quantitiesByQuotePriceItem.put(quoteId, new HashMap<ProductOrder, Map<ProductLedgerIndex, Map<Date, Map<String, List<LedgerEntry>>>>> ());
        }

        ProductLedgerIndex index;
        index = new ProductLedgerIndex(product, priceItem.orElse(null));
        index.setSapIndex(ledger.getProductOrderSample().getProductOrder().hasSapQuote());

        ProductOrder orderIndex = ledger.getProductOrderSample().getProductOrder();

        if(!quantitiesByQuotePriceItem.get(quoteId).containsKey(orderIndex)) {
            quantitiesByQuotePriceItem.get(quoteId).put(orderIndex, new HashMap<ProductLedgerIndex, Map<Date, Map<String, List<LedgerEntry>>>> ());
        }

        // If the price item has not been added yet, add the quantity, otherwise, add the quantity to what was there.
        if (!quantitiesByQuotePriceItem.get(quoteId).get(orderIndex).containsKey(index)) {
            quantitiesByQuotePriceItem.get(quoteId).get(orderIndex).put(index, new HashMap<Date, Map<String, List<LedgerEntry>>> ());
        }

        // Get the date bucket for this price item.
        Date bucketDate = ledger.getBucketDate();

        if (!quantitiesByQuotePriceItem.get(quoteId).get(orderIndex).get(index).containsKey(bucketDate)) {
            quantitiesByQuotePriceItem.get(quoteId).get(orderIndex).get(index).put(bucketDate, new HashMap< String, List<LedgerEntry>> ());
        }

        String deliveryCondition = Optional.ofNullable(ledger.getSapReplacement()).orElse("None");

        if(!quantitiesByQuotePriceItem.get(quoteId).get(orderIndex).get(index).get(bucketDate).containsKey(deliveryCondition)) {
            quantitiesByQuotePriceItem.get(quoteId).get(orderIndex).get(index).get(bucketDate).put(deliveryCondition,
                    new ArrayList<>());
        }
        // Add this ledger item.
        quantitiesByQuotePriceItem.get(quoteId).get(orderIndex).get(index).get(bucketDate).get(deliveryCondition).add(ledger);
    }

    /**
     * Return the quote on the ledger. If the ledger is not yet billed it won't be set, so return the quote on the
     * the product order since this is the one that will be billed.
     *
     * @param ledger The ledger entry being looked at.
     *
     * @return The quote identifier.
     */
    private static String getLedgerQuoteId(LedgerEntry ledger) {
        if (!StringUtils.isBlank(ledger.getQuoteId())) {
            return ledger.getQuoteId();
        }

        return ledger.getProductOrderSample().getProductOrder().getQuoteId();
    }

    public List<QuoteImportItem> getQuoteImportItems(PriceListCache priceListCache) throws QuoteServerException {
        List<QuoteImportItem> quoteItems = new ArrayList<>();



        for (String quoteId : quantitiesByQuotePriceItem.keySet()) {
            Map<ProductOrder, Map<ProductLedgerIndex, Map<Date, Map<String, List<LedgerEntry>>>>> quotePriceItems =
                    quantitiesByQuotePriceItem.get(quoteId);
            for (ProductOrder orderIndex : quotePriceItems.keySet()) {
                for (Map.Entry<ProductLedgerIndex, Map<Date, Map<String,List<LedgerEntry>>>> ledgerEntrybyLedgerIndex : quotePriceItems
                        .get(orderIndex).entrySet()) {
                    ProductLedgerIndex ledgerIndex = ledgerEntrybyLedgerIndex.getKey();
                    if (orderIndex.getQuoteSource() == ProductOrder.QuoteSourceType.QUOTE_SERVER) {
                        final QuotePriceItem ledgerIndexPriceItem =
                                priceListCache.findByKeyFields(ledgerIndex.getPriceItem());
                        if(ledgerIndexPriceItem != null) {
                            ledgerIndex.getPriceItem().setPrice(ledgerIndexPriceItem.getPrice());
                        }
                    }

                    for (Date bucketDate : ledgerEntrybyLedgerIndex.getValue().keySet()) {
//                        List<LedgerEntry> ledgerItems =
                        Map<String, List<LedgerEntry>> stringListMap =
                                quotePriceItems.get(orderIndex).get(ledgerIndex).get(bucketDate);

                        for (Map.Entry<String, List<LedgerEntry>> ledgerItemsByReplacement:stringListMap.entrySet()) {
                            List<LedgerEntry> creditLedgerItems = new ArrayList<>();
                            List<LedgerEntry> debitLedgerItems = new ArrayList<>();
                            List<LedgerEntry> replacementCreditLedgerItems = new ArrayList<>();
                            List<LedgerEntry> replacementDebitLedgerItems = new ArrayList<>();

                            List<LedgerEntry> ledgerItems = ledgerItemsByReplacement.getValue();
                            // Separate the items into debits and credits so that the quote server will not cancel out items.
                            for (LedgerEntry ledger : ledgerItems) {
                                if (ledger.getQuantity() < 0) {
                                    if (isReplacementPriceItem(priceListCache, ledger)) {
                                        replacementCreditLedgerItems.add(ledger);
                                    } else {
                                        creditLedgerItems.add(ledger);
                                    }
                                } else {
                                    if (isReplacementPriceItem(priceListCache, ledger)) {
                                        replacementDebitLedgerItems.add(ledger);
                                    } else {
                                        debitLedgerItems.add(ledger);
                                    }
                                }
                            }


                            addQuoteItemsForLedgerItems(quoteItems, quoteId,
                                    ledgerIndex.getPriceItem(),
                                    LedgerEntry.PriceItemType.PRIMARY_PRICE_ITEM, debitLedgerItems, bucketDate,
                                    ledgerIndex.getProduct(), orderIndex);

                            addQuoteItemsForLedgerItems(quoteItems, quoteId,
                                    ledgerIndex.getPriceItem(),
                                    LedgerEntry.PriceItemType.REPLACEMENT_PRICE_ITEM, replacementDebitLedgerItems,
                                    bucketDate,
                                    ledgerIndex.getProduct(), orderIndex);

                            addQuoteItemsForLedgerItems(quoteItems, quoteId,
                                    ledgerIndex.getPriceItem(),
                                    LedgerEntry.PriceItemType.PRIMARY_PRICE_ITEM, creditLedgerItems, bucketDate,
                                    ledgerIndex.getProduct(), orderIndex);

                            addQuoteItemsForLedgerItems(quoteItems, quoteId,
                                    ledgerIndex.getPriceItem(),
                                    LedgerEntry.PriceItemType.REPLACEMENT_PRICE_ITEM, replacementCreditLedgerItems,
                                    bucketDate,
                                    ledgerIndex.getProduct(), orderIndex);
                        }
                    }
                }
            }
        }

        return quoteItems;
    }

    private void addQuoteItemsForLedgerItems(
            List<QuoteImportItem> quoteItems, String quoteId, PriceItem priceItem,
            LedgerEntry.PriceItemType priceItemType, List<LedgerEntry> ledgerItems, Date bucketDate, Product product,
            ProductOrder productOrder) {

        String quoteType = priceItemType.getQuoteType();
        if (!ledgerItems.isEmpty()) {
            QuoteImportItem newQuoteItem = new QuoteImportItem(quoteId, priceItem, quoteType, ledgerItems, bucketDate,
                    product, productOrder);
            quoteItems.add(newQuoteItem);
        }
    }

    /**
     * This tests whether the ledger entry is a quote item or a quote replacement item. If the session was
     * already billed (a quote is applied), then pull this right off the ledger. If this has not been billed, then
     * calculate it from the product order.
     *
     * @param ledger The ledger entry.
     * @return Is the entry a replacement or not.
     */
    private boolean isReplacementPriceItem(PriceListCache priceListCache, LedgerEntry ledger) {
        if (ledger.getQuoteId() != null) {
            return LedgerEntry.PriceItemType.REPLACEMENT_PRICE_ITEM == ledger.getPriceItemType();
        }

        if(ledger.getProductOrderSample().getProductOrder().hasSapQuote()) {
            if(ledger.hasSapReplacementPricing()) {
                return true;
            } else {
                return false;
            }
        } else {
            // No quote, so calculate what it would be given the state of things now.
            final Product product = ledger.getProductOrderSample().getProductOrder().getProduct();

            final PriceItem priceItem = product.getPrimaryPriceItem();
            Collection<QuotePriceItem> quotePriceItems = priceListCache.getReplacementPriceItems(priceItem);

            for (QuotePriceItem quotePriceItem : quotePriceItems) {
                if (ledger.getPriceItem().getName().equals(quotePriceItem.getName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
