package org.broadinstitute.sequel.entity.bsp;

import org.broadinstitute.sequel.WeldBooter;
import org.broadinstitute.sequel.control.bsp.BSPSampleDataFetcher;
import org.broadinstitute.sequel.control.bsp.BSPSampleSearchColumn;
import org.broadinstitute.sequel.control.bsp.BSPSampleSearchService;
import org.broadinstitute.sequel.entity.bsp.BSPSample;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.broadinstitute.sequel.TestGroups.DATABASE_FREE;
import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;

public class BSPSampleTest extends WeldBooter {

    BSPSampleDataFetcher fetcher;

    @BeforeClass
    private void init() {
        fetcher = weldUtil.getFromContainer(BSPSampleDataFetcher.class);
    }
    
    @Test(groups = {EXTERNAL_INTEGRATION})
    public void test_patient_id_integration() {
        String sampleName = "SM-12CO4";
        BSPSample bspSample = new BSPSample(sampleName,
                null,
                fetcher);
        String patientId = bspSample.getPatientId();

        Assert.assertNotNull(patientId);
        Assert.assertEquals("PT-2LK3",patientId);


    }
    
    @Test(groups = {DATABASE_FREE})
    public void test_patient_id_mock() {
        List<String[]> patiendIds = new ArrayList<String[]>(1);
        patiendIds.add(new String[] {"Bill the Cat"});
        BSPSampleSearchService service = EasyMock.createMock(BSPSampleSearchService.class);
        EasyMock.expect(service.runSampleSearch(
                (Collection<String>) EasyMock.anyObject(),
                (BSPSampleSearchColumn) EasyMock.anyObject())
        ).andReturn(patiendIds).atLeastOnce();

        EasyMock.replay(service);
        BSPSample sample = new BSPSample("Sample1",null,new BSPSampleDataFetcher(service));
        Assert.assertEquals(patiendIds.iterator().next()[0],sample.getPatientId());
    }
}
