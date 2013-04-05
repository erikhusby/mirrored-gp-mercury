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
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.*;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
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
public class RapSheetEjb {
    private final static Log logger = LogFactory.getLog(RapSheetEjb.class);

    @Inject
    BucketEntryDao bucketEntryDao;

    @Inject
    MercurySampleDao mercurySampleDao;

    public void setBucketEntryDao(BucketEntryDao bucketEntryDao) {
        this.bucketEntryDao = bucketEntryDao;
    }

    public void setMercurySampleDao(MercurySampleDao mercurySampleDao) {
        this.mercurySampleDao = mercurySampleDao;
    }

    /**
     * Create rework for all samples in a LabVessel;
     *
     * @param labVessel      the labVessel where the sample is located.
     * @param reworkReason   Why is the rework being done.
     * @param reworkFromStep Where should the rework be reworked from.
     * @param comment        text describing why you are doing this.
     *
     * @throws InformaticsServiceException
     */
    public Collection<MercurySample> getVesselRapSheet(@NotNull LabVessel labVessel, ReworkReason reworkReason,
                                                       @NotNull LabEventType reworkFromStep,
                                                       @NotNull String comment)
            throws InformaticsServiceException {
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
                        throw new InformaticsServiceException(error);
                    }
                    /*
                     * IF this is a rework, get a ReworkEntry, otherwise get a RapSheetEntry
                     */
                    if (reworkReason == null && reworkFromStep == null) {
                        mercurySample = getRapSheetEntryDaoFree(mercurySample, labVessel, vesselPosition, comment);
                    } else {
                        mercurySample =
                                getReworkEntryDaoFree(mercurySample, labVessel, vesselPosition, reworkReason,
                                        reworkFromStep,
                                        comment);
                    }
                    reworks.add(mercurySample);
                }
            }
        }
        return reworks;
    }


    /**
     * Create rework for all samples in a LabVessel;
     *
     * @param labVessel any type of LabVessel, with samples in it.
     * @param comment   text describing why you are doing this.
     *
     * @throws InformaticsServiceException
     */
    public Collection<MercurySample> addRapSheet(@NotNull LabVessel labVessel, @NotNull String comment)
            throws InformaticsServiceException {
        Collection<MercurySample> reworks = getVesselRapSheet(labVessel, null, null, comment);
        if (!reworks.isEmpty()) {
            mercurySampleDao.persistAll((List<MercurySample>) reworks);
        }
        return reworks;
    }

    /**
     * Create rework for all samples in a LabVessel;
     *
     * @param labVessel      any type of LabVessel, with samples in it.
     * @param reworkReason   Why is the rework being done.
     * @param reworkFromStep Where should the rework be reworked from.
     * @param comment        text describing why you are doing this.
     *
     * @throws InformaticsServiceException
     */
    public Collection<MercurySample> addRework(@NotNull LabVessel labVessel, @NotNull ReworkReason reworkReason,
                                               @NotNull LabEventType reworkFromStep, @NotNull String comment)
            throws InformaticsServiceException {
        Collection<MercurySample> reworks = getVesselRapSheet(labVessel, reworkReason, reworkFromStep, comment);
        if (!reworks.isEmpty()) {
            mercurySampleDao.persistAll((List<MercurySample>) reworks);
        }
        return reworks;
    }


    /**
     * Create a @link RapSheet for one Sample
     *
     * @param mercurySample  the sample to be reworked.
     * @param labVessel      the labVessel where the sample is located.
     * @param vesselPosition Where on the vessel the sample is located.
     * @param comment        text describing why you are doing this.
     *
     * @return a sample with RapSheetEntry added
     */
    @DaoFree
    public MercurySample getRapSheetEntryDaoFree(@NotNull MercurySample mercurySample, @NotNull LabVessel labVessel,
                                                  @NotNull VesselPosition vesselPosition,
                                                  String comment) {
        LabVesselPosition labVesselPosition = new LabVesselPosition(vesselPosition, mercurySample);
        LabVesselComment<RapSheetEntry> rapSheetComment =
                new LabVesselComment<RapSheetEntry>(labVessel.getLatestEvent(), labVessel, comment);
        final RapSheetEntry rapSheetEntry = new RapSheetEntry(labVesselPosition, rapSheetComment);
        mercurySample.getRapSheet().addEntry(rapSheetEntry);
        return mercurySample;
    }

    /**
     * Create a @link ReworkEntry for one Sample
     *
     * @param mercurySample  the sample to be reworked.
     * @param labVessel      the labVessel where the sample is located.
     * @param vesselPosition Where on the vessel the sample is located.
     * @param reworkReason   Why is the rework being done.
     * @param reworkFromStep Where should the rework be reworked from.
     * @param comment        text describing why you are doing this.
     *
     * @return a sample with ReworkEntry added
     */
    @DaoFree
    public MercurySample getReworkEntryDaoFree(@NotNull MercurySample mercurySample, @NotNull LabVessel labVessel,
                                                @NotNull VesselPosition vesselPosition,
                                                @NotNull ReworkReason reworkReason,
                                                @NotNull LabEventType reworkFromStep, String comment) {
        LabVesselPosition labVesselPosition = new LabVesselPosition(vesselPosition, mercurySample);
        LabVesselComment<ReworkEntry> reworkComment =
                new LabVesselComment<ReworkEntry>(labVessel.getLatestEvent(), labVessel, comment);
        final ReworkEntry reworkEntry = new ReworkEntry(labVesselPosition, reworkComment, reworkReason,
                ReworkLevel.ONE_SAMPLE_RELEASE_REST_BATCH, reworkFromStep);
        mercurySample.getRapSheet().addEntry(reworkEntry);
        return mercurySample;
    }
}
