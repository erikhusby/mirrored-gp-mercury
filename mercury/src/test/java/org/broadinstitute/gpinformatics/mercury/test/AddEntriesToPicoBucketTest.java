package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
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

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * This test fakes adding tubes to a pico bucket
 */
@Test(groups = TestGroups.STANDARD)
public class AddEntriesToPicoBucketTest extends Arquillian {

    @Inject
    private BucketDao bucketDao;
    @Inject
    private LabVesselDao labVesselDao;
    @Inject
    private UserTransaction utx;
    @Inject
    private WorkflowConfig workflowConfig;
    @Inject
    private ProductOrderDao productOrderDao;
    @Inject
    private SampleDataFetcher sampleDataFetcher;

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
        ProductOrder order = productOrderDao.findByBusinessKey("PDO-107");   //183

        if (order != null) {
            ProductWorkflowDef workflowDef = workflowConfig.getWorkflow(order.getProduct().getWorkflowName());
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

/*
The following code is commented out because it no longer compiles. It was originally was part of a fixup test which
is now disabled. The date it was disabled was 2/7/13 with the comment "Disable fixup test."
*/
//                    MercurySample mercurySample = new MercurySample(sample.getName(), MercurySample.MetadataSource.BSP);
//                    String tubeBarcode = sample.getSampleData().getBarcodeForLabVessel();
//                    if (tubeBarcode != null) {
//                        //lookup the vessel... if it doesn't exist create one
//                        LabVessel vessel = labVesselDao.findByIdentifier(tubeBarcode);
//                        if (vessel == null) {
//                            vessel = new BarcodedTube(tubeBarcode);
//                        }
//                        vessel.addSample(mercurySample);
//                        // if (workingBucketIdentifier.getEntryMaterialType().getName().equals(materialType)) {
//                        workingBucket.addEntry(sample.getProductOrder(), vessel,
//                                               BucketEntry.BucketEntryType.PDO_ENTRY);
//                        // }
//                    }
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
