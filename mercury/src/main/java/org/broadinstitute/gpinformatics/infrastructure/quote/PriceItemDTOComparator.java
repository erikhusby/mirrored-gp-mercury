package org.broadinstitute.gpinformatics.infrastructure.quote;

import org.apache.commons.lang3.builder.CompareToBuilder;

import java.util.Comparator;

public class PriceItemDTOComparator implements Comparator<QuotePriceItem> {

    @Override
    public int compare(QuotePriceItem quotePriceItem, QuotePriceItem quotePriceItem1) {
        CompareToBuilder builder = new CompareToBuilder();
        builder.append(quotePriceItem.getPlatformName(), quotePriceItem1.getPlatformName());
        builder.append(quotePriceItem.getCategoryName(), quotePriceItem1.getCategoryName());
        builder.append(quotePriceItem.getName(), quotePriceItem1.getName());

        return builder.build();
    }
}
