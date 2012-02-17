package org.broadinstitute.sequel.entity.project;

import org.broadinstitute.sequel.entity.sample.SampleSheet;

/**
 * A {@link ProjectBranchable} is a branch
 * point in the transfer graph where all
 * downstream nodes and edges are assigned
 * to a particular {@link Project}.  It's basically
 * a way to encircle a subgraph and associate
 * all actions in a subgraph to a single {@link Project}.
 * 
 * Primarily this is aimed at serving {@link Project#isDevelopment() development projects},
 * but it could also be used for other kinds of re-use of {@link org.broadinstitute.sequel.entity.vessel.LabVessel}.
 * 
 * Take care when deciding whether something is {@link ProjectBranchable}
 * or {@link org.broadinstitute.sequel.entity.analysis.ReadBucketable}.  Sometimes users just
 * want to group their reads differently, which is
 * very different from associating lab activity
 * to particular {@link Project}s.
 */
public interface ProjectBranchable {
    
    public void branchFor(Project p,SampleSheet sampleSheet);
    
    public void branchAll(Project p);
}
