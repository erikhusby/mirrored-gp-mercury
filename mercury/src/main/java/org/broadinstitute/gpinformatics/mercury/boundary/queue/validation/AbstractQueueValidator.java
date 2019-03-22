package org.broadinstitute.gpinformatics.mercury.boundary.queue.validation;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.util.Collection;
import java.util.Map;

/**
 * Utilized for validation during Enqueue & Dequeue process.
 */
public interface AbstractQueueValidator {

    /**
     * Validates the lab vessel before the Enqueue occurs.  Example: Make sure only DNA can get into Pico.
     *
     * @param labVessels            Lab Vessels to validate
     * @param messageCollection     Messages back to the user
     * @return                      Map of LabVesselId to a validation result.
     */
    Map<Long, ValidationResult> validatePreEnqueue(Collection<LabVessel> labVessels, MessageCollection messageCollection);

    /**
     * Validates whether a single lab vessel has been c ompleted. This can be based on whatever makes the most sense for
     * the process.  Could be does it have concentration / volume?  Is there a bucket entry for the next step in the
     * process? etc.
     *
     * @param labVessel             Lab Vessel to check for completion
     * @param messageCollection     Messages back to the user.
     * @return                      True if it is completed, false otherwise
     */
    boolean isComplete(LabVessel labVessel, MessageCollection messageCollection);
}
