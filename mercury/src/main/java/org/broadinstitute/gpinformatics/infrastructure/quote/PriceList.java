package org.broadinstitute.gpinformatics.infrastructure.quote;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collection;

@XmlRootElement(name = "response")
public class PriceList {
    
    @XmlElement(name = "priceItem")
    private final Collection<PriceItem> priceItems = new ArrayList<PriceItem>();

    public PriceList() {}
    
    public Collection<PriceItem> getPriceItems() {
        return priceItems;
    }
    
    public void add(@Nonnull PriceItem priceItem) {
        priceItems.add(priceItem);
    }
}
