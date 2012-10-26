package org.broadinstitute.gpinformatics.mercury.entity.bucket;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a bucket in the workflow diagram
 */
@Entity
@Audited
@Table (schema = "mercury", name = "bucket",
        uniqueConstraints = @UniqueConstraint (columnNames = {"bucket_definition_name"}))
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

    protected Bucket () {
    }

    public Bucket ( @NotNull String bucketDefinitionIn ) {
        this.bucketDefinitionName = bucketDefinitionIn;
    }

    public String getBucketDefinitionName ( ) {
        return bucketDefinitionName;
    }

    public Collection<BucketEntry> getBucketEntries () {

        List setList = new ArrayList(bucketEntries);
        Collections.sort(setList, BucketEntry.byDate);
        return Collections.unmodifiableList ( setList );
    }

    /**
     * Does this bucket contain the given {@link BucketEntry}?
     * @param bucketEntry
     * @return
     */
    public boolean contains(BucketEntry bucketEntry) {
        return bucketEntries.contains(bucketEntry);
    }

    /**
     * adds a new {@link BucketEntry} to the bucket
     *
     * @param newEntry
     */
    public void addEntry( BucketEntry newEntry ) {
        bucketEntries.add(newEntry);

    }


    /**
     * Helper method to add a new item into the bucket
     * @param productOrderKey Business key of a Product order to associate with the new entry
     * @param vessel Lab Vessel to enter into the bucket.
     * @return an instance of a Bucket entry which represents the lab vessel and the product order for that entry
     */
    public BucketEntry addEntry ( String productOrderKey, LabVessel vessel ) {
        BucketEntry newEntry = new BucketEntry(vessel,productOrderKey, null );
        newEntry.setBucketExistence(this);
        bucketEntries.add(newEntry);

        return newEntry;
    }

    /**
     * Removes an item from bucket
     * @param entryToRemove instance of a bucket entry to remove which represents the lab vessel and the product order
     *                      for that entry
     */
    public void removeEntry ( BucketEntry entryToRemove) {
        bucketEntries.remove(entryToRemove);
    }

}
