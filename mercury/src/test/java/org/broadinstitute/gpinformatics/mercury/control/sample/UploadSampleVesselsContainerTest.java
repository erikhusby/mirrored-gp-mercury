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
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
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
@Test(groups = TestGroups.STANDARD, singleThreaded = true)
public class UploadSampleVesselsContainerTest extends Arquillian {

    private String[] parentChildHeaders = {"Sample ID", "Manufacturer Tube Barcode", "Container", "Position"};
    private String[] looseTubeHeaders = {"Sample ID", "Vessel Type"};

    @Inject
    private VesselEjb vesselEjb;

    @Inject
    private BarcodedTubeDao barcodedTubeDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Test
    public void testParentChild() {
        // Create a spreadsheet with IDs that aren't in the database.
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = simpleDateFormat.format(new Date());
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet();
        int currentRow = 0;
        Row row = sheet.createRow(currentRow);
        int column = 0;
        for (String header : parentChildHeaders) {
            Cell cell = row.createCell(column++);
            cell.setCellValue(header);
        }
        VesselPosition[] vesselPositions = RackOfTubes.RackType.Matrix96.getVesselGeometry().getVesselPositions();
        for (int j = 0; j < 2; j++) {
            for (int i = 1; i <= 10; i++) {
                column = 0;
                int rowNum = i + (j * 10);
                row = sheet.createRow(rowNum);
                row.createCell(column++).setCellValue("SM-" + timestamp + "pc" + rowNum);
                row.createCell(column++).setCellValue(timestamp + "pc" + rowNum);
                row.createCell(column++).setCellValue("CO-" + timestamp + "pc" + j);
                row.createCell(column++).setCellValue(vesselPositions[rowNum].toString());
            }
        }
        try {
            File tempFile = File.createTempFile("SampleVessels", ".xlsx");
            workbook.write(new FileOutputStream(tempFile));
            MessageCollection messageCollection = new MessageCollection();
            // Persist the samples
            vesselEjb.createSampleVessels(new FileInputStream(tempFile), "thompson", messageCollection,
                    new SampleParentChildVesselProcessor("Sheet1"));
            Assert.assertEquals(messageCollection.getErrors().size(), 0);

            // Check tube and sample
            BarcodedTube barcodedTube = barcodedTubeDao.findByBarcode(timestamp + "pc" + 1);
            Assert.assertNotNull(barcodedTube);
            Assert.assertEquals(barcodedTube.getMercurySamples().size(), 1);
            MercurySample mercurySample = barcodedTube.getMercurySamples().iterator().next();
            Assert.assertEquals(mercurySample.getSampleKey(), "SM-" + timestamp + "pc" + 1);
            Assert.assertEquals(mercurySample.getMetadataSource(), MercurySample.MetadataSource.MERCURY);

            // Check that attempting to create again the same tubes and samples gives errors.
            vesselEjb.createSampleVessels(new FileInputStream(tempFile), "thompson", messageCollection,
                    new SampleParentChildVesselProcessor("Sheet1"));
            Assert.assertFalse(messageCollection.getErrors().isEmpty());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InvalidFormatException e) {
            throw new RuntimeException(e);
        } catch (ValidationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testLooseTubes() {
        // Create a spreadsheet with IDs that aren't in the database.
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = simpleDateFormat.format(new Date());
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet();
        int currentRow = 0;
        Row row = sheet.createRow(currentRow);
        int column = 0;
        for (String header : looseTubeHeaders) {
            Cell cell = row.createCell(column++);
            cell.setCellValue(header);
        }
        for (int rowNum = 1; rowNum <= 10; rowNum++) {
            column = 0;
            row = sheet.createRow(rowNum);
            row.createCell(column++).setCellValue("SM-" + timestamp + "l" + rowNum);
            row.createCell(column++).setCellValue("Cryo vial [1.5mL]");
        }
        try {
            File tempFile = File.createTempFile("SampleVessels", ".xlsx");
            workbook.write(new FileOutputStream(tempFile));
            MessageCollection messageCollection = new MessageCollection();
            // Persist the samples
            vesselEjb.createSampleVessels(new FileInputStream(tempFile), "thompson", messageCollection,
                    new SampleLooseVesselProcessor("Sheet1"));
            Assert.assertEquals(messageCollection.getErrors().size(), 0);

            // Check tube and sample
            BarcodedTube barcodedTube = barcodedTubeDao.findByBarcode("SM-" + timestamp + "l" + 1);
            Assert.assertNotNull(barcodedTube);
            Assert.assertEquals(barcodedTube.getMercurySamples().size(), 1);
            MercurySample mercurySample = barcodedTube.getMercurySamples().iterator().next();
            Assert.assertEquals(mercurySample.getSampleKey(), "SM-" + timestamp + "l" + 1);
            Assert.assertEquals(mercurySample.getMetadataSource(), MercurySample.MetadataSource.MERCURY);

            // Check that attempting to create again the same tubes and samples gives errors.
            vesselEjb.createSampleVessels(new FileInputStream(tempFile), "thompson", messageCollection,
                    new SampleLooseVesselProcessor("Sheet1"));
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
