package org.broadinstitute.gpinformatics.athena;

import org.testng.annotations.BeforeClass;

public class WeldBooter {

    protected WeldUtil weldUtil;

    @BeforeClass
    public void bootWeld() {
         weldUtil = TestUtilities.bootANewWeld();
     }
}
