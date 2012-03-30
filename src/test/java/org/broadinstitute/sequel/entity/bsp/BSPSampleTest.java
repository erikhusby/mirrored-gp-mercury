package org.broadinstitute.sequel.entity.bsp;

import org.broadinstitute.sequel.WeldBooter;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleSearchService;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.broadinstitute.sequel.TestGroups.DATABASE_FREE;
import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;

public class BSPSampleTest extends WeldBooter {

    @Inject
    BSPSampleDataFetcher fetcher;

//    @BeforeClass
//    private void init() {
//        fetcher = weldUtil.getFromContainer(BSPSampleDataFetcher.class);
//    }
    
    @Test(groups = {EXTERNAL_INTEGRATION})
    public void test_patient_id_integration() {
        String sampleName = "SM-12CO4";
        BSPSample bspSample = new BSPSample(sampleName,
                null,
                fetcher.fetchSingleSampleFromBSP(sampleName));
        String patientId = bspSample.getPatientId();

        Assert.assertNotNull(patientId);
        Assert.assertEquals("PT-2LK3",patientId);


    }
    
    @Test(groups = {DATABASE_FREE})
    public void test_patient_id_mock() {
        List<String[]> resultColumns = new ArrayList<String[]>(1);
        resultColumns.add(new String[] {"Bill the Cat","2","3","4","5","6","7"});
        BSPSampleSearchService service = EasyMock.createMock(BSPSampleSearchService.class);
        Collection<String> samplesNames = new ArrayList<String>();
        String sampleName = "Sample1";
        samplesNames.add(sampleName);
        EasyMock.expect(service.runSampleSearch(
                (Collection<String>) EasyMock.anyObject(),
                (BSPSampleSearchColumn)EasyMock.anyObject(),
                (BSPSampleSearchColumn)EasyMock.anyObject(),
                (BSPSampleSearchColumn)EasyMock.anyObject(),
                (BSPSampleSearchColumn)EasyMock.anyObject(),
                (BSPSampleSearchColumn)EasyMock.anyObject(),
                (BSPSampleSearchColumn)EasyMock.anyObject(),
                (BSPSampleSearchColumn)EasyMock.anyObject())
        ).andReturn(resultColumns).atLeastOnce();

        EasyMock.replay(service);
        BSPSample sample = new BSPSample(sampleName,null,new BSPSampleDataFetcher(service).fetchSingleSampleFromBSP(sampleName));
        Assert.assertEquals(resultColumns.iterator().next()[0],sample.getPatientId());
    }
}
