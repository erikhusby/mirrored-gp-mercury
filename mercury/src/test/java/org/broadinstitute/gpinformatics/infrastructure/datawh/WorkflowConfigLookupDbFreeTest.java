package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

/**
 * unit test of Event Etl doing a lookup of WorkflowConfigDenorm.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class WorkflowConfigLookupDbFreeTest {

    // 3 successive days starting 1-jan-2013 00:00:00 EST
    static final long[] MSEC_DATES = new long[]{1357016400000L, 1357016400000L + 86400000L, 1357016400000L + 2 * 86400000L};

    private WorkflowConfigLookup wfConfigLookup;
    private WorkflowConfig workflowConfig = buildWorkflowConfig();


    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() throws Exception {

        WorkflowLoader workflowLoader = createMock(WorkflowLoader.class);
        expect(workflowLoader.load()).andReturn(workflowConfig);
        replay(workflowLoader);

        wfConfigLookup = new WorkflowConfigLookup();
        wfConfigLookup.setWorkflowLoader(workflowLoader);

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

        List<ProductOrderSample> emptyList = Collections.emptyList();
        ProductOrder pdo1 = new ProductOrder(0L, "", emptyList, null, product1, null);
        ProductOrder pdo2 = new ProductOrder(0L, "", emptyList, null, product2, null);
        ProductOrder pdo3 = new ProductOrder(0L, "", emptyList, null, product3, null);

        // Does lookups based on product and date.  Checks the denorm cache for hits.
        assertNull(wfConfigLookup.lookupWorkflowConfig("No such event", pdo1, new Date(MSEC_DATES[0] + 1000)));
        assertNull(wfConfigLookup.lookupWorkflowConfig("GSWash1", pdo1, new Date(MSEC_DATES[0] - 1000)));
        assertEquals((Long)wfConfigLookup.lookupWorkflowConfig("GSWash1", pdo1, new Date(MSEC_DATES[0] + 1000)).getWorkflowConfigDenormId(),
                Long.valueOf(7366990729258982731L));
        assertEquals(wfConfigLookup.cacheHit, 0);
        assertEquals((Long)wfConfigLookup.lookupWorkflowConfig("GSWash1", pdo1, new Date(MSEC_DATES[1] + 1000)).getWorkflowConfigDenormId(),
                Long.valueOf(1625593456990639556L));
        assertEquals(wfConfigLookup.cacheHit, 0);
        assertEquals((Long)wfConfigLookup.lookupWorkflowConfig("GSWash1", pdo1, new Date(MSEC_DATES[2] + 1000)).getWorkflowConfigDenormId(),
                Long.valueOf(-2472811271328835449L));
        assertEquals(wfConfigLookup.cacheHit, 0);
        assertEquals((Long)wfConfigLookup.lookupWorkflowConfig("GSWash1", pdo2, new Date(MSEC_DATES[2] + 1000)).getWorkflowConfigDenormId(),
                Long.valueOf(-2472811271328835449L));
        assertEquals(wfConfigLookup.cacheHit, 1);
        assertEquals((Long)wfConfigLookup.lookupWorkflowConfig("SageCleanup", pdo3, new Date(MSEC_DATES[2] + 1000)).getWorkflowConfigDenormId(),
                Long.valueOf(-961977840485104866L));
        assertEquals(wfConfigLookup.cacheHit, 1);
        assertEquals((Long)wfConfigLookup.lookupWorkflowConfig("GSWash1", pdo2, new Date(MSEC_DATES[2] + 1000)).getWorkflowConfigDenormId(),
                Long.valueOf(-2472811271328835449L));
        assertEquals(wfConfigLookup.cacheHit, 2);
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
