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
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.LabVesselComment;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.LabVesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.RapSheet;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkEntry;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkLevel;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkReason;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Encapsulates the business logic related to {@link RapSheet}s. and Rework This includes the creation
 * of a new batch entity and saving that to Jira
 */
@Stateful
@RequestScoped
public class ReworkEjb {
    private final static Log logger = LogFactory.getLog(ReworkEjb.class);

    @Inject
    BucketEntryDao bucketEntryDao;

    @Inject
    MercurySampleDao mercurySampleDao;

    @Inject
    ReworkEntryDao reworkEntryDao;

    @Inject
    LabVesselDao labVesselDao;
    /**
     * Create rework for all samples in a LabVessel;
     *
     * @param labVessel      the labVessel where the sample is located.
     * @param reworkReason   Why is the rework being done.
     * @param reworkLevel    What level to rework to
     * @param reworkFromStep Where should the rework be reworked from.
     * @param comment        text describing why you are doing this.
     *
     * @throws InformaticsServiceException
     */
    public Collection<MercurySample> getVesselRapSheet(@NotNull LabVessel labVessel, ReworkReason reworkReason,
                                                       ReworkLevel reworkLevel, @NotNull LabEventType reworkFromStep,
                                                       @NotNull String comment)
            throws ValidationException {
        Set<MercurySample> reworks = new HashSet<MercurySample>();

        for (VesselContainer vesselContainer : labVessel.getContainerList()) {
            for (VesselPosition vesselPosition : vesselContainer.getEmbedder().getVesselGeometry()
                    .getVesselPositions()) {
                final Collection<SampleInstance> samplesAtPosition =
                        labVessel.getSamplesAtPosition(vesselPosition.name());

                for (SampleInstance sampleInstance : samplesAtPosition) {
                    MercurySample mercurySample = sampleInstance.getStartingSample();
                    final BucketEntry bucketEntry =
                            bucketEntryDao.findByVesselAndPO(labVessel, mercurySample.getProductOrderKey());

                    if (bucketEntry != null) {
                        String error = String.format("Sample %s in product order %s already exists in the %s bucket.",
                                mercurySample.getSampleKey(), mercurySample.getProductOrderKey(),
                                bucketEntry.getBucket().getBucketDefinitionName());
                        logger.error(error);
                        throw new ValidationException(error);
                    }

                    mercurySample = getReworkEntryDaoFree(mercurySample, labVessel, vesselPosition,
                            reworkReason, reworkLevel, reworkFromStep, comment);

                    reworks.add(mercurySample);
                }
            }
        }
        return reworks;
    }

    /**
     * Create a @link ReworkEntry for one Sample
     *
     * @param mercurySample  the sample to be reworked.
     * @param labVessel      the labVessel where the sample is located.
     * @param vesselPosition Where on the vessel the sample is located.
     * @param reworkReason   Why is the rework being done.
     * @param reworkLevel    What level to rework to
     * @param reworkFromStep Where should the rework be reworked from.
     * @param comment        text describing why you are doing this.
     *
     * @return a sample with ReworkEntry added
     */
    @DaoFree
    public MercurySample getReworkEntryDaoFree(@NotNull MercurySample mercurySample, @NotNull LabVessel labVessel,
                                               @NotNull VesselPosition vesselPosition,
                                               @NotNull ReworkReason reworkReason,
                                               ReworkLevel reworkLevel, @NotNull LabEventType reworkFromStep,
                                               String comment) {
        LabVesselPosition labVesselPosition = new LabVesselPosition(vesselPosition, mercurySample);
        LabVesselComment<ReworkEntry> reworkComment =
                new LabVesselComment<ReworkEntry>(labVessel.getLatestEvent(), labVessel, comment);
        ReworkEntry reworkEntry = new ReworkEntry(labVesselPosition, reworkComment, reworkReason,
                reworkLevel, reworkFromStep);
        mercurySample.getRapSheet().addRework(reworkEntry);
        return mercurySample;
    }

    /**
     * Create rework for all samples in a LabVessel;
     *
     * @param labVessel      any type of LabVessel, with samples in it.
     * @param reworkReason   Why is the rework being done.
     * @param reworkFromStep Where should the rework be reworked from.
     * @param comment        text describing why you are doing this.
     *
     * @throws ValidationException
     */
    public List<MercurySample> addRework(@NotNull LabVessel labVessel, @NotNull ReworkReason reworkReason,
                                               @NotNull LabEventType reworkFromStep, @NotNull String comment)
            throws ValidationException {
        LabVessel v = labVesselDao.findByIdentifier(labVessel.getLabel());
        List<MercurySample> reworks =
                new ArrayList<MercurySample>(getVesselRapSheet(v, reworkReason,
                        ReworkLevel.ONE_SAMPLE_RELEASE_REST_BATCH, reworkFromStep, comment));
        if (!reworks.isEmpty()) {
            mercurySampleDao.persistAll(reworks);
        }
        return reworks;
    }

    public void setBucketEntryDao(BucketEntryDao bucketEntryDao) {
        this.bucketEntryDao = bucketEntryDao;
    }

    public void setMercurySampleDao(MercurySampleDao mercurySampleDao) {
        this.mercurySampleDao = mercurySampleDao;
    }


    public void startRework(@NotNull LabVessel labVessel) {
        for (MercurySample mercurySample : labVessel.getMercurySamples()) {
            mercurySample.getRapSheet().startRework();
        }
    }

    public void stopRework(@NotNull LabVessel labVessel) {
        for (MercurySample mercurySample : labVessel.getMercurySamples()) {
            mercurySample.getRapSheet().stopRework();
        }
    }


    public Collection<ReworkEntry> getNonActiveReworkEntries() {
        return reworkEntryDao.getNonActiveReworkEntries();
    }
}
