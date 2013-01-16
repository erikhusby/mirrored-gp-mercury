package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Test(groups = TestGroups.DATABASE_FREE)
public class EventEtlTest {

    // 3 successive days starting 1-jan-2013 00:00:00 EST
    static final long[] MSEC_DATES = new long[]{1357016400000L, 1357016400000L + 86400000L, 1357016400000L + 2 * 86400000L};

    private EventEtl eventEtl;
    private LabEventDao labEventDao;
    private ProductOrderDao pdoDao;
    private AuditReaderDao auditReaderDao;
    private WorkflowLoader workflowLoader;
    private WorkflowConfig workflowConfig = buildWorkflowConfig();
    private MercurySample sample1;
    private MercurySample sample2;
    private MercurySample sample3;


    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() throws Exception {
        labEventDao = createMock(LabEventDao.class);
        pdoDao = createMock(ProductOrderDao.class);
        auditReaderDao = createMock(AuditReaderDao.class);
        sample1 = createMock(MercurySample.class);
        sample2 = createMock(MercurySample.class);
        sample3 = createMock(MercurySample.class);

        workflowLoader = createMock(WorkflowLoader.class);
        expect(workflowLoader.load()).andReturn(workflowConfig);
        replay(workflowLoader);

        eventEtl = new EventEtl();
        eventEtl.setLabEventDao(labEventDao);
        eventEtl.setProductOrderDao(pdoDao);
        eventEtl.setAuditReaderDao(auditReaderDao);
        eventEtl.setWorkflowLoader(workflowLoader);
        // Should have at least 4 event names in WorkflowConfig.
        assertTrue(eventEtl.mapEventToWorkflows.size() > 4);

        verify(workflowLoader);
        reset(workflowLoader);
    }

    @Test
    public void testLookupWorkflowForEvent() throws Exception {

        Product product1 = new Product(true);
        product1.setProductName("Product 1");
        product1.setWorkflowName("Workflow 1");

        Product product2 = new Product(true);
        product2.setProductName("Product 2");
        product2.setWorkflowName("Workflow 1");

        Product product3 = new Product(true);
        product3.setProductName("Product 3");
        product3.setWorkflowName("Workflow 2");

        ProductOrder pdo1 = new ProductOrder(0L, null, null, null, product1, null);
        ProductOrder pdo2 = new ProductOrder(0L, null, null, null, product2, null);
        ProductOrder pdo3 = new ProductOrder(0L, null, null, null, product3, null);

        expect(sample1.getProductOrderKey()).andReturn(pdo1.getBusinessKey()).times(5);
        expect(sample2.getProductOrderKey()).andReturn(pdo2.getBusinessKey()).times(1);
        expect(sample3.getProductOrderKey()).andReturn(pdo3.getBusinessKey()).times(1);
        expect(pdoDao.findByBusinessKey(pdo1.getBusinessKey())).andReturn(pdo1).times(5);
        //not called due to cache hit: expect(pdoDao.findByBusinessKey(pdo2.getBusinessKey())).andReturn(pdo2).times(1);
        expect(pdoDao.findByBusinessKey(pdo3.getBusinessKey())).andReturn(pdo3).times(1);
        replay(labEventDao, pdoDao, auditReaderDao, sample1, sample2, sample3);

        // Does a variety of workflow config lookups.  The ugly long expected value is the WorkflowConfigDenorm id,
        // a calculated hash value based on the contents of the WorkflowConfig elements.
        assertEquals(eventEtl.lookupWorkflowConfigId("No such event", sample1, new Date(MSEC_DATES[0] + 1000)), 0L);
        assertEquals(eventEtl.lookupWorkflowConfigId("GSWash1", sample1, new Date(MSEC_DATES[0] - 1000)), 0L);
        assertEquals(eventEtl.lookupWorkflowConfigId("GSWash1", sample1, new Date(MSEC_DATES[0] + 1000)), -3820907449895214598L);
        assertEquals(eventEtl.lookupWorkflowConfigId("GSWash1", sample1, new Date(MSEC_DATES[1] + 1000)), -7175333637954737190L);
        assertEquals(eventEtl.lookupWorkflowConfigId("GSWash1", sample1, new Date(MSEC_DATES[2] + 1000)), 4622114345093982745L);
        // (check log for cache hit on next one)
        assertEquals(eventEtl.lookupWorkflowConfigId("GSWash1", sample2, new Date(MSEC_DATES[2] + 1000)), 4622114345093982745L);
        assertEquals(eventEtl.lookupWorkflowConfigId("SageCleanup", sample3, new Date(MSEC_DATES[2] + 1000)), -6067216737971651861L);

        verify(labEventDao, pdoDao, auditReaderDao, sample1, sample2, sample3);
    }


    static WorkflowConfig buildWorkflowConfig() {
        WorkflowConfig config = new WorkflowConfig();

        // defining workflows
        List<WorkflowProcessDef> workflowList = new ArrayList<WorkflowProcessDef>();

        WorkflowProcessDef w1 = new WorkflowProcessDef("Process 1");

        // these steps valid only for day 0
        WorkflowProcessDefVersion w1v1 = new WorkflowProcessDefVersion("1.0", new Date(MSEC_DATES[0]));
        w1v1.addStep(new WorkflowStepDef("Step 1").addLabEvent(LabEventType.GS_WASH_1));
        w1v1.addStep(new WorkflowStepDef("Step 2").addLabEvent(LabEventType.GS_WASH_2));
        w1v1.addStep(new WorkflowStepDef("Step 3").addLabEvent(LabEventType.GS_WASH_3));
        w1.addWorkflowProcessDefVersion(w1v1);

        // these steps valid only for day 1
        WorkflowProcessDefVersion w1v2 = new WorkflowProcessDefVersion("2.0", new Date(MSEC_DATES[1]));
        w1v2.addStep(new WorkflowStepDef("Step 1").addLabEvent(LabEventType.GS_WASH_4));
        w1v2.addStep(new WorkflowStepDef("Step 2").addLabEvent(LabEventType.GS_WASH_5));
        w1v2.addStep(new WorkflowStepDef("Step 3").addLabEvent(LabEventType.GS_WASH_6));
        w1.addWorkflowProcessDefVersion(w1v2);

        // these steps valid day 2 and later
        WorkflowProcessDefVersion w1v3 = new WorkflowProcessDefVersion("3.0", new Date(MSEC_DATES[2]));
        w1v3.addStep(new WorkflowStepDef("Step 1").addLabEvent(LabEventType.GS_WASH_1));
        w1v3.addStep(new WorkflowStepDef("Step 2").addLabEvent(LabEventType.GS_WASH_2));
        w1.addWorkflowProcessDefVersion(w1v3);
        workflowList.add(w1);

        WorkflowProcessDef w2 = new WorkflowProcessDef("Process 2");
        WorkflowProcessDefVersion w2v1 = new WorkflowProcessDefVersion("1.0", new Date(MSEC_DATES[0]));
        // these steps valid day 0 and later
        w2v1.addStep(new WorkflowStepDef("Step 1").addLabEvent(LabEventType.SAGE_LOADING));
        w2v1.addStep(new WorkflowStepDef("Step 2").addLabEvent(LabEventType.SAGE_LOADED));
        w2v1.addStep(new WorkflowStepDef("Step 3").addLabEvent(LabEventType.SAGE_UNLOADING));
        w2v1.addStep(new WorkflowStepDef("Step 4").addLabEvent(LabEventType.SAGE_CLEANUP));
        w2.addWorkflowProcessDefVersion(w2v1);
        workflowList.add(w2);

        config.setWorkflowProcessDefs(workflowList);

        // defining products
        List<ProductWorkflowDef> pList = new ArrayList<ProductWorkflowDef>();

        ProductWorkflowDef p1 = new ProductWorkflowDef("Workflow 1");
        // only valid day 0
        ProductWorkflowDefVersion p1v1 = new ProductWorkflowDefVersion("1.0", new Date(MSEC_DATES[0]));
        p1v1.addWorkflowProcessDef(w1);
        p1.addProductWorkflowDefVersion(p1v1);
        // valid day 1 and later
        ProductWorkflowDefVersion p1v2 = new ProductWorkflowDefVersion("2.0", new Date(MSEC_DATES[1]));
        p1v2.addWorkflowProcessDef(w1);
        p1.addProductWorkflowDefVersion(p1v2);
        pList.add(p1);

        ProductWorkflowDef p2 = new ProductWorkflowDef("Workflow 2");
        // valid day 0 and later
        ProductWorkflowDefVersion p2v1 = new ProductWorkflowDefVersion("1.0", new Date(MSEC_DATES[0]));
        p2v1.addWorkflowProcessDef(w2);
        p2.addProductWorkflowDefVersion(p2v1);
        pList.add(p2);

        config.setProductWorkflowDefs(pList);

        return config;
    }
}
