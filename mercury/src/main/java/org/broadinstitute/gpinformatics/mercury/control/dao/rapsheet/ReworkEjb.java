/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.ValidationWithRollbackException;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.ReworkReasonDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.ReworkDetail;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.ReworkLevel;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.ReworkReason;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates the business logic related to rework. This includes the creation of a new batch
 * entity and saving that to Jira.
 * <p/>
 * More information about Rework can be found here:
 * https://confluence.broadinstitute.org/display/GPI/Rework
 * <p/>
 * Some info on Rap sheets can be found here:
 * https://confluence.broadinstitute.org/display/GPI/January+Demo
 */
@Stateful
@RequestScoped
public class ReworkEjb {
    private static final Log logger = LogFactory.getLog(ReworkEjb.class);

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private ReworkEntryDao reworkEntryDao;

    @Inject
    private LabVesselDao labVesselDao;

    private BSPSampleDataFetcher bspSampleDataFetcher;

    @Inject
    private BucketEjb bucketEjb;

    @Inject
    private BucketDao bucketDao;

    @Inject
    private BucketEntryDao bucketEntryDao;

    @Inject
    private ReworkReasonDao reworkReasonDao;

    @Inject
    private ProductOrderSampleDao productOrderSampleDao;

    @Inject
    private WorkflowConfig workflowConfig;

    public ReworkEjb() {
    }

    /**
     * Searches for and returns candidate vessels and samples that can be used for "rework". All candidates must at
     * least have a single sample, a tube barcode, and a PDO. Multiple results for the same sample may be returned if
     * the sample is included in multiple PDOs.
     *
     * @param query tube barcode or sample ID to search for
     *
     * @return tube/sample/PDO selections that are valid for rework
     * <p/>
     * TODO Remove.  Only called from test cases.
     */
    public Collection<BucketCandidate> findBucketCandidates(String query) {
        return findBucketCandidates(Collections.singletonList(query));
    }

    /**
     * Find candidate ProductOrderKeys for BucketEntryId
     *
     * @return SortedSet of ProductOrderKeys
     */
    public Set<String> findBucketCandidatePdos(List<Long> bucketEntryId) {
        List<BucketEntry> foundEntries = bucketEntryDao.findByIds(bucketEntryId);
        Set<String> productOrderKeys = new HashSet<>();
        for (BucketEntry entry : foundEntries) {
            String label = entry.getLabVessel().getLabel();
            Collection<BucketCandidate> bucketCandidates = findBucketCandidates(label);
            for (BucketCandidate bucketCandidate : bucketCandidates) {
                productOrderKeys.add(bucketCandidate.getProductOrder().getBusinessKey());
            }
        }

        return productOrderKeys;
    }

    /**
     * Searches for and returns candidate vessels and samples that can be used for "rework". All candidates must at
     * least have a single sample, a tube barcode, and a PDO. Multiple results for the same sample may be returned if
     * the sample is included in multiple PDOs.
     *
     * @param query list of tube barcodes or sample IDs to search for
     *
     * @return tube/sample/PDO selections that are valid for rework
     */
    public Collection<BucketCandidate> findBucketCandidates(List<String> query) {
        Collection<BucketCandidate> bucketCandidates = new ArrayList<>();

        Set<LabVessel> labVessels = new HashSet<>();
        labVessels.addAll(labVesselDao.findByListIdentifiers(query));
        labVessels.addAll(labVesselDao.findBySampleKeyList(query));

        for (LabVessel vessel : labVessels) {
            List<ProductOrderSample> productOrderSamples = new ArrayList<>();
            for (SampleInstanceV2 sampleInstanceV2 : vessel.getSampleInstancesV2()) {
                for (ProductOrderSample productOrderSample : sampleInstanceV2.getAllProductOrderSamples()) {
                    if (productOrderSample.getProductOrder().getOrderStatus().readyForLab()) {
                        productOrderSamples.add(productOrderSample);
                    }
                }
            }
            // For a pooled tube added to a PDO, the above loop on getSampleInstancesV2 won't find ProductOrderSamples.
            // However, there may be a ProductOrderSample with a "sampleName" of the tube barcode.
            if (productOrderSamples.isEmpty()) {
                List<ProductOrderSample> bySamples = productOrderSampleDao.findBySamples(Collections.singleton(
                        vessel.getLabel()));
                if (bySamples.size() == 1) {
                    productOrderSamples.add(bySamples.get(0));
                }
            }
            bucketCandidates.addAll(collectBucketCandidatesForAMercuryVessel(vessel, productOrderSamples));
        }

        /*
         * For any sample whose sample data lives in Mercury, a bucket candidate will already have been found.
         * Therefore, there is no risk of querying BSP for these samples from here.
         *
         * If there are only BSP-sample-data samples in the query that Mercury does not know about, then
         *      bucketCandidates will be empty and the query will extend to search in BSP.
         * If there are only Mercury-sample-data samples in the query, bucketCandidates will not be empty.
         * If there are a mix of samples in the query, some subset may have been found and bucketCandidates created for
         *      them, in which case BSP will not be queried.
         *
         * TODO: Filter out of the user's query samples for which we've found candidates in Mercury rather than
         * querying BSP for all samples.
         * The above scenarios need to be taken into account when doing this to avoid querying BSP for samples whose
         * sample data is in Mercury, especially those that are BSP tubes that have been exported to CRSP!
         */
        if (bucketCandidates.isEmpty()) {
            Collection<ProductOrderSample> sampleCollection = new ArrayList<>();
            for (ProductOrderSample productOrderSample : productOrderSampleDao.findBySamples(query)) {
                if (productOrderSample.getProductOrder().getOrderStatus().readyForLab()) {
                    sampleCollection.add(productOrderSample);
                }
            }
            bucketCandidates.addAll(collectBucketCandidatesThatHaveBSPVessels(sampleCollection));
        }
        return bucketCandidates;
    }

    /**
     * collectBucketCandidatesThatHaveBSPVessels will, given a collection of ProductOrderSamples, create
     * BucketCandidates for the samples that have a Vessel that is represented in BSP.
     *
     * @param samplesById Map of ProductOrderSamples indexed by Sample Name
     *
     * @return A collection of Bucket Candidates to add to the collection of found Bucket Candidates
     */
    public Collection<BucketCandidate> collectBucketCandidatesThatHaveBSPVessels(
            Collection<ProductOrderSample> samplesById) {

        Collection<BucketCandidate> bucketCandidates = new ArrayList<>();
        Collection<String> sampleIDs = new HashSet<>();
        for (ProductOrderSample sample : samplesById) {
            sampleIDs.add(sample.getName());
        }
        Map<String, BspSampleData> bspResult = bspSampleDataFetcher.fetchSampleData(sampleIDs);
        bspSampleDataFetcher.fetchSamplePlastic(bspResult.values());
        for (ProductOrderSample sample : samplesById) {
            if (productOrderSampleCanEnterBucket(sample)) {
                String sampleKey = sample.getName();
                String tubeBarcode = bspResult.get(sampleKey).getBarcodeForLabVessel();
                if (StringUtils.isNotBlank(tubeBarcode)) {
                    bucketCandidates.add(getBucketCandidateConsideringProductFamily(sample, sampleKey,
                            tubeBarcode, ProductFamily.ProductFamilyInfo.EXOME, null, ""));
                }
            }
        }
        return bucketCandidates;
    }

    private boolean productOrderSampleCanEnterBucket(ProductOrderSample sample) {
        Workflow workflow = sample.getProductOrder().getProduct().getWorkflow();
        return sample.getProductOrder().getOrderStatus().readyForLab() && workflow.isWorkflowSupportedByMercury();
    }

    /**
     * collectBucketCandidatesForAMercuryVessel will extract bucket candidates from a collection of productOrderSamples
     * that are associated with a given Vessel.
     *
     * @param vessel              lab Vessel for which the samples are associated
     * @param productOrderSamples Collection of ProductOrderSamples that are associated with the Samples in the
     *                            Vessels hierarchy
     *
     * @return A collection of Bucket Candidates to add to the collection of found Bucket Candidates
     */
    public Collection<BucketCandidate> collectBucketCandidatesForAMercuryVessel(LabVessel vessel,
            Collection<ProductOrderSample> productOrderSamples) {

        Collection<BucketCandidate> bucketCandidates = new ArrayList<>();
        // make sure we have a matching product order sample
        for (ProductOrderSample sample : productOrderSamples) {
            if (productOrderSampleCanEnterBucket(sample)) {
                String eventName = vessel.getLastEventName();
                bucketCandidates.add(getBucketCandidateConsideringProductFamily(sample, sample.getName(),
                        vessel.getLabel(),
                        ProductFamily.ProductFamilyInfo.EXOME, vessel, eventName));
            }
        }
        return bucketCandidates;
    }

    /**
     * getBucketCandidateConsideringProductFamily will construct a bucket candidate based on given key information
     * and add a validation message if the candidate does not match the desired product family
     *
     * @param sample        ProductOrderSample that is associated with the desired bucket candidate
     * @param sampleKey     name of the sample to which the BucketCandidate will be associated
     * @param tubeBarcode   2d Manufacturers Barcode of the labvessel to which this candidate is to be associated
     * @param productFamily Product family for which this bucket Candidate is valid
     * @param labVessel     Entity representation of the Lab Vessle to which this candidate is to be associated
     * @param lastEventStep step in the workflow for which the vessel for this candidate originated.  Typically
     *                      used for Reworks.
     *
     * @return A new instance of a Bucket Candidate
     */
    public BucketCandidate getBucketCandidateConsideringProductFamily(@Nonnull ProductOrderSample sample,
            @Nonnull String sampleKey, @Nonnull String tubeBarcode,
            @Nonnull ProductFamily.ProductFamilyInfo productFamily, LabVessel labVessel, String lastEventStep) {
        BucketCandidate candidate = new BucketCandidate(sampleKey, tubeBarcode,
                sample.getProductOrder(), labVessel, lastEventStep);
        if (!sample.getProductOrder().getProduct().isSameProductFamily(productFamily)) {
            candidate.addValidationMessage("The PDO " + sample.getProductOrder().getBusinessKey() +
                                           " for Sample " + sampleKey +
                                           " is not part of the Exome family");
        }
        return candidate;
    }

    /**
     * Create rework for all samples in a LabVessel.
     * <p/>
     * TODO: make this @DaoFree; can't right now because of an eventual call to persist from LabEventFactory
     *
     * @param candidateVessel the vessel being added to the bucket
     * @param bucketDef
     * @param productOrder    the product order that the vessel is being added to the bucket for
     * @param reworkReason    If this is a rework, why the rework is being done
     * @param reworkFromStep  If this is a rework, where the rework should be reworked from
     * @param comment         If this is a rework, text describing why you are doing this
     * @param userName        the user adding the sample to the bucket, in case vessels/samples need to be created
     *                        on-the-fly
     * @param reworkCandidate Indicates whether the sample being added is for a rework
     *
     * @return The LabVessel instance related to the 2D Barcode given in the method call
     *
     * @throws ValidationException
     */
    private LabVessel addCandidate(@Nonnull final LabVessel candidateVessel, final WorkflowBucketDef bucketDef,
                                   @Nonnull ProductOrder productOrder, ReworkReason reworkReason,
                                   LabEventType reworkFromStep, String comment, @Nonnull String userName,
                                   boolean reworkCandidate, Date date) throws ValidationException {
        Map<WorkflowBucketDef, Collection<LabVessel>> bucketCandidate =
                new HashMap<WorkflowBucketDef, Collection<LabVessel>>() {
                    {
                        put(bucketDef, Collections.singleton(candidateVessel));
                    } };

        Collection<BucketEntry> bucketEntries = bucketEjb.add(bucketCandidate,
                reworkCandidate ? BucketEntry.BucketEntryType.REWORK_ENTRY : BucketEntry.BucketEntryType.PDO_ENTRY,
                LabEvent.UI_PROGRAM_NAME, userName, LabEvent.UI_EVENT_LOCATION, productOrder, date);

        // TODO: create the event in this scope instead of getting the "latest" event
        if (reworkCandidate) {
            for (BucketEntry bucketEntry : bucketEntries) {
                ReworkDetail reworkDetail =
                        new ReworkDetail(reworkReason, ReworkLevel.ONE_SAMPLE_RELEASE_REST_BATCH,
                                reworkFromStep, comment, bucketEntry.getLabVessel().getLatestEvent());
                bucketEntry.setReworkDetail(reworkDetail);
            }
        }

        return candidateVessel;
    }

    private LabVessel getLabVessel(String tubeBarcode, String sampleKey, String userName) {
        LabVessel reworkVessel = labVesselDao.findByIdentifier(tubeBarcode);

        if (reworkVessel == null) {
            reworkVessel = bucketEjb.createInitialVessels(Collections.singleton(sampleKey),
                    userName).iterator().next();
        }
        return reworkVessel;
    }

    /**
     * Validate and add a group of reworks to the specified bucket. This is the primary entry point for clients, e.g.
     * action beans.
     *
     * @param bucketCandidates tubes/samples/PDOs that are to be reworked
     * @param reworkReason     predefined text describing why the given vessels need to be reworked
     * @param comment          brief user comment to associate with these reworks
     * @param userName         the user adding the reworks, in case vessels/samples need to be created on-the-fly
     * @param bucketName       the name of the bucket to add reworks to
     *
     * @return Collection of validation messages
     *
     * @throws ValidationException Thrown in the case that some checked state of any Lab Vessel will not allow the
     *                             method to continue
     */
    public Collection<String> addAndValidateCandidates(@Nonnull Collection<BucketCandidate> bucketCandidates,
                                                       @Nonnull String reworkReason, @Nonnull String comment,
                                                       @Nonnull String userName,
                                                       @Nonnull String bucketName)
            throws ValidationWithRollbackException {
        Bucket bucket = bucketEjb.findOrCreateBucket(bucketName);
        ReworkReason reason = reworkReasonDao.findByReason(reworkReason);
        if (reason == null) {
            reason = new ReworkReason(reworkReason);
        }
        Collection<String> validationMessages = new ArrayList<>();
        for (BucketCandidate bucketCandidate : bucketCandidates) {
            try {
                addAndValidateBucketCandidate(bucketCandidate, reason, bucket, comment, userName);
            } catch (ValidationException e) {
                throw new ValidationWithRollbackException(e);
            }
        }
        return validationMessages;
    }

    /**
     * Validate and add a single rework to the specified bucket.
     *
     * @param bucketCandidate tube/sample/PDO that is to be reworked
     * @param reworkReason    predefined Text describing why the given vessel needs to be reworked
     * @param bucket          the bucket to add rework to
     * @param comment         brief user comment to associate with this rework
     * @param userName        the user adding the rework, in case vessels/samples need to be created on-the-fly
     *
     * @return Collection of validation messages
     *
     * @throws ValidationException Thrown in the case that some checked state of the Lab Vessel will not allow the
     *                             method to continue
     */
    private void addAndValidateBucketCandidate(
            @Nonnull BucketCandidate bucketCandidate,
            @Nonnull ReworkReason reworkReason,
            @Nonnull Bucket bucket,
            @Nonnull String comment,
            @Nonnull String userName)
            throws ValidationException {
        WorkflowBucketDef bucketDef = null;
        try {
            bucketDef = workflowConfig.findWorkflowBucketDef(bucketCandidate.getProductOrder(),
                    bucket.getBucketDefinitionName());
        } catch (RuntimeException e) {
            String error = e.getLocalizedMessage();
            throw new ValidationException(error);
        }
        if (bucketDef == null) {
            throw new ValidationException(String.format(
                    "%s cannot be added to '%s' because that bucket is invalid for the product '%s'",
                    bucketCandidate.getSampleKey(), bucket.getBucketDefinitionName(),
                    bucketCandidate.getProductOrder().getProduct().getProductName()));
        }

        LabEventType reworkFromStep = bucketDef.getBucketEventType();

        LabVessel reworkVessel =
                getLabVessel(bucketCandidate.getTubeBarcode(), bucketCandidate.getSampleKey(), userName);

        validateBucketItem(reworkVessel, bucketDef, bucketCandidate.getProductOrder(), bucketCandidate.getSampleKey(),
                bucketCandidate.isReworkItem());

        addCandidate(reworkVessel, bucketDef, bucketCandidate.getProductOrder(), reworkReason, reworkFromStep,
                comment, userName, bucketCandidate.isReworkItem(), new Date());
    }

    /**
     * validateBucketItem will execute certain validation rules on a rework sample in order to inform a submitter of
     * any issues with the state of the LabVessel with regards to using it for Rework.
     *
     * @param candidateVessel a LabVessel instance being submitted to a bucket
     * @param bucketDef       the bucket that the samples will be added to
     * @param productOrder    the product order for which the vessel is being added to a bucket
     * @param sampleKey       the sample being for which the vessel represents
     * @param reworkItem      Indicates whether the sample being added is for rework
     *
     * @return Collection of validation messages
     */
    public void validateBucketItem(@Nonnull LabVessel candidateVessel,
                                   @Nonnull WorkflowBucketDef bucketDef,
                                   @Nonnull ProductOrder productOrder, @Nonnull String sampleKey,
                                   boolean reworkItem)
            throws ValidationException {
        if (candidateVessel.checkCurrentBucketStatus(productOrder, bucketDef.getName(), BucketEntry.Status.Active)) {
            String error =
                    String.format("Tube %s with sample %s in product order %s already exists in the %s bucket.",
                            candidateVessel.getLabel(), sampleKey, productOrder.getJiraTicketKey(),
                            bucketDef.getName());
            logger.error(error);
            throw new ValidationException(error);
        }
        if (!bucketDef.meetsBucketCriteria(candidateVessel, productOrder)) {
            String error = String.format(
                    "Vessel '%s' can not go into '%s' because it does not match the bucket's criteria. %s",
                    candidateVessel.getLabel(),
                    bucketDef.getName(),
                    bucketDef.findMissingRequirements(productOrder, candidateVessel.getLatestMaterialType()));
            logger.error(error);
            throw new ValidationException(error);
        }
    }

    // TODO: Only called from BatchToJiraTest. Can that be modified to use a method that is used by application code?
    @Deprecated
    public void addReworkToBatch(@Nonnull LabBatch batch, @Nonnull String labVesselBarcode, String userName)
            throws ValidationException {
        BucketCandidate bucketCandidate = new BucketCandidate(labVesselBarcode);
        LabVessel reworkVessel = getLabVessel(bucketCandidate.getTubeBarcode(), bucketCandidate.getSampleKey(),
                userName
        );
        batch.addReworks(Arrays.asList(reworkVessel));
    }

    public Collection<LabVessel> getVesselsForRework(String bucketName) {
        Bucket bucket = bucketDao.findByName(bucketName);
        Set<LabVessel> vessels = new HashSet<>();
        for (BucketEntry bucketEntry : bucket.getReworkEntries()) {
            vessels.add(bucketEntry.getLabVessel());
        }
        return vessels;
    }

    @Inject
    public void setBspSampleDataFetcher(BSPSampleDataFetcher bspSampleDataFetcher) {
        this.bspSampleDataFetcher = bspSampleDataFetcher;
    }

    /**
     * A candidate vessel, based on a search/lookup, that a user may choose to "rework". The vessel may have already
     * gone through a workflow, or may be a sample that is in a PDO but needs to be run through a different process than
     * its PDO would normally call for.
     * <p/>
     * This represents the selection of a LabVessel and ProductOrder combination, or the case where there is only a
     * sample and a ProductOrder because LIMS hasn't yet seen the tube containing the sample.
     * <p/>
     * TODO: remove redundant fields and object references once non-vessel sample case is implemented
     */
    public static class BucketCandidate {

        public static final String REWORK_INDICATOR = "Rework";
        private final String tubeBarcode;
        private String sampleKey;
        private ProductOrder productOrder;
        private LabVessel labVessel;
        private List<String> validationMessages = new ArrayList<>();
        private boolean reworkItem;
        private String lastEventStep;
        private String currentSampleKey;

        /**
         * Create a rework candidate with just the tube barcode. Useful mainly in tests because, since a PDO isn't
         * specified, the tube's sample had better be in only one PDO.
         * <p/>
         * Only called from tests classes
         *
         * @param tubeBarcode
         */
        @Deprecated
        public BucketCandidate(@Nonnull String tubeBarcode) {
            this.tubeBarcode = tubeBarcode;
        }

        /**
         * Only called from test classes
         */
        public BucketCandidate(@Nonnull String tubeBarcode, @Nonnull ProductOrder productOrder) {
            this.tubeBarcode = tubeBarcode;
            this.productOrder = productOrder;
        }

        /**
         * Used with Main constructor and toString only
         *
         * @param tubeBarcode
         * @param sampleKey
         * @param productOrder
         */
        public BucketCandidate(@Nonnull String tubeBarcode, @Nonnull String sampleKey,
                               @Nonnull ProductOrder productOrder) {
            this(tubeBarcode, productOrder);
            this.sampleKey = sampleKey;
        }

        public BucketCandidate(@Nonnull String sampleKey, @Nonnull String tubeBarcode,
                               ProductOrder productOrder, LabVessel labVessel, String lastEventStep) {
            this(tubeBarcode, sampleKey, productOrder);
            this.labVessel = labVessel;
            this.lastEventStep = lastEventStep;
            Set<String> sampleNames = new HashSet<>();
            if (labVessel != null) {
                for (SampleInstanceV2 instance : this.labVessel.getSampleInstancesV2()) {
                    sampleNames.add(instance.getMercuryRootSampleName());
                }
            }
            this.currentSampleKey = StringUtils.join(sampleNames, ", ");
        }

        public String getSampleKey() {
            return sampleKey;
        }

        public String getTubeBarcode() {
            return tubeBarcode;
        }

        public ProductOrder getProductOrder() {
            return productOrder;
        }

        public LabVessel getLabVessel() {
            return labVessel;
        }

        public void addValidationMessage(String message) {
            validationMessages.add(message);
        }

        public boolean isValid() {
            return validationMessages.isEmpty();
        }

        public boolean isReworkItem() {
            return reworkItem;
        }

        public void setReworkItem(boolean reworkItem) {
            this.reworkItem = reworkItem;
        }

        public String getLastEventStep() {
            return lastEventStep;
        }

        public String getCurrentSampleKey() {
            return currentSampleKey;
        }

        @Override
        public String toString() {
            return String.format("%s|%s|%s", tubeBarcode, sampleKey, productOrder.getBusinessKey());
        }

        public static BucketCandidate fromString(String s, ProductOrderDao productOrderDao) {
            String[] parts = s.split("\\|");
            String tubeBarcode = parts[0];
            String sampleKey = parts[1];
            String productOrderKey = parts[2];
            return new BucketCandidate(tubeBarcode, sampleKey, productOrderDao.findByBusinessKey(productOrderKey));
        }
    }
}
