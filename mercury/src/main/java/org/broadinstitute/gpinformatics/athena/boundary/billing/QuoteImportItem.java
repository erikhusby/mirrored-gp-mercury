package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.boundary.products.InvalidProductException;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceList;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.sap.entity.DeliveryCondition;
import org.broadinstitute.sap.entity.quote.SapQuote;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A flattened structure of information needed to import an item into the quote server.
 */
public class QuoteImportItem {
    public static final String PDO_QUANTITY_FORMAT = "###,###,###.##";
    private String tabularIdentifier;
    private final String quoteId;
    private final PriceItem priceItem;
    private String quotePriceType;
    private final Date billToDate;
    private final List<LedgerEntry> ledgerItems;
    private Date startRange;
    private Date endRange;
    private final Set<String> workItems = new HashSet<>();
    private String sapItems;
    private Product product;
    private ProductOrder productOrder;

    private PriceList priceOnWorkDate;
    private Quote quote;
    private SapQuote sapQuote;

    public QuoteImportItem(
            String quoteId, PriceItem priceItem, String quotePriceType, List<LedgerEntry> ledgerItems, Date billToDate,
            Product product, ProductOrder productOrder) {

        this.quoteId = quoteId;
        this.priceItem = priceItem;
        this.quotePriceType = quotePriceType;
        this.ledgerItems = ledgerItems;
        this.billToDate = billToDate;
        this.product = product;
        this.productOrder = productOrder;

        if (ledgerItems != null) {
            for (LedgerEntry ledger : ledgerItems) {
                updateDateRange(ledger.getWorkCompleteDate());
                if (StringUtils.isNotBlank(ledger.getWorkItem())) {
                    workItems.add(ledger.getWorkItem());
                    if (tabularIdentifier == null) {
                        tabularIdentifier = ledger.getWorkItem();
                    }
                }
                if (StringUtils.isNotBlank(ledger.getSapDeliveryDocumentId())) {
                    sapItems = ledger.getSapDeliveryDocumentId();
                    if (tabularIdentifier == null) {
                        tabularIdentifier = sapItems;
                    }
                } else {
                    if (!StringUtils.equals(ledger.getSapDeliveryDocumentId(), sapItems)) {
                        throw new RuntimeException("Mis Matched SAPDelivery Document Found");
                    }
                }
            }
        }
    }

    public Collection<String> getWorkItems() {
        return Collections.unmodifiableCollection(workItems);
    }

    public Collection<LedgerEntry> getBillingCredits(){
        return ledgerItems.stream().filter(ledgerEntry -> ledgerEntry.getQuantity().compareTo(BigDecimal.ZERO) < 0).collect(Collectors.toSet());
    }

    public String getSapItems() {
        return sapItems;
    }


    public String getChargedAmountForPdo(@Nonnull String pdoBusinessKey) {
        BigDecimal quantity = BigDecimal.ZERO;
        for (LedgerEntry ledgerItem : ledgerItems) {
            if (pdoBusinessKey.equals(ledgerItem.getProductOrderSample().getProductOrder().getBusinessKey())) {
                quantity = quantity.add(ledgerItem.getQuantity());
            }
        }
        return new DecimalFormat(PDO_QUANTITY_FORMAT).format(quantity);
    }

    public int getNumberOfSamples(@Nonnull String pdoBusinessKey) {
        Set<String> sampleNames = new HashSet<>();
        for (LedgerEntry ledgerItem : ledgerItems) {
            if (pdoBusinessKey.equals(ledgerItem.getProductOrderSample().getProductOrder().getBusinessKey())) {
                sampleNames.add(ledgerItem.getProductOrderSample().getSampleKey());
            }
        }
        return sampleNames.size();
    }

    private void updateDateRange(Date completedDate) {
        if (startRange == null) {
            startRange = completedDate;
            endRange = completedDate;
            return;
        }

        if (completedDate.before(startRange)) {
            startRange = completedDate;
        }

        if (completedDate.after(endRange)) {
            endRange = completedDate;
        }
    }

    public String getQuoteId() {
        return quoteId;
    }

    public PriceItem getPriceItem() {
        return priceItem;
    }

    public BigDecimal getQuantity() {
        BigDecimal quantity = BigDecimal.ZERO;
        for (LedgerEntry ledgerItem : ledgerItems) {
            quantity = quantity.add(ledgerItem.getQuantity());
        }
        return quantity;
    }

    public BigDecimal getQuantityForSAP() {
        BigDecimal quantity = BigDecimal.ZERO;
        for (LedgerEntry ledgerItem : ledgerItems) {
            if (StringUtils.isBlank(ledgerItem.getSapDeliveryDocumentId())) {
                quantity = quantity.add(ledgerItem.getQuantity());
            }
        }
        return quantity;
    }

    public String getRoundedQuantity() {
        return new DecimalFormat(PDO_QUANTITY_FORMAT).format(getQuantity());
    }

    public Date getWorkCompleteDate() {
        return billToDate;
    }

    public String getBillingMessage() {
        // Since the quote message will apply to all items, just pull the message off the first item.
        return ledgerItems.get(0).getBillingMessage();
    }

    public void setBillingMessages(String billedMessage) {
        for (LedgerEntry ledgerItem : ledgerItems) {
            ledgerItem.setBillingMessage(billedMessage);
        }
    }

    public Date getStartRange() {
        return startRange;
    }

    public Date getEndRange() {
        return endRange;
    }


    public String getNumSamples() {
        return MessageFormat.format("{0} Sample{0, choice, 0#s|1#|1<s}", ledgerItems.size());
    }

    /**
     * This method should be invoked upon successful billing to update ledger entries with the quote to which they were
     * billed and the work item.
     *
     * @param itemIsReplacing           The item that is replacing the primary price item.
     * @param billingMessage            The message to be assigned to all entries.
     * @param quoteServerWorkItem       the id of the transaction in the quote server
     * @param replacementPriceItemNames names of price items that are valid replacements for {@link #priceItem}
     * @param sapDeliveryId
     */
    public void updateLedgerEntries(QuotePriceItem itemIsReplacing, String billingMessage, String quoteServerWorkItem,
                                    Collection<String> replacementPriceItemNames, String sapDeliveryId) {

        LedgerEntry.PriceItemType priceItemType = getPriceItemType(itemIsReplacing, replacementPriceItemNames);

        for (LedgerEntry ledgerEntry : ledgerItems) {
            ledgerEntry.setQuoteId(quoteId);
            ledgerEntry.setPriceItemType(priceItemType);
            ledgerEntry.setBillingMessage(billingMessage);
            ledgerEntry.setWorkItem(quoteServerWorkItem);
            if (StringUtils.isNotBlank(sapDeliveryId)) {
                ledgerEntry.setSapDeliveryDocumentId(sapDeliveryId);
            }
        }
    }

    /**
     * This method should be invoked upon successful billing to update ledger entries with the quote to which they were
     * billed and the work item.
     *
     * @param billingMessage            The message to be assigned to all entries.
     * @param quoteServerWorkItem       the id of the transaction in the quote server
     * @param sapDeliveryId
     */
    public void updateSapLedgerEntries(String billingMessage, String quoteServerWorkItem, String sapDeliveryId) {

        for (LedgerEntry ledgerEntry : ledgerItems) {
            ledgerEntry.setQuoteId(quoteId);
            ledgerEntry.setPriceItemType(LedgerEntry.PriceItemType.PRIMARY_PRICE_ITEM);
            ledgerEntry.setBillingMessage(billingMessage);
            ledgerEntry.setWorkItem(quoteServerWorkItem);
            if (StringUtils.isNotBlank(sapDeliveryId)) {
                ledgerEntry.setSapDeliveryDocumentId(sapDeliveryId);
            }
        }
    }

    /**
     * @return There should always be ledger entries and if not, it will throw an exception, which should be OK. This
     * just returns the first items sample because all items are grouped at a fine level by price item which means the
     * same product because price items are product based.
     */
    public Product getPrimaryProduct() {
        return ledgerItems.get(0).getProductOrderSample().getProductOrder().getProduct();
    }

    /**
     * While there is a shared primary product across all ledger entries, the price items these ledger entries
     * represent may not equate to the primary product.  So as not to confuse the primary product and the potential
     * addon product that may be billed, adding the visibility of the product being utilized
     *
     * @return
     */
    public Product getProduct() {
        return product;
    }

    public ProductOrder getProductOrder() {
        return productOrder;
    }

    public Collection<LedgerEntry> getLedgerItems() {
        return ledgerItems;
    }

    /**
     * Calculate if this item's price item is a replacement price item on this product. It returns a quote price item
     * object that is the primary.
     *
     * @param priceListCache The cache of the price list.
     *
     * @return null if this is not a replacement item or the primary price item if it is one.
     */
    public QuotePriceItem getPrimaryForReplacement(PriceList priceListCache) {
        PriceItem derivedPriceItem = getPrimaryProduct().getPrimaryPriceItem();

        // If this is optional, then return the primary as the 'is replacing.' This is comparing the quote price item
        // to the values on the product's price item, so do the item by item compare.
        for (QuotePriceItem optional : priceListCache.getReplacementPriceItems(derivedPriceItem)) {
            if (optional.isMercuryPriceItemEqual(priceItem)) {
                final QuotePriceItem priceItem = QuotePriceItem.convertMercuryPriceItem(derivedPriceItem);
                priceItem.setPrice(priceListCache.findByKeyFields(derivedPriceItem).getPrice());
                return priceItem;
            }
        }

        return null;
    }

    public LedgerEntry.PriceItemType getPriceItemType(QuotePriceItem itemIsReplacing,
                                                      Collection<String> replacementPriceItemNames) {
        LedgerEntry.PriceItemType type;

        if (itemIsReplacing != null) {
            type = LedgerEntry.PriceItemType.REPLACEMENT_PRICE_ITEM;
        } else {
            PriceItem priceItem = getPrimaryProduct().getPrimaryPriceItem();
            if (priceItem.getName().equals(getPriceItem().getName())
                || replacementPriceItemNames.contains(getPriceItem().getName())) {
                type = LedgerEntry.PriceItemType.PRIMARY_PRICE_ITEM;
            } else {
                // If it is not the primary or replacement right now, it has to be considered add on.
                type = LedgerEntry.PriceItemType.ADD_ON_PRICE_ITEM;
            }
        }

        return type;
    }

    public String getQuotePriceType() {
        return quotePriceType;
    }

    public void setQuotePriceType(String quotePriceType) {
        this.quotePriceType = quotePriceType;
    }

    /**
     * @return a list of keys of all PDOs that are affected by this collection of ledger items.
     */
    public Collection<String> getOrderKeys() {
        Set<String> keys = new HashSet<>();
        for (LedgerEntry entry : ledgerItems) {
            keys.add(entry.getProductOrderSample().getProductOrder().getJiraTicketKey());
        }
        return keys;
    }

    /**
     * If there is a single work item, it is returned.  Otherwise,
     * null is returned.
     */
    public String getSingleWorkItem() {
        String workItem = null;
        if (workItems.size() == 1) {
            workItem = workItems.iterator().next();
        }
        return workItem;
    }

    public PriceList getPriceOnWorkDate() {
        return priceOnWorkDate;
    }

    public void setPriceOnWorkDate(PriceList priceOnWorkDate) throws QuoteServerException {
        this.priceOnWorkDate = priceOnWorkDate;
        updatePriceOnQuoteImportItem();
    }

    public Quote getQuote() {
        return quote;
    }

    public void setQuote(Quote quote) {
        this.quote = quote;
    }

    void updatePriceOnQuoteImportItem() throws QuoteServerException {

        PriceList priceItemsForDate = getPriceOnWorkDate();

        final QuotePriceItem replacedProductQuotePriceItem = priceItemsForDate.findByKeyFields(priceItem);

        if (replacedProductQuotePriceItem == null || replacedProductQuotePriceItem.getPrice() == null) {
            throw new QuoteServerException(
                    "The price was not found for price item " + priceItem.getDisplayName());
        }

        getPriceItem().setPrice(replacedProductQuotePriceItem.getPrice());

    }

    public String getEffectivePrice() throws InvalidProductException {
        return this.priceOnWorkDate.getEffectivePrice(this.priceItem, this.quote, this.getWorkCompleteDate());
    }

    public boolean needsCustomization() {
        return productOrder.needsCustomization(product);
    }

    public boolean isBillingCredit() {
        return getQuantity().compareTo(BigDecimal.ZERO) < 0;
    }

    public SapQuote getSapQuote() {
        return sapQuote;
    }

    public void setSapQuote(SapQuote sapQuote) {
        this.sapQuote = sapQuote;
    }

    public String getTabularIdentifier() {
        return tabularIdentifier;
    }

    public boolean isSapOrder() { return productOrder.hasSapQuote();}

    public boolean isQuoteServerOrder() {
        return productOrder.hasQuoteServerQuote();
    }

    public DeliveryCondition getSapReplacementCondition() {
        DeliveryCondition replacementResult = null;
        Optional<String> reduce = Optional.empty();

        if(productOrder.hasSapQuote()) {
            replacementResult = null;
            for(LedgerEntry entry:ledgerItems) {
                if(StringUtils.isNotBlank(entry.getSapReplacement())) {
                    // The aggregation that creates the Quote Import Items includes SAP Replacement so all ledger
                    // entries in the Quote Inport Item should have the same sap Replacement.  Therefore, we only need
                    // to grab the first one that we find.
                    replacementResult = DeliveryCondition.fromConditionName(entry.getSapReplacement());
                    break;
                }
            }
        }

        return replacementResult;
    }
}
