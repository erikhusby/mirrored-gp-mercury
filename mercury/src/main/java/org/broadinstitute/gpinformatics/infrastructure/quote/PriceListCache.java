package org.broadinstitute.gpinformatics.infrastructure.quote;

import clover.org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.products.InvalidProductException;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.jmx.AbstractCache;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;


/**
 * Application-wide cache of all price items, potentially filtering by platform.
 */
@ApplicationScoped
public class PriceListCache extends AbstractCache implements Serializable {
    private static final long serialVersionUID = 1843525203075284455L;

    private Collection<QuotePriceItem> quotePriceItems = new ArrayList<>();

    private QuoteService quoteService;

    private static final Log logger = LogFactory.getLog(PriceListCache.class);

    public PriceListCache() {}

    /**
     * Not sure we really need this constructor any more except there are tests using it
     *
     * @param quotePriceItems The price list to hold in the cache
     */
    public PriceListCache(@Nonnull Collection<QuotePriceItem> quotePriceItems) {
        this.quotePriceItems = quotePriceItems;
    }

    @Inject
    public PriceListCache(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @Override
    public synchronized void refreshCache() {
        try {
            PriceList rawPriceList = quoteService.getAllPriceItems();

            // Only replace if the new list is non-null.
            if (rawPriceList != null) {
                quotePriceItems = rawPriceList.getQuotePriceItems();
            }
        } catch (QuoteServerException | QuoteNotFoundException e) {
            logger.error("Could not refresh the price item list.", e);
        }
    }

    /**
     * Return the price item list
     *
     * @return the price items
     */
    public Collection<QuotePriceItem> getQuotePriceItems() {
        if (quotePriceItems.isEmpty()) {
            refreshCache();
        }
        return quotePriceItems;
    }

    /**
     * Filter {@link QuotePriceItem}s by known platforms
     *
     * @param quotePlatformType The type of platform for the quote server
     *
     * @return The matching price items
     */
    public Collection<QuotePriceItem> getPriceItemsByPlatform(QuotePlatformType quotePlatformType) {
        Collection<QuotePriceItem> items = new HashSet<>();
        for (QuotePriceItem quotePriceItem : getQuotePriceItems()) {
            if (quotePlatformType.getPlatformName().equalsIgnoreCase(quotePriceItem.getPlatformName())) {
                items.add(quotePriceItem);
            }
        }
        return items;
    }

    
    public Collection<QuotePriceItem> getGSPPriceItems() {
        return getPriceItemsByPlatform(QuotePlatformType.SEQ);
    }


    public List<QuotePriceItem> searchPriceItems(String query,
                                                 PriceGrouping priceListFilter) {
        List<QuotePriceItem> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        // Currently searching all price items, not filtering by platform or anything else
        for (QuotePriceItem quotePriceItem : getQuotePriceItems()) {

            switch (priceListFilter) {
            case SSF_Only:
                if(!quotePriceItem.getPriceListName().equals(QuoteServiceImpl.SSF_PRICE_LIST_NAME)) {
                    continue;
                }
                break;
            case External_Only:
                if(!quotePriceItem.getPriceListName().equals(QuoteServiceImpl.EXTERNAL_PRICE_LIST_NAME) &&
                   !quotePriceItem.getPriceListName().equals(QuoteServiceImpl.CRSP_PRICE_LIST_NAME)) {
                    continue;
                }
                break;
            }

            if (quotePriceItem != null &&
                ((quotePriceItem.getPlatformName() != null && quotePriceItem.getPlatformName().toLowerCase().contains(lowerQuery)) ||
                 (quotePriceItem.getCategoryName() != null && quotePriceItem.getCategoryName().toLowerCase().contains(lowerQuery)) ||
                 (quotePriceItem.getName() != null && quotePriceItem.getName().toLowerCase().contains(lowerQuery)))) {
                results.add(quotePriceItem);
            }
        }

        return results;
    }

    public boolean contains(QuotePriceItem quotePriceItem) {
        return getQuotePriceItems().contains(quotePriceItem);
    }

    /**
     * The input is search keys concatenated like platform|category|name
     *
     * @param concatenatedKey The three part key
     *
     * @return The matching price item
     */
    public QuotePriceItem findByConcatenatedKey(String concatenatedKey) {

        if (concatenatedKey == null) {
            throw new RuntimeException("Invalid search key: " + concatenatedKey);
        }

        for (QuotePriceItem quotePriceItem : getQuotePriceItems()) {
            String currentKey = PriceItem.makeConcatenatedKey(quotePriceItem.getPlatformName(),
                    quotePriceItem.getCategoryName(), quotePriceItem.getName());

            if (concatenatedKey.equals(currentKey)) {
                return quotePriceItem;
            }
        }

        return null;
    }

    public QuotePriceItem findById(long priceItemId) {
        String idString = String.valueOf(priceItemId);
        for (QuotePriceItem quotePriceItem : getQuotePriceItems()) {
            if (quotePriceItem.getId().equals(idString)) {
                return quotePriceItem;
            }
        }

        return null;
    }

    public QuotePriceItem findByKeyFields(String platform, String category, String name) {

        QuotePriceItem foundItem = null;

        for (QuotePriceItem quotePriceItem : getQuotePriceItems()) {
            if (StringUtils.equals(quotePriceItem.getPlatformName(),platform) &&
                StringUtils.equals(quotePriceItem.getCategoryName(),category) &&
                StringUtils.equals(quotePriceItem.getName(),name)) {
                foundItem = quotePriceItem;
            }
        }

        return foundItem;
    }

    public QuotePriceItem findByKeyFields(PriceItem priceItem) {
        return findByKeyFields(priceItem.getPlatform(), priceItem.getCategory(), priceItem.getName());
    }

    public Collection<QuotePriceItem> getReplacementPriceItems(Product product) {
        Collection<QuotePriceItem> replacementPriceItems = new ArrayList<>();
        if(product.getPrimaryPriceItem() != null) {
            replacementPriceItems.addAll(getReplacementPriceItems(product.getPrimaryPriceItem()));
        }
        return replacementPriceItems;
    }

    public Collection<QuotePriceItem> getReplacementPriceItems(PriceItem primaryPriceItem) {
        try {
            QuotePriceItem quotePriceItem = findByKeyFields(primaryPriceItem);

            return quotePriceItem.getReplacementItems().getQuotePriceItems();
        } catch (Throwable t) {
            // Since this is coming from the quote server, we will just show nothing when there are any errors.
            return Collections.emptyList();
        }

    }

    /**
     * Given a price item, this method will compare the price of the price item on the price list with the Quote
     * line item (if one exists) on a given quote to see which price is lower.  The lower of the two is returned
     *
     * @param primaryPriceItem Price item defined on a product
     * @param orderQuote       Quote associated with the product order from which the product that defined the
     *                         price item is associated
     * @return Lowest price between the pricelist item and the quote item (if one exists)
     * @throws InvalidProductException   Thrown if the price item from the product orders product is not found on the
     * price list
     */
    public String getEffectivePrice(PriceItem primaryPriceItem, Quote orderQuote) throws InvalidProductException {

        final QuotePriceItem cachedPriceItem = findByKeyFields(primaryPriceItem);
        if(cachedPriceItem == null) {
            throw new InvalidProductException("The price item "+primaryPriceItem.getDisplayName()+" does not exist");
        }
        return getEffectivePrice(cachedPriceItem, orderQuote);
    }

    public String getEffectivePrice(QuotePriceItem cachedPriceItem, Quote orderQuote) {
        String price = cachedPriceItem.getPrice();
        QuoteItem foundMatchingQuoteItem = null;
        if (orderQuote != null) {
            foundMatchingQuoteItem = orderQuote.findCachedQuoteItem(cachedPriceItem.getPlatformName(),
                    cachedPriceItem.getCategoryName(), cachedPriceItem.getName());
        }
        if (foundMatchingQuoteItem  != null && !orderQuote.getExpired()) {
            if (new BigDecimal(foundMatchingQuoteItem .getPrice()).compareTo(new BigDecimal(cachedPriceItem.getPrice())) < 0) {
                price = foundMatchingQuoteItem .getPrice();
            }
        }
        return price;
    }

    public enum PriceGrouping {
        SSF_Only, External_Only, ALL;
    }
}
