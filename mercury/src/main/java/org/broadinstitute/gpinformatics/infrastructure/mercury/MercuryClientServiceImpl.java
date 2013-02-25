package org.broadinstitute.gpinformatics.infrastructure.mercury;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.*;

import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.util.*;
import java.util.logging.Logger;

@Impl
@Default
public class MercuryClientServiceImpl implements MercuryClientService {
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final String eventLocation = "BSP";
    private final LabEventType eventType = LabEventType.PICO_PLATING_BUCKET;


    @Inject
    private MercurySampleDao mercurySampleDao;
    @Inject
    private BucketBean bucketBean;
    @Inject
    private BucketDao bucketDao;
    @Inject
    private LabBatchDAO labBatchDao;
    @Inject
    private WorkflowLoader workflowLoader;

    private BSPUserList userList;

    @Inject
    public void setUserList(BSPUserList userList) {
        this.userList = userList;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    public Collection<ProductOrderSample> addSampleToPicoBucket(ProductOrder pdo) {

        // Finds all samples in receiving.  Start with batches that are active with type sample receipt,
        // get their vessels, then the sample names.
        Map<String, LabVessel> samplesInReceiving = new HashMap<String, LabVessel>();
        for (LabBatch labBatch : labBatchDao.findBatchesInReceiving()) {
            for (LabVessel labVessel : labBatch.getStartingLabVessels()) {
                for (MercurySample vesselSample : labVessel.getMercurySamples()) {
                    String sampleName = vesselSample.getSampleKey();
                    if (samplesInReceiving.containsKey(sampleName)) {
                        logger.fine("Found multiple samples in receiving having name " + sampleName);
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

        // For each pdo sample, if it is in receiving, adds it to pico bucket.
        List<ProductOrderSample> samplesAdded = new ArrayList<ProductOrderSample>();
        Collection<LabVessel> vesselsAdded = new ArrayList<LabVessel>();

        for (ProductOrderSample pdoSample : pdo.getSamples()) {
            String stockSampleName = pdoSample.getSampleName();
            LabVessel vessel = samplesInReceiving.get(stockSampleName);
            if (vessel == null) {
                continue;
            }
            // todo Validates entry into bucket (must be genomic DNA)
            vesselsAdded.add(vessel);
            samplesAdded.add(pdoSample);
        }
        bucketBean.add(vesselsAdded, picoBucket, username, eventLocation, eventType, pdo.getBusinessKey());
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
            bucketDao.persist(picoBucket);
            logger.fine("Creating new bucket " + bucketName);
        }
        return picoBucket;
    }
}

