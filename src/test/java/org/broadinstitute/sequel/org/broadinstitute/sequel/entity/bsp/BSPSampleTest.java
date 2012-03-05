package org.broadinstitute.sequel.org.broadinstitute.sequel.entity.bsp;

import org.broadinstitute.sequel.control.bsp.BSPSampleSearchService;
import org.broadinstitute.sequel.entity.bsp.BSPSample;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class BSPSampleTest {

    BSPSampleSearchService service;

    @BeforeClass(groups = "ExternalIntegration")
    public void initWeld() {
        WeldContainer weld = new Weld().initialize();
        service = weld.instance().select(BSPSampleSearchService.class).get();
    }

    @Test(groups = {"ExternalIntegration"})
    public void test_patient_id() {

        BSPSample bspSample = new BSPSample("SM-12CO4",
                null,
                service);
        String patientId = bspSample.getPatientId();

        Assert.assertNotNull(patientId);
        Assert.assertEquals("PT-2LK3",patientId);
    }
}
