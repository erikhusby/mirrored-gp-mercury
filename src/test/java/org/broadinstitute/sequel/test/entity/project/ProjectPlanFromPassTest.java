package org.broadinstitute.sequel.test.entity.project;

import org.broadinstitute.sequel.boundary.*;
import org.broadinstitute.sequel.bsp.EverythingYouAskForYouGetAndItsHuman;
import org.broadinstitute.sequel.entity.bsp.BSPStartingSample;
import org.broadinstitute.sequel.entity.project.*;
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

    private final long BAIT_ID = 5;

    private final String BAIT_DESIGN_NAME = "interesting genes";

    private AbstractPass setupProjectAndPass(String...sampleNames) {
        DirectedPass hsPass = new DirectedPass();
        hsPass.setBaitSetID(BAIT_ID);
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
        BaitSetListResult baitsCache = new BaitSetListResult();
        BaitSet baitSet = new BaitSet();
        baitSet.setDesignName(BAIT_DESIGN_NAME);
        baitSet.setId(BAIT_ID);
        baitsCache.getBaitSetList().add(baitSet);

        ProjectPlan projectPlan = new PassBackedProjectPlan(pass,bspDataFetcher,new MockQuoteService(),baitsCache);

        assertEquals(projectPlan.getStarters().size(),pass.getSampleDetailsInformation().getSample().size());
        assertEquals(projectPlan.getProject().getProjectName(),pass.getResearchProject());

        assertFalse(projectPlan.getStarters().isEmpty());

        final Collection<String> passSampleNames = new HashSet<String>();

        for (Sample sample : pass.getSampleDetailsInformation().getSample()) {
            passSampleNames.add(sample.getBspSampleID());
        }


        for (Starter starter : projectPlan.getStarters()) {
            assertNull(projectPlan.getAliquot(starter));
            assertEquals(starter.getSampleInstances().size(),1);
            SampleInstance sampleInstance = starter.getSampleInstances().iterator().next();
            assertTrue(passSampleNames.contains(sampleInstance.getStartingSample().getSampleName()));

            String aliquotName = starter.getLabel() + ".aliquot";
            BSPSampleAuthorityTwoDTube aliquot = new BSPSampleAuthorityTwoDTube(new BSPStartingSample(aliquotName,projectPlan,bspDataFetcher.fetchSingleSampleFromBSP(aliquotName)));

            // plating export will call this
            projectPlan.setAliquot(starter,aliquot);
            assertEquals(aliquot,projectPlan.getAliquot(starter));

            assertEquals(aliquot.getSampleInstances().size(),1);
            assertTrue(passSampleNames.contains(starter.getLabel()));
            assertEquals(aliquot.getSampleInstances().iterator().next().getStartingSample().getSampleName(), starter.getLabel() + ".aliquot");
        }

        assertFalse(projectPlan.getReagentDesigns().isEmpty());
        assertEquals(projectPlan.getReagentDesigns().size(),1);

        ReagentDesign baitDesign = projectPlan.getReagentDesigns().iterator().next();
        assertEquals(baitDesign.getDesignName(), BAIT_DESIGN_NAME);
        assertEquals(baitDesign.getReagentType(), ReagentDesign.REAGENT_TYPE.BAIT);

        // todo add xfers from aliquots

    }
}
