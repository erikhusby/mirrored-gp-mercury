package org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test the mapper.
 */
public class DesignationToFctMapperTest {
    @Test(enabled = false)
    public void testX() {
        DesignationToFctMapper designationToFctMapper = new DesignationToFctMapper();
        String fct = designationToFctMapper.getFctForDesignation("1A_10.07.2015");
        Assert.assertEquals(fct, "FCT-25891");
    }
}