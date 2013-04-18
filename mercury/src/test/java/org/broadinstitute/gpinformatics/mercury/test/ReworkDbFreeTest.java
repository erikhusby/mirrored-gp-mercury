package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabBatchComposition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.test.builders.ExomeExpressShearingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PicoPlatingEntityBuilder;
import org.easymock.EasyMock;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Test that samples can go partway through a workflow, be marked for rework, go to a previous
 * step, and then move to completion.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class ReworkDbFreeTest extends BaseEventTest {

    // Advance to Pond Pico, rework a sample from the start
    @Test(enabled = true, groups = TestGroups.DATABASE_FREE)
    public void testRework() throws Exception {

        String origLcsetSuffix = "-111";
        String reworkLcsetSuffix = "-222";

        long now = System.currentTimeMillis();
        String origRackBarcodeSuffix = String.valueOf(now);
        String reworkRackBarcodeSuffix = String.valueOf(now + 137L);

        String origTubePrefix = "999999";
        String reworkTubePrefix = "888888";
        int reworkIdx = NUM_POSITIONS_IN_RACK - 1; // arbitrary choice

        ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(NUM_POSITIONS_IN_RACK);
        AthenaClientServiceStub.addProductOrder(productOrder);
        Map<String, TwoDBarcodedTube> origRackMap = createInitialRack(productOrder, origTubePrefix);
        Bucket workingBucket = createAndPopulateBucket(origRackMap, productOrder, "Pico/Plating Bucket");

        BucketDao mockBucketDao = EasyMock.createMock(BucketDao.class);
        EasyMock.expect(mockBucketDao.findByName("Pico/Plating Bucket")).andReturn(workingBucket).times(2);
        EasyMock.expect(mockBucketDao.findByName(EasyMock.anyObject(String.class))).andReturn(new Bucket("FAKEBUCKET")).times(2);
        EasyMock.replay(mockBucketDao);

        PicoPlatingEntityBuilder pplatingEntityBuilder1 = runPicoPlatingProcess(
                origRackMap,
                productOrder,
                new LabBatch("origBatch", new HashSet<LabVessel>(origRackMap.values()), LabBatch.LabBatchType.WORKFLOW),
                origLcsetSuffix,
                origRackBarcodeSuffix,
                "1");

        ExomeExpressShearingEntityBuilder shearingEntityBuilder1 = runExomeExpressShearingProcess(
                productOrder,
                pplatingEntityBuilder1.getNormBarcodeToTubeMap(),
                pplatingEntityBuilder1.getNormTubeFormation(),
                pplatingEntityBuilder1.getNormalizationBarcode(),
                "1");

        VesselContainer origContainer = shearingEntityBuilder1.getShearingPlate().getContainerRole();

        // Selects the rework tube and verifies its lcset.
        TwoDBarcodedTube reworkTube = origRackMap.get(origTubePrefix + reworkIdx);
        assertNotNull(reworkTube);
        assertEquals(reworkTube.getLabBatchesList().size(), 1);
        assertEquals(reworkTube.getLabBatchesList().get(0).getBatchName(), "LCSET" + origLcsetSuffix);
        assertEquals(reworkTube.getMercurySamplesList().size(), 1);
        String reworkSampleKey = reworkTube.getMercurySamplesList().get(0).getSampleKey();

        // Starts the rework with a new rack of tubes and includes the rework tube.
        Map<String, TwoDBarcodedTube> reworkRackMap = createInitialRack(productOrder, reworkTubePrefix);
        assert(reworkRackMap.containsKey(reworkTubePrefix + reworkIdx));
        reworkRackMap.put(reworkTubePrefix + reworkIdx, reworkTube);
        for (TwoDBarcodedTube tube : reworkRackMap.values()) {
            workingBucket.addEntry(productOrder.getBusinessKey(), tube);
        }

        PicoPlatingEntityBuilder pplatingEntityBuilder2 = runPicoPlatingProcess(
                reworkRackMap,
                productOrder,
                new LabBatch("reworkBatch", new HashSet<LabVessel>(reworkRackMap.values()), LabBatch.LabBatchType.WORKFLOW),
                reworkLcsetSuffix,
                reworkRackBarcodeSuffix,
                "2");

        ExomeExpressShearingEntityBuilder shearingEntityBuilder2 = runExomeExpressShearingProcess(
                productOrder,
                pplatingEntityBuilder2.getNormBarcodeToTubeMap(),
                pplatingEntityBuilder2.getNormTubeFormation(),
                pplatingEntityBuilder2.getNormalizationBarcode(),
                "2");

        LibraryConstructionEntityBuilder lcEntityBuilder2 = runLibraryConstructionProcess(
                shearingEntityBuilder2.getShearingCleanupPlate(),
                shearingEntityBuilder2.getShearCleanPlateBarcode(),
                shearingEntityBuilder2.getShearingPlate(),
                "2");

        VesselContainer reworkContainer = (VesselContainer)lcEntityBuilder2.getPondRegRack().getContainerRole();

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
        assertEquals(origContainer.getAllLabBatches().size(), 2);
        validateLabBatchComposition(origContainer.getLabBatchCompositions(),
                NUM_POSITIONS_IN_RACK,
                new int[] {NUM_POSITIONS_IN_RACK, 1},
                new String[] {origLcsetSuffix, reworkLcsetSuffix});

        assertEquals(reworkContainer.getAllLabBatches().size(), 2);
        validateLabBatchComposition(reworkContainer.getLabBatchCompositions(),
                NUM_POSITIONS_IN_RACK,
                new int[] {NUM_POSITIONS_IN_RACK, 1},
                new String[] {reworkLcsetSuffix, origLcsetSuffix});

        // Rework tube should be in two lcsets.
        assertEquals(reworkTube.getAllLabBatches().size(), 2);

        // Checks the correct batch identifier for the rework tube, which depends on the end container.
        assert(reworkTube.getPluralityLabBatch(origContainer).getBatchName().endsWith(origLcsetSuffix));
        assert(reworkTube.getPluralityLabBatch(reworkContainer).getBatchName().endsWith(reworkLcsetSuffix));
    }


    private void validateLabBatchComposition(List<LabBatchComposition> composition, int denominator, int[] counts, String[] lcsetSuffixes) {
        assertEquals(composition.size(), counts.length);
        for (int idx = 0; idx < composition.size(); ++idx) {
            LabBatchComposition origComposition = composition.get(idx);
            assertEquals(origComposition.getDenominator(), denominator);
            assertEquals(origComposition.getCount(), counts[idx]);
            assertTrue(origComposition.getLabBatch().getBatchName().endsWith(lcsetSuffixes[idx]),
                    "Expected suffix " + lcsetSuffixes[idx] + " but got " + origComposition.getLabBatch().getBatchName());
        }
    }
}
