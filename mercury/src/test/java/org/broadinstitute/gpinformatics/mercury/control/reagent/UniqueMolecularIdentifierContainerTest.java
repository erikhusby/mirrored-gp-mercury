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
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.UMIReagent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
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
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Test persistence of UMI reagents.
 */
@Test(groups = TestGroups.STANDARD)
public class UniqueMolecularIdentifierContainerTest extends Arquillian {

    @Inject
    private UniqueMolecularIdentifierReagentFactory umiFactory;

    @Inject
    private LabVesselDao labVesselDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    public void testBasic() {
        InputStream testSpreadSheetInputStream = VarioskanParserTest.getTestResource("UMIReagents.xlsx");
        Assert.assertNotNull(testSpreadSheetInputStream);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = simpleDateFormat.format(new Date());

        try {
            // replace plate barcode with timestamps, to avoid unique constraint
            Workbook workbook = WorkbookFactory.create(testSpreadSheetInputStream);
            Sheet sheet = workbook.getSheet("Sheet1");
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    Cell plateCell = row.getCell(0);
                    plateCell.setCellValue("UMI" + timestamp + i);
                }
            }
            File tempFile = File.createTempFile("UMIReagents", ".xlsx");
            workbook.write(new FileOutputStream(tempFile));

            MessageCollection messageCollection = new MessageCollection();
            List<StaticPlate> staticPlates = umiFactory.buildUMIFromSpreadsheet(
                    new FileInputStream(tempFile), messageCollection);

            Assert.assertFalse(messageCollection.hasErrors(),
                    "messageCollection failed: " + messageCollection.getErrors());

            labVesselDao.flush();
            labVesselDao.clear();

            LabVessel labVessel = labVesselDao.findByIdentifier(staticPlates.get(0).getLabel());
            Assert.assertEquals(labVessel.getReagentContents().size(), 1);
            UMIReagent umiReagent =
                    (UMIReagent) labVessel.getReagentContents().iterator().next();
            Assert.assertEquals(umiReagent.getUmiLength(), Long.valueOf(6));
            Assert.assertEquals(umiReagent.getUmiLocation(), UMIReagent.UMILocation.INLINE_FIRST_READ);

        } catch (IOException | InvalidFormatException e) {
            throw new RuntimeException(e);
        }
    }
}
