package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
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

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * This test fakes adding tubes to a pico bucket
 */
public class AddEntriesToPicoBucketTest extends Arquillian {

    @Inject
    private AthenaClientService athenaClientService;

    @Inject
    private BucketDao bucketDao;
    @Inject
    private LabVesselDao labVesselDao;
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
        ProductOrder order = athenaClientService.retrieveProductOrderDetails("PDO-107");   //183

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

                    MercurySample mercurySample = new MercurySample(sample.getSampleName());
                    String tubeBarcode = sample.getBspSampleDTO().getBarcodeForLabVessel();
                    if (tubeBarcode != null) {
                        //lookup the vessel... if it doesn't exist create one
                        LabVessel vessel = labVesselDao.findByIdentifier(tubeBarcode);
                        if (vessel == null) {
                            vessel = new TwoDBarcodedTube(tubeBarcode);
                        }
                        vessel.addSample(mercurySample);
                        // if (workingBucketIdentifier.getEntryMaterialType().getName().equals(materialType)) {
                        workingBucket.addEntry(sample.getProductOrder().getBusinessKey(), vessel,
                                org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry.BucketEntryType.PDO_ENTRY);
                        // }
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
