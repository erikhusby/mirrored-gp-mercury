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

        if( includeNearestSample ){
            // This will capture controls
            sampleInstanceData.nearestSampleID = si.getNearestMercurySampleName();
        }

        // Best source of lab batch is from nearest event vessel
        LabBatch labBatch = si.getSingleBatch();
        sampleInstanceData.labBatch = labBatch;

        // PDO from single product order sample
        ProductOrderSample pdoSample = si.getSingleProductOrderSample();
        if( pdoSample != null ) {
            sampleInstanceData.pdoSample   = pdoSample;
            sampleInstanceData.pdoSampleId = pdoSample.getMercurySample().getSampleKey();
            sampleInstanceData.pdo = pdoSample.getProductOrder();
        }

        sampleInstanceData.molecularIndexingSchemeName = si.getMolecularIndexingScheme() != null ?
                si.getMolecularIndexingScheme().getName() : null;

        // ********* No need for further logic if lab batch and PDO sample found
        if( labBatch != null && pdoSample != null ) {
            return sampleInstanceData;
        }

        // ******** lab batch and/or PDO sample NOT found, try from single bucket entry ********
        BucketEntry singleBucketentry = si.getSingleBucketEntry();
        if( singleBucketentry != null ) {
            if( labBatch == null ) {
                labBatch = singleBucketentry.getLabBatch();
                sampleInstanceData.labBatch = labBatch;
            }
            if( pdoSample == null ) {
                pdoSample = si.getProductOrderSampleForSingleBucket();
                if (pdoSample != null) {
                    sampleInstanceData.pdo = pdoSample.getProductOrder();
                    sampleInstanceData.pdoSample = pdoSample;
                    sampleInstanceData.pdoSampleId = pdoSample.getMercurySample().getSampleKey();
                }
            }
        }

        // Missing single bucket entry or PDO Sample not bucketed
        if( pdoSample == null ) {
            // Get latest PDO and the sample which matches it (the PDO and sample must match)
            for (ProductOrderSample pdoSampleIter : si.getAllProductOrderSamples()) {
                // Get a valid PDO if none found from singleBucketEntry
                if( singleBucketentry == null ) {
                    sampleInstanceData.pdo = pdoSampleIter.getProductOrder();
                }
                if (pdoSampleIter.getMercurySample() != null) {
                    // And associate a sample with it if available
                    sampleInstanceData.pdoSample = pdoSampleIter;
                    sampleInstanceData.pdoSampleId = pdoSampleIter.getMercurySample().getSampleKey();
                    // getAllProductOrderSamples() sorts by closest first
                    // We've got the correct PDO data on the first PDO created before context date
                    if (pdoSampleIter.getProductOrder().getCreatedDate().compareTo(contextDate) < 1) {
                        break;
                    }
                }
            }
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
                    bucketTs = bucketEntry.getCreatedDate().getTime();
                    // Bucket entry meets the criteria, so hold onto newest sample ID
                    if (includeNearestSample && !bucketEntry.getLabVessel().getMercurySamples().isEmpty()) {
                        sampleInstanceData.nearestSampleID =
                                bucketEntry.getLabVessel().getMercurySamples().iterator().next()
                                        .getSampleKey();
                    }
                }
            }
        }

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
