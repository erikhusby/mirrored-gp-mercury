package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.broadinstitute.gpinformatics.mercury.control.vessel.FluidigmChipProcessorTest.FLUIDIGM_OUTPUT_CSV;
import static org.testng.Assert.*;

@Test(groups = TestGroups.DATABASE_FREE)
public class FluidigmRunFactoryTest {

    public static final String FLUIDIGM_OUTPUT_CSV = "FluidigmOutput.csv";
    public static final String CHIP_BARCODE = "000010553069";

    @Test
    public void testBasic() throws Exception {
        InputStream testSpreadSheet = VarioskanParserTest.getSpreadsheet(FLUIDIGM_OUTPUT_CSV);
        FluidigmChipProcessor fluidigmChipProcessor = new FluidigmChipProcessor();
        FluidigmChipProcessor.FluidigmRun run  = fluidigmChipProcessor.parse(testSpreadSheet);

        Map<String, StaticPlate> mapBarcodeToPlate = new HashMap<>();
        buildTubesAndTransfers(mapBarcodeToPlate, CHIP_BARCODE, "");
        StaticPlate.TubeFormationByWellCriteria.Result result =
                mapBarcodeToPlate.values().iterator().next().nearestFormationAndTubePositionByWell();

        MessageCollection messageCollection = new MessageCollection();
        FluidigmRunFactory fluidigmRunFactory = new FluidigmRunFactory();
        fluidigmRunFactory.createFluidigmRunDaoFree(run, mapBarcodeToPlate.values().iterator().next(), 1L,
                messageCollection, result);
    }

    public static Map<VesselPosition, BarcodedTube> buildTubesAndTransfers(Map<String, StaticPlate> mapBarcodeToPlate,
                                                                           String plate1Barcode, String tubePrefix) {
        Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
        for (VesselPosition vesselPosition : RackOfTubes.RackType.Matrix96.getVesselGeometry().getVesselPositions()) {
            BarcodedTube barcodedTube = new BarcodedTube(tubePrefix + vesselPosition.toString());
            barcodedTube.setVolume(new BigDecimal("75"));
            mapPositionToTube.put(vesselPosition, barcodedTube);
        }

        TubeFormation tubeFormation = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);

        StaticPlate staticPlate1 = new StaticPlate(plate1Barcode, StaticPlate.PlateType.Eppendorf96);
        mapBarcodeToPlate.put(staticPlate1.getLabel(), staticPlate1);

        //TODO Real section transfer in the future
        LabEvent labEvent1 = new LabEvent(LabEventType.FLUIDIGM_FINAL_TRANSFER, new Date(), "BATMAN", 1L, 101L,
                "Bravo");
        labEvent1.getSectionTransfers().add(new SectionTransfer(tubeFormation.getContainerRole(), SBSSection.ALL96,
                null, staticPlate1.getContainerRole(), SBSSection.ALL96, null, labEvent1));

        return mapPositionToTube;
    }

}