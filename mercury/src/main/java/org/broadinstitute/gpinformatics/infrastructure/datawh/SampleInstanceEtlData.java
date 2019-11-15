package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import java.util.Date;

/**
 * Shared logic to extract PDO, lab batch, and sample ID ETL data from a sample instance.
 */
public class SampleInstanceEtlData {
    private ProductOrder pdo;
    private LabBatch labBatch;
    private ProductOrderSample pdoSample;
    private String pdoSampleId;
    private String molecularIndexingSchemeName;
    private String nearestSampleID;

    private boolean includeNearestSample;

    /**
     * Created via factory method only
     */
    private SampleInstanceEtlData() {
    }

    /**
     * Factory method
     *
     * @param si                   The SampleInstanceV2 from which to extract data
     * @param contextDate          Required to find the newest bucket entry the sample was in (e.g. event date, sequencing run date)
     * @param includeNearestSample Optionally find the nearest sample ID in the chain of events
     *
     * @return An instance populated with all available data elements
     */
    public static SampleInstanceEtlData buildFromSampleInstance(
            SampleInstanceV2 si, Date contextDate, boolean includeNearestSample) {
        SampleInstanceEtlData sampleInstanceData = new SampleInstanceEtlData();
        sampleInstanceData.includeNearestSample = includeNearestSample;

        // Nearest PDO sample in past transfers
        sampleInstanceData.pdoSample = si.getSingleProductOrderSample();
        if( sampleInstanceData.pdoSample != null ) {
            sampleInstanceData.pdo = sampleInstanceData.pdoSample.getProductOrder();
            sampleInstanceData.pdoSampleId = sampleInstanceData.pdoSample.getMercurySample().getSampleKey();
        }

        // Best source of lab batch is from single bucket entry
        BucketEntry singleBucketentry = si.getSingleBucketEntry();
        if( singleBucketentry != null ) {
            sampleInstanceData.labBatch = singleBucketentry.getLabBatch();
            sampleInstanceData.pdoSample   = si.getProductOrderSampleForSingleBucket();
            sampleInstanceData.pdo = singleBucketentry.getProductOrder();
            if( sampleInstanceData.pdoSample != null ) {
                sampleInstanceData.pdoSampleId = sampleInstanceData.pdoSample.getMercurySample().getSampleKey();
            }
        } else {
            sampleInstanceData.labBatch = si.getSingleBatch();
        }

        if( includeNearestSample ){
            // This will capture controls
            sampleInstanceData.nearestSampleID = si.getNearestMercurySampleName();
        }

        // Batch logic for cases of ambiguous LCSETs
        if (sampleInstanceData.labBatch == null) {
            // Use the newest non-extraction bucket which was created sometime before this event
            long eventTs = contextDate.getTime();
            long bucketTs = 0L;
            for (BucketEntry bucketEntry : si.getAllBucketEntries()) {
                if (bucketEntry.getLabBatch() != null
                    && ( bucketEntry.getLabBatch().getBatchName().startsWith("LCSET")
                        ||  bucketEntry.getLabBatch().getBatchName().startsWith("ARRAY") )
                    && bucketEntry.getCreatedDate().getTime() > bucketTs
                    && bucketEntry.getCreatedDate().getTime() <= eventTs) {
                    sampleInstanceData.labBatch = bucketEntry.getLabBatch();
                    sampleInstanceData.pdo = bucketEntry.getProductOrder();
                    bucketTs = bucketEntry.getCreatedDate().getTime();
                }
            }
        }

        sampleInstanceData.molecularIndexingSchemeName = si.getMolecularIndexingScheme() != null ?
                si.getMolecularIndexingScheme().getName() : null;

        return sampleInstanceData;
    }

    public ProductOrder getPdo() {
        return pdo;
    }

    public String getPdoSampleId() {
        return pdoSampleId;
    }

    public LabBatch getLabBatch() {
        return labBatch;
    }

    public ProductOrderSample getPdoSample() {
        return pdoSample;
    }

    public String getMolecularIndexingSchemeName() {
        return molecularIndexingSchemeName;
    }

    public String getNearestSampleID() {
        if(!includeNearestSample) {
            throw new IllegalStateException("The option to include nearest sample was not set.");
        }
        return nearestSampleID;
    }
}
