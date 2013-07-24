package org.broadinstitute.gpinformatics.infrastructure.quote;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collection;

/**
 * This is the XML wrapper for the list of price items that make up a price items list of alternate charges.
 *
 * @author hrafal
 */
@XmlRootElement(name = "replacementItems")
public class ReplacementItems {

    @XmlElement(name = "priceItem")
    private final Collection<QuotePriceItem> quotePriceItems = new ArrayList<>();

    public ReplacementItems() {}

    public Collection<QuotePriceItem> getQuotePriceItems() {
        return quotePriceItems;
    }

    public void add(@Nonnull QuotePriceItem quotePriceItem) {
        quotePriceItems.add(quotePriceItem);
    }
}
