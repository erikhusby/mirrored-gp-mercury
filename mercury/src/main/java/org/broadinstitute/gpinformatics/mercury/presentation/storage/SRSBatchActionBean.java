package org.broadinstitute.gpinformatics.mercury.presentation.storage;


import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * This Action Bean takes Lab Batch or Lab Vessel Barcodes and generates XL20 pick lists by searching for the
 * tubes in the storage system.
 */
@UrlBinding(value = SRSBatchActionBean.ACTION_BEAN_URL)
public class SRSBatchActionBean extends CoreActionBean {

    private static final Logger logger = Logger.getLogger(SRSBatchActionBean.class.getName());

    // Page configuration
    private static final String SRS_BATCH = "SRS Batch";
    public static final String CREATE_BATCH = CoreActionBean.CREATE + SRS_BATCH;
    public static final String EDIT_BATCH = CoreActionBean.EDIT + SRS_BATCH;

    public static final String ACTION_BEAN_URL = "/storage/srs.action";
    private static final String VIEW_PAGE = "/storage/srs_batch.jsp";
    public static final String SRS_BATCH_PARAMETER = "labBatch";

    // Stages
    public enum Stage {
        CHOOSING, EDITING
    }

    // Events and outcomes
    private Stage stage;
    static final String ADD_SAMPLES_ACTION = "evtAddSamples";
    static final String ADD_BARCODES_ACTION = "evtAddBarcodes";
    static final String REMOVE_ACTION = "evtRemove";
    static final String TOGGLE_STATUS_ACTION = "evtToggleBatchStatus";

    private String batchName;
    private LabBatch labBatch;
    private Long labBatchId;
    private String[] inputValues;
    List<PickWorkspaceActionBean.BatchSelectionData> batchSelectionList;

    public SRSBatchActionBean() {
        super(CREATE_BATCH, EDIT_BATCH, SRS_BATCH_PARAMETER);
    }

    @Inject
    private LabBatchDao labBatchDao;
    @Inject
    private LabVesselDao labVesselDao;
    @Inject
    private MercurySampleDao mercurySampleDao;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {

        if (labBatchId == null) {
            stage = Stage.CHOOSING;
        } else {
            labBatch = labBatchDao.findById(LabBatch.class, labBatchId);
            if (labBatch == null) {
                stage = Stage.CHOOSING;
            } else {
                stage = Stage.EDITING;
            }
        }

        populateBatchList();
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(TOGGLE_STATUS_ACTION)
    public Resolution toggleBatchStatus() {

        if (labBatchId == null) {
            addGlobalValidationError("No batch selected.");
        } else {
            labBatch = labBatchDao.findById(LabBatch.class, labBatchId);
            // A little sanity test and some defense
            if (labBatch != null && labBatch.getLabBatchType() == LabBatch.LabBatchType.SRS) {
                if (labBatch.getActive()) {
                    labBatch.setActive(false);
                    addMessage("SRS batch " + labBatch.getBatchName() + " closed.");
                } else {
                    labBatch.setActive(true);
                    addMessage("SRS batch " + labBatch.getBatchName() + " re-opened.");
                }
                labBatchDao.flush();
            }
        }

        stage = Stage.CHOOSING;
        populateBatchList();
        return new ForwardResolution(VIEW_PAGE);
    }


    @HandlesEvent(SAVE_ACTION)
    public Resolution saveNew() {
        if (batchName == null || batchName.trim().isEmpty()) {
            addValidationError("batchName", "Batch name required.");
            stage = Stage.CHOOSING;
            return new ForwardResolution(VIEW_PAGE);
        }

        LabBatch batch = labBatchDao.findByName(batchName.trim());
        if( batch != null ) {
            addValidationError("batchName", "A batch already exists named {1}.", batchName);
            stage = Stage.CHOOSING;
            return new ForwardResolution(VIEW_PAGE);
        }

        labBatch = new LabBatch(batchName.trim(), Collections.emptySet(), LabBatch.LabBatchType.SRS);
        labBatchDao.persist(labBatch);
        stage = Stage.EDITING;
        populateBatchList();

        return new ForwardResolution(VIEW_PAGE);
    }

    /* ********* START SECTION TO ADD VESSELS TO SRS BATCH ******** */
    /**
     * Sets up bean for add events
     */
    @ValidationMethod(on = {ADD_SAMPLES_ACTION,ADD_BARCODES_ACTION,REMOVE_ACTION})
    public void validateAdd() {
        stage = Stage.EDITING;
        if (labBatchId == null) {
            addGlobalValidationError("No batch selected");
            stage = Stage.CHOOSING;
            return;
        }
        if (inputValues == null || inputValues.length == 0) {
            addValidationError("inputValues", "No values to add");
        }
        labBatch = labBatchDao.findById(LabBatch.class, labBatchId);
    }

    @HandlesEvent(ADD_SAMPLES_ACTION)
    public Resolution addSamples() {
        persistVessels(ADD_SAMPLES_ACTION);
        populateBatchList();
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(ADD_BARCODES_ACTION)
    public Resolution addVessels() {
        persistVessels(ADD_BARCODES_ACTION);
        populateBatchList();
        return new ForwardResolution(VIEW_PAGE);
    }

    private void persistVessels( String action ) {
        int count = 0;
        Set<LabVessel> vesselsToAdd = new HashSet<>();
        Set<LabVessel> batchVessels = labBatch.getNonReworkStartingLabVessels();
        for(String val : inputValues) {
            if (action.equals(ADD_BARCODES_ACTION)) {
                LabVessel vessel = labVesselDao.findByIdentifier(val);
                if (vessel == null) {
                    addGlobalValidationError(String.format("No vessel found for %s", val));
                    continue;
                } else if (batchVessels.contains(vessel)) {
                    addGlobalValidationError(String.format("Batch already contains vessel %s", val));
                    continue;
                } else {
                    vesselsToAdd.add(vessel);
                    count++;
                }
            } else {
                // Assume ADD_SAMPLES_ACTION
                MercurySample mercurySample = mercurySampleDao.findBySampleKey(val);
                if (mercurySample == null) {
                    addGlobalValidationError(String.format("No sample found for %s", val));
                    continue;
                } else {
                    for (LabVessel vessel : mercurySample.getLabVessel()) {
                        if (batchVessels.contains(vessel)) {
                            addGlobalValidationError(String.format("Batch already contains sample %s", val));
                            continue;
                        } else {
                            vesselsToAdd.add(vessel);
                            count++;

                        }
                    }
                }
            }
        }
        labBatch.addLabVessels(vesselsToAdd);

        if( count == inputValues.length ) {
            addMessage(String.format("Added %d %s to batch", count, action.equals(ADD_BARCODES_ACTION)?"vessels":"samples"));
        } else if( count > 0 ){
            addMessage(String.format("Added %d %s out of %d IDs supplied to batch", count, action.equals(ADD_BARCODES_ACTION)?"vessels":"samples", inputValues.length));
        }
        labVesselDao.flush();
    }

    public void setInputValues(String values ) {
        // Split on whitespace
        if( values != null ) {
            inputValues = values.trim().split("\\s+");
        } else {
            inputValues = new String[]{};
        }
    }
    /* ********* END SECTION TO ADD VESSELS TO SRS BATCH ******** */

    /**
     * Remove a vessel or group of vessels from batch
     */
    @HandlesEvent(REMOVE_ACTION)
    public Resolution removeVessel(){
        Set<LabVessel> vesselsToRemove = new HashSet<>();
        for(String val : inputValues) {

            LabVessel vessel = labVesselDao.findByIdentifier(val);
            if (vessel == null) {
                // Should never happen
                addGlobalValidationError(String.format("No vessel found for %s", val));
                continue;
            }
            vesselsToRemove.add(vessel);
        }
        if( vesselsToRemove.size() > 0 ) {
            Set<LabBatchStartingVessel> batchStartingVessels = labBatch.getLabBatchStartingVessels();
            Map<LabVessel,LabBatchStartingVessel> startingVesselMap = new HashMap<>();
            for( LabBatchStartingVessel batchStartingVessel : batchStartingVessels ) {
                startingVesselMap.put(batchStartingVessel.getLabVessel(), batchStartingVessel);
            }

            for( LabVessel vesselToRemove : vesselsToRemove ) {
                if( startingVesselMap.containsKey(vesselToRemove)) {
                    batchStartingVessels.remove(startingVesselMap.get(vesselToRemove));
                    addMessage(String.format("Removed vessel %s from batch", vesselToRemove.getLabel()));
                } else {
                    addGlobalValidationError(String.format("Batch does not contain vessel %s", vesselToRemove.getLabel()));
                }
            }
            labVesselDao.flush();
        }

        stage = Stage.EDITING;
        populateBatchList();
        return new ForwardResolution(VIEW_PAGE);
    }

    private void populateBatchList() {
        batchSelectionList = new ArrayList<>();
        // TODO: JMS Add option for inactive batches
        for (LabBatch batch : labBatchDao.findByType(LabBatch.LabBatchType.SRS)) {
            PickWorkspaceActionBean.BatchSelectionData batchData = new PickWorkspaceActionBean.BatchSelectionData(batch.getLabBatchId(), batch.getBatchName(), false, false);
            batchData.setActive(batch.getActive());
            batchSelectionList.add(batchData);
        }
    }

    public String getBatchName() {
        return labBatch != null ? labBatch.getBatchName() : batchName;
    }

    public void setBatchName(String batchName) {
        this.batchName = batchName;
    }

    public void setBatchId(Long batchId) {
        this.labBatchId = batchId;
    }

    public Long getBatchId() {
        this.labBatchId = labBatch==null?null:labBatch.getLabBatchId();
        return labBatchId;
    }

    public LabBatch getLabBatch() {
        return labBatch;
    }

    public Stage getStage() {
        return stage;
    }

    public List<PickWorkspaceActionBean.BatchSelectionData> getBatchSelectionList() {
        return batchSelectionList;
    }

    // For Testing
    public void setLabBatchDao(LabBatchDao labBatchDao) {
        this.labBatchDao = labBatchDao;
    }

    /**
     * Avoid having to use jstl to display or hide batch selection area
     */
    public String getDisplayAttribChoosing() {
        return Stage.CHOOSING.equals(stage) ? "block" : "none";
    }

    /**
     * Avoid having to use jstl to display or hide batch selection area
     */
    public String getDisplayAttribEditing() {
        return Stage.EDITING.equals(stage) ? "block" : "none";
    }

    /**
     * Convenience method for flagging visibility of inputs
     */
    public boolean getIsBatchActive() {
        return labBatch != null && labBatch.getActive();
    }

}
