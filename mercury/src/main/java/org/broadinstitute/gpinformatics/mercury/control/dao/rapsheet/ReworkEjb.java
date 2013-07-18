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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.infrastructure.mercury.MercuryClientEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.ReworkDetail;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.LabVesselComment;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.LabVesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.RapSheet;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkEntry;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
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
 * Encapsulates the business logic related to {@link RapSheet}s and rework. This includes the creation of a new batch
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
    private final static Log logger = LogFactory.getLog(ReworkEjb.class);

    @Inject
    MercurySampleDao mercurySampleDao;

    @Inject
    ReworkEntryDao reworkEntryDao;

    @Inject
    LabVesselDao labVesselDao;

    @Inject
    private AthenaClientService athenaClientService;

    @Inject
    private BSPSampleDataFetcher bspSampleDataFetcher;

    @Inject
    private MercuryClientEjb mercuryClientEjb;

    @Inject
    private BucketEjb bucketEjb;

    @Inject
    private BucketDao bucketDao;
    
    @Inject
    private BucketEntryDao bucketEntryDao;

    @Inject
    private WorkflowLoader workflowLoader;

    /**
     * Create rework for all samples in a LabVessel
     *
     * @param labVessel      the labVessel where the sample is located.
     * @param reworkReason   Why is the rework being done.
     * @param reworkLevel    What level to rework to
     * @param reworkFromStep Where should the rework be reworked from.
     * @param comment        text describing why you are doing this.
     * @param workflowName   Name of the workflow in which this vessel is to be reworked
     *
     * @throws InformaticsServiceException
     */
    @DaoFree
    public Collection<MercurySample> getVesselRapSheet(@Nonnull LabVessel labVessel,
                                                       ReworkEntry.ReworkReason reworkReason,
                                                       ReworkEntry.ReworkLevel reworkLevel,
                                                       @Nonnull LabEventType reworkFromStep,
                                                       @Nonnull String comment, String workflowName)
            throws ValidationException {

        Set<MercurySample> reworks = new HashSet<>();
        VesselPosition[] vesselPositions = labVessel.getVesselGeometry().getVesselPositions();
        if (vesselPositions == null) {
            vesselPositions = new VesselPosition[]{VesselPosition.TUBE1};
        }

        WorkflowBucketDef bucketDef = ProductWorkflowDefVersion.findBucketDef(workflowName, reworkFromStep);

        for (VesselPosition vesselPosition : vesselPositions) {
            Collection<SampleInstance> samplesAtPosition =
                    labVessel.getSamplesAtPosition(vesselPosition.name());
            for (SampleInstance sampleInstance : samplesAtPosition) {
                MercurySample mercurySample = sampleInstance.getStartingSample();

                //TODO SGM Revisit.  Ensure that this is truly what we wish to do.
                if (labVessel.checkCurrentBucketStatus(sampleInstance.getProductOrderKey(), bucketDef.getName(),
                        BucketEntry.Status.Active)) {
                    String error =
                            String.format("Sample %s in product order %s already exists in the %s bucket.",
                                    mercurySample.getSampleKey(), sampleInstance.getProductOrderKey(),
                                    bucketDef.getName());
                    logger.error(error);
                    throw new ValidationException(error);
                }

                getReworkEntryDaoFree(mercurySample, labVessel, vesselPosition,
                        reworkReason, reworkLevel, reworkFromStep, comment);

                reworks.add(mercurySample);
            }
        }
        return reworks;
    }

    /**
     * Create a @link(ReworkEntry) for one Sample
     *
     * @param mercurySample  the sample to be reworked.
     * @param labVessel      the labVessel where the sample is located.
     * @param vesselPosition Where on the vessel the sample is located.
     * @param reworkReason   Why is the rework being done.
     * @param reworkLevel    What level to rework to
     * @param reworkFromStep Where should the rework be reworked from.
     * @param comment        text describing why you are doing this.
     */
    @DaoFree
    public void getReworkEntryDaoFree(@Nonnull MercurySample mercurySample, @Nonnull LabVessel labVessel,
                                      @Nonnull VesselPosition vesselPosition,
                                      @Nonnull ReworkEntry.ReworkReason reworkReason,
                                      ReworkEntry.ReworkLevel reworkLevel, @Nonnull LabEventType reworkFromStep,
                                      String comment) {
        LabVesselPosition labVesselPosition = new LabVesselPosition(vesselPosition, mercurySample);
        LabVesselComment<ReworkEntry> reworkComment =
                new LabVesselComment<>(labVessel.getLatestEvent(), labVessel, comment);
        ReworkEntry reworkEntry = new ReworkEntry(labVesselPosition, reworkComment, reworkReason,
                reworkLevel, reworkFromStep);
        mercurySample.getRapSheet().addRework(reworkEntry);
    }

    /**
     * Searches for and returns candidate vessels and samples that can be used for "rework". All candidates must at
     * least have a single sample, a tube barcode, and a PDO. Multiple results for the same sample may be returned if
     * the sample is included in multiple PDOs.
     *
     * @param query tube barcode or sample ID to search for
     *
     * @return tube/sample/PDO selections that are valid for rework
     */
    public Collection<ReworkCandidate> findReworkCandidates(String query) {
        return findReworkCandidates(Collections.singletonList(query));
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
    public Collection<ReworkCandidate> findReworkCandidates(List<String> query) {
        Collection<ReworkCandidate> reworkCandidates = new ArrayList<>();

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

            for (Map.Entry<String, List<ProductOrderSample>> entryMap : athenaClientService
                    .findMapSampleNameToPoSample(sampleIds).entrySet()) {
                // TODO: fetch for all vessels in a single call and make looping over labVessels a @DaoFree method
                List<ProductOrderSample> productOrderSamples = entryMap.getValue();
                // make sure we have a matching product order sample
                for (ProductOrderSample sample : productOrderSamples) {

                    ReworkCandidate candidate = new ReworkCandidate(entryMap.getKey(),
                            sample.getProductOrder().getBusinessKey(), vessel.getLabel(),
                            sample.getProductOrder(), vessel);

                    if (!sample.getProductOrder().getProduct().isSameProductFamily(ProductFamily.ProductFamilyName.EXOME)) {
                        candidate.addValidationMessage("The PDO " + sample.getProductOrder().getBusinessKey() +
                                                       " for Sample " + entryMap.getKey() +
                                                       " is not part of the Exome family");
                    }

                    reworkCandidates.add(candidate);
                }
            }
        }

        // TODO: be smarter about which inputs produced results and query BSP for any that had no results from Mercury
        if (reworkCandidates.isEmpty()) {
            Map<String, List<ProductOrderSample>> samplesById =
                    athenaClientService.findMapSampleNameToPoSample(query);
            for (List<ProductOrderSample> samples : samplesById.values()) {
                Collection<String> sampleIDs = new ArrayList<>();
                for (ProductOrderSample sample : samples) {
                    sampleIDs.add(sample.getSampleName());
                }
                Map<String, BSPSampleDTO> bspResult = bspSampleDataFetcher.fetchSamplesFromBSP(sampleIDs);
                bspSampleDataFetcher.fetchSamplePlastic(bspResult.values());
                for (ProductOrderSample sample : samples) {
                    String sampleKey = sample.getSampleName();
                    String tubeBarcode = bspResult.get(sampleKey).getBarcodeForLabVessel();
                    final ReworkCandidate candidate =
                            new ReworkCandidate(sampleKey, sample.getProductOrder().getBusinessKey(),
                                    tubeBarcode, sample.getProductOrder(), null);
                    if (!sample.getProductOrder().getProduct().isSameProductFamily(ProductFamily.ProductFamilyName.EXOME)) {
                        candidate.addValidationMessage("The PDO " + sample.getProductOrder().getBusinessKey() +
                                                       " for Sample " + sampleKey +
                                                       " is not part of the Exome family");
                    }

                    reworkCandidates.add(candidate);
                }
            }
        }

        return reworkCandidates;
    }

    /**
     * Create rework for all samples in a LabVessel.
     *
     * TODO: make this @DaoFree; can't right now because of an eventual call to persist from LabEventFactory
     *
     * @param reworkVessel     the vessel being reworked
     * @param productOrderKey  the product order that the vessel is being reworked for
     * @param reworkReason     why the rework is being done
     * @param reworkFromStep   where the rework should be reworked from
     * @param bucket           the bucket to add the rework to
     * @param comment          text describing why you are doing this
     * @param userName         the user adding the rework, in case vessels/samples need to be created on-the-fly
     *
     * @return The LabVessel instance related to the 2D Barcode given in the method call
     *
     * @throws ValidationException
     */
    public LabVessel addRework(@Nonnull LabVessel reworkVessel, @Nonnull String productOrderKey,
                               @Nonnull ReworkEntry.ReworkReason reworkReason, @Nonnull LabEventType reworkFromStep,
                               @Nonnull Bucket bucket, @Nonnull String comment, @Nonnull String userName)
            throws ValidationException {
        Collection<BucketEntry> bucketEntries = bucketEjb
                .add(Collections.singleton(reworkVessel), bucket, BucketEntry.BucketEntryType.REWORK_ENTRY, userName,
                        LabEvent.UI_EVENT_LOCATION, reworkFromStep, productOrderKey);

        // TODO: create the event in this scope instead of getting the "latest" event
        for (BucketEntry bucketEntry : bucketEntries) {
            ReworkDetail reworkDetail =
                    new ReworkDetail(reworkReason, ReworkEntry.ReworkLevel.ONE_SAMPLE_RELEASE_REST_BATCH,
                            reworkFromStep, comment, bucketEntry.getLabVessel().getLatestEvent());
            bucketEntry.setReworkDetail(reworkDetail);
        }

        return reworkVessel;
    }

    private LabVessel getReworkLabVessel(String tubeBarcode, String sampleKey, String userName) {
        LabVessel reworkVessel = labVesselDao.findByIdentifier(tubeBarcode);

        if (reworkVessel == null) {
            reworkVessel = mercuryClientEjb.createInitialVessels(Collections.singleton(sampleKey),
                    userName).iterator().next();
        }
        return reworkVessel;
    }

    /**
     * Validate and add a group of reworks to the specified bucket. This is the primary entry point for clients, e.g.
     * action beans.
     *
     *
     * @param reworkCandidates    tubes/samples/PDOs that are to be reworked
     * @param reworkReason        predefined text describing why the given vessels need to be reworked
     * @param comment             brief user comment to associate with these reworks
     * @param userName            the user adding the reworks, in case vessels/samples need to be created on-the-fly
     * @param workflowName        name of the workflow in which these vessels are to be reworked
     * @param bucketName          the name of the bucket to add reworks to
     * @return Collection of validation messages
     *
     * @throws ValidationException Thrown in the case that some checked state of any Lab Vessel will not allow the
     *                             method to continue
     */
    public Collection<String> addAndValidateReworks(@Nonnull Collection<ReworkCandidate> reworkCandidates,
                                                    @Nonnull ReworkEntry.ReworkReason reworkReason,
                                                    @Nonnull String comment, @Nonnull String userName,
                                                    @Nonnull String workflowName, @Nonnull String bucketName)
            throws ValidationException {
        Bucket bucket = bucketEjb.findOrCreateBucket(bucketName);
        Collection<String> validationMessages = new ArrayList<>();
        for (ReworkCandidate reworkCandidate : reworkCandidates) {
            validationMessages.addAll(
                    addAndValidateRework(reworkCandidate, reworkReason, bucket, comment, workflowName, userName));
        }
        return validationMessages;
    }

    /**
     * Validate and add a single rework to the specified bucket.
     *
     * @param reworkCandidate     tube/sample/PDO that is to be reworked
     * @param reworkReason        predefined text describing why the given vessel needs to be reworked
     * @param bucketName          the name of the bucket to add rework to
     * @param comment             brief user comment to associate with this rework
     * @param workflowName        name of the workflow in which this vessel is to be reworked
     * @param userName            the user adding the rework, in case vessels/samples need to be created on-the-fly
     * @return Collection of validation messages
     *
     * @throws ValidationException Thrown in the case that some checked state of the Lab Vessel will not allow the
     *                             method to continue
     */
    public Collection<String> addAndValidateRework(@Nonnull ReworkCandidate reworkCandidate,
                                                   @Nonnull ReworkEntry.ReworkReason reworkReason,
                                                   @Nonnull String bucketName,
                                                   @Nonnull String comment,
                                                   @Nonnull String workflowName,
                                                   @Nonnull String userName)
        throws ValidationException {

        Bucket bucket = bucketEjb.findOrCreateBucket(bucketName);
        return addAndValidateRework(reworkCandidate, reworkReason, bucket, comment, workflowName, userName);
    }

    /**
     * Validate and add a single rework to the specified bucket.
     *
     * @param reworkCandidate     tube/sample/PDO that is to be reworked
     * @param reworkReason        predefined Text describing why the given vessel needs to be reworked
     * @param bucket              the bucket to add rework to
     * @param comment             brief user comment to associate with this rework
     * @param workflowName        name of the workflow in which this vessel is to be reworked
     * @param userName            the user adding the rework, in case vessels/samples need to be created on-the-fly
     * @return Collection of validation messages
     *
     * @throws ValidationException Thrown in the case that some checked state of the Lab Vessel will not allow the
     *                             method to continue
     */
    private Collection<String> addAndValidateRework(@Nonnull ReworkCandidate reworkCandidate,
                                                    @Nonnull ReworkEntry.ReworkReason reworkReason,
                                                    @Nonnull Bucket bucket,
                                                    @Nonnull String comment,
                                                    @Nonnull String workflowName,
                                                    @Nonnull String userName)
            throws ValidationException {

        WorkflowBucketDef bucketDef = findWorkflowBucketDef(workflowName, bucket.getBucketDefinitionName());
        LabEventType reworkFromStep = bucketDef.getBucketEventType();

        LabVessel reworkVessel =
                getReworkLabVessel(reworkCandidate.getTubeBarcode(), reworkCandidate.getSampleKey(), userName);

        Collection<String> validationMessages =
                validateReworkItem(reworkVessel, ProductWorkflowDefVersion.findBucketDef(workflowName, reworkFromStep),
                        reworkCandidate.getProductOrderKey(), reworkCandidate.getSampleKey());

        addRework(reworkVessel, reworkCandidate.getProductOrderKey(), reworkReason, reworkFromStep, bucket, comment,
                userName);

        return validationMessages;
    }

    private WorkflowBucketDef findWorkflowBucketDef(String workflowName, String bucketName) {
        WorkflowConfig workflowConfig = workflowLoader.load();
        ProductWorkflowDefVersion workflowDefVersion = workflowConfig.getWorkflowByName(workflowName)
                .getEffectiveVersion();
        WorkflowBucketDef bucketDef = workflowDefVersion.findBucketDefByName(bucketName);
        if (bucketDef == null) {
            throw new RuntimeException("Could not find bucket definition for: " + bucketName);
        }
        return bucketDef;
    }

    /**
     * validateReworkItem will execute certain validation rules on a rework sample in order to inform a submitter of
     * any issues with the state of the LabVessel with regards to using it for Rework.
     *
     * @param reworkVessel       a LabVessel instance being submitted for rework
     * @param bucketDef          the bucket that the reworks will be added to
     * @param productOrderKey    the product order that the vessel is being reworked for
     * @param sampleKey          the sample being reworked in the vessel
     * @return Collection of validation messages
     */
    public Collection<String> validateReworkItem(@Nonnull LabVessel reworkVessel, @Nonnull WorkflowBucketDef bucketDef,
                                                 @Nonnull String productOrderKey, @Nonnull String sampleKey)
            throws ValidationException {

        List<String> validationMessages = new ArrayList<>();

        if (reworkVessel.checkCurrentBucketStatus(productOrderKey, bucketDef.getName(),
                BucketEntry.Status.Active)) {
            String error =
                    String.format("Tube %s with sample %s in product order %s already exists in the %s bucket.",
                            reworkVessel.getLabel(), sampleKey, productOrderKey, bucketDef.getName());
            logger.error(error);
            throw new ValidationException(error);
        }

        if (!reworkVessel.hasAncestorBeenInBucket(bucketDef.getName())) {
            validationMessages.add("You have submitted a vessel to the bucket that may not be considered a rework.  " +
                                   "No ancestor of " + reworkVessel.getLabel() + " has ever been in been in the " +
                                   bucketDef.getName() + " before.");
        }

        if (!bucketDef.meetsBucketCriteria(reworkVessel)) {
            validationMessages.add("You have submitted a vessel to the bucket that contains at least one sample that " +
                                   "is not DNA");
        }

        return validationMessages;
    }

    // TODO: Only called from BatchToJiraTest. Can that be modified to use a method that is used by application code?
    public void addReworkToBatch(@Nonnull LabBatch batch, @Nonnull String labVesselBarcode,
                                 @Nonnull ReworkEntry.ReworkReason reworkReason,
                                 @Nonnull LabEventType reworkFromStep, @Nonnull String comment, String workflowName,
                                 String userName)
            throws ValidationException {
        final ReworkCandidate reworkCandidate = new ReworkCandidate(labVesselBarcode);
        LabVessel reworkVessel = getReworkLabVessel(reworkCandidate.getTubeBarcode(), reworkCandidate.getSampleKey(),
                userName
        );
        batch.addReworks(Arrays.asList(reworkVessel));
    }


    public void setMercurySampleDao(MercurySampleDao mercurySampleDao) {
        this.mercurySampleDao = mercurySampleDao;
    }

    public Collection<LabVessel> getVesselsForRework(String bucketName) {
        Bucket bucket = bucketDao.findByName(bucketName);
        Set<LabVessel> vessels = new HashSet<>();
        for (BucketEntry bucketEntry : bucket.getReworkEntries()) {
            vessels.add(bucketEntry.getLabVessel());
        }
        return vessels;
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
    public static class ReworkCandidate {

        private final String tubeBarcode;
        private String sampleKey;
        private String productOrderKey;
        private ProductOrder productOrder;
        private LabVessel labVessel;
        private List<String> validationMessages = new ArrayList<>();

        /**
         * Create a rework candidate with just the tube barcode. Useful mainly in tests because, since a PDO isn't
         * specified, the tube's sample had better be in only one PDO.
         *
         * @param tubeBarcode
         */
        @Deprecated
        public ReworkCandidate(@Nonnull String tubeBarcode) {
            this.tubeBarcode = tubeBarcode;
        }

        public ReworkCandidate(@Nonnull String tubeBarcode, @Nonnull String productOrderKey) {
            this(tubeBarcode);
            this.productOrderKey = productOrderKey;
        }

        public ReworkCandidate(@Nonnull String tubeBarcode, @Nonnull String sampleKey, @Nonnull String productOrderKey) {
            this(tubeBarcode);
            this.sampleKey = sampleKey;
            this.productOrderKey = productOrderKey;
        }

        public ReworkCandidate(@Nonnull String sampleKey, @Nonnull String productOrderKey, @Nonnull String tubeBarcode,
                               ProductOrder productOrder, LabVessel labVessel) {
            this(tubeBarcode, sampleKey, productOrderKey);
            this.productOrder = productOrder;
            this.labVessel = labVessel;
        }

        public String getSampleKey() {
            return sampleKey;
        }

        public String getProductOrderKey() {
            return productOrderKey;
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

        @Override
        public String toString() {
            return String.format("%s|%s|%s", tubeBarcode, sampleKey, productOrderKey);
        }

        public static ReworkCandidate fromString(String s) {
            String[] parts = s.split("\\|");
            String tubeBarcode = parts[0];
            String sampleKey = parts[1];
            String productOrderKey = parts[2];
            return new ReworkCandidate(tubeBarcode, sampleKey, productOrderKey);
        }
    }
}
