package org.broadinstitute.sequel;

import static org.broadinstitute.sequel.TestGroups.BOOT_WELD;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class WeldBooter {

    protected WeldUtil weldUtil;

    @BeforeClass
    public void bootWeld() {
         weldUtil = TestUtilities.bootANewWeld();
     }
}
