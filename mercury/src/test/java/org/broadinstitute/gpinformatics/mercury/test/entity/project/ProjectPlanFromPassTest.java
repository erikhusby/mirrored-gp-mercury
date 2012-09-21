package org.broadinstitute.gpinformatics.mercury.test.entity.project;

import org.broadinstitute.gpinformatics.mercury.boundary.*;
import org.broadinstitute.gpinformatics.mercury.bsp.EverythingYouAskForYouGetAndItsHuman;
import org.broadinstitute.gpinformatics.mercury.control.pass.PassBatchUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPStartingSample;
import org.broadinstitute.gpinformatics.mercury.entity.project.PassBackedProjectPlan;
import org.broadinstitute.gpinformatics.mercury.entity.project.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.project.Starter;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BSPSampleAuthorityTwoDTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.testng.annotations.Test;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
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
        FundingInformation fundingInfo = new FundingInformation();
/* R3_725
        PriceItem priceItem = new PriceItem();
        fundingInfo.setGspPriceItem(priceItem);
        priceItem.setCategoryName("Nacho");
        priceItem.setName("Cheese");
*/
        hsPass.setFundingInformation(fundingInfo);
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

        PassBackedProjectPlan projectPlan = new PassBackedProjectPlan(pass,bspDataFetcher,baitsCache,null);

        assertEquals(projectPlan.getStarters().size(),pass.getSampleDetailsInformation().getSample().size());
        assertEquals(projectPlan.getProject().getProjectName(),pass.getResearchProject());

        assertFalse(projectPlan.getStarters().isEmpty());

        final Collection<String> passSampleNames = new HashSet<String>();

        for (Sample sample : pass.getSampleDetailsInformation().getSample()) {
            passSampleNames.add(sample.getBspSampleID());
        }


        for (Starter starter : projectPlan.getStarters()) {
            assertNull(projectPlan.getAliquotForStarter(starter));
            assertEquals(starter.getSampleInstances().size(),1);
            SampleInstance sampleInstance = starter.getSampleInstances().iterator().next();
            assertTrue(passSampleNames.contains(sampleInstance.getStartingSample().getSampleName()));

            String aliquotName = starter.getLabel() + ".aliquot";
            BSPSampleAuthorityTwoDTube aliquot = new BSPSampleAuthorityTwoDTube(new BSPStartingSample(aliquotName,projectPlan,bspDataFetcher.fetchSingleSampleFromBSP(aliquotName)));

            // plating export will call this
            projectPlan.addAliquotForStarter(starter,aliquot);
            assertEquals(aliquot,projectPlan.getAliquotForStarter(starter));

            assertEquals(aliquot.getSampleInstances().size(),1);
            assertTrue(passSampleNames.contains(starter.getLabel()));
            assertEquals(aliquot.getSampleInstances().iterator().next().getStartingSample().getSampleName(), starter.getLabel() + ".aliquot");

        }

        for (Sample sample : pass.getSampleDetailsInformation().getSample()) {
            boolean foundIt = false;
            for (Starter starter : projectPlan.getStarters()) {
                if (sample.getBspSampleID().equals(starter.getLabel())) {
                    foundIt = true;
                    LabVessel aliquot = projectPlan.getAliquotForStarter(starter);
                    assertNotNull(aliquot);
                }
            }
            assertTrue(foundIt);
        }

        assertFalse(projectPlan.getReagentDesigns().isEmpty());
        assertEquals(projectPlan.getReagentDesigns().size(),1);

        ReagentDesign baitDesign = projectPlan.getReagentDesigns().iterator().next();
        assertEquals(baitDesign.getDesignName(), BAIT_DESIGN_NAME);
        assertEquals(baitDesign.getReagentType(), ReagentDesign.REAGENT_TYPE.BAIT);

        Set<LabBatch> batches = PassBatchUtil.createBatches(projectPlan, 1, "TestBatch");
        assertNotNull(batches);
        assertEquals(batches.size(),2);

        for (Starter starter : projectPlan.getStarters()) {
            boolean isStarterInBatch = false;
            for (LabBatch batch : batches) {
                assertEquals(batch.getStarters().size(),1);
                if (batch.getStarters().contains(starter)) {
                    isStarterInBatch = true;
                }
            }
            assertTrue(isStarterInBatch,"Starter " + starter + " did not make it into a batch");
        }


        // todo add xfers from aliquots

    }
}
