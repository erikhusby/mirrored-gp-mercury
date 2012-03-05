package org.broadinstitute.sequel.entity.project;

import org.broadinstitute.sequel.entity.bsp.BSPSample;
import org.broadinstitute.sequel.entity.run.IonSequencingTechnology;
import org.broadinstitute.sequel.entity.run.SequencingTechnology;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleSheetImpl;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.sequel.entity.workflow.LabWorkflow;

import static org.testng.Assert.*;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.HashSet;

public class ProjectTest {
    
    @Test(groups = {"DatabaseFree"})
    public void test_legacy_squid_project() {
        Project legacyProject = new BasicProject("Legacy Squid Project C203",null);
        ProjectPlan plan = new ProjectPlan(legacyProject,legacyProject.getProjectName() + " Plan");
        ReagentDesign bait = new ReagentDesign("agilent_foo", ReagentDesign.REAGENT_TYPE.BAIT);
        plan.addReagentDesign(bait);
        LabWorkflow workflow = new LabWorkflow("HybridSelection","9.6");

        SequencingPlanDetail ionPlan = new SequencingPlanDetail(workflow,
                new IonSequencingTechnology(65, IonSequencingTechnology.CHIP_TYPE.CHIP1),
                new XFoldCoverage(30));

        plan.addSequencingDetail(ionPlan);

        legacyProject.addProjectPlan(plan);

        SampleSheetImpl sampleSheet = new SampleSheetImpl();
        StartingSample startingSample = new BSPSample("BSPRoot123",legacyProject,null);
        sampleSheet.addStartingSample(startingSample);
        LabVessel starter = new TwoDBarcodedTube(startingSample.getContainerId(), sampleSheet);
        
        // todo: instead of a bogus TwoDBarcodedTube for the root, lookup BSP
        // container information inside a BSPVessel object, most of whose
        // methods throw exceptions that say "Hey, I'm from BSP, you can't do that!"
        plan.addStarter(starter);

        assertFalse(legacyProject.getAllStarters() == null);
        
        assertEquals(1,legacyProject.getAllStarters().size());

        for (LabVessel vessel : legacyProject.getAllStarters()) {
            assertEquals(starter,vessel);
        }
        
        assertFalse(plan.getStarters().isEmpty());
        assertEquals(1,plan.getStarters().size());

        for (LabVessel vessel : plan.getStarters()) {
            assertEquals(starter,vessel);
        }
        
        assertFalse(plan.getReagentDesigns() == null);
        assertEquals(1,plan.getReagentDesigns().size());
        
        ReagentDesign fetchedDesign = plan.getReagentDesigns().iterator().next();
        
        assertEquals(ReagentDesign.REAGENT_TYPE.BAIT,fetchedDesign.getReagentType());
        assertEquals("agilent_foo",fetchedDesign.getDesignName());

        Collection<SampleInstance> sampleInstances = new HashSet<SampleInstance>();

        for (LabVessel vessel : legacyProject.getAllStarters()) {
            sampleInstances.addAll(vessel.getSampleInstances());
        }
        
        assertEquals(1,sampleInstances.size());

        for (SampleInstance sampleInstance : sampleInstances) {
            assertEquals(startingSample,sampleInstance.getStartingSample());
            assertEquals(legacyProject,sampleInstance.getProject());
        }
        
        assertEquals(plan.getName(),legacyProject.getProjectName() + " Plan",plan.getName());
        
        assertEquals(1,plan.getPlanDetails().size());
        
        SequencingPlanDetail planDetail = plan.getPlanDetails().iterator().next();
        assertEquals(ionPlan,planDetail);
        
        assertEquals(SequencingTechnology.TECHNOLOGY_NAME.ION_TORRENT,ionPlan.getSequencingTechnology().getTechnologyName());
        assertEquals("HybridSelection",ionPlan.getWorkflow().getName());
        assertEquals(65,((IonSequencingTechnology)ionPlan.getSequencingTechnology()).getCycleCount());
        assertEquals(IonSequencingTechnology.CHIP_TYPE.CHIP1,((IonSequencingTechnology)ionPlan.getSequencingTechnology()).getChipType());
        
        assertEquals(30,((XFoldCoverage)ionPlan.getCoverageGoal()).getCoverageDepth());

    }
}
