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
import org.hamcrest.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;


@Test(groups = TestGroups.DATABASE_FREE)
public class ManifestImportProcessorTest {
    private static final String SHEET_NAME = "what-evaaaah";
    private ManifestImportProcessor processor;
    private Map<String, String> dataRow;

    @BeforeMethod
    public void setUp() throws Exception {
        processor = new ManifestImportProcessor(SHEET_NAME);
        dataRow = makeDataRow();
    }

    public void testGetHeaderNames() throws Exception {
        processor.processHeader(new ArrayList<>(dataRow.keySet()), 0);
        ArrayList<String> allHeaders = new ArrayList<>();
        for (ManifestHeader manifestHeader : ManifestHeader.values()) {
            allHeaders.add(manifestHeader.getText());
        }

        assertThat(processor.getHeaderNames(), containsInAnyOrder(allHeaders.toArray()));
    }

    public void testGetColumns() throws Exception {
        processor.processHeader(new ArrayList<>(dataRow.keySet()), 0);
        ArrayList<String> allHeaders = new ArrayList<>();
        for (ManifestHeader manifestHeader : ManifestHeader.values()) {
            allHeaders.add(manifestHeader.getText());
        }

        assertThat(processor.getColumnHeaders(),
                arrayContainingInAnyOrder((ColumnHeader[]) ManifestHeader.values()));
    }


    public void testProcessHeader() throws Exception {
        processor.processHeader(new ArrayList<>(dataRow.keySet()), 0);
        ArrayList<String> allHeaders = new ArrayList<>();
        for (ManifestHeader manifestHeader : ManifestHeader.values()) {
            allHeaders.add(manifestHeader.getText());
        }

        assertThat(processor.getHeaderNames(), containsInAnyOrder(allHeaders.toArray()));
    }

    public void testProcessRowDetailsShouldPass() throws Exception {
        processor.processRowDetails(dataRow, 0);
        assertThat(processor.getMessages(), is(empty()));
    }

    public void testProcessHeadersUnknownColumn() throws Exception {
        String unknownHeader = "new to you";
        int row = 0;
        dataRow.put(unknownHeader, "new to me too!");
        processor.processHeader(new ArrayList<>(dataRow.keySet()), row);
        assertThat(processor.getWarnings(), Matchers.hasItem(
                String.format("Sheet %s, Row #%d " + ManifestImportProcessor.UNKNOWN_HEADER_FORMAT, SHEET_NAME, row,
                        unknownHeader)));
    }

    public void testGetColumnHeaders() throws Exception {
        String headerNamesArray[] = dataRow.keySet().toArray(new String[dataRow.keySet().size()]);

        processor.processHeader(Arrays.asList(headerNamesArray), 0);
        assertThat(processor.getHeaderNames(), containsInAnyOrder(headerNamesArray));
    }

    private Map<String, String> makeDataRow() {
        Map<String, String> dataRow = new HashMap<>();
        dataRow.put(ManifestHeader.STUDY_NUMBER.getText(), "C16002");
        dataRow.put(ManifestHeader.SAMPLE_ID.getText(), "03101231193");
        dataRow.put(ManifestHeader.PATIENT_ID.getText(), "004-002");
        dataRow.put(ManifestHeader.SEX.getText(), "");
        dataRow.put(ManifestHeader.SAMPLE_TYPE.getText(), "DNA");
        dataRow.put(ManifestHeader.TEST_NAME.getText(), "BM DNA Storage 1");
        dataRow.put(ManifestHeader.COLLECTION_DATE.getText(), "10-Oct-1841");
        dataRow.put(ManifestHeader.VISIT.getText(), "Screening");
        dataRow.put(ManifestHeader.TUMOR_OR_NORMAL.getText(), "Tumor");

        return dataRow;
    }
}
