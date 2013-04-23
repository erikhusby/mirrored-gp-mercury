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
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
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

import static org.easymock.EasyMock.*;

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

    private List<LabVessel> labVessels = new ArrayList<LabVessel>();
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

    private Object[] mocks = new Object[]{bucketBean, bucketDao, labVesselDao, bspSampleDataFetcher};

    @BeforeMethod
    public void setUp() {

        reset(mocks);

        bspUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());
        labBatch = new LabBatch("ExEx Receipt Batch", new HashSet<LabVessel>(), LabBatch.LabBatchType.SAMPLES_RECEIPT);
        bucket = new Bucket(new WorkflowStepDef(PICO_PLATING_BUCKET));
        bspDtoMap = new HashMap<String, BSPSampleDTO>();

        final int SAMPLE_SIZE = 5;

        pdo = ProductOrderTestFactory.buildExExProductOrder(SAMPLE_SIZE);
        pdo.setCreatedBy(BSPManagerFactoryStub.QA_DUDE_USER_ID);
        pdoCreator = bspUserList.getById(BSPManagerFactoryStub.QA_DUDE_USER_ID).getUsername();

        service = new MercuryClientEjb(bucketBean, bucketDao, workflowLoader, bspUserList, labVesselDao,
                bspSampleDataFetcher);
    }


    public void testSamplesToPicoBucket() throws Exception {
        Collection<ProductOrderSample> expectedSamples = new ArrayList<ProductOrderSample>();

        int rackPosition = 1;
        for (ProductOrderSample pdoSample : pdo.getSamples()) {
            LabVessel labVessel = new TwoDBarcodedTube("R" + rackPosition++);

            boolean expectedInBucket;
            Map<BSPSampleSearchColumn, String> bspData = new HashMap<BSPSampleSearchColumn, String>();
            switch (rackPosition) {
                case 1:
                    // Unreceived root.
                    bspData.put(BSPSampleSearchColumn.MATERIAL_TYPE, "DNA:DNA Genomic");
                    bspData.put(BSPSampleSearchColumn.SAMPLE_ID, pdoSample.getSampleName());
                    bspData.put(BSPSampleSearchColumn.ROOT_SAMPLE, pdoSample.getSampleName());
                    bspData.put(BSPSampleSearchColumn.RECEIPT_DATE, null);
                    expectedInBucket = false;
                    break;

                case 2:
                    // Non-genomic material.
                    bspData.put(BSPSampleSearchColumn.MATERIAL_TYPE, "Tissue:Blood");
                    bspData.put(BSPSampleSearchColumn.SAMPLE_ID, pdoSample.getSampleName());
                    bspData.put(BSPSampleSearchColumn.ROOT_SAMPLE, null);
                    expectedInBucket = false;
                    break;

                case 3:
                    // Received root.
                    bspData.put(BSPSampleSearchColumn.MATERIAL_TYPE, "DNA:DNA Genomic");
                    bspData.put(BSPSampleSearchColumn.ROOT_SAMPLE, pdoSample.getSampleName());
                    bspData.put(BSPSampleSearchColumn.SAMPLE_ID, pdoSample.getSampleName());
                    bspData.put(BSPSampleSearchColumn.RECEIPT_DATE, "2013-04-22");
                    expectedInBucket = true;
                    break;

                default:
                    // Non-root samples.
                    bspData.put(BSPSampleSearchColumn.MATERIAL_TYPE, "DNA:DNA Genomic");
                    bspData.put(BSPSampleSearchColumn.ROOT_SAMPLE, null);
                    bspData.put(BSPSampleSearchColumn.SAMPLE_ID, pdoSample.getSampleName());
                    expectedInBucket = true;
                    break;
            }
            BSPSampleDTO bspDto = new BSPSampleDTO(bspData);
            MercurySample mercurySample = new MercurySample(pdo.getBusinessKey(), pdoSample.getSampleName(), bspDto);

            if (expectedInBucket) {
                expectedSamples.add(pdoSample);
            }

            labVessel.addSample(mercurySample);
            labBatch.addLabVessel(labVessel);
            labVessels.add(labVessel);
            bspDtoMap.put(pdoSample.getSampleName(), bspDto);
        }

        expect(labVesselDao.findBySampleKeyList((List<String>) anyObject())).andReturn(labVessels);
        expect(bucketDao.findByName(PICO_PLATING_BUCKET)).andReturn(bucket);
        expect(bspSampleDataFetcher.fetchSamplesFromBSP((List<String>)anyObject())).andReturn(bspDtoMap);
        bucketDao.persist(bucket);

        bucketBean.add((List<LabVessel>)anyObject(), eq(bucket), eq(pdoCreator), eq(EVENT_LOCATION), eq(EVENT_TYPE),
                eq(pdo.getBusinessKey()));

        replay(mocks);

        Collection<ProductOrderSample> addedSamples = service.addFromProductOrder(pdo);
        Assert.assertEqualsNoOrder(addedSamples.toArray(), expectedSamples.toArray());

        verify(mocks);
    }

}
