package org.broadinstitute.gpinformatics.infrastructure.mercury;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

import static org.easymock.EasyMock.*;
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
    private String workflowName = "Exome Express";
    private Long userId = 10400L;
    private String userName = "testUser";
    private String sampleKey = "SM-0000-0225135703";
    private String pdoKey = "PDO-0000";
    private List<LabVessel> labVessels = new ArrayList<LabVessel>();
    private Set<MercurySample> samples = new HashSet<MercurySample>();
    private Set<LabBatch> batches = new HashSet<LabBatch>();
    private LabEventType eventType = LabEventType.PICO_PLATING_BUCKET;
    private String eventLocation = LabEvent.UI_EVENT_LOCATION;

    private MercuryClientEjb service;
    private ProductOrder pdo = createMock(ProductOrder.class);
    private List<ProductOrderSample> pdoSamples = new ArrayList<ProductOrderSample>();
    private ProductOrderSample pdoSample1 = createMock(ProductOrderSample.class);
    private ProductOrderSample pdoSample2 = createMock(ProductOrderSample.class);
    private Product product = createMock(Product.class);
    private BSPUserList bspUserList = createMock(BSPUserList.class);
    private BspUser bspUser = createMock(BspUser.class);
    private BucketBean bucketBean = createMock(BucketBean.class);
    private BucketDao bucketDao = createMock(BucketDao.class);
    private Bucket bucket = createMock(Bucket.class);
    private WorkflowLoader workflowLoader = new WorkflowLoader(); // = createMock(WorkflowLoader.class);
    private LabBatch labBatch = createMock(LabBatch.class);
    private LabVessel labVessel = createMock(LabVessel.class);
    private MercurySample mercurySample = createMock(MercurySample.class);
    private LabVesselDao labVesselDao = createMock(LabVesselDao.class);

    private Object[] mocks = new Object[]{pdo, pdoSample1, pdoSample2, product, bspUserList, bspUser, bucketBean,
            bucketDao, bucket, labBatch, labVessel, mercurySample, labVesselDao};

    @BeforeClass
    public void beforeClass() throws Exception {
        service = new MercuryClientEjb(bucketBean, bucketDao, workflowLoader, bspUserList, labVesselDao);
        labVessels.add(labVessel);
        samples.add(mercurySample);
        batches.add(labBatch);
    }

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() {
        pdoSamples = new ArrayList<ProductOrderSample>();
        pdoSamples.add(pdoSample1);
        reset(mocks);
    }

    public void testSampleToPicoBucket() throws Exception {
        // subtle differences from testAddSampleToPicoBucketSubset: pdoSamples contains only pdoSample1, and pdo.getSamples() is expected and returns pdoSamples
        expect(pdo.getSamples()).andReturn(pdoSamples);
        expect(pdoSample1.getSampleName()).andReturn(sampleKey);
        expect(labVesselDao.findBySampleKeyList((List<String>)anyObject())).andReturn(labVessels);
        expect(labVessel.getLabBatches()).andReturn(batches);
        //if (batches.size() == 0)   batches = vessel.getNearestLabBatches();
        expect(labBatch.getLabBatchType()).andReturn(LabBatch.LabBatchType.SAMPLES_RECEIPT);
        expect(labVessel.getMercurySamples()).andReturn(samples);
        expect(mercurySample.getSampleKey()).andReturn(sampleKey);

        expect(pdo.getProduct()).andReturn(product);
        expect(product.getWorkflowName()).andReturn(workflowName).anyTimes();
        expect(bucketDao.findByName("Pico/Plating Bucket")).andReturn(bucket);
        expect(pdo.getCreatedBy()).andReturn(userId);
        expect(bspUserList.getById(userId)).andReturn(bspUser);
        expect(bspUser.getUsername()).andReturn(userName);

        expect(pdo.getBusinessKey()).andReturn(pdoKey);

        bucketBean.add(labVessels, bucket, userName, eventLocation, eventType, pdoKey);
        expect(bucket.getBucketId()).andReturn(10L);
        replay(mocks);

        Collection<ProductOrderSample> addedSamples = service.addFromProductOrder(pdo);
        Assert.assertEquals(addedSamples.size(), 1);

        verify(mocks);
    }

    public void testAddSampleToPicoBucketSubset() {
        // subtle differences from testSampleToPicoBucket: pdoSamples contains pdoSample1 and pdoSample2, and pdo.getSamples() is not expected
        pdoSamples.add(pdoSample2);
        expect(pdoSample1.getSampleName()).andReturn(sampleKey);
        expect(labVesselDao.findBySampleKeyList((List<String>)anyObject())).andReturn(labVessels);
        expect(labVessel.getLabBatches()).andReturn(batches);
        expect(labBatch.getLabBatchType()).andReturn(LabBatch.LabBatchType.SAMPLES_RECEIPT);
        expect(labVessel.getMercurySamples()).andReturn(samples);
        expect(mercurySample.getSampleKey()).andReturn(sampleKey);

        expect(pdo.getProduct()).andReturn(product);
        expect(product.getWorkflowName()).andReturn(workflowName).anyTimes();
        expect(bucketDao.findByName("Pico/Plating Bucket")).andReturn(bucket);
        expect(pdo.getCreatedBy()).andReturn(userId);
        expect(bspUserList.getById(userId)).andReturn(bspUser);
        expect(bspUser.getUsername()).andReturn(userName);

        expect(pdo.getBusinessKey()).andReturn(pdoKey);

        Collection<LabVessel> bucketVessels = new ArrayList<LabVessel>();
        bucketVessels.add(labVessel);
        bucketBean.add(bucketVessels, bucket, userName, eventLocation, eventType, pdoKey);
        expect(bucket.getBucketId()).andReturn(10L);
        replay(mocks);

        Collection<ProductOrderSample> addedSamples = service.addFromProductOrder(pdo, Collections.singleton(pdoSample1));
        Assert.assertEquals(addedSamples.size(), 1);

        verify(mocks);
    }
}
