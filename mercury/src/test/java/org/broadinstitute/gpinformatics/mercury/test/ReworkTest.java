package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientProducer;
import org.broadinstitute.gpinformatics.mercury.control.dao.rework.LabVesselCommentDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.rework.LabVesselCommentDto;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.rework.ReworkEntry;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.rework.ReworkLevel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.rework.ReworkReason;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.util.*;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

/**
 * Test that samples can go partway through a workflow, be marked for rework, go to a previous
 * step, and then move to completion.
 */
public class ReworkTest extends LabEventTest {
    @Inject
    private LabVesselCommentDao labVesselCommentDao;

    @Inject
    private UserTransaction utx;

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

    @BeforeTest(enabled = false, groups = {EXTERNAL_INTEGRATION})
    public void beforeIntegrationTests() throws  Exception {
        if (utx == null) {
            return;
        }
        utx.begin();
    }

    @AfterTest(enabled = false, groups = {EXTERNAL_INTEGRATION})
    public void afterIntegrationTests() throws SystemException {
        if (utx == null) {
            return;
        }
        utx.rollback();
    }

    @Test(enabled = false, groups = {EXTERNAL_INTEGRATION})
    public void testMarkForRework() {
        LabVesselCommentDto dto= getTestLabVesselCommentDto();
        labVesselCommentDao.addComment(dto);
    }

    @Test(groups = {DATABASE_FREE})
    public void testMarkForReworkDbFree() {
        LabVesselCommentDto dto= getTestLabVesselCommentDto();

    }

    private LabVesselCommentDto getTestLabVesselCommentDto() {
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

        PreFlightEntityBuilder preFlightEntityBuilder =
                new PreFlightEntityBuilder(bettaLimsMessageFactory,
                        labEventFactory, labEventHandler,
                        mapBarcodeToTube).invoke();

        ShearingEntityBuilder shearingEntityBuilder =
                new ShearingEntityBuilder(mapBarcodeToTube, preFlightEntityBuilder.getTubeFormation(),
                        bettaLimsMessageFactory, labEventFactory, labEventHandler,
                        preFlightEntityBuilder.getRackBarcode()).invoke();

        final StaticPlate shearingPlate = shearingEntityBuilder.getShearingPlate();
        final List<SampleInstance> sampleInstances = samplesAtPosition(shearingPlate, "A", "1");

        final MercurySample startingSample = sampleInstances.iterator().next().getStartingSample();
        Map<MercurySample, VesselPosition> vpMap = new HashMap<MercurySample, VesselPosition>();
        vpMap.put(startingSample, VesselPosition.A01);
        final ReworkEntry rapSheetEntry = new ReworkEntry(ReworkReason.MACHINE_ERROR, ReworkLevel.ENTIRE_BATCH,
                LabEventType.SHEARING_BUCKET_ENTRY);
        LabVesselCommentDto labVesselCommentDto = new LabVesselCommentDto(null, shearingPlate, rapSheetEntry, vpMap, "foo");
        return labVesselCommentDto;
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
}
