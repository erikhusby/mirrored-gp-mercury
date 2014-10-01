package org.broadinstitute.gpinformatics.mercury.control.reagent;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
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
import java.io.IOException;
import java.io.InputStream;

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
        // replace tube barcode and lot with timestamps, to avoid unique constraint
        try {
            Workbook workbook = WorkbookFactory.create(testSpreadSheetInputStream);
            Sheet sheet = workbook.getSheet("Sheet1");
            for (int i = 1; i < sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    row.getCell(0);
                }
            }

            Assert.assertNotNull(testSpreadSheetInputStream);
            controlReagentFactory.buildTubesFromSpreadsheet(testSpreadSheetInputStream, new MessageCollection());
        } catch (IOException | InvalidFormatException e) {
            throw new RuntimeException(e);
        }
    }
}
