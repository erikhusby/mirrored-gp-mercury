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

package org.broadinstitute.gpinformatics.infrastructure.common;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collection;

@Test(groups = TestGroups.DATABASE_FREE)
public class TokenInputTest {
    public void testExtractSearchTermsWithInput() throws Exception {
        String queryString = "foo bar baz foo-bar";
        Collection extractSearchTerms = getTestTokenInput().extractSearchTerms(queryString);

        Assert.assertEquals(extractSearchTerms.size(), 4);
        Assert.assertTrue(extractSearchTerms.contains("foo"));
        Assert.assertTrue(extractSearchTerms.contains("bar"));
        Assert.assertTrue(extractSearchTerms.contains("baz"));
        Assert.assertTrue(extractSearchTerms.contains("foo-bar"));
    }

    public void testExtractSearchTermsNullInput() throws Exception {
        Collection extractSearchTerms = getTestTokenInput().extractSearchTerms(null);
        Assert.assertTrue(extractSearchTerms.isEmpty());
    }

    public void testExtractSearchTermsEmptyInput() throws Exception {
            Collection extractSearchTerms = getTestTokenInput().extractSearchTerms("");
            Assert.assertTrue(extractSearchTerms.isEmpty());
        }

    private TokenInput getTestTokenInput(){
        return new TokenInput(true) {
            @Override
            protected String getTokenId(Object o) {
                return null;
            }

            @Override
            protected String getTokenName(Object o) {
                return null;
            }

            @Override
            protected String formatMessage(String messageString, Object o) {
                return null;
            }

            @Override
            protected Object getById(String key) {
                return null;
            }
        };

    }
}
