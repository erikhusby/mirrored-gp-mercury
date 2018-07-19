package org.broadinstitute.gpinformatics.athena.boundary.orders;

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
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Test(groups = TestGroups.STUBBY)
@Dependent
public class ReceiveSamplesEjbTest extends StubbyContainerTest {

    public ReceiveSamplesEjbTest(){}

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

    @BeforeMethod(groups = TestGroups.STUBBY, enabled = false)
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

        Map<String, Set<ProductOrderSample>> mapBySamples = productOrderSampleDao.findMapBySamples(sampleList);
        for (Map.Entry<String, Set<ProductOrderSample>> foundSamples : mapBySamples.entrySet()) {
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

    @AfterMethod(groups = TestGroups.STUBBY, enabled = false)
    public void tearDown() throws Exception {

        if(productOrderSampleDao == null) {
            return;
        }

        for (ProductOrderSample candidate : samplesToDelete) {
            ProductOrderSample candidateToRemove = productOrderSampleDao.findById(ProductOrderSample.class, candidate.getProductOrderSampleId());
            for(SampleReceiptValidation validation:candidateToRemove.getSampleReceiptValidations()) {

                SampleReceiptValidation foundvalidation = sampleReceiptValidationDao.findById(SampleReceiptValidation.class, validation.getValidationId());
                sampleReceiptValidationDao.remove(validation);
            }
            candidateToRemove.getSampleReceiptValidations().clear();
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

    @Test(groups = TestGroups.STUBBY, enabled = false)
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

        Map<String, Set<ProductOrderSample>> mapBySamples =
                productOrderSampleDao.findMapBySamples(testSampleRequestList);

        for (Map.Entry<String, Set<ProductOrderSample>> foundPosAffected : mapBySamples.entrySet()) {
            Assert.assertFalse(foundPosAffected.getValue().isEmpty());
            for (ProductOrderSample currentPOS : foundPosAffected.getValue()) {
                Assert.assertFalse(currentPOS.getSampleReceiptValidations().isEmpty());
                Assert.assertEquals(SampleReceiptValidation.SampleValidationReason.MISSING_SAMPLE_FROM_SAMPLE_KIT,
                        currentPOS.getSampleReceiptValidations().iterator().next().getReason());
                Assert.assertEquals(SampleReceiptValidation.SampleValidationType.WARNING,
                        currentPOS.getSampleReceiptValidations().iterator().next().getValidationType());
            }
        }
    }

    @Test(groups = TestGroups.STUBBY, enabled = false)
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

        Map<String, Set<ProductOrderSample>> mapBySamples =
                productOrderSampleDao.findMapBySamples(testSampleRequestList);

        for (Map.Entry<String, Set<ProductOrderSample>> foundPosAffected : mapBySamples.entrySet()) {

            Assert.assertFalse(foundPosAffected.getValue().isEmpty());
            for (ProductOrderSample currentPOS : foundPosAffected.getValue()) {
                Assert.assertTrue(currentPOS.getSampleReceiptValidations().isEmpty());
            }
        }
    }
}
