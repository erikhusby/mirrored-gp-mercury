package org.broadinstitute.sequel.test.entity.project;

import org.broadinstitute.sequel.boundary.*;
import org.broadinstitute.sequel.bsp.EverythingYouAskForYouGetAndItsHuman;
import org.broadinstitute.sequel.entity.bsp.BSPSample;
import org.broadinstitute.sequel.entity.project.BasicProjectPlan;
import org.broadinstitute.sequel.entity.project.PassBackedProjectPlan;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.project.Starter;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.vessel.BSPSampleAuthorityTwoDTube;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.sequel.infrastructure.quote.MockQuoteService;
import org.testng.annotations.Test;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;

import static org.broadinstitute.sequel.TestGroups.DATABASE_FREE;

import static org.testng.Assert.*;

public class ProjectPlanFromPassTest {

    private AbstractPass setupProjectAndPass(String...sampleNames) {
        DirectedPass hsPass = new DirectedPass();
        ProjectInformation projectInfo = new ProjectInformation();
        projectInfo.setPassNumber("PASS-123");
        hsPass.setProjectInformation(projectInfo);
        hsPass.setResearchProject("RP-123");
        SampleList sampleList = new SampleList();

        for (String sampleName: sampleNames) {
            Sample sample = new Sample();
            sample.setBspSampleID(sampleName);
            sampleList.getSample().add(sample);
        }
        hsPass.setSampleDetailsInformation(sampleList);
        hsPass.setBaitSetID(new Long(123));
        CoverageAndAnalysisInformation coverageAndAnalysisInformation = new CoverageAndAnalysisInformation();
        TargetCoverageModel targetCoverageModel = new TargetCoverageModel();
        targetCoverageModel.setCoveragePercentage(new BigInteger("80"));
        targetCoverageModel.setDepth(new BigInteger("20"));
        coverageAndAnalysisInformation.setTargetCoverageModel(targetCoverageModel);
        hsPass.setCoverageAndAnalysisInformation(coverageAndAnalysisInformation);
        return hsPass;

    }

    @Test(groups = {DATABASE_FREE})
    public void test_create_project_plan_from_pass() {
        AbstractPass pass = setupProjectAndPass("SM-123","SM-456");
        BSPSampleDataFetcher bspDataFetcher = new BSPSampleDataFetcher(new EverythingYouAskForYouGetAndItsHuman());
        ProjectPlan projectPlan = new PassBackedProjectPlan(pass,bspDataFetcher,new MockQuoteService());

        assertEquals(projectPlan.getStarters().size(),pass.getSampleDetailsInformation().getSample().size());
        assertEquals(projectPlan.getProject().getProjectName(),pass.getResearchProject());

        assertFalse(projectPlan.getStarters().isEmpty());

        final Collection<String> passSampleNames = new HashSet<String>();

        for (Sample sample : pass.getSampleDetailsInformation().getSample()) {
            passSampleNames.add(sample.getBspSampleID());
        }

        for (Sample passSample : pass.getSampleDetailsInformation().getSample()) {
            String aliquotName = passSample.getBspSampleID() + ".aliquot";
            BSPSampleAuthorityTwoDTube aliquot = new BSPSampleAuthorityTwoDTube(passSample,
                    new BSPSample(aliquotName,projectPlan,bspDataFetcher.fetchSingleSampleFromBSP(aliquotName)));

            assertEquals(aliquot.getSampleInstances().size(),1);
            assertTrue(passSampleNames.contains(aliquot.getPassSample().getBspSampleID()));
            assertEquals(aliquot.getSampleInstances().iterator().next().getStartingSample().getSampleName(),aliquot.getPassSample().getBspSampleID() + ".aliquot");
        }

        for (Starter starter : projectPlan.getStarters()) {
            assertEquals(starter.getSampleInstances().size(),1);
            SampleInstance sampleInstance = starter.getSampleInstances().iterator().next();
            assertTrue(passSampleNames.contains(sampleInstance.getStartingSample().getSampleName()));
        }
    }
}
