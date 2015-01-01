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
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Iterator;
import java.util.List;

@Test(groups = TestGroups.DATABASE_FREE)
public class BassResultsParserTest {

    public void testNoResults() {
        List<BassDTO> resultList = BassResultsParser.parse("");
        Assert.assertTrue(resultList.isEmpty());
    }

    public void testGetAttributeSingleFileType() {
        String bassResponse = "##FILE_TYPE=bam##\nid\tversion\nBA1234\t1\nBA2345\t12";
        List<BassDTO> resultList = BassResultsParser.parse(bassResponse);
        Iterator<BassDTO> resultIterator = resultList.iterator();
        Assert.assertEquals(resultIterator.next().getId(), "BA1234");
        Assert.assertEquals(resultIterator.next().getId(), "BA2345");
    }

    public void testGetAttributeMultiFileTypes() {
        String bassResponse =
                "##FILE_TYPE=bam##\nid\tversion\nBA1234\t1\n##FILE_TYPE=read_group_bam##\nid\tversion\nBA2345\t12";
        List<BassDTO> resultList = BassResultsParser.parse(bassResponse);
        Iterator<BassDTO> resultIterator = resultList.iterator();
        Assert.assertEquals(resultIterator.next().getId(), "BA1234");
        Assert.assertEquals(resultIterator.next().getId(), "BA2345");
    }

    public void testUnknownColumnDoesNotBlowUp() {
        String bassResponse =
                "##FILE_TYPE=bam##\nid\tnew_column_i_just_invented\nA\tB\n";
        List<BassDTO> resultList = BassResultsParser.parse(bassResponse);
        Iterator<BassDTO> resultIterator = resultList.iterator();
        Assert.assertEquals(resultIterator.next().getId(), "A");

    }
}
