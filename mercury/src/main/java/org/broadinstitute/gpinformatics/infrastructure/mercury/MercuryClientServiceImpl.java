package org.broadinstitute.gpinformatics.infrastructure.mercury;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.*;

import javax.ejb.Stateful;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.util.*;


@Impl
@Default
public class MercuryClientServiceImpl implements MercuryClientService {
    private final Log logger = LogFactory.getLog(getClass());
    private final String eventLocation = LabEvent.UI_EVENT_LOCATION;
    private final LabEventType eventType = LabEventType.PICO_PLATING_BUCKET;

    private BucketBean bucketBean;
    private BucketDao bucketDao;
    private LabBatchDAO labBatchDao;
    private WorkflowLoader workflowLoader;
    private BSPUserList userList;
    private LabVesselDao labVesselDao;

    @Inject
    public MercuryClientServiceImpl(BucketBean bucketBean, BucketDao bucketDao, LabBatchDAO labBatchDao,
                                    WorkflowLoader workflowLoader, BSPUserList userList, LabVesselDao lvd) {
        this.bucketBean = bucketBean;
        this.bucketDao = bucketDao;
        this.labBatchDao = labBatchDao;
        this.workflowLoader = workflowLoader;
        this.userList = userList;
        labVesselDao = lvd;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    public Collection<ProductOrderSample> addSampleToPicoBucket(ProductOrder pdo) {
        List<ProductOrderSample> samplesAdded = new ArrayList<ProductOrderSample>();
        Collection<LabVessel> vesselsAdded = new ArrayList<LabVessel>();

        // Finds the vessels for MercurySamples representing the pdo samples.
        List<String> sampleNames = new ArrayList<String>();
        for (ProductOrderSample pdoSample : pdo.getSamples()) {
            sampleNames.add(pdoSample.getSampleName());
        }
        List<LabVessel> vessels = labVesselDao.findBySampleKeyList(sampleNames);

        // Determines if the vessel is in receiving, by finding its active batch
        // and checking if that batch has sample receipt type.

        for (MercurySample mercurySample : mercurySamples) {

            LabVessel vessel = samplesInReceiving.get(stockSampleName);
            if (vessel == null) {
                continue;
            }
            // todo Validates entry into bucket (must be genomic DNA)
            vesselsAdded.add(vessel);
            samplesAdded.add(pdoSample);
        }
        bucketBean.add(vesselsAdded, picoBucket, username, eventLocation, eventType, pdo.getBusinessKey());

        // Finds all samples in receiving.  Start with batches that are active with type sample receipt,
        // get their vessels, then the sample names.
        Map<String, LabVessel> samplesInReceiving = new HashMap<String, LabVessel>();
        for (LabBatch labBatch : labBatchDao.findBatchesInReceiving()) {
            for (LabVessel labVessel : labBatch.getStartingLabVessels()) {
                for (MercurySample vesselSample : labVessel.getMercurySamples()) {
                    String sampleName = vesselSample.getSampleKey();
                    if (samplesInReceiving.containsKey(sampleName)) {
                        logger.debug("Found multiple samples in receiving having name " + sampleName);
                    }
                    samplesInReceiving.put(sampleName, labVessel);
                }
            }
        }

        // Finds the pico bucket from workflow config for this product.
        Bucket picoBucket = findPicoBucket(pdo.getProduct());
        if (picoBucket == null) {
            return Collections.EMPTY_LIST;
        }

        String username = null;
        Long bspUserId = pdo.getCreatedBy();
        if (bspUserId != null) {
            BspUser bspUser = userList.getById(bspUserId);
            if (bspUser != null) {
                username = bspUser.getUsername();
            }
        }

        return samplesAdded;
    }

    private Bucket findPicoBucket(Product product) {
        if (StringUtils.isBlank(product.getWorkflowName())) {
            return null;
        }

        WorkflowConfig workflowConfig = workflowLoader.load();
        assert(workflowConfig != null && workflowConfig.getProductWorkflowDefs() != null && workflowConfig.getProductWorkflowDefs().size() > 0);
        ProductWorkflowDef productWorkflowDef = workflowConfig.getWorkflowByName(product.getWorkflowName());
        ProductWorkflowDefVersion versionResult = productWorkflowDef.getEffectiveVersion();

        WorkflowStepDef bucketStep = versionResult.findStepByEventType(eventType.getName()).getStepDef();
        String bucketName = bucketStep.getName();
        Bucket picoBucket = bucketDao.findByName(bucketName);
        if (picoBucket == null) {
            picoBucket = new Bucket(bucketStep);
            logger.debug("Created new bucket " + bucketName);
        }
        return picoBucket;
    }
}

