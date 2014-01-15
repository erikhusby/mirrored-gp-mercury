package org.broadinstitute.gpinformatics.infrastructure.mercury;

import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.LabVesselFactory;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Stateful
@RequestScoped
public class MercuryClientEjb {
    private LabVesselDao labVesselDao;
    private BSPUserList userList;
    private BucketEjb bucketEjb;
    private WorkflowLoader workflowLoader;
    private BucketDao bucketDao;
    private BSPSampleDataFetcher bspSampleDataFetcher;
    private LabVesselFactory labVesselFactory;

    public MercuryClientEjb() {}

    @Inject
    public MercuryClientEjb(BucketEjb bucketEjb, BucketDao bucketDao,
                            WorkflowLoader workflowLoader, BSPUserList userList,
                            LabVesselDao labVesselDao, BSPSampleDataFetcher bspSampleDataFetcher,
                            LabVesselFactory labVesselFactory) {
        this.bucketEjb = bucketEjb;
        this.bucketDao = bucketDao;
        this.workflowLoader = workflowLoader;
        this.userList = userList;
        this.labVesselDao = labVesselDao;
        this.bspSampleDataFetcher = bspSampleDataFetcher;
        this.labVesselFactory = labVesselFactory;
    }

    public Collection<ProductOrderSample> addFromProductOrder(ProductOrder pdo) {
        return addFromProductOrder(pdo, pdo.getSamples());
    }

    /**
     * Puts product order samples into the appropriate bucket.  Does nothing if the product is not supported in Mercury.
     *
     * @return the samples that were actually added to the bucket
     */
    public Collection<ProductOrderSample> addFromProductOrder(ProductOrder order,
                                                              Collection<ProductOrderSample> samples) {

        Workflow workflow = order.getProduct() != null ? order.getProduct().getWorkflow() : null;
        if (!Workflow.SUPPORTED_WORKFLOWS.contains(workflow)) {
            return Collections.emptyList();
        }

        WorkflowConfig workflowConfig = workflowLoader.load();
        ProductWorkflowDefVersion workflowDefVersion =
                workflowConfig.getWorkflow(order.getProduct().getWorkflow()).getEffectiveVersion();
        WorkflowBucketDef initialBucketDef = workflowDefVersion.getInitialBucket();

        Bucket initialBucket = null;
        if (initialBucketDef != null) {
            initialBucket = bucketEjb.findOrCreateBucket(initialBucketDef.getName());
        }
        if (initialBucket == null) {
            return Collections.emptyList();
        }

        String username = null;
        Long bspUserId = order.getCreatedBy();
        if (bspUserId != null) {
            BspUser bspUser = userList.getById(bspUserId);
            if (bspUser != null) {
                username = bspUser.getUsername();
            }
        }

        // Finds existing vessels for the pdo samples, or if none, then either the sample has not been received
        // or the sample is a derived stock that has not been seen by Mercury.  Creates both standalone vessel and
        // MercurySample for the latter.
        Map<String, Collection<ProductOrderSample>> nameToSampleMap = new HashMap<>();
        Set<String> sampleKeyList = new HashSet<>();
        for (ProductOrderSample pdoSample : samples) {
            // A pdo can have multiple samples all with same sample name but with different sample position.
            // Each one will get a MercurySample and LabVessel put into the bucket.
            Collection<ProductOrderSample> pdoSampleList = nameToSampleMap.get(pdoSample.getName());
            if (pdoSampleList == null) {
                pdoSampleList = new ArrayList<>();
                nameToSampleMap.put(pdoSample.getName(), pdoSampleList);
            }
            pdoSampleList.add(pdoSample);
            sampleKeyList.add(pdoSample.getName());
        }

        List<LabVessel> vessels = labVesselDao.findBySampleKeyList(sampleKeyList);

        // Finds samples with no existing vessels.
        Collection<String> samplesWithoutVessel = new ArrayList<>(sampleKeyList);
        for (LabVessel vessel : vessels) {
            for (MercurySample sample : vessel.getMercurySamples()) {
                samplesWithoutVessel.remove(sample.getSampleKey());
            }
        }

        if (!CollectionUtils.isEmpty(samplesWithoutVessel)) {
            vessels.addAll(createInitialVessels(samplesWithoutVessel, username));
        }

        Collection<LabVessel> validVessels = applyBucketCriteria(vessels, initialBucketDef);

        bucketEjb.add(validVessels, initialBucket, BucketEntry.BucketEntryType.PDO_ENTRY, username,
                LabEvent.UI_EVENT_LOCATION, LabEvent.UI_PROGRAM_NAME, initialBucketDef.getBucketEventType(),
                order.getBusinessKey()
        );

        if (initialBucket.getBucketId() == null) {
            bucketDao.persist(initialBucket);
        }

        List<ProductOrderSample> samplesAdded = new ArrayList<>();
        for (LabVessel vessel : validVessels) {
            for (MercurySample sample : vessel.getMercurySamples()) {
                samplesAdded.addAll(nameToSampleMap.get(sample.getSampleKey()));
            }
        }
        return samplesAdded;
    }

    // todo jmt should this check be in bucketEjb.add?
    private Collection<LabVessel> applyBucketCriteria(Collection<LabVessel> vessels, WorkflowBucketDef bucketDef) {
        Collection<LabVessel> validVessels = new HashSet<>();
        for (LabVessel vessel : vessels) {
            if (bucketDef.meetsBucketCriteria(vessel)) {
                validVessels.add(vessel);
            }
        }
        return validVessels;
    }

    /**
     * Creates LabVessels to mirror BSP receptacles based on the given BSP sample IDs.
     *
     * @param samplesWithoutVessel    BSP sample IDs that need LabVessels
     * @param username                the user performing the operation leading to the LabVessels being created
     * @return the created LabVessels
     */
    public Collection<LabVessel> createInitialVessels(Collection<String>samplesWithoutVessel, String username) {
        Collection<LabVessel> vessels = new ArrayList<>();
        Map<String, BSPSampleDTO> bspDtoMap = bspSampleDataFetcher.fetchSamplesFromBSP(samplesWithoutVessel);
        for (String sampleName : samplesWithoutVessel) {
            BSPSampleDTO bspDto = bspDtoMap.get(sampleName);
            if (bspDto != null && bspDto.isSampleReceived()) {
                vessels.addAll(labVesselFactory
                        .buildInitialLabVessels(sampleName, bspDto.getBarcodeForLabVessel(), username, new Date()));
            }
        }
        return vessels;
    }
}
