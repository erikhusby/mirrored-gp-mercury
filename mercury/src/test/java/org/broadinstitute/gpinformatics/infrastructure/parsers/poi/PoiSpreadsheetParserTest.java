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

package org.broadinstitute.gpinformatics.infrastructure.parsers.poi;

import org.broadinstitute.gpinformatics.infrastructure.common.TestUtils;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Test(enabled = true)
public class PoiSpreadsheetParserTest {
    private static final String POI_TEST_XLS = "poi-test.xls";
    private static final String POI_TEST_XLSX = "poi-test.xlsx";
    private TestProcessor testProcessor;

    @BeforeMethod
    public void setUp() throws Exception {
        testProcessor = new TestProcessor();
    }

    @DataProvider(name = "excelFileDataProvider")
    public static Object[][] excelFileDataProvider() throws FileNotFoundException {
        return new Object[][]{
                new Object[]{new FileInputStream(TestUtils.getTestData(POI_TEST_XLS))},
                new Object[]{new FileInputStream(TestUtils.getTestData(POI_TEST_XLSX))},
        };

    }

    @Test(groups = TestGroups.DATABASE_FREE, dataProvider = "excelFileDataProvider")
    public void testParseAndValidateExcelFile(InputStream testFileInputStream) throws Exception {
        PoiSpreadsheetParser.processSingleWorksheet(testFileInputStream, testProcessor);
        for (Map<String, String> spreadsheetRowValues : testProcessor.getSpreadsheetValues()) {
            String calculatedValue = spreadsheetRowValues.get(TestHeaders.calculated.getText());
            String expectedValue = spreadsheetRowValues.get(TestHeaders.expected.getText());
            String failedReason = spreadsheetRowValues.get(TestHeaders.testname.getText()) + " failed.";
            assertThat(failedReason, calculatedValue, equalTo(expectedValue));
            PoiSpreadsheetValidator.validateSpreadsheetRow(spreadsheetRowValues, TestHeaders.class);
        }
    }

    enum TestHeaders implements ColumnHeader {
        testname(0, false, true, true),
        stringData1(1, false, true, false),
        stringData2(2, false, true, false),
        numericData1(3, false, false, false),
        numericData2(4, false, false, false),
        calculated(5, false, true, false),
        expected(6, false, true, false),
        aBoolean(7, false, false, false),
        aDate(8, true, false, false);
        private final int index;
        private final boolean isDateColumn;
        private final boolean isStringColumn;
        private final boolean requiredValue;
        private final String text;

        TestHeaders(int index, boolean isDateColumn, boolean isStringColumn, boolean requiredValue) {
            this.index = index;
            this.isDateColumn = isDateColumn;
            this.isStringColumn = isStringColumn;
            this.requiredValue = requiredValue;
            this.text = name();
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public boolean isRequiredHeader() {
            return false;
        }

        @Override
        public boolean isRequiredValue() {
            return requiredValue;
        }

        @Override
        public boolean isDateColumn() {
            return isDateColumn;
        }

        @Override
        public boolean isStringColumn() {
            return isStringColumn;
        }

    }

    private class TestProcessor extends TableProcessor {
        private List<Map<String, String>> spreadsheetValues = new ArrayList<>();

        private TestProcessor() {
            super(null);
        }

        @Override
        public List<String> getHeaderNames() {
            List<String> headers = new ArrayList<>();
            for (TestHeaders testHeaders : TestHeaders.values()) {
                headers.add(testHeaders.getText());
            }
            return headers;
        }

        @Override
        public void processHeader(List<String> headers, int row) {

        }

        @Override
        public void processRowDetails(Map<String, String> dataRow, int dataRowIndex) {
            spreadsheetValues.add(dataRow);
        }

        @Override
        protected ColumnHeader[] getColumnHeaders() {
            return TestHeaders.values();
        }

        @Override
        public void close() {

        }

        public List<Map<String, String>> getSpreadsheetValues() {
            return spreadsheetValues;
        }
    }
}
