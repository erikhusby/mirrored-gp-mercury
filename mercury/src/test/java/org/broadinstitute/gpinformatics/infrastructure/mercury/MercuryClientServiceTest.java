package org.broadinstitute.gpinformatics.infrastructure.mercury;

import org.apache.http.cookie.SM;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.datawh.ProductOrderEtl;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
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
import java.util.logging.Logger;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.easymock.EasyMock.*;

/**
 * Container test of MercuryClientService
 *
 * @author epolk
 */

@Test(enabled = true, groups = TestGroups.EXTERNAL_INTEGRATION)
public class MercuryClientServiceTest extends Arquillian {
    private Logger logger = Logger.getLogger(getClass().getName());
    private String workflowName = "Exome Express";
    private Long userId = 10400L;
    private String userName = "testUser";
    private String sampleName1 = "SM-1234-0224204803";
    private String sampleName2 = "SM-2345-0224204803";
    private String pdoKey = "PDO-8";

    @Inject
    private MercuryClientService service;

    private ProductOrder pdo = createMock(ProductOrder.class);
    private List<ProductOrderSample> pdoSamples = new ArrayList<ProductOrderSample>();
    private ProductOrderSample pdoSample1 = createMock(ProductOrderSample.class);
    private ProductOrderSample pdoSample2 = createMock(ProductOrderSample.class);
    private Product product = createMock(Product.class);
    private BSPUserList bspUserList = createMock(BSPUserList.class);
    private BspUser bspUser = createMock(BspUser.class);
    private Object[] mocks = new Object[]{pdo, pdoSample1, pdoSample2, product, bspUserList, bspUser};

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @BeforeClass
    public void beforeClass() throws Exception {
        //((MercuryClientServiceImpl)service).setUserList(bspUserList);
        // BucketBean(LabEventFactory labEventFactoryIn, JiraService testjiraService, LabBatchEjb batchEjb)
        pdoSamples.add(pdoSample1);
        pdoSamples.add(pdoSample2);
    }

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() {
        reset(mocks);
    }

    public void testSampleToPicoBucket() throws Exception {
        expect(pdo.getProduct()).andReturn(product);
        expect(product.getWorkflowName()).andReturn(workflowName).anyTimes();
        expect(pdo.getCreatedBy()).andReturn(userId);
        //expect(bspUserList.getById(userId)).andReturn(bspUser);
        //expect(bspUser.getUsername()).andReturn(userName);
        expect(pdo.getSamples()).andReturn(pdoSamples);
        expect(pdoSample1.getSampleName()).andReturn(sampleName1);
        expect(pdoSample2.getSampleName()).andReturn(sampleName2);
        expect(pdo.getBusinessKey()).andReturn(pdoKey);

        replay(mocks);

        Collection<ProductOrderSample> addedSamples = service.addSampleToPicoBucket(pdo);
        Assert.assertEquals(addedSamples.size(), 2);

        verify(mocks);
    }
}
