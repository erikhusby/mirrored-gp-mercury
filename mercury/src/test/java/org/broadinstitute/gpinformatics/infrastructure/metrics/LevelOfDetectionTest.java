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

package org.broadinstitute.gpinformatics.infrastructure.metrics;

import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.LevelOfDetection;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@Test(groups = TestGroups.DATABASE_FREE)
public class LevelOfDetectionTest {

    private static final double SMALLER_DOUBLE = 45.3171;
    private static final double LARGER_DOUBLE = 55.3455;
    private static final String PROJECT = "project";
    private static final String SAMPLE = "sample";
    private static final Integer VERSION = 1;

    public void testConstructOK() throws Exception {
        LevelOfDetection lod =new LevelOfDetection(SMALLER_DOUBLE, LARGER_DOUBLE);
        assertThat(lod, is(notNullValue()));
    }

    public void testEquals() throws Exception {
        LevelOfDetection aLOD =new LevelOfDetection(SMALLER_DOUBLE, LARGER_DOUBLE);
        LevelOfDetection bLOD =new LevelOfDetection(SMALLER_DOUBLE, LARGER_DOUBLE);
        assertThat(aLOD, equalTo(bLOD));
    }

    public void testMinimalConstructor() throws Exception {
        LevelOfDetection aLOD =new LevelOfDetection(SMALLER_DOUBLE, LARGER_DOUBLE);
        LevelOfDetection bLOD =new LevelOfDetection(SMALLER_DOUBLE, LARGER_DOUBLE);
        assertThat(bLOD, equalTo(aLOD));
    }

    public void testTransitiveEquals() throws Exception {
        LevelOfDetection aLOD =new LevelOfDetection(SMALLER_DOUBLE, LARGER_DOUBLE);
        LevelOfDetection bLOD =new LevelOfDetection(SMALLER_DOUBLE, LARGER_DOUBLE);
        assertThat(bLOD, equalTo(aLOD));
    }

    public void testNotEqual() throws Exception {
        LevelOfDetection aLOD =new LevelOfDetection(SMALLER_DOUBLE, SMALLER_DOUBLE);
        LevelOfDetection bLOD =new LevelOfDetection(LARGER_DOUBLE, LARGER_DOUBLE);
        assertThat(aLOD, not(equalTo(bLOD)));
    }

    public void testEqualsThisNull() throws Exception {
        LevelOfDetection lodThis=null;
        LevelOfDetection lodThat=new LevelOfDetection(SMALLER_DOUBLE, LARGER_DOUBLE);

        assertThat(lodThis, not(equalTo(lodThat)));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testInitializationExceptionMinimalConstructor() throws Exception {
        LevelOfDetection iWillNeverAmountToAnything = new LevelOfDetection(LARGER_DOUBLE, SMALLER_DOUBLE);
        Assert.fail("I should have thrown an IllegalStateException just now.");
    }

    @SuppressWarnings("unused")
    @Test(expectedExceptions = IllegalStateException.class)
    public void testInitializationException() throws Exception {
        LevelOfDetection iWillNeverAmountToAnything = new LevelOfDetection(LARGER_DOUBLE, SMALLER_DOUBLE);
        Assert.fail("I should have thrown an IllegalStateException just now.");
    }
}
