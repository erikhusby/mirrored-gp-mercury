/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.common.TestUtils;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetValidator;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

@Test(groups = TestGroups.DATABASE_FREE)
public class ManifestImporterTest {
    private static final String TEST_MANIFEST_DIRECTORY = "manifest-import";
    private static final String TEST_MANIFEST_XLS = relativePathToTestFile("test-manifest.xls");
    private static final String TEST_MANIFEST_XLSX = relativePathToTestFile("test-manifest.xlsx");
    private static final String MISSING_REQUIRED_SPECIMEN = relativePathToTestFile("test-manifest-missing-specimen.xlsx");
    private static final String MISSING_REQUIRED_VISIT = relativePathToTestFile("test-manifest-missing-visit.xlsx");
    private static final String MISSING_REQUIRED_COLLECTION_DATE = relativePathToTestFile("test-manifest-missing-collection_date.xlsx");
    private static final String MISSING_REQUIRED_SEX = relativePathToTestFile("test-manifest-missing-sex.xlsx");
    private static final String MISSING_REQUIRED_TUMOR_NORMAL = relativePathToTestFile("test-manifest-missing-tn.xlsx");
    private static final String MISSING_REQUIRED_MATERIAL_TYPE = relativePathToTestFile("test-manifest-missing-material-type.xlsx");
    private static final String MISSING_REQUIRED_PATIENT = relativePathToTestFile("test-manifest-missing-patient.xlsx");
    private static final String MISSING_HEADER_PATIENT = relativePathToTestFile("missing-patient-header.xlsx");
    private static final String UNKNOWN_HEADERS = relativePathToTestFile("test-manifest-unknown-headers.xlsx");
    private static final String TOO_MANY_SHEETS = relativePathToTestFile("test-manifest-too-many-sheets.xlsx");
    private static final String REARRANGED_COLUMNS = relativePathToTestFile("test-manifest-rearranged-columns.xlsx");
    private static final String DUPLICATE_COLUMNS = relativePathToTestFile("test-manifest-duplicate-columns.xlsx");
    private static final String EMPTY_MANIFEST= relativePathToTestFile("empty.xlsx");
    private static final String HEADERS_ONLY= relativePathToTestFile("headers-only.xlsx");
    private static final String VALIDATION_EXCEPTION_MESSAGE = "This test should have thrown a ValidationException.";

    private ManifestImportProcessor manifestImportProcessor;

    @BeforeMethod
    public void setUp() throws Exception {
        manifestImportProcessor = new ManifestImportProcessor();
    }

    @DataProvider(name = "excelFileDataProvider")
    public Object[][] excelFileDataProvider() throws FileNotFoundException {
        return new Object[][]{
                {TestUtils.getTestData(TEST_MANIFEST_XLS)}, {TestUtils.getTestData(TEST_MANIFEST_XLSX)},
        };

    }


    private static String relativePathToTestFile(String fileName) {
        return TEST_MANIFEST_DIRECTORY + "/" + fileName;
    }

    @Test(dataProvider = "excelFileDataProvider")
    public void testImport(String excelFileName) throws Exception {
        InputStream inputStream = new FileInputStream(excelFileName);
        PoiSpreadsheetParser.processSingleWorksheet(inputStream, manifestImportProcessor);

        validateManifestRecords(manifestImportProcessor);
        assertThat(manifestImportProcessor.getMessages(), emptyCollectionOf(String.class));
        assertThat(manifestImportProcessor.getWarnings(), emptyCollectionOf(String.class));
    }

    @DataProvider(name = "missingRequiredDataProvider")
    public static Object[][] missingRequiredDataProvider() {
        return new Object[][]{
                {MISSING_REQUIRED_COLLECTION_DATE, ManifestHeader.COLLECTION_DATE},
                {MISSING_REQUIRED_PATIENT, ManifestHeader.PATIENT_ID},
                {MISSING_REQUIRED_SEX, ManifestHeader.SEX},
                {MISSING_REQUIRED_SPECIMEN, ManifestHeader.SPECIMEN_NUMBER},
                {MISSING_REQUIRED_TUMOR_NORMAL, ManifestHeader.TUMOR_OR_NORMAL},
                {MISSING_REQUIRED_MATERIAL_TYPE, ManifestHeader.MATERIAL_TYPE},
                {MISSING_REQUIRED_VISIT, ManifestHeader.VISIT}
        };

    }

    @Test(dataProvider = "missingRequiredDataProvider")
    public void testImportMissing(String fileName, ManifestHeader header) throws Exception {
        InputStream inputStream = new FileInputStream(TestUtils.getTestData(fileName));

        try {
            PoiSpreadsheetParser.processSingleWorksheet(inputStream, manifestImportProcessor);
            validateManifestRecords(manifestImportProcessor);
            Assert.fail(VALIDATION_EXCEPTION_MESSAGE);
        } catch (ValidationException e) {
            assertThat(manifestImportProcessor.getMessages(),
                    hasItem(TableProcessor.getPrefixedMessage(
                            String.format(TableProcessor.REQUIRED_VALUE_IS_MISSING, header.getColumnName()), null, 1)));
            assertThat(manifestImportProcessor.getWarnings(), emptyCollectionOf(String.class));
        }
    }

    public void testImportMissingHeader() throws Exception {
        InputStream inputStream = new FileInputStream(TestUtils.getTestData(MISSING_HEADER_PATIENT));

        try {
            PoiSpreadsheetParser.processSingleWorksheet(inputStream, manifestImportProcessor);
            validateManifestRecords(manifestImportProcessor);
            Assert.fail(VALIDATION_EXCEPTION_MESSAGE);
        } catch (ValidationException e) {
            assertThat(manifestImportProcessor.getMessages(), hasItem(String.format(
                    TableProcessor.REQUIRED_HEADER_IS_MISSING, ManifestHeader.PATIENT_ID.getColumnName())));
            assertThat(manifestImportProcessor.getWarnings(), emptyCollectionOf(String.class));
        }
    }

    @Test(expectedExceptions = ValidationException.class)
    public void testImportTwoManyWorksheets() throws Exception {
        InputStream inputStream = new FileInputStream(TestUtils.getTestData(TOO_MANY_SHEETS));
        PoiSpreadsheetParser.processSingleWorksheet(inputStream, manifestImportProcessor);
    }

    @Test(expectedExceptions = ValidationException.class)
    public void testImportExtraHeaders() throws Exception {
        InputStream inputStream = new FileInputStream(TestUtils.getTestData(UNKNOWN_HEADERS));
        PoiSpreadsheetParser.processSingleWorksheet(inputStream, manifestImportProcessor);

        validateManifestRecords(manifestImportProcessor);
        String expectedError = String.format(TableProcessor.UNKNOWN_HEADER, "YOMAMA", 1);
        assertThat(manifestImportProcessor.getMessages(), contains(expectedError));
        assertThat(manifestImportProcessor.getWarnings(), emptyCollectionOf(String.class));
    }

    public void testImportRearrangedColumns() throws Exception {
        InputStream inputStream = new FileInputStream(TestUtils.getTestData(REARRANGED_COLUMNS));
        PoiSpreadsheetParser.processSingleWorksheet(inputStream, manifestImportProcessor);

        validateManifestRecords(manifestImportProcessor);
        assertThat(manifestImportProcessor.getMessages(), emptyCollectionOf(String.class));
        assertThat(manifestImportProcessor.getWarnings(), emptyCollectionOf(String.class));
    }


    @Test(expectedExceptions = ValidationException.class)
    public void testImportDuplicateColumns() throws Exception {
        InputStream inputStream = new FileInputStream(TestUtils.getTestData(DUPLICATE_COLUMNS));
        PoiSpreadsheetParser.processSingleWorksheet(inputStream, manifestImportProcessor);

        validateManifestRecords(manifestImportProcessor);
        String expectedError = String.format(TableProcessor.DUPLICATE_HEADER, "Patient_ID", 0);
        assertThat(manifestImportProcessor.getMessages(), hasItem(expectedError));
        assertThat(manifestImportProcessor.getWarnings(), emptyCollectionOf(String.class));
    }


    public void testEmptyFile() throws Exception {
        InputStream inputStream = new FileInputStream(TestUtils.getTestData(EMPTY_MANIFEST));
        try {
            PoiSpreadsheetParser.processSingleWorksheet(inputStream, manifestImportProcessor);
            Assert.fail();
        } catch (ValidationException e) {
            assertThat(manifestImportProcessor.getMessages(), hasItem(
                    String.format(TableProcessor.REQUIRED_HEADER_IS_MISSING, "Specimen_Number")));
            assertThat(manifestImportProcessor.getMessages(), hasItem(
                    String.format(TableProcessor.REQUIRED_HEADER_IS_MISSING, "Sex")));
            assertThat(manifestImportProcessor.getMessages(), hasItem(
                    String.format(TableProcessor.REQUIRED_HEADER_IS_MISSING, "Patient_ID")));
            assertThat(manifestImportProcessor.getMessages(), hasItem(
                    String.format(TableProcessor.REQUIRED_HEADER_IS_MISSING, "Collection_Date")));
            assertThat(manifestImportProcessor.getMessages(), hasItem(
                    String.format(TableProcessor.REQUIRED_HEADER_IS_MISSING, "Visit")));
            assertThat(manifestImportProcessor.getMessages(), hasItem(
                    String.format(TableProcessor.REQUIRED_HEADER_IS_MISSING, "SAMPLE_TYPE")));
            assertThat(manifestImportProcessor.getMessages(), hasItem(
                    String.format(TableProcessor.REQUIRED_HEADER_IS_MISSING, "Material Type")));
        }
    }

    public void testManifestFileHasHeadersButNoData() throws Exception {
        InputStream inputStream = new FileInputStream(TestUtils.getTestData(HEADERS_ONLY));
        try {
            PoiSpreadsheetParser.processSingleWorksheet(inputStream, manifestImportProcessor);
            validateManifestRecords(manifestImportProcessor);
            Assert.fail();
        } catch (ValidationException e) {
            assertThat(e.getValidationMessages(), hasItem(ManifestImportProcessor.NO_DATA_ERROR));
        }
    }

    /**
     * Basic sweep of manifest importing results. This asserts basic assumptions about the import are true. Validations
     * specific to a particular test must be run there.
     *
     * @param manifestImportProcessor ManifestImportProcessor which was used in an import.
     *
     * @throws ValidationException for any violations of data importing rules.
     */
    private void validateManifestRecords(ManifestImportProcessor manifestImportProcessor) throws ValidationException {
        Collection<ManifestRecord> manifestRecords;
        try {
            manifestRecords = manifestImportProcessor.getManifestRecords();
        } catch (ValidationException e) {
            assertThat(e.getValidationMessages(), equalTo(manifestImportProcessor.getMessages()));
            throw e;
        }
        Map<String, String> manifestRow = new HashMap<>();
        for (ManifestRecord manifestRecord : manifestRecords) {
            for (Metadata metadata : manifestRecord.getMetadata()) {
                ManifestHeader header = ManifestHeader.fromMetadataKey(metadata.getKey());
                manifestRow.put(header.getColumnName(), metadata.getValue());
            }
            PoiSpreadsheetValidator.validateSpreadsheetRow(manifestRow, ManifestHeader.class);
            for (Map.Entry<String, String> manifestCell : manifestRow.entrySet()) {
                String value = manifestCell.getValue();
                ManifestHeader manifestHeader = ManifestHeader.fromColumnName(manifestCell.getKey());
                switch (manifestHeader) {
                case TUMOR_OR_NORMAL:
                    assertThat(value, isOneOf("Tumor", "Normal"));
                    break;
                case SEX:
                    assertThat(value, isOneOf("Male", "Female"));
                    break;
                case VISIT:
                    assertThat(value, is("Screening"));
                    break;
                case SPECIMEN_NUMBER:
                    assertThat(value, startsWith("0310"));
                    break;
                case PATIENT_ID:
                    assertThat(value, startsWith("00"));
                    break;
                default:
                    if (manifestHeader.isRequiredHeader()) {
                        assertThat(value, not(isEmptyOrNullString()));
                    }
                }
            }
        }

    }
}
