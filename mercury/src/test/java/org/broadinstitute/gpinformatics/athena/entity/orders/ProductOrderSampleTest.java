package org.broadinstitute.gpinformatics.athena.entity.orders;

import com.google.common.collect.ImmutableMap;
import freemarker.ext.beans.HashAdapter;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntryTest;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataSourceResolver;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
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

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;


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
            MaterialType materialType = new MaterialType(BSP_MATERIAL_TYPE.getCategory(), BSP_MATERIAL_TYPE.getName());
            addOn = ProductTestFactory.createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, "partNumber");
            addOn.addAllowableMaterialType(materialType);
            addOn.setPrimaryPriceItem(new PriceItem("A", "B", "C", "D"));
            product.addAddOn(addOn);

            Map<BSPSampleSearchColumn, String> dataMap =
                    new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
                        put(BSPSampleSearchColumn.MATERIAL_TYPE, BSP_MATERIAL_TYPE.getFullName());
                    }};
            sample1 = new ProductOrderSample("Sample1", new BspSampleData(dataMap));

            dataMap = new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
                put(BSPSampleSearchColumn.MATERIAL_TYPE, "XXX:XXX");
            }};
            sample2 = new ProductOrderSample("Sample2", new BspSampleData(dataMap));

            List<ProductOrderSample> samples = new ArrayList<>();
            samples.add(sample1);
            samples.add(sample2);
            order.setSamples(samples);
        }
    }

    @DataProvider(name = "getBillablePriceItems")
    public static Object[][] makeGetBillablePriceItemsData() {
        TestPDOData data = new TestPDOData("GSP-123");
        Product product = data.product;
        Product addOn = data.addOn;

        List<PriceItem> expectedItems = new ArrayList<>();
        expectedItems.add(product.getPrimaryPriceItem());
        expectedItems.add(addOn.getPrimaryPriceItem());

        return new Object[][]{
                new Object[]{data.sample1, expectedItems},
                new Object[]{data.sample2, Collections.singletonList(product.getPrimaryPriceItem())}
        };
    }

    @Test(dataProvider = "getBillablePriceItems")
    public void testGetBillablePriceItems(ProductOrderSample sample, List<PriceItem> priceItems) {
        List<PriceItem> generatedItems = sample.getBillablePriceItems();
        assertThat(generatedItems.size(), is(equalTo(priceItems.size())));

        generatedItems.removeAll(priceItems);
        assertThat(generatedItems, is(empty()));
    }

    @DataProvider(name = "autoBillSample")
    public static Object[][] makeAutoBillSampleData() {
        TestPDOData data = new TestPDOData("GSP-123");
        Date completedDate = new Date();
        Set<LedgerEntry> ledgers = new HashSet<>();
        ledgers.add(new LedgerEntry(data.sample1, data.product.getPrimaryPriceItem(), completedDate, 1));
        ledgers.add(new LedgerEntry(data.sample1, data.addOn.getPrimaryPriceItem(), completedDate, 1));

        data.sample2.addLedgerItem(completedDate, data.product.getPrimaryPriceItem(), 1);
        LedgerEntry ledger = data.sample2.getLedgerItems().iterator().next();
        ledger.setBillingMessage(BillingSession.SUCCESS);
        ledger.setBillingSession(new BillingSession(0L, Collections.singleton(ledger)));

        return new Object[][]{
                // Create ledger items from a single sample.
                new Object[]{data.sample1, completedDate, ledgers},
                // Update existing ledger items with "new" bill count.
                new Object[]{data.sample1, completedDate, ledgers},
                // If sample is already billed, don't create any ledger items.
                new Object[]{data.sample2, completedDate, Collections.emptySet()}
        };
    }

    @Test(dataProvider = "autoBillSample")
    public void testAutoBillSample(ProductOrderSample sample, Date completedDate, Set<LedgerEntry> ledgerEntries) {
        sample.autoBillSample(completedDate, 1);
        assertThat(sample.getBillableLedgerItems(), is(equalTo(ledgerEntries)));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void test_getMetadataSource_throws_exception_when_not_yet_set() {
        ProductOrderSample sample = new ProductOrderSample("ABC");
        sample.getMetadataSource();
    }

    /**
     * The two options for handling explicit setting to null are to treat it as having not been set or to treat it as a
     * valid value. Treating it as a valid value is currently done for 2 reasons:
     * <ol>
     * <li>There is not a {@link MercurySample.MetadataSource} value for "none" or "unknown"</li>
     * <li>Treating null as unset would expose an implementation detail of {@link ProductOrderSample}
     * unnecessarily</li>
     * </ol>
     * This may be revisited at a later time provided that the effect on callers that depend on metadataSource being set
     * is considered.
     */
    @Test(expectedExceptions = IllegalStateException.class)
    public void test_setMetadataSource_to_null_value() {
        ProductOrderSample sample = new ProductOrderSample("ABC");
        sample.setMetadataSource(null);
        sample.getMetadataSource();
//        assertThat(sample.getMetadataSource(), nullValue());
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

    public void testMercurySampleIsReceivedAndAccessioned() {
        ProductOrderSample sample = new ProductOrderSample("ABC");

        MercurySample.MetadataSource metadataSource = MercurySample.MetadataSource.MERCURY;
        MercurySample testSample = new MercurySample(sample.getSampleKey(), Collections.<Metadata>emptySet());
        BarcodedTube barcodedTube = new BarcodedTube("VesselFor" + sample.getSampleKey(),
                BarcodedTube.BarcodedTubeType.MatrixTube);
        LabEvent collaboratorTransferEvent =
                new LabEvent(LabEventType.COLLABORATOR_TRANSFER, new Date(), "thisLocation", 0l, 0l, "testprogram");
        barcodedTube.getInPlaceLabEvents().add(collaboratorTransferEvent);
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
         * Param 1
         * Metadata source,       setMercurySample, set POS metadata Source, sample available, sampledata set, expected Result
         *
         */
        /*
          Source of the sample is determined to be Mercury
         */
        /*
            A MercurySample is defined and set on the product order sample, the Metadata source is not set,
            the sample has been accessioned, and the sample data
         */
        dataList.add(new Object[]{MercurySample.MetadataSource.MERCURY, TargetMercurySample.PRODUCT_ORDER_SAMPLE_AND_DATABASE, true, true, true});
        dataList.add(new Object[]{MercurySample.MetadataSource.MERCURY, TargetMercurySample.PRODUCT_ORDER_SAMPLE_AND_DATABASE, true, false, true});
        dataList.add(new Object[]{MercurySample.MetadataSource.MERCURY, TargetMercurySample.PRODUCT_ORDER_SAMPLE_AND_DATABASE, false, true, false});
        dataList.add(new Object[]{MercurySample.MetadataSource.MERCURY, TargetMercurySample.PRODUCT_ORDER_SAMPLE_AND_DATABASE, false, false, false});

        dataList.add(new Object[]{MercurySample.MetadataSource.MERCURY, TargetMercurySample.NEITHER, true, true, false});
        dataList.add(new Object[]{MercurySample.MetadataSource.MERCURY, TargetMercurySample.NEITHER, true, false, false});
        dataList.add(new Object[]{MercurySample.MetadataSource.MERCURY, TargetMercurySample.NEITHER, false, true, false});
        dataList.add(new Object[]{MercurySample.MetadataSource.MERCURY, TargetMercurySample.NEITHER, false, false, false});

        /*
            A Mercury sample existing in the Database without a relationship to a corresponding product order sample
             defined in the database should be an invalid case.  We make a proactive effort to bind the two entities
             when either is first entered into mercury.
          */
//        dataList.add(new Object[]{MercurySample.MetadataSource.MERCURY, TargetMercurySample.DATABASE, true, true, true});
//        dataList.add(new Object[]{MercurySample.MetadataSource.MERCURY, TargetMercurySample.DATABASE, true, false, true});
//        dataList.add(new Object[]{MercurySample.MetadataSource.MERCURY, TargetMercurySample.DATABASE, false, true, false});
//        dataList.add(new Object[]{MercurySample.MetadataSource.MERCURY, TargetMercurySample.DATABASE, false, false, false});

        /*
          Source of the sample is determined to be BSP
         */
        dataList.add(new Object[]{MercurySample.MetadataSource.BSP, TargetMercurySample.PRODUCT_ORDER_SAMPLE_AND_DATABASE, true, true, true});
        dataList.add(new Object[]{MercurySample.MetadataSource.BSP, TargetMercurySample.PRODUCT_ORDER_SAMPLE_AND_DATABASE, false, true, false});
        dataList.add(new Object[]{MercurySample.MetadataSource.BSP, TargetMercurySample.PRODUCT_ORDER_SAMPLE_AND_DATABASE, true, false, false});
        dataList.add(new Object[]{MercurySample.MetadataSource.BSP, TargetMercurySample.PRODUCT_ORDER_SAMPLE_AND_DATABASE, false, false, false});

        dataList.add(new Object[]{MercurySample.MetadataSource.BSP, TargetMercurySample.NEITHER, true, true, true});
        dataList.add(new Object[]{MercurySample.MetadataSource.BSP, TargetMercurySample.NEITHER, true, false, false});
        dataList.add(new Object[]{MercurySample.MetadataSource.BSP, TargetMercurySample.NEITHER, false, true, false});
        dataList.add(new Object[]{MercurySample.MetadataSource.BSP, TargetMercurySample.NEITHER, false, false, false});

        /*
            A Mercury sample existing in the Database without a relationship to a corresponding product order sample
             defined in the database should be an invalid case.  We make a proactive effort to bind the two entities
             when either is first entered into mercury.
         */
//        dataList.add(new Object[]{MercurySample.MetadataSource.BSP, TargetMercurySample.DATABASE, true, true, true});
//        dataList.add(new Object[]{MercurySample.MetadataSource.BSP, TargetMercurySample.DATABASE, true, false, false});
//        dataList.add(new Object[]{MercurySample.MetadataSource.BSP, TargetMercurySample.DATABASE, false, true, false});
//        dataList.add(new Object[]{MercurySample.MetadataSource.BSP, TargetMercurySample.DATABASE, false, false, false});

        /*
           Source of the sample is ambiguous.
         */
        dataList.add(new Object[]{null, TargetMercurySample.NEITHER, true,  true,  false});
        dataList.add(new Object[]{null, TargetMercurySample.NEITHER, true,  false, false});
        dataList.add(new Object[]{null, TargetMercurySample.NEITHER, false, true,  false});
        dataList.add(new Object[]{null, TargetMercurySample.NEITHER, false, false, false});

        return dataList.toArray(new Object[dataList.size()][]);
    }

    @Test(groups = TestGroups.DATABASE_FREE, dataProvider = "availableSampleConditions")
    public void testIsSampleAvailable(MercurySample.MetadataSource source, TargetMercurySample setMercurySample,
                                      boolean isSampleAvailable, boolean isSampleDataSet, boolean expectedResult) {
        String sampleId = "SM-2923";

        MercurySample sourceSample = null;
        ProductOrderSample productOrderSample = new ProductOrderSample(sampleId);

        SampleData receivedSampleData = new BspSampleData();
        if(source != null) {
            if (source == MercurySample.MetadataSource.BSP && isSampleDataSet) {
                Map<BSPSampleSearchColumn, String> sampleDataMap = new HashMap<>();
                sampleDataMap.put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, "C"+sampleId);
                if (isSampleAvailable) {
                    sampleDataMap.put(BSPSampleSearchColumn.RECEIPT_DATE,
                            new SimpleDateFormat(BspSampleData.BSP_DATE_FORMAT_STRING).format(new Date()));
                }
                receivedSampleData = new BspSampleData(sampleDataMap);
            } else {
                receivedSampleData =
                        new MercurySampleData(sampleId,
                                Collections.singleton(new Metadata(Metadata.Key.BROAD_SAMPLE_ID,sampleId)));
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

        if(source != null && source == MercurySample.MetadataSource.MERCURY && isSampleAvailable) {
            BarcodedTube barcodedTube = new BarcodedTube("VesselFor" + sourceSample.getSampleKey(),
                    BarcodedTube.BarcodedTubeType.MatrixTube);
            LabEvent collaboratorTransferEvent =
                    new LabEvent(LabEventType.COLLABORATOR_TRANSFER, new Date(), "thisLocation", 0l, 0l, "testprogram");
            barcodedTube.getInPlaceLabEvents().add(collaboratorTransferEvent);
            sourceSample.getLabVessel().add(barcodedTube);
        }

        SampleDataSourceResolver resolver = new SampleDataSourceResolver(mockDao);
        resolver.populateSampleDataSources(Collections.singleton(productOrderSample));
        Assert.assertEquals(productOrderSample.isSampleAvailable(), expectedResult);
    }

    private enum TargetMercurySample {
        PRODUCT_ORDER_SAMPLE_AND_DATABASE, DATABASE, NEITHER;
    }
}