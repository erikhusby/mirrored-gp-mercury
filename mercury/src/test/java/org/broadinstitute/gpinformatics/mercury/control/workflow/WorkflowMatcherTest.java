package org.broadinstitute.gpinformatics.mercury.control.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToVesselTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.GenericReagent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by thompson on 4/23/2015.
 */
public class WorkflowMatcherTest {
    @Test
    public void testExtraction() {
        // Create samples
        BarcodedTube t1 = new BarcodedTube("T1", BarcodedTube.BarcodedTubeType.MatrixTube);
        BarcodedTube t2 = new BarcodedTube("T2", BarcodedTube.BarcodedTubeType.MatrixTube);

        // Create new batch with workflow
        Set<LabVessel> starterVessels = new HashSet<>();
        starterVessels.add(t1);
        starterVessels.add(t2);
        LabBatch labBatch = new LabBatch("ESET-1", starterVessels, LabBatch.LabBatchType.WORKFLOW);

        // Add batch event
        LabEvent reagentsPrepared = new LabEvent(LabEventType.COVARIS_LOADED, new Date(), "ZAN", 1L, 101L, "UI");
        reagentsPrepared.setLabBatch(labBatch);

        // Add transfer with reagents
        LabEvent bloodToMicro = new LabEvent(LabEventType.EXTRACT_BLOOD_TO_MICRO, new Date(), "ZAN", 1L, 101L, "UI");
        BarcodedTube m1 = new BarcodedTube("M1", BarcodedTube.BarcodedTubeType.EppendoffFliptop15);
        bloodToMicro.getVesselToVesselTransfers().add(new VesselToVesselTransfer(t1, m1, bloodToMicro));
        bloodToMicro.addReagent(new GenericReagent("R1", "1234", null));

        // Add vessel event
        LabEvent addEthanol = new LabEvent(LabEventType.A_BASE, new Date(), "ZAN", 1L, 101L, "UI");
        addEthanol.setInPlaceLabVessel(m1);

        // Add transfer that doesn't match
        // Verify list of planned steps, with actual events

        // How to get uniqueness across multiple LOADED events?
        // There are 3 of Spin at 6,000 x g for 1 minute.

        // Need to render per-sample transfer links for steps that haven't happened yet?  Not per-sample, because
        // they must be scanned.  Could just render link unconditionally.

        // Need event types for each reagent addition, or use a generic event and encode expected reagents in workflow?
        // Spin event type, with speed and duration, or generic loaded event, with name / value pairs

        // workflow additions: descriptive text
        // LabEventType additions: batch event vs vessel event vs transfer
        // More parameters to manual transfer page: event type; source vessel(s)? - no, they must be scanned

        // Matcher takes a batch or (single sample) vessel, returns a list of [workflow step, event]

        // If there are multiple events for a step, should the step be repeated, or should the events be normalized?
        // It's possible (likely?) that the dates overlap, unless the focus is on a single sample, this argues for
        // repeating step information.

        // Still not sure how to represent batch events.
        // There are already two LabBatch references on LabEvent: BSP batches, manual overrides.

        // Which is prime, planned steps or actual events?
        // At start, there are no events
        // Later, there may be events with no workflow
        // Chronological list of pairs, where either side may be null?
        // Some steps will have multiple events, one for each vessel in the batch
        // Some steps will be associated with the batch, not with vessels
    }
}
