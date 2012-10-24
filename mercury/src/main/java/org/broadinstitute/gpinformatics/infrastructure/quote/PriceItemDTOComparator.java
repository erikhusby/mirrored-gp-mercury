package org.broadinstitute.gpinformatics.infrastructure.quote;

import org.apache.commons.lang3.builder.CompareToBuilder;

import java.util.Comparator;

public class PriceItemDTOComparator implements Comparator<PriceItem> {

    @Override
    public int compare(PriceItem priceItem, PriceItem priceItem1) {
        CompareToBuilder builder = new CompareToBuilder();
        builder.append(priceItem.getPlatformName(), priceItem1.getPlatformName());
        builder.append(priceItem.getCategoryName(), priceItem1.getCategoryName());
        builder.append(priceItem.getName(), priceItem1.getName());

        return builder.build();
    }
}
