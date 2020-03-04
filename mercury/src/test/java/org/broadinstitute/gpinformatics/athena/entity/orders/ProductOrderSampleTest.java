package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.boundary.products.InvalidProductException;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntryTest;
import org.broadinstitute.gpinformatics.athena.entity.billing.ProductLedgerIndex;
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
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToVesselTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleData;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@Test(groups = TestGroups.DATABASE_FREE)
public class ProductOrderSampleTest {

    private static ProductOrderSample createOrderedSample(String name, boolean sapOrder) {
        ProductOrderSample sample = new ProductOrderSample(name);
        ProductOrder order = new ProductOrder();
        Product product = new Product();
        PriceItem primaryPriceItem = new PriceItem("primary", "", null, "primary");
        if(sapOrder) {
            order.setQuoteId("2700039");
        } else {
            order.setQuoteId("GP-TEST");
        }
        product.setPrimaryPriceItem(primaryPriceItem);
        try {
            order.setProduct(product);
        } catch (InvalidProductException e) {
            Assert.fail(e.getMessage());
        }
        order.addSample(sample);
        return sample;
    }

    public static ProductOrderSample createBilledSample(String name, LedgerEntry.PriceItemType priceItemType,
                                                        boolean sapOrder) {
        ProductOrderSample sample = createOrderedSample(name, sapOrder);

        PriceItem billedPriceItem;
        if (priceItemType == LedgerEntry.PriceItemType.PRIMARY_PRICE_ITEM) {
            billedPriceItem = sample.getProductOrder().getProduct().getPrimaryPriceItem();
        } else {
            billedPriceItem = new PriceItem(priceItemType.name(), "", null, priceItemType.name());
        }
        if(sample.getProductOrder().hasSapQuote()) {
            sample.addLedgerItem(new Date(), sample.getProductOrder().getProduct(), BigDecimal.ONE, false);
        } else {
            sample.addLedgerItem(new Date(), billedPriceItem, BigDecimal.ONE);
        }
        LedgerEntry entry = sample.getLedgerItems().iterator().next();
        entry.setPriceItemType(priceItemType);
        new BillingSession(0L, Collections.singleton(entry));
        entry.setBillingMessage(BillingSession.SUCCESS);

        return sample;
    }

    public static ProductOrderSample createBilledSample(String name, boolean sapOrder) {
        return createBilledSample(name, LedgerEntry.PriceItemType.PRIMARY_PRICE_ITEM, sapOrder);
    }

    private static ProductOrderSample createUnbilledSampleWithLedger(String name, boolean sapOrder) {
        ProductOrderSample sample = createOrderedSample(name, sapOrder);
        LedgerEntry billedEntry = LedgerEntryTest.createOneLedgerEntry(sample, "price item", BigDecimal.ONE, new Date());
        sample.getLedgerItems().add(billedEntry);
        return sample;
    }

    @DataProvider(name = "testIsBilled")
    public Object[][] createIsBilledData() {
        return new Object[][]{
                {createBilledSample("ABC", false), true},
                {createBilledSample("ABC", LedgerEntry.PriceItemType.ADD_ON_PRICE_ITEM, false), false},
                {createBilledSample("ABC", LedgerEntry.PriceItemType.REPLACEMENT_PRICE_ITEM, false), true},
                {createUnbilledSampleWithLedger("ABC", false), false},
                {createOrderedSample("ABC", false), false}
        };
    }

    @Test(dataProvider = "testIsBilled")
    public void testIsBilled(ProductOrderSample sample, boolean isBilled) {
        Assert.assertEquals(sample.isCompletelyBilled(), isBilled);
    }

    /**
     * Tests {@link ProductOrderSample#isCompletelyBilled()} with each possible {@link LedgerEntry.PriceItemType}. This
     * test will fail if a PriceItemType is added without considering how it would affect the calculation for whether or
     * not a sample has been billed.
     *
     * @param priceItemType    the price item type to create a ledger for before invoking isCompletelyBilled()
     */
    @Test(dataProvider = "getPriceItemTypes")
    public void testIsCompletelyBilledHandlesAllPriceItemTypes(LedgerEntry.PriceItemType priceItemType) {
        ProductOrderSample productOrderSample = createBilledSample("SM-123", priceItemType, false);

        // No assert here, just making sure that an exception is not thrown if an unknown PriceItemType is encountered.
        productOrderSample.isCompletelyBilled();
    }

    @DataProvider
    public Object[][] getPriceItemTypes() {
        Object[][] result = new Object[LedgerEntry.PriceItemType.values().length][];
        int index = 0;
        for (LedgerEntry.PriceItemType priceItemType : LedgerEntry.PriceItemType.values()) {
            result[index++] = new Object[]{priceItemType};
        }
        return result;
    }


    /**
     * {@link ProductOrderSample#isCompletelyBilled()} should return false when the sample has been billed but then
     * credited, bringing the net quantity billed back to 0.
     */
    public void testIsBilledWithCredits() {
        ProductOrderSample sample = createBilledSample("test", false);

        // credit the price item already billed
        PriceItem billedPriceItem = sample.getProductOrder().getProduct().getPrimaryPriceItem();
        if(sample.getProductOrder().hasSapQuote()) {

            sample.addLedgerItem(new Date(), sample.getProductOrder().getProduct(), BigDecimal.ONE.negate(), false);
        } else {

            sample.addLedgerItem(new Date(), billedPriceItem, BigDecimal.ONE.negate());
        }
        // Flag the credit ledger entry as billed successfully
        for( LedgerEntry entry : sample.getLedgerItems() ) {
            entry.setPriceItemType(LedgerEntry.PriceItemType.PRIMARY_PRICE_ITEM);
            entry.setBillingMessage(BillingSession.SUCCESS);
        }

        Assert.assertFalse(sample.isCompletelyBilled());
    }

    /**
     * {@link ProductOrderSample#isCompletelyBilled()} should accumulate quantity accidentally billed as an add-on for
     * price items that have also been billed as a primary or replacement. The notion is that, if there has been a
     * billing event for a price item as a primary or replacement, all quantity billed against that price item should
     * be considered even if it was accidentally billed as an add-on.
     *
     * This makes primary/replacement billing more "sticky" than add-on billing in the sense that accidental billing as
     * a primary or replacement will promote add-on billing of the same price item to be treated as primary. As of this
     * change (July 2015), the only instances in production of price items being billed as both primary/replacement and
     * add-on are due to a primary or replacement being incorrectly classified as an add-on. Therefore, the promotion of
     * these add-ons is correct.
     *
     * Hopefully the product/add-on system will be cleaned up before the opposite case is encountered (if ever). If not,
     * then probably either the new case needs a data fix-up or the old cases will need data fix-ups.
     */
    public void testIsBilledWithCreditsAndAddOn() {
        ProductOrderSample sample = createBilledSample("test", false);

        // credit the price item with an add-on ledger entry
        PriceItem billedPriceItem = sample.getProductOrder().getProduct().getPrimaryPriceItem();
        if(sample.getProductOrder().hasSapQuote()) {

            sample.addLedgerItem(new Date(), sample.getProductOrder().getProduct(), BigDecimal.ONE.negate(), false);
        } else {
            sample.addLedgerItem(new Date(), billedPriceItem, BigDecimal.ONE.negate());
        }
        LedgerEntry entry = sample.getLedgerItems().iterator().next();
        entry.setPriceItemType(LedgerEntry.PriceItemType.ADD_ON_PRICE_ITEM);
        entry.setBillingMessage(BillingSession.SUCCESS);

        Assert.assertFalse(sample.isCompletelyBilled());
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
        LedgerEntry ledgerEntry = new LedgerEntry(data.sample1, data.product.getPrimaryPriceItem(), completedDate,
                BigDecimal.ONE);

        // Bill sample2.
        data.sample2.addLedgerItem(completedDate, data.product.getPrimaryPriceItem(), BigDecimal.ONE);
        LedgerEntry ledger = data.sample2.getLedgerItems().iterator().next();
        ledger.setBillingMessage(BillingSession.SUCCESS);
        ledger.setBillingSession(new BillingSession(0L, Collections.singleton(ledger)));

        TestPDOData dataSap = new TestPDOData("2700039");
        completedDate = new Date();
        LedgerEntry ledgerEntrySap = new LedgerEntry(dataSap.sample1, dataSap.product, completedDate, BigDecimal.ONE);

        // Bill sample2.
        dataSap.sample2.addLedgerItem(completedDate, dataSap.product, BigDecimal.ONE, false);
        LedgerEntry ledgerSap = dataSap.sample2.getLedgerItems().iterator().next();
        ledgerSap.setBillingMessage(BillingSession.SUCCESS);
        ledgerSap.setBillingSession(new BillingSession(0L, Collections.singleton(ledgerSap)));

        return new Object[][]{
                // Create ledger items from a single sample.
                new Object[]{data.sample1, completedDate, Collections.singleton(ledgerEntry)},
                // Update existing ledger items with "new" bill count.
                new Object[]{data.sample1, completedDate, Collections.singleton(ledgerEntry)},
                // If sample is already billed, don't create any ledger items.
                new Object[]{data.sample2, completedDate, Collections.emptySet()},
                // Create ledger items from a single sample.
                new Object[]{dataSap.sample1, completedDate, Collections.singleton(ledgerEntrySap)},
                // Update existing ledger items with "new" bill count.
                new Object[]{dataSap.sample1, completedDate, Collections.singleton(ledgerEntrySap)},
                // If sample is already billed, don't create any ledger items.
                new Object[]{dataSap.sample2, completedDate, Collections.emptySet()}
        };
    }

    @Test(dataProvider = "autoBillSample")
    public void testAutoBillSample(ProductOrderSample sample, Date completedDate, Set<LedgerEntry> ledgerEntries) {
        sample.autoBillSample(sample.getProductOrder().getProduct(), BigDecimal.ONE, completedDate);
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
        barcodedTube.setReceiptEvent(new BSPUserList.QADudeUser("LU", 1L),new Date(), 1L, LabEvent.UI_EVENT_LOCATION);
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
                barcodedTube.setReceiptEvent(new BSPUserList.QADudeUser("LU", 1L), new Date(), 1L,
                        LabEvent.UI_EVENT_LOCATION);
            }
        }

        SampleDataSourceResolver resolver = new SampleDataSourceResolver(mockDao);
        resolver.populateSampleDataSources(Collections.singleton(productOrderSample));
        Assert.assertEquals(productOrderSample.isSampleAvailable(), expectedResult);
    }

    @Test
    public void testGetNoReceiptDateFromMercurySample() {
        ProductOrderSample productOrderSample = createProductOrderSample("SM-TEST", "0123");

        assertThat(productOrderSample.getReceiptDate(), nullValue());
    }

    @Test
    public void testGetReceiptDateFromMercurySampleFromCurrentVessel() {
        ProductOrderSample productOrderSample = createProductOrderSample("SM-TEST", "0123");

        Calendar calendar = Calendar.getInstance();
        LabEvent labEvent =
                new LabEvent(LabEventType.SAMPLE_RECEIPT, calendar.getTime(), "ProductOrderSampleTest", 0L, 0L, "Test");
        LabVessel labVessel = productOrderSample.getMercurySample().getLabVessel().iterator().next();
        labVessel.addInPlaceEvent(labEvent);

        assertThat(productOrderSample.getReceiptDate(), equalTo(calendar.getTime()));
    }

    @Test
    public void testGetReceiptDateFromMercurySampleFromAncestorVessel() {
        ProductOrderSample productOrderSample = createProductOrderSample("SM-TEST", "0123");
        MercurySample parentSample = createMercurySample("SM-PARENT", "0001");
        LabVessel parentTube = parentSample.getLabVessel().iterator().next();

        Calendar calendar = Calendar.getInstance();
        LabEvent transferEvent =
                new LabEvent(LabEventType.A_BASE, calendar.getTime(), "ProductOrderSampleTest", 0L, 0L, "Test");
        transferEvent.getVesselToVesselTransfers()
                .add(new VesselToVesselTransfer(parentTube,
                        productOrderSample.getMercurySample().getLabVessel().iterator().next(), transferEvent));

        calendar.add(Calendar.DAY_OF_MONTH, 1);
        LabEvent receiptEvent =
                new LabEvent(LabEventType.SAMPLE_RECEIPT, calendar.getTime(), "ProductOrderSampleTest", 0L, 0L, "Test");
        parentTube.addInPlaceEvent(receiptEvent);

        assertThat(productOrderSample.getReceiptDate(), equalTo(calendar.getTime()));
    }

    @Test
    public void testDoNotGetReceivedDateFromMercurySampleFromDescendantVessel() {
        ProductOrderSample productOrderSample = createProductOrderSample("SM-TEST", "0123");
        LabVessel labVessel = productOrderSample.getMercurySample().getLabVessel().iterator().next();
        MercurySample childSample = createMercurySample("SM-CHILD", "0001");
        LabVessel childTube = childSample.getLabVessel().iterator().next();

        Calendar calendar = Calendar.getInstance();
        LabEvent transferEvent =
                new LabEvent(LabEventType.A_BASE, calendar.getTime(), "ProductOrderSampleTest", 0L, 0L, "Test");
        transferEvent.getVesselToVesselTransfers().add(new VesselToVesselTransfer(labVessel, childTube, transferEvent));

        LabEvent receiptEvent =
                new LabEvent(LabEventType.SAMPLE_RECEIPT, calendar.getTime(), "ProductOrderSampleTest", 0L, 0L, "Test");
        childTube.addInPlaceEvent(receiptEvent);

        assertThat(productOrderSample.getReceiptDate(), nullValue());
    }

    /**
     * Mercury won't always have a sample history to link extracted samples to their original root sample. However, any
     * sample derived from another sample is considered by Mercury to be "received".
     */
    @Test
    public void testSampleExtractedInBspIsReceived() {
        ProductOrderSample productOrderSample = createProductOrderSample("SM-TEST", "0123");
        productOrderSample.setSampleData(
                new BspSampleData(Collections.singletonMap(BSPSampleSearchColumn.ROOT_SAMPLE, "SM-ROOT")));

        assertThat(productOrderSample.isSampleReceived(), is(true));
    }

    /**
     * All of these test cases must satisfy the equation:
     *      expected resulting ready-to-bill = requested quantity - billed quantity
     */
    @DataProvider(name = "everything")
    public Object[][] everything() {
        // @formatter:off
        return new Object[][]{
            // billed       existing        requested       expected resulting
            // quantity     ready-to-bill   quantity        ready-to-bill
            {BigDecimal.valueOf(0), BigDecimal.valueOf(0), BigDecimal.valueOf(0), BigDecimal.valueOf(0)}, // no change (no existing billing)
            {BigDecimal.valueOf(0), BigDecimal.valueOf(0), BigDecimal.valueOf(1), BigDecimal.valueOf(1)}, // request new billing
            {BigDecimal.valueOf(0), BigDecimal.valueOf(0), BigDecimal.valueOf(2), BigDecimal.valueOf(2)}, // (redundant with "request new billing", but included for combinatorial completeness
            {BigDecimal.valueOf(0), BigDecimal.valueOf(1), BigDecimal.valueOf(0), BigDecimal.valueOf(0)}, // cancel unbilled request (no existing billing)
            {BigDecimal.valueOf(0), BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(1)}, // no change (pending billing request)
            {BigDecimal.valueOf(0), BigDecimal.valueOf(1), BigDecimal.valueOf(2), BigDecimal.valueOf(2)}, // update unbilled request (increase quantity, no existing billing)
            {BigDecimal.valueOf(1), BigDecimal.valueOf(0), BigDecimal.valueOf(0), BigDecimal.valueOf(-1)}, // credit
            {BigDecimal.valueOf(1), BigDecimal.valueOf(0), BigDecimal.valueOf(1), BigDecimal.valueOf(0)}, // no change (completed billing)
            {BigDecimal.valueOf(1), BigDecimal.valueOf(0), BigDecimal.valueOf(2), BigDecimal.valueOf(1)}, // request more billing
            {BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(0), BigDecimal.valueOf(-1)}, // update unbilled request (change from charge to credit)
            {BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(0)}, // cancel unbilled request (completed billing)
            {BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(2), BigDecimal.valueOf(1)}  // no change (completed billing and pending billing request)
        };
        // @formatter:on
    }

    /**
     * Test applying ledger updates in various scenarios. Variations include quantity fully billed, quantity requested
     * to be billed (but hasn't been billed to Broad Quotes, SAP, etc. yet), and the quantity update being requested.
     * The combination of these values also varies whether a quantity should be charged, credited, or if no changes are
     * needed at all.
     *
     * @param quantityBilled
     * @param quantityReadyToBill
     * @param quantityRequested
     * @param expectedQuantityReadyToBill
     * @throws StaleLedgerUpdateException
     */
    @Test(dataProvider = "everything")
    public void testEverything(BigDecimal quantityBilled, BigDecimal quantityReadyToBill, BigDecimal quantityRequested,
                               BigDecimal expectedQuantityReadyToBill) throws StaleLedgerUpdateException {

        /*
         * The expected resulting unbilled ledger quantity must equal the new quantity being requested minus the
         * quantity already billed. While the unbilled/ready-to-bill quantity affects the specifics of applying a
         * ledger update, it does not factor into the expected result because unbilled records can be modified.
         *
         * This assertion is here both to document that point and as a guard against nonsensical test cases.
         */
        assertThat(expectedQuantityReadyToBill, equalTo(quantityRequested.subtract(quantityBilled)));
        assertThat(quantityBilled, anyOf(equalTo(BigDecimal.valueOf(0)), equalTo(BigDecimal.valueOf(1))));

        ProductOrderSample productOrderSample;
        if (quantityBilled.compareTo(BigDecimal.ZERO) == 0) {
            productOrderSample = createOrderedSample("TEST", false);
        } else {
            productOrderSample = createBilledSample("TEST", false);
        }

        PriceItem priceItem = productOrderSample.getProductOrder().getProduct().getPrimaryPriceItem();
        if (quantityReadyToBill.compareTo(BigDecimal.ZERO) > 0) {
            addUnbilledLedgerEntry(productOrderSample, priceItem, quantityReadyToBill);
        }

        ProductOrderSample.LedgerQuantities ledgerQuantities = productOrderSample.getLedgerQuantities().get(
                ProductLedgerIndex.create(null, priceItem, productOrderSample.getProductOrder().hasSapQuote()));
        BigDecimal quantityBefore = ledgerQuantities != null ? ledgerQuantities.getTotal() : BigDecimal.ZERO;
        BigDecimal currentQuantity = quantityBefore; // these tests all assume no external changes
        Date workCompleteDate = new Date();

        ProductOrderSample.LedgerUpdate ledgerUpdate;
        if(productOrderSample.getProductOrder().hasSapQuote()) {
            ledgerUpdate =
                    new ProductOrderSample.LedgerUpdate(productOrderSample.getSampleKey(),
                            productOrderSample.getProductOrder().getProduct(), quantityBefore, currentQuantity,
                            quantityRequested, workCompleteDate, false);
        } else {
            ledgerUpdate =
                    new ProductOrderSample.LedgerUpdate(productOrderSample.getSampleKey(), priceItem,
                            productOrderSample.getProductOrder().getProduct(), quantityBefore, currentQuantity,
                            quantityRequested, workCompleteDate);
        }
        productOrderSample.applyLedgerUpdate(ledgerUpdate);

        LedgerEntry ledgerEntry;
        if(productOrderSample.getProductOrder().hasSapQuote()) {

            ledgerEntry = productOrderSample.findUnbilledLedgerEntryForProduct(productOrderSample.getProductOrder().getProduct());
        } else {
            ledgerEntry = productOrderSample.findUnbilledLedgerEntryForPriceItem(priceItem);
        }
        if (expectedQuantityReadyToBill.compareTo(BigDecimal.ZERO) == 0) {
            assertThat(ledgerEntry, nullValue());
            assertThat(productOrderSample.getBillableLedgerItems(), empty());
        } else {
            assertThat(ledgerEntry.getQuantity(), equalTo(expectedQuantityReadyToBill));
            assertThat(productOrderSample.getBillableLedgerItems(), hasSize(1));
        }
    }

    private void addUnbilledLedgerEntry(ProductOrderSample productOrderSample, PriceItem priceItem,
                                        BigDecimal quantityReadyToBill) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        Date oldWorkCompleteDate = calendar.getTime();
        if(productOrderSample.getProductOrder().hasSapQuote()) {

            productOrderSample.addLedgerItem(oldWorkCompleteDate,
                    productOrderSample.getProductOrder().getProduct(), quantityReadyToBill, false);
        } else {
            productOrderSample.addLedgerItem(oldWorkCompleteDate, priceItem,
                    quantityReadyToBill);
        }
    }

    @Test
    public void testApplyUpdateWithOutdatedInformationThrowsException() {
        ProductOrderSample productOrderSample = createOrderedSample("TEST", false);
        PriceItem priceItem = productOrderSample.getProductOrder().getProduct().getPrimaryPriceItem();

        ProductOrderSample.LedgerUpdate ledgerUpdate;
        if(productOrderSample.getProductOrder().hasSapQuote()) {
            ledgerUpdate =
                    new ProductOrderSample.LedgerUpdate(productOrderSample.getSampleKey(),
                            productOrderSample.getProductOrder().getProduct(), BigDecimal.valueOf(1),
                            BigDecimal.valueOf(0), BigDecimal.valueOf(2),
                            new Date(), false);
        } else {
            ledgerUpdate =
                    new ProductOrderSample.LedgerUpdate(productOrderSample.getSampleKey(), priceItem, productOrderSample.getProductOrder().getProduct(),
                            BigDecimal.valueOf(1), BigDecimal.valueOf(0), BigDecimal.valueOf(2),
                            new Date());
        }
        Exception caught = null;
        try {
            productOrderSample.applyLedgerUpdate(ledgerUpdate);
        } catch (Exception e) {
            caught = e;
        }
        assertThat(caught, instanceOf(StaleLedgerUpdateException.class));
    }

    public void testAddLedgerEntryWithNoWorkCompleteDateThrowsException() {
        ProductOrderSample productOrderSample = createOrderedSample("TEST", false);
        PriceItem priceItem = productOrderSample.getProductOrder().getProduct().getPrimaryPriceItem();

        ProductOrderSample.LedgerUpdate ledgerUpdate;
        if(productOrderSample.getProductOrder().hasSapQuote()) {
            ledgerUpdate =
                    new ProductOrderSample.LedgerUpdate(productOrderSample.getSampleKey(),
                            productOrderSample.getProductOrder().getProduct(), BigDecimal.valueOf(0),
                            BigDecimal.valueOf(0), BigDecimal.valueOf(1), null, false);
        } else {
            ledgerUpdate =
                    new ProductOrderSample.LedgerUpdate(productOrderSample.getSampleKey(), priceItem,
                            productOrderSample.getProductOrder().getProduct(), BigDecimal.valueOf(0),
                            BigDecimal.valueOf(0), BigDecimal.valueOf(1), null);
        }
        Exception caught = null;
        try {
            productOrderSample.applyLedgerUpdate(ledgerUpdate);
        } catch (Exception e) {
            caught = e;
        }
        assertThat(caught, instanceOf(IllegalArgumentException.class));
    }

    @NotNull
    private ProductOrderSample createProductOrderSample(String sampleName, String tubeBarcode) {
        ProductOrderSample productOrderSample;
        productOrderSample = new ProductOrderSample(sampleName);
        MercurySample mercurySample = createMercurySample(sampleName, tubeBarcode);
        mercurySample.addProductOrderSample(productOrderSample);
        return productOrderSample;
    }

    @NotNull
    private MercurySample createMercurySample(String sampleName, String tubeBarcode) {
        LabVessel labVessel = new BarcodedTube(tubeBarcode);
        MercurySample mercurySample = new MercurySample(sampleName, Collections.<Metadata>emptySet());
        mercurySample.addLabVessel(labVessel);
        return mercurySample;
    }

    private enum TargetMercurySample {
        PRODUCT_ORDER_SAMPLE_AND_DATABASE, DATABASE, NEITHER;
    }
}
