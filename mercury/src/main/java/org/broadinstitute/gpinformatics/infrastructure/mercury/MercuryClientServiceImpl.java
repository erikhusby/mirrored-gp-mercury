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
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;

import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Impl
@Default
public class MercuryClientServiceImpl implements MercuryClientService {
    private static final Log logger = LogFactory.getLog(MercuryClientServiceImpl.class);
    private static final String eventLocation = LabEvent.UI_EVENT_LOCATION;
    private static final LabEventType eventType = LabEventType.PICO_PLATING_BUCKET;

    private BucketBean bucketBean;
    private BucketDao bucketDao;
    private WorkflowLoader workflowLoader;
    private BSPUserList userList;
    private LabVesselDao labVesselDao;

    @Inject
    public MercuryClientServiceImpl(BucketBean bucketBean, BucketDao bucketDao, WorkflowLoader workflowLoader,
                                    BSPUserList userList, LabVesselDao lvd) {
        this.bucketBean = bucketBean;
        this.bucketDao = bucketDao;
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
        Map<String, ProductOrderSample> nameToSampleMap = new HashMap<String, ProductOrderSample>();
        for (ProductOrderSample pdoSample : pdo.getSamples()) {
            nameToSampleMap.put(pdoSample.getSampleName(), pdoSample);
        }
        List<String> listOfSampleNames = new ArrayList<String>(nameToSampleMap.keySet());
        List<LabVessel> vessels = labVesselDao.findBySampleKeyList(listOfSampleNames);

        // Determines if the vessel is in receiving, by finding its active batch
        // and checking if that batch is a sample receipt batch.

        // Finds the pico bucket from workflow config for this product.
        WorkflowBucketDef picoBucketDef = findPicoBucketDef(pdo.getProduct());
        Bucket picoBucket = findPicoBucket(picoBucketDef);
        if (picoBucket == null) {
            return Collections.EMPTY_LIST;
        }

        for (LabVessel vessel : vessels) {
            Collection<LabBatch> batches = vessel.getLabBatches();
            if (batches.isEmpty()) {
                batches = vessel.getNearestLabBatches();
            }
            if (batches.size() == 1 && batches.iterator().next().getLabBatchType() == LabBatch.LabBatchType.SAMPLES_RECEIPT) {
                // todo jmt should this check be in bucketBean.add?
                if (picoBucketDef.meetsBucketCriteria(vessel)) {
                    vesselsAdded.add(vessel);

                    for (MercurySample mercurySample : vessel.getMercurySamples()) {
                        assert(nameToSampleMap.containsKey(mercurySample.getSampleKey()));
                        samplesAdded.add(nameToSampleMap.get(mercurySample.getSampleKey()));
                    }
                }
            }
        }

        String username = null;
        Long bspUserId = pdo.getCreatedBy();
        if (bspUserId != null) {
            BspUser bspUser = userList.getById(bspUserId);
            if (bspUser != null) {
                username = bspUser.getUsername();
            }
        }

        bucketBean.add(vesselsAdded, picoBucket, username, eventLocation, eventType, pdo.getBusinessKey());

        return samplesAdded;
    }

    private WorkflowBucketDef findPicoBucketDef(Product product) {
        if (StringUtils.isBlank(product.getWorkflowName())) {
            return null;
        }

        WorkflowConfig workflowConfig = workflowLoader.load();
        assert(workflowConfig != null && workflowConfig.getProductWorkflowDefs() != null &&
                !workflowConfig.getProductWorkflowDefs().isEmpty());
        ProductWorkflowDef productWorkflowDef = workflowConfig.getWorkflowByName(product.getWorkflowName());
        ProductWorkflowDefVersion versionResult = productWorkflowDef.getEffectiveVersion();

        return (WorkflowBucketDef) versionResult.findStepByEventType(eventType.getName()).getStepDef();
    }

    private Bucket findPicoBucket(WorkflowBucketDef bucketStep) {
        String bucketName = bucketStep.getName();
        Bucket picoBucket = bucketDao.findByName(bucketName);
        if (picoBucket == null) {
            picoBucket = new Bucket(bucketStep);
            logger.debug("Created new bucket " + bucketName);
        }
        return picoBucket;
    }
}

