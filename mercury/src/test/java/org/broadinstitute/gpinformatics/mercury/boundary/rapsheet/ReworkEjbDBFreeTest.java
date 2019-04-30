package org.broadinstitute.gpinformatics.mercury.boundary.rapsheet;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderSampleTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Test(groups = TestGroups.DATABASE_FREE)
public class ReworkEjbDBFreeTest extends BaseEventTest {

    private ReworkEjb reworkEjb;
    private LabVessel labVessel;
    private List<ProductOrderSample> sampleList1;
    private ProductOrder testPdo1;
    private List<ProductOrderSample> sampleList2;
    private ProductOrder testPdo2;
    private List<ProductOrderSample> sampleList3;
    private ProductOrder testPdo3;
    private List<ProductOrderSample> sampleList4;
    private ProductOrder draftPDO;

    @Override
    @BeforeMethod
    public void setUp() {

        reworkEjb = new ReworkEjb();
        WorkflowLoader workflowLoader = new WorkflowLoader();
        reworkEjb.setWorkflowConfig(workflowLoader.load());

        labVessel = new BarcodedTube("22834023", BarcodedTube.BarcodedTubeType.MatrixTube);

        sampleList1 = ProductOrderSampleTestFactory.createDBFreeSampleList(MercurySample.MetadataSource.BSP, "SM-test1");
        testPdo1 = ProductOrderTestFactory.buildExExProductOrder(1);
        testPdo1.setJiraTicketKey("PDO-14");
        testPdo1.setSamples(sampleList1);
        testPdo1.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        testPdo1.getProduct().setProductFamily(new ProductFamily(
                ProductFamily.ProductFamilyInfo.EXOME.getFamilyName()));

        sampleList2 = ProductOrderSampleTestFactory.createDBFreeSampleList(MercurySample.MetadataSource.BSP, "SM-test2");
        testPdo2 = ProductOrderTestFactory.buildExExProductOrder(1);
        testPdo2.setJiraTicketKey("PDO-15");
        testPdo2.setSamples(sampleList2);
        testPdo2.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        testPdo2.getProduct().setProductFamily(new ProductFamily(
                ProductFamily.ProductFamilyInfo.EXOME.getFamilyName()));

        sampleList3 = ProductOrderSampleTestFactory.createDBFreeSampleList(MercurySample.MetadataSource.BSP, "SM-test3");
        testPdo3 = ProductOrderTestFactory.buildExExProductOrder(1);
        testPdo3.setJiraTicketKey("PDO-16");
        testPdo3.setSamples(sampleList3);
        testPdo3.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        testPdo3.getProduct().setProductFamily(new ProductFamily(
                ProductFamily.ProductFamilyInfo.EXOME.getFamilyName()));

        sampleList4 = ProductOrderSampleTestFactory.createDBFreeSampleList(MercurySample.MetadataSource.BSP, "SM-test4");
        draftPDO = ProductOrderTestFactory.buildExExProductOrder(1);
        draftPDO.setJiraTicketKey("PDO-17");
        draftPDO.setSamples(sampleList4);
        draftPDO.getProduct().setProductFamily(new ProductFamily(
                ProductFamily.ProductFamilyInfo.EXOME.getFamilyName()));
    }

    @Test
    public void vesselTestRetrieveThreeCandidatesOf4() {

        Collection<ProductOrderSample> samples = new HashSet<>();

        ProductOrderSample poSample1 = testPdo1.getSamples().get(0);
        samples.add(poSample1);

        ProductOrderSample poSample2 = testPdo2.getSamples().get(0);
        samples.add(poSample2);

        ProductOrderSample poSample3 = testPdo3.getSamples().get(0);
        samples.add(poSample3);

        ProductOrderSample poSample4 = draftPDO.getSamples().get(0);
        samples.add(poSample4);

        Collection<ReworkEjb.BucketCandidate> candidates =
                reworkEjb.collectBucketCandidatesForAMercuryVessel(labVessel, samples);

        Assert.assertEquals(candidates.size(), 3);
        Assert.assertTrue(((ReworkEjb.BucketCandidate) candidates.toArray()[0]).isValid());
        Assert.assertTrue(((ReworkEjb.BucketCandidate) candidates.toArray()[1]).isValid());
        Assert.assertTrue(((ReworkEjb.BucketCandidate) candidates.toArray()[2]).isValid());
    }

    @DataProvider(name = "productOrderStatusDataProvider")
    public static Object[][] productOrderStatusDataProvider() {
        return new Object[][]{
                new Object[]{ProductOrder.OrderStatus.Abandoned, 2, 0},
                new Object[]{ProductOrder.OrderStatus.Completed, 2, 2},
                new Object[]{ProductOrder.OrderStatus.Draft, 2, 0},
                new Object[]{ProductOrder.OrderStatus.Pending, 2, 0},
                new Object[]{ProductOrder.OrderStatus.Submitted, 2, 2}
        };
    }

    @Test(dataProvider = "productOrderStatusDataProvider")
    public void testBucketProductOrderSampleBucketAbilityBasedOnStatus(ProductOrder.OrderStatus orderStatus,
                                                                      int numberOfSamplesInProductOrder,
                                                                      int expectedNumberOfSamplesInBucket) {
        ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(numberOfSamplesInProductOrder);
        productOrder.setOrderStatus(orderStatus);

        Collection<ReworkEjb.BucketCandidate> candidates =
                reworkEjb.collectBucketCandidatesForAMercuryVessel(labVessel, productOrder.getSamples());
        assertThat(candidates.size(), is(expectedNumberOfSamplesInBucket));
    }

    @Test
    public void vesselTestRetrieveThreeValidOneInvalidOf5() {

        Set<ProductOrderSample> sampleSet = new HashSet<>();

        ProductOrderSample poSample1 = testPdo1.getSamples().get(0);
        sampleSet.add(poSample1);

        ProductOrderSample poSample2 = testPdo2.getSamples().get(0);
        sampleSet.add(poSample2);

        ProductOrderSample poSample3 = testPdo3.getSamples().get(0);
        sampleSet.add(poSample3);

        ProductOrderSample poSample4 = draftPDO.getSamples().get(0);
        sampleSet.add(poSample4);

        List<ProductOrderSample> sampleList5 = ProductOrderSampleTestFactory.createDBFreeSampleList(
                MercurySample.MetadataSource.BSP, "SM-test5");
        ProductOrder nonExomeExpressPdo = ProductOrderTestFactory.buildHybridSelectionProductOrder(1, "PDO-18");
        nonExomeExpressPdo.setJiraTicketKey("PDO-18");
        nonExomeExpressPdo.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        nonExomeExpressPdo.setSamples(sampleList5);
        nonExomeExpressPdo.getProduct().setWorkflowName(Workflow.ICE_EXOME_EXPRESS);
        nonExomeExpressPdo.getProduct().setProductFamily(new ProductFamily(
                ProductFamily.ProductFamilyInfo.EXOME.getFamilyName()));

        ProductOrderSample nonExomeExpressSample5 = nonExomeExpressPdo.getSamples().get(0);
        sampleSet.add(nonExomeExpressSample5);

        Collection<ReworkEjb.BucketCandidate> candidates =
                reworkEjb.collectBucketCandidatesForAMercuryVessel(labVessel, sampleSet);

        Assert.assertEquals(candidates.size(), 4);
//        Assert.assertTrue(((ReworkEjb.BucketCandidate) candidates.toArray()[0]).isValid());
//        Assert.assertTrue(((ReworkEjb.BucketCandidate) candidates.toArray()[1]).isValid());
//        Assert.assertTrue(((ReworkEjb.BucketCandidate) candidates.toArray()[2]).isValid());
//        Assert.assertFalse(((ReworkEjb.BucketCandidate) candidates.toArray()[3]).isValid());
    }


    @Test
    public void nonVesselTestRetrieveThreeCandidatesOf4() {

        Set<ProductOrderSample> productOrderSampleSet = new HashSet<>();

        ProductOrderSample poSample1 = testPdo1.getSamples().get(0);
        productOrderSampleSet.add(poSample1);

        ProductOrderSample poSample2 = testPdo2.getSamples().get(0);
        productOrderSampleSet.add(poSample2);

        ProductOrderSample poSample3 = testPdo3.getSamples().get(0);
        productOrderSampleSet.add(poSample3);

        ProductOrderSample poSample4 = draftPDO.getSamples().get(0);
        productOrderSampleSet.add(poSample4);


        BSPSampleDataFetcher mockFetcher = Mockito.mock(BSPSampleDataFetcher.class);
        Mockito.when(mockFetcher.fetchSampleData(Mockito.anyCollectionOf(String.class))).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Collection<String> sampleIds = (Collection<String>) invocationOnMock.getArguments()[0];

                Map<String, SampleData> sampleIdDataMap = new HashMap<>();

                for (String sampleId : sampleIds) {

                    sampleIdDataMap.put(sampleId,
                            new BspSampleDataStub(Collections.singletonMap(BSPSampleSearchColumn.SAMPLE_ID, sampleId)));

                }

                return sampleIdDataMap;
            }
        });
        reworkEjb.setBspSampleDataFetcher(mockFetcher);


        Collection<ReworkEjb.BucketCandidate> candidates =
                reworkEjb.collectBucketCandidatesThatHaveBSPVessels(productOrderSampleSet);

        Assert.assertEquals(candidates.size(), 3);
        Assert.assertTrue(((ReworkEjb.BucketCandidate) candidates.toArray()[0]).isValid());
        Assert.assertTrue(((ReworkEjb.BucketCandidate) candidates.toArray()[1]).isValid());
        Assert.assertTrue(((ReworkEjb.BucketCandidate) candidates.toArray()[2]).isValid());
    }

    @Test
    public void nonVesselTestRetrieveThreeValidOneInvalidOf5() {

        Set<ProductOrderSample> productOrderSampleSet = new HashSet<>();

        ProductOrderSample poSample1 = testPdo1.getSamples().get(0);
        productOrderSampleSet.add(poSample1);

        ProductOrderSample poSample2 = testPdo2.getSamples().get(0);
        productOrderSampleSet.add(poSample2);

        ProductOrderSample poSample3 = testPdo3.getSamples().get(0);
        productOrderSampleSet.add(poSample3);

        ProductOrderSample poSample4 = draftPDO.getSamples().get(0);
        productOrderSampleSet.add(poSample4);

        List<ProductOrderSample> sampleList5 = ProductOrderSampleTestFactory.createDBFreeSampleList(
                MercurySample.MetadataSource.BSP, "SM-test5");
        ProductOrder nonExomeExpressPdo = ProductOrderTestFactory.buildHybridSelectionProductOrder(1, "PDO-18");

        nonExomeExpressPdo.setJiraTicketKey("PDO-18");
        nonExomeExpressPdo.setSamples(sampleList5);
        nonExomeExpressPdo.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        nonExomeExpressPdo.getProduct().setWorkflowName(Workflow.ICE_EXOME_EXPRESS);
        nonExomeExpressPdo.getProduct().setProductFamily(new ProductFamily(
                ProductFamily.ProductFamilyInfo.EXOME.getFamilyName()));

        ProductOrderSample nonExomesample = nonExomeExpressPdo.getSamples().get(0);
        productOrderSampleSet.add(nonExomesample);

        BSPSampleDataFetcher mockFetcher = Mockito.mock(BSPSampleDataFetcher.class);
        Mockito.when(mockFetcher.fetchSampleData(Mockito.anyCollectionOf(String.class))).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Collection<String> sampleIds = (Collection<String>) invocationOnMock.getArguments()[0];

                Map<String, SampleData> sampleIdDataMap = new HashMap<>();

                for (String sampleId : sampleIds) {

                    sampleIdDataMap.put(sampleId,
                            new BspSampleDataStub(Collections.singletonMap(BSPSampleSearchColumn.SAMPLE_ID, sampleId)));

                }

                return sampleIdDataMap;
            }
        });
        reworkEjb.setBspSampleDataFetcher(mockFetcher);

        Collection<ReworkEjb.BucketCandidate> candidates =
                reworkEjb.collectBucketCandidatesThatHaveBSPVessels(productOrderSampleSet);

        Assert.assertEquals(candidates.size(), 4);
    }

    @Test
    public void testValidBucketEntry() {

        ReworkEjb.BucketCandidate validCandidate = reworkEjb.getBucketCandidateConsideringProductFamily(
                sampleList1.get(0), sampleList1.get(0).getSampleKey(), labVessel.getLabel(),
                ProductFamily.ProductFamilyInfo.EXOME, labVessel, ""
        );

        Assert.assertTrue(validCandidate.isValid());
    }


    private class BspSampleDataStub extends BspSampleData {

        public BspSampleDataStub() {
            super();
        }

        public BspSampleDataStub(Map<BSPSampleSearchColumn, String> dataMap) {
            super(dataMap);
        }

        @Override
        public String getBarcodeForLabVessel() {
            return "1101" + getValue(BSPSampleSearchColumn.SAMPLE_ID);
        }
    }

}
