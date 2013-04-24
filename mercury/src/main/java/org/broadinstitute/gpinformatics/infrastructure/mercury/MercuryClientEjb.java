package org.broadinstitute.gpinformatics.infrastructure.mercury;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
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

    private BucketBean bucketBean;

    private WorkflowLoader workflowLoader;

    private BucketDao bucketDao;
    private BSPSampleDataFetcher bspSampleDataFetcher;

    public MercuryClientEjb() {}

    @Inject
    public MercuryClientEjb(BucketBean bucketBean, BucketDao bucketDao,
                            WorkflowLoader workflowLoader, BSPUserList userList,
                            LabVesselDao labVesselDao, BSPSampleDataFetcher bspSampleDataFetcher) {
        this.bucketBean = bucketBean;
        this.bucketDao = bucketDao;
        this.workflowLoader = workflowLoader;
        this.userList = userList;
        this.labVesselDao = labVesselDao;
        this.bspSampleDataFetcher = bspSampleDataFetcher;
    }

    public Collection<ProductOrderSample> addFromProductOrder(ProductOrder pdo) {
        return addFromProductOrder(pdo, pdo.getSamples());
    }

    public Collection<ProductOrderSample> addFromProductOrder(ProductOrder pdo, Collection<ProductOrderSample> samples) {

        // Finds the pico bucket from workflow config for this product.
        WorkflowBucketDef picoBucketDef = findPicoBucketDef(pdo.getProduct());
        Bucket picoBucket = null;
        if (picoBucketDef != null) {
            picoBucket = findPicoBucket(picoBucketDef);
        }
        if (picoBucket == null) {
            return Collections.emptyList();
        }

        // Finds the vessels for MercurySamples representing the pdo samples.
        Map<String, ProductOrderSample> nameToSampleMap = new HashMap<String, ProductOrderSample>();
        for (ProductOrderSample pdoSample : samples) {
            nameToSampleMap.put(pdoSample.getSampleName(), pdoSample);
        }
        List<LabVessel> vessels = labVesselDao.findBySampleKeyList(new ArrayList<String>(nameToSampleMap.keySet()));

        Collection<LabVessel> validVessels = applyPicoBucketCriteria(vessels, picoBucketDef, pdo);

        String username = null;
        Long bspUserId = pdo.getCreatedBy();
        if (bspUserId != null) {
            BspUser bspUser = userList.getById(bspUserId);
            if (bspUser != null) {
                username = bspUser.getUsername();
            }
        }

        bucketBean.add(validVessels, picoBucket, username, LabEvent.UI_EVENT_LOCATION, LabEventType.PICO_PLATING_BUCKET,
                pdo.getBusinessKey());

        if (picoBucket.getBucketId() == null) {
            bucketDao.persist(picoBucket);
        }

        List<ProductOrderSample> samplesAdded = new ArrayList<ProductOrderSample>();
        for (LabVessel vessel : validVessels) {
            for (MercurySample sample : vessel.getMercurySamples()) {
                samplesAdded.add(nameToSampleMap.get(sample.getSampleKey()));
            }
        }
        return samplesAdded;
    }

    // todo jmt should this check be in bucketBean.add?
    private Collection<LabVessel> applyPicoBucketCriteria(Collection<LabVessel> vessels,
                                                          WorkflowBucketDef bucketDef,
                                                          ProductOrder pdo) {

        Collection<String> sampleNames = new ArrayList<String>();
        for (LabVessel vessel : vessels) {
            for (MercurySample mercurySample : vessel.getMercurySamples()) {
                sampleNames.add(mercurySample.getSampleKey());
            }
        }
        Map<String, BSPSampleDTO> bspDtoMap = bspSampleDataFetcher.fetchSamplesFromBSP(sampleNames);

        Collection<LabVessel> validVessels = new HashSet<LabVessel>();
        for (LabVessel vessel : vessels) {
            if (bucketDef.meetsBucketCriteria(vessel)) {

                for (MercurySample mercurySample : vessel.getMercurySamples()) {
                    String sampleKey = mercurySample.getSampleKey();
                    BSPSampleDTO bspDto = bspDtoMap.get(sampleKey);

                    if (bspDto != null && bspDto.isSampleReceived()) {
                        validVessels.add(vessel);
                    }
                }
            }
        }
        return validVessels;
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

        ProductWorkflowDefVersion.LabEventNode labEventNode =
                versionResult.findStepByEventType(LabEventType.PICO_PLATING_BUCKET.getName());
        if (labEventNode == null) {
            return null;
        } else {
            return (WorkflowBucketDef) labEventNode.getStepDef();
        }
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
