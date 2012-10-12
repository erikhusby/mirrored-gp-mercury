package org.broadinstitute.gpinformatics.mercury.entity.project;


import org.broadinstitute.gpinformatics.mercury.entity.sample.StartingSample;

import java.util.Collection;

/**
 * For many kinds of scientific projects, a single sample is not sufficient
 * for analysis.  For cancer projects, we often need both a tumor and normal
 * sample for the same patient so that we can see what's different in the
 * tumor vs the normal.  For autism studies, we might want to analyze the
 * sample from the kid with autism in conjunction with his unaffected sister,
 * mother, and father.  Some population studies have case/control sample
 * pairings, where analysis of individual with schizophrenia requires
 * analysis with a similar (in terms of gender, age, height, favorite
 * beer, etc.) sample.
 *
 * The analysis "buddies" are the generalization of the above use cases.
 * Analysis buddies exist within a single project.
 */
public class SampleAnalysisBuddies {

    public String getBuddyGroupName() {
        throw new RuntimeException("not implemented");
    }
    
    public Collection<StartingSample> getSamples() {
        throw new RuntimeException("not implemented");
    }
    
//    public Project getProject() {
//        throw new RuntimeException("not implemented");
//    }
    
}
