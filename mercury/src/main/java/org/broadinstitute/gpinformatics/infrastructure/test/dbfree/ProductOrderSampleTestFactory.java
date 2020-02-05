package org.broadinstitute.gpinformatics.infrastructure.test.dbfree;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleData;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

public class ProductOrderSampleTestFactory {

    private ProductOrderSampleTestFactory() {}

    public static List<ProductOrderSample> createSampleListWithMercurySamples(String... sampleList) {
        return createSampleList(sampleList, new HashSet<LedgerEntry>(), MercurySample.MetadataSource.BSP, false, true);
    }

    public static List<ProductOrderSample> createSampleList(String... sampleList) {
        return createSampleList(sampleList, new HashSet<LedgerEntry>(), MercurySample.MetadataSource.BSP, false, false);
    }

    public static List<ProductOrderSample> createDBFreeSampleList(MercurySample.MetadataSource metadataSource,
                                                                  String... sampleList) {
        return createSampleList(sampleList, new HashSet<LedgerEntry>(), metadataSource, true, true);
    }

    public static List<ProductOrderSample> createDBFreeSampleList(String... sampleList) {
        return createSampleList(sampleList, new HashSet<LedgerEntry>(), MercurySample.MetadataSource.BSP, true, true);
    }


    public static List<ProductOrderSample> createSampleList(String[] sampleArray,
                                                            Collection<LedgerEntry> billableItems,
                                                            MercurySample.MetadataSource metadataSource, boolean dbFree,
                                                            boolean createMercurySamples) {
        List<ProductOrderSample> productOrderSamples = new ArrayList<>(sampleArray.length);

        int counter = 1;

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
            if (createMercurySamples) {
                MercurySample mercurySample = new MercurySample(sampleName, metadataSource);
                LabVessel testVessel = new BarcodedTube(sampleName+"Tube", BarcodedTube.BarcodedTubeType.MatrixTube075);
                testVessel.setReceiptEvent(new BSPUserList.QADudeUser("LU", counter++),new Date(), 1L,
                        LabEvent.UI_EVENT_LOCATION);
                mercurySample.addLabVessel(testVessel);

                productOrderSample.setMercurySample(mercurySample);
            }
            productOrderSample.setMetadataSource(metadataSource);
            productOrderSample.setSampleComment("athenaComment");
            productOrderSample.getLedgerItems().addAll(billableItems);
            productOrderSamples.add(productOrderSample);
        }
        return productOrderSamples;
    }

    public static void markAsBilled(ProductOrderSample sampleToBeBilled) {

        final PriceItem priceItem = new PriceItem(sampleToBeBilled.getProductOrder().getQuoteId(), "platform", "category", "test");

        if(sampleToBeBilled.getProductOrder().hasSapQuote()) {
            sampleToBeBilled.addLedgerItem(new Date(), sampleToBeBilled.getProductOrder().getProduct(), BigDecimal.ONE,
                    false);
        } else {
            sampleToBeBilled.addLedgerItem(new Date(), priceItem, BigDecimal.ONE);
        }

        LedgerEntry toclose = sampleToBeBilled.getLedgerItems().iterator().next();
        toclose.setPriceItemType(LedgerEntry.PriceItemType.PRIMARY_PRICE_ITEM);
        toclose.setBillingMessage(BillingSession.SUCCESS);
        BillingSession closingSession = new BillingSession(ProductOrderTestFactory.TEST_CREATOR, Collections.singleton(toclose));
        closingSession.setBilledDate(new Date());

    }
}
