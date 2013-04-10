package org.broadinstitute.gpinformatics.mercury.entity.zims;

import edu.mit.broad.prodinfo.thrift.lims.TZReadType;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRead;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class ZimsIlluminaRunTest {

    /**
     * Verifies that the 0/null conversion for
     * {@link TZamboniRead} works.  Regression
     * test for GPLIM-1199
     */
    @Test(groups = DATABASE_FREE)
    public void test_add_reads() {
        ZimsIlluminaRun run = new ZimsIlluminaRun();
        TZamboniRead zamboniRead = new TZamboniRead((short)0,(short)1, TZReadType.INDEX);
        run.addRead(zamboniRead);

        ZamboniRead read = run.getReads().iterator().next();

        assertThat(read.getFirstCycle(), equalTo(null));
        assertThat(read.getLength(),equalTo(1));
    }
}
