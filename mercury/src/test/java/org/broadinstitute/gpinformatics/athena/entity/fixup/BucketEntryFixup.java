package org.broadinstitute.gpinformatics.athena.entity.fixup;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * This test fakes adding tubes to a pico bucket
 */
public class BucketEntryFixup extends Arquillian {
    @Inject
    private ProductOrderDao pdoDao;
    @Inject
    private BucketDao bucketDao;
    @Inject
    private UserTransaction utx;
    @Inject
    private WorkflowLoader workflowLoader;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }


    @BeforeMethod
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

//        utx.setTransactionTimeout(300);
        utx.begin();
    }

    @Test(enabled = false)
    public void addExomeExpressPicoBucketEntries() {
        ProductOrder order = null;
        List<ProductOrder> pdos = pdoDao.findAll(ProductOrderDao.FetchSpec.Product);
        //find an exome express pdo
        for (ProductOrder pdo : pdos) {
            if (pdo.getProduct().getPartNumber().equals("P-EX-0002")) {
                order = pdo;
                break;
            }
        }
        if (order != null) {
            WorkflowConfig workflowConfig = workflowLoader.load();
            ProductWorkflowDef workflowDef = workflowConfig.getWorkflowByName(order.getProduct().getWorkflowName());
            ProductWorkflowDefVersion workflowVersion = workflowDef.getEffectiveVersion();
            WorkflowBucketDef workingBucketIdentifier = null;
            for (WorkflowBucketDef bucketDef : workflowVersion.getBuckets()) {
                if (bucketDef.getName().equals("Pico/Plating Bucket")) {
                    workingBucketIdentifier = bucketDef;
                }
            }
            if (workingBucketIdentifier != null) {
                Bucket workingBucket = bucketDao.findByName(workingBucketIdentifier.getName());
                if (workingBucket == null) {
                    workingBucket = new Bucket(workingBucketIdentifier);
                }
                for (ProductOrderSample sample : order.getSamples()) {
                    LabVessel vessel = new TwoDBarcodedTube(sample.getSampleName());
                    MercurySample mercurySample = new MercurySample(sample.getProductOrder().getBusinessKey(), sample.getSampleName());
                    vessel.addSample(mercurySample);
                    String materialType = sample.getBspDTO().getMaterialType();
                    if (workingBucketIdentifier.getEntryMaterialType().getName().equals(materialType)) {
                        workingBucket.addEntry(sample.getProductOrder().getBusinessKey(), vessel);
                    }
                }
                bucketDao.persist(workingBucket);
            }

        }
    }

    @AfterMethod
    public void tearDown() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.commit();
    }

}
