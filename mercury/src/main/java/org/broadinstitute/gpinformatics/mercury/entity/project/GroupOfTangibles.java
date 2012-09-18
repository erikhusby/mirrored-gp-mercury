package org.broadinstitute.gpinformatics.mercury.entity.project;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabTangible;

import java.util.Collection;

/**
 * Re-usable grouping of samples/tangibles.  Useful
 * if you want to do multiple things on the
 * same tangibles, like generate Ion sequence
 * as well as PacBio sequence.  Group them once, then refer
 * to the group.
 *
 * This grouping is defined by project managers.  It
 * doesn't correspond to the batch used to start
 * lab work.
 */
public interface GroupOfTangibles {

    public String getName();

    public Collection<LabTangible> getLabTangibles();

    // todo notion of a feeder, can be streamed or batched

}
