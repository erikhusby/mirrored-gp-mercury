package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.lang3.tuple.Pair;
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
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.annotation.Nonnull;
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
     * @return A pair containing 2 lists of lab vessels: <br/>
     *   Left are primary vessels, e.g. associated with samples for batch starting vessels <br/>
     *   Right are vessels which may require persisting to support container logic (e.g. tube formations, racks)
     */
    public Pair<List<LabVessel>,List<LabVessel>> buildLabVessels(
            List<ParentVesselBean> parentVesselBeans,
            String userName,
            Date eventDate,
            LabEventType labEventType,
            MercurySample.MetadataSource metadataSource) {
        List<String> barcodes = new ArrayList<>();
        List<String> sampleIds = new ArrayList<>();
        for (ParentVesselBean parentVesselBean : parentVesselBeans) {
            barcodes.add(parentVesselBean.getManufacturerBarcode() == null ? parentVesselBean.getSampleId() :
                                 parentVesselBean.getManufacturerBarcode());

            String parentVesselType = parentVesselBean.getVesselType();

            if (parentVesselBean.getSampleId() != null) {
                sampleIds.add(parentVesselBean.getSampleId());
            }
            // todo jmt recurse to support 3-level containers?
            if (parentVesselBean.getChildVesselBeans() != null) {

                // Possibly needed to build tube formation
                Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();

                for (ChildVesselBean childVesselBean : parentVesselBean.getChildVesselBeans()) {

                    String childBarcode = childVesselBean.getManufacturerBarcode() == null ?
                            childVesselBean.getSampleId() :
                            childVesselBean.getManufacturerBarcode();
                    barcodes.add(childBarcode);

                    if (childVesselBean.getSampleId() != null) {
                        sampleIds.add(childVesselBean.getSampleId());
                    }

                    // If rack of tubes, build a tube formation to see if it already exists!
                    if (parentVesselType.toLowerCase().contains("rack") || parentVesselType.toLowerCase()
                            .contains("box")
                        || RackOfTubes.RackType.getByName(parentVesselType) != null) {

                        VesselPosition vesselPosition = VesselPosition.getByName(childVesselBean.getPosition());
                        if (vesselPosition == null) {
                            throw new RuntimeException("Unknown vessel position " + childVesselBean.getPosition());
                        }
                        BarcodedTube barcodedTube = new BarcodedTube(childBarcode);
                        mapPositionToTube.put(vesselPosition, barcodedTube);
                    }
                }

                if( !mapPositionToTube.isEmpty()) {
                    RackOfTubes.RackType tubeFormationType = RackOfTubes.RackType.getByName(parentVesselType);
                    if (tubeFormationType == null) {
                        tubeFormationType = RackOfTubes.RackType.Matrix96;
                    }
                    TubeFormation tubeFormation = new TubeFormation(mapPositionToTube, tubeFormationType);
                    // Query for existing tube formation
                    barcodes.add(tubeFormation.getLabel());
                }
            }
        }

        Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(barcodes);
        Map<String, MercurySample> mapIdToMercurySample = mercurySampleDao.findMapIdToMercurySample(
                sampleIds);
        Map<String, Set<ProductOrderSample>> mapIdToListPdoSamples = productOrderSampleDao.findMapBySamples(sampleIds);
        return buildLabVesselDaoFree(mapBarcodeToVessel, mapIdToMercurySample,
                                     mapIdToListPdoSamples, userName, eventDate,
                                     parentVesselBeans, labEventType, metadataSource);
    }


    /**
     * Creates initial vessel and sample, standalone, without parent vessel or corresponding event.
     *
     * @param sampleName the sample name/key
     * @param barcode    the vessel barcode
     * @param userName   the BSP user
     * @param eventDate  when the action was performed
     *
     * @return A pair containing 2 lists of lab vessels: <br/>
     *   Left are primary vessels, e.g. associated with samples for batch starting vessels <br/>
     *   Right are vessels which may require persisting to support container logic (e.g. tube formations, racks)
     */
    public Pair<List<LabVessel>,List<LabVessel>> buildInitialLabVessels(String sampleName, String barcode, String userName, Date eventDate,
            MercurySample.MetadataSource metadataSource) {
        List<String> barcodes = new ArrayList<>();
        barcodes.add(barcode);
        Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(barcodes);

        List<String> sampleIds = new ArrayList<>();
        sampleIds.add(sampleName);
        Map<String, MercurySample> mapIdToListMercurySample =
                mercurySampleDao.findMapIdToMercurySample(sampleIds);
        Map<String, Set<ProductOrderSample>> mapIdToListPdoSamples =
                productOrderSampleDao.findMapBySamples(sampleIds);

        List<ParentVesselBean> parentVesselBeans = new ArrayList<>();
        parentVesselBeans.add(new ParentVesselBean(barcode, sampleName, null, null));

        return buildLabVesselDaoFree(mapBarcodeToVessel, mapIdToListMercurySample, mapIdToListPdoSamples, userName,
                eventDate, parentVesselBeans, null, metadataSource);
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
     * @return A pair containing 2 lists of lab vessels: <br/>
     *   Left are primary vessels, e.g. associated with samples for batch starting vessels <br/>
     *   Right are vessels which may require persisting to support container logic (e.g. tube formations, racks)
     */
    @DaoFree
    public Pair<List<LabVessel>,List<LabVessel>> buildLabVesselDaoFree(
            Map<String, LabVessel> mapBarcodeToVessel,
            Map<String, MercurySample> mapIdToListMercurySample,
            Map<String, Set<ProductOrderSample>> mapIdToListPdoSamples,
            String userName,
            Date eventDate,
            List<ParentVesselBean> parentVesselBeans,
            LabEventType labEventType,
            MercurySample.MetadataSource metadataSource) {

        // These are directly related to downstream assignments (e.g. batch starting vessels)
        List<LabVessel> primaryLabVessels = new ArrayList<>();
        // These require persisting to support container logic (e.g. tube formations, racks)
        List<LabVessel> secondaryLabVessels = new ArrayList<>();

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
            String vesselType = parentVesselBean.getVesselType();

            // LabVessel may already exist if receipts are being backfilled
            LabVessel labVessel = mapBarcodeToVessel.get(barcode);

            if (parentVesselBean.getChildVesselBeans() == null || parentVesselBean.getChildVesselBeans().isEmpty()) {
                BarcodedTube.BarcodedTubeType tubeType = BarcodedTube.BarcodedTubeType.getByAutomationName(vesselType);
                if (tubeType == null) {
                    tubeType = BarcodedTube.BarcodedTubeType.MatrixTube;
                }
                BarcodedTube barcodedTube = labVessel == null ? new BarcodedTube(barcode, tubeType) :
                        (BarcodedTube) labVessel;

                MercurySample mercurySample = getMercurySample(mapIdToListMercurySample, mapIdToListPdoSamples,
                        sampleId, metadataSource);
                barcodedTube.addSample(mercurySample);
                if (labEventType != null) {
                    barcodedTube.addInPlaceEvent(new LabEvent(labEventType, eventDate, "BSP", disambiguator,
                                                                  operator, "BSP"));
                    disambiguator++;
                }
                primaryLabVessels.add(barcodedTube);
            } else {
                if (vesselType.toLowerCase().contains("plate")) {
                    StaticPlate.PlateType plateType = StaticPlate.PlateType.getByAutomationName(vesselType);
                    if (plateType == null) {
                        plateType = StaticPlate.PlateType.Eppendorf96;
                    }
                    StaticPlate staticPlate = (StaticPlate) mapBarcodeToVessel.get(
                            parentVesselBean.getManufacturerBarcode());
                    if (staticPlate == null) {
                        staticPlate = new StaticPlate(parentVesselBean.getManufacturerBarcode(), plateType,
                                parentVesselBean.getPlateName());
                    } else {
                        staticPlate.setName(parentVesselBean.getPlateName());
                    }
                    primaryLabVessels.add(staticPlate);

                    for (ChildVesselBean childVesselBean : parentVesselBean.getChildVesselBeans()) {
                        VesselPosition vesselPosition = VesselPosition.getByName(childVesselBean.getPosition());
                        if (vesselPosition == null) {
                            throw new RuntimeException("Unknown vessel position " + childVesselBean.getPosition());
                        }
                        PlateWell plateWell = staticPlate.getContainerRole().getVesselAtPosition(vesselPosition);
                        if (plateWell == null) {
                            plateWell = new PlateWell(staticPlate, vesselPosition);
                            staticPlate.getContainerRole().addContainedVessel(plateWell, vesselPosition);
                        }
                        plateWell.addSample(getMercurySample(mapIdToListMercurySample, mapIdToListPdoSamples,
                                childVesselBean.getSampleId(), metadataSource));
                    }
                    if (labEventType != null) {
                        staticPlate.addInPlaceEvent(new LabEvent(labEventType, eventDate, "BSP", disambiguator, operator,
                                                                 "BSP"));
                    }
                    disambiguator++;
                } else if (vesselType.toLowerCase().contains("rack") || vesselType.toLowerCase().contains("box")
                           || RackOfTubes.RackType.getByName(vesselType) != null) {
                    RackOfTubes rackOfTubes =
                            (RackOfTubes) mapBarcodeToVessel.get(parentVesselBean.getManufacturerBarcode());
                    if (rackOfTubes == null) {
                        RackOfTubes.RackType rackType = RackOfTubes.RackType.getByName(vesselType);
                        if (rackType == null) {
                            rackType = RackOfTubes.RackType.Matrix96;
                        }
                        rackOfTubes = new RackOfTubes(parentVesselBean.getManufacturerBarcode(), rackType,
                                parentVesselBean.getPlateName());
                    } else {
                        rackOfTubes.setName(parentVesselBean.getPlateName());
                    }
                    // Insure it's persisted
                    secondaryLabVessels.add(rackOfTubes);

                    Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
                    for (ChildVesselBean childVesselBean : parentVesselBean.getChildVesselBeans()) {
                        VesselPosition vesselPosition = VesselPosition.getByName(childVesselBean.getPosition());
                        if (vesselPosition == null) {
                            throw new RuntimeException("Unknown vessel position " + childVesselBean.getPosition());
                        }
                        BarcodedTube barcodedTube = (BarcodedTube) mapBarcodeToVessel.get(
                                childVesselBean.getManufacturerBarcode());
                        if (barcodedTube == null) {
                            barcodedTube = new BarcodedTube(childVesselBean.getManufacturerBarcode(),
                                    childVesselBean.getVesselType());
                        }
                        barcodedTube.addSample(getMercurySample(mapIdToListMercurySample, mapIdToListPdoSamples,
                                childVesselBean.getSampleId(), metadataSource));
                        if (labEventType != null) {
                            barcodedTube.addInPlaceEvent(new LabEvent(labEventType, eventDate, "BSP", disambiguator,
                                    operator, "BSP"));
                        }
                        disambiguator++;
                        mapPositionToTube.put(vesselPosition, barcodedTube);
                        primaryLabVessels.add(barcodedTube);
                    }
                    RackOfTubes.RackType tubeFormationType = RackOfTubes.RackType.getByName(vesselType);
                    if (tubeFormationType == null) {
                        tubeFormationType = RackOfTubes.RackType.Matrix96;
                    }
                    TubeFormation tubeFormation = new TubeFormation(mapPositionToTube, tubeFormationType);

                    // Is there an existing tube formation?
                    TubeFormation existingTubeFormation =
                            (TubeFormation) mapBarcodeToVessel.get(tubeFormation.getLabel());
                    if (existingTubeFormation != null) {
                        tubeFormation = existingTubeFormation;
                    }
                    tubeFormation.addRackOfTubes(rackOfTubes);
                    if (labEventType != null) {
                        tubeFormation.addInPlaceEvent(new LabEvent(labEventType, eventDate, "BSP", disambiguator, operator,
                                "BSP"));
                    }
                    // Insure it's persisted
                    secondaryLabVessels.add(tubeFormation);
                } else {
                    throw new RuntimeException("Unexpected vessel type with child vessels " +
                                               parentVesselBean.getVesselType());
                }
            }
        }

        return Pair.of(primaryLabVessels, secondaryLabVessels);
    }

    /**
     * Find or create a Mercury Sample for a given sampleId.  If MercurySample exists, use it; else if
     * ProductOrderSample exists, use its ProductOrder; else create MercurySample without ProductOrder key.
     *
     * @param mapIdToListMercurySample Mercury samples
     * @param mapIdToListPdoSamples    Athena samples
     * @param sampleId                 ID for which to create the Mercury Sample
     *
     * @param metadataSource BSP or Mercury
     * @return MercurySample with mandatory sampleId and optional Product Order
     */
    @DaoFree
    private MercurySample getMercurySample(Map<String, MercurySample> mapIdToListMercurySample,
            Map<String, Set<ProductOrderSample>> mapIdToListPdoSamples,
            String sampleId, @Nonnull MercurySample.MetadataSource metadataSource) {
        MercurySample mercurySample = mapIdToListMercurySample.get(sampleId);

        Set<ProductOrderSample> productOrderSamples = mapIdToListPdoSamples.get(sampleId);
        if (productOrderSamples == null) {
            productOrderSamples = Collections.emptySet();
        }

        if (mercurySample == null) {
            mercurySample = new MercurySample(sampleId, metadataSource);
        }

        for (ProductOrderSample productOrderSample : productOrderSamples) {
            mercurySample.addProductOrderSample(productOrderSample);
        }

        return mercurySample;
    }

    public void setBspUserList(BSPUserList bspUserList) {
        this.bspUserList = bspUserList;
    }
}
