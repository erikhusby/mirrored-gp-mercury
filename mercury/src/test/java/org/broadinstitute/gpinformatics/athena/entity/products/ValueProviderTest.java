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

package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleData;
import org.testng.annotations.Test;

import java.util.Collections;

@Test(groups = TestGroups.DATABASE_FREE)
public class ValueProviderTest {

    @Test(expectedExceptions = NullPointerException.class)
    public void testGetValueWithMercurySampleDataThrowsNPE() throws Exception {
        WgaValueProviderThrowsNPE wgaValueProvider = new WgaValueProviderThrowsNPE();

        ProductOrderSample sample =
                new ProductOrderSample("foo", new MercurySampleData("SM-1234", Collections.<Metadata>emptySet()));
        wgaValueProvider.getValue(sample);
    }

    public void testWgaValueProvider_GetValueWithMercurySampleData() {
        RiskCriterion.WgaValueProvider wgaValueProvider = new RiskCriterion.WgaValueProvider();

        ProductOrderSample sample =
                new ProductOrderSample("foo", new MercurySampleData("SM-1234", Collections.<Metadata>emptySet()));
        wgaValueProvider.getValue(sample);
    }

    /**
     * This is the original implementation of a value provider which will throw a NPE.
     */
    private static class WgaValueProviderThrowsNPE extends RiskCriterion.ValueProvider {
        private static final long serialVersionUID = -4849732345451486536L;

        @Override
        public String getValue(ProductOrderSample sample) {
            return String.valueOf(sample.getSampleData().getMaterialType().contains("WGA"));
        }
    }
}
