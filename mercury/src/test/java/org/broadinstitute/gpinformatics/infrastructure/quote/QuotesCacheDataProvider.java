/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2020 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.quote;

import org.testng.annotations.DataProvider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class QuotesCacheDataProvider {

    @DataProvider(name = "quotesForCacheProvider")
    static Iterator<Object[]> quotesForCacheProvider() {
        List<Object[]> testCases = new ArrayList<>();
        QuoteService.DEV_QUOTES.forEach(quoteId -> testCases.add(new Object[]{quoteId, true}));
        Stream.of("1234", "GP87UU", "GGP87U", "gp87u").forEach(quoteId -> testCases.add(new Object[]{quoteId, false}));
        return testCases.iterator();
    }
}
