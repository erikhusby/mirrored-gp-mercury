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

/**
 * DbFree test of MercuryClientService
 * @author epolk
 */

@Test(enabled = true, groups = TestGroups.DATABASE_FREE)
public class MercuryClientServiceDbFreeTest {
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

    private MercuryClientService service;
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
        service = new MercuryClientServiceImpl(bucketBean, bucketDao, workflowLoader, bspUserList, labVesselDao);
        pdoSamples.add(pdoSample1);
        labVessels.add(labVessel);
        samples.add(mercurySample);
        batches.add(labBatch);
    }

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() {
        reset(mocks);
    }

    public void testSampleToPicoBucket() throws Exception {
        expect(pdo.getSamples()).andReturn(pdoSamples);
        expect(pdoSample1.getSampleName()).andReturn(sampleKey);
        expect(labVesselDao.findBySampleKeyList((List<String>)anyObject())).andReturn(labVessels);
        expect(labVessel.getLabBatches()).andReturn(batches);
        //if (batches.size() == 0)   batches = vessel.getNearestLabBatches();
        expect(labBatch.getLabBatchType()).andReturn(LabBatch.LabBatchType.SAMPLES_RECEIPT);
        expect(labVessel.getMercurySamples()).andReturn(samples);
        expect(mercurySample.getSampleKey()).andReturn(sampleKey).times(2);

        expect(pdo.getProduct()).andReturn(product);
        expect(product.getWorkflowName()).andReturn(workflowName).anyTimes();
        expect(bucketDao.findByName("Pico/Plating Bucket")).andReturn(bucket);
        expect(pdo.getCreatedBy()).andReturn(userId);
        expect(bspUserList.getById(userId)).andReturn(bspUser);
        expect(bspUser.getUsername()).andReturn(userName);

        expect(pdo.getBusinessKey()).andReturn(pdoKey);

        bucketBean.add(labVessels, bucket, userName, eventLocation, eventType, pdoKey);
        replay(mocks);

        Collection<ProductOrderSample> addedSamples = service.addSampleToPicoBucket(pdo);
        Assert.assertEquals(addedSamples.size(), 1);

        verify(mocks);
    }
}
