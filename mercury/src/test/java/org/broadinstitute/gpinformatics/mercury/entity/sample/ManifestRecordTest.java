package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = TestGroups.DATABASE_FREE)
public class ManifestRecordTest {

    private ManifestRecord testRecord;


    @BeforeMethod
    public void setUp() throws Exception {

        testRecord = new ManifestRecord();

    }


    public void testCreateRecord() throws Exception {


    }
}
