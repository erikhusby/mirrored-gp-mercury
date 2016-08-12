
package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Test(groups = TestGroups.DATABASE_FREE)
public class CreateFCTActionBeanTest {
    private LabBatchEjb testBean = new LabBatchEjb();
    private List<LabVessel> stbTubes = new ArrayList<>();
    private BigDecimal conc = new BigDecimal("7.0");

    @BeforeTest
    public void setup() {
        for (int i = 0; i < 32; ++i) {
            stbTubes.add(new BarcodedTube("stbTube" + i));
        }
    }

    @Test
    public void testAllocationOf32x1() {
        Collection<Pair<LabBatchEjb.CreateFctDto, LabVessel>> dtoVessels = new ArrayList<>();
        for (int i = 0; i < 32; ++i) {
            LabVessel tube = stbTubes.get(i);
            dtoVessels.add(Pair.of(new LabBatchEjb.CreateFctDto(tube.getLabel(), "lcset" + i, conc, 1), tube));
        }
        for (IlluminaFlowcell.FlowcellType flowcellType : IlluminaFlowcell.FlowcellType.values()) {
            if (flowcellType.getCreateFct() == IlluminaFlowcell.CreateFct.NO) {
                continue;
            }
            Multimap<LabBatch, String> fctBatches = allocateAndTest(dtoVessels, flowcellType);
        }
    }

    @Test
    public void testAllocationOf32x8() {
        Collection<Pair<LabBatchEjb.CreateFctDto, LabVessel>> dtoVessels = new ArrayList<>();
        for (int i = 0; i < 32; ++i) {
            LabVessel tube = stbTubes.get(i);
            dtoVessels.add(Pair.of(new LabBatchEjb.CreateFctDto(tube.getLabel(), "lcset" + i, conc, 8), tube));
        }
        for (IlluminaFlowcell.FlowcellType flowcellType : IlluminaFlowcell.FlowcellType.values()) {
            if (flowcellType.getCreateFct() == IlluminaFlowcell.CreateFct.NO) {
                continue;
            }
            Multimap<LabBatch, String> fctBatches = allocateAndTest(dtoVessels, flowcellType);
        }
    }

    @Test
    public void testAllocationOf2x2OneLane() {
        Collection<Pair<LabBatchEjb.CreateFctDto, LabVessel>> dtoVessels = new ArrayList<>();
        for (int i = 0; i < 2; ++i) {
            LabVessel tube = stbTubes.get(i);
            dtoVessels.add(Pair.of(new LabBatchEjb.CreateFctDto(tube.getLabel(), "lcset" + i, conc, 2), tube));
        }
        Multimap<LabBatch, String> fctBatches = allocateAndTest(dtoVessels,
                IlluminaFlowcell.FlowcellType.MiSeqFlowcell);
    }

    @Test
    public void testSharedLcsets() {
        Collection<Pair<LabBatchEjb.CreateFctDto, LabVessel>> dtoVessels = new ArrayList<>();

        LabVessel tube = stbTubes.get(0);
        dtoVessels.add(Pair.of(new LabBatchEjb.CreateFctDto(tube.getLabel(), "lcset0", conc, 5), tube));

        tube = stbTubes.get(1);
        dtoVessels.add(Pair.of(new LabBatchEjb.CreateFctDto(tube.getLabel(), "lcset1", conc, 6), tube));

        tube = stbTubes.get(2);
        dtoVessels.add(Pair.of(new LabBatchEjb.CreateFctDto(tube.getLabel(), "lcset2", conc, 2), tube));

        tube = stbTubes.get(3);
        dtoVessels.add(Pair.of(new LabBatchEjb.CreateFctDto(tube.getLabel(), "lcset3", conc, 3), tube));

        for (IlluminaFlowcell.FlowcellType flowcellType : IlluminaFlowcell.FlowcellType.values()) {
            if (flowcellType.getCreateFct() == IlluminaFlowcell.CreateFct.NO) {
                continue;
            }
            Multimap<LabBatch, String> fctBatches = allocateAndTest(dtoVessels, flowcellType);
        }
    }

    private Multimap<LabBatch, String> allocateAndTest(Collection<Pair<LabBatchEjb.CreateFctDto, LabVessel>> dtoVessels,
                                                       IlluminaFlowcell.FlowcellType flowcellType) {

        int expectedLaneCount = 0;
        List<String> expectedBarcodeOnEachLane = new ArrayList<>();
        for (Pair<LabBatchEjb.CreateFctDto, LabVessel> pair : dtoVessels) {
            expectedLaneCount += pair.getLeft().getNumberLanes();
            for (int i = 0; i < pair.getLeft().getNumberLanes(); ++i) {
                expectedBarcodeOnEachLane.add(pair.getRight().getLabel());
            }
        }
        List<String> expectedBatchStartingVesselBarcodes = new ArrayList<>(expectedBarcodeOnEachLane);

        int flowcellLaneCount = flowcellType.getVesselGeometry().getRowCount();
        Assert.assertEquals(expectedLaneCount % flowcellLaneCount, 0, "Bad test setup");
        int expectedBatchCount = expectedLaneCount / flowcellLaneCount;

        // Does the starting vessel to FCT allocation.
        Multimap<LabBatch, String> fctBatches = testBean.makeFctDaoFree(dtoVessels, flowcellType);

        // Is the number of FCTs correct?
        Assert.assertEquals(fctBatches.size(), expectedBatchCount, flowcellType.getDisplayName());

        for (LabBatch fctBatch : fctBatches.keys()) {
            Assert.assertEquals(fctBatch.getLabBatchStartingVessels().size(), flowcellLaneCount);

            for (LabBatchStartingVessel batchStartingVessel : fctBatch.getLabBatchStartingVessels()) {
                // Did batches end up with correct starting vessels?
                String barcode = batchStartingVessel.getLabVessel().getLabel();
                Assert.assertTrue(expectedBatchStartingVesselBarcodes.remove(barcode),
                        "FCT batch has unexpected batch starting vessel " + barcode);
            }
            for (VesselPosition lane : flowcellType.getVesselGeometry().getVesselPositions()) {
                // Did flowcell vessels end up with correct starting vessels?
                LabVessel laneVessel = fctBatch.getStartingVesselByPosition(lane);
                String barcode = laneVessel.getLabel();
                Assert.assertTrue(expectedBarcodeOnEachLane.remove(barcode),
                        "Flowcell has unexpected batch starting vessel " + barcode);
            }
        }
        // Did all loading tube lanes get into the batches?
        Assert.assertEquals(expectedBatchStartingVesselBarcodes.size(), 0,
                "FCTs should have had " + StringUtils.join(expectedBatchStartingVesselBarcodes, ", "));
        // Did all loading tube lanes get onto flowcell vessels?
        Assert.assertTrue(expectedBarcodeOnEachLane.size() == 0,
                "Flowcell vessel should have had " + StringUtils.join(expectedBarcodeOnEachLane, ", "));

        return fctBatches;
    }
}
