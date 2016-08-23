package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Test creation of lab batches through the web service.
 */
public class LabBatchResourceTest extends ContainerTest {
    @Inject
    private LabBatchResource labBatchResource;

    @Test
    public void testBasics() {
        LabBatchBean labBatchBean = new LabBatchBean();
        labBatchBean.setBatchId("ARRAY-6546");
        labBatchBean.setUsername("jowalsh");

        ArrayList<ChildVesselBean> childVesselBeans = new ArrayList<>();
        ChildVesselBean childVesselBean = new ChildVesselBean("SM-CT82G", "SM-CT82G", "Well [200uL]", "A01", "PDO-9377");
        childVesselBeans.add(childVesselBean);
        childVesselBean = new ChildVesselBean("SM-CT82H", "SM-CT82H", "Well [200uL]", "A02", "PDO-9377");
        childVesselBeans.add(childVesselBean);

        ParentVesselBean parentVesselBean = new ParentVesselBean("CO-19484878", null, "Plate 96 Well PCR [200ul]",
                childVesselBeans);
        labBatchBean.setParentVesselBean(parentVesselBean);
        labBatchResource.createLabBatch(labBatchBean);
    }
}