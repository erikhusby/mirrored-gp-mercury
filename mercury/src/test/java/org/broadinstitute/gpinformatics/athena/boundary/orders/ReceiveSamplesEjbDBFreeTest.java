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

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() throws Exception {
        sampleKit1ID = "SK-tst1";
        sampleKit2ID = "SK-tst2";

        sample1Kit1 = "CSM-1SK1";
        sample2Kit1 = "CSM-2SK1";
        sample3Kit1 = "CSM-3SK1";
        sample4Kit1 = "CSM-4SK1";

        sampleKit1 = new SampleKit();
        sampleKit1.setSampleKitId(sampleKit1ID);

        kit1Samples = new ArrayList<>();
        Collections.addAll(kit1Samples, new Sample(sample1Kit1), new Sample(sample2Kit1), new Sample(sample3Kit1),
                new Sample(sample4Kit1));

        sampleKit1.setSamples(kit1Samples);

        sample1Kit2 = "CSM-1SK2";
        sample2Kit2 = "CSM-2SK2";
        sample3Kit2 = "CSM-3SK2";
        sample4Kit2 = "CSM-4SK2";

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
    }

    /**
     * Tests the case where exactly the same samples that are in a sample kit come in for receipt.  This is the "Happy"
     * test case
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
        Assert.assertFalse(pos2Kit1.getValidations().isEmpty());
        Assert.assertFalse(pos3Kit1.getValidations().isEmpty());
        Assert.assertTrue(pos4Kit1.getValidations().isEmpty());
    }

    @AfterMethod(groups = TestGroups.DATABASE_FREE)
    public void tearDown() throws Exception {
    }
}
