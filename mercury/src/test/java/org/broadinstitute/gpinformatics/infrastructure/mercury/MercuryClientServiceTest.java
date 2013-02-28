package org.broadinstitute.gpinformatics.infrastructure.mercury;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch_;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.easymock.EasyMock.*;

/**
 * Container test of MercuryClientService
 *
 * @author epolk
 */

@Test(enabled = true, groups = TestGroups.EXTERNAL_INTEGRATION)
public class MercuryClientServiceTest extends Arquillian {
    private String workflowName = "Exome Express";
    private Long userId = 10400L;
    private String pdoKey = "PDO-999";
    private List<String> sampleNames = new ArrayList<String>();

    @Inject
    private MercuryClientService service;
    @Inject
    private LabBatchDAO labBatchDao;

    private ProductOrder pdo = createMock(ProductOrder.class);
    private List<ProductOrderSample> pdoSamples = new ArrayList<ProductOrderSample>();
    private Product product = createMock(Product.class);
    private BSPUserList bspUserList = createMock(BSPUserList.class);
    private BspUser bspUser = createMock(BspUser.class);
    private Object[] mocks = new Object[]{pdo, pdoSamples, product, bspUserList, bspUser};

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @BeforeClass
    public void beforeClass() throws Exception {
    }

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() {
        CriteriaBuilder cb = labBatchDao.getEntityManager().getCriteriaBuilder();
        CriteriaQuery<LabBatch> cq = cb.createQuery(LabBatch.class);
        Root<LabBatch> root = cq.from(LabBatch.class);
        cq.where(cb.equal(root.get(LabBatch_.labBatchType), LabBatch.LabBatchType.SAMPLES_RECEIPT));
        LabBatch receiptBatch = labBatchDao.getEntityManager().createQuery(cq).setMaxResults(1).getSingleResult();
        Collection<LabVessel> receiptVessels = receiptBatch.getStartingLabVessels();
        for (LabVessel receiptVessel : receiptVessels) {
            for (MercurySample receiptSample: receiptVessel.getMercurySamples()) {
                sampleNames.add(receiptSample.getSampleKey());
                pdoSamples.add(createMock(ProductOrderSample.class));
            }
        }
        reset(mocks);
    }

    public void testSampleToPicoBucket() throws Exception {
        expect(pdo.getProduct()).andReturn(product);
        expect(product.getWorkflowName()).andReturn(workflowName).anyTimes();
        expect(pdo.getCreatedBy()).andReturn(userId);
        expect(pdo.getSamples()).andReturn(pdoSamples);
        for (int i = 0; i < sampleNames.size(); ++ i) {
            expect(pdoSamples.get(i).getSampleName()).andReturn(sampleNames.get(i));
        }
        expect(pdo.getBusinessKey()).andReturn(pdoKey);

        replay(mocks);

        Collection<ProductOrderSample> addedSamples = service.addSampleToPicoBucket(pdo);
        Assert.assertEquals(addedSamples.size(), pdoSamples.size());

        verify(mocks);
    }
}
