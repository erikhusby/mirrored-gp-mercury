package org.broadinstitute.gpinformatics.athena.boundary.orders;

import junit.framework.Assert;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.control.dao.samples.SampleReceiptValidationDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.samples.SampleReceiptValidation;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryStub;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class ReceiveSamplesEjbTest extends ContainerTest {

    @Inject
    ProductOrderSampleDao productOrderSampleDao;
    @Inject
    ProductOrderDao productOrderDao;
    @Inject
    ResearchProjectDao researchProjectDao;
    @Inject
    ReceiveSamplesEjb receiveSamplesEjb;
    @Inject
    SampleReceiptValidationDao sampleReceiptValidationDao;
    @Inject
    BSPUserList bspUserList;
    private List<ProductOrderSample> samplesToDelete;
    private ProductOrder testProductOrder;

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {

        if(productOrderSampleDao == null) {
            return;
        }
        samplesToDelete = new ArrayList<>();

        List<String> sampleList = new ArrayList<>();
        Collections.addAll(sampleList, BSPManagerFactoryStub.TEST_SK_SAMPLE_1,
                BSPManagerFactoryStub.TEST_SK_SAMPLE_2,
                BSPManagerFactoryStub.TEST_SK_SAMPLE_3,
                BSPManagerFactoryStub.TEST_SK_SAMPLE_4);

        ResearchProject testProject = researchProjectDao.findByTitle("ADHD");

        testProductOrder = new ProductOrder(bspUserList.getByUsername("scottmat"),testProject);
        testProductOrder.setTitle("Test PO for sampleReceipt" + (new Date()).getTime());

        Map<String, List<ProductOrderSample>> mapBySamples = productOrderSampleDao.findMapBySamples(sampleList);
        for (Map.Entry<String, List<ProductOrderSample>> foundSamples : mapBySamples.entrySet()) {
            if (foundSamples.getValue().isEmpty()) {
                ProductOrderSample newTestSample = new ProductOrderSample(foundSamples.getKey());
                newTestSample.setProductOrder(testProductOrder);

                samplesToDelete.add(newTestSample);
            }
        }
        if(!samplesToDelete.isEmpty()) {
            testProductOrder.addSamples(samplesToDelete);
        }
        productOrderDao.persist(testProductOrder);
        productOrderDao.flush();
    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {

        if(productOrderSampleDao == null) {
            return;
        }

        for (ProductOrderSample candidate : samplesToDelete) {
            ProductOrderSample candidateToRemove = productOrderSampleDao.findById(ProductOrderSample.class, candidate.getProductOrderSampleId());
            for(SampleReceiptValidation validation:candidateToRemove.getValidations()) {

                SampleReceiptValidation foundvalidation = sampleReceiptValidationDao.findById(SampleReceiptValidation.class, validation.getValidationId());
                sampleReceiptValidationDao.remove(validation);
            }
            candidateToRemove.getValidations().clear();
            productOrderSampleDao.remove(candidateToRemove);
        }
        productOrderSampleDao.flush();
        productOrderSampleDao.clear();

        ProductOrder testOrderCleanUp = productOrderDao.findById(testProductOrder.getProductOrderId());
        testOrderCleanUp.getSamples().clear();
        productOrderDao.remove(testOrderCleanUp);
        productOrderDao.flush();
        productOrderSampleDao.clear();

        samplesToDelete.clear();
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testValidationOneSampleShort() throws Exception {

        List<String> testSampleRequestList = new ArrayList<>();
        Collections
                .addAll(testSampleRequestList, BSPManagerFactoryStub.TEST_SK_SAMPLE_1,
                        BSPManagerFactoryStub.TEST_SK_SAMPLE_2,
                        BSPManagerFactoryStub.TEST_SK_SAMPLE_3);

        MessageCollection results = new MessageCollection();

        receiveSamplesEjb.validateForReceipt(testSampleRequestList, results, "scottmat");

        Assert.assertFalse(results.hasErrors());
        Assert.assertFalse(results.hasInfos());
        Assert.assertTrue(results.hasWarnings());

        Map<String, List<ProductOrderSample>> mapBySamples =
                productOrderSampleDao.findMapBySamples(testSampleRequestList);

        for (Map.Entry<String, List<ProductOrderSample>> foundPosAffected : mapBySamples.entrySet()) {
            Assert.assertFalse(foundPosAffected.getValue().isEmpty());
            for (ProductOrderSample currentPOS : foundPosAffected.getValue()) {
                Assert.assertFalse(currentPOS.getValidations().isEmpty());
                Assert.assertEquals(SampleReceiptValidation.SampleValidationReason.MISSING_SAMPLE_FROM_SAMPLE_KIT,
                        currentPOS.getValidations().iterator().next().getReason());
                Assert.assertEquals(SampleReceiptValidation.SampleValidationType.WARNING,
                        currentPOS.getValidations().iterator().next().getValidationType());
            }
        }
    }

    public void testValidationNoIssues() throws Exception {

        List<String> testSampleRequestList = new ArrayList<>();
        Collections
                .addAll(testSampleRequestList, BSPManagerFactoryStub.TEST_SK_SAMPLE_1,
                        BSPManagerFactoryStub.TEST_SK_SAMPLE_2,
                        BSPManagerFactoryStub.TEST_SK_SAMPLE_3,
                        BSPManagerFactoryStub.TEST_SK_SAMPLE_4);

        MessageCollection results = new MessageCollection();

        receiveSamplesEjb.validateForReceipt(testSampleRequestList, results, "scottmat");

        Assert.assertFalse(results.hasErrors());
        Assert.assertFalse(results.hasInfos());
        Assert.assertFalse(results.hasWarnings());

        Map<String, List<ProductOrderSample>> mapBySamples =
                productOrderSampleDao.findMapBySamples(testSampleRequestList);

        for (Map.Entry<String, List<ProductOrderSample>> foundPosAffected : mapBySamples.entrySet()) {

            Assert.assertFalse(foundPosAffected.getValue().isEmpty());
            for (ProductOrderSample currentPOS : foundPosAffected.getValue()) {
                Assert.assertTrue(currentPOS.getValidations().isEmpty());
            }
        }
    }
}
