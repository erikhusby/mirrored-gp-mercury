package org.broadinstitute.gpinformatics.mercury.entity.bucket;

import clover.org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
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
import java.io.IOException;
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
        newEntry.setBucketExistence(this);
        //TODO  SGM Create some form of lab event for adding to bucket
    }

    public void removeEntry ( BucketEntry entryToRemove) {
        bucketEntries.remove(entryToRemove);
        //TODO SGM create some form of lab event for removing from a bucket
    }



    public void createLabBatch(Set<LabVessel> entriesToBatch) throws IOException {

        //TODO Retrieve info on PDOs

        LabBatch newBatch = new LabBatch("",entriesToBatch);

        newBatch.submit();

    }

}
