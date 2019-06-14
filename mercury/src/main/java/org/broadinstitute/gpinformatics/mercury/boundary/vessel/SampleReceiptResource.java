package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.ObjectMarshaller;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.ws.WsMessageStore;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.LabVesselFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import java.text.Format;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JAX-RS web service used by BSP to notify Mercury that samples have been received.
 */
@SuppressWarnings("FeatureEnvy")
@Path("/samplereceipt")
@Stateful
@RequestScoped
public class SampleReceiptResource {

    private static final Log LOG = LogFactory.getLog(SampleReceiptResource.class);

    ObjectMarshaller<SampleReceiptBean> marshaller;

    @Inject
    private LabBatchDao labBatchDao;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private WsMessageStore wsMessageStore;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private LabVesselFactory labVesselFactory;

    public SampleReceiptResource() throws JAXBException {
        marshaller = new ObjectMarshaller<>(SampleReceiptBean.class);
    }

    @GET
    @Path("{batchName}")
    @Produces({MediaType.APPLICATION_XML})
    public SampleReceiptBean getByBatchName(@PathParam("batchName") String batchName) {
        LabBatch labBatch = labBatchDao.findByName(batchName);
        if (labBatch == null) {
            return null;
        }
        List<ParentVesselBean> parentVesselBeans = new ArrayList<>();
        Set<LabVessel> startingLabVessels = labBatch.getStartingBatchLabVessels();
        for (LabVessel startingLabVessel : startingLabVessels) {
            parentVesselBeans.add(new ParentVesselBean(
                    startingLabVessel.getLabel(),
                    startingLabVessel.getMercurySamples().iterator().next().getSampleKey(),
                    startingLabVessel.getType().getName(),
                    null));
        }
        LabVessel firstLabVessel = startingLabVessels.iterator().next();
        LabEvent labEvent = firstLabVessel.getInPlaceEventsWithContainers().iterator().next();
        BspUser bspUser = bspUserList.getById(labEvent.getEventOperator());
        return new SampleReceiptBean(
                labEvent.getEventDate(),
                labBatch.getBatchName(),
                parentVesselBeans,
                bspUser.getUsername());
    }

    /**
     * Accepts a message from BSP.  We unmarshal ourselves, rather than letting JAX-RS
     * do it, because we need to write the text to the file system.
     *
     * @param sampleReceiptBeanXml the text of the message
     */
    @POST
    @Consumes({MediaType.APPLICATION_XML})
    public String notifyOfReceipt(String sampleReceiptBeanXml) throws ResourceException {
        Date now = new Date();
        wsMessageStore.store(WsMessageStore.SAMPLE_RECEIPT_RESOURCE_TYPE, sampleReceiptBeanXml, now);
        try {
            SampleReceiptBean sampleReceiptBean =
                    marshaller.unmarshal(sampleReceiptBeanXml);

            return notifyOfReceipt(sampleReceiptBean);
        } catch (Exception e) {
            wsMessageStore.recordError(WsMessageStore.SAMPLE_RECEIPT_RESOURCE_TYPE, sampleReceiptBeanXml, now, e);
            LOG.error("Failed to process sample receipt", e);
            throw new ResourceException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * For samples received by BSP: find or create plastic; find or create MercurySamples; create receipt LabBatch.
     *
     * @param sampleReceiptBean from BSP
     *
     * @return string indicating success
     */
    public String notifyOfReceipt(SampleReceiptBean sampleReceiptBean) {
        return notifyOfReceipt(sampleReceiptBean, MercurySample.MetadataSource.BSP);
    }

    public String notifyOfReceipt(SampleReceiptBean sampleReceiptBean, MercurySample.MetadataSource metadataSource) {
        List<ParentVesselBean> parentVesselBeans = sampleReceiptBean.getParentVesselBeans();

        // todo jmt the SAMPLE_RECEIPT event seems to be used by this web service and by a BettaLIMS message.
        // Process is only interested in the primary vessels
        List<LabVessel> labVessels = labVesselFactory.buildLabVessels(parentVesselBeans,
                sampleReceiptBean.getReceivingUserName(), sampleReceiptBean.getReceiptDate(),
                LabEventType.SAMPLE_RECEIPT, metadataSource).getLeft();

        // If the kit has already been partially registered, append a timestamp to make a unique batch name.
        Format simpleDateFormat = FastDateFormat.getInstance("yyyyMMddHHmmssSSSS");
        LabBatch labBatch = labBatchDao.findByName(sampleReceiptBean.getKitId());
        String batchName =
                sampleReceiptBean.getKitId() + (labBatch == null ? "" : "-" + simpleDateFormat.format(new Date()));
        labBatchDao.persist(new LabBatch(batchName, new HashSet<>(labVessels),
                LabBatch.LabBatchType.SAMPLES_RECEIPT));
        return "Samples received: " + batchName;
    }


    void setBspUserList(BSPUserList bspUserList) {
        this.bspUserList = bspUserList;
    }
}
