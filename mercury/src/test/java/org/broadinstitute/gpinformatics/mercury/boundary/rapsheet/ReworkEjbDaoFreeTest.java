package org.broadinstitute.gpinformatics.mercury.boundary.rapsheet;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkEntry;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;
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
                        LabEventType.PICO_PLATING_BUCKET, "", WorkflowName.EXOME_EXPRESS.getWorkflowName());

        Assert.assertEquals(reworkSamples.size(),1);

        Assert.assertEquals(reworkSamples.iterator().next().getBspSampleName(),productOrder.getSamples().iterator().next().getBspSampleName());

    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void reworkDaoFreePreviouslyInBucket() throws ValidationException {

        ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(1);
        AthenaClientServiceStub.addProductOrder(productOrder);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");

        BucketEntry bucketEntry =
                new BucketEntry(mapBarcodeToTube.values().iterator().next(), productOrder.getBusinessKey(),
                        new Bucket("Pico/Plating Bucket"));
        bucketEntry.setStatus(BucketEntry.Status.Archived);
        mapBarcodeToTube.values().iterator().next().addBucketEntry(
                bucketEntry);

        ReworkEjb reworkEjb = new ReworkEjb();

        Collection<MercurySample> reworkSamples =
                reworkEjb.getVesselRapSheet(mapBarcodeToTube.values().iterator().next(),
                        ReworkEntry.ReworkReason.UNKNOWN_ERROR, ReworkEntry.ReworkLevel.ONE_SAMPLE_HOLD_REST_BATCH,
                        LabEventType.PICO_PLATING_BUCKET, "", WorkflowName.EXOME_EXPRESS.getWorkflowName());

        Assert.assertEquals(reworkSamples.size(),1);

        Assert.assertEquals(reworkSamples.iterator().next().getBspSampleName(),productOrder.getSamples().iterator().next().getBspSampleName());

    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void reworkDaoFreeAncestorPreviouslyInBucket() throws ValidationException {

        ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(1);
        AthenaClientServiceStub.addProductOrder(productOrder);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");

        LabBatch workflowBatch = new LabBatch("Exome Express Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()), LabBatch.LabBatchType.WORKFLOW);

        Date runDate = new Date();

        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToTube, productOrder,
                workflowBatch, null, String.valueOf(runDate.getTime()), "1", true);

        ReworkEjb reworkEjb = new ReworkEjb();

        Collection<MercurySample> reworkSamples =
                reworkEjb.getVesselRapSheet(
                        picoPlatingEntityBuilder.getNormBarcodeToTubeMap().values().iterator().next(),
                        ReworkEntry.ReworkReason.UNKNOWN_ERROR, ReworkEntry.ReworkLevel.ONE_SAMPLE_HOLD_REST_BATCH,
                        LabEventType.PICO_PLATING_BUCKET, "", WorkflowName.EXOME_EXPRESS.getWorkflowName());

        Assert.assertEquals(reworkSamples.size(),1);

        Assert.assertEquals(reworkSamples.iterator().next().getBspSampleName(),productOrder.getSamples().iterator().next().getBspSampleName());

    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void reworkDaoFreeAncestorStillInBucket() throws ValidationException {

        ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(1);
        AthenaClientServiceStub.addProductOrder(productOrder);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");

        LabBatch workflowBatch = new LabBatch("Exome Express Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()), LabBatch.LabBatchType.WORKFLOW);

        Date runDate = new Date();

        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToTube, productOrder,
                workflowBatch, null, String.valueOf(runDate.getTime()), "1", false);

        ReworkEjb reworkEjb = new ReworkEjb();

        Collection<MercurySample> reworkSamples =
                reworkEjb.getVesselRapSheet(
                    picoPlatingEntityBuilder.getNormBarcodeToTubeMap().values().iterator().next(),
                    ReworkEntry.ReworkReason.UNKNOWN_ERROR, ReworkEntry.ReworkLevel.ONE_SAMPLE_HOLD_REST_BATCH,
                    LabEventType.PICO_PLATING_BUCKET, "", WorkflowName.EXOME_EXPRESS.getWorkflowName());

        Assert.assertEquals(reworkSamples.size(),1);

        Assert.assertEquals(reworkSamples.iterator().next().getBspSampleName(),productOrder.getSamples().iterator().next().getBspSampleName());

    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void reworkDaoFreeInBucket() {

        ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(1);
        AthenaClientServiceStub.addProductOrder(productOrder);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");

        mapBarcodeToTube.values().iterator().next().addBucketEntry(
                new BucketEntry(mapBarcodeToTube.values().iterator().next(), productOrder.getBusinessKey(),
                        new Bucket("Pico/Plating Bucket")));

        ReworkEjb reworkEjb = new ReworkEjb();

        try {
            reworkEjb.getVesselRapSheet(mapBarcodeToTube.values().iterator().next(),
                    ReworkEntry.ReworkReason.UNKNOWN_ERROR, ReworkEntry.ReworkLevel.ONE_SAMPLE_HOLD_REST_BATCH,
                    LabEventType.PICO_PLATING_BUCKET, "", WorkflowName.EXOME_EXPRESS.getWorkflowName());

            Assert.fail();
        } catch (ValidationException e) {

        }
    }
}
