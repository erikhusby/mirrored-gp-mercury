package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetValidator;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestHeader;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

public class PlateMetadataImportProcessorTest {
    private static final String TEST_MANIFEST_DIRECTORY = "plate-receipt-metadata";
    private static final String TEST_MANIFEST_XLSX = TEST_MANIFEST_DIRECTORY + "/SingleCellGood.xlsx";
    private static final String TEST_MANIFEST_DUPLICATE_COL_XLSX = TEST_MANIFEST_DIRECTORY + "/SingleCellDuplicateCols.xlsx";
    private static final String TEST_MANIFEST_MULTI_PLATE_COL_XLSX = TEST_MANIFEST_DIRECTORY + "/SingleCellMultiPlates.xlsx";

    private PlateMetadataImportProcessor processor;

    @BeforeMethod
    public void setUp() throws Exception {
        processor = new PlateMetadataImportProcessor();
    }

    @Test
    public void testGoodUpload() throws Exception {
        InputStream inputStream = VarioskanParserTest.getSpreadsheet(TEST_MANIFEST_XLSX);
        PoiSpreadsheetParser.processSingleWorksheet(inputStream, processor);

        validateRecords(processor);
        assertThat(processor.getMessages(), emptyCollectionOf(String.class));
        assertThat(processor.getWarnings(), emptyCollectionOf(String.class));
    }

    @Test(expectedExceptions = ValidationException.class)
    public void testImportDuplicateColumns() throws Exception {
        InputStream inputStream = VarioskanParserTest.getSpreadsheet(TEST_MANIFEST_DUPLICATE_COL_XLSX);
        PoiSpreadsheetParser.processSingleWorksheet(inputStream, processor);
        validateRecords(processor);
    }

    @Test(expectedExceptions = ValidationException.class)
    public void testTooManyPlateBarcodes() throws Exception {
        InputStream inputStream = VarioskanParserTest.getSpreadsheet(TEST_MANIFEST_MULTI_PLATE_COL_XLSX);
        PoiSpreadsheetParser.processSingleWorksheet(inputStream, processor);
        validateRecords(processor);
    }

    /**
     * Basic sweep of manifest importing results. This asserts basic assumptions about the import are true. Validations
     * specific to a particular test must be run there.
     *
     * @param processor which was used in an import.
     *
     * @throws ValidationException for any violations of data importing rules.
     */
    private void validateRecords(PlateMetadataImportProcessor processor) throws ValidationException {
        List<RowMetadata> rowRecords;
        try {
            rowRecords = processor.getRowMetadataRecords();
        } catch (ValidationException e) {
            assertThat(e.getValidationMessages(), equalTo(processor.getMessages()));
            throw e;
        }
        Map<String, String> rowData = new HashMap<>();
        for (RowMetadata record : rowRecords) {
            for (Metadata metadata : record.getMetadata()) {
                String value = metadata.getValue();
                String well = record.getWell();
                switch (metadata.getKey()) {
                case SPECIES:
                    break;
                case CELL_TYPE:
                    assertThat(value, equalTo("t cell"));
                    break;
                case CELLS_PER_WELL:
                    assertThat(value, equalTo("1"));
                    break;
                case SAMPLE_ID:
                    assertThat(value, startsWith("CS-ID-"));
                    break;
                case PATIENT_ID:
                    assertThat(value, startsWith("CP-ID-"));
                    break;
                case COLLECTION_DATE:
                    assertThat(value, startsWith("10-May"));
                    break;
                case POSITIVE_CONTROL:
                    assertThat(value, startsWith("Positive Control"));
                    break;
                case NEGATIVE_CONTROL:
                    assertThat(value, startsWith("Negative Control"));
                    break;
                default:
                    throw new RuntimeException("Unknown metadata key in file: " + metadata.getKey().getDisplayName());
                }
            }
            PoiSpreadsheetValidator.validateSpreadsheetRow(rowData, ManifestHeader.class);

        }
    }
}