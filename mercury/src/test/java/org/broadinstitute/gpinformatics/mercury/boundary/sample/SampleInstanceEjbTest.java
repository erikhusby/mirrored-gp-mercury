package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleInstanceEntityDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VesselPooledTubesProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntity;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.STANDARD)
public class SampleInstanceEjbTest extends Arquillian {
    @Inject
    private SampleInstanceEjb sampleInstanceEjb;

    @Inject
    private SampleInstanceEntityDao sampleInstanceEntityDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    private final Random random = new Random(System.currentTimeMillis());

    @Test
    public void testPooledTubeUpload() throws Exception {
        boolean overwrite = false;
        for (Pair<String, Boolean> fileSuccessPair : Arrays.asList(
                Pair.of("PooledTubeReg.xlsx", true),
                Pair.of("PooledTube_Test-363_case1.xls", true),
                Pair.of("PooledTube_Test-363_case2.xls", true),
                Pair.of("PooledTube_Test-363_case3.xls", false),
                Pair.of("PooledTube_Test-363_case4.xls", true))) {

            boolean expectSuccess = fileSuccessPair.getRight();
            String filename = fileSuccessPair.getLeft();
            byte[] bytes = IOUtils.toByteArray(Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(filename));
            Assert.assertTrue(bytes.length > 0, "Zero length spreadsheet file " + filename);

            TableProcessor testProcessor = sampleInstanceEjb.bestProcessor(new ByteArrayInputStream(bytes)).getLeft();
            Assert.assertTrue(testProcessor instanceof VesselPooledTubesProcessor);

            // Makes randomized barcode, library, sample, root for the non-overwrite case.
            List<Map<String, String>> alternativeData = new ArrayList<>();
            if (!overwrite) {
                int randomNumber = random.nextInt(100000000) * 10;
                for (int rowIndex = 1; rowIndex < 3; ++rowIndex) {
                    String randomDigits = String.format("%010d", randomNumber + rowIndex);
                    Map<String, String> row = new HashMap<>();
                    row.put("Tube barcode", "E" + randomDigits);
                    row.put("Single sample library name", "Library" + randomDigits);
                    row.put("Broad sample ID", "SM-" + randomDigits);
                    row.put("Root Sample ID",
                            String.format("%010d", randomNumber + (new int[]{1, 3})[rowIndex - 1]));
                }
            }
            InputStream inputStream = modify(new ByteArrayInputStream(bytes), alternativeData);

            MessageCollection messageCollection = new MessageCollection();
            Pair<TableProcessor, List<SampleInstanceEntity>> pair = sampleInstanceEjb.doExternalUpload(inputStream,
                    overwrite, messageCollection);
            VesselPooledTubesProcessor processor = (VesselPooledTubesProcessor)pair.getLeft();

            if (expectSuccess) {
                Assert.assertTrue(messageCollection.getErrors().isEmpty(), "In " + filename + ": " +
                        StringUtils.join(messageCollection.getErrors(), "  ;  "));
                // Should have persisted all rows.
                for (int i = 0; i < processor.getBarcodes().size(); ++i) {
                    Assert.assertNotNull(labVesselDao.findByIdentifier(processor.getBarcodes().get(i)),
                            processor.getBarcodes().get(i));
                    String libraryName = processor.getSingleSampleLibraryName().get(i);
                    Assert.assertNotNull(sampleInstanceEntityDao.findByName(libraryName), libraryName);
                    String sampleName = processor.getBroadSampleId().get(i);
                    MercurySample mercurySample = mercurySampleDao.findBySampleKey(sampleName);
                    Assert.assertNotNull(mercurySample, sampleName);
                    MercurySample.MetadataSource metadataSource = mercurySample.getMetadataSource();
                    if (metadataSource == MercurySample.MetadataSource.MERCURY) {
                        Assert.assertTrue(CollectionUtils.isNotEmpty(mercurySample.getMetadata()),
                                "No metadata for " + sampleName);
                        if (StringUtils.isNotBlank(processor.getBroadParticipantId().get(i))) {
                            boolean found = false;
                            for (Metadata metadata : mercurySample.getMetadata()) {
                                if (metadata.getKey() == Metadata.Key.BROAD_PARTICIPANT_ID) {
                                    found = true;
                                    Assert.assertEquals(metadata.getValue(),
                                            processor.getBroadParticipantId().get(i));
                                    break;
                                }
                            }
                            Assert.assertTrue(found, "Missing participant id on " + sampleName);
                        }
                    } else {
                        Assert.assertEquals(metadataSource, MercurySample.MetadataSource.BSP, sampleName);
                    }
                    String rootSampleName = processor.getRootSampleId().get(i);
                    if (StringUtils.isNotBlank(rootSampleName)) {
                        Assert.assertNotNull(mercurySampleDao.findBySampleKey(rootSampleName), rootSampleName);
                    }
                }
            } else {
                // The failing test case should not have persisted any rows.
                for (int i = 0; i < processor.getBarcodes().size(); ++i) {
                    String libraryName = processor.getSingleSampleLibraryName().get(i);
                    Assert.assertNull(sampleInstanceEntityDao.findByName(libraryName), libraryName);
                }
                // Checks the error messages for expected problems.
                String diagnostic = "In " + filename + ": " + StringUtils.join(messageCollection.getErrors(), ";;");
                Assert.assertEquals(messageCollection.getErrors().size(), 20, diagnostic);
                Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                        SampleInstanceEjb.CONFLICT, 7, VesselPooledTubesProcessor.Headers.VOLUME.getText(),
                        "61.00", "60.00", "")), diagnostic);
                Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                        SampleInstanceEjb.CONFLICT, 7,
                        VesselPooledTubesProcessor.Headers.FRAGMENT_SIZE.getText(), "151.00", "150.00", "")),
                        diagnostic);
                Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                        SampleInstanceEjb.CONFLICT, 7, VesselPooledTubesProcessor.Headers.BAIT.getText() +
                                " and " + VesselPooledTubesProcessor.Headers.CAT.getText(),
                        "both", "only one", "")), diagnostic);
                Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                        SampleInstanceEjb.CONFLICT, 2, VesselPooledTubesProcessor.Headers.LSID.getText(),
                        "lsid:1", "null", "for existing Mercury Sample")), diagnostic);
                Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                        SampleInstanceEjb.CONFLICT, 2,
                        VesselPooledTubesProcessor.Headers.BROAD_PARTICIPANT_ID.getText(),
                        "987654321", "12001-015", "for existing Mercury Sample")), diagnostic);
                Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                        SampleInstanceEjb.CONFLICT, 2,
                        VesselPooledTubesProcessor.Headers.SPECIES.getText(),
                        "canine", "null", "for existing Mercury Sample")), diagnostic);

                Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                        SampleInstanceEjb.DUPLICATE, 3,
                        VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText(),
                        "in tube 01509634244")), diagnostic);

                Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                        SampleInstanceEjb.WRONG_TYPE, 8, VesselPooledTubesProcessor.Headers.READ_LENGTH.getText())),
                        diagnostic);
                Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                        SampleInstanceEjb.WRONG_TYPE, 8,
                        VesselPooledTubesProcessor.Headers.FRAGMENT_SIZE.getText())), diagnostic);

                Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                        SampleInstanceEjb.MISSING, 8, VesselPooledTubesProcessor.Headers.VOLUME.getText())),
                        diagnostic);
                Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                        SampleInstanceEjb.MISSING, 8,
                        VesselPooledTubesProcessor.Headers.FRAGMENT_SIZE.getText())), diagnostic);
                Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                        SampleInstanceEjb.MISSING, 5,
                        VesselPooledTubesProcessor.Headers.TUBE_BARCODE.getText())), diagnostic);
                Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                        SampleInstanceEjb.MISSING, 5, VesselPooledTubesProcessor.Headers.EXPERIMENT.getText())),
                        diagnostic);
                Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                        SampleInstanceEjb.MISSING, 6,
                        VesselPooledTubesProcessor.Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText())), diagnostic);
                Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                        SampleInstanceEjb.MISSING, 7,
                        VesselPooledTubesProcessor.Headers.BROAD_SAMPLE_ID.getText())), diagnostic);
                Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                        SampleInstanceEjb.MISSING, 7,
                        VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText())), diagnostic);

                Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                        SampleInstanceEjb.UNKNOWN, 5, VesselPooledTubesProcessor.Headers.EXPERIMENT.getText(),
                        "JIRA DEV")), diagnostic);
                Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                        SampleInstanceEjb.UNKNOWN, 7, VesselPooledTubesProcessor.Headers.CONDITIONS.getText(),
                        "sub-tasks of DEV-6796")), diagnostic);
                Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                        SampleInstanceEjb.UNKNOWN, 8, VesselPooledTubesProcessor.Headers.CONDITIONS.getText(),
                        "sub-tasks of DEV-6796")), diagnostic);
                Assert.assertTrue(messageCollection.getErrors().contains(String.format(
                        SampleInstanceEjb.UNKNOWN, 8,
                        VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText(),
                        "Mercury")), diagnostic);
            }
            overwrite = false;
        }
    }

    private InputStream modify(InputStream spreadsheet, List<Map<String, String>> alternativeData)
            throws Exception {

        // Reads the spreadsheet
        final List<Map<String, String>> rows = new ArrayList<>();
        TableProcessor processor = new TableProcessor("Sheet1") {
            private List<String> headerNames;

            @Override
            public List<String> getHeaderNames() {
                return headerNames;
            }

            @Override
            public void processHeader(List<String> headers, int row) {
                headerNames = headers;
            }

            @Override
            public void processRowDetails(Map<String, String> dataRow, int rowIndex) {
                rows.add(dataRow);
            }

            @Override
            protected ColumnHeader[] getColumnHeaders() {
                return new ColumnHeader[0];
            }

            @Override
            public void close() {
            }
        };
        processor.setHeaderRowIndex(0);
        PoiSpreadsheetParser parser = new PoiSpreadsheetParser(Collections.singletonMap("Sheet1", processor));
        parser.processUploadFile(spreadsheet);
        Assert.assertTrue(CollectionUtils.isNotEmpty(processor.getHeaderNames()));
        Assert.assertEquals(rows.size(), 2);

        // Writes new spreadsheet using alternative data.
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet();
        int currentSheetRow = 0;
        Row currentRow = sheet.createRow(currentSheetRow++);
        CreationHelper creationHelper = workbook.getCreationHelper();
        int col = 0;
        for (String header : processor.getHeaderNames()) {
            currentRow.createCell(col++).setCellValue(creationHelper.createRichTextString(header));
        }
        int rowIndex = 0;
        for (Map<String, String> row : rows) {
            currentRow = sheet.createRow(currentSheetRow++);
            col = 0;
            for (String header : processor.getHeaderNames()) {
                String cellValue = row.get(header);
                // If alternative data exists, uses it instead.
                if (alternativeData.size() > rowIndex && alternativeData.get(rowIndex).containsKey(header)) {
                    cellValue = alternativeData.get(rowIndex).get(header);
                }
                currentRow.createCell(col++, Cell.CELL_TYPE_STRING).setCellValue(cellValue);
            }
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        byte[] bytes = outputStream.toByteArray();
        IOUtils.closeQuietly(outputStream);
        //File tempFile = new File(FileUtils.getTempDirectory(), System.currentTimeMillis() + ".xls");
        //FileUtils.writeByteArrayToFile(tempFile, bytes);

        return new ByteArrayInputStream(bytes);
    }

}
