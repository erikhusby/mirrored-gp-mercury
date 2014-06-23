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
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.ReworkReasonDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.ReworkDetail;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.ReworkLevel;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.ReworkReason;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel.SampleType.PREFER_PDO;

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
    private WorkflowLoader workflowLoader;

    @Inject
    private ReworkReasonDao reworkReasonDao;

    @Inject
    private ProductOrderSampleDao productOrderSampleDao;

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
     *
     * TODO Remove.  Only called from test cases.
     *
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

        List<LabVessel> labVessels = new ArrayList<>();
        labVessels.addAll(labVesselDao.findByListIdentifiers(query));
        labVessels.addAll(labVesselDao.findBySampleKeyList(query));

        for (LabVessel vessel : labVessels) {

            /*
             * By using PREFER_PDO, we are able to get all samples for a vessel.  If there are samples associated with
             * PDOs, we will get just the PDO associated Samples.  If there are samples that are NOT associated with
             * PDOs, we will get those samples.
             */
            Set<SampleInstance> sampleInstances = vessel.getSampleInstances(PREFER_PDO, null);

            List<String> sampleIds = new ArrayList<>(sampleInstances.size());
            for (SampleInstance currentInstance : sampleInstances) {
                sampleIds.add(currentInstance.getStartingSample().getSampleKey());
            }

            Collection<ProductOrderSample> mapBySamples= productOrderSampleDao.findBySamples(sampleIds);
            bucketCandidates.addAll(collectBucketCandidatesForAMercuryVessel(vessel, mapBySamples));
        }

        // TODO: be smarter about which inputs produced results and query BSP for any that had no results from Mercury
        if (bucketCandidates.isEmpty()) {
            Collection<ProductOrderSample> sampleCollection = productOrderSampleDao.findBySamples(query);
            bucketCandidates.addAll(collectBucketCandidatesThatHaveBSPVessels(sampleCollection));
        }

        return bucketCandidates;
    }

    /**
     * collectBucketCandidatesThatHaveBSPVessels will, given a collection of ProductOrderSamples, create BucketCandidates
     * for the samples that have a Vessel that is represented in BSP.
     *
     *
     * @param samplesById Map of ProductOrderSamples indexed by Sample Name
     *
     * @return  A collection of Bucket Candidates to add to the collection of found Bucket Candidates
     */
    public Collection<BucketCandidate> collectBucketCandidatesThatHaveBSPVessels(
            Collection<ProductOrderSample> samplesById) {

        Collection<BucketCandidate> bucketCandidates = new ArrayList<>();
            Collection<String> sampleIDs = new HashSet<>();
            for (ProductOrderSample sample : samplesById) {
                sampleIDs.add(sample.getName());
            }
            Map<String, BSPSampleDTO> bspResult = bspSampleDataFetcher.fetchSamplesFromBSP(sampleIDs);
            bspSampleDataFetcher.fetchSamplePlastic(bspResult.values());
            for (ProductOrderSample sample : samplesById) {
                Workflow workflow = sample.getProductOrder().getProduct().getWorkflow();

                if (!sample.getProductOrder().isDraft() && Workflow.isWorkflowSupportedByMercury(workflow)) {
                    String sampleKey = sample.getName();
                    String tubeBarcode = bspResult.get(sampleKey).getBarcodeForLabVessel();

                    bucketCandidates.add(getBucketCandidateConsideringProductFamily(sample, sampleKey,
                            tubeBarcode, ProductFamily.ProductFamilyName.EXOME, null, ""));
                }
            }
        return bucketCandidates;
    }

    /**
     * collectBucketCandidatesForAMercuryVessel will extract bucket candidates from a collection of productOrderSamples
     * that are associated with a given Vessel.
     *
     *
     * @param vessel                lab Vessel for which the samples are associated
     * @param productOrderSamples   Collection of ProductOrderSamples that are associated with the Samples in the
     *                              Vessels hierarchy
     *
     * @return A collection of Bucket Candidates to add to the collection of found Bucket Candidates
     */
    public Collection<BucketCandidate> collectBucketCandidatesForAMercuryVessel(LabVessel vessel,
                                                                                Collection<ProductOrderSample> productOrderSamples) {

        Collection<BucketCandidate> bucketCandidates = new ArrayList<>();
            // make sure we have a matching product order sample
            for (ProductOrderSample sample : productOrderSamples) {
                Workflow workflow = sample.getProductOrder().getProduct().getWorkflow();

                if (!sample.getProductOrder().isDraft() && Workflow.isWorkflowSupportedByMercury(workflow)) {

                    String eventName = vessel.getLastEventName();

                    bucketCandidates.add(getBucketCandidateConsideringProductFamily(sample, sample.getName(),
                            vessel.getLabel(),
                            ProductFamily.ProductFamilyName.EXOME, vessel, eventName));
                }
            }
        return bucketCandidates;
    }

    /**
     * getBucketCandidateConsideringProductFamily will construct a bucket candidate based on given key information
     * and add a validation message if the candidate does not match the desired product family
     *
     * @param sample            ProductOrderSample that is associated with the desired bucket candidate
     * @param sampleKey         name of the sample to which the BucketCandidate will be associated
     * @param tubeBarcode       2d Manufacturers Barcode of the labvessel to which this candidate is to be associated
     * @param productFamily     Product family for which this bucket Candidate is valid
     * @param labVessel         Entity representation of the Lab Vessle to which this candidate is to be associated
     * @param lastEventStep     step in the workflow for which the vessel for this candidate originated.  Typically
     *                          used for Reworks.
     *
     * @return  A new instance of a Bucket Candidate
     */
    public BucketCandidate getBucketCandidateConsideringProductFamily(@Nonnull ProductOrderSample sample,
                                                                      @Nonnull String sampleKey,
                                                                      @Nonnull String tubeBarcode,
                                                                      @Nonnull ProductFamily.ProductFamilyName productFamily,
                                                                      LabVessel labVessel, String lastEventStep) {
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
     * @param productOrder    the product order that the vessel is being added to the bucket for
     * @param reworkReason    If this is a rework, why the rework is being done
     * @param reworkFromStep  If this is a rework, where the rework should be reworked from
     * @param bucket          the bucket to which the sample is to be added
     * @param comment         If this is a rework, text describing why you are doing this
     * @param userName        the user adding the sample to the bucket, in case vessels/samples need to be created
     *                        on-the-fly
     * @param reworkCandidate Indicates whether the sample being added is for a rework
     *
     * @return The LabVessel instance related to the 2D Barcode given in the method call
     *
     * @throws ValidationException
     */
    private LabVessel addCandidate(@Nonnull LabVessel candidateVessel, @Nonnull ProductOrder productOrder,
                                   ReworkReason reworkReason, LabEventType reworkFromStep,
                                   @Nonnull Bucket bucket, String comment, @Nonnull String userName,
                                   boolean reworkCandidate)
            throws ValidationException {
        Collection<BucketEntry> bucketEntries = bucketEjb
                .add(Collections.singleton(candidateVessel), bucket,
                        reworkCandidate ? BucketEntry.BucketEntryType.REWORK_ENTRY :
                                BucketEntry.BucketEntryType.PDO_ENTRY,
                        userName, LabEvent.UI_EVENT_LOCATION, LabEvent.UI_PROGRAM_NAME,
                        reworkFromStep, productOrder);

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
                validationMessages.addAll(addAndValidateBucketCandidate(bucketCandidate, reason, bucket, comment,
                        userName));
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
    private Collection<String> addAndValidateBucketCandidate(
            @Nonnull BucketCandidate bucketCandidate,
            @Nonnull ReworkReason reworkReason,
            @Nonnull Bucket bucket,
            @Nonnull String comment,
            @Nonnull String userName)
            throws ValidationException {

        Workflow workflow = bucketCandidate.getProductOrder().getProduct().getWorkflow();
        WorkflowBucketDef bucketDef = findWorkflowBucketDef(workflow, bucket.getBucketDefinitionName());
        LabEventType reworkFromStep = bucketDef.getBucketEventType();

        LabVessel reworkVessel =
                getLabVessel(bucketCandidate.getTubeBarcode(), bucketCandidate.getSampleKey(), userName);

        Collection<String> validationMessages =
                validateBucketItem(reworkVessel, ProductWorkflowDefVersion.findBucketDef(workflow, reworkFromStep),
                        bucketCandidate.getProductOrder(), bucketCandidate.getSampleKey(),
                        bucketCandidate.isReworkItem());

        addCandidate(reworkVessel, bucketCandidate.getProductOrder(), reworkReason, reworkFromStep,
                bucket, comment, userName, bucketCandidate.isReworkItem());

        return validationMessages;
    }

    private WorkflowBucketDef findWorkflowBucketDef(@Nonnull Workflow workflow, String bucketName) {
        WorkflowConfig workflowConfig = workflowLoader.load();
        ProductWorkflowDefVersion workflowDefVersion = workflowConfig.getWorkflow(workflow)
                                                                     .getEffectiveVersion();
        WorkflowBucketDef bucketDef = workflowDefVersion.findBucketDefByName(bucketName);
        if (bucketDef == null) {
            throw new RuntimeException("Could not find bucket definition for: " + bucketName);
        }
        return bucketDef;
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
    public Collection<String> validateBucketItem(@Nonnull LabVessel candidateVessel,
                                                 @Nonnull WorkflowBucketDef bucketDef,
                                                 @Nonnull ProductOrder productOrder, @Nonnull String sampleKey,
                                                 boolean reworkItem)
            throws ValidationException {

        List<String> validationMessages = new ArrayList<>();

        if (candidateVessel.checkCurrentBucketStatus(productOrder, bucketDef.getName(), BucketEntry.Status.Active)) {
            String error =
                    String.format("Tube %s with sample %s in product order %s already exists in the %s bucket.",
                            candidateVessel.getLabel(), sampleKey, productOrder.getJiraTicketKey(), bucketDef.getName());
            logger.error(error);
            throw new ValidationException(error);
        }

        if (reworkItem && !candidateVessel.hasAncestorBeenInBucket(bucketDef.getName())) {
            validationMessages.add("You have submitted a vessel to the bucket that may not be considered a rework.  " +
                                   "No ancestor of " + candidateVessel.getLabel() + " has ever been in been in the " +
                                   bucketDef.getName() + " before.");
        }

        if (!bucketDef.meetsBucketCriteria(candidateVessel)) {
            validationMessages.add("You have submitted a vessel to the bucket that contains at least one sample that " +
                                   "is not DNA");
        }

        return validationMessages;
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
         *
         * Only called from tests classes
         *
         * @param tubeBarcode
         */
        @Deprecated
        public BucketCandidate(@Nonnull String tubeBarcode) {
            this.tubeBarcode = tubeBarcode;
        }

        /**
         *
         * Only called from test classes
         *
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
            this.productOrder = productOrder;
            this.labVessel = labVessel;
            this.lastEventStep = lastEventStep;
            this.currentSampleKey = StringUtils.join(this.labVessel.getSampleInstances(LabVessel.SampleType.ANY, null), ", ");
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
