package org.broadinstitute.sequel.boundary.vessel;

import org.broadinstitute.sequel.nonthrift.jaxb.Response;

import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * @author breilly
 */
@Path("/nonthrift")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Stateless
public class NonthriftXsdEchoResource {

    @GET
    @Path("/echoBoolean")
    public Response echoBoolean(@QueryParam("value") boolean value) {
        Response response = new Response();
        response.setBooleanValue(value);
        return response;
    }

    @GET
    @Path("/echoDouble")
    public Response echoDouble(@QueryParam("value") double value) {
        Response response = new Response();
        response.setDoubleValue(value);
        return response;
    }

    @GET
    @Path("/echoString")
    public Response echoString(@QueryParam("value") String value) {
        Response response = new Response();
        response.setStringValue(value);
        return response;
    }

    @POST
    @Path("/echoFlowcellDesignation")
    public Response echoFlowcellDesignation(Response flowcellDesignationResponse) {
        Response response = new Response();
        response.setFlowcellDesignation(flowcellDesignationResponse.getFlowcellDesignation());
        return response;
    }
}
