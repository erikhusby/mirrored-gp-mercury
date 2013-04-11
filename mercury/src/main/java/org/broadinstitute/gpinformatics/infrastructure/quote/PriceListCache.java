package org.broadinstitute.gpinformatics.infrastructure.quote;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.jmx.AbstractCache;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;


/**
 * Application-wide cache of all price items, potentially filtering by platform.
 */
@ApplicationScoped
public class PriceListCache extends AbstractCache implements Serializable {
    private static final long serialVersionUID = 1843525203075284455L;
    public static final PriceListCache DUMMY_CACHE = new PriceListCache(new ArrayList<PriceItem> ());

    private Collection<PriceItem> priceItems = new ArrayList<PriceItem>();

    private PMBQuoteService quoteService;

    @Inject
    private Deployment deployment;

    private static final Log logger = LogFactory.getLog(PriceListCache.class);

    public PriceListCache() {}

    /**
     * Not sure we really need this constructor any more except there are tests using it
     *
     * @param priceItems The price list to hold in the cache
     */
    public PriceListCache(@Nonnull Collection<PriceItem> priceItems) {
        if (priceItems == null) {
             throw new NullPointerException("priceList cannot be null.");
        }

        this.priceItems = priceItems;
    }

    @Inject
    public PriceListCache(PMBQuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @Override
    public synchronized void refreshCache() {
        try {
            PriceList rawPriceList = quoteService.getAllPriceItems();

            // Only replace if the new list is non-null.
            if (rawPriceList != null) {
                priceItems = rawPriceList.getPriceItems();
            }

        } catch (Exception ex) {
            logger.error("Could not refresh the price item list", ex);
        }
    }

    /**
     * Return the price item list
     *
     * @return the price items
     */
    public Collection<PriceItem> getPriceItems() {
        if (priceItems.isEmpty()) {
            refreshCache();
        }
        return priceItems;
    }

    /**
     * Filter {@link PriceItem}s by known platforms
     *
     * @param quotePlatformType The type of platform for the quote server
     *
     * @return The matching price items
     */
    public Collection<PriceItem> getPriceItemsByPlatform(QuotePlatformType quotePlatformType) {
        Collection<PriceItem> items = new HashSet<PriceItem>();
        for (PriceItem priceItem : getPriceItems()) {
            if (quotePlatformType.getPlatformName().equalsIgnoreCase(priceItem.getPlatformName())) {
                items.add(priceItem);
            }
        }
        return items;
    }

    
    public Collection<PriceItem> getGSPPriceItems() {
        return getPriceItemsByPlatform(QuotePlatformType.SEQ);
    }


    public List<PriceItem> searchPriceItems(String query) {
        List<PriceItem> results = new ArrayList<PriceItem>();
        String lowerQuery = query.toLowerCase();

        // Currently searching all price items, not filtering by platform or anything else
        for (PriceItem priceItem : getPriceItems()) {
            if (priceItem != null &&
                ((priceItem.getPlatformName() != null && priceItem.getPlatformName().toLowerCase().contains(lowerQuery)) ||
                 (priceItem.getCategoryName() != null && priceItem.getCategoryName().toLowerCase().contains(lowerQuery)) ||
                 (priceItem.getName() != null && priceItem.getName().toLowerCase().contains(lowerQuery)))) {
                results.add(priceItem);
            }
        }

        return results;
    }

    public boolean contains(PriceItem priceItem) {
        return getPriceItems().contains(priceItem);
    }

    /**
     * The input is search keys concatenated like platform|category|name
     *
     * @param concatenatedKey The three part key
     *
     * @return The matching price item
     */
    public PriceItem findByConcatenatedKey(String concatenatedKey) {

        if (concatenatedKey == null) {
            throw new RuntimeException("Invalid search key: " + concatenatedKey);
        }

        for (PriceItem priceItem : getPriceItems()) {
            String currentKey =
                org.broadinstitute.gpinformatics.athena.entity.products.PriceItem.makeConcatenatedKey(
                        priceItem.getPlatformName(), priceItem.getCategoryName(),priceItem.getName());

            if (concatenatedKey.equals(currentKey)) {
                return priceItem;
            }
        }

        return null;
    }

    public PriceItem findById(long priceItemId) {
        String idString = String.valueOf(priceItemId);
        for (PriceItem priceItem : getPriceItems()) {
            if (priceItem.getId().equals(idString)) {
                return priceItem;
            }
        }

        return null;
    }

    public String getPriceItemName(long priceItemId) {
        PriceItem item = findById(priceItemId);
        if (item != null) {
            return item.getName();
        }

        return "invalid price item id " + priceItemId;
    }

    public PriceItem findByKeyFields(String platform, String category, String name) {
        for (PriceItem priceItem : getPriceItems()) {
            if (priceItem.getPlatformName().equals(platform) &&
                priceItem.getCategoryName().equals(category) &&
                priceItem.getName().equals(name)) {
                return priceItem;
            }
        }

        return null;
    }
}
