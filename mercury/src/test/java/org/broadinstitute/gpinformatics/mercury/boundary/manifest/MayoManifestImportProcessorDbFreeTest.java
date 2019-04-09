package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Test(groups = TestGroups.DATABASE_FREE)
public class MayoManifestImportProcessorDbFreeTest {
    private static final String EXCEL = "filename.xlsx";
    private static final String CSV = "filename.csv";

    public void testHeaderParsing1() throws Exception {
        for (String headers : Arrays.asList(
                "Package_Id,Biobankid_Sampleid,Box_Label,Well_Position,Sample_Id,Parent_Sample_Id,Matrix_Id," +
                        "Collection_Date,Biobank_Id,Sex_At_Birth,Age,Sample_Type,Treatments,Quantity_(ul)," +
                        "Total_Concentration_(ng/ul),Total_Dna(ng),Visit_Description,Sample_Source,Study," +
                        "Tracking_Number,Contact,Email,Requesting_Physician,Test_Name",
                // Units.
                "Package_Id,Biobankid_Sampleid,Box_Label,Well_Position,Sample_Id,Parent_Sample_Id,Matrix_Id," +
                        "Collection_Date,Biobank_Id,Sex_At_Birth,Age,Sample_Type,Treatments,Quantity_ul," +
                        "Total_Concentration ( ng / ul ) ,Total_Dna ng,Visit  Description  ,Sample_Source,Study," +
                        "Tracking_Number,Contact,Email,Requesting_Physician,Test_Name",
                // Mixed case, dropped parentheses, run-on Id.
                "PACKAGE_ID,BIOBANKID_SAMPLEID,BOX_LABEL,WELL_POSITION,SAMPLE_ID,PARENT_SAMPLE_ID,MATRIX_ID," +
                        "collection_date,biobank_id,sex_at_birth,age,sample_type,treatments,quantity_(ul)," +
                        "tOTAL_cONCENTRATION_nG/uL,Total Dna NG,vISIT_dESCRIPTION,sAMPLE_SOURCE,sTUDY," +
                        "Tracking Number,Contact,Email,Requesting Physician,Test Name",
                // only required headers
                "PackageId,Box Label,Well Position,SampleId,Matrix Id"
        )) {
            // Puts numbers in each value column that corresponds to a header.
            String content = headers + "\n" + IntStream.range(0, headers.split(",").length).
                    mapToObj(n -> String.format("%08d", n)).collect(Collectors.joining(","));
            MayoManifestImportProcessor processor = new MayoManifestImportProcessor();
            MessageCollection messageCollection = new MessageCollection();
            Multimap<String, ManifestRecord> records = processor.makeManifestRecords(
                    processor.parseAsCellGrid(content.getBytes(), CSV, messageCollection), CSV, messageCollection);
            Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors()));
            Assert.assertEquals(records.values().size(), 1);
        }
    }

    /** Parser should strip out spurious characters that are not 7-bit ASCII from the headers and values. */
    public void testHeaderParsing2() throws Exception {
        char[] invalidChars = {(char)0x9a, (char)0xa2, (char)0x07, (char)0x0a, (char)0x0b, (char)0x0c, (char)0x0d};
        String[] contentPieces = {"", "P", "ackage_Id", ",Biobankid_Samp", "leid,Box_Label,We", "ll_Position,",
                "", "Sample_Id", "", ",Parent_Sa", "mple_Id,", "Matrix_Id,Collection_Date,Biobank_Id,Sex_At_B",
                "irth,Age,Sample_Type,Treatments,Quantity_(", "ul", "),Total_Concen", "tration_(n", "g/ul),",
                "Total_Dna(ng),Visit_Description,Sample_Source,Study,", "Tracking_Number,Contact,Email,Reques",
                "ting_Physician,Test_Name\n9317", "107,", "2816424,71","33584,C12", ",6963270,6016485,6607943,01",
                "/01/2019,8109786,M,43,DNA,None,3", "42.18,103.", "87,348,First,8043153,7153373,3912376,15",
                "06172,5338480,", "", ","};
        // Joins the header and values with invalid chars that should be stripped out.
        StringBuilder builder = new StringBuilder();
        int idx = 0;
        for (String contentPiece : contentPieces) {
            builder.append(contentPiece).append(invalidChars[idx % invalidChars.length]);
        }
        MayoManifestImportProcessor processor = new MayoManifestImportProcessor();
        MessageCollection messages = new MessageCollection();
        Multimap<String, ManifestRecord> records = processor.makeManifestRecords(
                processor.parseAsCellGrid(builder.toString().getBytes(), CSV, messages), CSV, messages);
        Assert.assertFalse(messages.hasErrors(), StringUtils.join(messages.getErrors()));
        Assert.assertEquals(records.values().size(), 1);
        ManifestRecord record = records.values().iterator().next();
        Assert.assertEquals(record.getMetadataByKey(Metadata.Key.CONCENTRATION).getStringValue(), "103.87");
    }


    /** A file that is not a csv spreadsheet should be parsed as empty and not throw exceptions. */
    public void testNotAManifest() {
        byte[] content = {24, 55, 66, 71, 67, 116, 84, 8, 11, 122, 14, 65, 106, 36, 64, 30, 57, 110, 39, 70, 108,
                85, 7, 95, 2, 104, 15, 18, 21, 105, 56, 82, 28, 96, 47, 86, 107, 109, 27, 25, 22, 90, 74, 98, 9,
                113, 115, 19, 3, 10, 114, 68, 83, 20, 34, 102, 91, 23, 80, 16, 37, 44, 38, 119, 4, 5, 41, 78, 77,
                76, 87, 112, 61, 72, 124, 81, 51, 73, 59, 62, 1, 29, 99, 48, 126, 50, 88, 101, 33, 111, 40, 17,
                100, 79, 42, 6, 121, 31, 92, 127, 125, 94, 75, 69, 46, 52, 93, 49, 60, 45, 89, 63, 26, 13, 53,
                43, 35, 103, 97, 54, 118, 58, 12, 123, 32, 117, 120};

        MessageCollection messages = new MessageCollection();
        MayoManifestImportProcessor processor;
        try {
            // Can't read it as csv.
            processor = new MayoManifestImportProcessor();
            Assert.assertTrue(processor.makeManifestRecords(processor.parseAsCellGrid(content, CSV, messages),
                    CSV, messages).isEmpty());
            Assert.assertFalse(messages.hasErrors(), StringUtils.join(messages.getErrors()));
            Assert.assertEquals(messages.getWarnings().size(), 1, StringUtils.join(messages.getWarnings()));
            Assert.assertTrue(messages.getWarnings().get(0).startsWith(
                    String.format(MayoManifestImportProcessor.CANNOT_PARSE, CSV, "QQQ").split("QQQ")[0]),
                    StringUtils.join(messages.getWarnings()));

            // Can't read it as Excel either.
            processor = new MayoManifestImportProcessor();
            messages.clearAll();
            Assert.assertTrue(processor.makeManifestRecords(processor.parseAsCellGrid(content, EXCEL, messages),
                    EXCEL, messages).isEmpty());
            Assert.assertFalse(messages.hasErrors(), StringUtils.join(messages.getErrors()));
            Assert.assertEquals(messages.getWarnings().size(), 1, StringUtils.join(messages.getWarnings()));
            Assert.assertTrue(messages.getWarnings().get(0).startsWith(
                    String.format(MayoManifestImportProcessor.CANNOT_PARSE, EXCEL, "QQQ").split("QQQ")[0]),
                    StringUtils.join(messages.getWarnings()));
        } catch (Exception e) {
            Assert.fail("Should not have thrown exception");
        }
    }

    public void testErrors() {
        String headers = "Package Id,Biobankid Sampleid,Box Label,Well Position,Sample Id,Parent Sample Id," +
                "Matrix Id,Collection Date,Biobank Id,Sex At Birth,Age,Sample Type,Treatments," +
                "Quantity (ul),Total Concentration (ng/ul),Total Dna(ng),Visit Description,Sample Source," +
                "Study,Tracking Number,Contact,Email,Requesting Physician,Test Name";
        String values = "\nPK001,B001_S001,B001,A1,S001,PS001,M001,03/26/2019,B001,F,22,DNA,None,0.01,1.01,2," +
                "2nd Visit,Whole Blood,The Study Title,TRK001,theContact,email1@email.org,The Name,all";
        MessageCollection messages = new MessageCollection();
        MayoManifestImportProcessor processor;

        processor = new MayoManifestImportProcessor();
        Assert.assertTrue(processor.makeManifestRecords(processor.parseAsCellGrid(
                (headers + ",Package Id" + values + ",").getBytes(), CSV, messages), CSV, messages).isEmpty());
        Assert.assertTrue(messages.hasErrors(), StringUtils.join(messages.getErrors(), "; "));
        Assert.assertEquals(messages.getErrors().get(0), String.format(MayoManifestImportProcessor.DUPLICATE_HEADER,
                CSV, MayoManifestImportProcessor.Header.PACKAGE_ID.getText()));

        processor = new MayoManifestImportProcessor();
        messages.clearAll();
        Assert.assertTrue(processor.makeManifestRecords(processor.parseAsCellGrid(
                (headers + values.replace("PK001", "")).getBytes(), CSV, messages), CSV, messages).isEmpty());
        Assert.assertTrue(messages.hasErrors(), StringUtils.join(messages.getErrors(), "; "));
        Assert.assertEquals(messages.getErrors().get(0), String.format(MayoManifestImportProcessor.MISSING_DATA,
                CSV, MayoManifestImportProcessor.Header.PACKAGE_ID.getText()));

        processor = new MayoManifestImportProcessor();
        messages.clearAll();
        Assert.assertTrue(processor.makeManifestRecords(processor.parseAsCellGrid(
                (headers.replace("Package Id", "") + values).getBytes(), CSV, messages), CSV, messages).isEmpty());
        Assert.assertTrue(messages.hasErrors(), StringUtils.join(messages.getErrors(), "; "));
        Assert.assertEquals(messages.getErrors().get(0), String.format(MayoManifestImportProcessor.MISSING_HEADER,
                CSV, MayoManifestImportProcessor.Header.PACKAGE_ID.getText()));

        processor = new MayoManifestImportProcessor();
        messages.clearAll();
        Assert.assertTrue(processor.makeManifestRecords(processor.parseAsCellGrid(
                (headers + values.replace(",0.01,", ",NaN,")).getBytes(), CSV, messages), CSV, messages).isEmpty());
        Assert.assertTrue(messages.hasErrors(), StringUtils.join(messages.getErrors(), "; "));
        Assert.assertEquals(messages.getErrors().get(0), String.format(MayoManifestImportProcessor.NOT_NUMBER,
                CSV, MayoManifestImportProcessor.Header.VOLUME.getText(), "NaN"));

        processor = new MayoManifestImportProcessor();
        messages.clearAll();
        Assert.assertTrue(processor.makeManifestRecords(processor.parseAsCellGrid(
                (headers + values.replace(",1.01,", ",zero,")).getBytes(), CSV, messages), CSV, messages).isEmpty());
        Assert.assertTrue(messages.hasErrors(), StringUtils.join(messages.getErrors(), "; "));
        Assert.assertEquals(messages.getErrors().get(0), String.format(MayoManifestImportProcessor.NOT_NUMBER,
                CSV, MayoManifestImportProcessor.Header.CONCENTRATION.getText(), "zero"));

        processor = new MayoManifestImportProcessor();
        messages.clearAll();
        Assert.assertTrue(processor.makeManifestRecords(processor.parseAsCellGrid(
                (headers.replace("(ul)", "oz") + values).getBytes(), CSV, messages), CSV, messages).isEmpty());
        Assert.assertTrue(messages.hasErrors(), StringUtils.join(messages.getErrors(), "; "));
        Assert.assertEquals(messages.getErrors().get(0), String.format(MayoManifestImportProcessor.UNKNOWN_UNITS,
                CSV, MayoManifestImportProcessor.Header.VOLUME.getText(), "oz"));

        processor = new MayoManifestImportProcessor();
        messages.clearAll();
        Assert.assertTrue(processor.makeManifestRecords(processor.parseAsCellGrid(
                (headers.replace("(ul)", "") + values).getBytes(), CSV, messages), CSV, messages).isEmpty());
        Assert.assertTrue(messages.hasErrors(), StringUtils.join(messages.getErrors(), "; "));
        Assert.assertEquals(messages.getErrors().get(0), String.format(MayoManifestImportProcessor.UNKNOWN_UNITS, CSV,
                MayoManifestImportProcessor.Header.VOLUME.getText(), MayoManifestImportProcessor.NO_VALUE_PRESENT));

        processor = new MayoManifestImportProcessor();
        messages.clearAll();
        Assert.assertTrue(processor.makeManifestRecords(processor.parseAsCellGrid(
                (headers.replace("(ng/ul)", "") + values).getBytes(), CSV, messages), CSV, messages).isEmpty());
        Assert.assertTrue(messages.hasErrors(), StringUtils.join(messages.getErrors(), "; "));
        Assert.assertEquals(messages.getErrors().get(0), String.format(MayoManifestImportProcessor.UNKNOWN_UNITS,
                CSV, MayoManifestImportProcessor.Header.CONCENTRATION.getText(),
                MayoManifestImportProcessor.NO_VALUE_PRESENT));

        processor = new MayoManifestImportProcessor();
        messages.clearAll();
        Assert.assertTrue(processor.makeManifestRecords(processor.parseAsCellGrid(
                (headers.replace("(ng/ul)", "mmol") + values).getBytes(), CSV, messages), CSV, messages).isEmpty());
        Assert.assertTrue(messages.hasErrors(), StringUtils.join(messages.getErrors(), "; "));
        Assert.assertEquals(messages.getErrors().get(0), String.format(MayoManifestImportProcessor.UNKNOWN_UNITS,
                CSV, MayoManifestImportProcessor.Header.CONCENTRATION.getText(), "mmol"));

        // Unknown headers are only a warning.
        processor = new MayoManifestImportProcessor();
        messages.clearAll();
        Assert.assertFalse(processor.makeManifestRecords(processor.parseAsCellGrid(
                (headers.replace("Treatments", "Trick or Treats") + values).getBytes(), CSV, messages),
                CSV, messages).isEmpty());
        Assert.assertFalse(messages.hasErrors(), StringUtils.join(messages.getErrors(), "; "));
        Assert.assertTrue(messages.hasWarnings(), StringUtils.join(messages.getWarnings(), "; "));
        Assert.assertEquals(messages.getWarnings().get(0), String.format(MayoManifestImportProcessor.UNKNOWN_HEADER,
                CSV, "Trick or Treats"));
    }

    public void testUnits() throws Exception {
        String headers = "Quantity(%s),Total Concentration(%s),Total Dna(%s),Package Id,Box Label,Well Position," +
                "Sample Id,Matrix Id,Collection Date,Sample Source,Tracking Number,Requesting Physician\n";
        String data = "%s,%s,%s,P1,B1,A1,S1,T1,1/1/2020,DNA,K1,DrNo";
        for (Triple<String, String, String> triple : Arrays.asList(
                Triple.of("pL,pG/pL,pG", "1234567,2345678,3456.789", "1.23,2345678000.00,3.46"),
                Triple.of("nL,nG/pL,nG", "1234567,2345678,3456.789", "1234.57,2345678000000.00,3456.79"),
                Triple.of("nl,ng/ul,ng", "1234.567,2345678,3.456789", "1.23,2345678.00,3.46"),
                Triple.of("ul,ug/uL,ng", "1.23456,2.345678,3.45678", "1.23,2345.68,3.46"),
                Triple.of("ml,ug/mL,ug", "0.001234567,2.34567,0.00345678", "1.23,2.35,3.46"),
                Triple.of("l,mg/L,mg", "0.000001234567,2.34567,0.00000345678", "1.23,2.35,3.46"),
                Triple.of("l,mg/L,g", "0.000001234567,2.34567,0.00000345678", "1.23,2.35,3456.78")
        )) {
            MayoManifestImportProcessor processor = new MayoManifestImportProcessor();
            MessageCollection messages = new MessageCollection();
            String[] units = triple.getLeft().split(",");
            String[] dataValues = triple.getMiddle().split(",");
            String[] expectations = triple.getRight().split(",");
            String content = String.format(headers, units) + String.format(data, dataValues);
            List<List<String>> cellGrid = processor.parseAsCellGrid(content.getBytes(), CSV, messages);
            Multimap<String, ManifestRecord> records = processor.makeManifestRecords(cellGrid, CSV, messages);
            Assert.assertFalse(messages.hasErrors(), StringUtils.join(messages.getErrors()));
            ManifestRecord record = records.values().iterator().next();
            Assert.assertEquals(record.getMetadataByKey(Metadata.Key.QUANTITY).getValue(),
                    expectations[0], triple.toString());
            Assert.assertEquals(record.getMetadataByKey(Metadata.Key.CONCENTRATION).getValue(),
                    expectations[1], triple.toString());
            Assert.assertEquals(record.getMetadataByKey(Metadata.Key.MASS).getValue(),
                    expectations[2], triple.toString());
        }
    }

    public void testTwoRacks() throws Exception {
        MayoManifestImportProcessor processor = new MayoManifestImportProcessor();
        MessageCollection messageCollection = new MessageCollection();
        String content = "Package Id,Biobankid Sampleid,Box Label,Well Position,Sample Id,Parent Sample Id," +
                "Matrix Id,Collection Date,Biobank Id,Sex At Birth,Age,Sample Type,Treatments," +
                "Quantity (ul),Total Concentration (ng/ul),Total Dna(ng),Visit Description,Sample Source," +
                "Study,Tracking Number,Contact,Email,Requesting Physician,Test Name\n" +

                "PK001,B001_S001,B001,A1,S001,PS001,M001,03/26/2019,B001,F,22,DNA,No Treatments,0,1,2," +
                "2nd Visit,Whole Blood,The Study Title,TRK001,theContact,email1@email.org,The Name,all\n" +

                "PK001,B001_S002,B001,B1,S002,PS001,M002,03/26/2019,B001,F,22,DNA,No Treatments,0,1,2," +
                "2nd Visit,Whole Blood,The Study Title,TRK001,theContact,email2@email.org,The Name,all\n" +

                "PK001,B002_S003,B002,B1,S003,PS002,M003,03/26/2019,B002,M,33,DNA,No Treatments,0,1,2," +
                "2nd Visit,Whole Blood,The Study Title,TRK001,theContact,email3@email.org,Other,some\n" +

                "PK001,B002_S004,B002,C1,S004,PS002,M004,03/26/2019,B002,M,33,DNA,No Treatments,0,1,2," +
                "2nd Visit,Whole Blood,The Study Title,TRK001,theContact,email4@email.org,Other,a few\n";

        List<List<String>> cellGrid = processor.parseAsCellGrid(content.getBytes(), CSV, messageCollection);
        Multimap<String, ManifestRecord> records = processor.makeManifestRecords(cellGrid, CSV, messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors()));
        Assert.assertEquals(records.keySet().size(), 2);
        Assert.assertEquals(records.get("B001").size(), 2);
        Assert.assertEquals(records.get("B002").size(), 2);
    }

}
