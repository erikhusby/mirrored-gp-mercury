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
    private Metadata.Key key1 = Metadata.Key.SAMPLE_ID;
    private Metadata.Key key2 = Metadata.Key.GENDER;
    private Metadata.Key key3 = Metadata.Key.PATIENT_ID;
    private String value1 = "value1";
    private String value2 = "value2";
    private String value3 = "value3";


    @BeforeMethod
    public void setUp() throws Exception {

        Metadata metadata1 = new Metadata(key1, value1);
        Metadata metadata2 = new Metadata(key2, value2);
        Metadata metadata3 = new Metadata(key3, value3);

        testRecord = new ManifestRecord(Arrays.asList(metadata1, metadata2, metadata3));

    }

    public void testCreateRecord() throws Exception {

        Assert.assertEquals(testRecord.getField(key1).getValue(), value1);
        Assert.assertEquals(testRecord.getField(key2).getValue(), value2);
        Assert.assertEquals(testRecord.getField(key3).getValue(), value3);
        Assert.assertEquals(testRecord.getStatus(), ManifestRecord.Status.UPLOADED);
        Assert.assertNull(testRecord.getErrorStatus());

        testRecord.setStatus(ManifestRecord.Status.ABANDONED);
        testRecord.setErrorStatus(ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID);
        Assert.assertEquals(testRecord.getStatus(), ManifestRecord.Status.ABANDONED);
        Assert.assertEquals(testRecord.getErrorStatus(), ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID);
    }
}
