package org.broadinstitute.gpinformatics.athena.boundary.orders;

import junit.framework.Assert;
import org.broadinstitute.bsp.client.response.SampleKitListResponse;
import org.broadinstitute.bsp.client.sample.Sample;
import org.broadinstitute.bsp.client.sample.SampleKit;
import org.broadinstitute.bsp.client.sample.SampleManager;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.samples.SampleReceiptValidation;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleReceiptService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleReceiptServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactory;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Test(groups = TestGroups.DATABASE_FREE)
public class ReceiveSamplesEjbDBFreeTest {


    private String sampleKit1ID;
    private String sampleKit2ID;
    private List<Sample> kit1Samples;
    private SampleKit sampleKit2;
    private ProductOrderSample pos1Kit1;
    private ProductOrderSample pos2Kit1;
    private ProductOrderSample pos3Kit1;
    private ProductOrderSample pos4Kit1;
    private ProductOrderSample pos1Kit2;
    private ProductOrderSample pos2Kit2;
    private ProductOrderSample pos3Kit2;
    private ProductOrderSample pos4Kit2;
    private String sample1Kit1;
    private String sample2Kit1;
    private String sample3Kit1;
    private String sample4Kit1;
    private String sample1Kit2;
    private String sample2Kit2;
    private String sample3Kit2;
    private String sample4Kit2;
    private SampleKit sampleKit1;
    private String sample5Kit1;
    private ProductOrderSample pos5Kit1;

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() throws Exception {
        sampleKit1ID = "SK-tst1";
        sampleKit2ID = "SK-tst2";

        sample1Kit1 = (Deployment.isCRSP ? "CSM" : "SM") + "-1SK1";
        sample2Kit1 = (Deployment.isCRSP ? "CSM" : "SM") + "-2SK1";
        sample3Kit1 = (Deployment.isCRSP ? "CSM" : "SM") + "-3SK1";
        sample4Kit1 = (Deployment.isCRSP ? "CSM" : "SM") + "-4SK1";
        sample5Kit1 = (Deployment.isCRSP ? "CSM" : "SM") + "-5SK1";

        sampleKit1 = new SampleKit();
        sampleKit1.setSampleKitId(sampleKit1ID);

        kit1Samples = new ArrayList<>();
        Collections.addAll(kit1Samples, new Sample(sample1Kit1), new Sample(sample2Kit1), new Sample(sample3Kit1),
                new Sample(sample4Kit1));

        sampleKit1.setSamples(kit1Samples);

        sample1Kit2 = (Deployment.isCRSP ? "CSM" : "SM") + "-1SK2";
        sample2Kit2 = (Deployment.isCRSP ? "CSM" : "SM") + "-2SK2";
        sample3Kit2 = (Deployment.isCRSP ? "CSM" : "SM") + "-3SK2";
        sample4Kit2 = (Deployment.isCRSP ? "CSM" : "SM") + "-4SK2";

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

        List<String> test1RequestList = new ArrayList<>();

        test1RequestList.add(sample1Kit1);
        test1RequestList.add(sample2Kit1);
        test1RequestList.add(sample3Kit1);
        test1RequestList.add(sample4Kit1);

        BSPSampleReceiptService stubReceiptService = BSPSampleReceiptServiceProducer.stubInstance();
        BSPManagerFactory mockManagerFactory = Mockito.mock(BSPManagerFactory.class);
        SampleManager mockSampManager = Mockito.mock(SampleManager.class);

        SampleKitListResponse mockResponse = new SampleKitListResponse();
        mockResponse.setResult(Collections.singletonList(sampleKit1));
        mockResponse.setSuccess(true);
        Mockito.when(mockSampManager.getSampleKitsBySampleIds(Mockito.eq(test1RequestList))).thenReturn(mockResponse);
        Mockito.when(mockManagerFactory.createSampleManager()).thenReturn(mockSampManager);
        ProductOrderSampleDao mockPosDao = Mockito.mock(ProductOrderSampleDao.class);

        Map<String, List<ProductOrderSample>> posResult = new HashMap<>();
        posResult.put(sample1Kit1, Collections.singletonList(pos1Kit1));
        posResult.put(sample2Kit1, Collections.singletonList(pos2Kit1));
        posResult.put(sample3Kit1, Collections.singletonList(pos3Kit1));
        posResult.put(sample4Kit1, Collections.singletonList(pos4Kit1));
        Mockito.when(mockPosDao.findMapBySamples(Mockito.eq(test1RequestList))).thenReturn(posResult);

        BSPUserList testUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());

        ReceiveSamplesEjb testEjb =
                new ReceiveSamplesEjb(stubReceiptService, mockManagerFactory, mockPosDao, testUserList);

        MessageCollection validationResults = new MessageCollection();

        testEjb.validateForReceipt(test1RequestList, validationResults, "scottmat");

        Assert.assertFalse(validationResults.hasErrors());
        Assert.assertFalse(validationResults.hasInfos());
        Assert.assertFalse(validationResults.hasWarnings());

        Assert.assertTrue(pos1Kit1.getValidations().isEmpty());
        Assert.assertTrue(pos2Kit1.getValidations().isEmpty());
        Assert.assertTrue(pos3Kit1.getValidations().isEmpty());
        Assert.assertTrue(pos4Kit1.getValidations().isEmpty());
    }

    /**
     * Tests the case where less samples are received than are defined in the Sample kit with which they are associated
     *
     * @throws Exception
     */
    public void testPartialSamplesKit1Validation() throws Exception {

        List<String> test1RequestList = new ArrayList<>();

        test1RequestList.add(sample1Kit1);
        test1RequestList.add(sample2Kit1);
        test1RequestList.add(sample3Kit1);

        BSPSampleReceiptService stubReceiptService = BSPSampleReceiptServiceProducer.stubInstance();
        BSPManagerFactory mockManagerFactory = Mockito.mock(BSPManagerFactory.class);
        SampleManager mockSampManager = Mockito.mock(SampleManager.class);

        SampleKitListResponse mockResponse = new SampleKitListResponse();
        mockResponse.setResult(Collections.singletonList(sampleKit1));
        mockResponse.setSuccess(true);
        Mockito.when(mockSampManager.getSampleKitsBySampleIds(Mockito.eq(test1RequestList))).thenReturn(mockResponse);
        Mockito.when(mockManagerFactory.createSampleManager()).thenReturn(mockSampManager);
        ProductOrderSampleDao mockPosDao = Mockito.mock(ProductOrderSampleDao.class);

        Map<String, List<ProductOrderSample>> posResult = new HashMap<>();
        posResult.put(sample1Kit1, Collections.singletonList(pos1Kit1));
        posResult.put(sample2Kit1, Collections.singletonList(pos2Kit1));
        posResult.put(sample3Kit1, Collections.singletonList(pos3Kit1));
        Mockito.when(mockPosDao.findMapBySamples(Mockito.eq(test1RequestList))).thenReturn(posResult);

        BSPUserList testUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());

        ReceiveSamplesEjb testEjb =
                new ReceiveSamplesEjb(stubReceiptService, mockManagerFactory, mockPosDao, testUserList);

        MessageCollection validationResults = new MessageCollection();

        testEjb.validateForReceipt(test1RequestList, validationResults, "scottmat");

        Assert.assertFalse(validationResults.hasErrors());
        Assert.assertFalse(validationResults.hasInfos());
        Assert.assertTrue(validationResults.hasWarnings());

        Assert.assertFalse(pos1Kit1.getValidations().isEmpty());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationReason.MISSING_SAMPLE_FROM_SAMPLE_KIT,
                pos1Kit1.getValidations().iterator().next().getReason());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationType.WARNING,
                pos1Kit1.getValidations().iterator().next().getValidationType());
        Assert.assertFalse(pos2Kit1.getValidations().isEmpty());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationReason.MISSING_SAMPLE_FROM_SAMPLE_KIT,
                pos2Kit1.getValidations().iterator().next().getReason());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationType.WARNING,
                pos2Kit1.getValidations().iterator().next().getValidationType());
        Assert.assertFalse(pos3Kit1.getValidations().isEmpty());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationReason.MISSING_SAMPLE_FROM_SAMPLE_KIT,
                pos3Kit1.getValidations().iterator().next().getReason());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationType.WARNING,
                pos3Kit1.getValidations().iterator().next().getValidationType());
        Assert.assertTrue(pos4Kit1.getValidations().isEmpty());
    }

    /**
     * Test the case where a sample is returned that is not associated with any sample kit in BSP
     *
     * @throws Exception
     */
    public void testUnrecognizedSamplesKit1Validation() throws Exception {

        List<String> test1RequestList = new ArrayList<>();

        test1RequestList.add(sample1Kit1);
        test1RequestList.add(sample2Kit1);
        test1RequestList.add(sample3Kit1);
        test1RequestList.add(sample4Kit1);
        test1RequestList.add(sample5Kit1);

        BSPSampleReceiptService stubReceiptService = BSPSampleReceiptServiceProducer.stubInstance();
        BSPManagerFactory mockManagerFactory = Mockito.mock(BSPManagerFactory.class);
        SampleManager mockSampManager = Mockito.mock(SampleManager.class);

        SampleKitListResponse mockResponse = new SampleKitListResponse();
        mockResponse.setResult(Collections.singletonList(sampleKit1));
        mockResponse.setSuccess(true);
        Mockito.when(mockSampManager.getSampleKitsBySampleIds(Mockito.eq(test1RequestList))).thenReturn(mockResponse);
        Mockito.when(mockManagerFactory.createSampleManager()).thenReturn(mockSampManager);
        ProductOrderSampleDao mockPosDao = Mockito.mock(ProductOrderSampleDao.class);

        Map<String, List<ProductOrderSample>> posResult1 = new HashMap<>();
        posResult1.put(sample5Kit1, Collections.singletonList(pos5Kit1));
        Mockito.when(mockPosDao.findMapBySamples(Mockito.eq(Collections.singletonList(sample5Kit1))))
                .thenReturn(posResult1);

        Map<String, List<ProductOrderSample>> posResult2 = new HashMap<>();
        posResult2.put(sample1Kit1, Collections.singletonList(pos1Kit1));
        posResult2.put(sample2Kit1, Collections.singletonList(pos2Kit1));
        posResult2.put(sample3Kit1, Collections.singletonList(pos3Kit1));
        posResult2.put(sample4Kit1, Collections.singletonList(pos4Kit1));
        posResult2.put(sample5Kit1, Collections.singletonList(pos5Kit1));
        List<String> productOrderSampleRequest2 = new ArrayList<>();
        Collections.addAll(productOrderSampleRequest2, sample1Kit1, sample2Kit1, sample3Kit1, sample4Kit1, sample5Kit1);
        Mockito.when(mockPosDao.findMapBySamples(Mockito.eq(productOrderSampleRequest2))).thenReturn(posResult2);

        BSPUserList testUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());

        ReceiveSamplesEjb testEjb =
                new ReceiveSamplesEjb(stubReceiptService, mockManagerFactory, mockPosDao, testUserList);

        MessageCollection validationResults = new MessageCollection();

        testEjb.validateForReceipt(test1RequestList, validationResults, "scottmat");

        Assert.assertTrue(validationResults.hasErrors());
        Assert.assertFalse(validationResults.hasInfos());
        Assert.assertFalse(validationResults.hasWarnings());

        Assert.assertTrue(pos1Kit1.getValidations().isEmpty());
        Assert.assertTrue(pos2Kit1.getValidations().isEmpty());
        Assert.assertTrue(pos3Kit1.getValidations().isEmpty());
        Assert.assertTrue(pos4Kit1.getValidations().isEmpty());
        Assert.assertFalse(pos5Kit1.getValidations().isEmpty());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationReason.SAMPLE_NOT_IN_BSP,
                pos5Kit1.getValidations().iterator().next().getReason());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationType.BLOCKING,
                pos5Kit1.getValidations().iterator().next().getValidationType());
    }

    /**
     * Tests the case where the samples returned encompass 2 complete sample kits
     *
     * @throws Exception
     */
    public void testMultipleSamplesKitsValidation() throws Exception {

        List<String> test1RequestList = new ArrayList<>();

        test1RequestList.add(sample1Kit1);
        test1RequestList.add(sample2Kit1);
        test1RequestList.add(sample3Kit1);
        test1RequestList.add(sample4Kit1);
        test1RequestList.add(sample1Kit2);
        test1RequestList.add(sample2Kit2);
        test1RequestList.add(sample3Kit2);
        test1RequestList.add(sample4Kit2);

        BSPSampleReceiptService stubReceiptService = BSPSampleReceiptServiceProducer.stubInstance();
        BSPManagerFactory mockManagerFactory = Mockito.mock(BSPManagerFactory.class);
        SampleManager mockSampManager = Mockito.mock(SampleManager.class);

        SampleKitListResponse mockResponse = new SampleKitListResponse();
        List<SampleKit> skResponseKits = new ArrayList<>();
        skResponseKits.add(sampleKit1);
        skResponseKits.add(sampleKit2);
        mockResponse.setResult(skResponseKits);
        mockResponse.setSuccess(true);
        Mockito.when(mockSampManager.getSampleKitsBySampleIds(Mockito.eq(test1RequestList))).thenReturn(mockResponse);
        Mockito.when(mockManagerFactory.createSampleManager()).thenReturn(mockSampManager);
        ProductOrderSampleDao mockPosDao = Mockito.mock(ProductOrderSampleDao.class);

        Map<String, List<ProductOrderSample>> posResult1 = new HashMap<>();
        posResult1.put(sample1Kit1, Collections.singletonList(pos1Kit1));
        posResult1.put(sample2Kit1, Collections.singletonList(pos2Kit1));
        posResult1.put(sample3Kit1, Collections.singletonList(pos3Kit1));
        posResult1.put(sample4Kit1, Collections.singletonList(pos4Kit1));

        List<String> productOrderSampleRequest1 = new ArrayList<>();
        Collections.addAll(productOrderSampleRequest1, sample1Kit1, sample2Kit1, sample3Kit1, sample4Kit1);
        Mockito.when(mockPosDao.findMapBySamples(Mockito.eq(productOrderSampleRequest1))).thenReturn(posResult1);

        Map<String, List<ProductOrderSample>> posResult2 = new HashMap<>();
        posResult2.put(sample1Kit2, Collections.singletonList(pos1Kit2));
        posResult2.put(sample2Kit2, Collections.singletonList(pos2Kit2));
        posResult2.put(sample3Kit2, Collections.singletonList(pos3Kit2));
        posResult2.put(sample4Kit2, Collections.singletonList(pos4Kit2));

        List<String> productOrderSampleRequest2 = new ArrayList<>();
        Collections.addAll(productOrderSampleRequest2, sample1Kit2, sample2Kit2, sample3Kit2, sample4Kit2);
        Mockito.when(mockPosDao.findMapBySamples(Mockito.eq(productOrderSampleRequest2))).thenReturn(posResult2);

        Map<String, List<ProductOrderSample>> posResult3 = new HashMap<>();
        posResult3.put(sample1Kit1, Collections.singletonList(pos1Kit1));
        posResult3.put(sample2Kit1, Collections.singletonList(pos2Kit1));
        posResult3.put(sample3Kit1, Collections.singletonList(pos3Kit1));
        posResult3.put(sample4Kit1, Collections.singletonList(pos4Kit1));
        posResult3.put(sample1Kit2, Collections.singletonList(pos1Kit2));
        posResult3.put(sample2Kit2, Collections.singletonList(pos2Kit2));
        posResult3.put(sample3Kit2, Collections.singletonList(pos3Kit2));
        posResult3.put(sample4Kit2, Collections.singletonList(pos4Kit2));

        List<String> productOrderSampleRequest3 = new ArrayList<>();
        Collections.addAll(productOrderSampleRequest3, sample1Kit1, sample2Kit1, sample3Kit1, sample4Kit1, sample1Kit2,
                sample2Kit2, sample3Kit2, sample4Kit2);
        Mockito.when(mockPosDao.findMapBySamples(Mockito.eq(productOrderSampleRequest3))).thenReturn(posResult3);


        BSPUserList testUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());

        ReceiveSamplesEjb testEjb =
                new ReceiveSamplesEjb(stubReceiptService, mockManagerFactory, mockPosDao, testUserList);

        MessageCollection validationResults = new MessageCollection();

        testEjb.validateForReceipt(test1RequestList, validationResults, "scottmat");

        Assert.assertTrue(validationResults.hasErrors());
        Assert.assertFalse(validationResults.hasInfos());
        Assert.assertFalse(validationResults.hasWarnings());

        Assert.assertFalse(pos1Kit1.getValidations().isEmpty());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationReason.SAMPLES_FROM_MULTIPLE_KITS,
                pos1Kit1.getValidations().iterator().next().getReason());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationType.BLOCKING,
                pos1Kit1.getValidations().iterator().next().getValidationType());
        Assert.assertFalse(pos2Kit1.getValidations().isEmpty());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationReason.SAMPLES_FROM_MULTIPLE_KITS,
                pos2Kit1.getValidations().iterator().next().getReason());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationType.BLOCKING,
                pos2Kit1.getValidations().iterator().next().getValidationType());
        Assert.assertFalse(pos3Kit1.getValidations().isEmpty());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationReason.SAMPLES_FROM_MULTIPLE_KITS,
                pos3Kit1.getValidations().iterator().next().getReason());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationType.BLOCKING,
                pos3Kit1.getValidations().iterator().next().getValidationType());
        Assert.assertFalse(pos4Kit1.getValidations().isEmpty());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationReason.SAMPLES_FROM_MULTIPLE_KITS,
                pos4Kit1.getValidations().iterator().next().getReason());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationType.BLOCKING,
                pos4Kit1.getValidations().iterator().next().getValidationType());
        Assert.assertFalse(pos1Kit2.getValidations().isEmpty());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationReason.SAMPLES_FROM_MULTIPLE_KITS,
                pos1Kit2.getValidations().iterator().next().getReason());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationType.BLOCKING,
                pos1Kit2.getValidations().iterator().next().getValidationType());
        Assert.assertFalse(pos2Kit2.getValidations().isEmpty());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationReason.SAMPLES_FROM_MULTIPLE_KITS,
                pos2Kit2.getValidations().iterator().next().getReason());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationType.BLOCKING,
                pos2Kit2.getValidations().iterator().next().getValidationType());
        Assert.assertFalse(pos3Kit2.getValidations().isEmpty());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationReason.SAMPLES_FROM_MULTIPLE_KITS,
                pos3Kit2.getValidations().iterator().next().getReason());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationType.BLOCKING,
                pos3Kit2.getValidations().iterator().next().getValidationType());
        Assert.assertFalse(pos4Kit2.getValidations().isEmpty());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationReason.SAMPLES_FROM_MULTIPLE_KITS,
                pos4Kit2.getValidations().iterator().next().getReason());
        Assert.assertEquals(SampleReceiptValidation.SampleValidationType.BLOCKING,
                pos4Kit2.getValidations().iterator().next().getValidationType());
    }

    /**
     * Tests the case where not only are not all samples from the perspective sample kits returned, but also the samples
     * in the receipt set represent 2 separate and independent sample kits
     *
     * @throws Exception
     */
    public void testMultipleIncompleteSampleKitsValidation() throws Exception {

        List<String> test1RequestList = new ArrayList<>();

        test1RequestList.add(sample1Kit1);
        test1RequestList.add(sample3Kit1);
        test1RequestList.add(sample4Kit1);
        test1RequestList.add(sample5Kit1);
        test1RequestList.add(sample1Kit2);
        test1RequestList.add(sample2Kit2);
        test1RequestList.add(sample4Kit2);

        BSPSampleReceiptService stubReceiptService = BSPSampleReceiptServiceProducer.stubInstance();
        BSPManagerFactory mockManagerFactory = Mockito.mock(BSPManagerFactory.class);
        SampleManager mockSampManager = Mockito.mock(SampleManager.class);

        SampleKitListResponse mockResponse = new SampleKitListResponse();
        List<SampleKit> skResponseKits = new ArrayList<>();
        skResponseKits.add(sampleKit1);
        skResponseKits.add(sampleKit2);
        mockResponse.setResult(skResponseKits);
        mockResponse.setSuccess(true);
        Mockito.when(mockSampManager.getSampleKitsBySampleIds(Mockito.eq(test1RequestList))).thenReturn(mockResponse);
        Mockito.when(mockManagerFactory.createSampleManager()).thenReturn(mockSampManager);
        ProductOrderSampleDao mockPosDao = Mockito.mock(ProductOrderSampleDao.class);

        Map<String, List<ProductOrderSample>> posResult1 = new HashMap<>();
        posResult1.put(sample1Kit1, Collections.singletonList(pos1Kit1));
        posResult1.put(sample3Kit1, Collections.singletonList(pos3Kit1));
        posResult1.put(sample4Kit1, Collections.singletonList(pos4Kit1));

        List<String> productOrderSampleRequest1 = new ArrayList<>();
        Collections.addAll(productOrderSampleRequest1, sample1Kit1, sample3Kit1, sample4Kit1);
        Mockito.when(mockPosDao.findMapBySamples(Mockito.eq(productOrderSampleRequest1))).thenReturn(posResult1);

        Map<String, List<ProductOrderSample>> posResult2 = new HashMap<>();

        posResult2.put(sample1Kit1, Collections.singletonList(pos1Kit1));
        posResult2.put(sample2Kit1, Collections.singletonList(pos2Kit1));
        posResult2.put(sample3Kit1, Collections.singletonList(pos3Kit1));
        posResult2.put(sample4Kit1, Collections.singletonList(pos4Kit1));
        posResult2.put(sample5Kit1, Collections.singletonList(pos5Kit1));

        List<String> productOrderSampleRequest2 = new ArrayList<>();
        Collections.addAll(productOrderSampleRequest2, sample1Kit1, sample2Kit1, sample3Kit1, sample4Kit1, sample5Kit1);
        Mockito.when(mockPosDao.findMapBySamples(Mockito.eq(productOrderSampleRequest2))).thenReturn(posResult2);

        Map<String, List<ProductOrderSample>> posResult3 = new HashMap<>();
        posResult3.put(sample1Kit2, Collections.singletonList(pos1Kit2));
        posResult3.put(sample2Kit2, Collections.singletonList(pos2Kit2));
        posResult3.put(sample4Kit2, Collections.singletonList(pos4Kit2));

        List<String> productOrderSampleRequest3 = new ArrayList<>();
        Collections.addAll(productOrderSampleRequest3, sample1Kit2, sample2Kit2, sample4Kit2);
        Mockito.when(mockPosDao.findMapBySamples(Mockito.eq(productOrderSampleRequest3))).thenReturn(posResult3);

        Map<String, List<ProductOrderSample>> posResult4 = new HashMap<>();
        posResult4.put(sample1Kit2, Collections.singletonList(pos1Kit2));
        posResult4.put(sample2Kit2, Collections.singletonList(pos2Kit2));
        posResult4.put(sample3Kit2, Collections.singletonList(pos3Kit2));
        posResult4.put(sample4Kit2, Collections.singletonList(pos4Kit2));

        List<String> productOrderSampleRequest4 = new ArrayList<>();
        Collections.addAll(productOrderSampleRequest4, sample1Kit2, sample2Kit2, sample3Kit2, sample4Kit2);
        Mockito.when(mockPosDao.findMapBySamples(Mockito.eq(productOrderSampleRequest4))).thenReturn(posResult4);

        Map<String, List<ProductOrderSample>> posResult5 = new HashMap<>();
        posResult5.put(sample1Kit1, Collections.singletonList(pos1Kit1));
        posResult5.put(sample3Kit1, Collections.singletonList(pos3Kit1));
        posResult5.put(sample4Kit1, Collections.singletonList(pos4Kit1));
        posResult5.put(sample5Kit1, Collections.singletonList(pos5Kit1));
        posResult5.put(sample1Kit2, Collections.singletonList(pos1Kit2));
        posResult5.put(sample2Kit2, Collections.singletonList(pos2Kit2));
        posResult5.put(sample4Kit2, Collections.singletonList(pos4Kit2));

        List<String> productOrderSampleRequest5 = new ArrayList<>();
        Collections.addAll(productOrderSampleRequest5, sample1Kit1, sample3Kit1, sample4Kit1, sample5Kit1, sample1Kit2,
                sample2Kit2,
                sample4Kit2);
        Mockito.when(mockPosDao.findMapBySamples(Mockito.eq(productOrderSampleRequest5))).thenReturn(posResult5);

        Map<String, List<ProductOrderSample>> posResult6 = new HashMap<>();
        posResult6.put(sample5Kit1, Collections.singletonList(pos5Kit1));
        Mockito.when(mockPosDao.findMapBySamples(Mockito.eq(Collections.singletonList(sample5Kit1))))
                .thenReturn(posResult6);


        BSPUserList testUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());

        ReceiveSamplesEjb testEjb =
                new ReceiveSamplesEjb(stubReceiptService, mockManagerFactory, mockPosDao, testUserList);

        MessageCollection validationResults = new MessageCollection();

        testEjb.validateForReceipt(test1RequestList, validationResults, "scottmat");

        List<SampleReceiptValidation.SampleValidationReason> expectedReasons = new ArrayList<>();
        expectedReasons.add(SampleReceiptValidation.SampleValidationReason.MISSING_SAMPLE_FROM_SAMPLE_KIT);
        expectedReasons.add(SampleReceiptValidation.SampleValidationReason.SAMPLES_FROM_MULTIPLE_KITS);


        Assert.assertTrue(validationResults.hasErrors());
        Assert.assertFalse(validationResults.hasInfos());
        Assert.assertTrue(validationResults.hasWarnings());

        Assert.assertFalse(pos1Kit1.getValidations().isEmpty());
        Assert.assertEquals(2, pos1Kit1.getValidations().size());
        Assert.assertTrue(pos2Kit1.getValidations().isEmpty());
        Assert.assertFalse(pos3Kit1.getValidations().isEmpty());
        Assert.assertEquals(2, pos3Kit1.getValidations().size());
        Assert.assertFalse(pos4Kit1.getValidations().isEmpty());
        Assert.assertEquals(2, pos4Kit1.getValidations().size());

        Assert.assertFalse(pos5Kit1.getValidations().isEmpty());
        Assert.assertEquals(2, pos5Kit1.getValidations().size());

        Assert.assertFalse(pos1Kit2.getValidations().isEmpty());
        Assert.assertEquals(2, pos1Kit2.getValidations().size());
        Assert.assertFalse(pos2Kit2.getValidations().isEmpty());
        Assert.assertEquals(2, pos2Kit2.getValidations().size());
        Assert.assertTrue(pos3Kit2.getValidations().isEmpty());
        Assert.assertFalse(pos4Kit2.getValidations().isEmpty());
        Assert.assertEquals(2, pos4Kit2.getValidations().size());

        int notInBspCounter = 0;
        int missingSampleCounter = 0;
        int multipleKitCounter = 0;

        for (Map.Entry<String, List<ProductOrderSample>> posForValidaiton : posResult5.entrySet()) {
            for (ProductOrderSample currentPOS : posForValidaiton.getValue()) {
                for (SampleReceiptValidation currentValidation : currentPOS.getValidations()) {
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
                        Assert.assertEquals(SampleReceiptValidation.SampleValidationType.BLOCKING,
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
