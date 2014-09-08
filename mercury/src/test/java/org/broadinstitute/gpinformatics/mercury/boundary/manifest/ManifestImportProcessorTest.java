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

import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;


@Test(groups = TestGroups.DATABASE_FREE)
public class ManifestImportProcessorTest {
    private ManifestImportProcessor processor;
    private Map<String, String> dataRow;
    protected static final String TEST_UNKNOWN_HEADER_FORMAT =
            "Row #%d " + ManifestImportProcessor.UNKNOWN_HEADER_FORMAT;

    @BeforeMethod
    public void setUp() throws Exception {
        processor = new ManifestImportProcessor();
        dataRow = makeDataRow();
    }

    public void testGetHeaderNames() throws Exception {
        processor.processHeader(new ArrayList<>(dataRow.keySet()), 0);
        String[] allHeaders = new String[ManifestHeader.values().length];
        ManifestHeader[] values = ManifestHeader.values();
        for (int i = 0; i < values.length; i++) {
            ManifestHeader manifestHeader = values[i];
            allHeaders[i] = manifestHeader.getColumnHeader();
        }

        assertThat(processor.getHeaderNames(), containsInAnyOrder(allHeaders));
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
            allHeaders.add(manifestHeader.getColumnHeader());
        }

        assertThat(processor.getHeaderNames(), containsInAnyOrder(allHeaders.toArray()));
    }

    public void testProcessRowDetailsShouldPass() throws Exception {
        processor.processRowDetails(dataRow, 0);
        processor.getMessages();
        assertThat(processor.getMessages(), is(empty()));
        for (ManifestRecord manifestRecord : processor.getManifestRecords()) {
            List<Metadata> expectedMetadata = new ArrayList<>();
            for (Map.Entry<String, String> stringStringEntry : dataRow.entrySet()) {
                expectedMetadata
                        .add(new Metadata(ManifestHeader.fromColumnHeader(stringStringEntry.getKey()).getMetadataKey(),
                                stringStringEntry.getValue()));
            }
            assertThat(manifestRecord.getMetadata().toArray(), arrayContainingInAnyOrder(expectedMetadata.toArray()));
        }

    }

    public void testProcessHeadersUnknownColumn() throws Exception {
        String unknownHeader = "new to you";
        int row = 0;
        dataRow.put(unknownHeader, "new to me too!");
        processor.processHeader(new ArrayList<>(dataRow.keySet()), row);
        assertThat(processor.getMessages(),
                hasItem(String.format(TEST_UNKNOWN_HEADER_FORMAT, row, Arrays.asList(unknownHeader))));
    }

    public void testGetColumnHeaders() throws Exception {
        String headerNamesArray[] = dataRow.keySet().toArray(new String[dataRow.keySet().size()]);

        processor.processHeader(Arrays.asList(headerNamesArray), 0);
        assertThat(processor.getHeaderNames(), containsInAnyOrder(headerNamesArray));
    }

    private Map<String, String> makeDataRow() {
        Map<String, String> dataRow = new HashMap<>();
        dataRow.put(ManifestHeader.SPECIMEN_NUMBER.getColumnHeader(), "03101231193");
        dataRow.put(ManifestHeader.PATIENT_ID.getColumnHeader(), "004-002");
        dataRow.put(ManifestHeader.SEX.getColumnHeader(), "");
        dataRow.put(ManifestHeader.VISIT.getColumnHeader(), "Screening");
        dataRow.put(ManifestHeader.COLLECTION_DATE.getColumnHeader(), "10-Oct-1841");
        dataRow.put(ManifestHeader.TUMOR_OR_NORMAL.getColumnHeader(), "Tumor");

        return dataRow;
    }
}
