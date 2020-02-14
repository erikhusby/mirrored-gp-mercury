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
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.UMIReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.UniqueMolecularIdentifier;
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
    public static final String BARCODED_TUBE = "0000087654";

    public void testBasic() {
        InputStream testSpreadSheetInputStream = VarioskanParserTest.getTestResource("UMIReagents.xlsx");
        Assert.assertNotNull(testSpreadSheetInputStream);
        UniqueMolecularIdentifierReagentProcessor umiProcessor =
                new UniqueMolecularIdentifierReagentProcessor("Sheet1");
        PoiSpreadsheetParser parser = new PoiSpreadsheetParser(Collections.<String, TableProcessor>emptyMap());
        try {
            parser.processRows(WorkbookFactory.create(testSpreadSheetInputStream).getSheetAt(0), umiProcessor);
            Assert.assertEquals(umiProcessor.getMessages().size(), 0);
            Assert.assertEquals(umiProcessor.getMapBarcodeToReagentDto().size(), 5);
            Assert.assertEquals(umiProcessor.getMapUmiToUmi().size(), 7);

            UniqueMolecularIdentifierReagentFactory factory = new UniqueMolecularIdentifierReagentFactory();
            MessageCollection messageCollection = new MessageCollection();

            Map<String, LabVessel> mapBarcodeToPlate = new HashMap<>();
            Map<String, List<UniqueMolecularIdentifier>> mapBarcodeToReagent = new HashMap<>();
            List<LabVessel> labVessels = factory.buildPlates(umiProcessor.getMapBarcodeToReagentDto(),
                    messageCollection, mapBarcodeToPlate, mapBarcodeToReagent);
            Assert.assertFalse(messageCollection.hasErrors());
            Assert.assertEquals(labVessels.size(), 5);

            LabVessel staticPlate1 = labVessels.get(0);
            Assert.assertEquals(staticPlate1.getLabel(), PLATE1_BARCODE);
            Set<SampleInstanceV2> sampleInstances =
                    staticPlate1.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.A01);
            Assert.assertEquals(sampleInstances.size(), 1);
            SampleInstanceV2 sampleInstance = sampleInstances.iterator().next();
            Assert.assertEquals(sampleInstance.getReagents().size(), 1);
            UMIReagent umiReagent =
                    (UMIReagent) sampleInstance.getReagents().iterator().next();
            Assert.assertEquals(umiReagent.getUniqueMolecularIdentifier().getLocation(), UniqueMolecularIdentifier.UMILocation.INLINE_FIRST_READ);
            Assert.assertEquals(umiReagent.getUniqueMolecularIdentifier().getLength(), Long.valueOf(6));

            // Tube has 2 UMIs
            LabVessel barcodedTube = labVessels.get(labVessels.size() - 1);
            Assert.assertEquals(barcodedTube.getLabel(), BARCODED_TUBE);
            UniqueMolecularIdentifier firstRead = new UniqueMolecularIdentifier(UniqueMolecularIdentifier.UMILocation.BEFORE_FIRST_READ, 3L, 2L);
            UniqueMolecularIdentifier secondRead = new UniqueMolecularIdentifier(UniqueMolecularIdentifier.UMILocation.BEFORE_SECOND_READ, 3L, 2L);
            Assert.assertEquals(barcodedTube.getReagentContents().size(), 2);
            for (Reagent reagent: barcodedTube.getReagentContents()) {
                UMIReagent uniqueMolecularIdentifierReagent =
                        (UMIReagent) reagent;
                UniqueMolecularIdentifier actualUMI = uniqueMolecularIdentifierReagent.getUniqueMolecularIdentifier();
                if (actualUMI.getLocation() == UniqueMolecularIdentifier.UMILocation.BEFORE_FIRST_READ) {
                    Assert.assertEquals(actualUMI, firstRead);
                } else if (actualUMI.getLocation() == UniqueMolecularIdentifier.UMILocation.BEFORE_SECOND_READ) {
                    Assert.assertEquals(actualUMI, secondRead);
                } else {
                    Assert.fail("Failed to find one of the expected UMIs");
                }
            }
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
        Map<String, List<UniqueMolecularIdentifier>> mapBarcodeToReagent = new HashMap<>();
        mapBarcodeToPlate.put(indexPlateP7.getLabel(), indexPlateP7);

        UniqueMolecularIdentifierReagentFactory factory = new UniqueMolecularIdentifierReagentFactory();
        Map<String, List<UniqueMolecularIdentifierReagentProcessor.UniqueMolecularIdentifierDto>> mapBarcodeToUMI =
                new HashMap<>();
        UniqueMolecularIdentifierReagentProcessor.UniqueMolecularIdentifierDto dto =
                new UniqueMolecularIdentifierReagentProcessor.UniqueMolecularIdentifierDto(
                        UniqueMolecularIdentifier.UMILocation.BEFORE_SECOND_INDEX_READ, 4, 2, StaticPlate.PlateType.IndexedAdapterPlate96);
        mapBarcodeToUMI.put(indexPlateP7.getLabel(), Collections.singletonList(dto));
        factory.buildPlates(mapBarcodeToUMI,
                messageCollection, mapBarcodeToPlate, mapBarcodeToReagent);
    }

    private StaticPlate createIndexPlate(boolean withUmi, String plateBarcode) {
        StaticPlate indexPlateP7 = LabEventTest.buildIndexPlate(null, null,
                Collections.singletonList(MolecularIndexingScheme.IndexPosition.ILLUMINA_P7),
                Collections.singletonList(plateBarcode)).get(0);
        if (withUmi) {
            UniqueMolecularIdentifier umi = new UniqueMolecularIdentifier(UniqueMolecularIdentifier.UMILocation.BEFORE_SECOND_INDEX_READ, 3L, 2L);
            UMIReagent umiReagent = new UMIReagent(umi);
            for (VesselPosition vesselPosition: indexPlateP7.getVesselGeometry().getVesselPositions()) {
                PlateWell plateWell = new PlateWell(indexPlateP7, vesselPosition);
                plateWell.addReagent(umiReagent);
                indexPlateP7.getContainerRole().addContainedVessel(plateWell, vesselPosition);
            }
        }
        return indexPlateP7;
    }
}