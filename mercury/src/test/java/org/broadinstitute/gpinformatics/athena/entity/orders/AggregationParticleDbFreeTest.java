/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2019 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Test(groups = TestGroups.DATABASE_FREE)
public class AggregationParticleDbFreeTest {

    public static final String PDO_12345 = "PDO-12345";
    public static final String SM_1234 = "SM-1234";

    public void testFindAgpWithPdo(){
        assertThat(Product.AggregationParticle.PDO.build(SM_1234, PDO_12345), equalTo(PDO_12345));
    }

    public void testFindAgpWithPdoAliquot(){
        assertThat(Product.AggregationParticle.PDO_ALIQUOT.build(SM_1234, PDO_12345), equalTo(PDO_12345 + "." + SM_1234));
    }

    public void testFindAgpNullSample(){
        assertThat(Product.AggregationParticle.PDO_ALIQUOT.build(null,PDO_12345), equalTo(null));
    }

    public void testFindAgpNullOrder(){
        assertThat(Product.AggregationParticle.PDO_ALIQUOT.build(SM_1234,null), equalTo(null));
    }

    public void testFindAgpNullSampleAndOrder(){
        assertThat(Product.AggregationParticle.PDO_ALIQUOT.build(null,null), equalTo(null));
    }
}
