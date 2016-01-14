
package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Test(groups = TestGroups.DATABASE_FREE)
public class CreateFCTActionBeanTest {
    private CreateFCTActionBean testBean = new CreateFCTActionBean();
    private List<LabVessel> stbTubes = new ArrayList<>();
    private String eventDate = "today";
    private BigDecimal conc = new BigDecimal("7.0");

    @BeforeTest
    public void setup() {
        for (int i = 0; i < 32; ++i) {
            stbTubes.add(new BarcodedTube("stbTube" + i));
        }
    }

    @Test
    public void testAllocationOf32x1() {
        List<Pair<RowDto, LabVessel>> rowDtoLabVessels = new ArrayList<>();
        for (int i = 0; i < 32; ++i) {
            RowDto rowDto = new RowDto(stbTubes.get(i).getLabel(),
                    "lcset" + i,
                    eventDate,
                    "product" + i,
                    "startTube" + i,
                    "Denature",
                    conc);
            rowDto.setNumberLanes(1);
            rowDtoLabVessels.add(Pair.of(rowDto, stbTubes.get(i)));
        }
        for (IlluminaFlowcell.FlowcellType flowcellType : IlluminaFlowcell.FlowcellType.values()) {
            if (flowcellType.getCreateFct() == IlluminaFlowcell.CreateFct.NO) {
                continue;
            }
            List<Pair<LabBatch, Set<String>>> fctBatches = allocateAndTest(rowDtoLabVessels, flowcellType);
            checkLcsets(fctBatches);
        }
    }

    @Test
    public void testAllocationOf32x8() {
        List<Pair<RowDto, LabVessel>> rowDtoLabVessels = new ArrayList<>();
        for (int i = 0; i < 32; ++i) {
            RowDto rowDto = new RowDto(stbTubes.get(i).getLabel(),
                    "lcset" + i,
                    eventDate,
                    "product" + i,
                    "startTube" + i,
                    "Denature",
                    conc);
            rowDto.setNumberLanes(8);
            rowDtoLabVessels.add(Pair.of(rowDto, stbTubes.get(i)));
        }
        for (IlluminaFlowcell.FlowcellType flowcellType : IlluminaFlowcell.FlowcellType.values()) {
            if (flowcellType.getCreateFct() == IlluminaFlowcell.CreateFct.NO) {
                continue;
            }
            List<Pair<LabBatch, Set<String>>> fctBatches = allocateAndTest(rowDtoLabVessels, flowcellType);
            checkLcsets(fctBatches);
        }
    }

    @Test
    public void testAllocationOf2x2OneLane() {
        List<Pair<RowDto, LabVessel>> rowDtoLabVessels = new ArrayList<>();
        for (int i = 0; i < 2; ++i) {
            RowDto rowDto = new RowDto(stbTubes.get(i).getLabel(),
                    "lcset" + i,
                    eventDate,
                    "product" + i,
                    "startTube" + i,
                    "Denature",
                    conc);
            rowDto.setNumberLanes(2);
            rowDtoLabVessels.add(Pair.of(rowDto, stbTubes.get(i)));
        }
        List<Pair<LabBatch, Set<String>>> fctBatches = allocateAndTest(rowDtoLabVessels,
                IlluminaFlowcell.FlowcellType.MiSeqFlowcell);
        checkLcsets(fctBatches);
    }

    @Test
    public void testSharedLcsets() {
        List<Pair<RowDto, LabVessel>> rowDtoLabVessels = new ArrayList<>();
        int tubeIdx = 0;
        rowDtoLabVessels.add(Pair.of(new RowDto(stbTubes.get(tubeIdx).getLabel(), "lcset1", eventDate, "prod", "stube",
                "Denature", conc, 5), stbTubes.get(tubeIdx)));
        tubeIdx = 1;
        rowDtoLabVessels.add(Pair.of(new RowDto(stbTubes.get(tubeIdx).getLabel(), "lcset2", eventDate, "prod", "stube",
                "Denature", conc, 6), stbTubes.get(tubeIdx)));
        tubeIdx = 2;
        rowDtoLabVessels.add(Pair.of(new RowDto(stbTubes.get(tubeIdx).getLabel(), "lcset1", eventDate, "prod", "stube",
                "Denature", conc, 2), stbTubes.get(tubeIdx)));
        tubeIdx = 3;
        rowDtoLabVessels.add(Pair.of(new RowDto(stbTubes.get(tubeIdx).getLabel(), "lcset3", eventDate, "prod", "stube",
                "Denature", conc, 3), stbTubes.get(tubeIdx)));

        for (IlluminaFlowcell.FlowcellType flowcellType : IlluminaFlowcell.FlowcellType.values()) {
            if (flowcellType.getCreateFct() == IlluminaFlowcell.CreateFct.NO) {
                continue;
            }
            List<Pair<LabBatch, Set<String>>> fctBatches = allocateAndTest(rowDtoLabVessels, flowcellType);

            for (Pair<LabBatch, Set<String>> pair : fctBatches) {
                for (LabVessel labVessel : pair.getLeft().getStartingBatchLabVessels()) {
                    int foundTubeIdx = findIndexByName(stbTubes, labVessel);
                    switch (foundTubeIdx) {
                    case 0:
                    case 2:
                        Assert.assertTrue(pair.getRight().contains("lcset1"));
                        break;
                    case 1:
                        Assert.assertTrue(pair.getRight().contains("lcset2"));
                        break;
                    case 3:
                        Assert.assertTrue(pair.getRight().contains("lcset3"));
                        break;
                    default:
                        Assert.fail("Must not be here.");
                    }
                }
            }
        }
    }

    private List<Pair<LabBatch, Set<String>>> allocateAndTest(List<Pair<RowDto, LabVessel>> rowDtoLabVessels,
                                                              IlluminaFlowcell.FlowcellType flowcellType) {

        int expectedLaneCount = 0;
        List<String> expectedStartingVesselBarcodes = new ArrayList<>();
        for (Pair<RowDto, LabVessel> pair : rowDtoLabVessels) {
            expectedLaneCount += pair.getLeft().getNumberLanes();
            for (int i = 0; i < pair.getLeft().getNumberLanes(); ++i) {
                expectedStartingVesselBarcodes.add(pair.getLeft().getBarcode());
            }
        }

        int flowcellLaneCount = flowcellType.getVesselGeometry().getRowCount();
        Assert.assertEquals(expectedLaneCount % flowcellLaneCount, 0, "Bad test setup");
        int expectedBatchCount = expectedLaneCount / flowcellLaneCount;

        // Does the starting vessel to FCT allocation.
        List<Pair<LabBatch, Set<String>>> fctBatches = testBean.makeFctDaoFree(rowDtoLabVessels, flowcellType);

        Assert.assertEquals(fctBatches.size(), expectedBatchCount, flowcellType.getDisplayName());

        for (Pair<LabBatch, Set<String>> pair : fctBatches) {
            LabBatch fctBatch = pair.getLeft();
            Assert.assertEquals(fctBatch.getLabBatchStartingVessels().size(), flowcellLaneCount);

            for (LabBatchStartingVessel batchStartingVessel : fctBatch.getLabBatchStartingVessels()) {
                // Did we end up with same starting vessels?
                Assert.assertTrue(expectedStartingVesselBarcodes.remove(batchStartingVessel.getLabVessel().getLabel()),
                        "FCT starting vessel " + batchStartingVessel.getLabVessel().getLabel() + " is missing");
            }
            for (VesselPosition lane : flowcellType.getVesselGeometry().getVesselPositions()) {
                LabVessel laneVessel = fctBatch.getStartingVesselByPosition(lane);
                int foundIndex = findIndexByName(stbTubes, laneVessel);
                Assert.assertTrue(foundIndex >= 0);
            }
        }
        // Do the FCTs contain all the loading vessels?
        Assert.assertTrue(expectedStartingVesselBarcodes.size() == 0,
                "No FCT for tube(s) " + StringUtils.join(expectedStartingVesselBarcodes, ", "));

        return fctBatches;
    }

    private void checkLcsets(List<Pair<LabBatch, Set<String>>> fctBatches) {
        // Checks the linked lcsets
        for (Pair<LabBatch, Set<String>> pair : fctBatches) {
            LabBatch fctBatch = pair.getLeft();
            Set<String> lcsetNames = pair.getRight();

            Set<String> expectedLcsetNames = new HashSet<>();
            for (LabVessel startingVessel : fctBatch.getStartingBatchLabVessels()) {
                int foundIndex = findIndexByName(stbTubes, startingVessel);
                Assert.assertTrue(foundIndex >= 0);
                String name = "lcset" + foundIndex;
                expectedLcsetNames.add(name);
                Assert.assertTrue(lcsetNames.contains(name),
                        "Cannot find '" + name + "' in (" + StringUtils.join(lcsetNames, ", ") + ")");
            }
            Assert.assertTrue(expectedLcsetNames.containsAll(lcsetNames));
        }

    }

    private int findIndexByName(List<LabVessel> labVessels, LabVessel lookupVessel) {
        for (int i = 0; i < labVessels.size(); ++i) {
            if (labVessels.get(i).getLabel().equals(lookupVessel.getLabel())) {
                return i;
            }
        }
        return -1;
    }
}