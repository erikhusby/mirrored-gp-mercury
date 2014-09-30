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

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.common.TestUtils;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetValidator;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.startsWith;

@Test(groups = TestGroups.DATABASE_FREE)
public class ManifestImporterTest {
    private static final String TEST_MANIFEST_DIRECTORY = "manifest-import";
    private static final String TEST_MANIFEST_XLS = relativePathToTestFile("test-manifest.xls");
    private static final String TEST_MANIFEST_XLSX = relativePathToTestFile("test-manifest.xlsx");
    private static final String MISSING_REQUIRED_VALUE = relativePathToTestFile("test-manifest-missing-required.xlsx");
    private static final String UNKNOWN_HEADERS = relativePathToTestFile("test-manifest-unknown-headers.xlsx");
    private static final String TOO_MANY_SHEETS = relativePathToTestFile("test-manifest-too-many-sheets.xlsx");
    private static final String REARRANGED_COLUMNS = relativePathToTestFile("test-manifest-rearranged-columns.xlsx");
    private static final String DUPLICATE_COLUMNS = relativePathToTestFile("test-manifest-duplicate-columns.xlsx");
    private static final String ROW_NUMBER_PREFIX = "Row #%s ";

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
    public void testImport(String excelFileName) throws InvalidFormatException, IOException, ValidationException {
        InputStream inputStream = new FileInputStream(excelFileName);
        PoiSpreadsheetParser.processSingleWorksheet(inputStream, manifestImportProcessor);

        validateManifestRecords(manifestImportProcessor);
        assertThat(manifestImportProcessor.getMessages(), emptyCollectionOf(String.class));
        assertThat(manifestImportProcessor.getWarnings(), emptyCollectionOf(String.class));
    }

    @Test(expectedExceptions = ValidationException.class)
    public void testImportMissingRequiredValue() throws InvalidFormatException, IOException, ValidationException {
        InputStream inputStream = new FileInputStream(TestUtils.getTestData(MISSING_REQUIRED_VALUE));
        PoiSpreadsheetParser.processSingleWorksheet(inputStream, manifestImportProcessor);

        validateManifestRecords(manifestImportProcessor);
        assertThat(manifestImportProcessor.getMessages(),
                hasItem(String.format(ROW_NUMBER_PREFIX + TableProcessor.REQUIRED_VALUE_IS_MISSING, 1,
                        ManifestHeader.SPECIMEN_NUMBER.getColumnName())));
        assertThat(manifestImportProcessor.getWarnings(), emptyCollectionOf(String.class));
    }

    @Test(expectedExceptions = ValidationException.class)
    public void testImportTwoManyWorksheets() throws InvalidFormatException, IOException, ValidationException {
        InputStream inputStream = new FileInputStream(TestUtils.getTestData(TOO_MANY_SHEETS));
        PoiSpreadsheetParser.processSingleWorksheet(inputStream, manifestImportProcessor);
    }

    @Test(expectedExceptions = ValidationException.class)
    public void testImportExtraHeaders() throws InvalidFormatException, IOException, ValidationException {
        InputStream inputStream = new FileInputStream(TestUtils.getTestData(UNKNOWN_HEADERS));
        PoiSpreadsheetParser.processSingleWorksheet(inputStream, manifestImportProcessor);

        validateManifestRecords(manifestImportProcessor);
        String expectedError =
                String.format(ManifestImportProcessorTest.TEST_UNKNOWN_HEADER_FORMAT, 0, Arrays.asList("YOMAMA"));
        assertThat(manifestImportProcessor.getMessages(), contains(expectedError));
        assertThat(manifestImportProcessor.getWarnings(), emptyCollectionOf(String.class));
    }

    public void testImportRearrangedColumns() throws InvalidFormatException, IOException, ValidationException {
        InputStream inputStream = new FileInputStream(TestUtils.getTestData(REARRANGED_COLUMNS));
        PoiSpreadsheetParser.processSingleWorksheet(inputStream, manifestImportProcessor);

        validateManifestRecords(manifestImportProcessor);
        assertThat(manifestImportProcessor.getMessages(), emptyCollectionOf(String.class));
        assertThat(manifestImportProcessor.getWarnings(), emptyCollectionOf(String.class));
    }

    @Test(expectedExceptions = ValidationException.class)
    public void testImportDuplicateColumns() throws InvalidFormatException, IOException, ValidationException {
        InputStream inputStream = new FileInputStream(TestUtils.getTestData(DUPLICATE_COLUMNS));
        PoiSpreadsheetParser.processSingleWorksheet(inputStream, manifestImportProcessor);

        validateManifestRecords(manifestImportProcessor);
        String expectedError =
                String.format(ROW_NUMBER_PREFIX + ManifestImportProcessor.DUPLICATE_HEADER_FORMAT, 0, "Patient_ID");
        assertThat(manifestImportProcessor.getMessages(), hasItem(expectedError));
        assertThat(manifestImportProcessor.getWarnings(), emptyCollectionOf(String.class));
    }

    /**
     * Basic sweep of manifest importing results. This asserts basic assumptions about the import are true. Validations
     * specific to a particular test must be run there.
     *
     * @param manifestImportProcessor ManifestImportProcessor which was used in an import.
     *
     * @throws ValidationException for any violations of data importing rules.
     */
    private void validateManifestRecords(ManifestImportProcessor manifestImportProcessor)
            throws ValidationException {
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
                manifestRow.put(header.getColumnName(), metadata.getStringValue());
            }
            PoiSpreadsheetValidator.validateSpreadsheetRow(manifestRow, ManifestHeader.class);
            for (Map.Entry<String, String> manifestCell : manifestRow.entrySet()) {
                String value = manifestCell.getValue();
                switch (ManifestHeader.fromColumnName(manifestCell.getKey())) {
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
                }
            }
        }

    }
}
