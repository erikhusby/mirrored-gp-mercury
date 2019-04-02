package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceTestProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SequencingTemplateFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.project.JiraTicketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateType;
import org.easymock.EasyMock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Scott Matthews
 *         Date: 12/7/12
 *         Time: 4:31 PM
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class LabBatchEjbDBFreeTest {

    public static final String STUB_TEST_PDO_KEY = "PDO-999";

    private LabBatchEjb labBatchEJB;

    private LabBatchDao labBatchDao;

    private LabVesselDao tubeDao;

    private LinkedHashMap<String, LabVessel> mapBarcodeToTube = new LinkedHashMap<>();
    private ArrayList<String> pdoNames;
    private String scottmat;
    private String testLCSetKey;
    private String testFCTKey;
    private JiraTicketDao mockJira;
    private Set<String> vesselSampleList;
    @MockitoAnnotations.Mock
    private ProductOrderDao productOrderDao;

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);

        testLCSetKey = "LCSet-tst932";
        testFCTKey = "FCT-tst932";

        vesselSampleList = new HashSet<>(6);

        Collections.addAll(vesselSampleList, "SM-423", "SM-243", "SM-765", "SM-143", "SM-9243", "SM-118");

        ProductOrder testOrder = ProductOrderTestFactory.createDummyProductOrder();
        testOrder.setJiraTicketKey(STUB_TEST_PDO_KEY);

        Bucket bucket = new Bucket(new WorkflowBucketDef(LabBatchEJBTest.BUCKET_NAME));

        // starting rack
        int sampleIndex = 1;
        for (String sampleName : vesselSampleList) {
            String barcode = "R" + sampleIndex + sampleIndex + sampleIndex + sampleIndex + sampleIndex + sampleIndex;
            String bspStock = sampleName;
            BarcodedTube bspAliquot = new BarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(bspStock, MercurySample.MetadataSource.BSP));
            bucket.addEntry(testOrder, bspAliquot,BucketEntry.BucketEntryType.PDO_ENTRY);
            mapBarcodeToTube.put(barcode, bspAliquot);
            sampleIndex++;
        }


        labBatchEJB = new LabBatchEjb();
        labBatchEJB.setJiraService(JiraServiceTestProducer.stubInstance());

        SequencingTemplateFactory sequencingTemplateFactory = mock(SequencingTemplateFactory.class);
        SequencingTemplateType sequencingTemplateType = new SequencingTemplateType();
        sequencingTemplateType.setReadStructure("76T8B8B76T");
        when(sequencingTemplateFactory.getSequencingTemplate(any(LabBatch.class), anyBoolean())).thenReturn(sequencingTemplateType);
        labBatchEJB.setSequencingTemplateFactory(sequencingTemplateFactory);

        tubeDao = EasyMock.createMock(LabVesselDao.class);
        EasyMock.expect(tubeDao.findByIdentifier(EasyMock.eq("SM-423"))).andReturn(mapBarcodeToTube.get("R111111"));
        EasyMock.expect(tubeDao.findByIdentifier(EasyMock.eq("SM-243"))).andReturn(mapBarcodeToTube.get("R222222"));
        EasyMock.expect(tubeDao.findByIdentifier(EasyMock.eq("SM-765"))).andReturn(mapBarcodeToTube.get("R333333"));
        EasyMock.expect(tubeDao.findByIdentifier(EasyMock.eq("SM-143"))).andReturn(mapBarcodeToTube.get("R444444"));
        EasyMock.expect(tubeDao.findByIdentifier(EasyMock.eq("SM-9243"))).andReturn(mapBarcodeToTube.get("R555555"));
        EasyMock.expect(tubeDao.findByIdentifier(EasyMock.eq("SM-118"))).andReturn(mapBarcodeToTube.get("R666666"));
        labBatchEJB.setTubeDao(tubeDao);

        mockJira = EasyMock.createMock(JiraTicketDao.class);
        EasyMock.expect(mockJira.fetchByName(testLCSetKey))
                .andReturn(new JiraTicket(JiraServiceTestProducer.stubInstance(), testLCSetKey)).times(0, 1);
        EasyMock.expect(mockJira.fetchByName(testFCTKey))
                .andReturn(new JiraTicket(JiraServiceTestProducer.stubInstance(), testFCTKey)).times(0, 1);

        labBatchDao = EasyMock.createNiceMock(LabBatchDao.class);
        labBatchEJB.setLabBatchDao(labBatchDao);

        when(productOrderDao.findByBusinessKey(Mockito.anyString())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                String businessKey = (String)invocationOnMock.getArguments()[0];
                return ProductOrderTestFactory.createDummyProductOrder(businessKey);
            }
        });

        labBatchEJB.setProductOrderDao(productOrderDao);

        labBatchEJB.setWorkflowConfig(new WorkflowLoader().load());

        pdoNames = new ArrayList<>();
        Collections.addAll(pdoNames, STUB_TEST_PDO_KEY);

        EasyMock.replay(mockJira, labBatchDao, tubeDao);

        scottmat = "scottmat";
    }

    @AfterMethod(groups = TestGroups.DATABASE_FREE)
    public void tearDown() throws Exception {
        EasyMock.verify(labBatchDao);
    }

    @Test
    public void testCreateLabBatch() throws Exception {

        final String batchName = "Test create batch basic";
        LabBatch batchObject = new LabBatch(batchName, new HashSet<>(mapBarcodeToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);
        for (LabVessel labVessel : mapBarcodeToTube.values()) {
            for (BucketEntry bucketEntry : labVessel.getBucketEntries()) {
                batchObject.addBucketEntry(bucketEntry);
            }
        }

        LabBatch testBatch = labBatchEJB.createLabBatch(batchObject, "scottmat", CreateFields.IssueType.EXOME_EXPRESS,
                CreateFields.ProjectType.LCSET_PROJECT);

        Assert.assertNotNull(testBatch);
        Assert.assertNotNull(testBatch.getJiraTicket());
        Assert.assertNotNull(testBatch.getJiraTicket().getTicketName());
        Assert.assertNotNull(testBatch.getStartingBatchLabVessels());

        Assert.assertNotSame(batchName, testBatch.getBatchName());
        Assert.assertEquals(testBatch.getJiraTicket().getTicketName(), testBatch.getBatchName());

        Assert.assertEquals(6, testBatch.getStartingBatchLabVessels().size());
        Assert.assertEquals("6 samples with material types [] from MyResearchProject PDO-999\n", testBatch.getBatchDescription());
        Assert.assertNull(testBatch.getDueDate());
        Assert.assertEquals(testBatch.getBatchName(), testBatch.getJiraTicket().getTicketName());
        Assert.assertEquals(testBatch.getLabBatchType(), LabBatch.LabBatchType.WORKFLOW);
    }

    @Test
    void testCreateFCTBatch() throws Exception {

        List<LabBatch.VesselToLanesInfo> vesselToLanesInfo = new ArrayList<>();
        int idx = 0;
        for (LabVessel tube : mapBarcodeToTube.values()) {
            VesselPosition lane =
                    IlluminaFlowcell.FlowcellType.HiSeqFlowcell.getVesselGeometry().getVesselPositions()[idx++];
            vesselToLanesInfo.add(new LabBatch.VesselToLanesInfo(Collections.singletonList(lane), BigDecimal.TEN,
                    tube, "LCSET-0004", "CP Human WES (80/20)", Collections.<FlowcellDesignation>emptyList()));
        }
        LabBatch testBatch = new LabBatch(testFCTKey, vesselToLanesInfo, LabBatch.LabBatchType.FCT,
                IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell);
        labBatchEJB.createLabBatch(testBatch, "scottmat", CreateFields.IssueType.HISEQ_2500_RAPID_RUN);
        EasyMock.verify(mockJira);

        Assert.assertNotNull(testBatch);
        Assert.assertNotNull(testBatch.getJiraTicket());
        Assert.assertNotNull(testBatch.getJiraTicket().getTicketName());
        Assert.assertNotNull(testBatch.getStartingBatchLabVessels());
        Assert.assertNotNull(testBatch.getBatchName());
        Assert.assertEquals("FCT-123", testBatch.getBatchName());
        Assert.assertEquals(6, testBatch.getStartingBatchLabVessels().size());
        Assert.assertEquals(testBatch.getBatchDescription(), null);
        Assert.assertNull(testBatch.getDueDate());
        Assert.assertEquals(testBatch.getBatchName(), testBatch.getJiraTicket().getTicketName());
        Assert.assertEquals(testBatch.getLabBatchType(), LabBatch.LabBatchType.FCT);
    }

    @Test
    public void testCreateLabBatchWithVessels() throws Exception {

        LabBatch testBatch =
                labBatchEJB.createLabBatch(LabBatch.LabBatchType.WORKFLOW, Workflow.ICE_EXOME_EXPRESS,
                        testLCSetKey,null,null,"","scottmat",new HashSet<>(mapBarcodeToTube.values()),
                        Collections.<LabVessel>emptySet());
        for (LabVessel labVessel : mapBarcodeToTube.values()) {
            for (BucketEntry bucketEntry : labVessel.getBucketEntries()) {
                testBatch.addBucketEntry(bucketEntry);
            }
        }

        labBatchEJB.batchToJira("scottmat",null, testBatch, CreateFields.IssueType.EXOME_EXPRESS,
                CreateFields.ProjectType.LCSET_PROJECT);
        EasyMock.verify(mockJira);

        Assert.assertNotNull(testBatch);
        Assert.assertNotNull(testBatch.getJiraTicket());
        Assert.assertNotNull(testBatch.getJiraTicket().getTicketName());
        Assert.assertNotNull(testBatch.getStartingBatchLabVessels());
        Assert.assertNotNull(testBatch.getBatchName());
        Assert.assertEquals("LCSET-123", testBatch.getBatchName());
        Assert.assertEquals(6, testBatch.getStartingBatchLabVessels().size());
        Assert.assertEquals(testBatch.getBatchDescription(),
                            extractDescriptionPrefix(testBatch));
        Assert.assertNull(testBatch.getDueDate());
        Assert.assertEquals(testBatch.getBatchName(), testBatch.getJiraTicket().getTicketName());
    }

    @Test
    public void testCreateLabBatchWithJiraTicket() throws Exception {

        final String batchName = "second Test batch name";
        LabBatch batchObject = new LabBatch(batchName, new HashSet<>(mapBarcodeToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);

        for (LabVessel labVessel : mapBarcodeToTube.values()) {
            for (BucketEntry bucketEntry : labVessel.getBucketEntries()) {
                batchObject.addBucketEntry(bucketEntry);
            }
        }

        LabBatch testBatch = labBatchEJB.createLabBatch(batchObject, scottmat, CreateFields.IssueType.EXOME_EXPRESS,
                CreateFields.ProjectType.LCSET_PROJECT);

        Assert.assertNotNull(testBatch);
        Assert.assertNotNull(testBatch.getJiraTicket());
        Assert.assertNotNull(testBatch.getJiraTicket().getTicketName());

        Assert.assertNotSame(batchName, testBatch.getBatchName());
        Assert.assertEquals(testBatch.getJiraTicket().getTicketName(), testBatch.getBatchName());

        Assert.assertNotNull(testBatch.getStartingBatchLabVessels());
        Assert.assertEquals(6, testBatch.getStartingBatchLabVessels().size());
        Assert.assertEquals(testBatch.getBatchDescription(), extractDescriptionPrefix(testBatch));
        Assert.assertNull(testBatch.getDueDate());

        Assert.assertEquals(testBatch.getBatchName(), testBatch.getJiraTicket().getTicketName());
    }

    @Test
    public void testCreateLabBatchWithExtraValues() throws Exception {

        final String description =
                "New User defined description set here at the Create LabBatch call level.  SHould be useful when giving users the ability to set their own description for the LCSET or whatever ticket";
        final String batchName = "Third test of batch name.";
        LabBatch batchInput = new LabBatch(batchName, new HashSet<>(mapBarcodeToTube.values()),
                                           LabBatch.LabBatchType.WORKFLOW);
        batchInput.setBatchDescription(description);

        for (LabVessel labVessel : mapBarcodeToTube.values()) {
            for (BucketEntry bucketEntry : labVessel.getBucketEntries()) {
                batchInput.addBucketEntry(bucketEntry);
            }
        }

        LabBatch testBatch = labBatchEJB.createLabBatch(batchInput, scottmat, CreateFields.IssueType.EXOME_EXPRESS,
                CreateFields.ProjectType.LCSET_PROJECT);

        Assert.assertNotNull(testBatch);
        Assert.assertNotNull(testBatch.getJiraTicket());
        Assert.assertNotNull(testBatch.getJiraTicket().getTicketName());

        Assert.assertNotSame(batchName, testBatch.getBatchName());
        Assert.assertEquals(testBatch.getJiraTicket().getTicketName(), testBatch.getBatchName());

        Assert.assertNotNull(testBatch.getStartingBatchLabVessels());
        Assert.assertEquals(6, testBatch.getStartingBatchLabVessels().size());
        Assert.assertEquals(testBatch.getBatchDescription(), extractDescriptionPrefix(testBatch) +
                                                             "\n" +
                                                             "\n" + description);

        Assert.assertEquals(testBatch.getBatchName(), testBatch.getJiraTicket().getTicketName());
    }

    private String extractDescriptionPrefix(LabBatch testBatch) {

        ProductOrder testProductOrder = ProductOrderTestFactory.createDummyProductOrder(STUB_TEST_PDO_KEY);

        final String descriptionPrefix = String.format("%d samples with material types [] from %s %s\n",
                testBatch.getStartingBatchLabVessels().size(),
                                         testProductOrder.getResearchProject().getTitle(), STUB_TEST_PDO_KEY);
        return descriptionPrefix;
    }
}
