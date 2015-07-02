package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntryTest;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataSourceResolver;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleData;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;


@Test(groups = TestGroups.DATABASE_FREE)
public class ProductOrderSampleTest {

    public static ProductOrderSample createBilledSample(String name, LedgerEntry.PriceItemType priceItemType) {
        ProductOrderSample sample = new ProductOrderSample(name);
        LedgerEntry billedEntry = LedgerEntryTest.createBilledLedgerEntry(sample, priceItemType);
        sample.getLedgerItems().add(billedEntry);
        return sample;
    }

    public static ProductOrderSample createBilledSample(String name) {
        return createBilledSample(name, LedgerEntry.PriceItemType.PRIMARY_PRICE_ITEM);
    }

    public static ProductOrderSample createUnbilledSampleWithLedger(String name) {
        ProductOrderSample sample = new ProductOrderSample(name);
        LedgerEntry billedEntry = LedgerEntryTest.createOneLedgerEntry(sample, "price item", 1, new Date());
        sample.getLedgerItems().add(billedEntry);
        return sample;
    }

    @DataProvider(name = "testIsBilled")
    public Object[][] createIsBilledData() {
        return new Object[][]{
                {createBilledSample("ABC"), true},
                {createBilledSample("ABC", LedgerEntry.PriceItemType.ADD_ON_PRICE_ITEM), false},
                {createUnbilledSampleWithLedger("ABC"), false},
                {new ProductOrderSample("ABC"), false}
        };
    }

    @Test(dataProvider = "testIsBilled")
    public void testIsBilled(ProductOrderSample sample, boolean isBilled) {
        Assert.assertEquals(sample.isCompletelyBilled(), isBilled);
    }

    static final org.broadinstitute.bsp.client.sample.MaterialType BSP_MATERIAL_TYPE =
            new org.broadinstitute.bsp.client.sample.MaterialType("Cells", "Red Blood Cells");

    static class TestPDOData {
        final Product product;
        final Product addOn;
        final ProductOrderSample sample1;
        final ProductOrderSample sample2;

        public TestPDOData(String quoteId) {
            ProductOrder order = ProductOrderTestFactory.createDummyProductOrder();

            order.setQuoteId(quoteId);

            product = order.getProduct();
            addOn = ProductTestFactory.createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, "partNumber");
            addOn.setPrimaryPriceItem(new PriceItem("A", "B", "C", "D"));
            product.addAddOn(addOn);

            sample1 = new ProductOrderSample("Sample1");
            sample2 = new ProductOrderSample("Sample2");

            List<ProductOrderSample> samples = new ArrayList<>();
            samples.add(sample1);
            samples.add(sample2);
            order.setSamples(samples);
        }
    }

    @DataProvider(name = "autoBillSample")
    public static Object[][] makeAutoBillSampleData() {
        TestPDOData data = new TestPDOData("GSP-123");
        Date completedDate = new Date();
        LedgerEntry ledgerEntry = new LedgerEntry(data.sample1, data.product.getPrimaryPriceItem(), completedDate, 1);

        // Bill sample2.
        data.sample2.addLedgerItem(completedDate, data.product.getPrimaryPriceItem(), 1);
        LedgerEntry ledger = data.sample2.getLedgerItems().iterator().next();
        ledger.setBillingMessage(BillingSession.SUCCESS);
        ledger.setBillingSession(new BillingSession(0L, Collections.singleton(ledger)));

        return new Object[][]{
                // Create ledger items from a single sample.
                new Object[]{data.sample1, completedDate, Collections.singleton(ledgerEntry)},
                // Update existing ledger items with "new" bill count.
                new Object[]{data.sample1, completedDate, Collections.singleton(ledgerEntry)},
                // If sample is already billed, don't create any ledger items.
                new Object[]{data.sample2, completedDate, Collections.emptySet()}
        };
    }

    @Test(dataProvider = "autoBillSample")
    public void testAutoBillSample(ProductOrderSample sample, Date completedDate, Set<LedgerEntry> ledgerEntries) {
        sample.autoBillSample(completedDate, 1);
        assertThat(sample.getBillableLedgerItems(), is(ledgerEntries));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void test_getMetadataSource_throws_exception_when_not_yet_set() {
        ProductOrderSample sample = new ProductOrderSample("ABC");
        sample.getMetadataSource();
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void test_setMetadataSource_to_null_value() {
        ProductOrderSample sample = new ProductOrderSample("ABC");
        sample.setMetadataSource(null);
        sample.getMetadataSource();
    }

    public void test_setMetadataSource_to_nonnull_value() {
        ProductOrderSample sample = new ProductOrderSample("ABC");
        MercurySample.MetadataSource metadataSource = MercurySample.MetadataSource.MERCURY;
        sample.setMetadataSource(metadataSource);
        assertThat(sample.getMetadataSource(), equalTo(metadataSource));
    }


    public void testBspSampleIsReceived() {

        ProductOrderSample sample = new ProductOrderSample("ABC");

        MercurySample.MetadataSource metadataSource = MercurySample.MetadataSource.BSP;

        sample.setMetadataSource(metadataSource);

        SampleData bspTestData = new BspSampleData(new HashMap<BSPSampleSearchColumn, String>() {{
            put(BSPSampleSearchColumn.RECEIPT_DATE,
                    new SimpleDateFormat(BspSampleData.BSP_DATE_FORMAT_STRING).format(new Date()));
            put(BSPSampleSearchColumn.GENDER, "M");
        }});
        sample.setSampleData(bspTestData);

        assertThat(sample.isSampleAvailable(), equalTo(true));
    }

    public void testBspSampleIsNotReceived() {

        ProductOrderSample sample = new ProductOrderSample("ABC");

        MercurySample.MetadataSource metadataSource = MercurySample.MetadataSource.BSP;

        sample.setMetadataSource(metadataSource);

        SampleData bspTestData = new BspSampleData(new HashMap<BSPSampleSearchColumn, String>() {{
            put(BSPSampleSearchColumn.GENDER, "M");
        }});
        sample.setSampleData(bspTestData);

        assertThat(sample.isSampleAvailable(), equalTo(false));
    }

    public void testMercurySampleIsReceivedAndAccessioned() throws IOException {
        ProductOrderSample sample = new ProductOrderSample("ABC");

        MercurySample.MetadataSource metadataSource = MercurySample.MetadataSource.MERCURY;
        MercurySample testSample = new MercurySample(sample.getSampleKey(), Collections.<Metadata>emptySet());
        BarcodedTube barcodedTube = new BarcodedTube("VesselFor" + sample.getSampleKey(),
                BarcodedTube.BarcodedTubeType.MatrixTube);
        LabEvent collaboratorTransferEvent =
                new LabEvent(LabEventType.COLLABORATOR_TRANSFER, new Date(), "thisLocation", 0l, 0l, "testprogram");
        barcodedTube.addInPlaceEvent(collaboratorTransferEvent);
        barcodedTube.setReceiptEvent(new BSPUserList.QADudeUser("LU", 1L),new Date(), 1L);
        testSample.addLabVessel(barcodedTube);

        sample.setMetadataSource(metadataSource);
        sample.setMercurySample(testSample);
        sample.setSampleData(testSample.getSampleData());

        assertThat(sample.isSampleAvailable(), equalTo(true));
    }

    public void testMercurySampleIsNotReceivedOrAccessioned() {

        ProductOrderSample sample = new ProductOrderSample("ABC");

        MercurySample.MetadataSource metadataSource = MercurySample.MetadataSource.MERCURY;
        MercurySample testSample = new MercurySample(sample.getSampleKey(), Collections.<Metadata>emptySet());
        BarcodedTube barcodedTube = new BarcodedTube("VesselFor" + sample.getSampleKey(),
                BarcodedTube.BarcodedTubeType.MatrixTube);
        testSample.addLabVessel(barcodedTube);

        sample.setMetadataSource(metadataSource);
        sample.setMercurySample(testSample);
        sample.setSampleData(testSample.getSampleData());

        assertThat(sample.isSampleAvailable(), equalTo(false));
    }

    @DataProvider(name = "availableSampleConditions")
    public Object[][] availableSampleConditions(Method method) {

        List<Object[]> dataList = new ArrayList<>();
        /*
           Parameter 1 -- Metadata source

           Parameter 2 -- Whether or not Mercury sample is found on product

           Parameter 3 -- is the Sample Accessioned (only used for Mercury source)

           Parameter 4 -- is the Sample received

           Parameter 5 -- Is the sampledata set on the product order sample

           Parameter 6 --  expected Result
        */

        /*
           Source of the sample is determined to be Mercury
           A MercurySample entity is present in both the database and the ProductOrderSample in question
         */
        // Sample has been accessioned and received, sampleData is set on ProductOrderSample   --- Expect sample to be available
        dataList.add(new Object[]{MercurySample.MetadataSource.MERCURY, TargetMercurySample.PRODUCT_ORDER_SAMPLE_AND_DATABASE, true, true, true, true});
        // Sample has been accessioned and not received, sampleData is set on ProductOrderSample   --- Expect sample to be available
        dataList.add(new Object[]{MercurySample.MetadataSource.MERCURY, TargetMercurySample.PRODUCT_ORDER_SAMPLE_AND_DATABASE, true, false, true, false});
        // Sample has been accessioned and received, sampleData is not set on ProductOrderSample   --- Expect sample to be available
        dataList.add(new Object[]{MercurySample.MetadataSource.MERCURY, TargetMercurySample.PRODUCT_ORDER_SAMPLE_AND_DATABASE, true, true, false, true});
        // Sample has been accessioned and not received, sampleData is not set on ProductOrderSample   --- Expect sample to be available
        dataList.add(new Object[]{MercurySample.MetadataSource.MERCURY, TargetMercurySample.PRODUCT_ORDER_SAMPLE_AND_DATABASE, true, false, false, false});
        // Sample has neither been accessioned nor received, sampleData is set on ProductOrderSample   --- Expect sample to not be available
        dataList.add(new Object[]{MercurySample.MetadataSource.MERCURY, TargetMercurySample.PRODUCT_ORDER_SAMPLE_AND_DATABASE, false, false, true, false});
        // Sample has not been accessioned but it has been received, sampleData is set on ProductOrderSample   --- Expect sample to not be available
        dataList.add(new Object[]{MercurySample.MetadataSource.MERCURY, TargetMercurySample.PRODUCT_ORDER_SAMPLE_AND_DATABASE, false, true, true, false});
        // Sample has neither been accessioned nor received, sampleData is not set on ProductOrderSample   --- Expect sample to not be available
        dataList.add(new Object[]{MercurySample.MetadataSource.MERCURY, TargetMercurySample.PRODUCT_ORDER_SAMPLE_AND_DATABASE, false, false, false, false});
        // Sample has not been accessioned but it has been , sampleData is not set on ProductOrderSample   --- Expect sample to not be available
        dataList.add(new Object[]{MercurySample.MetadataSource.MERCURY, TargetMercurySample.PRODUCT_ORDER_SAMPLE_AND_DATABASE, false, true, false, false});

        /*
           A MercurySample entity is not present in either the database or the ProductOrderSample in question
         */
        //
        // Sample has been accessioned and received, sampleData is set on ProductOrderSample   --- Expect sample to not be available
        dataList.add(new Object[]{MercurySample.MetadataSource.MERCURY, TargetMercurySample.NEITHER, true, true, true, false});
        // Sample has been accessioned and not received, sampleData is set on ProductOrderSample   --- Expect sample to not be available
        dataList.add(new Object[]{MercurySample.MetadataSource.MERCURY, TargetMercurySample.NEITHER, true, false, true, false});
        // Sample has been accessioned and received, sampleData is not set on ProductOrderSample   --- Expect sample not to be available
        dataList.add(new Object[]{MercurySample.MetadataSource.MERCURY, TargetMercurySample.NEITHER, true, true, false, false});
        // Sample has been accessioned and not received, sampleData is not set on ProductOrderSample   --- Expect sample not to be available
        dataList.add(new Object[]{MercurySample.MetadataSource.MERCURY, TargetMercurySample.NEITHER, true, false, false, false});
        // Sample has neither been accessioned nor received, sampleData is set on ProductOrderSample   --- Expect sample to not be available
        dataList.add(new Object[]{MercurySample.MetadataSource.MERCURY, TargetMercurySample.NEITHER, false, false, true, false});
        // Sample has not been accessioned but it has been received, sampleData is set on ProductOrderSample   --- Expect sample to not be available
        dataList.add(new Object[]{MercurySample.MetadataSource.MERCURY, TargetMercurySample.NEITHER, false, true, true, false});
        // Sample has neither been accessioned nor received, sampleData is not set on ProductOrderSample   --- Expect sample to not be available
        dataList.add(new Object[]{MercurySample.MetadataSource.MERCURY, TargetMercurySample.NEITHER, false, false, false, false});
        // Sample has not been accessioned but it has been received, sampleData is not set on ProductOrderSample   --- Expect sample to not be available
        dataList.add(new Object[]{MercurySample.MetadataSource.MERCURY, TargetMercurySample.NEITHER, false, true, false, false});

        /*
           Source of the sample is determined to be BSP
           A MercurySample entity is present in both the database and the ProductOrderSample in question
         */
        // Sample has been received in BSP, sampleData is set on ProductOrderSample   --- Expect sample to be available
        dataList.add(new Object[]{MercurySample.MetadataSource.BSP, TargetMercurySample.PRODUCT_ORDER_SAMPLE_AND_DATABASE, true, true, true, true});
        // Sample has not been received in BSP, sampleData is set on ProductOrderSample   --- Expect sample to not be available
        dataList.add(new Object[]{MercurySample.MetadataSource.BSP, TargetMercurySample.PRODUCT_ORDER_SAMPLE_AND_DATABASE, false, false, true, false});
        // Sample has not been received in BSP, sampleData is not set on ProductOrderSample   --- Expect sample to not be available
        dataList.add(new Object[]{MercurySample.MetadataSource.BSP, TargetMercurySample.PRODUCT_ORDER_SAMPLE_AND_DATABASE, false, false, false, false});

        /*
           A MercurySample entity is not present in either the database or the ProductOrderSample in question
         */
        // Sample has been received in BSP, sampleData is set on ProductOrderSample   --- Expect sample to be available
        dataList.add(new Object[]{MercurySample.MetadataSource.BSP, TargetMercurySample.NEITHER, true, true, true, true});
        // Sample has not been received in BSP, sampleData is set on ProductOrderSample   --- Expect sample to not be available
        dataList.add(new Object[]{MercurySample.MetadataSource.BSP, TargetMercurySample.NEITHER, false, false, true, false});
        // Sample has not been received in BSP, sampleData is not set on ProductOrderSample   --- Expect sample to not be available
        dataList.add(new Object[]{MercurySample.MetadataSource.BSP, TargetMercurySample.NEITHER, false, false, false, false});


        /*
            Source of the sample is ambiguous.
            A MercurySample entity is not present in either the database or the ProductOrderSample in question
         */
        // Sample has not been received in BSP, sampleData is not set on ProductOrderSample   --- Expect sample to not be available
        dataList.add(new Object[]{null, TargetMercurySample.NEITHER, false, false, false, false});

        return dataList.toArray(new Object[dataList.size()][]);
    }

    @Test(groups = TestGroups.DATABASE_FREE, dataProvider = "availableSampleConditions")
    public void testIsSampleAvailable(MercurySample.MetadataSource source, TargetMercurySample setMercurySample,
                                      boolean isSampleAccessioned, boolean isSampleReceived, boolean isSampleDataSet,
                                      boolean expectedResult)
            throws IOException {
        String sampleId = "SM-2923";

        MercurySample sourceSample = null;
        ProductOrderSample productOrderSample = new ProductOrderSample(sampleId);

        SampleData receivedSampleData = new BspSampleData();
        if (source != null) {
            switch (source) {
            case BSP:
                if (isSampleDataSet) {
                    Map<BSPSampleSearchColumn, String> sampleDataMap = new HashMap<>();
                    sampleDataMap.put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, "C" + sampleId);
                    if (isSampleReceived) {
                        sampleDataMap.put(BSPSampleSearchColumn.RECEIPT_DATE,
                                new SimpleDateFormat(BspSampleData.BSP_DATE_FORMAT_STRING).format(new Date()));
                    }
                    receivedSampleData = new BspSampleData(sampleDataMap);
                }
                break;
            case MERCURY:
                if (isSampleDataSet) {
                    receivedSampleData =
                            new MercurySampleData(sampleId,
                                    Collections.singleton(new Metadata(Metadata.Key.BROAD_SAMPLE_ID, sampleId)));
                }
                break;
            }
        }

        productOrderSample.setSampleData(receivedSampleData);

        if(source != null) {
            sourceSample = new MercurySample(sampleId,source);
        }

        MercurySampleDao mockDao = Mockito.mock(MercurySampleDao.class);
        if(source != null) {
            if(setMercurySample == TargetMercurySample.PRODUCT_ORDER_SAMPLE_AND_DATABASE) {
                productOrderSample.setMercurySample(sourceSample);
            }
            if(setMercurySample != TargetMercurySample.NEITHER) {
                Map<String, MercurySample> mockResults = new HashMap<>();
                mockResults.put(sampleId, sourceSample);
                Mockito.when(mockDao.findMapIdToMercurySample(Mockito.anyCollectionOf(String.class))).thenReturn(
                        mockResults);
            }
        }

        if (sourceSample != null) {
            BarcodedTube barcodedTube = new BarcodedTube("VesselFor" + sourceSample.getSampleKey(),
                    BarcodedTube.BarcodedTubeType.MatrixTube);
            if (source == MercurySample.MetadataSource.MERCURY && isSampleAccessioned) {
                LabEvent collaboratorTransferEvent =
                        new LabEvent(LabEventType.COLLABORATOR_TRANSFER, new Date(), "thisLocation", 0l, 0l,
                                "testprogram");
                barcodedTube.addInPlaceEvent(collaboratorTransferEvent);
            }
            sourceSample.getLabVessel().add(barcodedTube);
            if (isSampleReceived) {
                barcodedTube.setReceiptEvent(new BSPUserList.QADudeUser("LU", 1L), new Date(), 1L);
            }
        }

        SampleDataSourceResolver resolver = new SampleDataSourceResolver(mockDao);
        resolver.populateSampleDataSources(Collections.singleton(productOrderSample));
        Assert.assertEquals(productOrderSample.isSampleAvailable(), expectedResult);
    }

    private enum TargetMercurySample {
        PRODUCT_ORDER_SAMPLE_AND_DATABASE, DATABASE, NEITHER;
    }
}
