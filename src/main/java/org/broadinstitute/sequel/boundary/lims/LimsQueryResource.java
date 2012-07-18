package org.broadinstitute.sequel.boundary.lims;

import org.broadinstitute.sequel.nonthrift.jaxb.BooleanMapType;
import org.broadinstitute.sequel.nonthrift.jaxb.Response;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * @author breilly
 */
@Path("/limsQuery")
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public class LimsQueryResource {

    @GET
    @Path("/findFlowcellDesignationByTaskName")
    public Response findFlowcellDesignationByTaskName(@QueryParam("taskName") String taskName) {
        Response response = new Response();
        BooleanMapType booleanMap = new BooleanMapType();
        response.setBooleanMap(booleanMap);
        return response;
    }
}
