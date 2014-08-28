package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.VesselEjb;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
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
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Test SampleVessel parsing with persistence
 */
@Test(groups = TestGroups.STANDARD)
public class UploadSampleVesselsContainerTest extends Arquillian {

    private String[] headers = {"Sample ID", "Manufacturer Tube Barcode", "Container", "Position"};

    @Inject
    private VesselEjb vesselEjb;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Test
    public void testPersistence() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = simpleDateFormat.format(new Date());
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet();
        int currentRow = 0;
        Row row = sheet.createRow(currentRow);
        int column = 0;
        for (String header : headers) {
            Cell cell = row.createCell(column++);
            cell.setCellValue(header);
        }
        VesselPosition[] vesselPositions = RackOfTubes.RackType.Matrix96.getVesselGeometry().getVesselPositions();
        for (int j = 0; j < 2; j++) {
            for (int i = 1; i <= 10; i++) {
                column = 0;
                int rowNum = i + (j * 10);
                row = sheet.createRow(rowNum);
                row.createCell(column++).setCellValue("SM-" + timestamp + rowNum);
                row.createCell(column++).setCellValue(timestamp + rowNum);
                row.createCell(column++).setCellValue("CO-" + timestamp + j);
                row.createCell(column++).setCellValue(vesselPositions[rowNum].toString());
            }
        }
        try {
            File tempFile = File.createTempFile("SampleVessels", ".xlsx");
            workbook.write(new FileOutputStream(tempFile));
            MessageCollection messageCollection = new MessageCollection();
            vesselEjb.createSampleVessels(new FileInputStream(tempFile), "thompson", messageCollection);
            Assert.assertEquals(messageCollection.getErrors().size(), 0);
            vesselEjb.createSampleVessels(new FileInputStream(tempFile), "thompson", messageCollection);
            Assert.assertFalse(messageCollection.getErrors().isEmpty());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InvalidFormatException e) {
            throw new RuntimeException(e);
        } catch (ValidationException e) {
            throw new RuntimeException(e);
        }
    }
}
