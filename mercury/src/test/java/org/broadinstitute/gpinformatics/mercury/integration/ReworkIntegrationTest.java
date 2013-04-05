package org.broadinstitute.gpinformatics.mercury.integration;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean;
import org.broadinstitute.gpinformatics.infrastructure.mercury.MercuryClientEjb;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkReason;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.TestCoreActionBeanContext;
import org.broadinstitute.gpinformatics.mercury.presentation.workflow.AddReworkActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.workflow.BucketViewActionBean;
import org.broadinstitute.gpinformatics.mocks.EverythingYouAskForYouGetAndItsHuman;
import org.broadinstitute.gpinformatics.mocks.HappyQuoteServiceMock;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReworkIntegrationTest extends Arquillian {

    // I think all the action beans may need
    // to be annotated with @Default, @Impl, or something
    // that tells the container that these are beans
    // that can be injected.

    @Inject
    ProductOrderActionBean pdoActionBean;

    @Inject
    BucketViewActionBean bucketActionBean;

    @Inject
    ReworkEjb reworkEjb;

    @Inject BucketViewActionBean bucketViewActionBean;

    private static final String[] STOCKS_FOR_PDO1 = new String[] {
            "SM-123" + System.currentTimeMillis(),
            "SM-456" + System.currentTimeMillis(),
            "SM-789" + System.currentTimeMillis()
    };

    private static final String[] STOCKS_FOR_PDO2 = new String[] {
        "SM-888" + System.currentTimeMillis(),
        "SM-999" + System.currentTimeMillis()
    };


    /**
     * There's some kind of setup needed here to get
     * the context initialized during testing
     */
    @BeforeMethod(alwaysRun = true)
    public void initializeActionBean() {
        // set up context for action beans
        pdoActionBean.setContext(new TestCoreActionBeanContext());
        bucketActionBean.setContext(new TestCoreActionBeanContext());
    }

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.TEST,
                EverythingYouAskForYouGetAndItsHuman.class,
                HappyQuoteServiceMock.class);
    }

    /**
     * Use pdo action beans to create PDOs using
     * real code, not just newing stuff up
     * @return
     */
    private ProductOrder createPdo1FromActionBean() {
        throw new RuntimeException("I haven't been written yet.");
    }

    /**
     * Use pdo action beans to create PDOs using
     * real code, not just newing stuff up
     * @return
     */
    private ProductOrder createPdo2FromActionBean() {
        throw new RuntimeException("I haven't been written yet.");
    }

    /**
     * Creates a LabBatch for everything in the given PDO
     * @param pdo
     * @return
     */
    private LabBatch createBatchForEverythingInPdo(ProductOrder pdo) {
       throw new RuntimeException("I haven't been written yet.");
    }

    /**
     * Creates and processes a transfer from vessels from {@link org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch#getStartingLabVessels()}
     * to a new plate
     * @param batch
     * @return
     */
    private LabVessel doRackToPlateTransferForEverythingInBatch(LabBatch batch) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Test(enabled = false)
    public void test_rework() throws Exception {
        // create first PDO
        ProductOrder pdo1 = createPdo1FromActionBean();
        // create batch for samples from PDO1

        // create 2nd pdo.
        ProductOrder pdo2 = createPdo2FromActionBean();

        LabBatch batch1 = createBatchForEverythingInPdo(pdo1);
        LabVessel newPlateForPdo1 = doRackToPlateTransferForEverythingInBatch(batch1);

        LabBatch batch2 = createBatchForEverythingInPdo(pdo2);
        LabVessel newPlateForPdo2 = doRackToPlateTransferForEverythingInBatch(batch2);

        for (SampleInstance sampleInstance : newPlateForPdo1.getSampleInstances()) {
            // the transfer from the source tubes to the plate for pdo1
            // should be for batch1
            assertThat(batch1,equalTo(sampleInstance.getLabBatch()));
        }

        for (SampleInstance sampleInstance : newPlateForPdo2.getSampleInstances()) {
            // the transfer from the source tubes to the plate for pdo2
            // should be for batch2
            assertThat(batch2,equalTo(sampleInstance.getLabBatch()));
        }

        // the above asserts can be expanded along with more transfers so that
        // we can verify that both sub-graphs of transfers are related to
        // the right lcsets

        // now create a batch for some samples in pdo1 and some samples
        // in pdo2.
        LabVessel aTubeFromBatch1 = batch1.getStartingLabVessels().iterator().next();
        LabVessel aTubeFromBatch2 = batch2.getStartingLabVessels().iterator().next();

        // arz not sure what to set for other params
        reworkEjb.addReworks(aTubeFromBatch1, null,null,null);
        // arz for the "do rework with different PDO" case, seems like this
        // method's signature would change to include a PDO...but that's for later
        reworkEjb.addReworks(aTubeFromBatch1, null,null,null);

        List<String> selectedReworks = new ArrayList<String>(2);
        selectedReworks.add(aTubeFromBatch1.getLabel());
        selectedReworks.add(aTubeFromBatch2.getLabel());
        bucketActionBean.setSelectedReworks(selectedReworks);

        // now that we've added two rework tubes from two different
        // pdos, this should create a new batch for our rework
        bucketActionBean.createBatch();

        LabBatch reworkBatch = null;// need something like bucketAction.getBatch(), or have createBatch() return the batch

        LabVessel newPlateForRework = doRackToPlateTransferForEverythingInBatch(reworkBatch);

        for (SampleInstance sampleInstance : newPlateForRework.getSampleInstances()) {
            assertThat(reworkBatch,equalTo(sampleInstance.getLabBatch()));
        }

        // get the list of samples from one of the tubes that was reworked
        Set<String> reworkSamplesFromPdo1 = new HashSet<String>();
        for (SampleInstance sampleInstance : aTubeFromBatch1.getSampleInstances()) {
            reworkSamplesFromPdo1.add(sampleInstance.getStartingSample().getSampleKey());
        }

        for (String sample : reworkSamplesFromPdo1) {
            // go through each sample from the reworked tube and verify that the
            // expected sample is there
            boolean foundSample = false;
            for (SampleInstance reworkedSampleInstance : newPlateForRework.getSampleInstances()) {
                if (sample.equals(reworkedSampleInstance.getStartingSample().getSampleKey())) {
                    foundSample = true;
                    // now verify that the PDO is the right PDO
                    assertThat(pdo1.getJiraTicketKey(),equalTo(reworkedSampleInstance.getStartingSample().getProductOrderKey()));
                }
            }
            assertThat("Could not find reworked " + sample + " in sample instance list for reworked plate.",foundSample);

        }

        // get the list of samples from the other one of the tubes that was reworked
        Set<String> reworkSamplesFromPdo2 = new HashSet<String>();
        for (SampleInstance sampleInstance : aTubeFromBatch2.getSampleInstances()) {
            reworkSamplesFromPdo2.add(sampleInstance.getStartingSample().getSampleKey());
        }

        for (String sample : reworkSamplesFromPdo2) {
            // go through each sample from the reworked tube and verify that the
            // expected sample is there
            boolean foundSample = false;
            for (SampleInstance reworkedSampleInstance : newPlateForRework.getSampleInstances()) {
                if (sample.equals(reworkedSampleInstance.getStartingSample().getSampleKey())) {
                    foundSample = true;
                    // now verify that the PDO is the right PDO
                    assertThat(pdo2.getJiraTicketKey(),equalTo(reworkedSampleInstance.getStartingSample().getProductOrderKey()));
                }
            }
            assertThat("Could not find reworked " + sample + " in sample instance list for reworked plate.",foundSample);

        }
    }
}
