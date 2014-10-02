package org.broadinstitute.gpinformatics.mercury.control.reagent;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Test persistence of control reagents.
 */
@Test(groups = TestGroups.STANDARD)
public class ControlReagentContainerTest extends Arquillian {

    @Inject
    private ControlReagentFactory controlReagentFactory;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    public void testBasic() {
        InputStream testSpreadSheetInputStream = VarioskanParserTest.getTestResource("ControlReagents.xlsx");
        Assert.assertNotNull(testSpreadSheetInputStream);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = simpleDateFormat.format(new Date());
        Map<String, String> mapControlToLot = new HashMap<>();
        mapControlToLot.put("NA12878", "SK-P" + timestamp);
        mapControlToLot.put("NO_TEMPLATE_CONTROL", "SK-N" + timestamp);
        // replace tube barcode and lot with timestamps, to avoid unique constraint
        try {
            Workbook workbook = WorkbookFactory.create(testSpreadSheetInputStream);
            Sheet sheet = workbook.getSheet("Sheet1");
            for (int i = 1; i < sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    Cell tubeCell = row.getCell(0);
                    tubeCell.setCellValue("CR" + timestamp + i);

                    Cell controlCell = row.getCell(1);
                    String controlCellValue = controlCell.getStringCellValue();
                    Cell lotCell = row.getCell(2);
                    lotCell.setCellValue(mapControlToLot.get(controlCellValue));
                }
            }
            File tempFile = File.createTempFile("ControlReagents", ".xlsx");
            workbook.write(new FileOutputStream(tempFile));
            controlReagentFactory.buildTubesFromSpreadsheet(new FileInputStream(tempFile), new MessageCollection());
        } catch (IOException | InvalidFormatException e) {
            throw new RuntimeException(e);
        }
    }
}
