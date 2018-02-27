package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleInstanceEntityDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryBarcodeUpdate;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor.REQUIRED_VALUE_IS_MISSING;

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


    @Test
    public void testPooledTubeUpload() throws Exception {
        String base = String.format("%09d", (new Random(System.currentTimeMillis())).nextInt(100000000));
        String[] ids =     {base + 0, base + 1};
        String[] rootIds = {base + 0, base + 2};

        for (List<String> testParameters : Arrays.asList(
                // parameters are:  filename, expectSuccess, overwrite, modifyData, modifyMetadata
                Arrays.asList("PooledTubeReg.xlsx",            "T", "F", "T", "F"),
                Arrays.asList("PooledTubeReg.xlsx",            "T", "T", "T", "T"),
                Arrays.asList("PooledTube_Test-363_case1.xls", "T", "T", "F", "F"),
                Arrays.asList("PooledTube_Test-363_case2.xls", "T", "T", "F", "F"),
                Arrays.asList("PooledTube_Test-363_case3.xls", "F", "F", "F", "F"),
                Arrays.asList("PooledTube_Test-363_case3.xls", "F", "T", "F", "F"),
                Arrays.asList("PooledTube_Test-363_case4.xls", "T", "T", "F", "F"))) {

            String filename = testParameters.get(0);
            boolean expectSuccess = testParameters.get(1).equals("T");
            boolean overwrite = testParameters.get(2).equals("T");
            boolean modifyData = testParameters.get(3).equals("T");
            boolean modifyMetadata = testParameters.get(4).equals("T");
            byte[] bytes = IOUtils.toByteArray(VarioskanParserTest.getSpreadsheet(filename));

            List<Map<String, String>> alternativeData = new ArrayList<>();
            if (modifyData) {
                // Makes unique barcode, library, sample name, and root sample name.
                // These are reused in the second upload.
                for (int i = 0; i < ids.length; ++i) {
                    Map<String, String> row = new HashMap<>();
                    row.put(VesselPooledTubesProcessor.Headers.TUBE_BARCODE.getText(), "E" + ids[i]);
                    row.put(VesselPooledTubesProcessor.Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText(),
                            "Library" + ids[i]);
                    row.put(VesselPooledTubesProcessor.Headers.BROAD_SAMPLE_ID.getText(), "SM-" + ids[i]);
                    row.put(VesselPooledTubesProcessor.Headers.ROOT_SAMPLE_ID.getText(), "SM-" + rootIds[i]);

                    if (modifyMetadata) {
                        // Metadata differences in the second upload should update sample data & root
                        // on the same samples that were uploaded in the first test iteration.
                        row.put(VesselPooledTubesProcessor.Headers.COLLABORATOR_SAMPLE_ID.getText(), "COLB-100" + i);
                        row.put(VesselPooledTubesProcessor.Headers.COLLABORATOR_PARTICIPANT_ID.getText(),
                                "COLAB-P-100" + i);
                        row.put(VesselPooledTubesProcessor.Headers.BROAD_PARTICIPANT_ID.getText(), "BP-ID-100" + i);
                        if (i == 1) {
                            row.put(VesselPooledTubesProcessor.Headers.GENDER.getText(), "M");
                            row.put(VesselPooledTubesProcessor.Headers.ROOT_SAMPLE_ID.getText(), "SM-" + ids[i]);
                        }
                        row.put(VesselPooledTubesProcessor.Headers.LSID.getText(), "lsid:100" + i);

                    }
                    alternativeData.add(row);
                }
                bytes = modifySpreadsheet(new ByteArrayInputStream(bytes), alternativeData);
            }

            MessageCollection messageCollection = new MessageCollection();
            VesselPooledTubesProcessor processor = new VesselPooledTubesProcessor(null);
            List<SampleInstanceEntity> entities = sampleInstanceEjb.doExternalUpload(new ByteArrayInputStream(bytes),
                    overwrite, processor, messageCollection);

            if (expectSuccess) {
                Assert.assertTrue(messageCollection.getErrors().isEmpty(), "In " + filename + ": " +
                        StringUtils.join(messageCollection.getErrors(), "; "));
                // Should have persisted all rows.
                for (int i = 0; i < processor.getBarcodes().size(); ++i) {
                    Assert.assertNotNull(labVesselDao.findByIdentifier(processor.getBarcodes().get(i)),
                            processor.getBarcodes().get(i));
                    String libraryName = processor.getSingleSampleLibraryName().get(i);
                    Assert.assertNotNull(sampleInstanceEntityDao.findByName(libraryName),
                            filename + " " + libraryName);

                    String sampleName = processor.getBroadSampleId().get(i);
                    String msg = filename + " " + sampleName;
                    MercurySample mercurySample = mercurySampleDao.findBySampleKey(sampleName);
                    Assert.assertNotNull(mercurySample, msg);
                    SampleData sampleData = mercurySample.getSampleData();

                    String rootSampleName = sampleData.getRootSample();
                    MercurySample rootSample = mercurySampleDao.findBySampleKey(rootSampleName);
                    Assert.assertTrue(StringUtils.isBlank(rootSampleName) || rootSample != null,
                            msg + " " + rootSampleName);

                    if (!processor.getBroadParticipantId().get(i).isEmpty()) {
                        Assert.assertEquals(sampleData.getPatientId(), processor.getBroadParticipantId().get(i), msg);
                    }
                    if (!processor.getCollaboratorSampleId().get(i).isEmpty()) {
                        Assert.assertEquals(sampleData.getCollaboratorsSampleName(),
                                processor.getCollaboratorSampleId().get(i), msg);
                    }
                    if (!processor.getCollaboratorParticipantId().get(i).isEmpty()) {
                        Assert.assertEquals(sampleData.getCollaboratorParticipantId(),
                                processor.getCollaboratorParticipantId().get(i), msg);
                    }
                    if (!processor.getGender().get(i).isEmpty()) {
                        Assert.assertEquals(sampleData.getGender(), processor.getGender().get(i), msg);
                    }
                    if (!processor.getSpecies().get(i).isEmpty()) {
                        Assert.assertEquals(sampleData.getOrganism(), processor.getSpecies().get(i), msg);
                    }
                    if (!processor.getLsid().get(i).isEmpty()) {
                        Assert.assertEquals(sampleData.getSampleLsid(), processor.getLsid().get(i), msg);
                    }
                    if (sampleData.getMetadataSource() == MercurySample.MetadataSource.MERCURY) {
                        Set<String> uniqueKeyNames = new HashSet<>();
                        for (Metadata metadata : mercurySample.getMetadata()) {
                            Assert.assertTrue(uniqueKeyNames.add(metadata.getKey().name()),
                                    "Duplicate MercurySample metadata key " + metadata.getKey().name());
                        }
                    }
                }
            } else {
                // The failing test cases should not have persisted any new Sample Instances.
                for (int i = 0; i < processor.getBarcodes().size(); ++i) {
                    String libraryName = processor.getSingleSampleLibraryName().get(i);
                    Assert.assertNull(sampleInstanceEntityDao.findByName(libraryName), filename + " " + libraryName);
                }
                // Checks the error messages for expected problems.
                // NOTE that the metadata for SM-748OO is in Mercury and not BSP even though the sample is in BSP.
                List<String> errors = new ArrayList<>(messageCollection.getErrors());
                List<String> warnings = new ArrayList<>(messageCollection.getWarnings());

                if (overwrite) {
                    errorIfMissing(errors, filename, String.format(SampleInstanceEjb.CONFLICT, 3,
                            VesselPooledTubesProcessor.Headers.COLLABORATOR_PARTICIPANT_ID.getText(), "12001-015", "", ""));
                    errorIfMissing(errors, filename, String.format(SampleInstanceEjb.CONFLICT, 3,
                            VesselPooledTubesProcessor.Headers.COLLABORATOR_SAMPLE_ID.getText(), "12102402873", "", ""));
                    errorIfMissing(errors, filename, String.format(SampleInstanceEjb.CONFLICT, 3,
                            VesselPooledTubesProcessor.Headers.GENDER.getText(), "Male", "", ""));
                    errorIfMissing(errors, filename, String.format(SampleInstanceEjb.CONFLICT, 3,
                            VesselPooledTubesProcessor.Headers.ROOT_SAMPLE_ID.getText(), "SM-UNKNOWN", "", ""));
                    errorIfMissing(errors, filename, String.format(SampleInstanceEjb.DUPLICATE, 3,
                            VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText() +
                                    " in tube 01509634244"));
                    errorIfMissing(errors, filename, String.format(SampleInstanceEjb.CONFLICT, 3,
                            VesselPooledTubesProcessor.Headers.VOLUME.getText(), "61.00", "60.00", ""));
                    errorIfMissing(errors, filename, String.format(SampleInstanceEjb.CONFLICT, 3,
                            VesselPooledTubesProcessor.Headers.FRAGMENT_SIZE.getText(), "151.00", "150.00", ""));
                    errorIfMissing(warnings, filename, String.format(SampleInstanceEjb.DUPLICATE_S_M, 3,
                            "SM-748OO", "Illumina_P5-Nijow_P7-Waren"));

                    errorIfMissing(warnings, filename, String.format(SampleInstanceEjb.DUPLICATE_S_M, 4,
                            "SM-748OO", "Illumina_P5-Nijow_P7-Waren"));

                    errorIfMissing(errors, filename, "Row #5 " + String.format(REQUIRED_VALUE_IS_MISSING,
                            VesselPooledTubesProcessor.Headers.TUBE_BARCODE.getText()));
                    errorIfMissing(errors, filename, "Row #5 " + String.format(REQUIRED_VALUE_IS_MISSING,
                            VesselPooledTubesProcessor.Headers.EXPERIMENT.getText()));
                    errorIfMissing(errors, filename, "Row #5 " + String.format(REQUIRED_VALUE_IS_MISSING,
                            VesselPooledTubesProcessor.Headers.CONDITIONS.getText()));

                    errorIfMissing(errors, filename, "Row #6 " + String.format(REQUIRED_VALUE_IS_MISSING,
                            VesselPooledTubesProcessor.Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText()));

                    errorIfMissing(errors, filename, "Row #7 " + String.format(REQUIRED_VALUE_IS_MISSING,
                            VesselPooledTubesProcessor.Headers.BROAD_SAMPLE_ID.getText()));
                    errorIfMissing(errors, filename, "Row #7 " + String.format(REQUIRED_VALUE_IS_MISSING,
                            VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText()));

                    errorIfMissing(errors, filename, String.format(SampleInstanceEjb.MUST_NOT_BE, 8,
                            VesselPooledTubesProcessor.Headers.BAIT.getText() + " and " +
                                    VesselPooledTubesProcessor.Headers.CAT.getText(), "both defined"));
                    errorIfMissing(errors, filename, String.format(SampleInstanceEjb.UNKNOWN_COND, 8, "DEV-6796"));
                    errorIfMissing(errors, filename, String.format(SampleInstanceEjb.CONFLICT, 8,
                            VesselPooledTubesProcessor.Headers.LSID.getText(), "lsid:3", "lsid:1", ""));
                    errorIfMissing(warnings, filename, String.format(SampleInstanceEjb.DUPLICATE_S_M, 8,
                            "SM-748OO", "Illumina_P5-Nijow_P7-Waren"));

                    errorIfMissing(errors, filename, String.format(SampleInstanceEjb.WRONG_TYPE, 9,
                            VesselPooledTubesProcessor.Headers.READ_LENGTH.getText(), "a positive integer"));
                    errorIfMissing(errors, filename, String.format(SampleInstanceEjb.WRONG_TYPE, 9,
                            VesselPooledTubesProcessor.Headers.FRAGMENT_SIZE.getText(), "a positive number"));
                    errorIfMissing(errors, filename, String.format(SampleInstanceEjb.WRONG_TYPE, 9,
                            VesselPooledTubesProcessor.Headers.VOLUME.getText(), "a positive number"));
                    errorIfMissing(errors, filename, String.format(SampleInstanceEjb.UNKNOWN_COND, 9, "DEV-6796"));
                    errorIfMissing(errors, filename, String.format(SampleInstanceEjb.UNKNOWN, 9,
                            VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText(), "Mercury"));
                    Assert.assertTrue(errors.isEmpty(), "Found unexpected errors: " + StringUtils.join(errors, "; "));
                } else {
                    // For the non-overwrite case checks for errors on the rows that try to update metadata.
                    String[] columnNames = {
                            VesselPooledTubesProcessor.Headers.BROAD_PARTICIPANT_ID.getText(),
                            VesselPooledTubesProcessor.Headers.LSID.getText(),
                            VesselPooledTubesProcessor.Headers.SPECIES.getText(),
                    };
                    for (int rowNumber : new int[]{3, 4, 8, 9}) {
                        errorIfMissing(errors, filename, String.format(SampleInstanceEjb.PREXISTING_VALUES, rowNumber,
                                StringUtils.join(columnNames, ", ")));
                    }
                }
            }
        }
    }

    @Test
    public void testTubeBarcodeUpdate() throws Exception {
        final boolean OVERWRITE = true;
        String base = String.format("%09d", (new Random(System.currentTimeMillis())).nextInt(100000000));
        String[] ids = {base + 0, base + 1};
        String[] rootIds = {base + 0, base + 2};

        // Uploads a pooled tube file.
        String filename1 = "PooledTubeReg.xlsx";
        byte[] bytes1 = IOUtils.toByteArray(VarioskanParserTest.getSpreadsheet(filename1));

        // Makes unique barcode, library, sample name, and root sample name.
        List<Map<String, String>> alternativeData1 = new ArrayList<>();
        for (int i = 0; i < ids.length; ++i) {
            Map<String, String> row = new HashMap<>();
            row.put(VesselPooledTubesProcessor.Headers.TUBE_BARCODE.getText(), "E" + ids[i]);
            row.put(VesselPooledTubesProcessor.Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText(), "Library" + ids[i]);
            row.put(VesselPooledTubesProcessor.Headers.BROAD_SAMPLE_ID.getText(), "SM-" + ids[i]);
            row.put(VesselPooledTubesProcessor.Headers.ROOT_SAMPLE_ID.getText(), "SM-" + rootIds[i]);
            alternativeData1.add(row);
        }
        bytes1 = modifySpreadsheet(new ByteArrayInputStream(bytes1), alternativeData1);
        VesselPooledTubesProcessor processor1 = new VesselPooledTubesProcessor(null);
        MessageCollection messages1 = new MessageCollection();
        sampleInstanceEjb.doExternalUpload(new ByteArrayInputStream(bytes1), !OVERWRITE, processor1, messages1);

        Assert.assertTrue(messages1.getErrors().isEmpty(), "In " + filename1 + ": " +
                StringUtils.join(messages1.getErrors(), "; "));
        // Should have persisted all rows.
        for (int i = 0; i < processor1.getBarcodes().size(); ++i) {
            Assert.assertNotNull(labVesselDao.findByIdentifier(processor1.getBarcodes().get(i)),
                    processor1.getBarcodes().get(i));
            String libraryName = processor1.getSingleSampleLibraryName().get(i);
            Assert.assertNotNull(sampleInstanceEntityDao.findByName(libraryName),
                    filename1+ " " + libraryName);
        }

        // Updates the tube barcodes. Since it only relies on a SampleInstanceEntity the upload
        // will work on both pooled tube uploads and external library uploads.
        String filename2 = "ExternalLibrarySampleBarcodeUpdate.xlsx";
        byte[] bytes2 = IOUtils.toByteArray(VarioskanParserTest.getSpreadsheet(filename2));

        // Makes new barcode for the existing library names.
        List<Map<String, String>> alternativeData2 = new ArrayList<>();
        for (int i = 0; i < ids.length; ++i) {
            Map<String, String> row = new HashMap<>();
            // New tube barcodes start with F. The header names must match what's in the spreadsheet.
            row.put("barcode", "F" + ids[i]);
            row.put("library", "Library" + ids[i]);
            alternativeData2.add(row);
        }
        bytes2 = modifySpreadsheet(new ByteArrayInputStream(bytes2), alternativeData2);
        MessageCollection messages2 = new MessageCollection();
        ExternalLibraryBarcodeUpdate processor2 = new ExternalLibraryBarcodeUpdate(null);

        sampleInstanceEjb.doExternalUpload(new ByteArrayInputStream(bytes2), OVERWRITE, processor2, messages2);

        Assert.assertTrue(messages2.getErrors().isEmpty(), "In " + filename2 + ": " +
                StringUtils.join(messages2.getErrors(), "; "));
        // Should have persisted all updates.
        for (int i = 0; i < processor2.getBarcodes().size(); ++i) {
            String barcode = processor2.getBarcodes().get(i);
            String libraryName = processor2.getLibraryNames().get(i);
            Assert.assertTrue(barcode.startsWith("F"));
            Assert.assertEquals(sampleInstanceEntityDao.findByName(libraryName).getLabVessel().getLabel(),
                    barcode, filename2 + " " + libraryName);
        }
    }

    private boolean errorIfMissing(List<String> errors, String filename, String expected) {
        for (Iterator<String> iterator = errors.iterator(); iterator.hasNext(); ) {
            String error = iterator.next();
            if (error.startsWith(expected)) {
                iterator.remove();
                return true;
            }
        }
        Assert.fail(filename + " error message \"" + expected + "\" is missing from the remaining errors: " +
                StringUtils.join(errors, "; "));
        return false;
    }

    private byte[] modifySpreadsheet(InputStream spreadsheet, List<Map<String, String>> alternativeData)
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
            public void processRowDetails(Map<String, String> dataRow, int rowIndex, boolean requiredValuesPresent) {
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
        for (Map<String, String> row : rows) {
            int rowIndex = currentSheetRow - 1;
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
        //debug  org.apache.commons.io.FileUtils.writeByteArrayToFile(java.io.File.createTempFile("modified", ".xls"), bytes);
        return bytes;
    }
}