/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.bass;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

@Test(groups = TestGroups.DATABASE_FREE)
public class BassDTOTest {

//    // Tests for getVolume()
//
//    public void testGetVolumeWhenNotSet() {
//        BassDTO bassDTO = new BassDTO();
//        Assert.assertEquals(bassDTO.getVolume(), 0.0, 0.001);
//    }
//
//    public void testId(){
//        Map<BassDTO.BassResultColumns, String> data = new HashMap<BassDTO.BassResultColumns, String>(){
//            { put(BassDTO.BassResultColumns.id, "ABCDE"); } };
//        BassDTO bassDTO =new BassDTO(data);
//        Assert.assertEquals(bassDTO.getId(), "ABCDE");
//    }
//
//    public void testId(){
//           Map<BassDTO.BassResultColumns, String> data = new HashMap<BassDTO.BassResultColumns, String>(){
//               { put(BassDTO.BassResultColumns.id, "ABCDE"); } };
//           BassDTO bassDTO =new BassDTO(data);
//           Assert.assertEquals(bassDTO.getId(), "ABCDE");
//       }
//    public void testGetVolumeWhenSet() {
//        Map<BSPSampleSearchColumn, String> data = new HashMap<>();
//        data.put(BSPSampleSearchColumn.VOLUME, "1.0");
//        BassDTO bassDTO = new BassDTO(data);
//        Assert.assertEquals(bassDTO.getVolume(), 1.0, 0.001);
//    }
//
//    // Tests for getRin()
//
//    public void testGetRinReturnsNullWhenNotSet() {
//        BassDTO bassDTO = new BassDTO(new HashMap<BSPSampleSearchColumn, String>());
//        Assert.assertNull(bassDTO.getRin());
//    }
//
//    public void testGetRinWhenSetWithSingleValue() {
//        Map<BSPSampleSearchColumn, String> data = new HashMap<>();
//        data.put(BSPSampleSearchColumn.RIN, "1.0");
//        BassDTO bassDTO = new BassDTO(data);
//        Assert.assertEquals(bassDTO.getRin(), 1.0, 0.001);
//    }
//
//    @Test(expectedExceptions = NumberFormatException.class)
//    public void testGetRinWhenNotANumber() {
//        Map<BSPSampleSearchColumn, String> data = new HashMap<>();
//        data.put(BSPSampleSearchColumn.RIN, "1,0");
//        BassDTO bassDTO = new BassDTO(data);
//        bassDTO.getRin();
//    }
//
//    public void testGetRinWhenSetWithValueRange() {
//        Map<BSPSampleSearchColumn, String> data = new HashMap<>();
//        data.put(BSPSampleSearchColumn.RIN, "1.2-3.4");
//        BassDTO bassDTO = new BassDTO(data);
//        Assert.assertEquals(bassDTO.getRin(), 1.2, 0.001);
//    }
//
//    // Tests for getRawRin()
//    /*
//     * These are a bit of overkill, but they guard against some misbehavior that have been seen with previous versions
//     * of the code.
//     */
//
//    public void testGetRawRinWhenNotSet() {
//        BassDTO bassDTO = new BassDTO(new HashMap<BSPSampleSearchColumn, String>());
//        Assert.assertEquals(bassDTO.getRawRin(), "");
//    }
//
//    public void testGetRawRinWhenSingleNumber() {
//        Map<BSPSampleSearchColumn, String> data = new HashMap<>();
//        data.put(BSPSampleSearchColumn.RIN, "1.0");
//        BassDTO bassDTO = new BassDTO(data);
//        Assert.assertEquals(bassDTO.getRawRin(), "1.0");
//    }
//
//    public void testGetRawRinWhenNotANumber() {
//        Map<BSPSampleSearchColumn, String> data = new HashMap<>();
//        data.put(BSPSampleSearchColumn.RIN, "1,0");
//        BassDTO bassDTO = new BassDTO(data);
//        Assert.assertEquals(bassDTO.getRawRin(), "1,0");
//    }
//
//    public void testGetRawRinWhenSetWithValueRange() {
//        Map<BSPSampleSearchColumn, String> data = new HashMap<>();
//        data.put(BSPSampleSearchColumn.RIN, "1.2-3.4");
//        BassDTO bassDTO = new BassDTO(data);
//        Assert.assertEquals(bassDTO.getRawRin(), "1.2-3.4");
//    }
//
//    // Tests for getRqs()
//
//    public void testGetRqsReturnsNullWhenNotSet() {
//        BassDTO bassDTO = new BassDTO(new HashMap<BSPSampleSearchColumn, String>());
//        Assert.assertNull(bassDTO.getRqs());
//    }
//
//    public void testGetRqsWhenSetWithNumber() {
//        Map<BSPSampleSearchColumn, String> data = new HashMap<>();
//        data.put(BSPSampleSearchColumn.RQS, "1.0");
//        BassDTO bassDTO = new BassDTO(data);
//        Assert.assertEquals(bassDTO.getRqs(), 1.0, 0.001);
//    }
//
//    @Test(expectedExceptions = NumberFormatException.class)
//    public void testGetRqsWhenNotANumber() {
//        Map<BSPSampleSearchColumn, String> data = new HashMap<>();
//        data.put(BSPSampleSearchColumn.RQS, "1,0");
//        BassDTO bassDTO = new BassDTO(data);
//        bassDTO.getRqs();
//    }
}
