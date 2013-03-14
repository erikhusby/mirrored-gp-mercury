package org.broadinstitute.gpinformatics.infrastructure.mercury;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
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
    private Log logger = LogFactory.getLog(getClass());
    private String workflowName = "Exome Express";  // must be used in WorkflowConfig.xml
    private Long userId = 10400L; // must be user BSP knows about
    private String pdoKey = "PDO-8";  // must exist on test jira (receives the comments put by BucketBean)
    private List<ProductOrderSample> pdoSamples = new ArrayList<ProductOrderSample>();

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private MercuryClientService service;
    @Inject
    private LabBatchDAO labBatchDAO;

    private ProductOrder pdo = createMock(ProductOrder.class);
    private Product product = createMock(Product.class);
    private BSPUserList bspUserList = createMock(BSPUserList.class);
    private BspUser bspUser = createMock(BspUser.class);
    private BSPSampleDTO bspSampleDto = createMock(BSPSampleDTO.class);
    private Object[] mocks = new Object[]{pdo, product, bspUserList, bspUser, bspSampleDto};

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @BeforeClass
    public void beforeClass() throws Exception {
    }

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() {
        pdoSamples.clear();
        reset(mocks);
    }

    private boolean setUpSamplesFromReceiptBatch() {
        // For the bucket entry criteria to work, the samples have to exist in BSP.
        // todo jmt How to recreate the test data after a database refresh?
        LabBatch labBatch = labBatchDAO.findByName("SK-27D9");
        if (labBatch == null) {
            return false;
        }
        Collection<LabVessel> receiptVessels = labBatch.getStartingLabVessels();
            for (LabVessel receiptVessel : receiptVessels) {
                for (MercurySample receiptSample: receiptVessel.getMercurySamples()) {
                    String sampleName = receiptSample.getSampleKey();
                    ProductOrderSample pdoSample = new ProductOrderSample(sampleName, bspSampleDto);
                    pdoSamples.add(pdoSample);
                }
            }
        logger.info("Testing with " + pdoSamples.size() + " receipt samples");
        return true;
    }

    public void testSampleToPicoBucket() throws Exception {
        if (setUpSamplesFromReceiptBatch()) {
            expect(pdo.getProduct()).andReturn(product);
            expect(product.getWorkflowName()).andReturn(workflowName).anyTimes();
            expect(pdo.getCreatedBy()).andReturn(userId);
            expect(pdo.getSamples()).andReturn(pdoSamples);
            expect(pdo.getBusinessKey()).andReturn(pdoKey);

            replay(mocks);

            Collection<ProductOrderSample> addedSamples = service.addSampleToPicoBucket(pdo);
            Assert.assertEquals(addedSamples.size(), pdoSamples.size());

            verify(mocks);
        } else {
            logger.info("Skipping test due to missing test data");
        }
    }

    public void testNoReceiptSamples() throws Exception {
        expect(pdo.getProduct()).andReturn(product);
        expect(product.getWorkflowName()).andReturn(workflowName).anyTimes();
        expect(pdo.getCreatedBy()).andReturn(userId);
        expect(pdo.getSamples()).andReturn(pdoSamples);
        expect(pdo.getBusinessKey()).andReturn(pdoKey);

        replay(mocks);

        Collection<ProductOrderSample> addedSamples = service.addSampleToPicoBucket(pdo);
        Assert.assertEquals(addedSamples.size(), pdoSamples.size());

        verify(mocks);
    }
}
