package org.broadinstitute.sequel.boundary.vessel;

import org.broadinstitute.sequel.nonthrift.jaxb.FlowcellDesignationType;

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
@Stateless
public class NonthriftXsdEchoResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/echoBoolean")
    public boolean echoBoolean(@QueryParam("value") boolean value) {
        return value;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/echoDouble")
    public double echoDouble(@QueryParam("value") double value) {
        return value;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/echoString")
    public String echoString(@QueryParam("value") String value) {
        return value;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/echoFlowcellDesignation")
    public FlowcellDesignationType echoFlowcellDesignation(FlowcellDesignationType flowcellDesignation) {
        return flowcellDesignation;
    }
}
