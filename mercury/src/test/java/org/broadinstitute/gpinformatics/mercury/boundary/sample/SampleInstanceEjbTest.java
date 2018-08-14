package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.apache.commons.collections4.CollectionUtils;
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
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleInstanceEntityDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryBarcodeUpdate;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessor;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorEzPass;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorNewTech;
import org.broadinstitute.gpinformatics.mercury.control.sample.VesselPooledTubesProcessor;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
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
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.STANDARD)
public class SampleInstanceEjbTest extends Arquillian {
    final private boolean OVERWRITE = true;
    private Random random = new Random(System.currentTimeMillis());

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
    public void testPooledTubeUploadModified() throws Exception {
        final String filename = "PooledTubeReg.xlsx";
        final String base = String.format("%09d", random.nextInt(100000000));

        for (boolean overwrite : new boolean[]{false, true}) {
            // Makes unique barcode, library, sample name, and root sample name.
            // These are reused in the second upload.
            List<Map<String, String>> alternativeData = new ArrayList<>();
            for (int i = 0; i < 2; ++i) {
                String id = base + i;
                Map<String, String> row = new HashMap<>();
                row.put(VesselPooledTubesProcessor.Headers.TUBE_BARCODE.getText(), "E" + id);
                row.put(VesselPooledTubesProcessor.Headers.LIBRARY_NAME.getText(), "Library" + id);
                row.put(VesselPooledTubesProcessor.Headers.BROAD_SAMPLE_ID.getText(), "SM-" + id);
                row.put(VesselPooledTubesProcessor.Headers.ROOT_SAMPLE_ID.getText(), "SM-" + base + "0");

                if (overwrite) {
                    // Metadata differences in the second upload should update sample data & root
                    // on the same samples that were uploaded in the first test iteration.
                    row.put(VesselPooledTubesProcessor.Headers.COLLABORATOR_SAMPLE_ID.getText(), "COLB-100" + i);
                    row.put(VesselPooledTubesProcessor.Headers.COLLABORATOR_PARTICIPANT_ID.getText(),
                            "COLAB-P-100" + i);
                    row.put(VesselPooledTubesProcessor.Headers.BROAD_PARTICIPANT_ID.getText(), "BP-ID-100" + i);
                    if (i == 1) {
                        row.put(VesselPooledTubesProcessor.Headers.GENDER.getText(), "M");
                        row.put(VesselPooledTubesProcessor.Headers.ROOT_SAMPLE_ID.getText(), "SM-" + id);
                    }
                    row.put(VesselPooledTubesProcessor.Headers.LSID.getText(), "lsid:100" + i);
                }
                alternativeData.add(row);
            }
            byte[] bytes = modifySpreadsheet(VarioskanParserTest.getSpreadsheet(filename), alternativeData);

            MessageCollection messageCollection = new MessageCollection();
            VesselPooledTubesProcessor processor = new VesselPooledTubesProcessor(null);
            List<SampleInstanceEntity> entities = sampleInstanceEjb.doExternalUpload(new ByteArrayInputStream(bytes),
                    overwrite, processor, messageCollection);

            Assert.assertTrue(messageCollection.getErrors().isEmpty(), "In " + filename + ": " +
                    StringUtils.join(messageCollection.getErrors(), "; "));
            // Should have persisted all rows.
            for (int i = 0; i < processor.getBarcodes().size(); ++i) {
                Assert.assertNotNull(labVesselDao.findByIdentifier(processor.getBarcodes().get(i)),
                        processor.getBarcodes().get(i));
                String libraryName = processor.getLibraryNames().get(i);
                Assert.assertNotNull(sampleInstanceEntityDao.findByName(libraryName),
                        filename + " " + libraryName);

                String sampleName = SampleInstanceEjb.get(processor.getSampleNames(), i);
                String msg = filename + " " + sampleName;
                MercurySample mercurySample = mercurySampleDao.findBySampleKey(sampleName);
                Assert.assertNotNull(mercurySample, msg);
                SampleData sampleData = mercurySample.getSampleData();

                String rootSampleName = sampleData.getRootSample();
                MercurySample rootSample = mercurySampleDao.findBySampleKey(rootSampleName);
                Assert.assertTrue(StringUtils.isBlank(rootSampleName) || rootSample != null,
                        msg + " " + rootSampleName);

                Assert.assertEquals(sampleData.getPatientId(),
                        SampleInstanceEjb.get(processor.getBroadParticipantIds(), i), msg);
                Assert.assertEquals(sampleData.getCollaboratorsSampleName(),
                        SampleInstanceEjb.get(processor.getCollaboratorSampleIds(), i), msg);
                Assert.assertEquals(sampleData.getCollaboratorParticipantId(),
                        SampleInstanceEjb.get(processor.getCollaboratorParticipantIds(), i), msg);
                Assert.assertEquals(sampleData.getGender(),
                        SampleInstanceEjb.get(processor.getSexes(), i), msg);
                Assert.assertEquals(sampleData.getOrganism(),
                        SampleInstanceEjb.get(processor.getOrganisms(), i), msg);
                Assert.assertEquals(sampleData.getSampleLsid(),
                        SampleInstanceEjb.get(processor.getLsids(), i), msg);

                if (sampleData.getMetadataSource() == MercurySample.MetadataSource.MERCURY) {
                    Set<String> uniqueKeyNames = new HashSet<>();
                    for (Metadata metadata : mercurySample.getMetadata()) {
                        Assert.assertTrue(uniqueKeyNames.add(metadata.getKey().name()),
                                "Duplicate MercurySample metadata key " + metadata.getKey().name());
                    }
                }
            }
        }
    }

    @Test
    public void testPooledTubeUpload() throws Exception {
        for (String filename : Arrays.asList(
                "PooledTube_Test-363_case1.xls",
                "PooledTube_Test-363_case2.xls",
                "PooledTube_Test-363_case4.xls")) {
            MessageCollection messageCollection = new MessageCollection();
            VesselPooledTubesProcessor processor = new VesselPooledTubesProcessor(null);
            List<SampleInstanceEntity> entities = sampleInstanceEjb.doExternalUpload(
                    new ByteArrayInputStream(IOUtils.toByteArray(VarioskanParserTest.getSpreadsheet(filename))),
                    true, processor, messageCollection);
            // Should be no errors.
            Assert.assertTrue(messageCollection.getErrors().isEmpty(), "In " + filename + ": " +
                    StringUtils.join(messageCollection.getErrors(), "; "));
            // Should have persisted all rows.
            Assert.assertEquals(entities.size(), processor.getBarcodes().size());
            for (int i = 0; i < processor.getBarcodes().size(); ++i) {
                Assert.assertNotNull(labVesselDao.findByIdentifier(processor.getBarcodes().get(i)),
                        processor.getBarcodes().get(i));
                String libraryName = processor.getLibraryNames().get(i);
                Assert.assertNotNull(sampleInstanceEntityDao.findByName(libraryName),
                        filename + " " + libraryName);

                String sampleName = SampleInstanceEjb.get(processor.getSampleNames(), i);
                String msg = filename + " " + sampleName;
                MercurySample mercurySample = mercurySampleDao.findBySampleKey(sampleName);
                Assert.assertNotNull(mercurySample, msg);
                SampleData sampleData = mercurySample.getSampleData();

                String rootSampleName = sampleData.getRootSample();
                MercurySample rootSample = mercurySampleDao.findBySampleKey(rootSampleName);
                Assert.assertTrue(StringUtils.isBlank(rootSampleName) || rootSample != null,
                        msg + " " + rootSampleName);

                // This test uses sample SM-748OO with metadata in Mercury.
                Assert.assertEquals(sampleData.getMetadataSource(), MercurySample.MetadataSource.MERCURY);
                Assert.assertEquals(sampleData.getPatientId(), "12001-015", msg);
                Assert.assertEquals(sampleData.getCollaboratorsSampleName(), "12102402873", msg);
                Assert.assertEquals(sampleData.getCollaboratorParticipantId(), "12001-015", msg);
                Assert.assertEquals(sampleData.getGender(), "Male", msg);
                Assert.assertTrue(StringUtils.isBlank(sampleData.getOrganism()), msg);
                Assert.assertTrue(StringUtils.isBlank(sampleData.getSampleLsid()), msg);

                Set<String> uniqueKeyNames = new HashSet<>();
                for (Metadata metadata : mercurySample.getMetadata()) {
                    Assert.assertTrue(uniqueKeyNames.add(metadata.getKey().name()),
                            "Duplicate MercurySample metadata key " + metadata.getKey().name());
                }

            }
        }
    }


    @Test
    public void testTubeBarcodeUpdate() throws Exception {
        String base = String.format("%09d", random.nextInt(100000000));
        String[] ids = {base + 0, base + 1};

        // Uploads a pooled tube file.
        String filename1 = "PooledTubeReg.xlsx";
        byte[] bytes1 = IOUtils.toByteArray(VarioskanParserTest.getSpreadsheet(filename1));

        // Makes unique barcode, library, sample name, and root sample name.
        List<Map<String, String>> alternativeData1 = new ArrayList<>();
        for (int i = 0; i < ids.length; ++i) {
            Map<String, String> row = new HashMap<>();
            row.put(VesselPooledTubesProcessor.Headers.TUBE_BARCODE.getText(), "E" + ids[i]);
            row.put(VesselPooledTubesProcessor.Headers.LIBRARY_NAME.getText(), "Library" + ids[i]);
            row.put(VesselPooledTubesProcessor.Headers.BROAD_SAMPLE_ID.getText(), "SM-" + ids[i]);
            row.put(VesselPooledTubesProcessor.Headers.ROOT_SAMPLE_ID.getText(), "");
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
            String libraryName = processor1.getLibraryNames().get(i);
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

    @Test
    public void testNewTechUploads() {
        for (Pair<String, ? extends ExternalLibraryProcessor> pair : Arrays.asList(
                Pair.of("ExternalLibraryEZPassTest.xlsx", new ExternalLibraryProcessorEzPass(null)),
                Pair.of("ExternalLibraryMultiOrganismTest.xlsx", new ExternalLibraryProcessorNewTech(null)),
                Pair.of("ExternalLibraryNONPooledTest.xlsx", new ExternalLibraryProcessorNewTech(null)),
                Pair.of("ExternalLibraryPooledTest.xlsx", new ExternalLibraryProcessorNewTech(null)))) {
            MessageCollection messages = new MessageCollection();
            String file = pair.getLeft();
            InputStream spreadsheet = VarioskanParserTest.getSpreadsheet(file);
            ExternalLibraryProcessor processor = pair.getRight();
            sampleInstanceEjb.doExternalUpload(spreadsheet, OVERWRITE, processor, messages);

            Assert.assertTrue(messages.getErrors().isEmpty(), "In " + file + " " + messages.getErrors());
            // Should have persisted all rows.
            for (String libraryName : processor.getLibraryNames()) {
                Assert.assertNotNull(sampleInstanceEntityDao.findByName(libraryName), "Library '" + libraryName + "'");
            }
        }
    }

    private byte[] modifySpreadsheet(InputStream spreadsheet, List<Map<String, String>> alternativeData)
            throws Exception {

        // Reads the spreadsheet
        final List<Map<String, String>> rows = new ArrayList<>();
        ExternalLibraryProcessor processor = new ExternalLibraryProcessor("Sheet1") {
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

            @Override
            public List<SampleInstanceEjb.RowDto> parseUpload(InputStream inputStream, MessageCollection messages) {
                return Collections.EMPTY_LIST;
            }

            @Override
            public void validateAllRows(List<SampleInstanceEjb.RowDto> dtos, boolean overwrite,
                    MessageCollection messageCollection) {
            }

            @Override
            public List<SampleInstanceEntity> makeOrUpdateEntities(List<SampleInstanceEjb.RowDto> rowDtos) {
                return Collections.EMPTY_LIST;
            }

            @Override
            public boolean supportsSampleKitRequest() {
                return false;
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
                // If alternative data exists, uses it instead. Does the lookup using adjusted header name.
                if (alternativeData.size() > rowIndex) {
                    header = processor.adjustHeaderName(header);
                    for (String alternativeDataHeader : alternativeData.get(rowIndex).keySet()) {
                        if (processor.adjustHeaderName(alternativeDataHeader).equals(header)) {
                            cellValue = alternativeData.get(rowIndex).get(alternativeDataHeader);
                        }
                    }
                }
                currentRow.createCell(col++, Cell.CELL_TYPE_STRING).setCellValue(cellValue);
            }
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        byte[] bytes = outputStream.toByteArray();
        IOUtils.closeQuietly(outputStream);
        // Uncomment the next line to save a copy of the modified spreadsheet for debugging.
        //org.apache.commons.io.FileUtils.writeByteArrayToFile(java.io.File.createTempFile("modified", ".xls"), bytes);
        return bytes;
    }
}