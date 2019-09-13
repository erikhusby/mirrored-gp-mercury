package org.broadinstitute.gpinformatics.mercury.control.hsa;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

@Test(groups = TestGroups.DATABASE_FREE)
public class DNAUtilsTest {

    @Test
    public void testReverseComplement() {
        String dna = "AATTCCGG";
        String expected = "CCGGAATT";
        String actual = DNAUtils.reverseComplement(dna);
        Assert.assertEquals(expected, actual);
    }
}