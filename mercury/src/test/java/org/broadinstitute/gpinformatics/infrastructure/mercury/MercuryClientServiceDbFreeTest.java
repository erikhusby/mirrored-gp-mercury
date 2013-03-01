package org.broadinstitute.gpinformatics.infrastructure.mercury;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;
import java.util.logging.Logger;

import static org.easymock.EasyMock.*;

/**
 * DbFree test of MercuryClientService.
 *
 * singleThreaded because EasyMock mocks are not thread-safe during recording.
 *
 * @author epolk
 * @author breilly
 */
@Test(enabled = true, groups = TestGroups.DATABASE_FREE, singleThreaded = true)
public class MercuryClientServiceDbFreeTest {
    private Logger logger = Logger.getLogger(getClass().getName());
    private String workflowName = "Exome Express";
    private Long userId = 10400L;
    private String userName = "testUser";
    private String sampleKey = "SM-0000-0225135703";
    private String pdoKey = "PDO-0000";
    private Collection<LabBatch> labBatches = new ArrayList<LabBatch>();
    private Set<LabVessel> labVessels = new HashSet<LabVessel>();
    private Set<MercurySample> samples = new HashSet<MercurySample>();
    private LabEventType eventType = LabEventType.PICO_PLATING_BUCKET;
    private String eventLocation = "BSP";

    private MercuryClientService service;
    private ProductOrder pdo = createMock(ProductOrder.class);
    private List<ProductOrderSample> pdoSamples;
    private ProductOrderSample pdoSample1 = createMock(ProductOrderSample.class);
    private ProductOrderSample pdoSample2 = createMock(ProductOrderSample.class);
    private Product product = createMock(Product.class);
    private BSPUserList bspUserList = createMock(BSPUserList.class);
    private BspUser bspUser = createMock(BspUser.class);
    private BucketBean bucketBean = createMock(BucketBean.class);
    private BucketDao bucketDao = createMock(BucketDao.class);
    private Bucket bucket = createMock(Bucket.class);
    private LabBatchDAO labBatchDao = createMock(LabBatchDAO.class);
    private WorkflowLoader workflowLoader = new WorkflowLoader(); // = createMock(WorkflowLoader.class);
    private LabBatch labBatch = createMock(LabBatch.class);
    private LabVessel labVessel = createMock(LabVessel.class);
    private MercurySample sample = createMock(MercurySample.class);

    private Object[] mocks = new Object[]{pdo, pdoSample1, pdoSample2, product, bspUserList, bspUser, bucketBean,
            bucketDao, bucket, labBatchDao, labBatch, labVessel, sample};

    @BeforeClass
    public void beforeClass() throws Exception {
        service = new MercuryClientServiceImpl(bucketBean, bucketDao, labBatchDao, workflowLoader, bspUserList);
        labBatches.add(labBatch);
        labVessels.add(labVessel);
        samples.add(sample);
    }

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() {
        pdoSamples = new ArrayList<ProductOrderSample>();
        pdoSamples.add(pdoSample1);
        reset(mocks);
    }

    public void testSampleToPicoBucket() throws Exception {
        // subtle differences from testAddSampleToPicoBucketSubset: pdoSamples contains only pdoSample1, and pdo.getSamples() is expected and returns pdoSamples
        expect(labBatchDao.findBatchesInReceiving()).andReturn(labBatches);
        expect(labBatch.getStartingLabVessels()).andReturn(labVessels);
        expect(labVessel.getMercurySamples()).andReturn(samples);
        expect(sample.getSampleKey()).andReturn(sampleKey);
        expect(pdo.getProduct()).andReturn(product);
        expect(product.getWorkflowName()).andReturn(workflowName).anyTimes();
        expect(bucketDao.findByName("Pico/Plating Bucket")).andReturn(bucket);
        expect(pdo.getCreatedBy()).andReturn(userId);
        expect(bspUserList.getById(userId)).andReturn(bspUser);
        expect(bspUser.getUsername()).andReturn(userName);
        expect(pdo.getSamples()).andReturn(pdoSamples);
        expect(pdoSample1.getSampleName()).andReturn(sampleKey);
        expect(pdo.getBusinessKey()).andReturn(pdoKey);
        Collection<LabVessel> bucketVessels = new ArrayList<LabVessel>();
        bucketVessels.add(labVessel);
        bucketBean.add(bucketVessels, bucket, userName, eventLocation, eventType, pdoKey);
        replay(mocks);

        Collection<ProductOrderSample> addedSamples = service.addSampleToPicoBucket(pdo);
        Assert.assertEquals(addedSamples.size(), 1);

        verify(mocks);
    }

    public void testAddSampleToPicoBucketSubset() {
        // subtle differences from testSampleToPicoBucket: pdoSamples contains pdoSample1 and pdoSample2, and pdo.getSamples() is not expected
        pdoSamples.add(pdoSample2);
        expect(labBatchDao.findBatchesInReceiving()).andReturn(labBatches);
        expect(labBatch.getStartingLabVessels()).andReturn(labVessels);
        expect(labVessel.getMercurySamples()).andReturn(samples);
        expect(sample.getSampleKey()).andReturn(sampleKey);
        expect(pdo.getProduct()).andReturn(product);
        expect(product.getWorkflowName()).andReturn(workflowName).anyTimes();
        expect(bucketDao.findByName("Pico/Plating Bucket")).andReturn(bucket);
        expect(pdo.getCreatedBy()).andReturn(userId);
        expect(bspUserList.getById(userId)).andReturn(bspUser);
        expect(bspUser.getUsername()).andReturn(userName);
        expect(pdoSample1.getSampleName()).andReturn(sampleKey);
        expect(pdo.getBusinessKey()).andReturn(pdoKey);
        Collection<LabVessel> bucketVessels = new ArrayList<LabVessel>();
        bucketVessels.add(labVessel);
        bucketBean.add(bucketVessels, bucket, userName, eventLocation, eventType, pdoKey);
        replay(mocks);

        Collection<ProductOrderSample> addedSamples = service.addSampleToPicoBucket(pdo, Collections.singleton(pdoSample1));
        Assert.assertEquals(addedSamples.size(), 1);

        verify(mocks);
    }
}
