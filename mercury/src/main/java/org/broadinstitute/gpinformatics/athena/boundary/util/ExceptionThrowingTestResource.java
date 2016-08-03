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

import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;

import javax.ejb.EJBException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/test")
public class ExceptionThrowingTestResource {

    public static final String RESPONSE_STATUS = "ResponseStatus";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/throwsResourceException")
    public List<String> throwsResourceException(@QueryParam(RESPONSE_STATUS) Integer statusCode) {
        Response.Status status = Response.Status.fromStatusCode(statusCode);
        throw new ResourceException("Oopsie, I threw a ResourceException", status);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/throwsInformaticsServiceException")
    public List<String> throwsInformaticsServiceException() {
        throw new InformaticsServiceException("Oopsie, I threw an InformaticsServiceException");
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/throwsEJBException")
    public List<String> throwsEJBException() {
        throw new EJBException("Oopsie, I threw an EJBException");
    }
}
