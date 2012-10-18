package org.broadinstitute.gpinformatics.mercury.entity.bucket;

import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a bucket in the workflow diagram
 */
@Entity
@Audited
@Table (schema = "mercury", uniqueConstraints = @UniqueConstraint (columnNames = {"bucketDefinition"}))
public class Bucket {

    // todo wire up to workflow definition

    @SequenceGenerator (name = "SEQ_BUCKET", schema = "mercury",  sequenceName = "SEQ_BUCKET")
    @GeneratedValue (strategy = GenerationType.SEQUENCE, generator = "SEQ_BUCKET")
    @Id
    private Long bucketId;

    @OneToMany (mappedBy = "bucketExistence")
    private Set<BucketEntry> bucketEntries = new HashSet<BucketEntry>();

    @Column ( name = "bucket_definition_name")
    private String bucketDefinitionName;

    public Bucket ( @NotNull String bucketDefinitionIn ) {
        this.bucketDefinitionName = bucketDefinitionIn;
    }

    public String getBucketDefinitionName ( ) {
        return bucketDefinitionName;
    }

    public Set<BucketEntry> getBucketEntries ( ) {
        return Collections.unmodifiableSet( bucketEntries );
    }

    /**
     * Does this bucket contain the given
     * {@link BucketEntry}?
     * @param bucketEntry
     * @return
     */
    public boolean contains(BucketEntry bucketEntry) {

        return bucketEntries.contains(bucketEntry);

    }

    /**
     *
     * @param newEntry
     */
    public void addEntry( BucketEntry newEntry ) {
        bucketEntries.add(newEntry);
    }

    public void removeEntry ( BucketEntry entryToRemove) {
        bucketEntries.remove(entryToRemove);
    }

}
