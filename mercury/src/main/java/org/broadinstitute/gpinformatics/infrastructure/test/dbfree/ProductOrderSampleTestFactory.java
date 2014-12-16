package org.broadinstitute.gpinformatics.infrastructure.test.dbfree;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class ProductOrderSampleTestFactory {

    private ProductOrderSampleTestFactory() {}

    public static List<ProductOrderSample> createSampleList(String... sampleList) {
        return createSampleList(sampleList, new HashSet<LedgerEntry>(), MercurySample.MetadataSource.BSP, false);
    }


    public static List<ProductOrderSample> createDBFreeSampleList(MercurySample.MetadataSource metadataSource,
                                                                  String... sampleList) {
        return createSampleList(sampleList, new HashSet<LedgerEntry>(), metadataSource, true);
    }

    public static List<ProductOrderSample> createDBFreeSampleList(String... sampleList) {
        return createSampleList(sampleList, new HashSet<LedgerEntry>(), MercurySample.MetadataSource.BSP, true);
    }


    public static List<ProductOrderSample> createSampleList(String[] sampleArray,
                                                            Collection<LedgerEntry> billableItems,
                                                            MercurySample.MetadataSource metadataSource, boolean dbFree) {
        List<ProductOrderSample> productOrderSamples = new ArrayList<>(sampleArray.length);
        for (String sampleName : sampleArray) {
            ProductOrderSample productOrderSample;
            SampleData sampleData;
            if (dbFree) {
                if (metadataSource == MercurySample.MetadataSource.BSP) {
                    sampleData = new BspSampleData(ImmutableMap.of(BSPSampleSearchColumn.SAMPLE_ID, sampleName));
                } else {
                    sampleData = new MercurySampleData(sampleName,
                            ImmutableSet.of(new Metadata(Metadata.Key.SAMPLE_ID, sampleName)));
                }
                productOrderSample = new ProductOrderSample(sampleName, sampleData);
            } else {
                productOrderSample = new ProductOrderSample(sampleName);
            }
            MercurySample mercurySample = new MercurySample(sampleName, metadataSource);
            productOrderSample.setMercurySample(mercurySample);
            productOrderSample.setMetadataSource(metadataSource);
            productOrderSample.setSampleComment("athenaComment");
            productOrderSample.getLedgerItems().addAll(billableItems);
            productOrderSamples.add(productOrderSample);
        }
        return productOrderSamples;
    }
}
