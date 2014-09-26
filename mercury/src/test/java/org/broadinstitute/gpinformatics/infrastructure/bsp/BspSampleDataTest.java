package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.google.common.collect.ImmutableMap;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

@Test(groups = TestGroups.DATABASE_FREE)
public class BspSampleDataTest {

    public void testGetPatientIdReturnsParticipantIdFromBspSampleSearch() {
        String participantId = "PT-1";
        BspSampleData bspSampleData =
                new BspSampleData(ImmutableMap.of(BSPSampleSearchColumn.PARTICIPANT_ID, participantId));
        Assert.assertEquals(bspSampleData.getPatientId(), participantId);
    }

    // Tests for getVolume()

    public void testGetVolumeWhenNotSet() {
        SampleData bspSampleData = new BspSampleData(new HashMap<BSPSampleSearchColumn, String>());
        Assert.assertEquals(bspSampleData.getVolume(), 0.0, 0.001);
    }

    public void testGetVolumeWhenSet() {
        Map<BSPSampleSearchColumn, String> data = new HashMap<>();
        data.put(BSPSampleSearchColumn.VOLUME, "1.0");
        SampleData bspSampleData = new BspSampleData(data);
        Assert.assertEquals(bspSampleData.getVolume(), 1.0, 0.001);
    }

    // Tests for getRin()

    public void testGetRinReturnsNullWhenNotSet() {
        SampleData bspSampleData = new BspSampleData(new HashMap<BSPSampleSearchColumn, String>());
        Assert.assertNull(bspSampleData.getRin());
    }

    public void testGetRinWhenSetWithSingleValue() {
        Map<BSPSampleSearchColumn, String> data = new HashMap<>();
        data.put(BSPSampleSearchColumn.RIN, "1.0");
        SampleData bspSampleData = new BspSampleData(data);
        Assert.assertEquals(bspSampleData.getRin(), 1.0, 0.001);
    }

    @Test(expectedExceptions = NumberFormatException.class)
    public void testGetRinWhenNotANumber() {
        Map<BSPSampleSearchColumn, String> data = new HashMap<>();
        data.put(BSPSampleSearchColumn.RIN, "1,0");
        SampleData bspSampleData = new BspSampleData(data);
        bspSampleData.getRin();
    }

    public void testGetRinWhenSetWithValueRange() {
        Map<BSPSampleSearchColumn, String> data = new HashMap<>();
        data.put(BSPSampleSearchColumn.RIN, "1.2-3.4");
        SampleData bspSampleData = new BspSampleData(data);
        Assert.assertEquals(bspSampleData.getRin(), 1.2, 0.001);
    }

    // Tests for getRawRin()
    /*
     * These are a bit of overkill, but they guard against some misbehavior that have been seen with previous versions
     * of the code.
     */

    public void testGetRawRinWhenNotSet() {
        SampleData bspSampleData = new BspSampleData(new HashMap<BSPSampleSearchColumn, String>());
        Assert.assertEquals(bspSampleData.getRawRin(), "");
    }

    public void testGetRawRinWhenSingleNumber() {
        Map<BSPSampleSearchColumn, String> data = new HashMap<>();
        data.put(BSPSampleSearchColumn.RIN, "1.0");
        SampleData bspSampleData = new BspSampleData(data);
        Assert.assertEquals(bspSampleData.getRawRin(), "1.0");
    }

    public void testGetRawRinWhenNotANumber() {
        Map<BSPSampleSearchColumn, String> data = new HashMap<>();
        data.put(BSPSampleSearchColumn.RIN, "1,0");
        SampleData bspSampleData = new BspSampleData(data);
        Assert.assertEquals(bspSampleData.getRawRin(), "1,0");
    }

    public void testGetRawRinWhenSetWithValueRange() {
        Map<BSPSampleSearchColumn, String> data = new HashMap<>();
        data.put(BSPSampleSearchColumn.RIN, "1.2-3.4");
        SampleData bspSampleData = new BspSampleData(data);
        Assert.assertEquals(bspSampleData.getRawRin(), "1.2-3.4");
    }

    // Tests for getRqs()

    public void testGetRqsReturnsNullWhenNotSet() {
        SampleData bspSampleData = new BspSampleData(new HashMap<BSPSampleSearchColumn, String>());
        Assert.assertNull(bspSampleData.getRqs());
    }

    public void testGetRqsWhenSetWithNumber() {
        Map<BSPSampleSearchColumn, String> data = new HashMap<>();
        data.put(BSPSampleSearchColumn.RQS, "1.0");
        SampleData bspSampleData = new BspSampleData(data);
        Assert.assertEquals(bspSampleData.getRqs(), 1.0, 0.001);
    }

    @Test(expectedExceptions = NumberFormatException.class)
    public void testGetRqsWhenNotANumber() {
        Map<BSPSampleSearchColumn, String> data = new HashMap<>();
        data.put(BSPSampleSearchColumn.RQS, "1,0");
        SampleData bspSampleData = new BspSampleData(data);
        bspSampleData.getRqs();
    }
}
