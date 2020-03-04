package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.bsp.client.response.SampleKitListResponse;
import org.broadinstitute.bsp.client.sample.Sample;
import org.broadinstitute.bsp.client.sample.SampleKit;
import org.broadinstitute.bsp.client.sample.SampleManager;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.samples.SampleReceiptValidation;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleReceiptService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleReceiptServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactory;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.BSPRestClient;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.QueueEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.enqueuerules.DnaQuantEnqueueOverride;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.SampleReceiptResource;
import org.broadinstitute.gpinformatics.mercury.control.vessel.BSPRestService;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Test(groups = TestGroups.DATABASE_FREE)
public class ReceiveSamplesEjbDBFreeTest {


    private SampleKit sampleKit1;
    private SampleKit sampleKit2;

    private ProductOrderSample pos1Kit1;
    private ProductOrderSample pos2Kit1;
    private ProductOrderSample pos3Kit1;
    private ProductOrderSample pos4Kit1;
    private ProductOrderSample pos1Kit2;
    private ProductOrderSample pos2Kit2;
    private ProductOrderSample pos3Kit2;
    private ProductOrderSample pos4Kit2;
    private ProductOrderSample pos5Kit1;

    private String sample1Kit1;
    private String sample2Kit1;
    private String sample3Kit1;
    private String sample4Kit1;
    private String sample1Kit2;
    private String sample2Kit2;
    private String sample3Kit2;
    private String sample4Kit2;
    private String sample5Kit1;

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() throws Exception {
        String sampleKit1ID = "SK-tst1";
        String sampleKit2ID = "SK-tst2";

        sample1Kit1 = "SM-1SK1";
        sample2Kit1 = "SM-2SK1";
        sample3Kit1 = "SM-3SK1";
        sample4Kit1 = "SM-4SK1";
        sample5Kit1 = "SM-5SK1";

        sampleKit1 = new SampleKit();
        sampleKit1.setSampleKitId(sampleKit1ID);

        List<Sample> kit1Samples = new ArrayList<>();
        Collections.addAll(kit1Samples, new Sample(sample1Kit1), new Sample(sample2Kit1), new Sample(sample3Kit1),
                new Sample(sample4Kit1));

        sampleKit1.setSamples(kit1Samples);

        sample1Kit2 = "SM-1SK2";
        sample2Kit2 = "SM-2SK2";
        sample3Kit2 = "SM-3SK2";
        sample4Kit2 = "SM-4SK2";

        sampleKit2 = new SampleKit();
        sampleKit2.setSampleKitId(sampleKit2ID);
        List<Sample> kit2Samples = new ArrayList<>();
        Collections.addAll(kit2Samples, new Sample(sample1Kit2), new Sample(sample2Kit2), new Sample(sample3Kit2),
                new Sample(sample4Kit2));

        sampleKit2.setSamples(kit2Samples);

        pos1Kit1 = new ProductOrderSample(sample1Kit1);
        pos2Kit1 = new ProductOrderSample(sample2Kit1);
        pos3Kit1 = new ProductOrderSample(sample3Kit1);
        pos4Kit1 = new ProductOrderSample(sample4Kit1);

        pos1Kit2 = new ProductOrderSample(sample1Kit2);
        pos2Kit2 = new ProductOrderSample(sample2Kit2);
        pos3Kit2 = new ProductOrderSample(sample3Kit2);
        pos4Kit2 = new ProductOrderSample(sample4Kit2);
        pos5Kit1 = new ProductOrderSample(sample5Kit1);
    }

    /**
     * Tests the case where exactly the same samples that are in a sample kit come in for receipt.  This is the "Happy"
     * test case
     *
     * @throws Exception
     */
    public void testAllSamplesKit1Validation() throws Exception {

        Set<String> test1RequestSMIds = new HashSet<>();

        test1RequestSMIds.add(sample1Kit1);
        test1RequestSMIds.add(sample2Kit1);
        test1RequestSMIds.add(sample3Kit1);
        test1RequestSMIds.add(sample4Kit1);

        BSPSampleReceiptService stubReceiptService = new BSPSampleReceiptServiceStub();
        BSPManagerFactory mockManagerFactory = Mockito.mock(BSPManagerFactory.class);
        SampleManager mockSampManager = Mockito.mock(SampleManager.class);
        SampleReceiptResource sampleReceiptResource = Mockito.mock(SampleReceiptResource.class);
        BSPRestService bspRestService = Mockito.mock(BSPRestService.class);

        SampleKitListResponse mockResponse = new SampleKitListResponse();
        mockResponse.setResult(Collections.singletonList(sampleKit1));
        mockResponse.setSuccess(true);
        Mockito.when(mockSampManager.getSampleKitsBySampleIds(Mockito.eq(test1RequestSMIds))).thenReturn(mockResponse);
        Mockito.when(mockManagerFactory.createSampleManager()).thenReturn(mockSampManager);
        ProductOrderSampleDao mockPosDao = Mockito.mock(ProductOrderSampleDao.class);

        Map<String, Set<ProductOrderSample>> posResult = new HashMap<>();
        posResult.put(sample1Kit1, Collections.singleton(pos1Kit1));
        posResult.put(sample2Kit1, Collections.singleton(pos2Kit1));
        posResult.put(sample3Kit1, Collections.singleton(pos3Kit1));
        posResult.put(sample4Kit1, Collections.singleton(pos4Kit1));
        Mockito.when(mockPosDao.findMapBySamples(Mockito.eq(test1RequestSMIds))).thenReturn(posResult);

        BSPUserList testUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());

        ReceiveSamplesEjb testEjb =
                new ReceiveSamplesEjb(stubReceiptService, mockManagerFactory, mockPosDao, testUserList,
                        bspRestService, sampleReceiptResource,
                        Mockito.mock(LabVesselDao.class), Mockito.mock(QueueEjb.class),
                        Mockito.mock(DnaQuantEnqueueOverride.class));

        // TODO: Review this to msee what needs to be mocked on labVesselDao and QueueEjb
        MessageCollection validationResults = new MessageCollection();

        testEjb.validateForReceipt(test1RequestSMIds, validationResults, "scottmat");

        Assert.assertFalse(validationResults.hasErrors());
        Assert.assertFalse(validationResults.hasInfos());
        Assert.assertFalse(validationResults.hasWarnings());

        Assert.assertTrue(pos1Kit1.getSampleReceiptValidations().isEmpty());
        Assert.assertTrue(pos2Kit1.getSampleReceiptValidations().isEmpty());
        Assert.assertTrue(pos3Kit1.getSampleReceiptValidations().isEmpty());
        Assert.assertTrue(pos4Kit1.getSampleReceiptValidations().isEmpty());
    }

    /**
     * Tests the case where less samples are received than are defined in the Sample kit with which they are associated
     *
     * @throws Exception
     */
    public void testPartialSamplesKit1Validation() throws Exception {

        Set<String> test2RequestSMIds = new HashSet<>();

        test2RequestSMIds.add(sample1Kit1);
        test2RequestSMIds.add(sample2Kit1);
        test2RequestSMIds.add(sample3Kit1);

        BSPSampleReceiptService stubReceiptService = new BSPSampleReceiptServiceStub();
        BSPManagerFactory mockManagerFactory = Mockito.mock(BSPManagerFactory.class);
        SampleManager mockSampManager = Mockito.mock(SampleManager.class);
        SampleReceiptResource sampleReceiptResource = Mockito.mock(SampleReceiptResource.class);
        BSPRestService bspRestService = Mockito.mock(BSPRestService.class);

        SampleKitListResponse mockResponse = new SampleKitListResponse();
        mockResponse.setResult(Collections.singletonList(sampleKit1));
        mockResponse.setSuccess(true);
        Mockito.when(mockSampManager.getSampleKitsBySampleIds(Mockito.eq(test2RequestSMIds))).thenReturn(mockResponse);
        Mockito.when(mockManagerFactory.createSampleManager()).thenReturn(mockSampManager);
        ProductOrderSampleDao mockPosDao = Mockito.mock(ProductOrderSampleDao.class);

        Map<String, Set<ProductOrderSample>> posResult = new HashMap<>();
        posResult.put(sample1Kit1, Collections.singleton(pos1Kit1));
        posResult.put(sample2Kit1, Collections.singleton(pos2Kit1));
        posResult.put(sample3Kit1, Collections.singleton(pos3Kit1));
        Mockito.when(mockPosDao.findMapBySamples(Mockito.eq(test2RequestSMIds))).thenReturn(posResult);

        BSPUserList testUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());

        LabVesselDao labVesselDao = Mockito.mock(LabVesselDao.class);
        QueueEjb queueEjb = Mockito.mock(QueueEjb.class);
        DnaQuantEnqueueOverride dnaQuantEnqueueOverride = Mockito.mock(DnaQuantEnqueueOverride.class);
        ReceiveSamplesEjb testEjb = new ReceiveSamplesEjb(stubReceiptService, mockManagerFactory, mockPosDao,
                testUserList, bspRestService, sampleReceiptResource, labVesselDao, queueEjb, dnaQuantEnqueueOverride);
// TODO: Review this to msee what needs to be mocked on labVesselDao and QueueEjb
        MessageCollection validationResults = new MessageCollection();

        testEjb.validateForReceipt(test2RequestSMIds, validationResults, "scottmat");

        Assert.assertFalse(validationResults.hasErrors());
        Assert.assertFalse(validationResults.hasInfos());
        Assert.assertTrue(validationResults.hasWarnings());

        Assert.assertFalse(pos1Kit1.getSampleReceiptValidations().isEmpty());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationReason.MISSING_SAMPLE_FROM_SAMPLE_KIT,
                pos1Kit1.getSampleReceiptValidations().iterator().next().getReason());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationType.WARNING,
                pos1Kit1.getSampleReceiptValidations().iterator().next().getValidationType());
        Assert.assertFalse(pos2Kit1.getSampleReceiptValidations().isEmpty());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationReason.MISSING_SAMPLE_FROM_SAMPLE_KIT,
                pos2Kit1.getSampleReceiptValidations().iterator().next().getReason());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationType.WARNING,
                pos2Kit1.getSampleReceiptValidations().iterator().next().getValidationType());
        Assert.assertFalse(pos3Kit1.getSampleReceiptValidations().isEmpty());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationReason.MISSING_SAMPLE_FROM_SAMPLE_KIT,
                pos3Kit1.getSampleReceiptValidations().iterator().next().getReason());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationType.WARNING,
                pos3Kit1.getSampleReceiptValidations().iterator().next().getValidationType());
        Assert.assertTrue(pos4Kit1.getSampleReceiptValidations().isEmpty());
    }

    /**
     * Test the case where a sample is returned that is not associated with any sample kit in BSP
     *
     * @throws Exception
     */
    public void testUnrecognizedSamplesKit1Validation() throws Exception {

        Set<String> test3RequestSMIds = new HashSet<>();

        test3RequestSMIds.add(sample1Kit1);
        test3RequestSMIds.add(sample2Kit1);
        test3RequestSMIds.add(sample3Kit1);
        test3RequestSMIds.add(sample4Kit1);
        test3RequestSMIds.add(sample5Kit1);

        BSPSampleReceiptService stubReceiptService = new BSPSampleReceiptServiceStub();
        BSPManagerFactory mockManagerFactory = Mockito.mock(BSPManagerFactory.class);
        SampleManager mockSampManager = Mockito.mock(SampleManager.class);
        SampleReceiptResource sampleReceiptResource = Mockito.mock(SampleReceiptResource.class);
        BSPRestService bspRestService = Mockito.mock(BSPRestService.class);

        SampleKitListResponse mockResponse = new SampleKitListResponse();
        mockResponse.setResult(Collections.singletonList(sampleKit1));
        mockResponse.setSuccess(true);
        Mockito.when(mockSampManager.getSampleKitsBySampleIds(Mockito.eq(test3RequestSMIds))).thenReturn(mockResponse);
        Mockito.when(mockManagerFactory.createSampleManager()).thenReturn(mockSampManager);
        ProductOrderSampleDao mockPosDao = Mockito.mock(ProductOrderSampleDao.class);

        Map<String, Set<ProductOrderSample>> posResult2 = new HashMap<>();
        posResult2.put(sample1Kit1, Collections.singleton(pos1Kit1));
        posResult2.put(sample2Kit1, Collections.singleton(pos2Kit1));
        posResult2.put(sample3Kit1, Collections.singleton(pos3Kit1));
        posResult2.put(sample4Kit1, Collections.singleton(pos4Kit1));
        posResult2.put(sample5Kit1, Collections.singleton(pos5Kit1));
        Mockito.when(mockPosDao.findMapBySamples(Mockito.eq(test3RequestSMIds))).thenReturn(posResult2);

        BSPUserList testUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());

        LabVesselDao labVesselDao = Mockito.mock(LabVesselDao.class);
        QueueEjb queueEjb = Mockito.mock(QueueEjb.class);
        DnaQuantEnqueueOverride dnaQuantEnqueueOverride = Mockito.mock(DnaQuantEnqueueOverride.class);
        ReceiveSamplesEjb testEjb = new ReceiveSamplesEjb(stubReceiptService, mockManagerFactory, mockPosDao,
                testUserList, bspRestService, sampleReceiptResource, labVesselDao, queueEjb, dnaQuantEnqueueOverride);
// TODO: Review this to msee what needs to be mocked on labVesselDao and QueueEjb
        MessageCollection validationResults = new MessageCollection();

        testEjb.validateForReceipt(test3RequestSMIds, validationResults, "scottmat");

        Assert.assertTrue(validationResults.hasErrors());
        Assert.assertFalse(validationResults.hasInfos());
        Assert.assertFalse(validationResults.hasWarnings());

        Assert.assertTrue(pos1Kit1.getSampleReceiptValidations().isEmpty());
        Assert.assertTrue(pos2Kit1.getSampleReceiptValidations().isEmpty());
        Assert.assertTrue(pos3Kit1.getSampleReceiptValidations().isEmpty());
        Assert.assertTrue(pos4Kit1.getSampleReceiptValidations().isEmpty());
        Assert.assertFalse(pos5Kit1.getSampleReceiptValidations().isEmpty());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationReason.SAMPLE_NOT_IN_BSP,
                pos5Kit1.getSampleReceiptValidations().iterator().next().getReason());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationType.BLOCKING,
                pos5Kit1.getSampleReceiptValidations().iterator().next().getValidationType());
    }

    /**
     * Tests the case where the samples returned encompass 2 complete sample kits
     *
     * @throws Exception
     */
    public void testMultipleSamplesKitsValidation() throws Exception {

        Set<String> test4RequestSMIds = new HashSet<>();

        test4RequestSMIds.add(sample1Kit1);
        test4RequestSMIds.add(sample2Kit1);
        test4RequestSMIds.add(sample3Kit1);
        test4RequestSMIds.add(sample4Kit1);
        test4RequestSMIds.add(sample1Kit2);
        test4RequestSMIds.add(sample2Kit2);
        test4RequestSMIds.add(sample3Kit2);
        test4RequestSMIds.add(sample4Kit2);

        BSPSampleReceiptService stubReceiptService = new BSPSampleReceiptServiceStub();
        BSPManagerFactory mockManagerFactory = Mockito.mock(BSPManagerFactory.class);
        SampleManager mockSampManager = Mockito.mock(SampleManager.class);
        SampleReceiptResource sampleReceiptResource = Mockito.mock(SampleReceiptResource.class);
        BSPRestService bspRestService = Mockito.mock(BSPRestService.class);

        SampleKitListResponse mockResponse = new SampleKitListResponse();
        List<SampleKit> skResponseKits = new ArrayList<>();
        skResponseKits.add(sampleKit1);
        skResponseKits.add(sampleKit2);
        mockResponse.setResult(skResponseKits);
        mockResponse.setSuccess(true);
        Mockito.when(mockSampManager.getSampleKitsBySampleIds(Mockito.eq(test4RequestSMIds))).thenReturn(mockResponse);
        Mockito.when(mockManagerFactory.createSampleManager()).thenReturn(mockSampManager);
        ProductOrderSampleDao mockPosDao = Mockito.mock(ProductOrderSampleDao.class);

        Map<String, Set<ProductOrderSample>> posResult1 = new HashMap<>();
        posResult1.put(sample1Kit1, Collections.singleton(pos1Kit1));
        posResult1.put(sample2Kit1, Collections.singleton(pos2Kit1));
        posResult1.put(sample3Kit1, Collections.singleton(pos3Kit1));
        posResult1.put(sample4Kit1, Collections.singleton(pos4Kit1));
        posResult1.put(sample1Kit2, Collections.singleton(pos1Kit2));
        posResult1.put(sample2Kit2, Collections.singleton(pos2Kit2));
        posResult1.put(sample3Kit2, Collections.singleton(pos3Kit2));
        posResult1.put(sample4Kit2, Collections.singleton(pos4Kit2));

        Mockito.when(mockPosDao.findMapBySamples(Mockito.eq(test4RequestSMIds))).thenReturn(posResult1);

        BSPUserList testUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());

        LabVesselDao labVesselDao = Mockito.mock(LabVesselDao.class);
        QueueEjb queueEjb = Mockito.mock(QueueEjb.class);
        DnaQuantEnqueueOverride dnaQuantEnqueueOverride = Mockito.mock(DnaQuantEnqueueOverride.class);
        ReceiveSamplesEjb testEjb = new ReceiveSamplesEjb(stubReceiptService, mockManagerFactory, mockPosDao,
                testUserList, bspRestService, sampleReceiptResource, labVesselDao, queueEjb, dnaQuantEnqueueOverride);
// TODO: Review this to msee what needs to be mocked on labVesselDao and QueueEjb
        MessageCollection validationResults = new MessageCollection();

        testEjb.validateForReceipt(test4RequestSMIds, validationResults, "scottmat");

        Assert.assertFalse(validationResults.hasErrors());
        Assert.assertFalse(validationResults.hasInfos());
        Assert.assertTrue(validationResults.hasWarnings());

        Assert.assertFalse(pos1Kit1.getSampleReceiptValidations().isEmpty());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationReason.SAMPLES_FROM_MULTIPLE_KITS,
                pos1Kit1.getSampleReceiptValidations().iterator().next().getReason());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationType.WARNING,
                pos1Kit1.getSampleReceiptValidations().iterator().next().getValidationType());
        Assert.assertFalse(pos2Kit1.getSampleReceiptValidations().isEmpty());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationReason.SAMPLES_FROM_MULTIPLE_KITS,
                pos2Kit1.getSampleReceiptValidations().iterator().next().getReason());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationType.WARNING,
                pos2Kit1.getSampleReceiptValidations().iterator().next().getValidationType());
        Assert.assertFalse(pos3Kit1.getSampleReceiptValidations().isEmpty());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationReason.SAMPLES_FROM_MULTIPLE_KITS,
                pos3Kit1.getSampleReceiptValidations().iterator().next().getReason());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationType.WARNING,
                pos3Kit1.getSampleReceiptValidations().iterator().next().getValidationType());
        Assert.assertFalse(pos4Kit1.getSampleReceiptValidations().isEmpty());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationReason.SAMPLES_FROM_MULTIPLE_KITS,
                pos4Kit1.getSampleReceiptValidations().iterator().next().getReason());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationType.WARNING,
                pos4Kit1.getSampleReceiptValidations().iterator().next().getValidationType());
        Assert.assertFalse(pos1Kit2.getSampleReceiptValidations().isEmpty());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationReason.SAMPLES_FROM_MULTIPLE_KITS,
                pos1Kit2.getSampleReceiptValidations().iterator().next().getReason());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationType.WARNING,
                pos1Kit2.getSampleReceiptValidations().iterator().next().getValidationType());
        Assert.assertFalse(pos2Kit2.getSampleReceiptValidations().isEmpty());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationReason.SAMPLES_FROM_MULTIPLE_KITS,
                pos2Kit2.getSampleReceiptValidations().iterator().next().getReason());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationType.WARNING,
                pos2Kit2.getSampleReceiptValidations().iterator().next().getValidationType());
        Assert.assertFalse(pos3Kit2.getSampleReceiptValidations().isEmpty());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationReason.SAMPLES_FROM_MULTIPLE_KITS,
                pos3Kit2.getSampleReceiptValidations().iterator().next().getReason());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationType.WARNING,
                pos3Kit2.getSampleReceiptValidations().iterator().next().getValidationType());
        Assert.assertFalse(pos4Kit2.getSampleReceiptValidations().isEmpty());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationReason.SAMPLES_FROM_MULTIPLE_KITS,
                pos4Kit2.getSampleReceiptValidations().iterator().next().getReason());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationType.WARNING,
                pos4Kit2.getSampleReceiptValidations().iterator().next().getValidationType());
    }

    /**
     * Tests the case where not only are not all samples from the perspective sample kits returned, but also the samples
     * in the receipt set represent 2 separate and independent sample kits
     *
     * @throws Exception
     */
    public void testMultipleIncompleteSampleKitsValidation() throws Exception {

        Set<String> test5RequestSMIds = new HashSet<>();

        test5RequestSMIds.add(sample1Kit1);
        test5RequestSMIds.add(sample3Kit1);
        test5RequestSMIds.add(sample4Kit1);
        test5RequestSMIds.add(sample5Kit1);
        test5RequestSMIds.add(sample1Kit2);
        test5RequestSMIds.add(sample2Kit2);
        test5RequestSMIds.add(sample4Kit2);

        BSPSampleReceiptService stubReceiptService = new BSPSampleReceiptServiceStub();
        BSPManagerFactory mockManagerFactory = Mockito.mock(BSPManagerFactory.class);
        SampleManager mockSampManager = Mockito.mock(SampleManager.class);
        SampleReceiptResource sampleReceiptResource = Mockito.mock(SampleReceiptResource.class);
        BSPRestService bspRestService = Mockito.mock(BSPRestService.class);
        BSPRestClient bspRestClient = Mockito.mock(BSPRestClient.class);

        SampleKitListResponse mockResponse = new SampleKitListResponse();
        List<SampleKit> skResponseKits = new ArrayList<>();
        skResponseKits.add(sampleKit1);
        skResponseKits.add(sampleKit2);
        mockResponse.setResult(skResponseKits);
        mockResponse.setSuccess(true);
        Mockito.when(mockSampManager.getSampleKitsBySampleIds(Mockito.eq(test5RequestSMIds))).thenReturn(mockResponse);
        Mockito.when(mockManagerFactory.createSampleManager()).thenReturn(mockSampManager);
        ProductOrderSampleDao mockPosDao = Mockito.mock(ProductOrderSampleDao.class);

        Map<String, Set<ProductOrderSample>> posResult1 = new HashMap<>();
        posResult1.put(sample1Kit1, Collections.singleton(pos1Kit1));
        posResult1.put(sample3Kit1, Collections.singleton(pos3Kit1));
        posResult1.put(sample4Kit1, Collections.singleton(pos4Kit1));
        posResult1.put(sample5Kit1, Collections.singleton(pos5Kit1));
        posResult1.put(sample1Kit2, Collections.singleton(pos1Kit2));
        posResult1.put(sample2Kit2, Collections.singleton(pos2Kit2));
        posResult1.put(sample4Kit2, Collections.singleton(pos4Kit2));

        Mockito.when(mockPosDao.findMapBySamples(Mockito.eq(test5RequestSMIds))).thenReturn(posResult1);

        BSPUserList testUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());

        LabVesselDao labVesselDao = Mockito.mock(LabVesselDao.class);
        QueueEjb queueEjb = Mockito.mock(QueueEjb.class);
        DnaQuantEnqueueOverride dnaQuantEnqueueOverride = Mockito.mock(DnaQuantEnqueueOverride.class);
        ReceiveSamplesEjb testEjb = new ReceiveSamplesEjb(stubReceiptService, mockManagerFactory, mockPosDao,
                testUserList, bspRestService, sampleReceiptResource, labVesselDao, queueEjb, dnaQuantEnqueueOverride);
// TODO: Review this to msee what needs to be mocked on labVesselDao and QueueEjb
        MessageCollection validationResults = new MessageCollection();

        testEjb.validateForReceipt(test5RequestSMIds, validationResults, "scottmat");

        Set<SampleReceiptValidation.SampleValidationReason> expectedReasons = new HashSet<>();
        expectedReasons.add(SampleReceiptValidation.SampleValidationReason.MISSING_SAMPLE_FROM_SAMPLE_KIT);
        expectedReasons.add(SampleReceiptValidation.SampleValidationReason.SAMPLES_FROM_MULTIPLE_KITS);

        Assert.assertTrue(validationResults.hasErrors());
        Assert.assertFalse(validationResults.hasInfos());
        Assert.assertTrue(validationResults.hasWarnings());

        Assert.assertFalse(pos1Kit1.getSampleReceiptValidations().isEmpty());
        Assert.assertEquals(2, pos1Kit1.getSampleReceiptValidations().size());
        Assert.assertTrue(pos2Kit1.getSampleReceiptValidations().isEmpty());
        Assert.assertFalse(pos3Kit1.getSampleReceiptValidations().isEmpty());
        Assert.assertEquals(2, pos3Kit1.getSampleReceiptValidations().size());
        Assert.assertFalse(pos4Kit1.getSampleReceiptValidations().isEmpty());
        Assert.assertEquals(2, pos4Kit1.getSampleReceiptValidations().size());

        Assert.assertFalse(pos5Kit1.getSampleReceiptValidations().isEmpty());
        Assert.assertEquals(2, pos5Kit1.getSampleReceiptValidations().size());

        Assert.assertFalse(pos1Kit2.getSampleReceiptValidations().isEmpty());
        Assert.assertEquals(2, pos1Kit2.getSampleReceiptValidations().size());
        Assert.assertFalse(pos2Kit2.getSampleReceiptValidations().isEmpty());
        Assert.assertEquals(2, pos2Kit2.getSampleReceiptValidations().size());
        Assert.assertTrue(pos3Kit2.getSampleReceiptValidations().isEmpty());
        Assert.assertFalse(pos4Kit2.getSampleReceiptValidations().isEmpty());
        Assert.assertEquals(2, pos4Kit2.getSampleReceiptValidations().size());

        int notInBspCounter = 0;
        int missingSampleCounter = 0;
        int multipleKitCounter = 0;
        for (Map.Entry<String, Set<ProductOrderSample>> posForValidaiton : posResult1.entrySet()) {
            for (ProductOrderSample currentPOS : posForValidaiton.getValue()) {
                for (SampleReceiptValidation currentValidation : currentPOS.getSampleReceiptValidations()) {
                    if (!currentPOS.getName().equals(sample5Kit1)) {
                        Assert.assertTrue(expectedReasons.contains(currentValidation.getReason()));
                    }
                    switch (currentValidation.getReason()) {
                    case MISSING_SAMPLE_FROM_SAMPLE_KIT:
                        missingSampleCounter++;
                        Assert.assertEquals(SampleReceiptValidation.SampleValidationType.WARNING,
                                currentValidation.getValidationType());
                        if (currentPOS.getName().equals(sample5Kit1)) {
                            Assert.fail();
                        }
                        break;
                    case SAMPLE_NOT_IN_BSP:
                        notInBspCounter++;
                        if (!currentPOS.getName().equals(sample5Kit1)) {
                            Assert.fail();
                        }
                        Assert.assertEquals(SampleReceiptValidation.SampleValidationType.BLOCKING,
                                currentValidation.getValidationType());
                        break;

                    case SAMPLES_FROM_MULTIPLE_KITS:
                        multipleKitCounter++;
                        Assert.assertEquals(SampleReceiptValidation.SampleValidationType.WARNING,
                                currentValidation.getValidationType());
                        break;
                    }
                }
            }
        }

        Assert.assertEquals(1, notInBspCounter);
        Assert.assertEquals(6, missingSampleCounter);
        Assert.assertEquals(7, multipleKitCounter);

    }

    @AfterMethod(groups = TestGroups.DATABASE_FREE)
    public void tearDown() throws Exception {
    }
}
