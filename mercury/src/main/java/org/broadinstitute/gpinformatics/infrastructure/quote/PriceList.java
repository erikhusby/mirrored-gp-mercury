package org.broadinstitute.gpinformatics.infrastructure.quote;

import org.apache.commons.lang3.time.DateUtils;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

@XmlRootElement(name = "response")
public class PriceList {
    
    @XmlElement(name = "priceItem")
    private final Collection<QuotePriceItem> quotePriceItems = new ArrayList<>();

    public PriceList() {}
    
    public Collection<QuotePriceItem> getQuotePriceItems() {
        return quotePriceItems;
    }
    
    public void add(@Nonnull QuotePriceItem quotePriceItem) {
        quotePriceItems.add(quotePriceItem);
    }

    public QuotePriceItem findByKeyFields(String platform, String category, String name) {
        return findByKeyFields(platform, category, name, null);
    }
    public QuotePriceItem findByKeyFields(String platform, String category, String name, Date effectiveDate) {

        QuotePriceItem foundItem = null;

        for (QuotePriceItem quotePriceItem : getQuotePriceItems()) {
            if (quotePriceItem.getPlatformName().equals(platform) &&
                quotePriceItem.getCategoryName().equals(category) &&
                quotePriceItem.getName().equals(name)) {
                if(effectiveDate == null ||
                   (DateUtils.truncate(effectiveDate, Calendar.DATE).equals(quotePriceItem.getEffectiveDate()))) {
                    foundItem = quotePriceItem;
                }
            }
        }

        return foundItem;
    }

}
