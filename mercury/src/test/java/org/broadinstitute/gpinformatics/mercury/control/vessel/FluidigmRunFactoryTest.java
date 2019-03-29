package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.run.Snp;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.FingerprintingEntityBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.not;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Test(groups = TestGroups.DATABASE_FREE)
public class FluidigmRunFactoryTest extends BaseEventTest {

    public static final String FLUIDIGM_OUTPUT_CSV = "FluidigmOutput.csv";
    public static final String CHIP_BARCODE = "000010553069";

    @Test
    public void testBasic() throws Exception {
        InputStream testSpreadSheet = VarioskanParserTest.getSpreadsheet(FLUIDIGM_OUTPUT_CSV);
        FluidigmChipProcessor fluidigmChipProcessor = new FluidigmChipProcessor();
        FluidigmChipProcessor.FluidigmRun run  = fluidigmChipProcessor.parse(testSpreadSheet);

        Assert.assertEquals(run.getReagentPlateName(), "FluidigmFPv5");

        StaticPlate ifcChip = buildTubesAndTransfers(CHIP_BARCODE);
        StaticPlate.TubeFormationByWellCriteria.Result result =
                ifcChip.nearestFormationAndTubePositionByWell();

        FluidigmRunFactory fluidigmRunFactory = new FluidigmRunFactory();

        // Test that the gender SNP will be skipped
        Map<String, Snp> mapAssayToSnp = mock(Map.class);
        Snp snp = new Snp("rsId", false, false);

        List<String> failedSnps = Arrays.asList("rs1052053", "rs390299", "rs2241759", "rs2549797", "rs2737706");
        List<String> ignoredSnps = Arrays.asList("AMG_3b","rs1052053", "rs390299", "rs2241759", "rs2549797", "rs2737706");
        when(mapAssayToSnp.get(argThat(not(isIn(ignoredSnps))))).thenReturn(snp);

        Snp genderSnp = new Snp("AMG_3b", false, true);
        when(mapAssayToSnp.get("AMG_3b")).thenReturn(genderSnp);

        Snp failedSnp = new Snp("rsIFail", true, false);
        when(mapAssayToSnp.get(argThat(isIn(failedSnps)))).thenReturn(failedSnp);

        MessageCollection messageCollection = new MessageCollection();
        LabMetricRun labMetricRun = fluidigmRunFactory.createFluidigmRunDaoFree(run, ifcChip, 1L,
                result, mapAssayToSnp, null);

        Assert.assertEquals(messageCollection.hasErrors(), false);

        Assert.assertEquals(labMetricRun.getLabMetrics().size(), 1056);
        LabMetric labMetric = findLabMetric(LabMetric.MetricType.CALL_RATE_Q20, labMetricRun.getLabMetrics());

        //Check that the total calls metadata was 90 since we skipped the gender/failed assays
        Metadata totalCalls = findMetadata(labMetric.getMetadataSet(), Metadata.Key.TOTAL_POSSIBLE_CALLS);
        Assert.assertEquals(totalCalls.getNumberValue().intValue(), 90);
    }

    private LabMetric findLabMetric(LabMetric.MetricType metricType, Set<LabMetric> labMetrics) {
        for (LabMetric labMetric: labMetrics) {
            if (labMetric.getName() == metricType) {
                return labMetric;
            }
        }
        return null;
    }

    private static Metadata findMetadata(Set<Metadata> metadataSet, Metadata.Key key) {
        for (Metadata metadata: metadataSet) {
            if (metadata.getKey() == key) {
                return metadata;
            }
        }
        return null;
    }

    private StaticPlate buildTubesAndTransfers(String fingerprintingInputPlate) {
        int numSamples = NUM_POSITIONS_IN_RACK - 2;
        ProductOrder productOrder = ProductOrderTestFactory.buildFingerprintingProductOrder(numSamples);
        Map<String, BarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");

        LabBatch workflowBatch = new LabBatch("Fingerprinting Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(Workflow.FINGERPRINTING);
        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, "1");

        TubeFormation daughterTubeFormation = daughterPlateTransfer(mapBarcodeToTube, workflowBatch);

        Map<String, LabVessel> mapBarcodeToDaughterTube = new HashMap<>();
        for (BarcodedTube barcodedTube : daughterTubeFormation.getContainerRole().getContainedVessels()) {
            mapBarcodeToDaughterTube.put(barcodedTube.getLabel(), barcodedTube);
        }

        PlateTransferEventType fingerprintingPlateSetup = getBettaLimsMessageTestFactory().buildRackToPlate(
                "FingerprintingPlateSetupForwardBsp", "FingerprintingRack",
                new ArrayList<>(mapBarcodeToDaughterTube.keySet()), fingerprintingInputPlate);
        LabEvent fingerprintingPlateSetupEvent = getLabEventFactory().buildFromBettaLims(
                fingerprintingPlateSetup, mapBarcodeToDaughterTube);
        getLabEventHandler().processEvent(fingerprintingPlateSetupEvent);
        StaticPlate fingerprintingPlate = (StaticPlate) fingerprintingPlateSetupEvent.getTargetLabVessels().iterator().next();

        FingerprintingEntityBuilder entityBuilder = new FingerprintingEntityBuilder(
                getBettaLimsMessageTestFactory(), getLabEventFactory(), getLabEventHandler(),
                fingerprintingPlate, "Fingerprinting");
        entityBuilder.invoke();
        return entityBuilder.getIfcChip();
    }

}