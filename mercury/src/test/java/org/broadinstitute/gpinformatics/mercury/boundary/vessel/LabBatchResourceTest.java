package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Test creation of lab batches through the web service.
 */
@Test(groups = TestGroups.STANDARD)
public class LabBatchResourceTest extends Arquillian {
    @Inject
    private LabBatchResource labBatchResource;

    @Inject
    private LabBatchDao labBatchDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Test
    public void testBasics() {
        LabBatchBean labBatchBean = new LabBatchBean();
        String batchId = "ARRAY-" + System.currentTimeMillis();
        labBatchBean.setBatchId(batchId);
        labBatchBean.setUsername("jowalsh");

        ArrayList<ChildVesselBean> childVesselBeans = new ArrayList<>();
        ChildVesselBean childVesselBean = new ChildVesselBean("SM-CT82G", "SM-CT82G", "Well [200uL]", "A01");
        childVesselBeans.add(childVesselBean);
        childVesselBean = new ChildVesselBean("SM-CT82H", "SM-CT82H", "Well [200uL]", "A02");
        childVesselBeans.add(childVesselBean);

        ParentVesselBean parentVesselBean = new ParentVesselBean("CO-19484878", null, "Plate 96 Well PCR [200ul]",
                childVesselBeans);
        labBatchBean.setParentVesselBean(parentVesselBean);
        labBatchBean.setWorkflowName("Infinium");
        labBatchResource.createLabBatch(labBatchBean);

        LabBatch labBatch = labBatchDao.findByBusinessKey(batchId);
        // Bucket entries never happen on these samples.
        // See PDO logic in LabBatchResource#createLabBatchByParentVessel - Product = PICO Only, PDO status = Completed
        Assert.assertEquals(labBatch.getBucketEntries().size(), 0);
        // Batch starting vessels do happen
        Assert.assertEquals(labBatch.getLabBatchStartingVessels().size(), 2);
    }
}