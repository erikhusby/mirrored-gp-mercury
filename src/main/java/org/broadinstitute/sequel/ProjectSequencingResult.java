package org.broadinstitute.sequel;


import java.util.Collection;

public class ProjectSequencingResult implements SequencingResult {

    /**
     * Make a new sequencing result that
     * only pertains to this project.
     * @param project
     */
    public ProjectSequencingResult(Project project) {

    }

    /**
     * Gives you run information that is only relevant
     * to the project.
     * @return
     */
    public Collection<SequencingRun> getSequencingRuns() {
        throw new RuntimeException("I haven't been written yet.");
    }
}
