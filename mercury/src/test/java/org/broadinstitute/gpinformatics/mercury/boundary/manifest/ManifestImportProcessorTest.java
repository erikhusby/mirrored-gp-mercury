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

import org.broadinstitute.gpinformatics.infrastructure.parsers.AccessioningColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor.UNKNOWN_HEADER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;


@Test(groups = TestGroups.DATABASE_FREE)
public class ManifestImportProcessorTest {
    private ManifestImportProcessor processor;
    private Map<String, String> dataRow;

    @BeforeMethod
    public void setUp() throws Exception {
        processor = new ManifestImportProcessor(ManifestSessionEjb.AccessioningProcessType.CRSP, "importFileName");
        dataRow = makeDataRow();
    }

    public void testGetHeaderNames() throws Exception {
        processor.processHeader(new ArrayList<>(dataRow.keySet()), 0);
        String[] allHeaders = new String[ManifestHeader.values().length];
        ManifestHeader[] values = ManifestHeader.values();
        for (int i = 0; i < values.length; i++) {
            ManifestHeader manifestHeader = values[i];
            allHeaders[i] = manifestHeader.getColumnName();
        }

        assertThat(processor.getHeaderNames(), containsInAnyOrder(allHeaders));
    }

    public void testGetHeaderNameWithNonexistentHeader() {
        String unknownHeaderName = "who, me?";
        try {
            ManifestHeader.fromColumnName(unknownHeaderName);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is(
                    AccessioningColumnHeader.NO_MANIFEST_HEADER_FOUND_FOR_COLUMN + unknownHeaderName));
        }
    }

    public void testGetColumns() throws Exception {
        processor.processHeader(new ArrayList<>(dataRow.keySet()), 0);

        assertThat(processor.getColumnHeaders(),
                arrayContainingInAnyOrder((ColumnHeader[]) ManifestHeader.values()));
    }


    public void testProcessHeader() throws Exception {
        processor.processHeader(new ArrayList<>(dataRow.keySet()), 0);
        List<String> allHeaders = new ArrayList<>();
        for (ManifestHeader manifestHeader : ManifestHeader.values()) {
            allHeaders.add(manifestHeader.getColumnName());
        }

        assertThat(processor.getHeaderNames(), containsInAnyOrder(allHeaders.toArray()));
    }

    public void testValidateHeader_Several_Bad_Headers() throws Exception {
        List<String> headers = new ArrayList<>(makeDataRow().keySet());
        List<String> unknownHeaders = Arrays.asList("bad header 1", "bad header 2");
        headers.addAll(unknownHeaders);
        boolean headersAreValid = processor.validateColumnHeaders(headers, 0);
        assertThat(headersAreValid, is(not(true)));
        assertThat(processor.getMessages(), contains(
                TableProcessor.getPrefixedMessage("Unknown header(s) \"bad header 1\", \"bad header 2\".", null, 1)));
        assertThat(processor.getWarnings(), hasSize(0));
    }

    public void testProcessRowDetailsShouldPass() throws Exception {
        processor.processRowDetails(dataRow, 0, true);
        processor.getMessages();
        assertThat(processor.getMessages(), is(empty()));
        for (ManifestRecord manifestRecord : processor.getManifestRecords()) {
            List<Metadata> expectedMetadata = new ArrayList<>();
            for (Map.Entry<String, String> dataRowEntry : dataRow.entrySet()) {
                expectedMetadata
                        .add(new Metadata(ManifestHeader.fromColumnName(dataRowEntry.getKey()).getMetadataKey(),
                                dataRowEntry.getValue()));
            }
            assertThat(manifestRecord.getMetadata().toArray(), arrayContainingInAnyOrder(expectedMetadata.toArray()));
        }

    }

    public void testProcessRowDetailsUnknownMaterialType() throws Exception {
        String unknownMaterial = "Goop";
        dataRow.put(ManifestHeader.MATERIAL_TYPE.getColumnName(), unknownMaterial);

        processor.processRowDetails(dataRow, 0, true);
        processor.getMessages();
        // A message about the first row in a spreadsheet should display "Row #1"
        assertThat(processor.getMessages(),
                hasItem(String.format("Row #1 An unrecognized material type was entered: %s", unknownMaterial)));
    }

    public void testProcessRowDetailsNullMaterialType() throws Exception {
        String nullMaterial = null;
        dataRow.put(ManifestHeader.MATERIAL_TYPE.getColumnName(), nullMaterial);

        processor.processRowDetails(dataRow, 0, true);
        processor.getMessages();
        assertThat(processor.getMessages(),
                hasItem(String.format("Row #1 An unrecognized material type was entered: %s", nullMaterial)));
    }


    public void testProcessHeadersUnknownColumn() throws Exception {
        String unknownHeader = "new to you";
        dataRow.put(unknownHeader, "new to me too!");
        processor.validateColumnHeaders(new ArrayList<>(dataRow.keySet()), 0);
        assertThat(processor.getMessages(), hasItem(
                TableProcessor.getPrefixedMessage(String.format(UNKNOWN_HEADER, unknownHeader), null, 1)));
        assertThat(processor.getWarnings(), hasSize(0));
    }

    public void testGetColumnHeaders() throws Exception {
        String headerNamesArray[] = dataRow.keySet().toArray(new String[dataRow.keySet().size()]);

        processor.processHeader(Arrays.asList(headerNamesArray), 0);
        assertThat(processor.getHeaderNames(), containsInAnyOrder(headerNamesArray));
    }

    private Map<String, String> makeDataRow() {
        Map<String, String> dataRow = new HashMap<>();
        dataRow.put(ManifestHeader.SPECIMEN_NUMBER.getColumnName(), "03101231193");
        dataRow.put(ManifestHeader.PATIENT_ID.getColumnName(), "004-002");
        dataRow.put(ManifestHeader.SEX.getColumnName(), "");
        dataRow.put(ManifestHeader.VISIT.getColumnName(), "Screening");
        dataRow.put(ManifestHeader.COLLECTION_DATE.getColumnName(), "10-Oct-1841");
        dataRow.put(ManifestHeader.TUMOR_OR_NORMAL.getColumnName(), "Tumor");
        dataRow.put(ManifestHeader.MATERIAL_TYPE.getColumnName(), "DNA");

        return dataRow;
    }
}
