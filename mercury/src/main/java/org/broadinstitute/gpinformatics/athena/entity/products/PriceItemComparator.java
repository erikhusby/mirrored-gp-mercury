package org.broadinstitute.gpinformatics.athena.entity.products;

import org.apache.commons.lang3.builder.CompareToBuilder;

import java.util.Comparator;

/**
 * Simple {@link Comparator} for {@link PriceItem}s, considers platform, category, name in that order
 */
public class PriceItemComparator implements Comparator<PriceItem> {
    @Override
    public int compare(PriceItem priceItem, PriceItem priceItem1) {
        CompareToBuilder builder = new CompareToBuilder();
        builder.append(priceItem.getPlatform(), priceItem1.getPlatform());
        builder.append(priceItem.getCategory(), priceItem1.getCategory());
        builder.append(priceItem.getName(), priceItem1.getName());

        return builder.build();
    }
}
