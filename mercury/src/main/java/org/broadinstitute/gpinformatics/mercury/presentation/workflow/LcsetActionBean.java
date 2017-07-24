package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.broadinstitute.bsp.client.rackscan.ScannerException;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.presentation.vessel.RackScanActionBean;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Allows a user to declare the positive and negative controls in an LCSET.
 */
@UrlBinding(LcsetActionBean.ACTION_BEAN_URL)
public class LcsetActionBean extends RackScanActionBean {
    static final String ACTION_BEAN_URL = "/workflow/Lcset.action";
    private static final String PAGE_TITLE = "LCSET";

    private static final String LCSET_PAGE = "/workflow/lcset_controls.jsp";

    public static final String SCAN_CONTROLS_EVENT = "scanControls";
    public static final String CONFIRM_CONTROLS_EVENT = "confirmControls";

    /** Entered by the user. */
    @Validate(required = true, on = {SCAN_CONTROLS_EVENT, CONFIRM_CONTROLS_EVENT})
    private String lcsetName;

    /** Ask the user to confirm that these should be added to the LCSET. */
    private List<String> controlBarcodes = new ArrayList<>();
    private List<String> addBarcodes = new ArrayList<>();
    private List<String> removeBarcodes = new ArrayList<>();

    private VesselGeometry vesselGeometry = RackOfTubes.RackType.Matrix96.getVesselGeometry();

    @Inject
    private BarcodedTubeDao barcodedTubeDao;

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private LabBatchEjb labBatchEjb;

    /**
     * Populates the list of pico sample dispositions for the jsp to display.
     */
    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(LCSET_PAGE);
    }

    @HandlesEvent(SCAN_CONTROLS_EVENT)
    public Resolution scanControls() throws ScannerException {
        scan();
        MessageCollection messageCollection = new MessageCollection();
        Iterator<Map.Entry<String, String>> it = rackScan.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> pair = it.next();
            if (pair.getValue().isEmpty()) {
                it.remove();
            }
        }

        LabBatchEjb.ValidateRackScanReturn validateRackScanReturn = labBatchEjb.validateRackScan(lcsetName,
                rackScan, messageCollection);
        for (LabVessel labVessel : validateRackScanReturn.getControlTubes()) {
            controlBarcodes.add(labVessel.getLabel());
        }
        for (LabVessel labVessel : validateRackScanReturn.getAddTubes()) {
            addBarcodes.add(labVessel.getLabel());
        }
        for (LabVessel labVessel : validateRackScanReturn.getRemoveTubes()) {
            removeBarcodes.add(labVessel.getLabel());
        }

        addMessages(messageCollection);
        return new ForwardResolution(LCSET_PAGE);
    }

    @HandlesEvent(CONFIRM_CONTROLS_EVENT)
    public Resolution confirmControls() throws ScannerException {
        // todo jmt move these to single method in EJB
        labBatchEjb.addControlsToLcset(lcsetName, controlBarcodes);

        Map<String, BarcodedTube> mapBarcodeToTube = barcodedTubeDao.findByBarcodes(addBarcodes);
        List<Long> bucketEntryIds = new ArrayList<>();
        String bucketName = null;
        for (Map.Entry<String, BarcodedTube> barcodedTubeEntry : mapBarcodeToTube.entrySet()) {
            SampleInstanceV2 sampleInstance = barcodedTubeEntry.getValue().getSampleInstancesV2().iterator().next();
            for (BucketEntry bucketEntry : sampleInstance.getAllBucketEntries()) {
                if (bucketEntry.getLabBatch() == null) {
                    bucketEntryIds.add(bucketEntry.getBucketEntryId());
                    bucketName = bucketEntry.getBucket().getBucketDefinitionName();
                }
            }
        }
        try {
            labBatchEjb.addToLabBatch(lcsetName, bucketEntryIds, null, bucketName, this, null);
        } catch (IOException | ValidationException e) {
            throw new RuntimeException(e);
        }

//        labBatchEjb.removeFromLabBatch();
//        autoExport
        addMessage("Made modifications to LCSET");
        return new ForwardResolution(LCSET_PAGE);
    }

    @SuppressWarnings("SuspiciousGetterSetter")
    @Override
    public String getRackScanPageUrl() {
        return ACTION_BEAN_URL;
    }

    @Override
    public String getPageTitle() {
        return PAGE_TITLE;
    }

    public String getLcsetName() {
        return lcsetName;
    }

    @SuppressWarnings("unused")
    public void setLcsetName(String lcsetName) {
        this.lcsetName = lcsetName;
    }

    public List<String> getControlBarcodes() {
        return controlBarcodes;
    }

    @SuppressWarnings("unused")
    public void setControlBarcodes(List<String> controlBarcodes) {
        this.controlBarcodes = controlBarcodes;
    }

    public List<String> getAddBarcodes() {
        return addBarcodes;
    }

    @SuppressWarnings("unused")
    public void setAddBarcodes(List<String> addBarcodes) {
        this.addBarcodes = addBarcodes;
    }

    public List<String> getRemoveBarcodes() {
        return removeBarcodes;
    }

    @SuppressWarnings("unused")
    public void setRemoveBarcodes(List<String> removeBarcodes) {
        this.removeBarcodes = removeBarcodes;
    }

    public VesselGeometry getVesselGeometry() {
        return vesselGeometry;
    }
}
