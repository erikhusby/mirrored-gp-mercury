package org.broadinstitute.sequel;

import java.util.Collection;

public interface ProjectPlan {

    public Collection<ProjectPlanDetail> getPlanDetails();

    /**
     * Something about the "why" of this
     * project.  What's the data going to
     * be used for?
     * @return
     */
    public String getTextOverview();

    /**
     * Basically how much sequencing are we going
     * to do for this project?  We assume that the
     * sequencing goal is uniform for every sample
     * in the project.
     * @return
     */

    // todo way to abstract categories for weekly prioritization/capacity
    // planning from critter.

    // organism, collaborator, initiative/funding source/quote, prep
    // type, sequencing type, outbreak,

}
