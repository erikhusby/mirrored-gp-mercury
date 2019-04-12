package org.broadinstitute.gpinformatics.mercury.control.reagent;

import com.opencsv.bean.CsvBindByName;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.testng.Assert.*;

@Test(groups = TestGroups.DATABASE_FREE)
public class ReagentDesignImportProcessorTest {

    private static String IMPORT_CSV = "Probe Tube CSV.csv";
    private static String HEADER = "Manufacturer Design ID,Design Name,2-D Barcode,Volume in Tube (uL),Probe Mass (ng),Synthesis Date,Manufacturing Date,Storage Conditions,Lot Number,Expiration Date\n";

    private static final String DESIGN_ID = "Did";
    private static final String DESIGN_NAME = "dName";
    private static final String TUBE_BARCODE = "tubeBar";
    private static final String VOL = "20";
    private static final String MASS = "30";
    private static final String SYNTHESIS_DATE = "10/15/2018";
    private static final String MANUFACTURING_DATE = "10/16/2018";
    private static final String STORAGE = "-20C";
    private static final String LOT = "myLot";
    private static final String EXP_DATE = "10/15/2029";
    private static final String OLD_EXP_DATE = "10/15/1987";
    private static final String WEIRD_EXP_DATE = "1987/15/10";

    private static final String ERR_FORMAT = "Failed to parse input file: Field '%s' is mandatory but no value was provided.";
    private static final String REPEATED_BARCODE_ERR = "Barcode repeated in upload: " + TUBE_BARCODE;
    private static final String EXPIRED_BARCODE_ERR = TUBE_BARCODE + " is expired.";

    private ReagentDesignImportProcessor processor;
    private MessageCollection messageCollection;

    @BeforeMethod
    public void setup() {
        processor = new ReagentDesignImportProcessor();
        messageCollection = new MessageCollection();
    }

    @Test
    public void testBasic() {
        InputStream testSpreadSheet = VarioskanParserTest.getSpreadsheet(IMPORT_CSV);
        try {
            ReagentDesignImportProcessor processor = new ReagentDesignImportProcessor();
            MessageCollection messageCollection = new MessageCollection();
            List<ReagentDesignImportProcessor.ReagentImportDto> dtos = processor.parse(testSpreadSheet,
                    messageCollection);
            assertEquals(messageCollection.getErrors().size(), 0);
            assertEquals(dtos.size(), 1);
        } finally {
            try {
                testSpreadSheet.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Test
    public void testDesignIdLinkedToTwoDesignNamesFails() {
        String rowA = createRow(DESIGN_ID, DESIGN_NAME, TUBE_BARCODE, VOL,
                MASS, SYNTHESIS_DATE, MANUFACTURING_DATE, STORAGE, LOT,
                EXP_DATE);
        String rowB = createRow(DESIGN_ID, "DNAME2", "TubeBarcode2", VOL,
                MASS, SYNTHESIS_DATE, MANUFACTURING_DATE, STORAGE, LOT,
                EXP_DATE);
        runParseTest(HEADER + rowA + rowB, "Can't link Design ID DNAME2 it's already linked to dName in upload");
    }

    @Test
    public void testDuplicateBarcodeInSpreadsheet() {
        String rowA = createRow(DESIGN_ID, DESIGN_NAME, TUBE_BARCODE, VOL,
                MASS, SYNTHESIS_DATE, MANUFACTURING_DATE, STORAGE, LOT,
                EXP_DATE);
        runParseTest(HEADER + rowA + rowA, REPEATED_BARCODE_ERR);
    }

    @Test
    public void testExpiredDate() {
        String data = HEADER + createRow(DESIGN_ID, DESIGN_NAME, TUBE_BARCODE, VOL,
                MASS, SYNTHESIS_DATE, MANUFACTURING_DATE, STORAGE, LOT,
                OLD_EXP_DATE);
        runParseTest(data, EXPIRED_BARCODE_ERR);
    }

    @Test
    public void testWeirdDateFormats() {
        String data = HEADER + createRow(DESIGN_ID, DESIGN_NAME, TUBE_BARCODE, VOL,
                MASS, SYNTHESIS_DATE, MANUFACTURING_DATE, STORAGE, LOT,
                WEIRD_EXP_DATE);
        processor.parse(new ByteArrayInputStream(data.getBytes()), messageCollection);
        assertThat(messageCollection.getErrors().get(0), containsString("Unparseable date"));
    }

    /**
     * Parser dies on the first missing required field, so loop through all required fields and build a
     * csv starting with no required fields and appending in order to see if correct error messages are displayed
     * if they are null
     */
    @Test
    public void testAllNull() {
        List<String> values = Arrays.asList(DESIGN_ID, DESIGN_NAME, TUBE_BARCODE, VOL, MASS, SYNTHESIS_DATE,
                MANUFACTURING_DATE, STORAGE, LOT, EXP_DATE);

        Field[] fieldNames = ReagentDesignImportProcessor.ReagentImportDto.class.getDeclaredFields();
        List<String> requiredFields = Arrays.stream(fieldNames)
                .filter(f -> f.isAnnotationPresent(CsvBindByName.class) && f.getAnnotation(CsvBindByName.class).required())
                .map(Field::getName)
                .collect(Collectors.toList());

        for (int i = 1; i < values.size(); i++) {
            String [] rowData = new String[10];
            List<String> nonNullCols = new ArrayList<>(values.subList(0, i));
            for (int arrIdx = 0; arrIdx < nonNullCols.size(); arrIdx++) {
                rowData[arrIdx] = nonNullCols.get(arrIdx);
            }
            String data = HEADER + createRow(rowData);

            String errMsg = String.format(ERR_FORMAT, requiredFields.get(i));
            runParseTest(data, errMsg);
        }

    }

    private void runParseTest(String data, String expectedError) {
        processor.parse(new ByteArrayInputStream(data.getBytes()), messageCollection);
        assertThat(messageCollection.getErrors(), hasItem(expectedError));
    }

    private String createRow(String ... data) {
        return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                ofNullable(data[0]).orElse(""),
                ofNullable(data[1]).orElse(""),
                ofNullable(data[2]).orElse(""),
                ofNullable(data[3]).orElse(""),
                ofNullable(data[4]).orElse(""),
                ofNullable(data[5]).orElse(""),
                ofNullable(data[6]).orElse(""),
                ofNullable(data[7]).orElse(""),
                ofNullable(data[8]).orElse(""),
                ofNullable(data[9]).orElse("")
        );
    }
}