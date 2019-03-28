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
                        "Tracking_Number,Contact,Email,Requesting_Physcian,Test_Name",
                // Mixed case, dropped parentheses, run-on Id.
                "PACKAGE_ID,BIOBANKID_SAMPLEID,BOX_LABEL,WELL_POSITION,SAMPLE_ID,PARENT_SAMPLE_ID,MATRIX_ID," +
                        "collection_date,biobank_id,sex_at_birth,age,sample_type,treatments,quantity_(ul)," +
                        "tOTAL_cONCENTRATION_nG/uL,Total Dna NG,vISIT_dESCRIPTION,sAMPLE_SOURCE,sTUDY," +
                        "Tracking Number,Contact,Email,Requesting Physcian,Test Name",
                // only required headers
                "PackageId,Box Label,Well Position,SampleId,Matrix Id"
        )) {
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

    public void testUnits() throws Exception {
        String headers = "Quantity(%s),Total Concentration(%s),Total Dna(%s),Package Id,Box Label,Well Position," +
                "Sample Id,Matrix Id,Collection Date,Sample Source,Tracking Number,Requesting Physcian\n";
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
                "Study,Tracking Number,Contact,Email,Requesting Physcian,Test Name\n" +

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
