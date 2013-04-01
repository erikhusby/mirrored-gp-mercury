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
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
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
import java.util.Set;


@Stateful
@RequestScoped
public class ReworkEjb {
    private final static Log logger = LogFactory.getLog(ReworkEjb.class);

    @Inject
    BucketEntryDao bucketEntryDao;

    @Inject
    MercurySampleDao mercurySampleDao;

    /**
     * Create rework for all samples in a LabVessel;
     *
     * @param labVessel
     * @param reworkReason
     * @param reworkFromStep
     * @param reworkComment
     *
     * @throws InformaticsServiceException
     */
    public Collection<MercurySample> addReworks(@NotNull LabVessel labVessel, @NotNull ReworkReason reworkReason,
                                                @NotNull LabEventType reworkFromStep, String reworkComment)
            throws InformaticsServiceException {
        Set<MercurySample> reworks = new HashSet<MercurySample>();

        for (VesselContainer vesselContainer : labVessel.getContainerList()) {
            for (VesselPosition vesselPosition : vesselContainer.getEmbedder().getVesselGeometry()
                    .getVesselPositions()) {
                final Collection<SampleInstance> samplesAtPosition =
                        labVessel.getSamplesAtPosition(vesselPosition.name());

                for (SampleInstance sampleInstance : samplesAtPosition) {
                    final MercurySample mercurySample = sampleInstance.getStartingSample();
                    final BucketEntry bucketEntry =
                            bucketEntryDao.findByVesselAndPO(labVessel, mercurySample.getProductOrderKey());

                    if (bucketEntry != null) {
                        String error = String.format("Sample %s in product order %s already exists in the %s bucket.",
                                mercurySample.getSampleKey(), mercurySample.getProductOrderKey(),
                                bucketEntry.getBucket().getBucketDefinitionName());
                        logger.error(error);
                        throw new InformaticsServiceException(error);
                    }
                    mercurySample.reworkSample(
                            reworkReason, ReworkLevel.ONE_SAMPLE_RELEASE_REST_BATCH,
                            labVessel.getLatestEvent(),
                            reworkFromStep, labVessel, vesselPosition, reworkComment
                    );
                    reworks.add(mercurySample);
                }
            }
        }

        final ArrayList<MercurySample> reworkList = new ArrayList<MercurySample>(reworks);
        if (!reworks.isEmpty()) {
            mercurySampleDao.persistAll(reworkList);
        }
        return reworkList;
    }
}
