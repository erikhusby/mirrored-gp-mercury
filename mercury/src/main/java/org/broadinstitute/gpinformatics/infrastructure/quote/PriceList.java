package org.broadinstitute.gpinformatics.infrastructure.quote;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collection;

@XmlRootElement(name = "response")
public class PriceList {
    
    @XmlElement(name = "priceItem")
    private final Collection<QuotePriceItem> quotePriceItems = new ArrayList<QuotePriceItem>();

    public PriceList() {}
    
    public Collection<QuotePriceItem> getQuotePriceItems() {
        return quotePriceItems;
    }
    
    public void add(@Nonnull QuotePriceItem quotePriceItem) {
        quotePriceItems.add(quotePriceItem);
    }
}
