package org.broadinstitute.gpinformatics.mercury.entity.bucket;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;
import org.hibernate.annotations.BatchSize;
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
    @Where(clause = "status='Active' and entry_type='PDO_ENTRY'")
    @BatchSize(size = 100)
    private Set<BucketEntry> bucketEntries = new HashSet<>();

    @OneToMany(mappedBy = "bucket", cascade = CascadeType.PERSIST)
    @Where(clause = "status='Active' and entry_type='REWORK_ENTRY'")
    @BatchSize(size = 100)
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

    /**
     * Returns the active bucket entries for this bucket, sorted by added date and product order ranking.
     *
     * @return the active bucket entries
     *
     * @see BucketEntry#byDate
     */
    public Collection<BucketEntry> getBucketEntries() {

        List<BucketEntry> setList = new ArrayList<>(bucketEntries);
        Collections.sort(setList, BucketEntry.byDate);
        return Collections.unmodifiableList(setList);
    }

    /**
     * Returns the active rework entries for this bucket, sorted by added date and product order ranking.
     *
     * @return the active rework entries
     *
     * @see BucketEntry#byDate
     */
    public Collection<BucketEntry> getReworkEntries() {
        List<BucketEntry> setList = new ArrayList<>(reworkEntries);
        Collections.sort(setList, BucketEntry.byDate);
        return Collections.unmodifiableList(setList);
    }

    /**
     * add a rework entry.
     * package-local because this should only be used in tests
     * @param reworkEntry
     */
    void addReworkEntry(BucketEntry reworkEntry) {
        reworkEntries.add(reworkEntry);
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
     * Helper method to add a new item into the bucket
     *
     * @param productOrder    a Product order to associate with the new entry
     * @param vessel          Lab Vessel to enter into the bucket.
     * @param entryType
     *
     * @param workflow
     * @return an instance of a Bucket entry which represents the lab vessel and the product order for that entry
     */
    public BucketEntry addEntry(ProductOrder productOrder, LabVessel vessel, BucketEntry.BucketEntryType entryType,
                                @Nonnull Workflow workflow) {
        int productOrderRanking = getBucketEntries().size() + 1;
        BucketEntry newEntry =
                new BucketEntry(vessel, productOrder, this, entryType, productOrderRanking);
        bucketEntries.add(newEntry);
        vessel.addBucketEntry(newEntry);
        return newEntry;
    }

    // TODO: since this is currently only used in tests it should be moved, or the tests should use a different constructor.
    public BucketEntry addEntry(ProductOrder productOrder, LabVessel vessel, BucketEntry.BucketEntryType entryType) {
        return addEntry(productOrder, vessel, entryType, productOrder.getProduct().getWorkflow());
    }

    /**
     * Removes an item from bucket
     *
     * @param entryToRemove instance of a bucket entry to remove which represents the lab vessel and the product order
     *                      for that entry
     */
    public void removeEntry(BucketEntry entryToRemove) {
        switch (entryToRemove.getEntryType()) {
        case PDO_ENTRY:
            bucketEntries.remove(entryToRemove);
            break;
        case REWORK_ENTRY:
            reworkEntries.remove(entryToRemove);
            break;
        default:
            throw new RuntimeException("Unexpected bucket entry type: " + entryToRemove.getEntryType());
        }
        entryToRemove.setStatus(BucketEntry.Status.Archived);
    }

    public Long getBucketId() {
        return bucketId;
    }

    public BucketEntry findEntry(@Nonnull LabVessel entryVessel) {

        List<BucketEntry> foundEntries = new LinkedList<>();
        BucketEntry foundBucketItem = null;

        for (BucketEntry currEntry : bucketEntries) {
            if (currEntry.getLabVessel().equals(entryVessel)) {
                foundEntries.add(currEntry);
            }
        }

        for (BucketEntry reworkEntry : reworkEntries) {
            if (reworkEntry.getLabVessel().equals(entryVessel)) {
                foundEntries.add(reworkEntry);
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
                currentEntry.getProductOrder().getBusinessKey().equals(productOrderKey)) {
                foundEntry = currentEntry;
                break;
            }
        }

        return foundEntry;
    }
}
