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
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Encapsulates the business logic related to {@link RapSheet}s. and Rework This includes the creation
 * of a new batch entity and saving that to Jira
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

    MercurySampleDao mercurySampleDao;

    ReworkEntryDao reworkEntryDao;

    LabVesselDao labVesselDao;

    public ReworkEjb() {
    }

    @Inject
    public ReworkEjb(MercurySampleDao mercurySampleDao,
                     ReworkEntryDao reworkEntryDao,
                     LabVesselDao labVesselDao) {
        this.mercurySampleDao = mercurySampleDao;
        this.reworkEntryDao = reworkEntryDao;
        this.labVesselDao = labVesselDao;
    }

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

        WorkflowBucketDef bucketDef = LabEventHandler
                .findBucketDef(workflowName, reworkFromStep);

        for (VesselPosition vesselPosition : vesselPositions) {
            final Collection<SampleInstance> samplesAtPosition =
                    labVessel.getSamplesAtPosition(vesselPosition.name());
            for (SampleInstance sampleInstance : samplesAtPosition) {
                MercurySample mercurySample = sampleInstance.getStartingSample();


                //TODO SGM Revisit.  Ensure that this is truly what we wish to do.
                if (labVessel
                        .isAncestorInBucket(sampleInstance.getProductOrderKey(), bucketDef.getName())) {
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
     *
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
     * {@link #addRework(String, org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkEntry.ReworkReason,
     * org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType, String, String)}, create a
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

        WorkflowBucketDef bucketDef = LabEventHandler
                .findBucketDef(workflowName, reworkStep);

        if (Boolean.FALSE.equals(reworkVessel.hasAncestorBeenInBucket(bucketDef.getName()))) {
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
}
