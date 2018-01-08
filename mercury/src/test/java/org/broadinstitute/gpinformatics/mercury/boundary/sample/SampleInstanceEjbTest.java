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
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleInstanceEntityDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VesselPooledTubesProcessor;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

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

    private final Random random = new Random(System.currentTimeMillis());

    @Test
    public void testPooledTubeUpload() throws Exception {
        // Makes randomized barcode, library, sample, root for the non-overwrite case.
        List<Map<String, String>> alternativeData = new ArrayList<>();
        int randomNumber = random.nextInt(100000000) * 10;
        for (int rowIndex : Arrays.asList(1, 2)) {
            String randomDigits = String.format("%010d", randomNumber + rowIndex);
            Map<String, String> row = new HashMap<>();
            row.put("Tube barcode", "E" + randomDigits);
            row.put("Single sample library name", "Library" + randomDigits);
            row.put("Broad sample ID", "SM-" + randomDigits);
            row.put("Root Sample ID", String.format("SM-%010d", randomNumber + (new int[]{1, 3})[rowIndex - 1]));
            alternativeData.add(row);
        }

        for (Pair<String, Boolean> fileSuccessPair : Arrays.asList(
                Pair.of("PooledTubeReg.xlsx", true),
                Pair.of("PooledTube_Test-363_case1.xls", true),
                Pair.of("PooledTube_Test-363_case2.xls", true),
                Pair.of("PooledTube_Test-363_case3.xls", false),
                Pair.of("PooledTube_Test-363_case4.xls", true))) {

            boolean expectSuccess = fileSuccessPair.getRight();
            String filename = fileSuccessPair.getLeft();
            boolean overwrite = filename.startsWith("PooledTube_Test-363_");

            byte[] bytes = IOUtils.toByteArray(Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(filename));
            Assert.assertTrue(bytes.length > 0, "Zero length spreadsheet file " + filename);

            // Writes unique barcode, library, etc. for the PooledTubeReg spreadsheet.
            if (filename.startsWith("PooledTubeReg")) {
                bytes = modifySpreadsheet(new ByteArrayInputStream(bytes), alternativeData);
            }

            TableProcessor testProcessor = sampleInstanceEjb.bestProcessor(new ByteArrayInputStream(bytes)).getLeft();
            Assert.assertTrue(testProcessor instanceof VesselPooledTubesProcessor);

            MessageCollection messageCollection = new MessageCollection();
            Pair<TableProcessor, List<SampleInstanceEntity>> pair = sampleInstanceEjb.doExternalUpload(
                    new ByteArrayInputStream(bytes), overwrite, messageCollection);

            VesselPooledTubesProcessor processor = (VesselPooledTubesProcessor)pair.getLeft();
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

                    SampleData rootSampleData = (rootSample != null) ? rootSample.getSampleData() : null;
                    if (!processor.getBroadParticipantId().get(i).isEmpty()) {
                        Assert.assertEquals((rootSampleData != null ? rootSampleData : sampleData)
                                .getPatientId(), processor.getBroadParticipantId().get(i), msg);
                    }
                    if (!processor.getCollaboratorSampleId().get(i).isEmpty()) {
                        Assert.assertEquals((rootSampleData != null ? rootSampleData : sampleData)
                                .getCollaboratorsSampleName(), processor.getCollaboratorSampleId().get(i), msg);
                    }
                    if (!processor.getCollaboratorParticipantId().get(i).isEmpty()) {
                        Assert.assertEquals((rootSampleData != null ? rootSampleData : sampleData)
                                .getCollaboratorParticipantId(), processor.getCollaboratorParticipantId().get(i),
                                msg);
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
                }
            } else {
                // The failing test case should not have persisted any rows.
                for (int i = 0; i < processor.getBarcodes().size(); ++i) {
                    String libraryName = processor.getSingleSampleLibraryName().get(i);
                    Assert.assertNull(sampleInstanceEntityDao.findByName(libraryName), filename + " " + libraryName);
                }
                // Checks the error messages for expected problems.
                // FYI metadata for SM-748OO is from Mercury and not BSP even though the sample is in BSP.
                List<String> errors = new ArrayList<>(messageCollection.getErrors());
                errorIfMissing(errors, filename, String.format(SampleInstanceEjb.CONFLICT, 2,
                        VesselPooledTubesProcessor.Headers.BROAD_PARTICIPANT_ID.getText(),
                        "987654321", "12001-015", "", ""));
                errorIfMissing(errors, filename, String.format(SampleInstanceEjb.CONFLICT, 2,
                        VesselPooledTubesProcessor.Headers.SPECIES.getText(), "canine", "null", "", ""));
                errorIfMissing(errors, filename, String.format(SampleInstanceEjb.CONFLICT, 2,
                        VesselPooledTubesProcessor.Headers.LSID.getText(), "lsid:1", "null", "", ""));
                errorIfMissing(errors, filename, String.format(SampleInstanceEjb.DUPLICATE, 3,
                        VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText() +
                                " in tube 01509634244"));
                errorIfMissing(errors, filename, String.format(SampleInstanceEjb.UNKNOWN, 3,
                        VesselPooledTubesProcessor.Headers.ROOT_SAMPLE_ID.getText(), "Mercury",
                        "Broad Sample already exists"));
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
                errorIfMissing(errors, filename, String.format(SampleInstanceEjb.CONFLICT, 8,
                        VesselPooledTubesProcessor.Headers.VOLUME.getText(), "61.00", "60.00", "", ""));
                errorIfMissing(errors, filename, String.format(SampleInstanceEjb.CONFLICT, 8,
                        VesselPooledTubesProcessor.Headers.FRAGMENT_SIZE.getText(), "151.00", "150.00", "", ""));
                errorIfMissing(errors, filename, String.format(SampleInstanceEjb.MUST_NOT_BE, 8,
                        VesselPooledTubesProcessor.Headers.BAIT.getText() + " and " +
                        VesselPooledTubesProcessor.Headers.CAT.getText(), "both"));
                errorIfMissing(errors, filename, String.format(SampleInstanceEjb.UNKNOWN, 8,
                        VesselPooledTubesProcessor.Headers.CONDITIONS.getText(), "sub-tasks of DEV-6796"));
                errorIfMissing(errors, filename, String.format(SampleInstanceEjb.CONFLICT, 8,
                        VesselPooledTubesProcessor.Headers.LSID.getText(), "lsid:3", "null", "", ""));
                errorIfMissing(errors, filename, String.format(SampleInstanceEjb.WRONG_TYPE, 9,
                        VesselPooledTubesProcessor.Headers.READ_LENGTH.getText(), "numeric"));
                errorIfMissing(errors, filename, String.format(SampleInstanceEjb.WRONG_TYPE, 9,
                        VesselPooledTubesProcessor.Headers.FRAGMENT_SIZE.getText(), "numeric"));
                errorIfMissing(errors, filename, String.format(SampleInstanceEjb.WRONG_TYPE, 9,
                        VesselPooledTubesProcessor.Headers.VOLUME.getText(), "numeric"));
                errorIfMissing(errors, filename, String.format(SampleInstanceEjb.UNKNOWN, 9,
                        VesselPooledTubesProcessor.Headers.CONDITIONS.getText(), "sub-tasks of DEV-6796"));
                errorIfMissing(errors, filename, String.format(SampleInstanceEjb.UNKNOWN, 9,
                        VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText(), "Mercury"));
                Assert.assertTrue(errors.isEmpty(), "Found unexpected errors: " + StringUtils.join(errors, "; "));
            }
        }
    }

    private boolean errorIfMissing(List<String> errors, String filename, String... tokens) {
        for (Iterator<String> iterator = errors.iterator(); iterator.hasNext(); ) {
            String error = iterator.next();
            boolean hasAllTokens = true;
            for (String token : tokens) {
                if (!error.contains(token)) {
                    hasAllTokens = false;
                    break;
                }
            }
            if (hasAllTokens) {
                iterator.remove();
                return true;
            }
        }
        Assert.fail(filename + " missing an error message that contains \"" +
                StringUtils.join(tokens, "\" and \"") + "\"; " +
                "Existing errors are: " + StringUtils.join(errors, "; "));
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
        return bytes;
    }
}