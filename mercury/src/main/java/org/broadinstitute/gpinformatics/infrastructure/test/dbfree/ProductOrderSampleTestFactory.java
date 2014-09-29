package org.broadinstitute.gpinformatics.infrastructure.test.dbfree;

import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class ProductOrderSampleTestFactory {

    private ProductOrderSampleTestFactory() {}

    public static List<ProductOrderSample> createSampleList(String... sampleList) {
        return createSampleList(sampleList, new HashSet<LedgerEntry>(), false);
    }


    public static List<ProductOrderSample> createDBFreeSampleList(String... sampleList) {
        return createSampleList(sampleList, new HashSet<LedgerEntry>(), true);
    }


    public static List<ProductOrderSample> createSampleList(String[] sampleArray,
                                                            Collection<LedgerEntry> billableItems,
                                                            boolean dbFree) {
        List<ProductOrderSample> productOrderSamples = new ArrayList<>(sampleArray.length);
        for (String sampleName : sampleArray) {
            ProductOrderSample productOrderSample;
            if (dbFree) {
                productOrderSample = new ProductOrderSample(sampleName, new BspSampleData());
            } else {
                productOrderSample = new ProductOrderSample(sampleName);
            }
            productOrderSample.setSampleComment("athenaComment");
            productOrderSample.getLedgerItems().addAll(billableItems);
            productOrderSamples.add(productOrderSample);
        }
        return productOrderSamples;
    }
}
