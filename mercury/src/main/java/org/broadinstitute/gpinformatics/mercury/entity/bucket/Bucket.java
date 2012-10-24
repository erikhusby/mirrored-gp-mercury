package org.broadinstitute.gpinformatics.mercury.entity.bucket;

/**
 * Represents a bucket in the workflow diagram
 */
public class Bucket {

    // todo wire up to workflow definition

    /**
     * Does this bucket contain the given
     * {@link BucketEntry}?
     * @param bucketEntry
     * @return
     */
    public boolean contains(BucketEntry bucketEntry) {
        throw new RuntimeException("not implemeted yet");
    }
}
