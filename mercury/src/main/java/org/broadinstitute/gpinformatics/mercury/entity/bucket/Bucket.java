package org.broadinstitute.gpinformatics.mercury.entity.bucket;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a bucket in the workflow diagram
 */
public class Bucket {

    // todo wire up to workflow definition

    private Long bucketId;

    private Set<BucketEntry> bucketEntries = new HashSet<BucketEntry>();

    private String bucketDefinition;



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
