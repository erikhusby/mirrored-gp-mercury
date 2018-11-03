package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.projects.ResearchProjectEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderSampleTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.ProductOrderDBTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.STANDARD)
public class ProductOrderContainerTest extends Arquillian {
    @Inject
    private BSPUserList userList;

    @Inject
    ResearchProjectEjb researchProjectEjb;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    JiraService jiraService;

    @Inject
    private UserTransaction utx;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    public static ProductOrder createSimpleProductOrder(ResearchProjectEjb researchProjectEjb,
            BSPUserList userList) throws Exception {
        return new ProductOrder(ResearchProjectTestFactory.TEST_CREATOR, "containerTest Product Order Test1",
                ProductOrderSampleTestFactory.createSampleListWithMercurySamples("SM-1P3X9", "SM-1P3WY", "SM-1P3XN"),
                "newQuote", ProductTestFactory.createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, "partNumber"),
                ResearchProjectTestFactory
                        .createDummyResearchProject(researchProjectEjb, userList, "Test Research Project"));
    }

    public void testSimpleProductOrder() throws Exception {
        ProductOrder testOrder = createSimpleProductOrder(researchProjectEjb, userList);

        Assert.assertEquals(testOrder.getUniqueParticipantCount(), 3);
        Assert.assertEquals(testOrder.getUniqueSampleCount(), 3);
        Assert.assertEquals(testOrder.getNormalCount(), 0);
        Assert.assertEquals(testOrder.getTumorCount(), 0);

        Assert.assertEquals(testOrder.getTotalSampleCount(), 3);
        Assert.assertEquals(testOrder.getDuplicateCount(), 0);
        Assert.assertEquals(testOrder.getFemaleCount(), 3);
        Assert.assertEquals(testOrder.getMaleCount(), 0);

        Assert.assertTrue(testOrder.getCountsByStockType().containsKey(ProductOrderSample.ACTIVE_IND));
        Assert.assertEquals(testOrder.getCountsByStockType().get(ProductOrderSample.ACTIVE_IND).intValue(), 3);

        // Test the sample order should be the same as when created.
        Assert.assertEquals(testOrder.getSamples().get(0).getName(), "SM-1P3X9");
        Assert.assertEquals(testOrder.getSamples().get(1).getName(), "SM-1P3WY");
        Assert.assertEquals(testOrder.getSamples().get(2).getName(), "SM-1P3XN");

        Assert.assertEquals(testOrder.getReceivedSampleCount(), 3);

        Assert.assertEquals(testOrder.getActiveSampleCount(), 3);
    }

    public void testSimpleProductOrderWithConsent() throws Exception {
        ProductOrder testOrder = ProductOrderDBTestFactory.createTestExExProductOrder(researchProjectDao, productDao);
        testOrder.setCreatedBy(10950L);

        Collection<RegulatoryInfo> availableRegulatoryInfos = testOrder.findAvailableRegulatoryInfos();
        Assert.assertFalse(availableRegulatoryInfos.isEmpty());
        testOrder.setRegulatoryInfos(availableRegulatoryInfos);

        BspUser bspUser = new BspUser();
        bspUser.setUserId(10950L);
        testOrder.prepareToSave(bspUser, ProductOrder.SaveType.CREATING);
        productOrderDao.persist(testOrder.getProduct());
        Assert.assertTrue(StringUtils.isNotEmpty(testOrder.getJiraTicketKey()));
    }

    public void testSimpleNonBspProductOrder() throws Exception {
        ProductOrder testOrder =
                new ProductOrder(ResearchProjectTestFactory.TEST_CREATOR, "containerTest Product Order Test2",
                        ProductOrderSampleTestFactory.createSampleListWithMercurySamples("SM_12CO4", "SM_1P3WY",
                                "SM_1P3XN"),
                        "newQuote",
                        ProductTestFactory.createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, "partNumber"),
                        ResearchProjectTestFactory
                                .createDummyResearchProject(researchProjectEjb, userList, "Test Research Project"));

        Assert.assertEquals(testOrder.getUniqueSampleCount(), 3);

        Assert.assertEquals(testOrder.getTotalSampleCount(), 3);
        Assert.assertEquals(testOrder.getDuplicateCount(), 0);
        Assert.assertEquals(testOrder.getSampleCount(), 0);
    }

    public void testProductOrderSampleBarcode() throws Exception {
        if (utx == null) {
            return;
        }
        utx.begin();
        String uniqueId = System.currentTimeMillis() + "T";
        final int numSamples = 4;
        String[] sampleIds = new String[numSamples];
        for (int i = 0; i < numSamples; ++i) {
            sampleIds[i] = uniqueId + i;
            String tubeBarcode = "A" + sampleIds[i];
            MercurySample mercurySample = new MercurySample(sampleIds[i], MercurySample.MetadataSource.MERCURY);
            LabVessel tube = new BarcodedTube(tubeBarcode, BarcodedTube.BarcodedTubeType.MatrixTube075);
            tube.setReceiptEvent(new BSPUserList.QADudeUser("LU", i), new Date(), (long) i, LabEvent.UI_EVENT_LOCATION);
            mercurySample.addLabVessel(tube);
            labVesselDao.getEntityManager().persist(tube);
            labVesselDao.flush();
        }
        ProductOrder testOrder = new ProductOrder(ResearchProjectTestFactory.TEST_CREATOR, uniqueId + "test",
                // Mix of sample names and tube barcodes.
                Arrays.asList(new ProductOrderSample(sampleIds[0]), new ProductOrderSample("A" + sampleIds[1]),
                        new ProductOrderSample(sampleIds[2]), new ProductOrderSample("A" + sampleIds[3])),
                "newQuote", ProductTestFactory.createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, "partNumber"),
                ResearchProjectTestFactory.createDummyResearchProject(researchProjectEjb, userList, uniqueId));

        Assert.assertEquals(testOrder.getUniqueSampleCount(), numSamples);
        Assert.assertEquals(testOrder.getTotalSampleCount(), numSamples);
        utx.rollback();
    }

    /**
     * A pdo sample name refers to both a tube barcode and a mercury sample name. This is a contrived
     * test case but External Library uploads can have arbitrary tube barcodes and sample names.
     */
    public void testAmbiguousProductOrderSample() throws Exception {
        if (utx == null) {
            return;
        }
        try {
            utx.begin();
            String uniqueId = System.currentTimeMillis() + "V";
            String ambiguousName = uniqueId + 1;
            for (int i = 0; i < 2; ++i) {
                String sampleId = uniqueId + i;
                String tubeBarcode = i == 0 ? ambiguousName : ("B" + sampleId);
                MercurySample mercurySample = new MercurySample(sampleId, MercurySample.MetadataSource.MERCURY);
                LabVessel tube = new BarcodedTube(tubeBarcode, BarcodedTube.BarcodedTubeType.MatrixTube075);
                tube.setReceiptEvent(new BSPUserList.QADudeUser("LU", i), new Date(), (long) i,
                        LabEvent.UI_EVENT_LOCATION);
                mercurySample.addLabVessel(tube);
                labVesselDao.getEntityManager().persist(tube);
                labVesselDao.flush();
                Assert.assertNotNull(labVesselDao.findByIdentifier(tubeBarcode), tubeBarcode);
            }
            try {
                ProductOrder testOrder = new ProductOrder(ResearchProjectTestFactory.TEST_CREATOR, uniqueId + "test",
                        Arrays.asList(new ProductOrderSample(uniqueId + 0), new ProductOrderSample(ambiguousName)),
                        "newQuote", ProductTestFactory.createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, "partNumber"),
                        ResearchProjectTestFactory.createDummyResearchProject(researchProjectEjb, userList, uniqueId));
                // getUniqueSampleCount() should invoke ProductOrder.loadSampleData()
                // which should throw due to the ambiguous pdo sample name.
                Assert.assertEquals(testOrder.getUniqueSampleCount(), 2);
                Assert.fail("Should have thrown when given an ambiguous pdo sample name.");
            } catch (Exception e) {
                Assert.assertEquals(e.getMessage(), String.format(ProductOrder.AMBIGUOUS_PDO_SAMPLE, ambiguousName));
            }
        } finally {
            utx.rollback();
        }
    }
}