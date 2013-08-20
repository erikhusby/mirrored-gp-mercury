package org.broadinstitute.gpinformatics.mercury.boundary.rapsheet;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkEntry;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.PicoPlatingEntityBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;

/**
 * Tests the ReworkEjb logic in a DBFree manner.  This test should cover the basic functionality while remaining
 * (relatively) speedy on execution
 */
public class ReworkEjbDaoFreeTest extends BaseEventTest {

    @Test(groups = TestGroups.DATABASE_FREE)
    public void reworkDaoFree() throws ValidationException {

        ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(1);
        AthenaClientServiceStub.addProductOrder(productOrder);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");

        ReworkEjb reworkEjb = new ReworkEjb();

        Collection<MercurySample> reworkSamples =
                reworkEjb.getVesselRapSheet(mapBarcodeToTube.values().iterator().next(),
                        ReworkEntry.ReworkReason.UNKNOWN_ERROR, ReworkEntry.ReworkLevel.ONE_SAMPLE_HOLD_REST_BATCH,
                        LabEventType.PICO_PLATING_BUCKET, "", Workflow.EXOME_EXPRESS);

        Assert.assertEquals(reworkSamples.size(), 1);

        Assert.assertEquals(reworkSamples.iterator().next().getBspSampleName(),
                productOrder.getSamples().iterator().next().getBspSampleName());

    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void reworkDaoFreePreviouslyInBucket() throws ValidationException {

        ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(1);
        AthenaClientServiceStub.addProductOrder(productOrder);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");

        BucketEntry bucketEntry =
                new BucketEntry(mapBarcodeToTube.values().iterator().next(), productOrder.getBusinessKey(),
                        new Bucket("Pico/Plating Bucket"), BucketEntry.BucketEntryType.PDO_ENTRY);
        bucketEntry.setStatus(BucketEntry.Status.Archived);
        mapBarcodeToTube.values().iterator().next().addBucketEntry(
                bucketEntry);

        ReworkEjb reworkEjb = new ReworkEjb();

        Collection<MercurySample> reworkSamples =
                reworkEjb.getVesselRapSheet(mapBarcodeToTube.values().iterator().next(),
                        ReworkEntry.ReworkReason.UNKNOWN_ERROR, ReworkEntry.ReworkLevel.ONE_SAMPLE_HOLD_REST_BATCH,
                        LabEventType.PICO_PLATING_BUCKET, "", Workflow.EXOME_EXPRESS);

        Assert.assertEquals(reworkSamples.size(), 1);

        Assert.assertEquals(reworkSamples.iterator().next().getBspSampleName(),
                productOrder.getSamples().iterator().next().getBspSampleName());

    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void reworkDaoFreeAncestorPreviouslyInBucket() throws ValidationException {
        expectedRouting = SystemRouter.System.MERCURY;

        ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(1);
        AthenaClientServiceStub.addProductOrder(productOrder);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");

        LabBatch workflowBatch = new LabBatch("Exome Express Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()), LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());
        workflowBatch.setWorkflow(Workflow.EXOME_EXPRESS);

        Date runDate = new Date();

        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, "1");
        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToTube,
                String.valueOf(runDate.getTime()), "1", true);

        ReworkEjb reworkEjb = new ReworkEjb();

        Collection<MercurySample> reworkSamples =
                reworkEjb.getVesselRapSheet(
                        picoPlatingEntityBuilder.getNormBarcodeToTubeMap().values().iterator().next(),
                        ReworkEntry.ReworkReason.UNKNOWN_ERROR, ReworkEntry.ReworkLevel.ONE_SAMPLE_HOLD_REST_BATCH,
                        LabEventType.PICO_PLATING_BUCKET, "", Workflow.EXOME_EXPRESS);

        Assert.assertEquals(reworkSamples.size(), 1);

        Assert.assertEquals(reworkSamples.iterator().next().getBspSampleName(),
                productOrder.getSamples().iterator().next().getBspSampleName());

    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void reworkDaoFreeAncestorStillInBucket() throws ValidationException {
        expectedRouting = SystemRouter.System.MERCURY;

        ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(1);
        AthenaClientServiceStub.addProductOrder(productOrder);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");

        LabBatch workflowBatch = new LabBatch("Exome Express Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()), LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(Workflow.EXOME_EXPRESS);
        workflowBatch.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());

        Date runDate = new Date();

        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, "1");
        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToTube,
                String.valueOf(runDate.getTime()), "1", false);

        ReworkEjb reworkEjb = new ReworkEjb();

        Collection<MercurySample> reworkSamples =
                reworkEjb.getVesselRapSheet(
                        picoPlatingEntityBuilder.getNormBarcodeToTubeMap().values().iterator().next(),
                        ReworkEntry.ReworkReason.UNKNOWN_ERROR, ReworkEntry.ReworkLevel.ONE_SAMPLE_HOLD_REST_BATCH,
                        LabEventType.PICO_PLATING_BUCKET, "", Workflow.EXOME_EXPRESS);

        Assert.assertEquals(reworkSamples.size(), 1);

        Assert.assertEquals(reworkSamples.iterator().next().getBspSampleName(),
                productOrder.getSamples().iterator().next().getBspSampleName());

    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void reworkDaoFreeInBucket() {

        ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(1);
        AthenaClientServiceStub.addProductOrder(productOrder);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");

        mapBarcodeToTube.values().iterator().next().addBucketEntry(
                new BucketEntry(mapBarcodeToTube.values().iterator().next(), productOrder.getBusinessKey(),
                        new Bucket("Pico/Plating Bucket"), BucketEntry.BucketEntryType.PDO_ENTRY));

        ReworkEjb reworkEjb = new ReworkEjb();

        try {
            reworkEjb.getVesselRapSheet(mapBarcodeToTube.values().iterator().next(),
                    ReworkEntry.ReworkReason.UNKNOWN_ERROR, ReworkEntry.ReworkLevel.ONE_SAMPLE_HOLD_REST_BATCH,
                    LabEventType.PICO_PLATING_BUCKET, "", Workflow.EXOME_EXPRESS);

            Assert.fail();
        } catch (ValidationException e) {

        }
    }
}
