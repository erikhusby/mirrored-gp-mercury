
package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationDto;
import org.broadinstitute.gpinformatics.mercury.presentation.run.FctDto;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb.laneToLinkedLcsets;

@Test(groups = TestGroups.DATABASE_FREE)
public class CreateFCTActionBeanTest {
    private LabBatchEjb testBean = new LabBatchEjb();
    private final Map<String, LabVessel> loadingTubes = new HashMap<>();
    private List<LabVessel> stbTubes = new ArrayList<LabVessel>() {{
        for (int i = 0; i < 32; ++i) {
            LabVessel tube = new BarcodedTube("stbTube" + i);
            loadingTubes.put(tube.getLabel(), tube);
            add(tube);
        }
    }};
    private BigDecimal conc = new BigDecimal("7.0");
    private LabBatch lcset = new LabBatch("LB1234", new HashSet<>(stbTubes), Collections.EMPTY_SET,
            LabBatch.LabBatchType.WORKFLOW, "workflowName", "desc", new Date(), "");

    @Test
    public void testCreateFct32x1() {
        Set<String> expectedLcsets = new HashSet<>();
        List<CreateFctDto> dtos = new ArrayList<>();
        for (int i = 0; i < 32; ++i) {
            LabVessel tube = stbTubes.get(i);
            CreateFctDto dto = new CreateFctDto(tube.getLabel(), "lcset" + i, conc, 1);
            dtos.add(dto);
            //  Tests the split/join in the getter/setter.
            dto.setProduct("product 1" + DesignationDto.DELIMITER + "product 2");
            Assert.assertEquals(dto.getProduct(), "product 1" + DesignationDto.DELIMITER + "product 2");
            dto.setProduct("product 3");
            Assert.assertEquals(dto.getProduct(), "product 3");
            expectedLcsets.add("lcset" + i);
        }
        int laneCount = 0;
        for (CreateFctDto dto : dtos) {
            laneCount += dto.getNumberLanes();
        }

        for (IlluminaFlowcell.FlowcellType flowcellType : IlluminaFlowcell.FlowcellType.values()) {
            if (flowcellType.getCreateFct() == IlluminaFlowcell.CreateFct.NO) {
                continue;
            }
            Assert.assertTrue(CollectionUtils.isEmpty(allocateAndTest(dtos, loadingTubes, flowcellType, null,
                    expectedLcsets, laneCount / flowcellType.getVesselGeometry().getRowCount(), false).getRight()));
            // All are allocated.
            for (CreateFctDto dto : dtos) {
                Assert.assertTrue(dto.getAllocatedLanes() == dto.getNumberLanes());
            }
        }
    }

    @Test
    public void testCreateFct1x32() {
        CreateFctDto dto = new CreateFctDto("stbTube0", "lcset0", conc, 32);
        for (IlluminaFlowcell.FlowcellType flowcellType : IlluminaFlowcell.FlowcellType.values()) {
            if (flowcellType.getCreateFct() == IlluminaFlowcell.CreateFct.NO) {
                continue;
            }
            Assert.assertTrue(CollectionUtils.isEmpty(allocateAndTest(Collections.singletonList(dto),
                    loadingTubes, flowcellType, null, Collections.singleton("lcset0"),
                    32 / flowcellType.getVesselGeometry().getRowCount(), false).getRight()));
            // All are allocated.
            Assert.assertTrue(dto.getAllocatedLanes() == dto.getNumberLanes());
        }
    }

    @Test
    public void testCreateFct1x32diverse() {
        CreateFctDto dto = new CreateFctDto("stbTube0", "lcset0", conc, 32);
        for (IlluminaFlowcell.FlowcellType flowcellType : IlluminaFlowcell.FlowcellType.values()) {
            if (flowcellType.getCreateFct() == IlluminaFlowcell.CreateFct.NO) {
                continue;
            }
            // If flowcell size is 1 or 2 there will be flowcells made, otherwise none.
            int flowcellSize = flowcellType.getVesselGeometry().getRowCount();
            int flowcellCount = (flowcellSize > 2) ? 0 : (32 / flowcellSize);

            Pair<List<String>, List<String>> pair = allocateAndTest(Collections.singletonList(dto),
                    loadingTubes, flowcellType, null, null, flowcellCount, true);
            // None, or all allocated.
            Assert.assertEquals(dto.getAllocatedLanes(), flowcellCount == 0 ? 0 : dto.getNumberLanes());
            Assert.assertEquals(pair.getRight().size(), flowcellCount == 0 ? 32 : 0);
            while (pair.getLeft().size() > 0) {
                Assert.assertTrue(pair.getLeft().remove(flowcellSize == 1 ? "stbTube0" : "stbTube0 stbTube0"));
            }
        }
    }

    @Test
    public void testCreateFct32x8() {
        Set<String> expectedLcsets = new HashSet<>();
        List<CreateFctDto> dtos = new ArrayList<>();
        for (int i = 0; i < 32; ++i) {
            LabVessel tube = stbTubes.get(i);
            dtos.add(new CreateFctDto(tube.getLabel(), "lcset" + i, conc, 8));
            expectedLcsets.add("lcset" + i);
        }

        int laneCount = 0;
        for (CreateFctDto dto : dtos) {
            laneCount += dto.getNumberLanes();
        }
        for (IlluminaFlowcell.FlowcellType flowcellType : IlluminaFlowcell.FlowcellType.values()) {
            if (flowcellType.getCreateFct() == IlluminaFlowcell.CreateFct.NO) {
                continue;
            }
            Assert.assertTrue(CollectionUtils.isEmpty(allocateAndTest(dtos, loadingTubes, flowcellType, null,
                    expectedLcsets, laneCount / flowcellType.getVesselGeometry().getRowCount(), false).getRight()));
            // All are allocated.
            for (CreateFctDto dto : dtos) {
                Assert.assertTrue(dto.getAllocatedLanes() == dto.getNumberLanes());
            }
        }
    }

    @Test
    public void testDesignationOf32x8() {
        Set<String> expectedLcsets = new HashSet<>();
        List<DesignationDto> dtos = new ArrayList<>();
        for (int i = 0; i < 32; ++i) {
            LabVessel tube = stbTubes.get(i);

            DesignationDto dto = new DesignationDto();
            dto.setDesignationId((long) i);
            dto.setBarcode(tube.getLabel());
            dto.setIndexType(FlowcellDesignation.IndexType.DUAL);
            dto.setLcset("lcset" + i / 9);
            dto.setNumberLanes(8);
            dto.setPairedEndRead(true);
            dto.setReadLength(151);
            dto.setRegulatoryDesignation("RESEARCH");
            dto.setSelected(true);
            // All are 8 lane flowcells.
            dto.setSequencerModel(Arrays.asList(IlluminaFlowcell.FlowcellType.HiSeqFlowcell,
                    IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell,
                    IlluminaFlowcell.FlowcellType.HiSeqX10Flowcell,
                    IlluminaFlowcell.FlowcellType.HiSeqFlowcell).get(i / 8));
            dto.setStatus(FlowcellDesignation.Status.QUEUED);

            //  Tests the split/join in the getter/setter.
            dto.setProductNames(Arrays.asList("product 1", "product 2"));
            Assert.assertEquals(dto.getProduct(), "product 1" + DesignationDto.DELIMITER + "product 2");
            dto.setProduct("product 3");
            Assert.assertEquals(dto.getProduct(), "product 3");
            expectedLcsets.add(dto.getLcset());

            dtos.add(dto);
        }

        Pair<List<String>, List<String>> pair = allocateAndTest(dtos, loadingTubes, null, null, expectedLcsets,
                32, false);

        // All are allocated.
        Assert.assertTrue(pair.getRight().isEmpty());

        // Spot checks the lane layouts.
        Assert.assertTrue(pair.getLeft().
                remove("stbTube0 stbTube0 stbTube0 stbTube0 stbTube0 stbTube0 stbTube0 stbTube0"));
        Assert.assertTrue(pair.getLeft().
                remove("stbTube1 stbTube1 stbTube1 stbTube1 stbTube1 stbTube1 stbTube1 stbTube1"));
        Assert.assertTrue(pair.getLeft().
                remove("stbTube31 stbTube31 stbTube31 stbTube31 stbTube31 stbTube31 stbTube31 stbTube31"));

    }

    @Test
    public void testDesignationOf1x32() {
        DesignationDto dto = new DesignationDto();
        dto.setDesignationId(1L);
        dto.setBarcode("stbTube0");
        dto.setIndexType(FlowcellDesignation.IndexType.DUAL);
        dto.setLcset("lcset0");
        dto.setNumberLanes(32);
        dto.setPairedEndRead(true);
        dto.setReadLength(151);
        dto.setRegulatoryDesignation("RESEARCH");
        dto.setSelected(true);
        dto.setStatus(FlowcellDesignation.Status.QUEUED);

        for (IlluminaFlowcell.FlowcellType flowcellType : Arrays.asList(IlluminaFlowcell.FlowcellType.HiSeqFlowcell,
                IlluminaFlowcell.FlowcellType.MiSeqFlowcell,
                IlluminaFlowcell.FlowcellType.NovaSeqFlowcell,
                IlluminaFlowcell.FlowcellType.NovaSeqS4Flowcell,
                IlluminaFlowcell.FlowcellType.NovaSeqSPFlowcell)) {

            dto.setSequencerModel(flowcellType);

            // All are allocated.
            Assert.assertTrue(allocateAndTest(Collections.singletonList(dto), loadingTubes, null, null, null,
                    32 / flowcellType.getVesselGeometry().getRowCount(), false).getRight().isEmpty());

            // For diverse samples, when flowcell size is 1 or 2 there will be flowcells made, otherwise none.
            int flowcellSize = flowcellType.getVesselGeometry().getRowCount();
            int flowcellCount = (flowcellSize > 2) ? 0 : (32 / flowcellSize);

            Pair<List<String>, List<String>> pair = allocateAndTest(Collections.singletonList(dto),
                    loadingTubes, null, null, null, flowcellCount, true);

            // None, or all allocated.
            Assert.assertEquals(dto.getAllocatedLanes(), flowcellCount == 0 ? 0 : dto.getNumberLanes());
            Assert.assertEquals(pair.getRight().size(), flowcellCount == 0 ? 32 : 0);
        }
    }

    @Test
    public void testDesignationPriority9A1() {
        Set<String> expectedLcsets = new HashSet<>();
        // Makes 3 designations having numberLanes of 1, 9, 10, prioritized as 9, then 10 and 1.
        // Should make two flowcells of 8 lanes.
        // 9 lane dto should be fully allocated, 10 should get split, and 1 should be unallocated.
        List<DesignationDto> dtos = new ArrayList<>();

        int[] numberLanes = {1, 9, 10};
        for (int idx = 0; idx < numberLanes.length; ++idx) {
            LabVessel tube = stbTubes.get(idx);

            DesignationDto dto = new DesignationDto();
            dto.setDesignationId((long) idx);
            dto.setBarcode(tube.getLabel());
            dto.setIndexType(FlowcellDesignation.IndexType.DUAL);
            dto.setLcset("lcset" + idx / 3);
            dto.setNumberLanes(numberLanes[idx]);
            dto.setPairedEndRead(true);
            dto.setReadLength(151);
            dto.setRegulatoryDesignation("RESEARCH");
            dto.setSelected(true);
            dto.setSequencerModel(IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell);
            dto.setStatus(FlowcellDesignation.Status.QUEUED);
            // Changes priority of the 9 lane dto.
            if (dto.getNumberLanes() == 9) {
                dto.setPriority(FlowcellDesignation.Priority.HIGH);
            }
            expectedLcsets.add(dto.getLcset());
            dtos.add(dto);
        }

        // dto[2] should be split into 7 lanes (allocated) and 3 lanes (unallocated).
        DesignationDto splitDto = new DesignationDto();
        splitDto.setBarcode(stbTubes.get(2).getLabel());
        splitDto.setSequencerModel(IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell);
        splitDto.setNumberLanes(3);
        splitDto.setStatus(FlowcellDesignation.Status.QUEUED);

        Pair<List<String>, List<String>> pair = allocateAndTest(dtos, loadingTubes, null,
                Collections.singletonList(splitDto), expectedLcsets, 2, false);

        // Checks the dto that got split.
        Assert.assertEquals(dtos.get(2).getNumberLanes().intValue(), 7);

        // Checks dto allocations.
        Assert.assertFalse(dtos.get(0).getAllocatedLanes() == dtos.get(0).getNumberLanes());
        Assert.assertTrue(dtos.get(1).getAllocatedLanes() == dtos.get(1).getNumberLanes());
        Assert.assertTrue(dtos.get(2).getAllocatedLanes() == dtos.get(2).getNumberLanes());

        Assert.assertTrue(pair.getRight().remove(stbTubes.get(2).getLabel()));
        Assert.assertTrue(pair.getRight().remove(stbTubes.get(2).getLabel()));
        Assert.assertTrue(pair.getRight().remove(stbTubes.get(2).getLabel()));
        Assert.assertTrue(pair.getRight().remove(stbTubes.get(0).getLabel()));
        Assert.assertTrue(pair.getRight().isEmpty());

        // Checks the lane layout
        Assert.assertTrue(pair.getLeft().
                remove("stbTube1 stbTube1 stbTube1 stbTube1 stbTube1 stbTube1 stbTube1 stbTube1"));
        Assert.assertTrue(pair.getLeft().
                remove("stbTube1 stbTube2 stbTube2 stbTube2 stbTube2 stbTube2 stbTube2 stbTube2"));
        Assert.assertTrue(pair.getLeft().isEmpty());
    }

    @Test
    public void testDesignationGrouping() {
        // Makes 3 designations and 1 FCT.
        // WGS is not restricted to group by regulatory designation, but ExEx is.
        // dto[0] has stb0 = RESEARCH & ExEx
        // dto[1] has stb1 = CLINICAL & WGS   can combine with stb0 or stb2; stb0 is expected due to barcode order
        // dto[2] has stb2 = CLINICAL & ExEx  expected to be unallocated

        List<DesignationDto> dtos = new ArrayList<>();

        for (int idx = 0; idx < 3; ++idx) {
            LabVessel tube = stbTubes.get(idx);

            DesignationDto dto = new DesignationDto();
            dto.setDesignationId((long) idx);
            dto.setBarcode(tube.getLabel());
            dto.setIndexType(FlowcellDesignation.IndexType.DUAL);
            dto.setLcset("lcset" + idx);
            dto.setNumberLanes(new int[]{4, 4, 4}[idx]);
            dto.setPairedEndRead(true);
            // Set groupBy to simulate WGS and ExEx behavior.
            dto.setGroupByRegulatoryDesignation(new boolean[]{true, false, true}[idx]);
            dto.setReadLength(151);
            dto.setRegulatoryDesignation(Arrays.asList("RESEARCH", "CLINICAL", "CLINICAL").get(idx));
            dto.setSelected(true);
            dto.setSequencerModel(IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell);
            dto.setStatus(FlowcellDesignation.Status.QUEUED);
            dtos.add(dto);
        }

        Pair<List<String>, List<String>> pair = allocateAndTest(dtos, loadingTubes, null, null, null, 1, false);

        Assert.assertTrue(dtos.get(0).getAllocatedLanes() == dtos.get(0).getNumberLanes());
        Assert.assertTrue(dtos.get(1).getAllocatedLanes() == dtos.get(1).getNumberLanes());
        Assert.assertFalse(dtos.get(2).getAllocatedLanes() == dtos.get(2).getNumberLanes());


        Assert.assertTrue(pair.getRight().remove(stbTubes.get(2).getLabel()));
        Assert.assertTrue(pair.getRight().remove(stbTubes.get(2).getLabel()));
        Assert.assertTrue(pair.getRight().remove(stbTubes.get(2).getLabel()));
        Assert.assertTrue(pair.getRight().remove(stbTubes.get(2).getLabel()));
        Assert.assertTrue(pair.getRight().isEmpty());

        // Checks the lane layout
        Assert.assertTrue(pair.getLeft().
                remove("stbTube0 stbTube0 stbTube0 stbTube0 stbTube1 stbTube1 stbTube1 stbTube1"));
        Assert.assertTrue(pair.getLeft().isEmpty());
    }

    @Test
    public void testDesignationGrouping2() {
        // Makes 3 designations and 1 FCT.
        // WGS is not restricted to group by regulatory designation, but ExEx is.
        // dto[0] has stb0 = CLINICAL & ExEx
        // dto[1] has stb2 = RESEARCH & ExEx  expected to be unallocated
        // dto[2] has stb1 = CLINICAL & WGS   can combine with stb0 or stb2; stb0 is expected due to barcode order

        List<DesignationDto> dtos = new ArrayList<>();

        for (int idx = 0; idx < 3; ++idx) {
            LabVessel tube = Arrays.asList(stbTubes.get(0), stbTubes.get(2), stbTubes.get(1)).get(idx);

            DesignationDto dto = new DesignationDto();
            dto.setDesignationId((long) idx);
            dto.setBarcode(tube.getLabel());
            dto.setIndexType(FlowcellDesignation.IndexType.DUAL);
            dto.setLcset("lcset" + idx);
            dto.setNumberLanes(new int[]{4, 4, 4}[idx]);
            dto.setPairedEndRead(true);
            // Set groupBy to simulate WGS and ExEx behavior.
            dto.setGroupByRegulatoryDesignation(new boolean[]{true, true, false}[idx]);
            dto.setReadLength(151);
            dto.setRegulatoryDesignation(Arrays.asList("CLINICAL", "RESEARCH", "CLINICAL").get(idx));
            dto.setSelected(true);
            dto.setSequencerModel(IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell);
            dto.setStatus(FlowcellDesignation.Status.QUEUED);
            dtos.add(dto);
        }

        Pair<List<String>, List<String>> pair = allocateAndTest(dtos, loadingTubes, null, null, null, 1, false);

        Assert.assertTrue(dtos.get(0).getAllocatedLanes() == dtos.get(0).getNumberLanes());
        Assert.assertFalse(dtos.get(1).getAllocatedLanes() == dtos.get(1).getNumberLanes());
        Assert.assertTrue(dtos.get(2).getAllocatedLanes() == dtos.get(2).getNumberLanes());

        Assert.assertTrue(pair.getRight().remove("stbTube2"));
        Assert.assertTrue(pair.getRight().remove("stbTube2"));
        Assert.assertTrue(pair.getRight().remove("stbTube2"));
        Assert.assertTrue(pair.getRight().remove("stbTube2"));
        Assert.assertEquals(pair.getRight().size(), 0);

        // Checks the lane layout
        Assert.assertTrue(pair.getLeft().
                remove("stbTube0 stbTube0 stbTube0 stbTube0 stbTube1 stbTube1 stbTube1 stbTube1"));
        Assert.assertTrue(pair.getLeft().isEmpty());
    }

    @Test
    public void testDesignation21() {
        Set<String> expectedLcsets = new HashSet<>();
        // Makes 2 designations having numberLanes of 2 and 1, neither of which will be allocated
        // since they won't fill an 8 lane flowcell.

        List<DesignationDto> dtos = new ArrayList<>();
        for (int i = 0; i < 2; ++i) {
            LabVessel tube = stbTubes.get(i);

            DesignationDto dto = new DesignationDto();
            dto.setDesignationId((long) i);
            dto.setBarcode(tube.getLabel());
            dto.setIndexType(FlowcellDesignation.IndexType.DUAL);
            dto.setLcset("lcset100");
            dto.setNumberLanes(2 - i);
            dto.setPairedEndRead(true);
            dto.setReadLength(151);
            dto.setRegulatoryDesignation("RESEARCH");
            dto.setSelected(true);
            dto.setSequencerModel(IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell);
            dto.setStatus(FlowcellDesignation.Status.QUEUED);
            dtos.add(dto);
        }

        List<String> unallocated = allocateAndTest(dtos, loadingTubes, null, null, expectedLcsets, 0, false).
                getRight();

        Assert.assertTrue(unallocated.remove("stbTube0"));
        Assert.assertTrue(unallocated.remove("stbTube0"));
        Assert.assertTrue(unallocated.remove("stbTube1"));
        Assert.assertEquals(unallocated.size(), 0);
    }

    @Test
    public void testAllocationOf2x2() {
        Set<String> expectedLcsets = new HashSet<>();
        List<CreateFctDto> dtos = new ArrayList<>();
        for (int i = 0; i < 2; ++i) {
            LabVessel tube = stbTubes.get(i);
            dtos.add(new CreateFctDto(tube.getLabel(), "lcset" + i, conc, 2));
            expectedLcsets.add("lcset" + i);
        }

        int laneCount = 0;
        for (CreateFctDto dto : dtos) {
            laneCount += dto.getNumberLanes();
        }

        Assert.assertEquals(allocateAndTest(dtos, loadingTubes, IlluminaFlowcell.FlowcellType.MiSeqFlowcell,
                null, expectedLcsets,
                laneCount / IlluminaFlowcell.FlowcellType.MiSeqFlowcell.getVesselGeometry().getRowCount(), false).
                getRight().size(), 0);
    }

    @Test
    public void testSharedLcsets() {
        Set<String> expectedLcsets = new HashSet<>();
        List<CreateFctDto> dtos = new ArrayList<>();
        LabVessel tube = stbTubes.get(0);
        dtos.add(new CreateFctDto(tube.getLabel(), "lcset0", conc, 5));
        expectedLcsets.add("lcset0");

        tube = stbTubes.get(1);
        dtos.add(new CreateFctDto(tube.getLabel(), "lcset1", conc, 6));
        expectedLcsets.add("lcset1");

        tube = stbTubes.get(2);
        dtos.add(new CreateFctDto(tube.getLabel(), "lcset2", conc, 2));
        expectedLcsets.add("lcset2");

        tube = stbTubes.get(3);
        dtos.add(new CreateFctDto(tube.getLabel(), "lcset3", conc, 3));
        expectedLcsets.add("lcset3");

        for (IlluminaFlowcell.FlowcellType flowcellType : IlluminaFlowcell.FlowcellType.values()) {
            if (flowcellType.getCreateFct() == IlluminaFlowcell.CreateFct.NO) {
                continue;
            }
            Assert.assertTrue(CollectionUtils.isEmpty(allocateAndTest(dtos, loadingTubes, flowcellType, null,
                    expectedLcsets, 16 / flowcellType.getVesselGeometry().getRowCount(), false).getRight()));
        }
    }

    @Test
    public void testMixedLaneCount() {
        int[][] numberLanes = {{1, 3}, {1, 15}, {3}};
        int testIdx = 0;
        for (IlluminaFlowcell.FlowcellType flowcellType : Arrays.asList(
                IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell,
                IlluminaFlowcell.FlowcellType.HiSeqX10Flowcell,
                IlluminaFlowcell.FlowcellType.MiSeqFlowcell
        )) {
            Set<String> expectedLcsets = new HashSet<>();
            List<CreateFctDto> dtos = new ArrayList<>();
            for (int i = 0; i < numberLanes[testIdx].length; ++i) {
                LabVessel tube = stbTubes.get(i);
                dtos.add(new CreateFctDto(tube.getLabel(), "lcset" + i, conc, numberLanes[testIdx][i]));
                expectedLcsets.add("lcset" + i);
            }

            int laneCount = 0;
            for (CreateFctDto dto : dtos) {
                laneCount += dto.getNumberLanes();
            }
            Pair<List<String>, List<String>> pair = allocateAndTest(dtos, loadingTubes, flowcellType, null,
                    expectedLcsets, laneCount / flowcellType.getVesselGeometry().getRowCount(), false);

            // All lanes get allocated.
            Assert.assertTrue(pair.getRight().isEmpty());

            if (testIdx == 1) {
                Assert.assertTrue(pair.getLeft().
                        remove("stbTube1 stbTube1 stbTube1 stbTube1 stbTube1 stbTube1 stbTube1 stbTube1"));
                Assert.assertTrue(pair.getLeft().
                        remove("stbTube1 stbTube1 stbTube1 stbTube1 stbTube1 stbTube1 stbTube1 stbTube0"));
                Assert.assertTrue(pair.getLeft().isEmpty());
            }

            ++testIdx;
        }
    }

    @Test
    public void testCycles() {
        DesignationDto dto = new DesignationDto();
        dto.setDesignationId(1L);
        dto.setBarcode(stbTubes.get(0).getLabel());
        dto.setIndexType(FlowcellDesignation.IndexType.DUAL);
        dto.setLcset("lcset100");
        dto.setNumberLanes(2);
        dto.setPairedEndRead(true);
        dto.setReadLength(151);
        dto.setRegulatoryDesignation("RESEARCH");
        dto.setSelected(true);
        dto.setSequencerModel(IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell);
        dto.setStatus(FlowcellDesignation.Status.QUEUED);

        Assert.assertEquals(dto.calculateCycles(), 318);

        dto.setPairedEndRead(false);
        Assert.assertEquals(dto.calculateCycles(), 167);

        dto.setIndexType(FlowcellDesignation.IndexType.SINGLE);
        Assert.assertEquals(dto.calculateCycles(), 159);

        dto.setReadLength(null);
        Assert.assertEquals(dto.calculateCycles(), 8);

        dto.setIndexType(null);
        Assert.assertEquals(dto.calculateCycles(), 0);
    }

    @Test
    public void testMixedRegulatory() {
        List<DesignationDto> dtos = new ArrayList<>();
        for (int i = 0; i < 4; ++i) {
            DesignationDto dto = new DesignationDto();
            dto.setDesignationId((long)i);
            dto.setBarcode(stbTubes.get(i).getLabel());
            dto.setGroupByRegulatoryDesignation(true);
            dto.setIndexType(FlowcellDesignation.IndexType.DUAL);
            dto.setLcset("lcset100");
            dto.setNumberLanes(1);
            dto.setPairedEndRead(true);
            dto.setReadLength(151);
            dto.setRegulatoryDesignation("RESEARCH");
            dto.setSelected(true);
            // 2 lane flowcells.
            dto.setSequencerModel(IlluminaFlowcell.FlowcellType.NovaSeqFlowcell);
            dto.setStatus(FlowcellDesignation.Status.QUEUED);
            dtos.add(dto);
        }

        List<DesignationDto> group = new ArrayList<>();
        for (DesignationDto dto : dtos) {
            Assert.assertTrue(dto.isCompatible(group), dto.getBarcode());
            group.add(dto);
        }

        Assert.assertEquals(allocateAndTest(dtos, loadingTubes, null, null, null, 2, false).getRight().size(), 0);

        // Make an outlier.
        DesignationDto outlier = dtos.get(3);
        outlier.setRegulatoryDesignation("CLINICAL");
        group.clear();
        for (DesignationDto dto : dtos) {
            if (dto == outlier) {
                Assert.assertFalse(dto.isCompatible(group));
            } else {
                Assert.assertTrue(dto.isCompatible(group));
                group.add(dto);
            }
        }

        // Set ignore regulatory designation and the dto should be compatible with all others.
        dtos.get(0).setGroupByRegulatoryDesignation(false);
        for (DesignationDto dto : dtos) {
            Assert.assertTrue(dtos.get(0).isCompatible(Collections.singleton(dto)));
        }

        // Set ignore regulatory designation on the outlier and all dtos should be compatible.
        dtos.get(0).setGroupByRegulatoryDesignation(true);
        outlier.setGroupByRegulatoryDesignation(false);
        group.clear();
        for (DesignationDto dto : dtos) {
            Assert.assertTrue(dto.isCompatible(group));
            group.add(dto);
        }
    }

    @Test
    public void testCreateFctDiverse() {
        List<FctDto> dtos = Arrays.asList(
                new CreateFctDto("stbTube0", "lcset0", conc, 2),
                new CreateFctDto("stbTube1", "lcset0", conc, 2),
                new CreateFctDto("stbTube2", "lcset0", conc, 2),
                new CreateFctDto("stbTube3", "lcset0", conc, 2));

        Pair<List<String>, List<String>> pair = allocateAndTest(dtos, loadingTubes,
                IlluminaFlowcell.FlowcellType.NovaSeqS4Flowcell, null,
                Collections.singleton("lcset0"), 2, true);

        // All lanes get allocated.
        Assert.assertTrue(pair.getRight().isEmpty());

        // Checks the lane layout
        Assert.assertTrue(pair.getLeft().remove("stbTube0 stbTube1 stbTube2 stbTube3"));
        Assert.assertTrue(pair.getLeft().remove("stbTube0 stbTube1 stbTube2 stbTube3"));
        Assert.assertTrue(pair.getLeft().isEmpty());
    }

    @Test
    public void testCreateFctMinimalDiverse() {
        List<FctDto> dtos = Arrays.asList(
                new CreateFctDto("stbTube0", "lcset0", conc, 2),
                new CreateFctDto("stbTube1", "lcset0", conc, 2));

        Pair<List<String>, List<String>> pair = allocateAndTest(dtos, loadingTubes,
                IlluminaFlowcell.FlowcellType.NovaSeqS4Flowcell, null,
                Collections.singleton("lcset0"), 1, true);

        // All lanes get allocated.
        Assert.assertTrue(pair.getRight().isEmpty());

        // Checks the lane layout
        Assert.assertTrue(pair.getLeft().remove("stbTube0 stbTube1 stbTube0 stbTube1"));
        Assert.assertTrue(pair.getLeft().isEmpty());
    }

    @Test
    public void testCreateFctDiverseLongList() {
        List<FctDto> dtos = Arrays.asList(
                new CreateFctDto("stbTube10", "lcset0", conc, 2),
                new CreateFctDto("stbTube11", "lcset0", conc, 2),
                new CreateFctDto("stbTube12", "lcset0", conc, 2),
                new CreateFctDto("stbTube13", "lcset0", conc, 2),
                new CreateFctDto("stbTube14", "lcset0", conc, 2),
                new CreateFctDto("stbTube15", "lcset0", conc, 2),
                new CreateFctDto("stbTube16", "lcset0", conc, 2),
                new CreateFctDto("stbTube17", "lcset0", conc, 2),
                new CreateFctDto("stbTube18", "lcset0", conc, 2),
                new CreateFctDto("stbTube19", "lcset0", conc, 2),
                new CreateFctDto("stbTube20", "lcset0", conc, 2),
                new CreateFctDto("stbTube21", "lcset0", conc, 2));

        Pair<List<String>, List<String>> pair = allocateAndTest(dtos, loadingTubes,
                IlluminaFlowcell.FlowcellType.NovaSeqS4Flowcell, null,
                Collections.singleton("lcset0"), 6, true);

        // All lanes get allocated.
        Assert.assertTrue(pair.getRight().isEmpty());

        // Checks the lane layout
        Assert.assertTrue(pair.getLeft().remove("stbTube10 stbTube11 stbTube12 stbTube13"));
        Assert.assertTrue(pair.getLeft().remove("stbTube10 stbTube11 stbTube12 stbTube13"));
        Assert.assertTrue(pair.getLeft().remove("stbTube14 stbTube15 stbTube16 stbTube17"));
        Assert.assertTrue(pair.getLeft().remove("stbTube14 stbTube15 stbTube16 stbTube17"));
        Assert.assertTrue(pair.getLeft().remove("stbTube18 stbTube19 stbTube20 stbTube21"));
        Assert.assertTrue(pair.getLeft().remove("stbTube18 stbTube19 stbTube20 stbTube21"));
        Assert.assertTrue(pair.getLeft().isEmpty());
    }

    @Test
    public void testCreateFctDiverseFail() {
        List<FctDto> dtos = Arrays.asList(
                new CreateFctDto("stbTube0", "lcset0", conc, 3),
                new CreateFctDto("stbTube1", "lcset0", conc, 2));
        try {
            allocateAndTest(dtos, loadingTubes, IlluminaFlowcell.FlowcellType.NovaSeqS4Flowcell, null,
                    Collections.singleton("lcset0"), 1, true);
            Assert.fail("should have thrown");
        } catch (Exception e) {
            // expected
        }
    }

    /** Tests sample diversity's maximum samples per flowcell. */
    @Test
    public void testDesignationSplit29ABdiverse() {
        // Makes 4 designations but can only allocate one 8 lane flowcell due to sample diversity (2 lanes per sample).
        List<DesignationDto> dtos = new ArrayList<>();
        int[] numberLanes = {2, 9, 10, 11};
        final String lcset = "lcset1";

        for (int idx = 0; idx < numberLanes.length; ++idx) {
            LabVessel tube = stbTubes.get(idx);

            DesignationDto dto = new DesignationDto();
            dto.setDesignationId((long) idx);
            dto.setBarcode(tube.getLabel());
            dto.setIndexType(FlowcellDesignation.IndexType.DUAL);
            dto.setLcset(lcset);
            dto.setNumberLanes(numberLanes[idx]);
            dto.setPairedEndRead(true);
            dto.setReadLength(151);
            dto.setRegulatoryDesignation("RESEARCH");
            dto.setSelected(true);
            dto.setSequencerModel(IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell);
            dto.setStatus(FlowcellDesignation.Status.QUEUED);
            dtos.add(dto);
        }

        // Only one flowcell will be designated. The remaining 24 lanes are unallocated in 3 split dtos.
        final int expectedLaneAllocationCount = 2;
        List<DesignationDto> splitDtos = new ArrayList<>();
        for (int i = 0; i < numberLanes.length - 1; ++i) {
            // The first dto will not have a split.
            int dtoIdx = i + 1;
            DesignationDto dto = new DesignationDto();
            dto.setBarcode(stbTubes.get(dtoIdx).getLabel());
            dto.setSequencerModel(IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell);
            dto.setNumberLanes(numberLanes[dtoIdx] - expectedLaneAllocationCount);
            splitDtos.add(dto);
        }

        Pair<List<String>, List<String>> pair = allocateAndTest(dtos, loadingTubes, null, splitDtos,
                Collections.singleton(lcset), 1, true);

        // Checks the unallocated lanes.
        for (int i = 0; i < 7; ++i) {
            Assert.assertTrue(pair.getRight().remove("stbTube1"));
            Assert.assertTrue(pair.getRight().remove("stbTube2"));
            Assert.assertTrue(pair.getRight().remove("stbTube3"));
        }
        Assert.assertTrue(pair.getRight().remove("stbTube2"));
        Assert.assertTrue(pair.getRight().remove("stbTube3"));
        Assert.assertTrue(pair.getRight().remove("stbTube3"));
        Assert.assertTrue(pair.getRight().isEmpty());

        // Checks the lane layout. Ordering is by decreasing numberLanes.
        Assert.assertTrue(pair.getLeft().
                remove("stbTube3 stbTube2 stbTube1 stbTube0 stbTube3 stbTube2 stbTube1 stbTube0"));
        Assert.assertTrue(pair.getLeft().isEmpty());

        // Split should have changed the number of lanes.
        for (int i = 0; i < numberLanes.length; ++i) {
            Assert.assertEquals((int)dtos.get(i).getNumberLanes(), 2);
            Assert.assertEquals(dtos.get(i).getAllocatedLanes(), 2);
        }
    }

    /** Tests sample diversity's typical behavior. */
    @Test
    public void testDesignationPriority20x4diverse() {
        Set<String> lcsets = new HashSet<>();
        // Sample diversity should put each of the 4 samples one each 4-lane flowcell.
        List<DesignationDto> dtos = new ArrayList<>();

        int[] numberLanes = {5, 5, 5, 5};
        for (int idx = 0; idx < numberLanes.length; ++idx) {
            LabVessel tube = stbTubes.get(idx);

            DesignationDto dto = new DesignationDto();
            dto.setDesignationId((long)idx);
            dto.setBarcode(tube.getLabel());
            dto.setIndexType(FlowcellDesignation.IndexType.DUAL);
            dto.setLcset("lcset0");
            dto.setNumberLanes(numberLanes[idx]);
            dto.setPairedEndRead(true);
            dto.setReadLength(151);
            dto.setRegulatoryDesignation("RESEARCH");
            dto.setSelected(true);
            dto.setSequencerModel(IlluminaFlowcell.FlowcellType.NovaSeqS4Flowcell);
            dto.setStatus(FlowcellDesignation.Status.QUEUED);
            lcsets.add(dto.getLcset());
            dtos.add(dto);
        }
        // Setting priority puts the sample in the first lane.
        dtos.get(2).setPriority(FlowcellDesignation.Priority.HIGH);

        Pair<List<String>, List<String>> pair = allocateAndTest(dtos, loadingTubes, null, null, lcsets, 5, true);

        // None are unallocated.
        Assert.assertEquals(pair.getRight().size(), 0);

        // Checks the lane layouts. Order is by priority, then tube label.
        while (!pair.getLeft().isEmpty()) {
            Assert.assertTrue(pair.getLeft().remove("stbTube2 stbTube0 stbTube1 stbTube3"));
        }
    }

    /** Tests sample diversity lane layout. */
    @Test
    public void testDesignationX7diverse() {
        Set<String> lcsets = new HashSet<>();
        // Sample diversity should put each of the 4 samples one each 4-lane flowcell.
        List<DesignationDto> dtos = new ArrayList<>();

        int[] laneCounts = {5, 5, 5, 5, 1, 1, 1, 1, 2, 2};
        for (int idx = 0; idx < laneCounts.length; ++idx) {
            LabVessel tube = stbTubes.get(idx);

            DesignationDto dto = new DesignationDto();
            dto.setDesignationId((long)idx);
            dto.setBarcode(tube.getLabel());
            dto.setIndexType(FlowcellDesignation.IndexType.DUAL);
            dto.setLcset("lcset0");
            dto.setNumberLanes(laneCounts[idx]);
            dto.setPairedEndRead(true);
            dto.setReadLength(151);
            dto.setRegulatoryDesignation("RESEARCH");
            dto.setSelected(true);
            dto.setSequencerModel(IlluminaFlowcell.FlowcellType.NovaSeqS4Flowcell);
            dto.setStatus(FlowcellDesignation.Status.QUEUED);
            lcsets.add(dto.getLcset());
            dtos.add(dto);
        }
        // stbTube8 lanes should be allocated first.
        dtos.get(8).setPriority(FlowcellDesignation.Priority.HIGH);
        // stbTube6 lanes should be allocated last.
        dtos.get(6).setPriority(FlowcellDesignation.Priority.LOW);

        Pair<List<String>, List<String>> pair = allocateAndTest(dtos, loadingTubes, null, null, lcsets, 7, true);

        // None are unallocated.
        Assert.assertEquals(pair.getRight().size(), 0);

        // Checks the lane layouts. Order is by priority, then number of lanes, then tube label, and does
        // not change as lanes are allocated.
        Assert.assertTrue(pair.getLeft().remove("stbTube8 stbTube0 stbTube1 stbTube2"));
        Assert.assertTrue(pair.getLeft().remove("stbTube8 stbTube0 stbTube1 stbTube2"));
        Assert.assertTrue(pair.getLeft().remove("stbTube0 stbTube1 stbTube2 stbTube3"));
        Assert.assertTrue(pair.getLeft().remove("stbTube0 stbTube1 stbTube2 stbTube3"));
        Assert.assertTrue(pair.getLeft().remove("stbTube0 stbTube1 stbTube2 stbTube3"));
        Assert.assertTrue(pair.getLeft().remove("stbTube3 stbTube9 stbTube4 stbTube5"));
        Assert.assertTrue(pair.getLeft().remove("stbTube3 stbTube9 stbTube7 stbTube6"));
        Assert.assertEquals(pair.getLeft().size(), 0);
    }

    /**
     * Allocates and validates.
     * @return Pair consisting of:
     * <ol>List of one String per flowcell, each one being the concatenated starting vessel barcodes in lane order.
     * <ol>List of the unallocated tube barcodes, one per unallocated lane.
     */
    private <DTO_TYPE extends FctDto> Pair<List<String>, List<String>> allocateAndTest(List<DTO_TYPE> dtos,
            Map<String, LabVessel> loadingTubes, IlluminaFlowcell.FlowcellType flowcellType, List<DTO_TYPE> splitDtos,
            Set<String> expectedLcsetNames, int expectedFctCount, boolean sampleDiversity) {

        int expectedLaneCount = 0;
        List<String> expectedBarcodeOnEachLane = new ArrayList<>();
        for (DTO_TYPE dto : dtos) {
            expectedLaneCount += dto.getNumberLanes();
            Assert.assertNotNull(dto.getLcset());
            for (int i = 0; i < dto.getNumberLanes(); ++i) {
                expectedBarcodeOnEachLane.add(dto.getBarcode());
            }
            dto.setAllocatedLanes(0);
        }
        List<String> expectedBatchStartingVesselBarcodes = new ArrayList<>(expectedBarcodeOnEachLane);

        // Allocates designations to flowcells.
        Pair<List<LabBatch>, List<DTO_TYPE>> fctReturn = testBean.makeFctDaoFree(dtos, loadingTubes,
                Collections.<String, FlowcellDesignation>emptyMap(), flowcellType, sampleDiversity);

        // Is the number of FCTs correct?
        Assert.assertEquals(fctReturn.getLeft().size(), expectedFctCount);

        // Checks the number of split dtos.
        Assert.assertEquals(fctReturn.getRight().size(), CollectionUtils.size(splitDtos));

        for (DesignationDto split : (List<DesignationDto>)fctReturn.getRight()) {
            Assert.assertNotNull(split);
            Assert.assertNull(split.getDesignationId());
            Assert.assertTrue(split.isSelected());
            Assert.assertEquals(split.getStatus(), FlowcellDesignation.Status.QUEUED);
            // Matches expected split based on barcode. There should be only one match.
            Assert.assertEquals(splitDtos.stream().
                    filter(dto -> dto.getBarcode().equals(split.getBarcode())).count(), 1);
            DTO_TYPE expectedSplit = splitDtos.stream().
                    filter(dto -> dto.getBarcode().equals(split.getBarcode())).findFirst().orElse(null);
            Assert.assertNotNull(expectedSplit);
            Assert.assertEquals(split.getPriorityValue(), expectedSplit.getPriorityValue());
            Assert.assertEquals(split.getLoadingConc(), expectedSplit.getLoadingConc());
            Assert.assertEquals(split.getNumberLanes(), expectedSplit.getNumberLanes());
        }

        Set<String> foundLcsetNames = new HashSet<>();
        List<String> startingVessels = new ArrayList<>();
        for (LabBatch fctBatch : fctReturn.getLeft()) {
            Set<String> lcsetNames = new HashSet<>(laneToLinkedLcsets(fctBatch).values());

            Assert.assertEquals(fctBatch.getLabBatchStartingVessels().size(),
                    fctBatch.getFlowcellType().getVesselGeometry().getRowCount());

            // Iterates on the starting vessels by increasing lane number.
            List<String> barcodes = new ArrayList<>();
            fctBatch.getLabBatchStartingVessels().stream().
                    sorted((o1, o2) -> o1.getVesselPosition().name().compareTo(o2.getVesselPosition().name())).
                    forEach(batchStartingVessel -> {
                        String barcode = batchStartingVessel.getLabVessel().getLabel();
                        barcodes.add(barcode);
                        // Did batches end up with correct starting vessels?
                        Assert.assertTrue(expectedBatchStartingVesselBarcodes.remove(barcode),
                                "FCT batch has unexpected batch starting vessel " + barcode);
                    });
            // Adds the starting vessel barcodes in lane order.
            startingVessels.add(StringUtils.join(barcodes, " "));

            for (VesselPosition lane : fctBatch.getFlowcellType().getVesselGeometry().getVesselPositions()) {
                // Did flowcell vessels end up with correct starting vessels?
                LabVessel laneVessel = fctBatch.getStartingVesselByPosition(lane);
                String barcode = laneVessel.getLabel();
                Assert.assertTrue(expectedBarcodeOnEachLane.remove(barcode),
                        "Flowcell has unexpected batch starting vessel " + barcode);
            }

            Assert.assertTrue(CollectionUtils.isNotEmpty(lcsetNames));
            foundLcsetNames.addAll(lcsetNames);
        }
        Assert.assertEquals(expectedBarcodeOnEachLane, expectedBatchStartingVesselBarcodes);

        if (expectedLcsetNames != null) {
            // Unexpected lcsets = found - expected.
            Set<String> unexpectedLcsetNames = new HashSet<>();
            unexpectedLcsetNames.addAll(foundLcsetNames);
            unexpectedLcsetNames.removeAll(expectedLcsetNames);
            Assert.assertTrue(unexpectedLcsetNames.isEmpty(), StringUtils.join(unexpectedLcsetNames, " "));
            // Missing lcsets = expected - found;
            Set<String> missingLcsetNames = new HashSet<>();
            missingLcsetNames.addAll(expectedLcsetNames);
            missingLcsetNames.removeAll(foundLcsetNames);
            Assert.assertTrue(missingLcsetNames.isEmpty(), StringUtils.join(missingLcsetNames, " "));
        }
        return Pair.of(startingVessels, expectedBatchStartingVesselBarcodes);
    }
}
