package org.broadinstitute.pmbridge.entity.bsp;

import org.broadinstitute.pmbridge.DeploymentBuilder;
import org.broadinstitute.pmbridge.infrastructure.bsp.BSPSampleDataFetcher;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;

import static org.broadinstitute.pmbridge.TestGroups.EXTERNAL_INTEGRATION;

public class BSPSampleTest extends Arquillian {

    @Inject
    BSPSampleDataFetcher fetcher;

    @Deployment
    public static WebArchive buildBridgeWar() {
//        WebArchive war = DeploymentBuilder.createWebArchive()
//                .addPackage(BSPSampleTest.class.getPackage())
//                .addPackage(BSPSampleDataFetcher.class.getPackage())
//                ;
//        war = DeploymentBuilder.addWarDependencies(war);
        WebArchive war = DeploymentBuilder.buildBridgeWar();

        return war;
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



}
