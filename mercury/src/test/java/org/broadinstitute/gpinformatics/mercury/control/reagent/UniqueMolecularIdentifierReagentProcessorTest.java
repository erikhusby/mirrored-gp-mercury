package org.broadinstitute.gpinformatics.mercury.control.reagent;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.UMIReagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Test(groups = TestGroups.DATABASE_FREE)
public class UniqueMolecularIdentifierReagentProcessorTest {

    public static final String PLATE1_BARCODE = "000000012345";

    public void testBasic() {
        InputStream testSpreadSheetInputStream = VarioskanParserTest.getTestResource("UMIReagents.xlsx");
        Assert.assertNotNull(testSpreadSheetInputStream);
        UniqueMolecularIdentifierReagentProcessor umiProcessor =
                new UniqueMolecularIdentifierReagentProcessor("Sheet1");
        PoiSpreadsheetParser parser = new PoiSpreadsheetParser(Collections.<String, TableProcessor>emptyMap());
        try {
            parser.processRows(WorkbookFactory.create(testSpreadSheetInputStream).getSheetAt(0), umiProcessor);
            Assert.assertEquals(umiProcessor.getMessages().size(), 0);
            Assert.assertEquals(umiProcessor.getMapBarcodeToReagentDto().size(), 4);
            Assert.assertEquals(umiProcessor.getMapUmiToUmi().size(), 3);

            UniqueMolecularIdentifierReagentFactory factory = new UniqueMolecularIdentifierReagentFactory();
            MessageCollection messageCollection = new MessageCollection();

            Map<String, LabVessel> mapBarcodeToPlate = new HashMap<>();
            Map<String, UMIReagent> mapBarcodeToReagent = new HashMap<>();
            List<StaticPlate> staticPlates = factory.buildPlates(umiProcessor.getMapBarcodeToReagentDto(),
                    messageCollection, mapBarcodeToPlate, mapBarcodeToReagent);
            Assert.assertFalse(messageCollection.hasErrors());
            Assert.assertEquals(staticPlates.size(), 4);

            StaticPlate staticPlate1 = staticPlates.get(0);
            Assert.assertEquals(staticPlate1.getLabel(), PLATE1_BARCODE);
            Set<SampleInstanceV2> sampleInstances =
                    staticPlate1.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.A01);
            Assert.assertEquals(sampleInstances.size(), 1);
            SampleInstanceV2 sampleInstance = sampleInstances.iterator().next();
            Assert.assertEquals(sampleInstance.getReagents().size(), 1);
            UMIReagent umi =
                    (UMIReagent) sampleInstance.getReagents().iterator().next();
            Assert.assertEquals(umi.getUmiLocation(), UMIReagent.UMILocation.INLINE_FIRST_READ);
            Assert.assertEquals(umi.getUmiLength(), Long.valueOf(6));
        } catch (ValidationException | IOException | InvalidFormatException e) {
            throw new RuntimeException(e);
        }
    }

    public void testIndexAdapterPlateCheck() {
        StaticPlate indexPlateP7 = createIndexPlate(false, "UMITestIndexAdapterPlate");
        StaticPlate indexPlateWithUMI = createIndexPlate(true, "UMITestIndexAdapterPlateWithUMI");
        MessageCollection messageCollection = new MessageCollection();
        testIndexPlate(indexPlateP7, messageCollection);
        Assert.assertEquals(messageCollection.getErrors().size(), 0);
        testIndexPlate(indexPlateWithUMI, messageCollection);
        Assert.assertEquals(messageCollection.getErrors().size(), 1);
    }

    private void testIndexPlate(StaticPlate indexPlateP7, MessageCollection messageCollection) {
        Map<String, LabVessel> mapBarcodeToPlate = new HashMap<>();
        Map<String, UMIReagent> mapBarcodeToReagent = new HashMap<>();
        mapBarcodeToPlate.put(indexPlateP7.getLabel(), indexPlateP7);

        UniqueMolecularIdentifierReagentFactory factory = new UniqueMolecularIdentifierReagentFactory();
        Map<String, UniqueMolecularIdentifierReagentProcessor.UniqueMolecularIdentifierDto> mapBarcodeToUMI =
                new HashMap<>();
        UniqueMolecularIdentifierReagentProcessor.UniqueMolecularIdentifierDto dto =
                new UniqueMolecularIdentifierReagentProcessor.UniqueMolecularIdentifierDto(
                        UMIReagent.UMILocation.BEFORE_SECOND_INDEX_READ, 4);
        mapBarcodeToUMI.put(indexPlateP7.getLabel(), dto);
        factory.buildPlates(mapBarcodeToUMI,
                messageCollection, mapBarcodeToPlate, mapBarcodeToReagent);
    }

    private StaticPlate createIndexPlate(boolean withUmi, String plateBarcode) {
        StaticPlate indexPlateP7 = LabEventTest.buildIndexPlate(null, null,
                Collections.singletonList(MolecularIndexingScheme.IndexPosition.ILLUMINA_P7),
                Collections.singletonList(plateBarcode)).get(0);
        if (withUmi) {
            UMIReagent umiReagent = new UMIReagent(UMIReagent.UMILocation.BEFORE_SECOND_INDEX_READ, 3L);
            for (VesselPosition vesselPosition: indexPlateP7.getVesselGeometry().getVesselPositions()) {
                PlateWell plateWell = new PlateWell(indexPlateP7, vesselPosition);
                plateWell.addReagent(umiReagent);
                indexPlateP7.getContainerRole().addContainedVessel(plateWell, vesselPosition);
            }
        }
        return indexPlateP7;
    }
}