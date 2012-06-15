package org.broadinstitute.sequel.test.entity.bsp;

import org.broadinstitute.sequel.entity.bsp.BSPStartingSample;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleSearchService;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.broadinstitute.sequel.TestGroups.DATABASE_FREE;

public class DBFreeBSPSampleTest {

    @Test(groups = {DATABASE_FREE})
    public void test_patient_id_mock() {
        List<String[]> resultColumns = new ArrayList<String[]>(1);
        resultColumns.add(new String[] {"Bill the Cat","2","3","4","5","6","7","8"});
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
                (BSPSampleSearchColumn)EasyMock.anyObject(),
                (BSPSampleSearchColumn)EasyMock.anyObject())
        ).andReturn(resultColumns).atLeastOnce();

        EasyMock.replay(service);
        BSPStartingSample sample = new BSPStartingSample(sampleName,null,new BSPSampleDataFetcher(service).fetchSingleSampleFromBSP(sampleName));
        Assert.assertEquals(resultColumns.iterator().next()[0], sample.getPatientId());
    }
}
