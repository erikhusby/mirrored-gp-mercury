package org.broadinstitute.gpinformatics.infrastructure.quote;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collection;

@XmlRootElement(name = "response")
public class PriceList {
    
    @XmlElement(name = "priceItem")
    private Collection<PriceItem> priceItems = new ArrayList<PriceItem>();
    
    public PriceList() {}
    
    public Collection<PriceItem> getPriceItems() {
        return priceItems;
    }
    
    public void add(PriceItem priceItem) {
        if (priceItem == null) {
             throw new NullPointerException("priceItem cannot be null."); 
        }
        priceItems.add(priceItem);
    }
}
