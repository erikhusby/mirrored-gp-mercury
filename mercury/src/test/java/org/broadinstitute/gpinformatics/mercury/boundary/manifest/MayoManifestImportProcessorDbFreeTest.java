package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.apache.commons.lang.WordUtils;
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
import java.util.stream.Stream;

@Test(groups = TestGroups.DATABASE_FREE)
public class MayoManifestImportProcessorDbFreeTest {
    private static final String CSV = "filename.csv";

    public void testHeaderParsing1() {
        for (String headers : Arrays.asList(
                "Package_Id,BiobankId_SampleId,Package_StorageunitId,Box_Id/Plate_Id,Well_Position," +
                        "Sample_Id,Parent_Sample_Id,Matrix_Id,Collection_Date,Biobank_Id,Sex_At_Birth," +
                        "Age,NY_State_(Y/N),Sample_Type,Treatments,Quantity_(ul),Total_Concentration_(ng/ul)," +
                        "Total_Dna(ng),Visit_Description,Sample_Source," +
                        "Study,Tracking_Number,Contact,Email,Requesting_Physician,Test_Name," +
                        "Failure Mode,Failure Mode Desc",
                // With run-on Id, no parens.
                "PackageId,BiobankId SampleId,Package StorageunitId,BoxId,Well Position,SampleId,Parent SampleId," +
                        "MatrixId,Collection Date,BiobankId,Sex At Birth,Age,NY State Y/N,Sample Type,Treatments," +
                        "Quantity ul,Total Concentration ng/ul,Total Dna ng,Visit Description,Sample Source," +
                        "Study,Tracking Number,Contact,Email,Requesting Physician,Test Name",
                // Units.
                "Package_Id,Biobankid_Sampleid,Package StorageunitId,Box_id,Well_Position,Sample_Id,Parent_Sample_Id," +
                        "MatrixId,Collection_Date,Biobank_Id,Sex_At_Birth,Age,Sample_Type,Treatments,Quantity_ul," +
                        "Total_Concentration ( ng / ul ) ,Total_Dna ng,Visit  Description  ,Sample_Source,Study," +
                        "Tracking_Number,Contact,Email,Requesting_Physician,Test_Name",
                // Mixed case, dropped parentheses.
                "PACKAGE_ID,BIOBANKID_SAMPLEID,PACKAGE STORAGEUNITID,BOXID,WELL_POSITION,SAMPLE_ID,PARENT_SAMPLE_ID," +
                        "MATRIX_ID,collection_date,biobank_id,sex_at_birth,age,sample_type,treatments,quantity_(ul)," +
                        "tOTAL_cONCENTRATION_nG/uL,Total Dna NG,vISIT_dESCRIPTION,sAMPLE_SOURCE,sTUDY," +
                        "Tracking Number,Contact,Email,Requesting Physician,Test Name"
        )) {
            parseAndReturnErrorsAndWarnings(headers, true);
        }
    }

    public void testHeaderErrorsAndWarnings() {
        final String headers = Stream.of(MayoManifestImportProcessor.Header.values()).
                map(header -> WordUtils.capitalize(header.getText(), new char[]{' '})).
                collect(Collectors.joining(",")).
                replace("Quantity", "Quantity (ul)").
                replace("Total Concentration", "Total Concentration (ng/ul)").
                replace("Total Dna", "Total Dna (ng)");

        // Tests success when all headers present.
        parseAndReturnErrorsAndWarnings(headers, true);

        // Does multiple runs, removing a different one of the required headers each time.
        // Tests failure to make manifest records and also that the missing header message is given.
        Stream.of(MayoManifestImportProcessor.Header.values()).
                filter(header -> header.isRequired()).
                forEach(dropThisHeader -> {
                    String headerString = Stream.of(MayoManifestImportProcessor.Header.values()).
                            filter(header -> header != dropThisHeader).
                            map(header -> WordUtils.capitalize(header.getText(), new char[]{' '})).
                            collect(Collectors.joining(","));
                    String message = String.format(MayoManifestImportProcessor.MISSING_HEADER, CSV,
                            dropThisHeader.getText());
                    MessageCollection messageCollection = parseAndReturnErrorsAndWarnings(headerString, false);
                    Assert.assertTrue(messageCollection.getErrors().contains(message),
                            StringUtils.join(messageCollection.getErrors(), " ; "));
                });

        // Duplicate headers.
        // Tests failure to make manifest records and also that the missing header message is given.
        String message = String.format(MayoManifestImportProcessor.DUPLICATE_HEADER, CSV,
                MayoManifestImportProcessor.Header.PACKAGE_ID.getText() + ", " +
                        MayoManifestImportProcessor.Header.SAMPLE_ID.getText());
        MessageCollection messageCollection = parseAndReturnErrorsAndWarnings(headers + ",PackageId,SampleId", false);
        Assert.assertTrue(messageCollection.getErrors().contains(message),
                "Errors: " + StringUtils.join(messageCollection.getErrors(), " ; "));

        message = String.format(MayoManifestImportProcessor.DUPLICATE_HEADER, CSV,
                MayoManifestImportProcessor.Header.STUDY.getText());
        messageCollection = parseAndReturnErrorsAndWarnings(headers + ",Study", false);
        Assert.assertTrue(messageCollection.getErrors().contains(message),
                "Errors: " + StringUtils.join(messageCollection.getErrors(), " ; "));

        // Bad units in the headers.
        // Tests failure to make manifest records and also that the missing header message is given.
        message = String.format(MayoManifestImportProcessor.UNKNOWN_UNITS, CSV, "Quantity (oz)");
        messageCollection = parseAndReturnErrorsAndWarnings(StringUtils.replace(headers, "(ul)", "(oz)"), false);
        Assert.assertTrue(messageCollection.getErrors().contains(message),
                "Errors: " + StringUtils.join(messageCollection.getErrors(), " ; "));

        message = String.format(MayoManifestImportProcessor.UNKNOWN_UNITS, CSV, "Quantity");
        messageCollection = parseAndReturnErrorsAndWarnings(StringUtils.replace(headers, "(ul)", ""), false);
        Assert.assertTrue(messageCollection.getErrors().contains(message),
                "Errors: " + StringUtils.join(messageCollection.getErrors(), " ; "));

        message = String.format(MayoManifestImportProcessor.UNKNOWN_UNITS, CSV, "Total Concentration (mmol)");
        messageCollection = parseAndReturnErrorsAndWarnings(StringUtils.replace(headers, "(ng/ul)", "(mmol)"), false);
        Assert.assertTrue(messageCollection.getErrors().contains(message),
                "Errors: " + StringUtils.join(messageCollection.getErrors(), " ; "));

        message = String.format(MayoManifestImportProcessor.UNKNOWN_UNITS, CSV, "Total Concentration (ng)");
        messageCollection = parseAndReturnErrorsAndWarnings(StringUtils.replace(headers, "(ng/ul)", "(ng)"), false);
        Assert.assertTrue(messageCollection.getErrors().contains(message),
                "Errors: " + StringUtils.join(messageCollection.getErrors(), " ; "));

        message = String.format(MayoManifestImportProcessor.UNKNOWN_UNITS, CSV, "Total Dna (ng/ul)");
        messageCollection = parseAndReturnErrorsAndWarnings(StringUtils.replace(headers, "(ng)", "(ng/ul)"), false);
        Assert.assertTrue(messageCollection.getErrors().contains(message),
                "Errors: " + StringUtils.join(messageCollection.getErrors(), " ; "));

        message = String.format(MayoManifestImportProcessor.UNKNOWN_UNITS, CSV, "Total Dna");
        messageCollection = parseAndReturnErrorsAndWarnings(StringUtils.replace(headers, "(ng)", ""), false);
        Assert.assertTrue(messageCollection.getErrors().contains(message),
                "Errors: " + StringUtils.join(messageCollection.getErrors(), " ; "));

        // Unknown headers does not give an error or warning.
        messageCollection = parseAndReturnErrorsAndWarnings(headers + "," + "SurpiseMe", true);
        Assert.assertFalse(messageCollection.hasErrors(),
                "Errors: " + StringUtils.join(messageCollection.getErrors(), " ; "));
        Assert.assertFalse(messageCollection.hasWarnings(),
                "Warnings " + StringUtils.join(messageCollection.getWarnings(), " ; "));
    }

    private MessageCollection parseAndReturnErrorsAndWarnings(String headerString, boolean expectSuccess) {
        // Puts a value in each column that corresponds to a header.
        String content = headerString + "\n" + IntStream.range(0, headerString.split(",").length).
                mapToObj(n -> String.format("%08d", n)).collect(Collectors.joining(","));
        MayoManifestImportProcessor processor = new MayoManifestImportProcessor();
        MessageCollection messageCollection = new MessageCollection();
        List<ManifestRecord> records = processor.makeManifestRecords(
                processor.parseAsCellGrid(content.getBytes(), CSV, messageCollection), CSV, messageCollection);
        Assert.assertTrue(expectSuccess && !messageCollection.hasErrors() && !messageCollection.hasWarnings() ||
                !expectSuccess && (messageCollection.hasErrors() || messageCollection.hasWarnings()),
                headerString + " has Errors " + StringUtils.join(messageCollection.getErrors(), "; ") +
                        " and Warnings " + StringUtils.join(messageCollection.getWarnings(), "; "));
        Assert.assertEquals(records.size(), expectSuccess ? 1 : 0,
                headerString + " has Errors " + StringUtils.join(messageCollection.getErrors(), "; ") +
                        " and Warnings " + StringUtils.join(messageCollection.getWarnings(), "; "));
        return messageCollection;
    }

    /** Parser should strip out spurious characters that are not 7-bit ASCII from the headers and values. */
    public void testHeaderParsing2() {
        char[] invalidChars = {(char)0x9a, (char)0xa2, (char)0x07, (char)0x0a, (char)0x0b, (char)0x0c, (char)0x0d};
        String[] contentPieces = {"", "P", "ackage_Id", ",Biobankid_Samp", "leid,Package Sto", "rageunitid,Box_id,We",
                "ll_Position,", "", "Sample_Id", "", ",Parent_Sa", "mple_Id,",
                "Matrix_Id,Collection_Date,Biobank_Id,Sex_At_B", "irth,Age,Sample_Type,Treatments,Quantity_(",
                "ul", "),Total_Concen", "tration_(n", "g/ul),",
                "Total_Dna(ng),Visit_Description,Sample_Source,Study,", "Tracking_Number,Contact,Email,Reques",
                "ting_Physician,Test_Name\n9317", "107,", "2816424,71","33,584,C12", ",6963270,6016485,6607943,01",
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
        List<ManifestRecord> records = processor.makeManifestRecords(
                processor.parseAsCellGrid(builder.toString().getBytes(), CSV, messages), CSV, messages);
        Assert.assertFalse(messages.hasErrors(), StringUtils.join(messages.getErrors()));
        Assert.assertEquals(records.size(), 1);
        ManifestRecord record = records.iterator().next();
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

        } catch (Exception e) {
            Assert.fail("Should not have thrown exception");
        }
    }

    public void testDataErrorsAndWarnings() {
        String headers = "Package Id,Biobankid Sampleid,Package Storageunitid,Box ID,Well Position," +
                "Sample Id,Parent Sample Id," +
                "Matrix Id,Collection Date,Biobank Id,Sex At Birth,Age,Sample Type,Treatments," +
                "Quantity (ul),Total Concentration (ng/ul),Total Dna(ng),Visit Description,Sample Source," +
                "Study,Tracking Number,Contact,Email,Requesting Physician,Test Name";
        String values = "\nPK001,B001_S001,S-1,B001,A1,S001,PS001,M001,03/26/2019,B001,F,22,DNA,None,0.01,1.01," +
                // Tests if parser can handle a quoted Total Dna(ng) field that has a comma.
                "\"2,400\",2nd Visit,Whole Blood,The Study Title,TRK001,theContact,email1@email.org,The Name,all";
        MessageCollection messages = new MessageCollection();
        MayoManifestImportProcessor processor;
        String expected;

        // OK as is.
        processor = new MayoManifestImportProcessor();
        messages.clearAll();
        Assert.assertEquals(processor.makeManifestRecords(processor.parseAsCellGrid(
                (headers + values).getBytes(), CSV, messages), CSV, messages).size(), 1);
        Assert.assertFalse(messages.hasErrors(), StringUtils.join(messages.getErrors(), "; "));
        Assert.assertFalse(messages.hasWarnings(), StringUtils.join(messages.getWarnings(), "; "));

        processor = new MayoManifestImportProcessor();
        messages.clearAll();
        Assert.assertTrue(processor.makeManifestRecords(processor.parseAsCellGrid(
                (headers + values.replace("PK001", "")).getBytes(), CSV, messages), CSV, messages).isEmpty());
        expected = String.format(MayoManifestImportProcessor.MISSING_DATA, CSV,
                MayoManifestImportProcessor.Header.PACKAGE_ID.getText());
        Assert.assertTrue(messages.getErrors().contains(expected), StringUtils.join(messages.getErrors()));
        Assert.assertFalse(messages.hasWarnings(), StringUtils.join(messages.getWarnings(), "; "));

        processor = new MayoManifestImportProcessor();
        messages.clearAll();
        Assert.assertTrue(processor.makeManifestRecords(processor.parseAsCellGrid(
                (headers + values.replace(",0.01,", ",NaN,")).getBytes(), CSV, messages), CSV, messages).isEmpty());
        expected = String.format(MayoManifestImportProcessor.INVALID_DATA,
                CSV, MayoManifestImportProcessor.Header.VOLUME.getText());
        Assert.assertTrue(messages.getErrors().contains(expected), StringUtils.join(messages.getErrors()));

        processor = new MayoManifestImportProcessor();
        messages.clearAll();
        Assert.assertTrue(processor.makeManifestRecords(processor.parseAsCellGrid(
                (headers + values.replace(",1.01,", ",zero,")).getBytes(), CSV, messages), CSV, messages).isEmpty());
        expected = String.format(MayoManifestImportProcessor.INVALID_DATA,
                CSV, MayoManifestImportProcessor.Header.CONCENTRATION.getText());
        Assert.assertTrue(messages.getErrors().contains(expected), StringUtils.join(messages.getErrors()));

        processor = new MayoManifestImportProcessor();
        messages.clearAll();
        Assert.assertTrue(processor.makeManifestRecords(processor.parseAsCellGrid(
                (headers + values + values.replace(",A1,", ",A2,")).getBytes(), CSV, messages), CSV, messages).isEmpty());
        Assert.assertEquals(messages.getErrors().size(), 1, StringUtils.join(messages.getErrors()));
        Assert.assertEquals(messages.getErrors().get(0),
                String.format(MayoManifestImportProcessor.DUPLICATE_TUBE, CSV, "M001"));

        processor = new MayoManifestImportProcessor();
        messages.clearAll();
        Assert.assertTrue(processor.makeManifestRecords(processor.parseAsCellGrid(
                (headers + values + values.replace(",M001,", ",M002,")).getBytes(), CSV, messages), CSV, messages).isEmpty());
        Assert.assertEquals(messages.getErrors().size(), 1, StringUtils.join(messages.getErrors()));
        Assert.assertEquals(messages.getErrors().get(0),
                String.format(MayoManifestImportProcessor.DUPLICATE_POSITION, CSV, "S-1 A1"));
}

    public void testUnits() {
        String headers = "Quantity(%s),Total Concentration(%s),Total Dna(%s),Package Id," +
                "Package Storageunitid,Well Position,BiobankId,Biobank Id Sample Id,Sample Id," +
                "Matrix Id,Collection Date,Sample Source,Tracking Number\n";
        String data = "%s,%s,%s,P1,B1,A1,S1,BS1,SS1,T1,1/1/2020,DNA,K1";
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
            List<ManifestRecord> records = processor.makeManifestRecords(cellGrid, CSV, messages);
            Assert.assertFalse(messages.hasErrors(), StringUtils.join(messages.getErrors()));
            Assert.assertFalse(messages.hasWarnings(), StringUtils.join(messages.getWarnings()));
            ManifestRecord record = records.iterator().next();
            Assert.assertEquals(record.getMetadataByKey(Metadata.Key.VOLUME).getValue(),
                    expectations[0], triple.toString());
            Assert.assertEquals(record.getMetadataByKey(Metadata.Key.CONCENTRATION).getValue(),
                    expectations[1], triple.toString());
            Assert.assertEquals(record.getMetadataByKey(Metadata.Key.MASS).getValue(),
                    expectations[2], triple.toString());
        }
    }

    public void testTwoRacks() {
        MayoManifestImportProcessor processor = new MayoManifestImportProcessor();
        MessageCollection messageCollection = new MessageCollection();
        String content = "Package Id,Biobankid Sampleid,Package Storageunitid,Box id," +
                "Well Position,Sample Id,Parent Sample Id,Matrix Id,Collection Date," +
                "Biobank Id,Sex At Birth,Age,Sample Type,Treatments,Quantity (ul)," +
                "Total Concentration (ng/ul),Total Dna(ng),Visit Description,Sample Source," +
                "Study,Tracking Number,Contact,Email,Requesting Physician,Test Name\n" +

                "PK001,B001_S001,SU001,BX001," +
                "A1,S001,PS001,M001,03/26/2019," +
                "B001,F,22,DNA,No Treatments,10," +
                "1,2,2nd Visit,Whole Blood," +
                "The Study Title,TRK001,theContact,email1@email.org,The Name,all\n" +

                "PK001,B001_S002,SU001,BX001,B1,S002,PS001,M002,03/26/2019,B001,F,22,DNA,No Treatments,10,1,2," +
                "2nd Visit,Whole Blood,The Study Title,TRK001,theContact,email2@email.org,The Name,all\n" +

                "PK001,B002_S003,SU002,BX002,B1,S003,PS002,M003,03/26/2019,B002,M,33,DNA,No Treatments,10,1,2," +
                "2nd Visit,Whole Blood,The Study Title,TRK001,theContact,email3@email.org,Other,some\n" +

                "PK001,B002_S004,SU002,BX002,C1,S004,PS002,M004,03/26/2019,B002,M,33,DNA,No Treatments,10,1,2," +
                "2nd Visit,Whole Blood,The Study Title,TRK001,theContact,email4@email.org,Other,a few\n";

        List<List<String>> cellGrid = processor.parseAsCellGrid(content.getBytes(), CSV, messageCollection);
        List<ManifestRecord> records = processor.makeManifestRecords(cellGrid, CSV, messageCollection);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors()));
        Assert.assertEquals(records.stream().
                map(manifestRecord -> manifestRecord.getMetadataByKey(Metadata.Key.RACK_LABEL).getValue()).
                filter(value -> value.equals("SU001")).count(), 2);
        Assert.assertEquals(records.stream().
                map(manifestRecord -> manifestRecord.getMetadataByKey(Metadata.Key.RACK_LABEL).getValue()).
                filter(value -> value.equals("SU002")).count(), 2);
    }

}
