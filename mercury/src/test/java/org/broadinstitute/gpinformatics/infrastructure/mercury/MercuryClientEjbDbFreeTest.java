package org.broadinstitute.gpinformatics.infrastructure.mercury;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryStub;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.LabVesselFactory;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;
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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

/**
 * DbFree test of MercuryClientEjb.
 *
 * @author epolk
 * @author breilly
 */
// singleThreaded because EasyMock mocks are not thread-safe during recording.
@Test(enabled = true, groups = TestGroups.DATABASE_FREE, singleThreaded = true)
public class MercuryClientEjbDbFreeTest {
    private final LabEventType EVENT_TYPE = LabEventType.PICO_PLATING_BUCKET;
    private final String EVENT_LOCATION = LabEvent.UI_EVENT_LOCATION;

    private final int SAMPLE_SIZE = 5;

    private MercuryClientEjb service;
    private ProductOrder pdo;
    private LabBatch labBatch;
    private final WorkflowLoader workflowLoader = new WorkflowLoader();
    private BSPUserList bspUserList;
    private Bucket bucket;
    private String pdoCreator;
    private final Map<String, BSPSampleDTO> bspDtoMap = new HashMap<>();
    private final Collection<ProductOrderSample> expectedSamples = new ArrayList<>();
    private final List<LabVessel> labVessels = new ArrayList<>();

    private BucketEjb bucketEjb = createMock(BucketEjb.class);
    private BucketDao bucketDao = createMock(BucketDao.class);
    private LabVesselDao labVesselDao = createMock(LabVesselDao.class);
    private BSPSampleDataFetcher bspSampleDataFetcher = createMock(BSPSampleDataFetcher.class);
    private LabVesselFactory labVesselFactory = createMock(LabVesselFactory.class);

    private Object[] mocks = new Object[]{bucketEjb, bucketDao, labVesselDao, bspSampleDataFetcher, labVesselFactory};

    @BeforeClass(groups = TestGroups.DATABASE_FREE)
    private void beforeClass() {
        bspUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());
        pdoCreator = bspUserList.getById(BSPManagerFactoryStub.QA_DUDE_USER_ID).getUsername();
    }

    private void setUp(Workflow workflow) {
        reset(mocks);

        switch (workflow) {
        case AGILENT_EXOME_EXPRESS:
            labBatch = new LabBatch("ExEx Batch", new HashSet<LabVessel>(), LabBatch.LabBatchType.SAMPLES_RECEIPT);
            bucket = new Bucket(new WorkflowStepDef("Pico/Plating Bucket"));
            pdo = ProductOrderTestFactory.buildExExProductOrder(SAMPLE_SIZE);
            break;
        case ICE:
            labBatch = new LabBatch("ICE Batch", new HashSet<LabVessel>(), LabBatch.LabBatchType.SAMPLES_RECEIPT);
            bucket = new Bucket(new WorkflowStepDef("Pico/Plating Bucket"));
            pdo = ProductOrderTestFactory.buildIceProductOrder(SAMPLE_SIZE);
            break;
        default:
            throw new RuntimeException("Unsupported workflow type: " + workflow.name());
        }

        expectedSamples.clear();
        labVessels.clear();
        bspDtoMap.clear();
        pdo.setCreatedBy(BSPManagerFactoryStub.QA_DUDE_USER_ID);
        setupMercurySamples(pdo, expectedSamples, labVessels);

        service = new MercuryClientEjb(bucketEjb, bucketDao, workflowLoader, bspUserList, labVesselDao,
                bspSampleDataFetcher, labVesselFactory);
    }

    // Creates test samples and updates expectedSamples and labVessels.
    private void setupMercurySamples(ProductOrder pdo, Collection<ProductOrderSample> expectedSamples,
                                     List<LabVessel> labVessels) {
        for (int rackPosition = 1; rackPosition <= pdo.getSamples().size(); ++rackPosition) {
            ProductOrderSample pdoSample = pdo.getSamples().get(rackPosition - 1);

            Map<BSPSampleSearchColumn, String> bspData = new HashMap<>();
            switch (rackPosition) {
                case 1:
                    // Unreceived root should be rejected.
                    bspData.put(BSPSampleSearchColumn.MATERIAL_TYPE, "DNA:DNA Genomic");
                    bspData.put(BSPSampleSearchColumn.SAMPLE_ID, pdoSample.getSampleName());
                    bspData.put(BSPSampleSearchColumn.ROOT_SAMPLE, pdoSample.getSampleName());
                    bspData.put(BSPSampleSearchColumn.RECEIPT_DATE, null);
                    break;

                case 2:
                    // Received root but non-genomic material, should be rejected.
                    bspData.put(BSPSampleSearchColumn.MATERIAL_TYPE, "Tissue:Blood");
                    bspData.put(BSPSampleSearchColumn.SAMPLE_ID, pdoSample.getSampleName());
                    bspData.put(BSPSampleSearchColumn.ROOT_SAMPLE, pdoSample.getSampleName());
                    bspData.put(BSPSampleSearchColumn.RECEIPT_DATE, "04/22/2013");
                    break;

                case 3:
                    // Received root should be accepted.
                    bspData.put(BSPSampleSearchColumn.MATERIAL_TYPE, "DNA:DNA Genomic");
                    bspData.put(BSPSampleSearchColumn.ROOT_SAMPLE, pdoSample.getSampleName());
                    bspData.put(BSPSampleSearchColumn.SAMPLE_ID, pdoSample.getSampleName());
                    bspData.put(BSPSampleSearchColumn.RECEIPT_DATE, "04/22/2013");
                    expectedSamples.add(pdoSample);
                    break;

                default:
                    // FYI case 4 will be a derived sample that Mercury doesn't know about yet.
                    // Non-root samples should all be accepted.
                    bspData.put(BSPSampleSearchColumn.MATERIAL_TYPE, "DNA:DNA Genomic");
                    bspData.put(BSPSampleSearchColumn.ROOT_SAMPLE, "ROOT");
                    bspData.put(BSPSampleSearchColumn.SAMPLE_ID, pdoSample.getSampleName());
                    expectedSamples.add(pdoSample);
                    break;
            }
            BSPSampleDTO bspDto = new BSPSampleDTO(bspData);
            bspDto.addPlastic(makeTubeBarcode(rackPosition));
            bspDtoMap.put(pdoSample.getSampleName(), bspDto);

            LabVessel labVessel = new TwoDBarcodedTube(makeTubeBarcode(rackPosition));
            labVessel.addSample(new MercurySample(pdoSample.getSampleName(), bspDto));
            labVessels.add(labVessel);

            labBatch.addLabVessel(labVessel);
        }
    }


    public void testSamplesToPicoBucket() throws Exception {
        for (Workflow workflow : (new Workflow[] {Workflow.AGILENT_EXOME_EXPRESS, Workflow.ICE})) {
            setUp(workflow);

            WorkflowConfig workflowConfig = workflowLoader.load();
            ProductWorkflowDefVersion workflowDefVersion = workflowConfig.getWorkflow(workflow).getEffectiveVersion();
            WorkflowBucketDef initialBucketDef = workflowDefVersion.getInitialBucket();

            reset(mocks);

            // Mock should return sample for those that Mercury knows about, i.e. all except the 1st and 4th samples.
            // The 4th sample is in house so a standalone vessel/sample should be created.
            List<LabVessel> mockVessels = new ArrayList<>();
            for (int rackPosition = 1; rackPosition <= SAMPLE_SIZE; ++rackPosition) {
                ProductOrderSample pdoSample = pdo.getSamples().get(rackPosition - 1);
                if (rackPosition != 1 && rackPosition != 4) {
                    mockVessels.add(labVessels.get(rackPosition - 1));
                }
                if (rackPosition == 4) {
                    List<LabVessel> mockCreatedVessels = new ArrayList<>();
                    mockCreatedVessels.add(labVessels.get(rackPosition - 1));
                    expect(labVesselFactory.buildInitialLabVessels(eq(pdoSample.getSampleName()),
                            eq(makeTubeBarcode(rackPosition)), eq(pdoCreator), (Date)anyObject()))
                            .andReturn(mockCreatedVessels);
                }
            }

            expect(labVesselDao.findBySampleKeyList((Collection<String>)anyObject())).andReturn(mockVessels);
            expect(bucketEjb.findOrCreateBucket(initialBucketDef.getName())).andReturn(bucket);
            // Should be OK to return more samples in map than was asked for.
            expect(bspSampleDataFetcher.fetchSamplesFromBSP((List<String>)anyObject())).andReturn(bspDtoMap);
            bucketDao.persist(bucket);

            // It's OK to return an empty collection because the returned bucket entries are only needed for rework cases.
            expect(bucketEjb.add((List<LabVessel>)anyObject(), eq(bucket),
                    eq(BucketEntry.BucketEntryType.PDO_ENTRY), eq(pdoCreator), eq(EVENT_LOCATION),
                    eq(LabEvent.UI_PROGRAM_NAME),
                    eq(EVENT_TYPE),
                    eq(pdo.getBusinessKey()))).andReturn(Collections.<BucketEntry>emptySet());

            replay(mocks);

            Collection<ProductOrderSample> addedSamples = service.addFromProductOrder(pdo);
            Assert.assertEqualsNoOrder(addedSamples.toArray(), expectedSamples.toArray());

            verify(mocks);
        }
    }

    private String makeTubeBarcode(int rackPosition) {
        return "R" + rackPosition;
    }
}
