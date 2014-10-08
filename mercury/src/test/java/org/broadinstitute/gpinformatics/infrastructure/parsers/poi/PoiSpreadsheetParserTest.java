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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyCollectionOf;

@Test(groups = TestGroups.DATABASE_FREE)
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

    private static final boolean IS_DATE = true;
    private static final boolean NON_DATE = false;

    enum TestHeaders implements ColumnHeader {
        testname(NON_DATE, ColumnHeader.IS_STRING, ColumnHeader.REQUIRED_VALUE),
        stringData1(NON_DATE, ColumnHeader.IS_STRING),
        stringData2(NON_DATE, ColumnHeader.IS_STRING),
        numericData1(NON_DATE, ColumnHeader.NON_STRING),
        numericData2(NON_DATE, ColumnHeader.NON_STRING),
        calculated(NON_DATE, ColumnHeader.IS_STRING),
        expected(NON_DATE, ColumnHeader.IS_STRING),
        aBoolean(NON_DATE, ColumnHeader.NON_STRING),
        aDate(IS_DATE, ColumnHeader.NON_STRING);
        private final boolean isDateColumn;
        private final boolean isStringColumn;
        private final boolean requiredValue;
        private final String text;

        TestHeaders(boolean isDateColumn, boolean isStringColumn) {
            this(isDateColumn, isStringColumn, ColumnHeader.OPTIONAL_VALUE);
        }

        TestHeaders(boolean isDateColumn, boolean isStringColumn, boolean requiredValue) {
            this.isDateColumn = isDateColumn;
            this.isStringColumn = isStringColumn;
            this.requiredValue = requiredValue;
            text = name();
        }

        @Override
        public String getText() {
            return text;
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

    private static class TestProcessor extends TableProcessor {
        private final List<Map<String, String>> spreadsheetValues = new ArrayList<>();

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
