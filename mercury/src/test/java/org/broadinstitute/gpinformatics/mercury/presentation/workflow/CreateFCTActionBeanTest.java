
package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

@Test(groups = TestGroups.DATABASE_FREE)
public class CreateFCTActionBeanTest {
    private LabBatchEjb testBean = new LabBatchEjb();
    private List<LabVessel> stbTubes = new ArrayList<LabVessel>(){{
        for (int i = 0; i < 32; ++i) {
            add(new BarcodedTube("stbTube" + i));
        }
    }};
    private BigDecimal conc = new BigDecimal("7.0");
    private LabBatch lcset = new LabBatch("LB1234", new HashSet<>(stbTubes), Collections.EMPTY_SET,
            LabBatch.LabBatchType.WORKFLOW, "workflowName", "desc", new Date(), "");

    @Test
    public void testAllocationOf32x1() {
        Collection<Pair<FctDto, LabVessel>> dtoVessels = new ArrayList<>();
        for (int i = 0; i < 32; ++i) {
            LabVessel tube = stbTubes.get(i);
            dtoVessels.add(Pair.of((FctDto)new CreateFctDto(tube.getLabel(), "lcset" + i, conc, 1), tube));
        }
        for (IlluminaFlowcell.FlowcellType flowcellType : IlluminaFlowcell.FlowcellType.values()) {
            if (flowcellType.getCreateFct() == IlluminaFlowcell.CreateFct.NO) {
                continue;
            }
            Assert.assertTrue(CollectionUtils.isEmpty(allocateAndTest(dtoVessels, flowcellType, null)));
        }
    }

    @Test
    public void testAllocationOf32x8() {
        Collection<Pair<FctDto, LabVessel>> dtoVessels = new ArrayList<>();
        for (int i = 0; i < 32; ++i) {
            LabVessel tube = stbTubes.get(i);
            dtoVessels.add(Pair.of((FctDto)new CreateFctDto(tube.getLabel(), "lcset" + i, conc, 8), tube));
        }
        for (IlluminaFlowcell.FlowcellType flowcellType : IlluminaFlowcell.FlowcellType.values()) {
            if (flowcellType.getCreateFct() == IlluminaFlowcell.CreateFct.NO) {
                continue;
            }
            Assert.assertTrue(CollectionUtils.isEmpty(allocateAndTest(dtoVessels, flowcellType, null)));
        }
    }

    @Test
    public void testDesignationOf32x8() {
        Collection<Pair<FctDto, LabVessel>> dtoVessels = new ArrayList<>();
        for (int i = 0; i < 32; ++i) {
            LabVessel tube = stbTubes.get(i);

            DesignationDto dto = new DesignationDto();
            dto.setBarcode(tube.getLabel());
            dto.setSequencerModel(IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell);
            dto.setNumberLanes(8);
            dto.setStatus(FlowcellDesignation.Status.QUEUED);

            dtoVessels.add(Pair.of((FctDto)dto, tube));
         }
        Assert.assertTrue(CollectionUtils.isEmpty(allocateAndTest(dtoVessels,
                IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell, null)));
    }

    @Test
    public void testDesignationSplit9AB() {
        // Makes 3 designations having numberLanes of 9, 10, and 11, same priority, but
        // their sizing should cause 11 and 10 to be fully allocated, and 9 gets split.
        List<DesignationDto> designationDtos = new ArrayList<>();
        Collection<Pair<FctDto, LabVessel>> dtoVessels = new ArrayList<>();
        int[] numberLanes = {9, 10, 11};
        for (int idx = 0; idx < numberLanes.length; ++idx) {
            LabVessel tube = stbTubes.get(idx);

            DesignationDto dto = new DesignationDto();
            dto.setBarcode(tube.getLabel());
            dto.setSequencerModel(IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell);
            dto.setNumberLanes(numberLanes[idx]);
            dto.setStatus(FlowcellDesignation.Status.QUEUED);
            dto.setSelected(true);

            designationDtos.add(dto);
            dtoVessels.add(Pair.of((FctDto) dto, tube));
        }
        // The 0th dto will get 3 lanes allocated and 6 lanes in the unallocated split dto.
        DesignationDto splitDto = new DesignationDto();
        splitDto.setBarcode(stbTubes.get(0).getLabel());
        splitDto.setSequencerModel(IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell);
        splitDto.setNumberLanes(6);

        List<String> unallocated = allocateAndTest(dtoVessels, IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell,
                splitDto);

        // Split should have reduced the allocated number of lanes.
        Assert.assertEquals((int) designationDtos.get(0).getNumberLanes(), 3);

        // Checks allocation status.
        for (DesignationDto dto : designationDtos) {
            Assert.assertTrue(dto.isAllocated());
        }

        Assert.assertEquals(unallocated.size(), 6);
        for (int i = 0; i < 6; ++i) {
            Assert.assertTrue(unallocated.remove(stbTubes.get(0).getLabel()));
        }
        Assert.assertEquals(unallocated.size(), 0);
    }

    @Test
    public void testDesignationPriority9A1() {
        // Makes 3 designations having numberLanes of 1, 9, 10, prioritized as 9, then 10 and 1.
        // Should make two flowcells of 8 lanes.
        // 9 lane dto should be fully allocated, 10 should get split, and 1 should be unallocated.
        List<DesignationDto> designationDtos = new ArrayList<>();
        Collection<Pair<FctDto, LabVessel>> dtoVessels = new ArrayList<>();
        int[] numberLanes = {1, 9, 10};
        for (int idx = 0; idx < numberLanes.length; ++idx) {
            LabVessel tube = stbTubes.get(idx);

            DesignationDto dto = new DesignationDto();
            dto.setBarcode(tube.getLabel());
            dto.setSequencerModel(IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell);
            dto.setNumberLanes(numberLanes[idx]);
            dto.setStatus(FlowcellDesignation.Status.QUEUED);
            dto.setDesignationId((long)idx);
            dto.setSelected(true);
            // Changes priority of the 9 lane dto.
            if (dto.getNumberLanes() == 9) {
                dto.setPriority(FlowcellDesignation.Priority.HIGH);
            }

            designationDtos.add(dto);
            dtoVessels.add(Pair.of((FctDto)dto, tube));
        }

        // dto[2] should be split into 7 lanes (allocated) and 3 lanes (unallocated).
        DesignationDto splitDto = new DesignationDto();
        splitDto.setBarcode(stbTubes.get(2).getLabel());
        splitDto.setSequencerModel(IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell);
        splitDto.setNumberLanes(3);
        splitDto.setStatus(FlowcellDesignation.Status.QUEUED);

        List<String> unallocatedBarcodes = allocateAndTest(dtoVessels, IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell,
                splitDto);

        // Checks the dto that got split.
        Assert.assertEquals((int)designationDtos.get(2).getNumberLanes(), 7);

        // Checks dto allocations and the unallocated.
        Assert.assertFalse(designationDtos.get(0).isAllocated());
        Assert.assertTrue(designationDtos.get(1).isAllocated());
        Assert.assertTrue(designationDtos.get(2).isAllocated());

        Assert.assertTrue(unallocatedBarcodes.remove(stbTubes.get(2).getLabel()));
        Assert.assertTrue(unallocatedBarcodes.remove(stbTubes.get(2).getLabel()));
        Assert.assertTrue(unallocatedBarcodes.remove(stbTubes.get(2).getLabel()));
        Assert.assertTrue(unallocatedBarcodes.remove(stbTubes.get(0).getLabel()));
        Assert.assertEquals(unallocatedBarcodes.size(), 0);
    }

    @Test
    public void testDesignation21() {
        // Makes 2 designations having numberLanes of 2 and 1, neither of which will be allocated
        // since they won't fill an 8 lane flowcell.
        Collection<Pair<FctDto, LabVessel>> dtoVessels = new ArrayList<>();
        List<DesignationDto> designationDtos = new ArrayList<>();
        for (int i = 0; i < 2; ++i) {
            LabVessel tube = stbTubes.get(i);

            DesignationDto dto = new DesignationDto();
            dto.setBarcode(tube.getLabel());
            dto.setSequencerModel(IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell);
            dto.setNumberLanes(2 - i);
            dto.setStatus(FlowcellDesignation.Status.QUEUED);
            dto.setSelected(true);

            designationDtos.add(dto);
            dtoVessels.add(Pair.of((FctDto)dto, tube));
        }

        List<String> unallocatedBarcodes = allocateAndTest(dtoVessels, IlluminaFlowcell.FlowcellType.HiSeq4000Flowcell,
                null);
        Assert.assertEquals(unallocatedBarcodes.size(), 3);

        // No dtos were allocated?
        for (DesignationDto dto : designationDtos) {
            Assert.assertFalse(dto.isAllocated());
            for (int i = 0; i < dto.getNumberLanes(); ++i) {
                Assert.assertTrue(unallocatedBarcodes.remove(dto.getBarcode()));
            }
        }
        Assert.assertEquals(unallocatedBarcodes.size(), 0);
    }

    @Test
    public void testAllocationOf2x2OneLane() {
        Collection<Pair<FctDto, LabVessel>> dtoVessels = new ArrayList<>();
        for (int i = 0; i < 2; ++i) {
            LabVessel tube = stbTubes.get(i);
            dtoVessels.add(Pair.of((FctDto)new CreateFctDto(tube.getLabel(), "lcset" + i, conc, 2), tube));
        }
        Assert.assertTrue(CollectionUtils.isEmpty(allocateAndTest(dtoVessels,
                IlluminaFlowcell.FlowcellType.MiSeqFlowcell, null)));
    }

    @Test
    public void testSharedLcsets() {
        Collection<Pair<FctDto, LabVessel>> dtoVessels = new ArrayList<>();

        LabVessel tube = stbTubes.get(0);
        dtoVessels.add(Pair.of((FctDto)new CreateFctDto(tube.getLabel(), "lcset0", conc, 5), tube));

        tube = stbTubes.get(1);
        dtoVessels.add(Pair.of((FctDto)new CreateFctDto(tube.getLabel(), "lcset1", conc, 6), tube));

        tube = stbTubes.get(2);
        dtoVessels.add(Pair.of((FctDto)new CreateFctDto(tube.getLabel(), "lcset2", conc, 2), tube));

        tube = stbTubes.get(3);
        dtoVessels.add(Pair.of((FctDto)new CreateFctDto(tube.getLabel(), "lcset3", conc, 3), tube));

        for (IlluminaFlowcell.FlowcellType flowcellType : IlluminaFlowcell.FlowcellType.values()) {
            if (flowcellType.getCreateFct() == IlluminaFlowcell.CreateFct.NO) {
                continue;
            }
            Assert.assertTrue(CollectionUtils.isEmpty(allocateAndTest(dtoVessels, flowcellType, null)));
        }
    }

    @Test
    public void testCycles() {
        DesignationDto dto = new DesignationDto();

        dto.setReadLength(151);
        dto.setPairedEndRead(true);
        dto.setIndexType(FlowcellDesignation.IndexType.DUAL);
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

    /**
     * Allocates and validates.
     * @return list of the unallocated tube barcodes.
     */
    private List<String> allocateAndTest(Collection<Pair<FctDto, LabVessel>> dtoVessels,
                                         IlluminaFlowcell.FlowcellType flowcellType, FctDto splitDto) {

        int expectedLaneCount = 0;
        List<String> expectedBarcodeOnEachLane = new ArrayList<>();
        for (Pair<FctDto, LabVessel> pair : dtoVessels) {
            expectedLaneCount += pair.getLeft().getNumberLanes();
            for (int i = 0; i < pair.getLeft().getNumberLanes(); ++i) {
                expectedBarcodeOnEachLane.add(pair.getRight().getLabel());
            }
        }
        List<String> expectedBatchStartingVesselBarcodes = new ArrayList<>(expectedBarcodeOnEachLane);

        boolean isDesignationDto = dtoVessels.iterator().next().getLeft() instanceof DesignationDto;

        int flowcellLaneCount = flowcellType.getVesselGeometry().getRowCount();
        if (!isDesignationDto) {
            Assert.assertEquals(expectedLaneCount % flowcellLaneCount, 0, "Bad test setup");
        }
        int expectedBatchCount = expectedLaneCount / flowcellLaneCount;


        // Does the starting vessel to FCT allocation.
        Pair<Multimap<LabBatch, String>, FctDto> fctBatches = testBean.makeFctDaoFree(dtoVessels,
                flowcellType, !isDesignationDto);

        // Checks the split dto.
        if (fctBatches.getRight() == null) {
            Assert.assertNull(splitDto);
        } else {
            Assert.assertTrue(isDesignationDto);
            DesignationDto designationSplit = (DesignationDto)fctBatches.getRight();
            Assert.assertNotNull(splitDto);
            Assert.assertEquals(designationSplit.getAllocationOrder(), splitDto.getAllocationOrder());
            Assert.assertEquals(designationSplit.getBarcode(), splitDto.getBarcode());
            Assert.assertEquals(designationSplit.getLoadingConc(), splitDto.getLoadingConc());
            Assert.assertEquals(designationSplit.getNumberLanes(), splitDto.getNumberLanes());
            Assert.assertNull(designationSplit.getDesignationId());
            Assert.assertTrue(designationSplit.isSelected());
            Assert.assertEquals(designationSplit.getStatus(), FlowcellDesignation.Status.QUEUED);
        }

        // Is the number of FCTs correct?
        Assert.assertEquals(fctBatches.getLeft().size(), expectedBatchCount, " On " + flowcellType.getDisplayName());

        for (LabBatch fctBatch : fctBatches.getLeft().keys()) {
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
        Assert.assertEquals(expectedBarcodeOnEachLane, expectedBatchStartingVesselBarcodes);
        return (expectedBatchStartingVesselBarcodes);
    }
}
