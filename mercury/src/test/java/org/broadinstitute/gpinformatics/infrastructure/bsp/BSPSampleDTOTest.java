package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

@Test(groups = TestGroups.DATABASE_FREE)
public class BSPSampleDTOTest {

    public void testGetVolumeWhenNotSet() {
        BSPSampleDTO bspSampleDTO = new BSPSampleDTO(new HashMap<BSPSampleSearchColumn, String>());
        Assert.assertEquals(bspSampleDTO.getVolume(), 0.0, 0.001);
    }

    public void testGetVolumeWhenSet() {
        Map<BSPSampleSearchColumn, String> data = new HashMap<>();
        data.put(BSPSampleSearchColumn.VOLUME, "1.0");
        BSPSampleDTO bspSampleDTO = new BSPSampleDTO(data);
        Assert.assertEquals(bspSampleDTO.getVolume(), 1.0, 0.001);
    }

    public void testGetRinReturnsNullWhenNotSet() {
        BSPSampleDTO bspSampleDTO = new BSPSampleDTO(new HashMap<BSPSampleSearchColumn, String>());
        Assert.assertNull(bspSampleDTO.getRin());
    }

    public void testGetRinWhenSetWithSingleValue() {
        Map<BSPSampleSearchColumn, String> data = new HashMap<>();
        data.put(BSPSampleSearchColumn.RIN, "1.0");
        BSPSampleDTO bspSampleDTO = new BSPSampleDTO(data);
        Assert.assertEquals(bspSampleDTO.getRin(), 1.0, 0.001);
    }

    @Test(expectedExceptions = NumberFormatException.class)
    public void testGetRinWhenNotANumber() {
        Map<BSPSampleSearchColumn, String> data = new HashMap<>();
        data.put(BSPSampleSearchColumn.RIN, "1,0");
        BSPSampleDTO bspSampleDTO = new BSPSampleDTO(data);
        bspSampleDTO.getRin();
    }

    public void testGetRinWhenSetWithValueRange() {
        Map<BSPSampleSearchColumn, String> data = new HashMap<>();
        data.put(BSPSampleSearchColumn.RIN, "1.2-3.4");
        BSPSampleDTO bspSampleDTO = new BSPSampleDTO(data);
        Assert.assertEquals(bspSampleDTO.getRin(), 1.2, 0.001);
    }

    public void testGetRqsReturnsNullWhenNotSet() {
        BSPSampleDTO bspSampleDTO = new BSPSampleDTO(new HashMap<BSPSampleSearchColumn, String>());
        Assert.assertNull(bspSampleDTO.getRqs());
    }

    public void testGetRqsWhenSetWithNumber() {
        Map<BSPSampleSearchColumn, String> data = new HashMap<>();
        data.put(BSPSampleSearchColumn.RQS, "1.0");
        BSPSampleDTO bspSampleDTO = new BSPSampleDTO(data);
        Assert.assertEquals(bspSampleDTO.getRqs(), 1.0, 0.001);
    }

    @Test(expectedExceptions = NumberFormatException.class)
    public void testGetRqsWhenNotANumber() {
        Map<BSPSampleSearchColumn, String> data = new HashMap<>();
        data.put(BSPSampleSearchColumn.RQS, "1,0");
        BSPSampleDTO bspSampleDTO = new BSPSampleDTO(data);
        bspSampleDTO.getRqs();
    }
}
