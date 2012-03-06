package org.broadinstitute.sequel.org.broadinstitute.sequel.entity.bsp;

import org.broadinstitute.sequel.control.bsp.BSPSampleSearchColumn;
import org.broadinstitute.sequel.control.bsp.BSPSampleSearchService;
import org.broadinstitute.sequel.entity.bsp.BSPSample;
import org.easymock.EasyMock;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class BSPSampleTest {

    
    @Test(groups = {"ExternalIntegration"})
    public void test_patient_id_integration() {
        WeldContainer weld = new Weld().initialize();
        BSPSampleSearchService service = weld.instance().select(BSPSampleSearchService.class).get();
        
        BSPSample bspSample = new BSPSample("SM-12CO4",
                null,
                service);
        String patientId = bspSample.getPatientId();

        Assert.assertNotNull(patientId);
        Assert.assertEquals("PT-2LK3",patientId);
    }
    
    @Test(groups = {"DatabaseFree"})
    public void test_patient_id_mock() {
        List<String[]> patiendIds = new ArrayList<String[]>(1);
        patiendIds.add(new String[] {"Bill the Cat"});
        BSPSampleSearchService service = EasyMock.createMock(BSPSampleSearchService.class);
        EasyMock.expect(service.runSampleSearch(
                (Collection<String>) EasyMock.anyObject(),
                (BSPSampleSearchColumn) EasyMock.anyObject())
        ).andReturn(patiendIds).atLeastOnce();

        EasyMock.replay(service);
        BSPSample sample = new BSPSample("Sample1",null,service);
        Assert.assertEquals(patiendIds.iterator().next()[0],sample.getPatientId());
    }
}
