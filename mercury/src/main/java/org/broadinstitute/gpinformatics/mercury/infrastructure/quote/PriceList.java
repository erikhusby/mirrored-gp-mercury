package org.broadinstitute.gpinformatics.mercury.infrastructure.quote;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collection;

@XmlRootElement(name = "PriceList")
public class PriceList {
    
    @XmlElement(name = "PriceItem")
    private Collection<PriceItem> prices = new ArrayList<PriceItem>();
    
    public PriceList() {}
    
    public Collection<PriceItem> getPriceList() {
        return prices;
    }
    
    public void add(PriceItem priceItem) {
        if (priceItem == null) {
             throw new NullPointerException("priceItem cannot be null."); 
        }
        prices.add(priceItem);
    }
}
