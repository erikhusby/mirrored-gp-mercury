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
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketBean;
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
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

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
    private String PICO_PLATING_BUCKET = "Pico/Plating Bucket";
    private LabEventType EVENT_TYPE = LabEventType.PICO_PLATING_BUCKET;
    private String EVENT_LOCATION = LabEvent.UI_EVENT_LOCATION;

    private final int SAMPLE_SIZE = 5;

    private MercuryClientEjb service;
    private ProductOrder pdo;
    private LabBatch labBatch;
    private WorkflowLoader workflowLoader = new WorkflowLoader();
    private BSPUserList bspUserList;
    private Bucket bucket;
    private String pdoCreator;
    private Map<String, BSPSampleDTO> bspDtoMap;

    private BucketBean bucketBean = createMock(BucketBean.class);
    private BucketDao bucketDao = createMock(BucketDao.class);
    private LabVesselDao labVesselDao = createMock(LabVesselDao.class);
    private BSPSampleDataFetcher bspSampleDataFetcher = createMock(BSPSampleDataFetcher.class);
    private LabVesselFactory labVesselFactory = createMock(LabVesselFactory.class);

    private Object[] mocks = new Object[]{bucketBean, bucketDao, labVesselDao, bspSampleDataFetcher, labVesselFactory};

    @BeforeMethod
    public void setUp() {
        reset(mocks);

        bspUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());
        pdoCreator = bspUserList.getById(BSPManagerFactoryStub.QA_DUDE_USER_ID).getUsername();

        labBatch = new LabBatch("ExEx Receipt Batch", new HashSet<LabVessel>(), LabBatch.LabBatchType.SAMPLES_RECEIPT);
        bucket = new Bucket(new WorkflowStepDef(PICO_PLATING_BUCKET));
        bspDtoMap = new HashMap<String, BSPSampleDTO>();

        pdo = ProductOrderTestFactory.buildExExProductOrder(SAMPLE_SIZE);
        pdo.setCreatedBy(BSPManagerFactoryStub.QA_DUDE_USER_ID);

        service = new MercuryClientEjb(bucketBean, bucketDao, workflowLoader, bspUserList, labVesselDao,
                bspSampleDataFetcher, labVesselFactory);
    }

    // Creates test samples and updates expectedSamples and labVessels.
    private void setupMercurySamples(ProductOrder pdo, Collection<ProductOrderSample> expectedSamples,
                                     List<LabVessel> labVessels) {

        List<MercurySample> mercurySamples = new ArrayList<MercurySample>();

        for (int rackPosition = 1; rackPosition <= pdo.getSamples().size(); ++rackPosition) {
            ProductOrderSample pdoSample = pdo.getSamples().get(rackPosition - 1);

            Map<BSPSampleSearchColumn, String> bspData = new HashMap<BSPSampleSearchColumn, String>();
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

        Collection<ProductOrderSample> expectedSamples = new ArrayList<ProductOrderSample>();
        List<LabVessel> labVessels = new ArrayList<LabVessel>();
        setupMercurySamples(pdo, expectedSamples, labVessels);

        reset(mocks);
        // Mock should return sample for those that Mercury knows about, i.e. all except the 1st and 4th test samples.
        // The 4th sample is in house so a standalone vessel/sample should be created.
        List<String> pdoSampleNames = new ArrayList<String>();
        List<LabVessel> mockVessels = new ArrayList<LabVessel>();
        for (int rackPosition = 1; rackPosition <= SAMPLE_SIZE; ++rackPosition) {
            ProductOrderSample pdoSample = pdo.getSamples().get(rackPosition - 1);
            if (rackPosition != 1 && rackPosition != 4) {
                mockVessels.add(labVessels.get(rackPosition - 1));
            }
            if (rackPosition == 4) {
                List<LabVessel> mockCreatedVessels = new ArrayList<LabVessel>();
                mockCreatedVessels.add(labVessels.get(rackPosition - 1));
                expect(labVesselFactory.buildInitialLabVessels(eq(pdoSample.getSampleName()),
                        eq(makeTubeBarcode(rackPosition)), eq(pdoCreator), (Date)anyObject()))
                        .andReturn(mockCreatedVessels);
            }

        }
        expect(labVesselDao.findBySampleKeyList((Collection<String>)anyObject())).andReturn(mockVessels);
        expect(bucketDao.findByName(PICO_PLATING_BUCKET)).andReturn(bucket);
        // Should be OK to return more samples in map than was asked for.
        expect(bspSampleDataFetcher.fetchSamplesFromBSP((List<String>)anyObject())).andReturn(bspDtoMap);
        bucketDao.persist(bucket);

        // It's OK to return an empty collection because the returned bucket entries are only needed for rework cases.
        expect(bucketBean.add((List<LabVessel>)anyObject(), eq(bucket),
                eq(BucketEntry.BucketEntryType.PDO_ENTRY), eq(pdoCreator), eq(EVENT_LOCATION), eq(EVENT_TYPE),
                eq(pdo.getBusinessKey()))).andReturn(Collections.<BucketEntry>emptySet());

        replay(mocks);

        Collection<ProductOrderSample> addedSamples = service.addFromProductOrder(pdo);
        Assert.assertEqualsNoOrder(addedSamples.toArray(), expectedSamples.toArray());

        verify(mocks);
    }


    private String makeTubeBarcode(int rackPosition) {
        return "R" + rackPosition;
    }
}
