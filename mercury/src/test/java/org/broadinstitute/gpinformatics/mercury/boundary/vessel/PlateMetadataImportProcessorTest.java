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

    /**
     * Basic sweep of manifest importing results. This asserts basic assumptions about the import are true. Validations
     * specific to a particular test must be run there.
     *
     * @param PlateMetadataImportProcessor processor which was used in an import.
     *
     * @throws ValidationException for any violations of data importing rules.
     */
    private void validateRecords(PlateMetadataImportProcessor processor) throws ValidationException {
        List<PlateMetadataImportProcessor.RowMetadata> rowRecords;
        try {
            rowRecords = processor.getRowMetadataRecords();
        } catch (ValidationException e) {
            assertThat(e.getValidationMessages(), equalTo(processor.getMessages()));
            throw e;
        }
        Map<String, String> rowData = new HashMap<>();
        for (PlateMetadataImportProcessor.RowMetadata record : rowRecords) {
            for (Metadata metadata : record.getMetadata()) {
                String value = metadata.getValue();
                String well = record.getWell();
                switch (metadata.getKey()) {
                case SAMPLE_ID:
                    assertThat(value, startsWith("SM-"));
                    break;
                case SPECIES:
                    break;
                case CELL_TYPE:
                    assertThat(value, equalTo("t cell"));
                    break;
                case CELLS_PER_WELL:
                    assertThat(value, equalTo(1));
                    break;
                case COLLABORATOR_SAMPLE_ID:
                    assertThat(value, startsWith("CS-ID-"));
                    break;
                case POSITIVE_CONTROL:
                    if (well.equalsIgnoreCase("A01")) {
                        assertThat(value, startsWith("Yes"));
                    } else {
                        throw new RuntimeException("Don't expect Positive Control metadata at position " + well);
                    }
                    break;
                case NEGATIVE_CONTROL:
                    if (well.equalsIgnoreCase("D01")) {
                        assertThat(value, startsWith("Yes"));
                    } else {
                        throw new RuntimeException("Don't expect Positive Control metadata at position " + well);
                    }
                    break;
                case COLLABORATOR_PARTICIPANT_ID:
                    assertThat(value, startsWith("CP-ID-"));
                    break;
                default:
                    throw new RuntimeException("Unknown metadata key in file: " + metadata.getKey().getDisplayName());
                }
            }
            PoiSpreadsheetValidator.validateSpreadsheetRow(rowData, ManifestHeader.class);

        }
    }
}