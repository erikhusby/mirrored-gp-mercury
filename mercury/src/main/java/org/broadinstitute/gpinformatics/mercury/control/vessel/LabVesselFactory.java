 package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ChildVesselBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ParentVesselBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates LabVessels for (initially) web services
 */
public class LabVesselFactory implements Serializable {

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private BSPUserList bspUserList;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private AthenaClientService athenaClientService;

    /**
     * For each piece of plastic being received: find or create plastic; find or create a corresponding Mercury sample
     * @param parentVesselBeans top level plastic
     * @param userName the name of the user who performed the action (in BSP)
     * @param eventDate when the action was performed
     * @param labEventType type of event to create
     * @return vessels, associated with samples
     */
    public List<LabVessel> buildLabVessels(
            List<ParentVesselBean> parentVesselBeans,
            String userName,
            Date eventDate,
            LabEventType labEventType) {
        List<String> barcodes = new ArrayList<String>();
        List<String> sampleIds = new ArrayList<String>();
        for (ParentVesselBean parentVesselBean : parentVesselBeans) {
            barcodes.add(parentVesselBean.getManufacturerBarcode() == null ? parentVesselBean.getSampleId() :
                    parentVesselBean.getManufacturerBarcode());
            if (parentVesselBean.getSampleId() != null) {
                sampleIds.add(parentVesselBean.getSampleId());
            }
            // todo jmt recurse to support 3-level containers?
            if (parentVesselBean.getChildVesselBeans() != null) {
                for (ChildVesselBean childVesselBean : parentVesselBean.getChildVesselBeans()) {
                    barcodes.add(childVesselBean.getManufacturerBarcode() == null ? childVesselBean.getSampleId() :
                            childVesselBean.getManufacturerBarcode());
                    if (childVesselBean.getSampleId() != null) {
                        sampleIds.add(childVesselBean.getSampleId());
                    }
                }
            }
        }

        Map<String,LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(barcodes);
        Map<String, List<MercurySample>> mapIdToListMercurySample = mercurySampleDao.findMapIdToListMercurySample(sampleIds);
        Map<String, List<ProductOrderSample>> mapIdToListPdoSamples = athenaClientService.findMapSampleNameToPoSample(sampleIds);
        return buildLabVesselDaoFree(mapBarcodeToVessel, mapIdToListMercurySample,
                mapIdToListPdoSamples, userName, eventDate,
                parentVesselBeans, labEventType);
    }

    /**
     * For each piece of plastic being received: find or create plastic; find or create a corresponding Mercury sample
     * @param mapBarcodeToVessel plastic entities
     * @param mapIdToListMercurySample Mercury samples
     * @param mapIdToListPdoSamples from Athena
     * @param userName the name of the user who performed the action (in BSP)
     * @param eventDate when the action was performed
     * @param parentVesselBeans top level plastic
     * @param labEventType type of event to create
     * @return vessels, associated with samples
     */
    @DaoFree
    public List<LabVessel> buildLabVesselDaoFree(
            Map<String, LabVessel> mapBarcodeToVessel,
            Map<String, List<MercurySample>> mapIdToListMercurySample,
            Map<String, List<ProductOrderSample>> mapIdToListPdoSamples,
            String userName,
            Date eventDate,
            List<ParentVesselBean> parentVesselBeans,
            LabEventType labEventType) {

        long disambiguator = 1L;
        List<LabVessel> labVessels = new ArrayList<LabVessel>();
        BspUser bspUser = bspUserList.getByUsername(userName);
        if (bspUser == null) {
            throw new RuntimeException("Failed to find user " + userName);
        }
        Long operator = bspUser.getUserId();

        for (ParentVesselBean parentVesselBean : parentVesselBeans) {
            String sampleId = parentVesselBean.getSampleId();
            String barcode = parentVesselBean.getManufacturerBarcode() == null ? sampleId :
                    parentVesselBean.getManufacturerBarcode();
            // LabVessel may already exist if receipts are being backfilled
            LabVessel labVessel = mapBarcodeToVessel.get(barcode);

            if (parentVesselBean.getChildVesselBeans() == null || parentVesselBean.getChildVesselBeans().isEmpty()) {
                // todo jmt differentiate Cryo vial, Conical, Slide, Flip-top etc.
                TwoDBarcodedTube twoDBarcodedTube = labVessel == null ? new TwoDBarcodedTube(barcode) :
                        (TwoDBarcodedTube) labVessel;

                MercurySample mercurySample = getMercurySample(mapIdToListMercurySample, mapIdToListPdoSamples, sampleId);
                twoDBarcodedTube.addSample(mercurySample);
                twoDBarcodedTube.addInPlaceEvent(new LabEvent(labEventType, eventDate, "BSP", disambiguator, operator));
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
                        if (vesselPosition == null) {
                            throw new RuntimeException("Unknown vessel position " + childVesselBean.getPosition());
                        }
                        PlateWell plateWell = new PlateWell(staticPlate, vesselPosition);
                        plateWell.addSample(getMercurySample(mapIdToListMercurySample, mapIdToListPdoSamples,
                                childVesselBean.getSampleId()));
                        staticPlate.getContainerRole().addContainedVessel(plateWell, vesselPosition);
                    }
                    staticPlate.addInPlaceEvent(new LabEvent(labEventType, eventDate, "BSP", disambiguator, operator));
                    disambiguator++;
                } else if (vesselType.contains("rack")) {
                    RackOfTubes rackOfTubes = new RackOfTubes(parentVesselBean.getManufacturerBarcode(),
                            RackOfTubes.RackType.Matrix96);
                    Map<VesselPosition, TwoDBarcodedTube> mapPositionToTube = new HashMap<VesselPosition, TwoDBarcodedTube>();
                    for (ChildVesselBean childVesselBean : parentVesselBean.getChildVesselBeans()) {
                        VesselPosition vesselPosition = VesselPosition.getByName(childVesselBean.getPosition());
                        if (vesselPosition == null) {
                            throw new RuntimeException("Unknown vessel position " + childVesselBean.getPosition());
                        }
                        TwoDBarcodedTube twoDBarcodedTube = new TwoDBarcodedTube(childVesselBean.getManufacturerBarcode());
                        twoDBarcodedTube.addSample(getMercurySample(mapIdToListMercurySample, mapIdToListPdoSamples,
                                childVesselBean.getSampleId()));
                        twoDBarcodedTube.addInPlaceEvent(new LabEvent(labEventType, eventDate, "BSP", disambiguator,
                                operator));
                        disambiguator++;
                        mapPositionToTube.put(vesselPosition, twoDBarcodedTube);
                        labVessels.add(twoDBarcodedTube);
                    }
                    TubeFormation tubeFormation = new TubeFormation(mapPositionToTube,
                            RackOfTubes.RackType.Matrix96);
                    tubeFormation.addRackOfTubes(rackOfTubes);
                } else {
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

    public void setBspUserList(BSPUserList bspUserList) {
        this.bspUserList = bspUserList;
    }
}
