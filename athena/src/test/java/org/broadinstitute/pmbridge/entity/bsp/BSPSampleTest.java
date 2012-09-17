package org.broadinstitute.pmbridge.entity.bsp;

import org.broadinstitute.pmbridge.JavaBeanTester;
import org.broadinstitute.pmbridge.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.pmbridge.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.pmbridge.infrastructure.bsp.MockBSPSampleSearchService;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.beans.IntrospectionException;

import static org.broadinstitute.pmbridge.TestGroups.UNIT;

public class BSPSampleTest {

    public static final String SM_12_CO4 = "SM-12CO4";
    public static final String PT_2_LK3 = "PT-2LK3";

    @Test(groups = {UNIT})
    public void test_patient_id_integration() {
        BSPSampleDataFetcher fetcher = new BSPSampleDataFetcher(new MockBSPSampleSearchService());
        String sampleName = SM_12_CO4;
        final BSPSampleDTO bspDTO = fetcher.fetchSingleSampleFromBSP(sampleName);
        BSPSample bspSample = new BSPSample( new SampleId(sampleName), bspDTO);
        Assert.assertEquals(PT_2_LK3, bspSample.getPatientId());

        // other ctr
        BSPSample bspSample2 = new BSPSample( new SampleId(sampleName) );
        Assert.assertFalse( bspSample2.hasBSPDTOBeenInitialized());
        bspSample2.setBspDTO(bspDTO);
        Assert.assertTrue( bspSample2.hasBSPDTOBeenInitialized());
        Assert.assertEquals(PT_2_LK3, bspSample2.getPatientId());
        Assert.assertEquals(SM_12_CO4,bspSample2.getId().getValue());
//        Assert.assertEquals(bspDTO.getContainerId(),bspSample2.getContainerId());
        Assert.assertEquals(bspDTO.getOrganism(),bspSample2.getOrganism());

        Assert.assertEquals( bspSample, bspSample2);

    }



    @Test(groups = {UNIT})
    public void testClass() throws IntrospectionException {

        JavaBeanTester.test(BSPSample.class, "bspDTO");


    }
}
