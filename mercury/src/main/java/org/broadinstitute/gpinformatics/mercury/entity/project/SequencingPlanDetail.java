package org.broadinstitute.gpinformatics.mercury.entity.project;

import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingTechnology;

/**
 * A project can have multiple aspects.
 * A simple project might just have one
 * aspect, like "sequence these samples
 * to 20x with Illumina 76 bp unpaired runs".
 *
 * More complex projects will come online
 * with things like fluidigm,
 * which will allow a single aliquot
 * to be turned into 454, ion, illumina,
 * and pacbio libraries.  In these sorts
 * of projects, we assume that we want
 * the work rolled up to a single project.
 *
 * So how do you have a single project
 * where a single sample in that project
 * might be queued for work across n sequencing
 * technologies?
 *
 * You make a plan with multiple plan details.
 */
public class SequencingPlanDetail {

    private SequencingTechnology sequencingTechnology;

    private CoverageGoal coverageGoal;

    private ProjectPlan projectPlan;
    
    public SequencingPlanDetail(SequencingTechnology sequencingTechnology,
                                CoverageGoal coverageGoal,
                                ProjectPlan projectPlan) {
        if (projectPlan == null) {
             throw new NullPointerException("projectPlan cannot be null.");
        }
         this.sequencingTechnology = sequencingTechnology;
        this.coverageGoal = coverageGoal;
        this.projectPlan = projectPlan;
        projectPlan.addSequencingDetail(this);
    }

    public ProjectPlan getProjectPlan() {
        return projectPlan;
    }

    /**
     * Illumina, Ion, Pacbio, Sanger, etc.  Each implementation
     * has its own run setup parameters.
     * @return
     */
    public SequencingTechnology getSequencingTechnology() {
        return sequencingTechnology;
    }

    /**
     * 20x?  90% @ 20x for HS?  20 gigabases? 2 lanes?
     * @return
     */
    public CoverageGoal getCoverageGoal() {
        return coverageGoal;
    }
}
