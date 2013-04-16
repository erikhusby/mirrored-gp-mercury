package org.broadinstitute.gpinformatics.infrastructure.mercury;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
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
    private Map<BSPSampleSearchColumn, String> bspData;
    private WorkflowLoader workflowLoader = new WorkflowLoader();
    private BSPUserList bspUserList;
    private Bucket bucket;
    private String pdoCreator;

    private BucketBean bucketBean = createMock(BucketBean.class);
    private BucketDao bucketDao = createMock(BucketDao.class);
    private LabVesselDao labVesselDao = createMock(LabVesselDao.class);

    private Object[] mocks = new Object[]{bucketBean, bucketDao, labVesselDao};

    @BeforeMethod
    public void setUp() {

        reset(mocks);

        bspData = new HashMap<BSPSampleSearchColumn, String>() {{
            put(BSPSampleSearchColumn.PRIMARY_DISEASE, "Cancer");
            put(BSPSampleSearchColumn.LSID, "org.broad:SM-2345");
            put(BSPSampleSearchColumn.MATERIAL_TYPE, "DNA:DNA WGA Cleaned");
            put(BSPSampleSearchColumn.MATERIAL_TYPE, "DNA:DNA Genomic");
            put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, "5432");
            put(BSPSampleSearchColumn.SPECIES, "Homo Sapiens");
            put(BSPSampleSearchColumn.PARTICIPANT_ID, "PT-2345");
        }};

        bspUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());
        labBatch = new LabBatch("ExEx Receipt Batch", new HashSet<LabVessel>(), LabBatch.LabBatchType.SAMPLES_RECEIPT);
        bucket = new Bucket(new WorkflowStepDef(PICO_PLATING_BUCKET));

        final int SAMPLE_SIZE = 2;

        pdo = ProductOrderTestFactory.buildExExProductOrder(SAMPLE_SIZE);
        pdo.setCreatedBy(BSPManagerFactoryStub.QA_DUDE_USER_ID);
        pdoCreator = bspUserList.getById(BSPManagerFactoryStub.QA_DUDE_USER_ID).getUsername();

        int rackPosition = 1;
        for (ProductOrderSample pdoSample : pdo.getSamples()) {
            LabVessel labVessel = new TwoDBarcodedTube("R" + rackPosition++);

            MercurySample mercurySample =
                    new MercurySample(pdoSample.getSampleName(), new BSPSampleDTO(bspData));

            labVessel.addSample(mercurySample);
            labBatch.addLabVessel(labVessel);
            labVessels.add(labVessel);
        }

        service = new MercuryClientEjb(bucketBean, bucketDao, workflowLoader, bspUserList, labVesselDao);
    }


    public void testSamplesToPicoBucket() throws Exception {

        expect(labVesselDao.findBySampleKeyList((List<String>) anyObject())).andReturn(labVessels);
        expect(bucketDao.findByName(PICO_PLATING_BUCKET)).andReturn(bucket);
        bucketDao.persist(bucket);
        bucketBean.add(labVessels, bucket, pdoCreator, EVENT_LOCATION, EVENT_TYPE, pdo.getBusinessKey());

        replay(mocks);

        Collection<ProductOrderSample> addedSamples = service.addFromProductOrder(pdo);
        Assert.assertEquals(addedSamples, pdo.getSamples());

        verify(mocks);
    }

}
