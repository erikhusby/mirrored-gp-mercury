package org.broadinstitute.gpinformatics.athena.infrastructure.quote;

import java.util.Collection;
import java.util.HashSet;

public class PriceListCache {
    
    private PriceList prices;
    
    public PriceListCache(PriceList priceList) {
        if (priceList == null) {
             throw new NullPointerException("priceList cannot be null."); 
        }
        this.prices = priceList;
    }
    
    public Collection<PriceItem> getPlatformPriceList(String platformName) {
        Collection<PriceItem> priceItems = new HashSet<PriceItem>();
        for (PriceItem priceItem : prices.getPriceList()) {
            if (priceItem.getPlatform().equalsIgnoreCase(platformName) ) {
                priceItems.add(priceItem);
            }
        }
        return priceItems;
    }
}
