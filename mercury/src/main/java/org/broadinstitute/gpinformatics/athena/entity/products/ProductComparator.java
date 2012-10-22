package org.broadinstitute.gpinformatics.athena.entity.products;

import org.apache.commons.lang3.builder.CompareToBuilder;

import java.util.Comparator;

/**
 * Simple {@link Comparator} for {@link Product}s, uses only the part number
 */
public class ProductComparator implements Comparator<Product> {

    @Override
    public int compare(Product product, Product product1) {
        CompareToBuilder builder = new CompareToBuilder();
        builder.append(product.getPartNumber(), product1.getPartNumber());
        return builder.build();
    }
}
