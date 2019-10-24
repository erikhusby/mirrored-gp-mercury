package org.broadinstitute.gpinformatics.mercury.boundary.bucket;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.BSPSampleDataFetcherImpl;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceTestProducer;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.vessel.LabVesselFactory;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow.AGILENT_EXOME_EXPRESS;
import static org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow.ICE;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

/**
 * DbFree test of MercuryClientEjb.
 */
// singleThreaded because EasyMock mocks are not thread-safe during recording.
@Test(enabled = true, groups = TestGroups.DATABASE_FREE, singleThreaded = true)
public class BucketEjbDbFreeTest {
    private final LabEventType EVENT_TYPE = LabEventType.PICO_PLATING_BUCKET;
    private final String EVENT_LOCATION = LabEvent.UI_EVENT_LOCATION;

    private final int SAMPLE_SIZE = 5;

    private ProductOrder pdo;
    private List<LabVessel> mockVessels;
    private LabBatch labBatch;
    private WorkflowLoader workflowLoader = new WorkflowLoader();
    private BSPUserList bspUserList;
    private Bucket bucket;
    private String pdoCreator;
    private final Map<String, BspSampleData> bspSampleDataMap = new HashMap<>();
    private final Collection<ProductOrderSample> expectedSamples = new HashSet<>();
    private final List<LabVessel> labVessels = new ArrayList<>();

    private BucketEjb bucketEjb;

    private LabEventFactory labEventFactory = EasyMock.createNiceMock(LabEventFactory.class);
    private BucketDao bucketDao = createNiceMock(BucketDao.class);
    private BucketEntryDao bucketEntryDao = createMock(BucketEntryDao.class);
    private LabVesselDao labVesselDao = createNiceMock(LabVesselDao.class);
    private BSPSampleDataFetcher bspSampleDataFetcher = createMock(BSPSampleDataFetcherImpl.class);
    private LabVesselFactory labVesselFactory = createMock(LabVesselFactory.class);

    private Object[] mocks =
            new Object[]{bucketDao, bucketEntryDao, labVesselDao, bspSampleDataFetcher,
                    labVesselFactory, labEventFactory};
    public MercurySampleDao mercurysampleDao;

    @BeforeClass(groups = TestGroups.DATABASE_FREE)
    private void beforeClass() {
        bspUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());
        pdoCreator = bspUserList.getById(BSPManagerFactoryStub.QA_DUDE_USER_ID).getUsername();
        workflowLoader.load();
    }

    private void setUp(String workflow) {
        reset(mocks);

        switch (workflow) {
        case AGILENT_EXOME_EXPRESS:
            labBatch = new LabBatch("ExEx Batch", new HashSet<LabVessel>(), LabBatch.LabBatchType.SAMPLES_RECEIPT);
            pdo = ProductOrderTestFactory.buildExExProductOrder(SAMPLE_SIZE);
            break;
        case ICE:
            labBatch = new LabBatch("ICE Batch", new HashSet<LabVessel>(), LabBatch.LabBatchType.SAMPLES_RECEIPT);
            pdo = ProductOrderTestFactory.buildIceProductOrder(SAMPLE_SIZE);
            break;
        default:
            throw new RuntimeException("Unsupported workflow type: " + workflow);
        }

        // Changes the name of the 4th pdo sample to be the vessel barcode.
        Collection<ProductOrderSample> productOrderSamples = pdo.getSamples();
        Collection<ProductOrderSample> revisedProductOrderSamples = new ArrayList();
        for (ProductOrderSample productOrderSample : productOrderSamples) {
            revisedProductOrderSamples.add(productOrderSample.getName().startsWith("SM-3") ?
                    new ProductOrderSample("R3", productOrderSample.getSampleData()) : productOrderSample);
        }
        pdo.setSamples(revisedProductOrderSamples);

        expectedSamples.clear();
        labVessels.clear();
        bspSampleDataMap.clear();
        pdo.setCreatedBy(BSPManagerFactoryStub.QA_DUDE_USER_ID);
        setupMercurySamples(pdo, expectedSamples, labVessels);

        bucketEjb = new BucketEjb(labEventFactory, JiraServiceTestProducer.stubInstance(), bucketDao, bucketEntryDao,
                                  labVesselDao, labVesselFactory, bspSampleDataFetcher,
                                  bspUserList, workflowLoader, createNiceMock(ProductOrderDao.class), mercurysampleDao);
    }

    // Creates test samples and updates expectedSamples and labVessels.
    private void setupMercurySamples(ProductOrder pdo, Collection<ProductOrderSample> expectedSamples,
                                     List<LabVessel> labVessels) {

        mercurysampleDao = Mockito.mock(MercurySampleDao.class);

        Map<String, MercurySample> sampleMap = new HashMap<>();

        for (int rackPosition = 1; rackPosition <= pdo.getSamples().size(); ++rackPosition) {
            ProductOrderSample pdoSample = pdo.getSamples().get(rackPosition - 1);

            Map<BSPSampleSearchColumn, String> bspData = new HashMap<>();
            switch (rackPosition) {
            case 1:
                // Unreceived root should be rejected.
                bspData.put(BSPSampleSearchColumn.MATERIAL_TYPE, MaterialType.DNA_DNA_GENOMIC.getDisplayName());
                bspData.put(BSPSampleSearchColumn.SAMPLE_ID, pdoSample.getName());
                bspData.put(BSPSampleSearchColumn.ROOT_SAMPLE, pdoSample.getName());
                bspData.put(BSPSampleSearchColumn.RECEIPT_DATE, null);
                break;

            case 2:
                // Received root but non-genomic material, should NOT be accepted.
                bspData.put(BSPSampleSearchColumn.MATERIAL_TYPE, MaterialType.FRESH_BLOOD.getDisplayName());
                bspData.put(BSPSampleSearchColumn.SAMPLE_ID, pdoSample.getName());
                bspData.put(BSPSampleSearchColumn.ROOT_SAMPLE, pdoSample.getName());
                bspData.put(BSPSampleSearchColumn.RECEIPT_DATE, "04/22/2013");
                break;

            case 3:
                // Received root should be accepted.
                bspData.put(BSPSampleSearchColumn.MATERIAL_TYPE, MaterialType.DNA_DNA_GENOMIC.getDisplayName());
                bspData.put(BSPSampleSearchColumn.ROOT_SAMPLE, pdoSample.getName());
                bspData.put(BSPSampleSearchColumn.SAMPLE_ID, pdoSample.getName());
                bspData.put(BSPSampleSearchColumn.RECEIPT_DATE, "04/22/2013");
                expectedSamples.add(pdoSample);
                break;

            default:
                // FYI case 4 will be a derived sample that Mercury doesn't know about yet.
                // Non-root samples should all be accepted.
                bspData.put(BSPSampleSearchColumn.MATERIAL_TYPE, MaterialType.DNA_DNA_GENOMIC.getDisplayName());
                bspData.put(BSPSampleSearchColumn.ROOT_SAMPLE, "ROOT");
                bspData.put(BSPSampleSearchColumn.SAMPLE_ID, pdoSample.getName());
                expectedSamples.add(pdoSample);
                break;
            }
            bspData.put(BSPSampleSearchColumn.MANUFACTURER_BARCODE, makeTubeBarcode(rackPosition));
            BspSampleData bspSampleData = new BspSampleData(bspData);
            bspSampleDataMap.put(pdoSample.getName(), bspSampleData);

            LabVessel labVessel = new BarcodedTube(makeTubeBarcode(rackPosition));
            MercurySample mercurySample = new MercurySample(pdoSample.getName(), bspSampleData);
            sampleMap.put(pdoSample.getName(), mercurySample);
            labVessel.addSample(mercurySample);
            labVessels.add(labVessel);

            labBatch.addLabVessel(labVessel);
        }

        Mockito.when(mercurysampleDao.findMapIdToMercurySample(Mockito.anyCollection())).thenReturn(sampleMap);
    }

    @Test(enabled = true, groups = TestGroups.DATABASE_FREE)
    public void testSamplesToPicoBucket() throws Exception {
        for (String workflow : (new String[]{AGILENT_EXOME_EXPRESS, ICE})) {
            setupCoreMocks(workflow, true);

            expect(labEventFactory
                    .buildFromBatchRequests(EasyMock.<List<BucketEntry>>anyObject(), EasyMock.<String>anyObject(),
                            EasyMock.<LabBatch>anyObject(), EasyMock.<String>anyObject(), EasyMock.<String>anyObject(),
                            EasyMock.<LabEventType>anyObject(), EasyMock.<Date>anyObject(), eq(0)))
                    .andReturn(Collections.<LabEvent>emptyList()).anyTimes();

            expect(bspSampleDataFetcher.fetchSampleData(EasyMock.<Collection<String>>anyObject(), EasyMock.anyObject(),
                    EasyMock.anyObject(), EasyMock.anyObject(), EasyMock.anyObject(), EasyMock.anyObject()))
                    .andReturn(bspSampleDataMap);

            replay(mocks);

            Map<String, Collection<ProductOrderSample>> samplesByBucket = bucketEjb.addSamplesToBucket(pdo);
            Collection<ProductOrderSample> addedSamples = samplesByBucket.get("Pico/Plating Bucket");
            if(addedSamples == null) {
                System.out.println("added samples are null");
            }
            if(expectedSamples == null) {
                System.out.println("expected samples are null");
            }
            Assert.assertEqualsNoOrder(addedSamples.toArray(), expectedSamples.toArray());

            verify(mocks);
        }
    }

    public void testApplyBucketCriteriaNoWorkflow(){
        setupCoreMocks(AGILENT_EXOME_EXPRESS, true);
        pdo.getProduct().setWorkflowName(Workflow.NONE);

        Pair<ProductWorkflowDefVersion, Collection<BucketEntry>> workflowBucketEntriesPair =
                bucketEjb.applyBucketCriteria(mockVessels, pdo, "whatever",
                        ProductWorkflowDefVersion.BucketingSource.PDO_SUBMISSION, new Date());
        Collection<BucketEntry> bucketEntries = workflowBucketEntriesPair.getRight();
        Assert.assertTrue(bucketEntries.isEmpty());
    }

    public void testDuplicateSamplesToPicoBucket() throws Exception {
        String workflow = AGILENT_EXOME_EXPRESS;
        setupCoreMocks(workflow, true);

        expect(bspSampleDataFetcher.fetchSampleData(EasyMock.<Collection<String>>anyObject()))
                .andReturn(bspSampleDataMap);

        replay(mocks);
        ProductWorkflowDef workflowDef = workflowLoader.load().getWorkflow(AGILENT_EXOME_EXPRESS);

        WorkflowBucketDef picoBucket = workflowDef.getEffectiveVersion().findBucketDefByName("Pico/Plating Bucket");

        Map<WorkflowBucketDef, Collection<LabVessel>> newBucketEntry = new HashMap<>();
        LabVessel labVessel = mockVessels.get(1);
        newBucketEntry.put(picoBucket, Collections.singleton(labVessel));

        Collection<BucketEntry> bucketEntries = bucketEjb
                .add(newBucketEntry, BucketEntry.BucketEntryType.PDO_ENTRY, LabEvent.UI_PROGRAM_NAME, "seinfeld",
                        LabEvent.UI_EVENT_LOCATION, pdo, new Date());
        Assert.assertEquals(bucketEntries.size(), 1);

        bucketEntries = bucketEjb
                .add(newBucketEntry, BucketEntry.BucketEntryType.PDO_ENTRY, LabEvent.UI_PROGRAM_NAME, "seinfeld",
                        LabEvent.UI_EVENT_LOCATION, pdo, new Date());
        Assert.assertTrue(bucketEntries.isEmpty());
    }

    private String makeTubeBarcode(int rackPosition) {
        return "R" + rackPosition;
    }

    private void setupCoreMocks(String workflow, boolean createVessels) {
        setUp(workflow);
        mockVessels = new ArrayList<>();
        final Multimap<String, LabVessel> sampleKeyToVessels = HashMultimap.create();
        final Map<String, LabVessel> barcodeToVessel = new HashMap<>();

        // Mock should return sample for those that Mercury knows about, i.e. all except the 1st and 4th samples.
        // The 4th sample is in house so a standalone vessel/sample should be created.
        // The 3rd product order sample name is a vessel barcode.
        for (int rackPosition = 1; rackPosition <= SAMPLE_SIZE; ++rackPosition) {
            ProductOrderSample pdoSample = pdo.getSamples().get(rackPosition - 1);
            LabVessel labVessel = labVessels.get(rackPosition - 1);
            if (rackPosition != 1 && rackPosition != 4) {
                mockVessels.add(labVessel);
                if (rackPosition != 3) {
                    sampleKeyToVessels.put(pdoSample.getName(), labVessel);
                }
                barcodeToVessel.put(labVessel.getLabel(), labVessel);
            }
            if (createVessels) {
                if (rackPosition == 4) {
                    List<LabVessel> mockCreatedVessels = new ArrayList<>();
                    List<LabVessel> secondaryVessels = new ArrayList<>();
                    Pair<List<LabVessel>,List<LabVessel>> factoryVesselPair = Pair.of(mockCreatedVessels, secondaryVessels);
                    mockCreatedVessels.add(labVessels.get(rackPosition - 1));
                    expect(labVesselFactory.buildInitialLabVessels(eq(pdoSample.getName()),
                            eq(makeTubeBarcode(rackPosition)), eq(pdoCreator), (Date) anyObject(),
                            eq(MercurySample.MetadataSource.BSP))).
                                    andReturn(factoryVesselPair);
                }
            }
        }

        expect(labVesselDao.findBySampleKey(EasyMock.<String>anyObject())).andAnswer(new IAnswer<List<LabVessel>>() {
            @Override
            public List<LabVessel> answer() throws Throwable {
                String sampleKey = (String) EasyMock.getCurrentArguments()[0];
                return new ArrayList<>(sampleKeyToVessels.get(sampleKey));
            }
        }).anyTimes();

        expect(labVesselDao.findByIdentifier(EasyMock.<String>anyObject())).andAnswer(new IAnswer<LabVessel>() {
            @Override
            public LabVessel answer() throws Throwable {
                String barcode = (String) EasyMock.getCurrentArguments()[0];
                return barcodeToVessel.get(barcode);
            }
        }).anyTimes();

        expect(bucketDao.findByName(EasyMock.<String>anyObject())).andAnswer(new IAnswer<Bucket>() {
            @Override
            public Bucket answer() throws Throwable {
                String arg = (String) EasyMock.getCurrentArguments()[0];
                bucket = new Bucket(arg);
                return bucket;
            }
        }).anyTimes();
    }
}
