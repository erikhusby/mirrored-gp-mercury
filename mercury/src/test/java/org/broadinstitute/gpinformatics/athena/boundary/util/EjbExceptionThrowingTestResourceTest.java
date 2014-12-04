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

package org.broadinstitute.gpinformatics.athena.boundary.util;

/**
 * This class runs all the same tests as ExceptionThrowingTestResourceTest but calls the EJB version of
 * ExceptionThrowingTestResource. I know.. I don't like doing code reuse like this, but it seemed the simplest way.
 * <p/>
 * The results of the tests should be ths same whether or not you are hitting an EJB.  These tests ensure the
 * EJBExceptionMapper forwards to other Mappers when the Resource is an EJB.
 */
public class EjbExceptionThrowingTestResourceTest extends ExceptionThrowingTestResourceTest {
    @Override
    protected String getResourcePath() {
        return "test-ejb";
    }
}
