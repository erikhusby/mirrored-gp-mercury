package org.broadinstitute.pmbridge.entity.bsp;

import org.broadinstitute.pmbridge.WeldBooter;
import org.broadinstitute.pmbridge.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.pmbridge.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.pmbridge.infrastructure.bsp.BSPSampleSearchService;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.broadinstitute.pmbridge.TestGroups.DATABASE_FREE;
import static org.broadinstitute.pmbridge.TestGroups.EXTERNAL_INTEGRATION;

public class BSPSampleTest extends WeldBooter {

    BSPSampleDataFetcher fetcher;

    @BeforeClass
    private void init() {
        fetcher = weldUtil.getFromContainer(BSPSampleDataFetcher.class);
    }

    @Test(groups = {EXTERNAL_INTEGRATION})
    public void test_patient_id_integration() {
        SampleId sampleId = new SampleId("SM-12CO4");
        BSPSample bspSample = new BSPSample(sampleId,
                fetcher.fetchSingleSampleFromBSP(sampleId.toString()));
        String patientId = bspSample.getPatientId();

        Assert.assertNotNull(patientId);
        Assert.assertEquals("PT-2LK3",patientId);

    }

    @Test(groups = {DATABASE_FREE})
    public void test_patient_id_mock() {
        List<String[]> resultColumns = new ArrayList<String[]>(1);
        resultColumns.add(new String[] {"1","2","3","4","5","6","7"});
        BSPSampleSearchService service = EasyMock.createMock(BSPSampleSearchService.class);
        SampleId sampleId = new SampleId("Sample1");
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
        BSPSample sample = new BSPSample(sampleId,new BSPSampleDataFetcher(service).fetchSingleSampleFromBSP(sampleId.toString()));
        Assert.assertEquals(resultColumns.iterator().next()[0],sample.getPatientId());
        Assert.assertEquals(new BigDecimal(resultColumns.iterator().next()[5]),sample.getVolume());
        Assert.assertEquals(new BigDecimal(resultColumns.iterator().next()[6]),sample.getConcentration());

    }

}
