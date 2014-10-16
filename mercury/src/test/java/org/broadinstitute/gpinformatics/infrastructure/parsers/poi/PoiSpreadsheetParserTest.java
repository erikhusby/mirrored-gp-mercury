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

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.common.TestUtils;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@Test(groups = TestGroups.DATABASE_FREE)
public class PoiSpreadsheetParserTest {
    private static final String POI_TEST_XLS = "poi-test.xls";
    private static final String POI_TEST_XLSX = "poi-test.xlsx";
    private static final String POI_TEST_TRAILING_BLANK_LINES = "poi-test-trailing-blank-lines.xlsx";
    private static final String POI_TEST_INTERVENING_BLANK_LINES = "poi-test-intervening-blank-lines.xlsx";

    private TestProcessor testProcessor;
    private TestProcessor testProcessorIgnoreTrailingBlankLines;

    @BeforeMethod
    public void setUp() throws Exception {
        testProcessor = new TestProcessor();
        testProcessorIgnoreTrailingBlankLines = new TestProcessor(TableProcessor.IgnoreTrailingBlankLines.YES);
    }

    @DataProvider(name = "excelFileDataProvider")
    public static Object[][] excelFileDataProvider() throws FileNotFoundException {
        return new Object[][]{
                {new FileInputStream(TestUtils.getTestData(POI_TEST_XLS))},
                {new FileInputStream(TestUtils.getTestData(POI_TEST_XLSX))},
        };
    }

    @Test(dataProvider = "excelFileDataProvider")
    public void testParseAndValidateExcelFile(InputStream testFileInputStream) throws Exception {
        PoiSpreadsheetParser.processSingleWorksheet(testFileInputStream, testProcessor);
        assertThat(testProcessor.getMessages(), emptyCollectionOf(String.class));
        for (Map<String, String> spreadsheetRowValues : testProcessor.getSpreadsheetValues()) {
            PoiSpreadsheetValidator.validateSpreadsheetRow(spreadsheetRowValues, TestHeaders.class);
        }
    }

    @Test(dataProvider = "excelFileDataProvider", expectedExceptions = ValidationException.class)
    public void testParseAndValidateExcelFileTooManyRowsFails(InputStream testFileInputStream) throws Exception {
        PoiSpreadsheetParser.processSingleWorksheet(testFileInputStream, new TestProcessor() {
            @Override
            public void validateNumberOfWorksheets(int actualNumberOfSheets) throws ValidationException {
                throw new ValidationException("No, No!, Noooooooeoo!");
            }
        });
    }

    @Test(dataProvider = "excelFileDataProvider")
    public void testParseAndValidateExcelFileTooManyRowsPasses(InputStream testFileInputStream) throws Exception {
        PoiSpreadsheetParser.processSingleWorksheet(testFileInputStream, testProcessor);
        assertThat(testProcessor.getMessages(), emptyCollectionOf(String.class));
        assertThat(testProcessor.getWarnings(), emptyCollectionOf(String.class));
    }

    enum TestHeaders implements ColumnHeader {
        testname(0, ColumnHeader.NON_DATE, ColumnHeader.IS_STRING, ColumnHeader.REQUIRED_VALUE),
        stringData1(1, ColumnHeader.NON_DATE, ColumnHeader.IS_STRING, ColumnHeader.OPTIONAL_VALUE),
        stringData2(2, ColumnHeader.NON_DATE, ColumnHeader.IS_STRING, ColumnHeader.OPTIONAL_VALUE),
        numericData1(3, ColumnHeader.NON_DATE, ColumnHeader.NON_STRING, ColumnHeader.OPTIONAL_VALUE),
        numericData2(4, ColumnHeader.NON_DATE, ColumnHeader.NON_STRING, ColumnHeader.OPTIONAL_VALUE),
        calculated(5, ColumnHeader.NON_DATE, ColumnHeader.IS_STRING, ColumnHeader.OPTIONAL_VALUE),
        expected(6, ColumnHeader.NON_DATE, ColumnHeader.IS_STRING, ColumnHeader.OPTIONAL_VALUE),
        aBoolean(7, ColumnHeader.NON_DATE, ColumnHeader.NON_STRING, ColumnHeader.OPTIONAL_VALUE),
        aDate(8, ColumnHeader.IS_DATE, ColumnHeader.NON_STRING, ColumnHeader.OPTIONAL_VALUE);
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
            this(IgnoreTrailingBlankLines.NO);
        }

        private TestProcessor(IgnoreTrailingBlankLines ignoreTrailingBlankLines) {
            super(null, ignoreTrailingBlankLines);
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

    public void representsBlankLine() {
        List<String> line = new ArrayList<>();
        assertThat(PoiSpreadsheetParser.representsBlankLine(line), is(true));

        line.add("");
        assertThat(PoiSpreadsheetParser.representsBlankLine(line), is(true));
        line.add("  ");
        assertThat(PoiSpreadsheetParser.representsBlankLine(line), is(true));
        line.add("\t");
        assertThat(PoiSpreadsheetParser.representsBlankLine(line), is(true));

        line.add("  \tx");
        assertThat(PoiSpreadsheetParser.representsBlankLine(line), is(false));
    }

    public void findNonTrailingBlankLineIndexes() {
        assertThat(PoiSpreadsheetParser.findNonTrailingBlankLineIndexes(Arrays.asList(3, 4, 5), 5), is(empty()));

        assertThat(PoiSpreadsheetParser.findNonTrailingBlankLineIndexes(Arrays.asList(1, 2, 4, 5), 5), is(equalTo(
                Arrays.asList(1, 2))));

        assertThat(PoiSpreadsheetParser.findNonTrailingBlankLineIndexes(Collections.<Integer>emptyList(), 7),
                is(equalTo(Collections.<Integer>emptyList())));
    }

    public void trailingBlankLines() throws IOException, InvalidFormatException, ValidationException {
        String testData = TestUtils.getTestData(POI_TEST_TRAILING_BLANK_LINES);
        List<String> messages;

        messages = PoiSpreadsheetParser.processSingleWorksheet(new FileInputStream(testData),
                testProcessorIgnoreTrailingBlankLines);
        assertThat(messages, is(empty()));

        messages = PoiSpreadsheetParser.processSingleWorksheet(new FileInputStream(testData), testProcessor);
        assertThat(messages, is(not(empty())));
    }

    public void interveningBlankLines() throws IOException, InvalidFormatException, ValidationException {
        String testData = TestUtils.getTestData(POI_TEST_INTERVENING_BLANK_LINES);
        List<String> messages;

        messages = PoiSpreadsheetParser.processSingleWorksheet(new FileInputStream(testData),
                testProcessorIgnoreTrailingBlankLines);
        assertThat(messages, is(not(empty())));

        messages = PoiSpreadsheetParser.processSingleWorksheet(new FileInputStream(testData), testProcessor);
        assertThat(messages, is(not(empty())));
    }
}
