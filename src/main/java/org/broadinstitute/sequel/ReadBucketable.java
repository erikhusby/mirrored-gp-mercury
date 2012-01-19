package org.broadinstitute.sequel;


import java.util.Collection;

/**
 * Something is {@link ReadBucket} if
 * you can apply new {@link ReadBucket}s
 * to the thing.  For example, maybe
 * all the reads derived from a
 * particular {@link LabVessel} should
 * be aggregated into a single BAM file.
 * That would make {@link LabVessel} {@link ReadBucket}.
 * 
 * Or maybe there's some dev work someone is
 * doing to a disparate group of {@link IlluminaFlowcell}
 * flowcells, and they want the reads from
 * these flowcells grouped together.  In this
 * case, the {@link IlluminaFlowcell} becomes
 * {@link ReadBucket}.
 * 
 * Notice a distinction from {@link ProjectBranchable}.
 * A PM {@link Project branches the project} when they
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

    public void branchFor(ReadBucket bucket, SampleSheet sampleSheet);

    public Collection<ReadBucket> getReadBuckets();

}
