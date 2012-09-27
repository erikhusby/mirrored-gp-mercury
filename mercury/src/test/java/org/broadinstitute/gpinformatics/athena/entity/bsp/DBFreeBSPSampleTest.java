package org.broadinstitute.gpinformatics.athena.entity.bsp;

/**
 * From Sequel
 * modified by mccrory
 */
public class DBFreeBSPSampleTest {



    //TODO PMB refactor BSPSampleDTO_PMB class out.
//    @Test(groups = {UNIT})
//    public void test_patient_id_mock() {
//        List<String[]> resultColumns = new ArrayList<String[]>(1);
//        resultColumns.add(new String[] {"1","2","3","4","5","6","7","8"});
//        BSPSampleSearchService service = EasyMock.createMock(BSPSampleSearchService.class);
//        SampleId sampleId = new SampleId("Sample1");
//        EasyMock.expect(service.runSampleSearch(
//                (Collection<String>) EasyMock.anyObject(),
//                (BSPSampleSearchColumn)EasyMock.anyObject(),
//                (BSPSampleSearchColumn)EasyMock.anyObject(),
//                (BSPSampleSearchColumn)EasyMock.anyObject(),
//                (BSPSampleSearchColumn)EasyMock.anyObject(),
//                (BSPSampleSearchColumn)EasyMock.anyObject(),
//                (BSPSampleSearchColumn)EasyMock.anyObject(),
//                (BSPSampleSearchColumn)EasyMock.anyObject(),
//                (BSPSampleSearchColumn)EasyMock.anyObject())
//        ).andReturn(resultColumns).atLeastOnce();
//
//        EasyMock.replay(service);
//        BSPSample sample = new BSPSample(sampleId,new BSPSampleDataFetcher(service).fetchSingleSampleFromBSP(sampleId.toString()));
//        Assert.assertEquals(resultColumns.iterator().next()[0], sample.getPatientId());
//        Assert.assertEquals(new BigDecimal(resultColumns.iterator().next()[5]),sample.getVolume());
//        Assert.assertEquals(new BigDecimal(resultColumns.iterator().next()[6]),sample.getConcentration());
//
//    }
}

