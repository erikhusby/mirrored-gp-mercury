package org.broadinstitute.sequel.boundary.labevent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.sequel.bettalims.jaxb.BettaLIMSMessage;
import org.broadinstitute.sequel.control.dao.labevent.LabEventDao;
import org.broadinstitute.sequel.control.labevent.LabEventFactory;
import org.broadinstitute.sequel.control.labevent.LabEventHandler;
import org.broadinstitute.sequel.entity.labevent.LabEvent;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

/**
 * Allows BettaLIMS messages to be submitted through JAX-RS
 */
@Path("/bettalimsmessage")
@Stateless
public class BettalimsMessageResource {

    private static final Log LOG = LogFactory.getLog(BettalimsMessageResource.class);

    @Context
    private UriInfo uriInfo;

    @Inject
    private LabEventFactory labEventFactory;

    @Inject
    private LabEventHandler labEventHandler;

    @Inject
    private LabEventDao labEventDao;

    public static class BettaLIMSException extends WebApplicationException {
        public BettaLIMSException(String message, int status) {
            super(Response.status(status).entity(message).type(MediaType.TEXT_PLAIN_TYPE).build());
        }
    }

    /**
     * Accepts a message from (typically) a liquid handling deck
     * @param message the text of the message
     */
    @POST
    @Consumes({MediaType.APPLICATION_XML})
    public Response processMessage(BettaLIMSMessage message) {
        try {
            List<LabEvent> labEvents = labEventFactory.buildFromBettaLims(message);
            for (LabEvent labEvent : labEvents) {
                labEventHandler.processEvent(labEvent, null);
                labEventDao.persist(labEvent);
            }
        } catch (Exception e) {
            LOG.error("Failed to process run", e);
/*
todo jmt fix this
            if(e.getMessage().contains(LabWorkflowBatchException.MESSAGE)) {
                throw new BettaLIMSException(e.getMessage(), 201);
            } else {
*/
                throw new BettaLIMSException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
/*
            }
*/
        }
        // The VWorks client seems to prefer 200 to 204
        return Response.status(Response.Status.OK).entity("Message persisted").type(MediaType.TEXT_PLAIN_TYPE).build();
    }
}