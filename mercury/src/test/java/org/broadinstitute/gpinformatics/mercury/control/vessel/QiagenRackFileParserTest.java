package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.qiagen.generated.Rack;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.qiagen.generated.RackPosition;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Test parsing of Qiagen Rack file.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class QiagenRackFileParserTest {
    public static final String QIAGEN_RACK_XML_OUTPUT = "RackFile_1013740015402336601842.xml";

    @Test
    public void testParseXmlFile() {
        InputStream inputStream = VarioskanParserTest.getTestResource(QIAGEN_RACK_XML_OUTPUT);
        try {
            Rack rack =
                    new QiagenRackFileParser().unmarshal(VarioskanParserTest.getTestResource(QIAGEN_RACK_XML_OUTPUT));
            Map<String, String> wellToBarcode = new HashMap<>();
            for (RackPosition rackPosition: rack.getRackPosition()) {
                if (!rackPosition.getSampleId().getValue().isEmpty()) {
                    wellToBarcode.put(rackPosition.getPositionName().getValue().replaceAll(":", ""),
                            rackPosition.getSampleId().getValue());
                }
            }
            MessageCollection messageCollection = new MessageCollection();
            QiagenRackFileParser parser = new QiagenRackFileParser();
            PlateTransferEventType plateTransferEventType = new PlateTransferEventType();
            PlateType sourcePlate = new PlateType();
            sourcePlate.setPhysType("QiasymphonyCarrier24");
            plateTransferEventType.setSourcePlate(sourcePlate);
            plateTransferEventType.setSourcePositionMap(new PositionMapType());
            plateTransferEventType.setPlate(new PlateType());
            plateTransferEventType.setPositionMap(new PositionMapType());
            parser.attachSourcePlateData(plateTransferEventType, inputStream, messageCollection);
            assertNotNull(plateTransferEventType);
            assertEquals(messageCollection.hasErrors(), false);
            assertEquals(plateTransferEventType.getPlate().getBarcode(), "1013740015402336601842");
        } catch (JAXBException e) {
            e.printStackTrace();
        } finally {
        try {
            inputStream.close();
        } catch (IOException ignored) {
        }
    }
    }
}