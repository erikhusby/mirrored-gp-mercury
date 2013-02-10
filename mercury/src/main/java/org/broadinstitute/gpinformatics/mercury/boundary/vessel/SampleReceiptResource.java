package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.inject.Inject;
import javax.ws.rs.POST;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JAX-RS web service used by BSP to notify Mercury that samples have been received.
 */
@SuppressWarnings("FeatureEnvy")
public class SampleReceiptResource {

    @Inject
    private LabVesselDao labVesselDao;

    @POST
    public String notifyOfReceipt(SampleReceiptBean sampleReceiptBean) {
        List<String> barcodes = new ArrayList<String>();
        for (ParentVesselBean parentVesselBean : sampleReceiptBean.getParentVesselBeans()) {
            barcodes.add(parentVesselBean.getManufacturerBarcode() == null ? parentVesselBean.getSampleId() :
                    parentVesselBean.getManufacturerBarcode());
            // todo jmt recurse to support 3-level containers?
            if(parentVesselBean.getChildVesselBeans() != null) {
                for (ChildVesselBean childVesselBean : parentVesselBean.getChildVesselBeans()) {
                    barcodes.add(childVesselBean.getManufacturerBarcode() == null ? childVesselBean.getSampleId() :
                            childVesselBean.getManufacturerBarcode());
                }
            }
        }
        Map<String,LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(barcodes);
        notifyOfReceiptDaoFree(sampleReceiptBean, mapBarcodeToVessel);
        return "Samples received";
    }

    @DaoFree
    List<LabVessel> notifyOfReceiptDaoFree(SampleReceiptBean sampleReceiptBean, Map<String,LabVessel> mapBarcodeToVessel) {
        List<LabVessel> labVessels = new ArrayList<LabVessel>();
        for (ParentVesselBean parentVesselBean : sampleReceiptBean.getParentVesselBeans()) {
            String barcode = parentVesselBean.getManufacturerBarcode() == null ? parentVesselBean.getSampleId() :
                    parentVesselBean.getManufacturerBarcode();
            LabVessel labVessel = mapBarcodeToVessel.get(barcode);
            if(labVessel != null) {
                throw new RuntimeException("Vessel has already been received " + barcode);
            }

            if(parentVesselBean.getChildVesselBeans() == null || parentVesselBean.getChildVesselBeans().isEmpty()) {
                // todo jmt differentiate Cryo vial, Conical, Slide, Flip-top etc.?
                TwoDBarcodedTube twoDBarcodedTube = new TwoDBarcodedTube(barcode);
                twoDBarcodedTube.addSample(new MercurySample(parentVesselBean.getProductOrderKey(),
                        parentVesselBean.getSampleId()));
                labVessels.add(twoDBarcodedTube);
            } else {
                String vesselType = parentVesselBean.getVesselType().toLowerCase();
                if (vesselType.contains("plate")) {
                    // todo jmt map other geometries
                    StaticPlate staticPlate = new StaticPlate(parentVesselBean.getManufacturerBarcode(), StaticPlate.PlateType.Eppendorf96);
                    labVessels.add(staticPlate);
                    for (ChildVesselBean childVesselBean : parentVesselBean.getChildVesselBeans()) {
                        VesselPosition vesselPosition = VesselPosition.getByName(childVesselBean.getPosition());
                        if(vesselPosition == null) {
                            throw new RuntimeException("Unknown vessel position " + childVesselBean.getPosition());
                        }
                        staticPlate.getContainerRole().addContainedVessel(new PlateWell(staticPlate, vesselPosition), vesselPosition);
                    }

                } /*else if(vesselType.contains("rack")) {

                } */else {
                    throw new RuntimeException("Unexpected vessel type with child vessels " + parentVesselBean.getVesselType());
                }
            }
        }

        return labVessels;
    }
}
