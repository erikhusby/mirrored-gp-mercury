package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.util.Set;

import static org.testng.Assert.*;

@Test(groups = TestGroups.DATABASE_FREE)
public class DBSPuncherFileParserTest {
    public static final String SINGLE_WELL_FILE = "20170301_02.DAT";

    @Test
    public void testSingleWellFile() {
        InputStream inputStream = VarioskanParserTest.getTestResource(SINGLE_WELL_FILE);
        DBSPuncherFileParser parser = new DBSPuncherFileParser();
        MessageCollection messageCollection = new MessageCollection();
        DBSPuncherFileParser.DBSPuncherRun dbsPuncherRun = parser.parseRun(inputStream, messageCollection);
        Assert.assertEquals(messageCollection.hasErrors(), false);
        Assert.assertEquals(dbsPuncherRun.getMapPositionToSampleBarcode().size(), 1);
        Assert.assertEquals(dbsPuncherRun.getPlateBarcode(), "012345678912");
        Assert.assertEquals(dbsPuncherRun.getMapPositionToSampleBarcode().get(VesselPosition.A01), "SM-4ZKAL");
    }
}