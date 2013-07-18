package org.broadinstitute.gpinformatics.infrastructure.mercury;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.broadinstitute.gpinformatics.mercury.entity.workflow.*;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.*;

/**
 * @author breilly
 */
@Stateful
@RequestScoped
public class MercuryClientEjb {
    private static final Log logger = LogFactory.getLog(MercuryClientEjb.class);
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

    public Collection<ProductOrderSample> addFromProductOrder(ProductOrder pdo, Collection<ProductOrderSample> samples) {
        // Limited to ExomeExpress pdos.
        if (pdo.getProduct() == null ||
                !WorkflowName.EXOME_EXPRESS.getWorkflowName().equals(pdo.getProduct().getWorkflowName())) {
            return Collections.emptyList();
        }

        WorkflowConfig workflowConfig = workflowLoader.load();
        ProductWorkflowDefVersion workflowDefVersion =
                workflowConfig.getWorkflowByName(pdo.getProduct().getWorkflowName())
                        .getEffectiveVersion();
        WorkflowBucketDef initialBucketDef = workflowDefVersion.getInitialBucket();

        Bucket initialBucket = null;
        if (initialBucketDef != null) {
            initialBucket = bucketEjb.findOrCreateBucket(initialBucketDef.getName());
        }
        if (initialBucket == null) {
            return Collections.emptyList();
        }

        String username = null;
        Long bspUserId = pdo.getCreatedBy();
        if (bspUserId != null) {
            BspUser bspUser = userList.getById(bspUserId);
            if (bspUser != null) {
                username = bspUser.getUsername();
            }
        }

        // Finds existing vessels for the pdo samples, or if none, then either the sample has not been received
        // or the sample is a derived stock that has not been seen by Mercury.  Creates both standalone vessel and
        // MercurySample for the latter.
        Map<String, Collection<ProductOrderSample>> nameToSampleMap = new HashMap<String, Collection<ProductOrderSample>>();
        Set<String> sampleKeyList = new HashSet<String>();
        for (ProductOrderSample pdoSample : samples) {
            // A pdo can have multiple samples all with same sample name but with different sample position.
            // Each one will get a MercurySample and LabVessel put into the bucket.
            Collection<ProductOrderSample> pdoSampleList = nameToSampleMap.get(pdoSample.getSampleName());
            if (pdoSampleList == null) {
                pdoSampleList = new ArrayList<ProductOrderSample>();
                nameToSampleMap.put(pdoSample.getSampleName(), pdoSampleList);
            }
            pdoSampleList.add(pdoSample);
            sampleKeyList.add(pdoSample.getSampleName());
        }

        List<LabVessel> vessels = labVesselDao.findBySampleKeyList(sampleKeyList);

        // Finds samples with no existing vessels.
        Collection<String> samplesWithoutVessel = new ArrayList<String>(sampleKeyList);
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
                LabEvent.UI_EVENT_LOCATION, initialBucketDef.getBucketEventType(), pdo.getBusinessKey());

        if (initialBucket.getBucketId() == null) {
            bucketDao.persist(initialBucket);
        }

        List<ProductOrderSample> samplesAdded = new ArrayList<ProductOrderSample>();
        for (LabVessel vessel : validVessels) {
            for (MercurySample sample : vessel.getMercurySamples()) {
                samplesAdded.addAll(nameToSampleMap.get(sample.getSampleKey()));
            }
        }
        return samplesAdded;
    }

    // todo jmt should this check be in bucketEjb.add?
    private Collection<LabVessel> applyBucketCriteria(Collection<LabVessel> vessels, WorkflowBucketDef bucketDef) {
        Collection<LabVessel> validVessels = new HashSet<LabVessel>();
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
        Collection<LabVessel> vessels = new ArrayList<LabVessel>();
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
