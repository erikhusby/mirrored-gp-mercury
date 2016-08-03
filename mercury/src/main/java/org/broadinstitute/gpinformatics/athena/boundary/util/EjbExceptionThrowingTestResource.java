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

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Path;

/**
 * This class is the EJB version of ExceptionThrowingTestResource. The only difference are the annotations.
 */
@Path("/test-ejb")
@Stateful
@RequestScoped
public class EjbExceptionThrowingTestResource extends ExceptionThrowingTestResource {
}