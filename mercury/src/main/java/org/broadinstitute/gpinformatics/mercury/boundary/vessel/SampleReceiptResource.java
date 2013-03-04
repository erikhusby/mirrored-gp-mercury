package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.ObjectMarshaller;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.infrastructure.ws.WsMessageStore;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * JAX-RS web service used by BSP to notify Mercury that samples have been received.
 */
@SuppressWarnings("FeatureEnvy")
@Path("/samplereceipt")
@Stateful
@RequestScoped
public class SampleReceiptResource {

    // todo jmt currently set to thompson, which user should this be?
    private static final long OPERATOR = 1701L;

    private static final Log LOG = LogFactory.getLog(SampleReceiptResource.class);

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private LabBatchDAO labBatchDAO;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private AthenaClientService athenaClientService;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private WsMessageStore wsMessageStore;

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
            SampleReceiptBean sampleReceiptBean = ObjectMarshaller.unmarshall(SampleReceiptBean.class,
                    new StringReader(sampleReceiptBeanXml));
            return notifyOfReceipt(sampleReceiptBean);
        } catch (Exception e) {
            wsMessageStore.recordError(WsMessageStore.SAMPLE_RECEIPT_RESOURCE_TYPE, sampleReceiptBeanXml, now, e);
            LOG.error("Failed to process sample receipt", e);
            throw new ResourceException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * For samples received by BSP: find or create plastic; find or create MercurySamples; create receipt LabBatch.
     * @param sampleReceiptBean from BSP
     * @return string indicating success
     */
    public String notifyOfReceipt(SampleReceiptBean sampleReceiptBean) {
        List<String> barcodes = new ArrayList<String>();
        List<String> sampleIds = new ArrayList<String>();
        for (ParentVesselBean parentVesselBean : sampleReceiptBean.getParentVesselBeans()) {
            barcodes.add(parentVesselBean.getManufacturerBarcode() == null ? parentVesselBean.getSampleId() :
                    parentVesselBean.getManufacturerBarcode());
            if(parentVesselBean.getSampleId() != null) {
                sampleIds.add(parentVesselBean.getSampleId());
            }
            // todo jmt recurse to support 3-level containers?
            if(parentVesselBean.getChildVesselBeans() != null) {
                for (ChildVesselBean childVesselBean : parentVesselBean.getChildVesselBeans()) {
                    barcodes.add(childVesselBean.getManufacturerBarcode() == null ? childVesselBean.getSampleId() :
                            childVesselBean.getManufacturerBarcode());
                    if(childVesselBean.getSampleId() != null) {
                        sampleIds.add(childVesselBean.getSampleId());
                    }
                }
            }
        }

        Map<String,LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(barcodes);
        Map<String, List<MercurySample>> mapIdToListMercurySample = mercurySampleDao.findMapIdToListMercurySample(sampleIds);
        Map<String, List<ProductOrderSample>> mapIdToListPdoSamples = athenaClientService.findMapBySamples(sampleIds);
        List<LabVessel> labVessels = notifyOfReceiptDaoFree(sampleReceiptBean, mapBarcodeToVessel, mapIdToListMercurySample,
                mapIdToListPdoSamples);

        // If the kit has already been partially registered, append a timestamp to make a unique batch name
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSSS");
        LabBatch labBatch = labBatchDAO.findByName(sampleReceiptBean.getKitId());
        String batchName = sampleReceiptBean.getKitId() + (labBatch == null ? "" : "-" + simpleDateFormat.format(new Date()));
        labBatchDAO.persist(new LabBatch(batchName, new HashSet<LabVessel>(labVessels),
                LabBatch.LabBatchType.SAMPLES_RECEIPT));
        return "Samples received";
    }

    /**
     * For each piece of plastic being received: find or create plastic; find or create a corresponding Mercury sample
     * @param sampleReceiptBean DTO
     * @param mapBarcodeToVessel plastic entities
     * @param mapIdToListMercurySample Mercury samples
     * @param mapIdToListPdoSamples from Athena
     * @return vessels, associated with samples
     */
    @DaoFree
    List<LabVessel> notifyOfReceiptDaoFree(SampleReceiptBean sampleReceiptBean,
            Map<String, LabVessel> mapBarcodeToVessel,
            Map<String, List<MercurySample>> mapIdToListMercurySample,
            Map<String, List<ProductOrderSample>> mapIdToListPdoSamples) {

        long disambiguator = 1L;
        List<LabVessel> labVessels = new ArrayList<LabVessel>();
        for (ParentVesselBean parentVesselBean : sampleReceiptBean.getParentVesselBeans()) {
            String sampleId = parentVesselBean.getSampleId();
            String barcode = parentVesselBean.getManufacturerBarcode() == null ? sampleId :
                    parentVesselBean.getManufacturerBarcode();
            // LabVessel may already exist if receipts are being backfilled
            LabVessel labVessel = mapBarcodeToVessel.get(barcode);

            if(parentVesselBean.getChildVesselBeans() == null || parentVesselBean.getChildVesselBeans().isEmpty()) {
                // todo jmt differentiate Cryo vial, Conical, Slide, Flip-top etc.
                TwoDBarcodedTube twoDBarcodedTube = labVessel == null ? new TwoDBarcodedTube(barcode) :
                        (TwoDBarcodedTube) labVessel;

                MercurySample mercurySample = getMercurySample(mapIdToListMercurySample, mapIdToListPdoSamples, sampleId);
                twoDBarcodedTube.addSample(mercurySample);
                twoDBarcodedTube.addInPlaceEvent(new LabEvent(LabEventType.SAMPLE_RECEIPT,
                        sampleReceiptBean.getReceiptDate(), "BSP", disambiguator, OPERATOR));
                disambiguator++;
                labVessels.add(twoDBarcodedTube);
            } else {
                String vesselType = parentVesselBean.getVesselType().toLowerCase();
                if (vesselType.contains("plate")) {
                    // todo jmt map other geometries
                    StaticPlate staticPlate = new StaticPlate(parentVesselBean.getManufacturerBarcode(),
                            StaticPlate.PlateType.Eppendorf96);
                    labVessels.add(staticPlate);
                    for (ChildVesselBean childVesselBean : parentVesselBean.getChildVesselBeans()) {
                        VesselPosition vesselPosition = VesselPosition.getByName(childVesselBean.getPosition());
                        if(vesselPosition == null) {
                            throw new RuntimeException("Unknown vessel position " + childVesselBean.getPosition());
                        }
                        PlateWell plateWell = new PlateWell(staticPlate, vesselPosition);
                        plateWell.addSample(getMercurySample(mapIdToListMercurySample, mapIdToListPdoSamples,
                                childVesselBean.getSampleId()));
                        staticPlate.getContainerRole().addContainedVessel(plateWell, vesselPosition);
                    }
                    staticPlate.addInPlaceEvent(new LabEvent(LabEventType.SAMPLE_RECEIPT,
                            sampleReceiptBean.getReceiptDate(), "BSP", disambiguator, OPERATOR));
                } /* todo jmt else if(vesselType.contains("rack")) {

                } */else {
                    throw new RuntimeException("Unexpected vessel type with child vessels " +
                            parentVesselBean.getVesselType());
                }
            }
        }

        return labVessels;
    }

    /**
     * Find or create a Mercury Sample for a given sampleId.  If MercurySample exists, use it; else if
     * ProductOrderSample exists, use its ProductOrder; else create MercurySample without ProductOrder key.
     * @param mapIdToListMercurySample Mercury samples
     * @param mapIdToListPdoSamples Athena samples
     * @param sampleId ID for which to create the Mercury Sample
     * @return MercurySample with mandatory sampleId and optional Product Order
     */
    @DaoFree
    private MercurySample getMercurySample(Map<String, List<MercurySample>> mapIdToListMercurySample,
            Map<String, List<ProductOrderSample>> mapIdToListPdoSamples, String sampleId) {
        List<MercurySample> mercurySamples = mapIdToListMercurySample.get(sampleId);
        if(mercurySamples == null) {
            mercurySamples = Collections.emptyList();
        }
        List<ProductOrderSample> productOrderSamples = mapIdToListPdoSamples.get(sampleId);
        if(productOrderSamples == null) {
            productOrderSamples = Collections.emptyList();
        }
        if(productOrderSamples.size() > 1) {
            throw new RuntimeException("More than one ProductOrderSample for " + sampleId);
        }

        MercurySample mercurySample;
        if(mercurySamples.isEmpty()) {
            if(productOrderSamples.isEmpty()) {
                mercurySample = new MercurySample(sampleId);
            } else {
                ProductOrderSample productOrderSample = productOrderSamples.get(0);
                mercurySample = new MercurySample(productOrderSample.getProductOrder().getBusinessKey(),
                        productOrderSample.getSampleName());
            }
        } else if(mercurySamples.size() > 1) {
            throw new RuntimeException("More than one MercurySample for " + sampleId);
        } else {
            mercurySample = mercurySamples.get(0);
            if(mercurySample.getProductOrderKey() == null && !productOrderSamples.isEmpty()) {
                mercurySample.setProductOrderKey(productOrderSamples.get(0).getProductOrder().getBusinessKey());
            }
        }
        return mercurySample;
    }
}
