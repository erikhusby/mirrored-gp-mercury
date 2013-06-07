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
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
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
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;

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

import static org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel.SampleType.WITH_PDO;

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

    /**
     * Create rework for all samples in a LabVessel;
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

        WorkflowBucketDef bucketDef = LabEventHandler.findBucketDef(workflowName, reworkFromStep);

        for (VesselPosition vesselPosition : vesselPositions) {
            final Collection<SampleInstance> samplesAtPosition =
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
        Collection<ReworkCandidate> reworkCandidates = new ArrayList<>();

        List<LabVessel> labVessels = new ArrayList<>();
        LabVessel labVessel = labVesselDao.findByIdentifier(query);
        if (labVessel != null) {
            labVessels.add(labVessel);
        }
        labVessels.addAll(labVesselDao.findBySampleKey(query));

        for (LabVessel vessel : labVessels) {
            Set<SampleInstance> sampleInstances = vessel.getSampleInstances(WITH_PDO, null);

            if (sampleInstances.size() != 1) {
                // per Zimmer, ignore tubes that are pools when querying by sample
                // TODO: report a warning?
            } else {
                SampleInstance sampleInstance = sampleInstances.iterator().next();
                String sampleKey = sampleInstance.getStartingSample().getSampleKey();
                String productOrderKey = sampleInstance.getProductOrderKey();

                // TODO: fetch for all vessels in a single call and make looping over labVessels a @DaoFree method
                List<ProductOrderSample> productOrderSamples =
                        athenaClientService.findMapSampleNameToPoSample(Collections.singletonList(sampleKey))
                                .get(sampleKey);

                // make sure we have a matching product order sample
                for (ProductOrderSample sample : productOrderSamples) {
                    if (sample.getProductOrder().getBusinessKey().equals(productOrderKey)) {

                        ProductOrder productOrder = athenaClientService.retrieveProductOrderDetails(productOrderKey);
                        ReworkCandidate candidate = new ReworkCandidate(sampleKey, productOrderKey, vessel.getLabel(),
                                productOrder, vessel);

                        if (!ProductFamily.ProductFamilyName.EXOME.getFamilyName()
                                .equals(productOrder.getProduct().getProductFamily().getName())) {
                            candidate.addValidationMessage("The PDO " + productOrder.getBusinessKey() +
                                                           " for Sample "  + sampleKey +
                                                           " is not part of the Exome family");
                        }

                        reworkCandidates.add(candidate);
                    }
                }
            }
        }

        if (reworkCandidates.isEmpty()) {
            List<ProductOrderSample> samples = athenaClientService.findMapSampleNameToPoSample(
                    Collections.singletonList(query)).get(query);

            Collection<String> sampleIDs = new ArrayList<>();
            for (ProductOrderSample sample : samples) {
                sampleIDs.add(sample.getSampleName());
            }
            Map<String, BSPSampleDTO> bspResult = bspSampleDataFetcher.fetchSamplesFromBSP(sampleIDs);
            bspSampleDataFetcher.fetchSamplePlastic(bspResult.values());
            for (ProductOrderSample sample : samples) {
                String sampleKey = sample.getSampleName();
                String tubeBarcode = bspResult.get(sampleKey).getBarcodeForLabVessel();
                reworkCandidates.add(new ReworkCandidate(sampleKey, sample.getProductOrder().getBusinessKey(),
                        tubeBarcode, sample.getProductOrder(), null));
            }
        }

        return reworkCandidates;
    }

    /**
     * Create rework for all samples in a LabVessel;
     *
     * @param labVesselBarcode 2D Barcode of any type of LabVessel, with samples in it.
     * @param reworkReason     Why is the rework being done.
     * @param reworkFromStep   Where should the rework be reworked from.
     * @param comment          text describing why you are doing this.
     * @param workflowName     Name of the workflow in which this vessel is to be reworked
     *
     * @return The LabVessel instance related to the 2D Barcode given in the method call
     *
     * @throws ValidationException
     */
    public LabVessel addRework(@Nonnull String labVesselBarcode, @Nonnull ReworkEntry.ReworkReason reworkReason,
                               @Nonnull LabEventType reworkFromStep, @Nonnull String comment,
                               @Nonnull String workflowName)
            throws ValidationException {

        LabVessel reworkVessel = labVesselDao.findByIdentifier(labVesselBarcode);

        List<MercurySample> reworks = new ArrayList<>(
                getVesselRapSheet(reworkVessel, reworkReason, ReworkEntry.ReworkLevel.ONE_SAMPLE_RELEASE_REST_BATCH,
                        reworkFromStep,
                        comment, workflowName));

        if (!reworks.isEmpty()) {
            mercurySampleDao.persistAll(reworks);
        }

        return reworkVessel;
    }

    /**
     * addAndValidateRework will, like
     * {@link #addRework(String, org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkEntry.ReworkReason, org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType, String, String)}, create a
     * {@link ReworkEntry} for all samples in a vessel.
     * <p/>
     * In addition to creating a ReworkEntry, this method will execute some validation rules against the rework entry
     * for the sole purpose of informing the user of the state of the vessel that they have just submitted for rework.
     *
     * @param reworkVesselBarcode 2D Barcode of a vessel to be reworked.
     * @param reworkReason        predefined Text describing why the given vessel needs to be reworked
     * @param reworkFromStep      Step in the workflow at which rework is to begin
     * @param comment             Brief user comment to associate with this rework.
     * @param workflowName        Name of the workflow in which this vessel is to be reworked
     *
     * @return Collection of validation messages
     *
     * @throws ValidationException Thrown in the case that some checked state of the Lab Vessel will not allow the
     *                             method to continue
     */
    public Collection<String> addAndValidateRework(@Nonnull String reworkVesselBarcode,
                                                   @Nonnull ReworkEntry.ReworkReason reworkReason,
                                                   @Nonnull LabEventType reworkFromStep, @Nonnull String comment,
                                                   @Nonnull String workflowName)
            throws ValidationException {

        LabVessel reworkVessel = addRework(reworkVesselBarcode, reworkReason, reworkFromStep, comment, workflowName);

        return validateReworkItem(reworkVessel, reworkFromStep, workflowName);
    }

    /**
     * validateReworkItem will execute certain validation rules on a rework sample in order to inform a submitter of
     * any issues with the state of the LabVessel with regards to using it for Rework.
     *
     * @param reworkVessel a LabVessel instance being submitted for rework
     * @param reworkStep   Step in the workflow at which rework is to begin
     * @param workflowName Name of the workflow in which this vessel is to be reworked
     *
     * @return Collection of validation messages
     */
    public Collection<String> validateReworkItem(@Nonnull LabVessel reworkVessel, @Nonnull LabEventType reworkStep,
                                                 @Nonnull String workflowName) {
        List<String> validationMessages = new ArrayList<>();

        WorkflowBucketDef bucketDef = LabEventHandler.findBucketDef(workflowName, reworkStep);

        if (!reworkVessel.hasAncestorBeenInBucket(bucketDef.getName())) {
            validationMessages.add("You have submitted a vessel to the bucket that may not be considered a rework.  " +
                                   "No ancestor of " + reworkVessel.getLabel() + " has ever been in been in the " +
                                   bucketDef.getName() + " before.");
        }

        if (!bucketDef.meetsBucketCriteria(reworkVessel)) {
            validationMessages.add("You have submitted a vessel to the bucket that contains at least one sample that " +
                                   "is not Genomic DNA");
        }

        return validationMessages;
    }

    public void addReworkToBatch(@Nonnull LabBatch batch, @Nonnull String labVesselBarcode,
                                 @Nonnull ReworkEntry.ReworkReason reworkReason,
                                 @Nonnull LabEventType reworkFromStep, @Nonnull String comment, String workflowName)
            throws ValidationException {
        LabVessel reworkVessel = addRework(labVesselBarcode, reworkReason, reworkFromStep, comment, workflowName);
        batch.addReworks(Arrays.asList(reworkVessel));
    }


    public void setMercurySampleDao(MercurySampleDao mercurySampleDao) {
        this.mercurySampleDao = mercurySampleDao;
    }

    public Collection<LabVessel> getVesselsForRework() {
        Set<LabVessel> inactiveVessels = new HashSet<>();
        for (ReworkEntry rework : reworkEntryDao.getActive()) {
            inactiveVessels.add(rework.getLabVesselComment().getLabVessel());
        }
        return inactiveVessels;
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
        private String sampleKey;
        private String productOrderKey;
        private String tubeBarcode;
        private ProductOrder productOrder;
        private LabVessel labVessel;
        private List<String> validationMessages = new ArrayList<>();

        public ReworkCandidate(@Nonnull String sampleKey, @Nonnull String productOrderKey, @Nonnull String tubeBarcode,
                               ProductOrder productOrder, LabVessel labVessel) {
            this.sampleKey = sampleKey;
            this.productOrderKey = productOrderKey;
            this.tubeBarcode = tubeBarcode;
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
    }
}
