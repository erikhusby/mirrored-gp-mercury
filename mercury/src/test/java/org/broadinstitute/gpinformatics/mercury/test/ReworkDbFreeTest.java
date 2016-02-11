package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.LabEventTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabBatchComposition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.test.builders.ExomeExpressShearingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PicoPlatingEntityBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test that samples can go partway through a workflow, be marked for rework, go to a previous
 * step, and then move to completion.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class ReworkDbFreeTest extends BaseEventTest {

    @Test(enabled = true, groups = TestGroups.DATABASE_FREE)
    public void testAddReworkToBatchFromBucket() {
        expectedRouting = SystemRouter.System.MERCURY;

        String origLcsetSuffix = "-111";
        String reworkLcsetSuffix = "-222";

        long now = java.lang.System.currentTimeMillis();
        String origRackBarcodeSuffix = String.valueOf(now);
        String reworkRackBarcodeSuffix = String.valueOf(now + 137L);
        String origTubePrefix = "999999";
        String reworkTubePrefix = "888888";

        int startingSampleSize = 15;
        ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(startingSampleSize);
        Map<String, BarcodedTube> origRackMap = createInitialRack(productOrder, origTubePrefix);

        LabBatch origBatch =
                new LabBatch("origBatch", new HashSet<LabVessel>(origRackMap.values()), LabBatch.LabBatchType.WORKFLOW);
        origBatch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
        origBatch.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());
        bucketBatchAndDrain(origRackMap, productOrder, origBatch, origLcsetSuffix);
        PicoPlatingEntityBuilder pplatingEntityBuilder1 = runPicoPlatingProcess(
                origRackMap,
                origRackBarcodeSuffix,
                "1", true);

        ExomeExpressShearingEntityBuilder shearingEntityBuilder1 = runExomeExpressShearingProcess(
                pplatingEntityBuilder1.getNormBarcodeToTubeMap(),
                pplatingEntityBuilder1.getNormTubeFormation(),
                pplatingEntityBuilder1.getNormalizationBarcode(),
                "1");
        VesselContainer<PlateWell> origContainer = shearingEntityBuilder1.getShearingPlate().getContainerRole();

        LibraryConstructionEntityBuilder lcEntityBuilder1 = runLibraryConstructionProcess(
                shearingEntityBuilder1.getShearingCleanupPlate(),
                shearingEntityBuilder1.getShearCleanPlateBarcode(),
                shearingEntityBuilder1.getShearingPlate(),
                "1");

        Map<String, BarcodedTube> reworkRackMap = createInitialRack(productOrder, reworkTubePrefix);

        LabBatch reworkBatch = new LabBatch("reworkBatch", new HashSet<LabVessel>(reworkRackMap.values()),
                                            LabBatch.LabBatchType.WORKFLOW);

        reworkBatch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
        reworkBatch.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());
        bucketBatchAndDrain(reworkRackMap, productOrder, reworkBatch, reworkLcsetSuffix);
        PicoPlatingEntityBuilder pplatingEntityBuilder2 = runPicoPlatingProcess(
                reworkRackMap,
                reworkRackBarcodeSuffix,
                "2", true);
        //add reworks midstream at the shearing bucket from the original lcset which is at after lc construction

        Set<LabVessel> vessels = new HashSet<>();
        int numberOfReworks = 2;
        for (LabVessel tube : lcEntityBuilder1.getPondRegRack().getContainerRole().getContainedVessels()) {
            if (tube.getSampleInstanceCount() == 1) {
                vessels.add(tube);
            }
            if (vessels.size() == numberOfReworks) {
                break;
            }
        }


        Iterator<LabVessel> iterator = vessels.iterator();
        LabVessel tube1 = iterator.next();
        LabVessel tube2 = iterator.next();
        BucketEntry bucketEntry1 =
                new BucketEntry(tube1, productOrder, BucketEntry.BucketEntryType.REWORK_ENTRY);
        BucketEntry bucketEntry2 =
                new BucketEntry(tube2, productOrder, BucketEntry.BucketEntryType.REWORK_ENTRY);
        reworkBatch.addBucketEntry(bucketEntry1);
        reworkBatch.addBucketEntry(bucketEntry2);

        reworkBatch.addLabVessels(vessels);
        reworkBatch.addReworks(vessels);

        TubeFormation normTubeFormation = pplatingEntityBuilder2.getNormTubeFormation();
        TubeFormation reworkTubeFormation = LabEventTestFactory
                .makeTubeFormation(normTubeFormation, (BarcodedTube) tube1, (BarcodedTube) tube2);
        Map<String, BarcodedTube> reworkBarcodedTubeMap = new HashMap<>();

        for (BarcodedTube currTube : reworkTubeFormation.getContainerRole().getContainedVessels()) {
            reworkBarcodedTubeMap.put(currTube.getLabel(), currTube);
        }

        ExomeExpressShearingEntityBuilder shearingEntityBuilder2 = runExomeExpressShearingProcess(
                reworkBarcodedTubeMap,
                reworkTubeFormation,
                pplatingEntityBuilder2.getNormalizationBarcode(),
                "2");

        VesselContainer<PlateWell> reworkContainer = shearingEntityBuilder2.getShearingPlate().getContainerRole();
        // Checks the lab batch composition of the non-rework and rework containers.
        Assert.assertEquals(origContainer.getAllLabBatches().size(), 1);
        validateLabBatchComposition(origContainer.getLabBatchCompositions(),
                                    startingSampleSize,
                                    new int[]{startingSampleSize},
                                    new String[]{origLcsetSuffix, reworkLcsetSuffix});

        Assert.assertEquals(reworkContainer.getAllLabBatches().size(), 2);
        validateLabBatchComposition(reworkContainer.getLabBatchCompositions(),
                                    startingSampleSize + numberOfReworks,
                                    new int[]{startingSampleSize + numberOfReworks, numberOfReworks},
                                    new String[]{reworkLcsetSuffix, origLcsetSuffix});

        // Rework tube should be in two lcsets.
        Assert.assertEquals(tube1.getAllLabBatches().size(), 2);

        // Checks the correct batch identifier for the rework tube, which depends on the end container.
        Assert.assertTrue(tube1.getPluralityLabBatch(origContainer).getBatchName().endsWith(origLcsetSuffix));
        Assert.assertTrue(tube1.getPluralityLabBatch(reworkContainer).getBatchName().endsWith(reworkLcsetSuffix));
    }

    // Advance to Pond Pico, rework a sample from the start
    @Test(enabled = true, groups = TestGroups.DATABASE_FREE)
    public void testRework() {
        expectedRouting = SystemRouter.System.MERCURY;

        String origLcsetSuffix = "-111";
        String reworkLcsetSuffix = "-222";

        long now = java.lang.System.currentTimeMillis();
        String origRackBarcodeSuffix = String.valueOf(now);
        String reworkRackBarcodeSuffix = String.valueOf(now + 137L);

        String origTubePrefix = "999999";
        String reworkTubePrefix = "888888";
        int reworkIdx = NUM_POSITIONS_IN_RACK - 1; // arbitrary choice

        ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(NUM_POSITIONS_IN_RACK);
        Map<String, BarcodedTube> origRackMap = createInitialRack(productOrder, origTubePrefix);

        LabBatch origBatch =
                new LabBatch("origBatch", new HashSet<LabVessel>(origRackMap.values()), LabBatch.LabBatchType.WORKFLOW);
        origBatch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
        origBatch.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());
        bucketBatchAndDrain(origRackMap, productOrder, origBatch, origLcsetSuffix);
        PicoPlatingEntityBuilder pplatingEntityBuilder1 = runPicoPlatingProcess(
                origRackMap,
                origRackBarcodeSuffix,
                "1", true);

        ExomeExpressShearingEntityBuilder shearingEntityBuilder1 = runExomeExpressShearingProcess(
                pplatingEntityBuilder1.getNormBarcodeToTubeMap(),
                pplatingEntityBuilder1.getNormTubeFormation(),
                pplatingEntityBuilder1.getNormalizationBarcode(),
                "1");

        VesselContainer<PlateWell> origContainer = shearingEntityBuilder1.getShearingPlate().getContainerRole();

        // Selects the rework tube and verifies its lcset.
        BarcodedTube reworkTube = origRackMap.get(origTubePrefix + reworkIdx);
        Assert.assertNotNull(reworkTube);
        Assert.assertEquals(reworkTube.getLabBatches().size(), 1);
        Assert.assertEquals(reworkTube.getLabBatches().iterator().next().getBatchName(), "LCSET" + origLcsetSuffix);
        Assert.assertEquals(reworkTube.getMercurySamples().size(), 1);
        String reworkSampleKey = reworkTube.getMercurySamples().iterator().next().getSampleKey();

        // Starts the rework with a new rack of tubes and includes the rework tube.
        Map<String, BarcodedTube> reworkRackMap = createInitialRack(productOrder, reworkTubePrefix);
        Assert.assertTrue(reworkRackMap.containsKey(reworkTubePrefix + reworkIdx));
        reworkRackMap.remove(reworkTubePrefix + reworkIdx);
        reworkRackMap.put(reworkTube.getLabel(), reworkTube);

        LabBatch reworkBatch = new LabBatch("reworkBatch", new HashSet<LabVessel>(reworkRackMap.values()),
                                            LabBatch.LabBatchType.WORKFLOW);
        reworkBatch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
        reworkBatch.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());

        bucketBatchAndDrain(reworkRackMap, productOrder, reworkBatch, reworkLcsetSuffix);
        PicoPlatingEntityBuilder pplatingEntityBuilder2 = runPicoPlatingProcess(
                reworkRackMap,
                reworkRackBarcodeSuffix,
                "2", true);

        ExomeExpressShearingEntityBuilder shearingEntityBuilder2 = runExomeExpressShearingProcess(
                pplatingEntityBuilder2.getNormBarcodeToTubeMap(),
                pplatingEntityBuilder2.getNormTubeFormation(),
                pplatingEntityBuilder2.getNormalizationBarcode(),
                "2");

        LibraryConstructionEntityBuilder lcEntityBuilder2 = runLibraryConstructionProcess(
                shearingEntityBuilder2.getShearingCleanupPlate(),
                shearingEntityBuilder2.getShearCleanPlateBarcode(),
                shearingEntityBuilder2.getShearingPlate(),
                "2");

        VesselContainer<BarcodedTube> reworkContainer = lcEntityBuilder2.getPondRegRack().getContainerRole();

        /* shows the vessel transfers, for debug
            TransferVisualizerFrame transferVisualizerFrame = new TransferVisualizerFrame();
            transferVisualizerFrame.renderVessel(reworkRackMap.values().iterator().next());
            try {
                Thread.sleep(500000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        */


        // Checks the lab batch composition of the non-rework and rework containers.
        Assert.assertEquals(origContainer.getAllLabBatches().size(), 2);
        validateLabBatchComposition(origContainer.getLabBatchCompositions(),
                                    NUM_POSITIONS_IN_RACK,
                                    new int[]{NUM_POSITIONS_IN_RACK, 1},
                                    new String[]{origLcsetSuffix, reworkLcsetSuffix});

        Assert.assertEquals(reworkContainer.getAllLabBatches().size(), 2);
        validateLabBatchComposition(reworkContainer.getLabBatchCompositions(),
                                    NUM_POSITIONS_IN_RACK,
                                    new int[]{NUM_POSITIONS_IN_RACK, 1},
                                    new String[]{reworkLcsetSuffix, origLcsetSuffix});

        // Rework tube should be in two lcsets.
        Assert.assertEquals(reworkTube.getAllLabBatches().size(), 2);

        // Checks the correct batch identifier for the rework tube, which depends on the end container.
        Assert.assertTrue(reworkTube.getPluralityLabBatch(origContainer).getBatchName().endsWith(origLcsetSuffix));
        Assert.assertTrue(reworkTube.getPluralityLabBatch(reworkContainer).getBatchName().endsWith(reworkLcsetSuffix));
    }

    @Test(enabled = true)
    public void testMultiplePdos() {
        expectedRouting = SystemRouter.System.MERCURY;

        ProductOrder productOrder1 = ProductOrderTestFactory.createDummyProductOrder(4, "PDO-8",
                                                                                     Workflow.AGILENT_EXOME_EXPRESS, 1L,
                                                                                     "Test 1", "Test 1", false,
                                                                                     "ExEx-001", "A", "ExExQuoteId");
        ProductOrder productOrder2 = ProductOrderTestFactory.createDummyProductOrder(3, "PDO-9",
                                                                                     Workflow.AGILENT_EXOME_EXPRESS, 1L,
                                                                                     "Test 2", "Test 2", false,
                                                                                     "ExEx-001", "B", "ExExQuoteId");
        final Date runDate = new Date();

        Map<String, BarcodedTube> mapBarcodeToTube1 = createInitialRack(productOrder1, "R1");
        Map<String, BarcodedTube> mapBarcodeToTube2 = createInitialRack(productOrder2, "R2");
        Map.Entry<String, BarcodedTube> stringBarcodedTubeEntry =
                mapBarcodeToTube1.entrySet().iterator().next();
        mapBarcodeToTube2.put(stringBarcodedTubeEntry.getKey(), stringBarcodedTubeEntry.getValue());

        LabBatch workflowBatch1 = new LabBatch("Exome Express Batch 1",
                                               new HashSet<LabVessel>(mapBarcodeToTube1.values()),
                                               LabBatch.LabBatchType.WORKFLOW);
        workflowBatch1.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
        workflowBatch1.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());

        LabBatch workflowBatch2 = new LabBatch("Exome Express Batch 2",
                                               new HashSet<LabVessel>(mapBarcodeToTube2.values()),
                                               LabBatch.LabBatchType.WORKFLOW);
        workflowBatch2.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
        workflowBatch2.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());

        bucketBatchAndDrain(mapBarcodeToTube1, productOrder1, workflowBatch1, "1");
        PicoPlatingEntityBuilder picoPlatingEntityBuilder1 = runPicoPlatingProcess(mapBarcodeToTube1,
                                                                                   String.valueOf(runDate.getTime()),
                                                                                   "1", true);
        bucketBatchAndDrain(mapBarcodeToTube2, productOrder2, workflowBatch2, "2");
        PicoPlatingEntityBuilder picoPlatingEntityBuilder2 = runPicoPlatingProcess(mapBarcodeToTube2,
                                                                                   String.valueOf(runDate.getTime()),
                                                                                   "2", true);

        ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder1 = runExomeExpressShearingProcess(
                picoPlatingEntityBuilder1.getNormBarcodeToTubeMap(),
                picoPlatingEntityBuilder1.getNormTubeFormation(), picoPlatingEntityBuilder1.getNormalizationBarcode(),
                "1");
        ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder2 = runExomeExpressShearingProcess(
                picoPlatingEntityBuilder2.getNormBarcodeToTubeMap(),
                picoPlatingEntityBuilder2.getNormTubeFormation(), picoPlatingEntityBuilder1.getNormalizationBarcode(),
                "2");

        runTransferVisualizer(stringBarcodedTubeEntry.getValue());

        for (SampleInstanceV2 sampleInstance : exomeExpressShearingEntityBuilder2.getShearingCleanupPlate()
                                                                               .getSampleInstancesV2()) {
            Assert.assertEquals(sampleInstance.getSingleBatch(), workflowBatch2);
        }
    }

    private void validateLabBatchComposition(List<LabBatchComposition> composition, int denominator, int[] counts,
                                             String[] lcsetSuffixes) {
        Assert.assertEquals(composition.size(), counts.length);
        for (int idx = 0; idx < composition.size(); ++idx) {
            LabBatchComposition origComposition = composition.get(idx);
            Assert.assertEquals(origComposition.getDenominator(), denominator);
            Assert.assertEquals(origComposition.getCount(), counts[idx]);
            Assert.assertTrue(origComposition.getLabBatch().getBatchName().endsWith(lcsetSuffixes[idx]),
                              "Expected suffix " + lcsetSuffixes[idx] + " but got " + origComposition.getLabBatch()
                                                                                                     .getBatchName());
        }
    }
}
