package org.broadinstitute.sequel.control.quote;

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
    
    public Collection<PriceItem> getGSPPriceList() {
        Collection<PriceItem> priceItems = new HashSet<PriceItem>();
        for (PriceItem priceItem : prices.getPriceList()) {
            if (PriceItem.GSP_PLATFORM_NAME.equalsIgnoreCase(priceItem.getPlatform())) {
                priceItems.add(priceItem);
            }
        }
        return priceItems;
    }
}
