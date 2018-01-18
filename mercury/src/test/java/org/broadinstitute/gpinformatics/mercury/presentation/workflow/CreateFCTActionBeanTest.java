
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
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationDto;
import org.broadinstitute.gpinformatics.mercury.presentation.run.FctDto;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
    private List<LabVessel> stbTubes = new ArrayList<LabVessel>(){{
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
    public void testAllocationOf32x1() {
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
            unsetAllocated(dtos);
            Assert.assertTrue(CollectionUtils.isEmpty(allocateAndTest(dtos, loadingTubes, flowcellType, null,
                    expectedLcsets, laneCount / flowcellType.getVesselGeometry().getRowCount())));
            // All are allocated.
            for (CreateFctDto dto : dtos) {
                Assert.assertTrue(dto.isAllocated());
            }
        }
    }

    @Test
    public void testAllocationOf32x8() {
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
            unsetAllocated(dtos);
            Assert.assertTrue(CollectionUtils.isEmpty(allocateAndTest(dtos, loadingTubes, flowcellType, null,
                    expectedLcsets, laneCount / flowcellType.getVesselGeometry().getRowCount())));
            // All are allocated.
            for (CreateFctDto dto : dtos) {
                Assert.assertTrue(dto.isAllocated());
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
            dto.setDesignationId((long)i);
            dto.setBarcode(tube.getLabel());
            dto.setIndexType(FlowcellDesignation.IndexType.DUAL);
            dto.setLcset("lcset" + i / 9);
            dto.setNumberLanes(8);
            dto.setPairedEndRead(true);
            dto.setReadLength(151);
            dto.setRegulatoryDesignation("RESEARCH");
            dto.setSelected(true);
            dto.setBarcode(tube.getLabel());
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

        Assert.assertTrue(CollectionUtils.isEmpty(allocateAndTest(dtos, loadingTubes, null, null,
                expectedLcsets, 32)));
        // All are allocated.
        for (DesignationDto dto : dtos) {
            Assert.assertTrue(dto.isAllocated());
        }
     }

    @Test
    public void testDesignationSplit9AB() {
        // Makes 4 designations having numberLanes of 1, 9, 10, and 11, same priority, but their
        // sizing should cause 11 and 10 to be fully allocated, 9 gets split, and 1 is unallocated.
        List<DesignationDto> dtos = new ArrayList<>();
        int[] numberLanes = {1, 9, 10, 11};
        final String[] lcsets = {"lcset1", "lcset9", "lcset10", "lcset11"};
        // Expects one lcset to be excluded.
        Set<String> expectedLcsets = new HashSet<String>() {{
            add(lcsets[1]);
            add(lcsets[2]);
            add(lcsets[3]);
        }};
        for (int idx = 0; idx < numberLanes.length; ++idx) {
            LabVessel tube = stbTubes.get(idx);

            DesignationDto dto = new DesignationDto();
            dto.setDesignationId((long)idx);
            dto.setBarcode(tube.getLabel());
            dto.setIndexType(FlowcellDesignation.IndexType.DUAL);
            dto.setLcset(lcsets[idx]);
            dto.setNumberLanes(numberLanes[idx]);
            dto.setPairedEndRead(true);
            dto.setReadLength(151);
            dto.setRegulatoryDesignation("RESEARCH");
            dto.setSelected(true);
            dto.setSequencerModel(IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell);
            dto.setStatus(FlowcellDesignation.Status.QUEUED);
            dtos.add(dto);
        }
        // The 9 lane dto will get 3 lanes allocated and 6 lanes left as unallocated split dto.
        DesignationDto splitDto = new DesignationDto();
        splitDto.setBarcode(stbTubes.get(1).getLabel());
        splitDto.setSequencerModel(IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell);
        splitDto.setNumberLanes(6);

        List<String> unallocated = allocateAndTest(dtos, loadingTubes, null, splitDto, expectedLcsets, 3);

        // Split should have reduced the allocated number of lanes.
        Assert.assertEquals((int) dtos.get(1).getNumberLanes(), 3);

        // All but 1 lane dto should be allocated.
        for (int i = 1; i < dtos.size(); ++i) {
            Assert.assertTrue(dtos.get(i).isAllocated());
        }

        Assert.assertEquals(unallocated.size(), 7);
        Assert.assertTrue(unallocated.remove(stbTubes.get(0).getLabel()));
        for (int i = 0; i < 6; ++i) {
            Assert.assertTrue(unallocated.remove(stbTubes.get(1).getLabel()));
        }
        Assert.assertEquals(unallocated.size(), 0);
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
            dto.setDesignationId((long)idx);
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

        List<String> unallocated = allocateAndTest(dtos, loadingTubes, null, splitDto, expectedLcsets, 2);

        // Checks the dto that got split.
        Assert.assertEquals(dtos.get(2).getNumberLanes().intValue(), 7);

        // Checks dto allocations and the unallocated.
        Assert.assertFalse(dtos.get(0).isAllocated());
        Assert.assertTrue(dtos.get(1).isAllocated());
        Assert.assertTrue(dtos.get(2).isAllocated());

        Assert.assertTrue(unallocated.remove(stbTubes.get(2).getLabel()));
        Assert.assertTrue(unallocated.remove(stbTubes.get(2).getLabel()));
        Assert.assertTrue(unallocated.remove(stbTubes.get(2).getLabel()));
        Assert.assertTrue(unallocated.remove(stbTubes.get(0).getLabel()));
        Assert.assertEquals(unallocated.size(), 0);
    }

    @Test
    public void testDesignationGrouping() {
        Set<String> expectedLcsets = new HashSet<>();
        // Makes 3 designations and 1 FCT.
        // dto[0] has stb0 = RESEARCH & ExEx
        // dto[1] has stb1 = CLINICAL & WGS    should combine with dto[0]
        // dto[2] has stb2 = CLINICAL & ExEx   should be unallocated

        List<DesignationDto> dtos = new ArrayList<>();

        for (int idx = 0; idx < 3; ++idx) {
            LabVessel tube = stbTubes.get(idx);

            DesignationDto dto = new DesignationDto();
            dto.setDesignationId((long)idx);
            dto.setBarcode(tube.getLabel());
            dto.setIndexType(FlowcellDesignation.IndexType.DUAL);
            dto.setLcset("lcset" + idx);
            dto.setNumberLanes(new int[] {4, 4, 4}[idx]);
            dto.setPairedEndRead(true);
            dto.setGroupByRegulatoryDesignation(new boolean[] {true, false, true}[idx]);
            dto.setReadLength(151);
            dto.setRegulatoryDesignation(Arrays.asList("RESEARCH", "CLINICAL", "CLINICAL").get(idx));
            dto.setSelected(true);
            dto.setSequencerModel(IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell);
            dto.setStatus(FlowcellDesignation.Status.QUEUED);
            // Changes priority of the 9 lane dto.
            if (dto.getNumberLanes() == 9) {
                dto.setPriority(FlowcellDesignation.Priority.HIGH);
            }
            dtos.add(dto);
        }

        unsetAllocated(dtos);
        List<String> unallocated = allocateAndTest(dtos, loadingTubes, null, null, null, 1);

        Assert.assertTrue(dtos.get(0).isAllocated());
        Assert.assertTrue(dtos.get(1).isAllocated());
        Assert.assertFalse(dtos.get(2).isAllocated());

        Assert.assertTrue(unallocated.remove(stbTubes.get(2).getLabel()));
        Assert.assertTrue(unallocated.remove(stbTubes.get(2).getLabel()));
        Assert.assertTrue(unallocated.remove(stbTubes.get(2).getLabel()));
        Assert.assertTrue(unallocated.remove(stbTubes.get(2).getLabel()));
        Assert.assertEquals(unallocated.size(), 0);


        // Rearrange so the order is
        // dto[0] has stb1 = CLINICAL & WGS
        // dto[1] has stb2 = CLINICAL & ExEx   should combine with dto[0]
        // dto[2] has stb0 = RESEARCH & ExEx   should be unallocated

        DesignationDto temp = dtos.get(0);
        dtos.set(0, dtos.get(1));
        dtos.set(1, dtos.get(2));
        dtos.set(2, temp);

        unsetAllocated(dtos);
        unallocated = allocateAndTest(dtos, loadingTubes, null, null, null, 1);

        Assert.assertTrue(dtos.get(0).isAllocated());
        Assert.assertTrue(dtos.get(1).isAllocated());
        Assert.assertFalse(dtos.get(2).isAllocated());

        Assert.assertTrue(unallocated.remove(stbTubes.get(0).getLabel()));
        Assert.assertTrue(unallocated.remove(stbTubes.get(0).getLabel()));
        Assert.assertTrue(unallocated.remove(stbTubes.get(0).getLabel()));
        Assert.assertTrue(unallocated.remove(stbTubes.get(0).getLabel()));
        Assert.assertEquals(unallocated.size(), 0);

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
            dto.setDesignationId((long)i);
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

        List<String> unallocated = allocateAndTest(dtos, loadingTubes, null, null, expectedLcsets, 0);
        Assert.assertEquals(unallocated.size(), 3);

        // No dtos were allocated?
        for (DesignationDto dto : dtos) {
            Assert.assertFalse(dto.isAllocated());
            for (int i = 0; i < dto.getNumberLanes(); ++i) {
                Assert.assertTrue(unallocated.remove(dto.getBarcode()));
            }
        }
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

        Assert.assertTrue(CollectionUtils.isEmpty(allocateAndTest(dtos, loadingTubes,
                IlluminaFlowcell.FlowcellType.MiSeqFlowcell, null, expectedLcsets,
                laneCount / IlluminaFlowcell.FlowcellType.MiSeqFlowcell.getVesselGeometry().getRowCount())));
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
            unsetAllocated(dtos);
            Assert.assertTrue(CollectionUtils.isEmpty(allocateAndTest(dtos, loadingTubes, flowcellType, null,
                    expectedLcsets, 16 / flowcellType.getVesselGeometry().getRowCount())));
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
            unsetAllocated(dtos);
            Assert.assertTrue(CollectionUtils.isEmpty(allocateAndTest(dtos, loadingTubes, flowcellType, null,
                    expectedLcsets, laneCount / flowcellType.getVesselGeometry().getRowCount())));
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

        List<String> unallocated = allocateAndTest(dtos, loadingTubes, null, null, null, 2);
        Assert.assertEquals(unallocated.size(), 0);

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

    /**
     * Allocates and validates.
     * @return list of the unallocated tube barcodes.
     */
    private <DTO_TYPE extends FctDto> List<String> allocateAndTest(List<DTO_TYPE>dtos,
            Map<String, LabVessel>loadingTubes, IlluminaFlowcell.FlowcellType flowcellType, DTO_TYPE splitDto,
            Set<String> expectedLcsetNames, int expectedFctCount) {

        int expectedLaneCount = 0;
        List<String> expectedBarcodeOnEachLane = new ArrayList<>();
        for (DTO_TYPE dto : dtos) {
            expectedLaneCount += dto.getNumberLanes();
            Assert.assertNotNull(dto.getLcset());
            for (int i = 0; i < dto.getNumberLanes(); ++i) {
                expectedBarcodeOnEachLane.add(dto.getBarcode());
            }
        }
        List<String> expectedBatchStartingVesselBarcodes = new ArrayList<>(expectedBarcodeOnEachLane);

        boolean isDesignationDto = dtos.get(0) instanceof DesignationDto;

        if (!isDesignationDto) {
            int flowcellLaneCount = flowcellType.getVesselGeometry().getRowCount();
            Assert.assertEquals(expectedLaneCount % flowcellLaneCount, 0, "Bad test setup for " + flowcellType.name() +
            " having barcodes " + StringUtils.join(expectedBarcodeOnEachLane, " "));
        }

        // Allocates designations to flowcells.
        Pair<List<LabBatch>, List<DTO_TYPE>> fctReturn = testBean.makeFctDaoFree(dtos, loadingTubes,
                Collections.<String, FlowcellDesignation>emptyMap(), flowcellType);

        // Is the number of FCTs correct?
        Assert.assertEquals(fctReturn.getLeft().size(), expectedFctCount);

        // Checks the split dto.
        if (fctReturn.getRight().isEmpty()) {
            Assert.assertNull(splitDto, "Missing split " + (splitDto != null ? splitDto.getBarcode() : ""));
        } else {
            Assert.assertTrue(isDesignationDto);
            Assert.assertEquals(fctReturn.getRight().size(), 1);
            DesignationDto designationSplit = (DesignationDto)fctReturn.getRight().get(0);
            Assert.assertNotNull(splitDto);
            Assert.assertEquals(designationSplit.getAllocationOrder(), splitDto.getAllocationOrder());
            Assert.assertEquals(designationSplit.getBarcode(), splitDto.getBarcode());
            Assert.assertEquals(designationSplit.getLoadingConc(), splitDto.getLoadingConc());
            Assert.assertEquals(designationSplit.getNumberLanes(), splitDto.getNumberLanes());
            Assert.assertNull(designationSplit.getDesignationId());
            Assert.assertTrue(designationSplit.isSelected());
            Assert.assertEquals(designationSplit.getStatus(), FlowcellDesignation.Status.QUEUED);
        }

        Set<String> foundLcsetNames = new HashSet<>();
        for (LabBatch fctBatch : fctReturn.getLeft()) {
            Set<String> lcsetNames = new HashSet<>(laneToLinkedLcsets(fctBatch).values());

            Assert.assertEquals(fctBatch.getLabBatchStartingVessels().size(),
                    fctBatch.getFlowcellType().getVesselGeometry().getRowCount());

            for (LabBatchStartingVessel batchStartingVessel : fctBatch.getLabBatchStartingVessels()) {
                // Did batches end up with correct starting vessels?
                String barcode = batchStartingVessel.getLabVessel().getLabel();
                Assert.assertTrue(expectedBatchStartingVesselBarcodes.remove(barcode),
                        "FCT batch has unexpected batch starting vessel " + barcode);
            }

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
        return (expectedBatchStartingVesselBarcodes);
    }

    private <DTO_TYPE extends FctDto> void unsetAllocated(Collection<DTO_TYPE> dtos) {
        for (DTO_TYPE dto : dtos) {
            dto.setAllocated(false);
        }
    }
}
