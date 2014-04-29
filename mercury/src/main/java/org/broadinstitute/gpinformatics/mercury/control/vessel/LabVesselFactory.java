package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
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

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Creates LabVessels for (initially) web services
 */
@Stateful
@RequestScoped
public class LabVesselFactory implements Serializable {

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private ProductOrderSampleDao productOrderSampleDao;

    /**
     * For each piece of plastic being received: find or create plastic; find or create a corresponding Mercury sample
     *
     * @param parentVesselBeans top level plastic
     * @param userName          the name of the user who performed the action (in BSP)
     * @param eventDate         when the action was performed
     * @param labEventType      type of event to create
     *
     * @return vessels, associated with samples
     */
    public List<LabVessel> buildLabVessels(
            List<ParentVesselBean> parentVesselBeans,
            String userName,
            Date eventDate,
            LabEventType labEventType) {
        List<String> barcodes = new ArrayList<>();
        List<String> sampleIds = new ArrayList<>();
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

        Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(barcodes);
        Map<String, List<MercurySample>> mapIdToListMercurySample = mercurySampleDao.findMapIdToListMercurySample(
                sampleIds);
        Map<String, Set<ProductOrderSample>> mapIdToListPdoSamples = productOrderSampleDao.findMapBySamples(sampleIds);
        return buildLabVesselDaoFree(mapBarcodeToVessel, mapIdToListMercurySample,
                                     mapIdToListPdoSamples, userName, eventDate,
                                     parentVesselBeans, labEventType);
    }


    /**
     * Creates initial vessel and sample, standalone, without parent vessel or corresponding event.
     *
     * @param sampleName the sample name/key
     * @param barcode    the vessel barcode
     * @param userName   the BSP user
     * @param eventDate  when the action was performed
     *
     * @return vessels, associated with samples
     */
    public List<LabVessel> buildInitialLabVessels(String sampleName, String barcode, String userName, Date eventDate) {
        List<String> barcodes = new ArrayList<>();
        barcodes.add(barcode);
        Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(barcodes);

        List<String> sampleIds = new ArrayList<>();
        sampleIds.add(sampleName);
        Map<String, List<MercurySample>> mapIdToListMercurySample =
                mercurySampleDao.findMapIdToListMercurySample(sampleIds);
        Map<String, Set<ProductOrderSample>> mapIdToListPdoSamples =
                productOrderSampleDao.findMapBySamples(sampleIds);

        List<ParentVesselBean> parentVesselBeans = new ArrayList<>();
        parentVesselBeans.add(new ParentVesselBean(barcode, sampleName, null, null));

        return buildLabVesselDaoFree(mapBarcodeToVessel, mapIdToListMercurySample, mapIdToListPdoSamples, userName,
                                     eventDate, parentVesselBeans, null);
    }

    /**
     * For each piece of plastic being received: find or create plastic; find or create a corresponding Mercury sample
     *
     * @param mapBarcodeToVessel       plastic entities
     * @param mapIdToListMercurySample Mercury samples
     * @param mapIdToListPdoSamples    from Athena
     * @param userName                 the name of the user who performed the action (in BSP)
     * @param eventDate                when the action was performed
     * @param parentVesselBeans        top level plastic
     * @param labEventType             type of event to create
     *
     * @return vessels, associated with samples
     */
    @DaoFree
    public List<LabVessel> buildLabVesselDaoFree(
            Map<String, LabVessel> mapBarcodeToVessel,
            Map<String, List<MercurySample>> mapIdToListMercurySample,
            Map<String, Set<ProductOrderSample>> mapIdToListPdoSamples,
            String userName,
            Date eventDate,
            List<ParentVesselBean> parentVesselBeans,
            LabEventType labEventType) {

        List<LabVessel> labVessels = new ArrayList<>();
        BspUser bspUser = bspUserList.getByUsername(userName);
        if (bspUser == null) {
            throw new RuntimeException("Failed to find user " + userName);
        }
        Long operator = bspUser.getUserId();

        long disambiguator = 1L;
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

                MercurySample mercurySample = getMercurySample(mapIdToListMercurySample, mapIdToListPdoSamples,
                                                               sampleId);
                twoDBarcodedTube.addSample(mercurySample);
                if (labEventType != null) {
                    twoDBarcodedTube.addInPlaceEvent(new LabEvent(labEventType, eventDate, "BSP", disambiguator,
                                                                  operator, "BSP"));
                    disambiguator++;
                }
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
                    staticPlate.addInPlaceEvent(new LabEvent(labEventType, eventDate, "BSP", disambiguator, operator,
                                                             "BSP"));
                    disambiguator++;
                } else if (vesselType.contains("rack") || vesselType.contains("box")) {
                    RackOfTubes rackOfTubes =
                            (RackOfTubes) mapBarcodeToVessel.get(parentVesselBean.getManufacturerBarcode());
                    if (rackOfTubes == null) {
                        rackOfTubes = new RackOfTubes(parentVesselBean.getManufacturerBarcode(),
                                                      RackOfTubes.RackType.Matrix96);
                    }
                    Map<VesselPosition, TwoDBarcodedTube> mapPositionToTube = new HashMap<>();
                    for (ChildVesselBean childVesselBean : parentVesselBean.getChildVesselBeans()) {
                        VesselPosition vesselPosition = VesselPosition.getByName(childVesselBean.getPosition());
                        if (vesselPosition == null) {
                            throw new RuntimeException("Unknown vessel position " + childVesselBean.getPosition());
                        }
                        TwoDBarcodedTube twoDBarcodedTube = (TwoDBarcodedTube) mapBarcodeToVessel.get(
                                childVesselBean.getManufacturerBarcode());
                        if (twoDBarcodedTube == null) {
                            twoDBarcodedTube = new TwoDBarcodedTube(childVesselBean.getManufacturerBarcode());
                        }
                        twoDBarcodedTube.addSample(getMercurySample(mapIdToListMercurySample, mapIdToListPdoSamples,
                                                                    childVesselBean.getSampleId()));
                        twoDBarcodedTube.addInPlaceEvent(new LabEvent(labEventType, eventDate, "BSP", disambiguator,
                                                                      operator, "BSP"));
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
     *
     * @param mapIdToListMercurySample Mercury samples
     * @param mapIdToListPdoSamples    Athena samples
     * @param sampleId                 ID for which to create the Mercury Sample
     *
     * @return MercurySample with mandatory sampleId and optional Product Order
     */
    @DaoFree
    private MercurySample getMercurySample(Map<String, List<MercurySample>> mapIdToListMercurySample,
                                           Map<String, Set<ProductOrderSample>> mapIdToListPdoSamples,
                                           String sampleId) {
        List<MercurySample> mercurySamples = mapIdToListMercurySample.get(sampleId);
        if (mercurySamples == null) {
            mercurySamples = Collections.emptyList();
        }
        Set<ProductOrderSample> productOrderSamples = mapIdToListPdoSamples.get(sampleId);
        if (productOrderSamples == null) {
            productOrderSamples = Collections.emptySet();
        }

        MercurySample mercurySample = null;
        if (mercurySamples.isEmpty()) {
            if (productOrderSamples.isEmpty()) {
                mercurySample = new MercurySample(sampleId);
            } else {
                for (ProductOrderSample productOrderSample : productOrderSamples) {
                    mercurySample = new MercurySample(productOrderSample.getName());
                }
            }
        } else if (mercurySamples.size() > 1) {
            throw new RuntimeException("More than one MercurySample for " + sampleId);
        } else {
            mercurySample = mercurySamples.get(0);
        }
        return mercurySample;
    }

    public void setBspUserList(BSPUserList bspUserList) {
        this.bspUserList = bspUserList;
    }
}
