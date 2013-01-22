package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientProducer;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;
import static org.broadinstitute.gpinformatics.mercury.entity.workflow.rework.ReworkEntry.ReworkLevel.ONE_SAMPLE_HOLD_REST_BATCH;
import static org.broadinstitute.gpinformatics.mercury.entity.workflow.rework.ReworkEntry.ReworkReason.MACHINE_ERROR;

/**
 * Test that samples can go partway through a workflow, be marked for rework, go to a previous
 * step, and then move to completion.
 */
public class ReworkTest extends LabEventTest {
    @Test
    public void testX() {
        // Advance to Pond Pico

        // Mark 2 samples rework
        // How to verify marked?  Users want to know how many times a MercurySample has been reworked
        // Advance rest of samples to end
        // Verify that repeated transition is flagged as error on non-reworked samples
        // Re-enter 2 samples at Pre-LC? (Re-entry points in a process are enabled / disabled on a per product basis)
        // Can rework one sample in a pool?  No.
    }

    @BeforeTest(groups = {EXTERNAL_INTEGRATION})
    public void beforeIntegrationTests(){

    }

    @AfterTest(groups = {EXTERNAL_INTEGRATION})
    public void afterIntegrationTests(){

    }

    @Test(groups = {EXTERNAL_INTEGRATION})
    public void testMarkForRework(){

    }

    @Test(groups = {DATABASE_FREE})
    public void testMarkForReworkDbFree() {
        //        Controller.startCPURecording(true);

        List<ProductOrderSample> productOrderSamples = new ArrayList<ProductOrderSample>();
        ProductOrder productOrder = new ProductOrder(101L, "Test PO", productOrderSamples, "GSP-123", new Product(
                "Test product", new ProductFamily("Test product family"), "test", "1234", null, null, 10000, 20000, 100,
                40, null, null, true, "Whole Genome Shotgun", false), new ResearchProject(101L, "Test RP",
                "Test synopsis", false));
        String jiraTicketKey = "PD0-2";
        productOrder.setJiraTicketKey(jiraTicketKey);
        productOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        LabEventTest.mapKeyToProductOrder.put(jiraTicketKey, productOrder);

        // starting rack
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        for (int rackPosition = 1; rackPosition <= LabEventTest.NUM_POSITIONS_IN_RACK; rackPosition++) {
            String barcode = "R" + rackPosition;
            String bspStock = "SM-" + rackPosition;
            TwoDBarcodedTube bspAliquot = new TwoDBarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(jiraTicketKey, bspStock));
            mapBarcodeToTube.put(barcode, bspAliquot);
        }

        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
        LabEventFactory labEventFactory = new LabEventFactory();
        labEventFactory.setLabEventRefDataFetcher(labEventRefDataFetcher);
        LabEventHandler labEventHandler =
                new LabEventHandler(new WorkflowLoader(), AthenaClientProducer.stubInstance());

        LabEventTest.PreFlightEntityBuilder preFlightEntityBuilder =
                new LabEventTest.PreFlightEntityBuilder(bettaLimsMessageFactory,
                        labEventFactory, labEventHandler,
                        mapBarcodeToTube).invoke();

        LabEventTest.ShearingEntityBuilder shearingEntityBuilder =
                new LabEventTest.ShearingEntityBuilder(mapBarcodeToTube, preFlightEntityBuilder.getTubeFormation(),
                        bettaLimsMessageFactory, labEventFactory, labEventHandler,
                        preFlightEntityBuilder.getRackBarcode()).invoke();

        final StaticPlate shearingPlate = shearingEntityBuilder.getShearingPlate();
        final List<SampleInstance> sampleInstances = samplesAtPosition(shearingPlate, "A", "1");

        final MercurySample startingSample = sampleInstances.iterator().next().getStartingSample();
        startingSample.markForRework(MACHINE_ERROR, ONE_SAMPLE_HOLD_REST_BATCH,
                LabEventType.SHEARING_BUCKET_ENTRY, null, "Houston, we have a problem");
    }

    private List<SampleInstance> samplesAtPosition(LabVessel vessel, String rowName, String columnName) {
        List<SampleInstance> sampleInstances;
        VesselPosition position = VesselPosition.getByName(rowName + columnName);
        VesselContainer<?> vesselContainer = vessel.getContainerRole();
        if (vesselContainer != null) {
            sampleInstances = vesselContainer.getSampleInstancesAtPositionList(position);
        } else {
            sampleInstances = vessel.getSampleInstancesList();
        }
        return sampleInstances;
    }

    private List<MercurySample> toMercurySamples(List<SampleInstance> sampleInstances) {
        List<MercurySample> mercurySamples = new ArrayList<MercurySample>(sampleInstances.size());
        for (SampleInstance sampleInstance : sampleInstances) {
            mercurySamples.add(sampleInstance.getStartingSample());
        }
        return mercurySamples;
    }
}
