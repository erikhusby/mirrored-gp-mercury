package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;

import java.util.Collection;

/**
 * When someone puts a {@link LabVessel} into a
 * {@link LabBatch}, there is an opportunity to set
 * an "override" for the {@link org.broadinstitute.gpinformatics.mercury.entity.project.BasicProjectPlan}, so that
 * a user can say "Do this {@link org.broadinstitute.gpinformatics.mercury.entity.project.WorkflowDescription workflow},
 * but do it on behalf of a different {@link org.broadinstitute.gpinformatics.mercury.entity.project.BasicProjectPlan}.
 */
public class LabEventProjectPlanOverrider {

    private LabBatchDAO labBatchDAO;

    public LabEventProjectPlanOverrider() {}

    public LabEventProjectPlanOverrider(LabBatchDAO labBatchDAO) {
        this.labBatchDAO = labBatchDAO;
    }

    // test arz: rack of tubes -> plate, verify plate.getSampleInstances() has overridden projectPlan
    // test arz: plate section -> plate section, verify dest. plate's sampleInstance have override project plan
    // test arz: cherry pick: change project plan on source, verify destination's sampleInstances reflect new project plan
    // test arz: in-place no-transfer event on rack and plate: check destination sample instances

    // test arz: xfer from a -> b, no override, then a -> c with override.  verify b's sample instances have
    // original projectplan, but c's have the overriden one.
    // test arz: a->b, a->c as above, then do c->d AND b->d, expect exception for d.sampleinstance.getSingleProjectPlan().  expect
    // to see both overrides in d.sampleinstance.getSingleProjectPlan()


    public void setProjectPlanOverrides(LabEvent labEvent,Collection<? extends LabVessel> vessels,Collection<LabBatch> possibleBatches) {
        if (possibleBatches.size() > 1) {
            // todo make this an alert message back to the jira ticket?
            throw new RuntimeException("Cannot resolve which of " + possibleBatches.size() + " batches to use");
        }
        else if (possibleBatches.size() == 1) {
            LabBatch batch = possibleBatches.iterator().next();
            batch.getJiraTicket().addComment(labEvent.getEventOperator().getLogin() + "is processing " + labEvent.getEventName().name() + " at " + labEvent.getEventLocation());
            for (LabVessel vessel : vessels) {
//                ProjectPlan projectPlanOverride = batch.getProjectPlanOverride(vessel);
//                if (projectPlanOverride != null) {
//                    throw new RuntimeException("I haven't been written yet");
//                    //labEvent.setProjectPlanOverride(vessel,projectPlanOverride);
//                }
            }
        }
        // else no batches
    }

    public void setProjectPlanOverrides(LabEvent labEvent,RackOfTubes rackOfTubes) {
        Collection<TwoDBarcodedTube> sourceTubes = rackOfTubes.getVesselContainer().getContainedVessels();
        setProjectPlanOverrides(labEvent,sourceTubes,labBatchDAO.guessActiveBatchesForVessels(sourceTubes));
    }

    /**
     * Figures out what (if any) {@link org.broadinstitute.gpinformatics.mercury.entity.project.BasicProjectPlan} overrides
     * should be and sets them, per {@link LabVessel} in the
     * {@link LabEvent}
     * @param labEvent
     */
    public void setProjectPlanOverrides(LabEvent labEvent) {
        Collection<LabVessel> vesselsForBatch = null;
        boolean isRackOfTubes = false;
        if (!labEvent.getSourceLabVessels().isEmpty()) {
            setProjectPlanOverrides(labEvent,labEvent.getSourceLabVessels());
        }
        else {
            setProjectPlanOverrides(labEvent,labEvent.getTargetLabVessels());
        }
    }

    public void setProjectPlanOverrides(LabEvent labEvent,Collection<LabVessel> vessels) {
        boolean isRackOfTubes = false;
        for (LabVessel source : vessels) {
            // special case: the batch may have been defined as a set of
            // tubes
            if (source instanceof RackOfTubes) {
                setProjectPlanOverrides(labEvent,(RackOfTubes)source);
                isRackOfTubes = true;
            }
        }
        if (!isRackOfTubes) {
            setProjectPlanOverrides(labEvent,vessels,labBatchDAO.guessActiveBatchesForVessels(vessels));
        }
        // else we've already taken care of this because this is a rack of tubes
    }
}
