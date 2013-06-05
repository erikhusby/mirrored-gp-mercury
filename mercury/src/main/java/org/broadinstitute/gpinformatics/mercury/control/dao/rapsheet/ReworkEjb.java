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
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUtil;
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
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

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
    BucketEntryDao bucketEntryDao;

    @Inject
    MercurySampleDao mercurySampleDao;

    @Inject
    ReworkEntryDao reworkEntryDao;

    @Inject
    LabVesselDao labVesselDao;

    @Inject
    private ProductOrderSampleDao productOrderSampleDao;

    @Inject
    private AthenaClientService athenaClientService;

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
    public Collection<MercurySample> getVesselRapSheet(@Nonnull LabVessel labVessel, ReworkEntry.ReworkReason reworkReason,
                                                       ReworkEntry.ReworkLevel reworkLevel, @Nonnull LabEventType reworkFromStep,
                                                       @Nonnull String comment)
            throws ValidationException {
        Set<MercurySample> reworks = new HashSet<MercurySample>();
        VesselPosition[] vesselPositions=labVessel.getVesselGeometry().getVesselPositions();
        if (vesselPositions==null){
            vesselPositions = new VesselPosition[]{VesselPosition.TUBE1};
        }
        for (VesselPosition vesselPosition : vesselPositions) {
            final Collection<SampleInstance> samplesAtPosition =
                    labVessel.getSamplesAtPosition(vesselPosition.name());
            for (SampleInstance sampleInstance : samplesAtPosition) {
                MercurySample mercurySample = sampleInstance.getStartingSample();
                BucketEntry bucketEntry =
                        bucketEntryDao.findByVesselAndPO(labVessel, sampleInstance.getProductOrderKey());

                if (bucketEntry != null) {
                    if (bucketEntry.getStatus().equals(BucketEntry.Status.Active)) {
                        String error =
                                String.format("Sample %s in product order %s already exists in the %s bucket.",
                                        mercurySample.getSampleKey(), sampleInstance.getProductOrderKey(),
                                        bucketEntry.getBucket().getBucketDefinitionName());
                        logger.error(error);
                        throw new ValidationException(error);
                    }
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
     * @return a sample with ReworkEntry added
     */
    @DaoFree
    public void getReworkEntryDaoFree(@Nonnull MercurySample mercurySample, @Nonnull LabVessel labVessel,
                                               @Nonnull VesselPosition vesselPosition,
                                               @Nonnull ReworkEntry.ReworkReason reworkReason,
                                               ReworkEntry.ReworkLevel reworkLevel, @Nonnull LabEventType reworkFromStep,
                                               String comment) {
        LabVesselPosition labVesselPosition = new LabVesselPosition(vesselPosition, mercurySample);
        LabVesselComment<ReworkEntry> reworkComment =
                new LabVesselComment<ReworkEntry>(labVessel.getLatestEvent(), labVessel, comment);
        ReworkEntry reworkEntry = new ReworkEntry(labVesselPosition, reworkComment, reworkReason,
                reworkLevel, reworkFromStep);
        mercurySample.getRapSheet().addRework(reworkEntry);
    }

    /**
     * Searches for and returns candidate vessels and samples that can be used for "rework". All candidates must at
     * least have a single sample, a tube barcode, and a PDO. Multiple results for the same sample may be returned if
     * the sample is included in multiple PDOs.
     *
     * @param query    tube barcode or sample ID to search for
     * @return tube/sample/PDO selections that are valid for rework
     */
    public Collection<ReworkCandidate> findReworkCandidates(String query) {
        Collection<ReworkCandidate> reworkCandidates = new ArrayList<>();

        List<LabVessel> labVessels = new ArrayList<>();
        if (BSPUtil.isInBspFormat(query)) {
            labVessels.addAll(labVesselDao.findBySampleKey(query));
        } else {
            LabVessel labVessel = labVesselDao.findByIdentifier(query);
            if (labVessel != null) {
                labVessels.add(labVessel);
            }
        }

        // TODO: handle the case where LIMS doesn't know the tube, but we can still look it up in BSP
//        List<ProductOrderSample> samples =
//                productOrderSampleDao.findMapBySamples(Collections.singletonList(query)).get(query);

        for (LabVessel vessel : labVessels) {
            Set<SampleInstance> sampleInstances = vessel.getSampleInstances(WITH_PDO, null);

            if (sampleInstances.size() != 1) {
                // per Zimmer, ignore tubes that are pools when querying by sample
                // TODO: report a warning?
            } else {
                SampleInstance sampleInstance = sampleInstances.iterator().next();
                String sampleKey = sampleInstance.getStartingSample().getSampleKey();
                String productOrderKey = sampleInstance.getProductOrderKey();

                List<ProductOrderSample> productOrderSamples =
                        productOrderSampleDao.findMapBySamples(Collections.singletonList(sampleKey)).get(sampleKey);

                // make sure we have a matching product order sample
                for (ProductOrderSample sample : productOrderSamples) {
                    if (sample.getProductOrder().getBusinessKey().equals(productOrderKey)) {
                        ProductOrder productOrder = athenaClientService.retrieveProductOrderDetails(productOrderKey);
                        reworkCandidates.add(new ReworkCandidate(sampleKey, productOrderKey, vessel.getLabel(),
                                productOrder, vessel));
                    }
                }
            }
        }

        return reworkCandidates;
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
    public void addRework(@Nonnull LabVessel labVessel, @Nonnull ReworkEntry.ReworkReason reworkReason,
                               @Nonnull LabEventType reworkFromStep, @Nonnull String comment)
            throws ValidationException {
        List<MercurySample> reworks = new ArrayList<MercurySample>(
                getVesselRapSheet(labVessel, reworkReason, ReworkEntry.ReworkLevel.ONE_SAMPLE_RELEASE_REST_BATCH, reworkFromStep,
                        comment));

        if (!reworks.isEmpty()) {
            mercurySampleDao.persistAll(reworks);
        }
    }

    public void addReworkToBatch(@Nonnull LabBatch batch, @Nonnull LabVessel labVessel,
                                     @Nonnull ReworkEntry.ReworkReason reworkReason,
                                     @Nonnull LabEventType reworkFromStep, @Nonnull String comment)
            throws ValidationException {
        addRework(labVessel, reworkReason, reworkFromStep, comment);
        batch.addReworks(Arrays.asList(labVessel));
    }


    public void setBucketEntryDao(BucketEntryDao bucketEntryDao) {
        this.bucketEntryDao = bucketEntryDao;
    }

    public void setMercurySampleDao(MercurySampleDao mercurySampleDao) {
        this.mercurySampleDao = mercurySampleDao;
    }

    public Collection<LabVessel> getVesselsForRework(){
        Set<LabVessel> inactiveVessels=new HashSet<LabVessel>();
        for (ReworkEntry rework : reworkEntryDao.getActive()) {
            inactiveVessels.add(rework.getLabVesselComment().getLabVessel());
        }
        return inactiveVessels;
    }

    /**
     * A candidate vessel, based on a search/lookup, that a user may choose to "rework". The vessel may have already
     * gone through a workflow, or may be a sample that is in a PDO but needs to be run through a different process than
     * its PDO would normally call for.
     *
     * This represents the selection of a LabVessel and ProductOrder combination, or the case where there is only a
     * sample and a ProductOrder because LIMS hasn't yet seen the tube containing the sample.
     *
     * TODO: remove redundant fields and object references once non-vessel sample case is implemented
     */
    public static class ReworkCandidate {
        private String sampleKey;
        private String productOrderKey;
        private String tubeBarcode;
        private ProductOrder productOrder;
        private LabVessel labVessel;

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
    }
}
