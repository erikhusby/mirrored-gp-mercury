package org.broadinstitute.gpinformatics.mercury.entity.bucket;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;
import org.hibernate.annotations.Where;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Represents a bucket in the workflow diagram
 */
@Entity
@Audited
@Table(schema = "mercury", name = "bucket",
        uniqueConstraints = @UniqueConstraint(columnNames = {"bucketDefinitionName"}))
public class Bucket {

    // todo wire up to workflow definition

    @SequenceGenerator(name = "SEQ_BUCKET", schema = "mercury", sequenceName = "SEQ_BUCKET")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BUCKET")
    @Id
    private Long bucketId;

    @OneToMany(mappedBy = "bucket", cascade = CascadeType.PERSIST)
    @Where(clause = "status='Active' and entryType='PDO_ENTRY'")
    private Set<BucketEntry> bucketEntries = new HashSet<>();

    @OneToMany(mappedBy = "bucket", cascade = CascadeType.PERSIST)
    @Where(clause = "status='Active' and entryType='REWORK_ENTRY'")
    private Set<BucketEntry> reworkEntries = new HashSet<>();

    @Column()
    private String bucketDefinitionName;

    protected Bucket() {
    }

    public Bucket(@Nonnull String bucketDefinitionIn) {
        this.bucketDefinitionName = bucketDefinitionIn;
    }

    public Bucket(@Nonnull WorkflowStepDef bucketDef) {
        this(bucketDef.getName());
    }

    public String getBucketDefinitionName() {
        return bucketDefinitionName;
    }

    public Collection<BucketEntry> getBucketEntries() {

        List<BucketEntry> setList = new ArrayList<>(bucketEntries);
        Collections.sort(setList, BucketEntry.byDate);
        return Collections.unmodifiableList(setList);
    }

    public Collection<BucketEntry> getReworkEntries() {
        List<BucketEntry> setList = new ArrayList<>(reworkEntries);
        Collections.sort(setList, BucketEntry.byDate);
        return Collections.unmodifiableList(setList);
    }

    /**
     * Does this bucket contain the given {@link BucketEntry}?
     *
     * @param bucketEntry
     *
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
    public void addEntry(BucketEntry newEntry) {
        newEntry.setBucket(this);
        newEntry.setProductOrderRanking(getBucketEntries().size() + 1);
        newEntry.getLabVessel().addBucketEntry(newEntry);
        bucketEntries.add(newEntry);

    }


    /**
     * Helper method to add a new item into the bucket
     *
     *
     * @param productOrderKey Business key of a Product order to associate with the new entry
     * @param vessel          Lab Vessel to enter into the bucket.
     *
     * @param entryType
     * @return an instance of a Bucket entry which represents the lab vessel and the product order for that entry
     */
    public BucketEntry addEntry(String productOrderKey, LabVessel vessel, BucketEntry.BucketEntryType entryType) {
        BucketEntry newEntry = new BucketEntry(vessel, productOrderKey, this, entryType);
        addEntry(newEntry);
        return newEntry;
    }

    /**
     * Removes an item from bucket
     *
     * @param entryToRemove instance of a bucket entry to remove which represents the lab vessel and the product order
     *                      for that entry
     */
    public void removeEntry(BucketEntry entryToRemove) {
        bucketEntries.remove(entryToRemove);
        entryToRemove.setStatus(BucketEntry.Status.Archived);
    }

    public Long getBucketId() {
        return bucketId;
    }

    public BucketEntry findEntry(@Nonnull LabVessel entryVessel) {

        List<BucketEntry> foundEntries = new LinkedList<BucketEntry>();
        BucketEntry foundBucketItem = null;

        for (BucketEntry currEntry : bucketEntries) {
            if (currEntry.getLabVessel().equals(entryVessel)) {
                foundEntries.add(currEntry);
            }
        }

        if (foundEntries.size() > 1) {
            throw new IllegalStateException(
                    "There is more than one entry in the bucket for the given vessel " + entryVessel.getLabel());
        }
        if (!foundEntries.isEmpty()) {
            foundBucketItem = foundEntries.get(0);
        }
        return foundBucketItem;

    }

    public BucketEntry findEntry(@Nonnull LabVessel entryVessel, @Nonnull String productOrderKey) {
        BucketEntry foundEntry = null;
        for (BucketEntry currentEntry : bucketEntries) {
            if (currentEntry.getLabVessel().equals(entryVessel) &&
                currentEntry.getPoBusinessKey().equals(productOrderKey)) {
                foundEntry = currentEntry;
                break;
            }
        }

        return foundEntry;
    }
}
