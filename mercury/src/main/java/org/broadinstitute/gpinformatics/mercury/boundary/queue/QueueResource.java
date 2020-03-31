package org.broadinstitute.gpinformatics.mercury.boundary.queue;

import org.broadinstitute.bsp.client.response.DequeueContents;
import org.broadinstitute.bsp.client.response.EnqueueContents;
import org.broadinstitute.bsp.client.response.EnqueueResponse;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueOrigin;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueSpecialization;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Web service utilized for automatically queueing items into a mercury queue.
 */
@Stateful
@RequestScoped
@Path("/queue")
public class QueueResource {

    @Inject
    private QueueEjb queueEjb;

    @Inject
    private LabVesselDao labVesselDao;

    /**
     * Adds items to the specified queue.
     *
     * @param enqueueContents   Contains the information necessary to enqueue lab vessels
     * @return                  Response back to the calling application
     */
    @POST
    @Path("/enqueue")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    public Response enqueueVessels(EnqueueContents enqueueContents) {

        MessageCollection messageCollection = new MessageCollection();

        QueueType queueType = QueueType.valueOf(QueueType.class, enqueueContents.getQueueType().name());
        Set<LabVessel> labVessels = new HashSet<>(labVesselDao.findByBarcodes(enqueueContents.getTubeBarcodes()).values());
        labVessels.addAll(labVesselDao.findBySampleKeyOrLabVesselLabel(enqueueContents.getTubeBarcodes()));
        labVessels.removeAll(Collections.singletonList(null));
        QueueOrigin queueOrigin = QueueOrigin.RECEIVING;
        if (enqueueContents.getReadableName().startsWith("Ext")) { // todo jmt add type to DTO?
            queueOrigin = QueueOrigin.EXTRACTION;
        }
        QueueSpecialization queueSpecialization = null;
        if (enqueueContents.getQueueSpecialization() != null) {
            queueSpecialization = QueueSpecialization.valueOf(enqueueContents.getQueueSpecialization());
        }
        queueEjb.enqueueLabVessels(labVessels, queueType, enqueueContents.getReadableName(),
                messageCollection, queueOrigin, queueSpecialization);

        // Decided not to pass back the QueueGroupingId to BSP. So passing null instead.
        EnqueueResponse enqueueResponse = new EnqueueResponse(null, messageCollection);
        return Response.status(Response.Status.OK).entity(enqueueResponse).type(MediaType.APPLICATION_XML).build();
        // todo jmt enqueueContents.getExistingQueueGroupingId() not used?
    }

    /**
     * Removes lab vessels from a queue.
     *
     * @param dequeueContents   Contains the information necessary to dequeue lab vessels
     * @return                  Response back to the calling application
     */
    @POST
    @Path("/dequeue")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    public Response dequeueVessels(DequeueContents dequeueContents) {

        MessageCollection messageCollection = new MessageCollection();

        QueueType queueType = QueueType.valueOf(QueueType.class, dequeueContents.getQueueType().name());
        Collection<LabVessel> labVessels = labVesselDao.findByBarcodes(dequeueContents.getTubeBarcodes()).values();
        queueEjb.dequeueLabVessels(labVessels, queueType, messageCollection, dequeueContents.getDequeueingOptions());

        return Response.status(Response.Status.OK).entity(messageCollection).type(MediaType.APPLICATION_XML).build();
    }
}
