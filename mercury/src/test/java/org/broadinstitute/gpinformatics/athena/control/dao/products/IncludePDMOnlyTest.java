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

package org.broadinstitute.gpinformatics.athena.control.dao.products;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao.IncludePDMOnly;

@Test(groups = TestGroups.DATABASE_FREE)
public class IncludePDMOnlyTest {
    public void testIncludePDMOnlyYes() {
        Assert.assertEquals(IncludePDMOnly.toIncludePDMOnly(true), IncludePDMOnly.YES);
    }
    public void testIncludePDMOnlyNo() {
        Assert.assertEquals(IncludePDMOnly.toIncludePDMOnly(false), IncludePDMOnly.NO);
    }
}
