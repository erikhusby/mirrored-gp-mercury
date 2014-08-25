package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;

@Test(groups = TestGroups.DATABASE_FREE)
public class ManifestRecordTest {

    private ManifestRecord testRecord;
    private Metadata.Key key1;
    private Metadata.Key key2;
    private Metadata.Key key3;
    private String value1;
    private String value2;
    private String value3;


    @BeforeMethod
    public void setUp() throws Exception {

        key1 = Metadata.Key.SAMPLE_ID;
        value1 = "value1";
        Metadata metadata1 = new Metadata(key1, value1);
        key2 = Metadata.Key.GENDER;
        value2 = "value2";
        Metadata metadata2 = new Metadata(key2, value2);
        key3 = Metadata.Key.PATIENT_ID;
        value3 = "value3";
        Metadata metadata3 = new Metadata(key3, value3);

        testRecord = new ManifestRecord(Arrays.asList(metadata1, metadata2, metadata3));

    }


    public void testCreateRecord() throws Exception {

        Assert.assertEquals(testRecord.getField(Metadata.Key.SAMPLE_ID).getValue(), value1);

    }
}
