package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import clover.org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.presentation.sample.WalkUpSequencing;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/Walkup")
@Stateless
public class WalkupSequencingResource {
    public static final String STATUS_SUCCESS = "{\"status\": \"success\"}";

    @Inject
    private SampleInstanceEjb sampleInstanceEjb;

    @Inject
    private UserBean userBean;

    @GET
    @Path("/query")
    @Produces(MediaType.APPLICATION_JSON)
    public String getRun(@QueryParam("runName") String runName) {
        return runName;
    }

    @POST
    @Consumes({"application/json"})
    @Produces({"application/json"})
    @Path("/postSequenceData")
    public String getJson(WalkUpSequencing walkUpSequencing) {
        userBean.login("seqsystem");
        MessageCollection messageCollection = new MessageCollection();
        sampleInstanceEjb.verifyAndPersistSubmission(walkUpSequencing, messageCollection);
        return messageCollection.hasErrors() ?
                "{\"status\":" + "\"" + StringUtils.join(messageCollection.getErrors(), ",") + "\"}" :
                STATUS_SUCCESS;
    }

}
