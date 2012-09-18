package org.broadinstitute.gpinformatics.mercury.entity.analysis;

/**
 * A bucket into which users like project
 * managers, Bass users, bioinformatics types,
 * and sequence analyzers look to find their
 * read data.  <b>A {@link ReadBucket} is not a {@link org.broadinstitute.gpinformatics.mercury.entity.project.Project}</b>.
 *
 * Ok, sometimes a read bucket might be a project.
 * But sometimes the read bucket is a different
 * grouping concept.
 * 
 * Assembly analysts want to group "assemble-able"
 * things in the same bucket (by things like
 * organism, strain, species, etc.)
 * 
 * The tech dev team needs to put reads
 * for the same experimental conditions
 * together in the same bucket.
 * 
 * There might be IRB rules or
 * submission constraints that
 * require us to put reads into different
 * buckets.
 * 
 * Historically a {@link org.broadinstitute.gpinformatics.mercury.entity.project.Project} has been
 * the de-facto {@link ReadBucket}, but
 * this is the source of many
 * problems.
 *
 * For meta studies where users want to take
 * reads from across projects in some
 * zany way 5 years after we've sequenced things,
 * we'd make a ReadBucket for that.
 */
public interface ReadBucket {

    /**
     * What's the name of the bucket?  This
     * probably corresponds to a bass "project".
     * @return
     */
    public String getBucketName();
}
