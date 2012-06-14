package org.broadinstitute.sequel.entity.analysis;


import java.util.Collection;

/**
 * Something is {@link ReadBucket} if
 * you can apply new {@link ReadBucket}s
 * to the thing.  For example, maybe
 * all the reads derived from a
 * particular {@link org.broadinstitute.sequel.entity.vessel.LabVessel} should
 * be aggregated into a single BAM file.
 * That would make {@link org.broadinstitute.sequel.entity.vessel.LabVessel} {@link ReadBucket}.
 * 
 * Or maybe there's some dev work someone is
 * doing to a disparate group of {@link org.broadinstitute.sequel.entity.run.IlluminaFlowcell}
 * flowcells, and they want the reads from
 * these flowcells grouped together.  In this
 * case, the {@link org.broadinstitute.sequel.entity.run.IlluminaFlowcell} becomes
 * {@link ReadBucket}.
 * 
 * Notice a distinction from {@link org.broadinstitute.sequel.entity.project.ProjectBranchable}.
 * A PM {@link org.broadinstitute.sequel.entity.project.Project branches the project} when they
 * want to group the labwork for operational reasons,
 * capacity planning, or billing.  One {@link ReadBucket
 * branches the {@link ReadBucket}s} when one just
 * wants to group the reads together for analysis
 * so that the ultimate user of the reads has a simple,
 * single key into bass.
 *
 */
public interface ReadBucketable {

    public void branchAll(ReadBucket bucket);

    public Collection<ReadBucket> getReadBuckets();

}
