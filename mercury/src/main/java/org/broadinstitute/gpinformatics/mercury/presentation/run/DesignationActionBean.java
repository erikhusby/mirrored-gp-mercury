package org.broadinstitute.gpinformatics.mercury.presentation.run;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateRangeSelector;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FlowcellDesignationEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@UrlBinding("/run/FlowcellDesignation.action")
public class DesignationActionBean extends CoreActionBean implements DesignationUtils.Caller {
    private static final String VIEW_PAGE = "/run/designation_create.jsp";
    private static final String TUBE_LCSET_PAGE = "/run/designation_lcset_select.jsp";

    public static final String LOAD_DENATURE_ACTION = "loadDenature";
    public static final String LOAD_NORM_ACTION = "loadNorm";
    public static final String LOAD_POOLNORM_ACTION = "loadPoolNorm";
    public static final String SUBMIT_TUBE_LCSET_ACTION = "submitTubeLcsets";
    public static final String SET_MULTIPLE_ACTION = "setMultiple";
    public static final String PENDING_ACTION = "pending";

    public static final String CONTROLS = "Controls";

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private BarcodedTubeDao barcodedTubeDao;

    @Inject
    private FlowcellDesignationEjb designationTubeEjb;

    private String lcsetsBarcodes;
    private LabEventType selectedEventType;
    private List<DesignationDto> dtos = new ArrayList<>();
    private DesignationDto multiEdit = new DesignationDto();
    private Set<LabBatch> loadLcsets = new HashSet<>();
    private Set<LabVessel> loadTubes = new HashSet<>();
    private List<DesignationUtils.LcsetAssignmentDto> tubeLcsetAssignments = new ArrayList<>();
    private boolean showProcessed;
    private boolean showAbandoned;
    private boolean showQueued = true;
    private boolean append;
    private DesignationUtils utils = new DesignationUtils(this);

    private static final EnumSet DESCENDANTS = EnumSet.of(TransferTraverserCriteria.TraversalDirection.Descendants);

    {
        GregorianCalendar startDate = new GregorianCalendar();
        startDate.add(Calendar.MONTH, -2);
        setDateRange(new DateRangeSelector(DateRangeSelector.CREATE_CUSTOM));
        setDateRangeStart(startDate.getTime());
        setDateRangeEnd(new Date());
    }

    /**
     * Displays the page with the designation tubes, if any.
     */
    @HandlesEvent(VIEW_ACTION)
    @DefaultHandler
    public Resolution view() {
        // Filters out the unwanted dto's by status.
        for (Iterator<DesignationDto> iter = dtos.iterator(); iter.hasNext(); ) {
            DesignationDto dto = iter.next();
            if (!showAbandoned && dto.getStatus() == FlowcellDesignation.Status.ABANDONED ||
                !showProcessed && dto.getStatus() == FlowcellDesignation.Status.IN_FCT ||
                !showQueued && dto.getStatus() == FlowcellDesignation.Status.QUEUED) {
                iter.remove();
                continue;
            }
            dto.setSelected(false);
        }
        Set<DesignationDto> uniqueDtos = new HashSet<>(dtos);
        dtos.clear();
        dtos.addAll(uniqueDtos);
        return new ForwardResolution(VIEW_PAGE);
    }

    /**
     * Displays the list of denature tubes to select from.
     */
    @HandlesEvent(LOAD_DENATURE_ACTION)
    public Resolution loadDenature() {
        selectedEventType = LabEventType.DENATURE_TRANSFER;
        return loadAny();
    }

    /**
     * Displays the list of norm tubes to select from.
     */
    @HandlesEvent(LOAD_NORM_ACTION)
    public Resolution loadNorm() {
        selectedEventType = LabEventType.NORMALIZATION_TRANSFER;
        return loadAny();
    }

    /**
     * Displays the list of pooled norm tubes to select from.
     */
    @HandlesEvent(LOAD_POOLNORM_ACTION)
    public Resolution loadPoolNorm() {
        selectedEventType = LabEventType.POOLING_TRANSFER;
        return loadAny();
    }

    private Resolution loadAny() {
        clearValidationErrors();
        if (isNotBlank(lcsetsBarcodes)) {
            // Parses the user input lcsets or tube barcodes into loadLcsets and loadTubes.
            parseLcsetsBarcodes();
        } else {
            // When no user input lcset or barcodes, gets all undesignated loading tubes in the date range.
            loadTubes.addAll(designationTubeEjb.eligibleDesignations(getDateRange()));
        }
        // If there are any barcodes, finds their lcset(s). In case there are
        // multiple lcsets per tube, the tube-lcset combinations get put in
        // tubeLcsetSelection to be resolved by user in the TUBE_LCSET_PAGE.
        Map<LabVessel, LabBatch> loadingTubeLcset = new HashMap<>();
        if (CollectionUtils.isNotEmpty(loadTubes)) {
            loadingTubeLcset = linkLcsetToSpecifiedTubes();
            if (tubeLcsetAssignments.size() > 0) {
                return new ForwardResolution(TUBE_LCSET_PAGE);
            }
        }
        if (!append) {
            dtos.clear();
        }
        // At this point there should be a single lcset per loading tube.
        makeDtos(loadingTubeLcset);
        return view();
    }

    /**
     * Displays the list of saved but unqueued designations.
     */
    @HandlesEvent(PENDING_ACTION)
    public Resolution loadPendingDesignations() {
        clearValidationErrors();
        if (!append) {
            dtos.clear();
        }
        List <FlowcellDesignation.Status> statusesToShow = new ArrayList<FlowcellDesignation.Status>(){{
            if (getShowAbandoned()) {
                add(FlowcellDesignation.Status.ABANDONED);
            }
            if (getShowQueued()) {
                add(FlowcellDesignation.Status.QUEUED);
            }
            if (getShowProcessed()) {
                add(FlowcellDesignation.Status.IN_FCT);
            }
        }};
        MessageCollection messageCollection = new MessageCollection();
        tubeLcsetAssignments = utils.makeDtosFromDesignations(designationTubeEjb.existingDesignations(statusesToShow),
                messageCollection);
        if (tubeLcsetAssignments.size() > 0) {
            return new ForwardResolution(TUBE_LCSET_PAGE);
        }
        addMessages(messageCollection);
        return view();
    }

    /** Changes the selected UI rows. If the row is queued or abandoned the changes are persisted. */
    @HandlesEvent(SET_MULTIPLE_ACTION)
    public Resolution setMultiple() {
        utils.applyMultiEdit(DesignationUtils.TARGETABLE_STATUSES, designationTubeEjb);
        multiEdit = new DesignationDto();
        return view();
    }


    /**
     * This method is called after the user specifies the desired lcset
     * for each loading tube barcode that had mulitple lcsets.
     */
    @HandlesEvent(SUBMIT_TUBE_LCSET_ACTION)
    public Resolution submitTubeLcsets() {
        clearValidationErrors();
        parseLcsetsBarcodes();
        // Collects the user-specified lcset mappings.
        Map<LabVessel, LabBatch> loadingTubeLcset = new HashMap<>();
        for (DesignationUtils.LcsetAssignmentDto assignmentDto : tubeLcsetAssignments) {
            if (isNotBlank(assignmentDto.getSelectedLcsetName())) {
                LabVessel tube = barcodedTubeDao.findByBarcode(assignmentDto.getBarcode());
                LabBatch lcset = labBatchDao.findByName(assignmentDto.getSelectedLcsetName());
                loadingTubeLcset.put(tube, lcset);
            } else {
                addMessage(String.format(DesignationUtils.UNKNOWN_LCSET_MSG, assignmentDto.getDesignationId(),
                        assignmentDto.getBarcode()));
            }
        }
        // Removes tubes that did not get an lcset assignment.
        loadTubes.retainAll(loadingTubeLcset.keySet());
        // Resumes making the designation dto's to display.
        makeDtos(loadingTubeLcset);
        return view();
    }


    /**
     * Parses the lab batches and lab vessels from the user-specified lcset names and barcodes.
     */
    private void parseLcsetsBarcodes() {
        if (isNotBlank(lcsetsBarcodes)) {
            // Keeps only the decimal digits, ascii letters, hyphen, and space delimiters.
            String[] tokens = lcsetsBarcodes.replaceAll("[^\\p{Nd}\\p{Ll}\\p{Lu}\\p{Pd}]", " ").
                    replaceAll("[^\\x20-\\x7E]", "").toUpperCase().split(" ");
            for (String token : tokens) {
                if (isNotBlank(token)) {
                    BarcodedTube tube = barcodedTubeDao.findByBarcode(token);
                    if (tube != null) {
                        loadTubes.add(tube);
                    } else {
                        String lcsetName = "LCSET-" + token;
                        LabBatch batch = labBatchDao.findByBusinessKey(lcsetName);
                        if (batch != null) {
                            loadLcsets.add(batch);
                        } else {
                            addValidationError("lcsetsBarcodes", "Could not find barcode or lcset for " + token);
                        }
                    }
                }
            }
        }
    }

    /**
     * Populates the UI's table of designation tubes from LCSETs and/or loading tube barcodes.
     *
     * @param loadingTubeToLcset  Map of barcoded tube and its lcset.
     */
    private void makeDtos(@Nonnull Map<LabVessel, LabBatch> loadingTubeToLcset) {
        Multimap<LabVessel, LabEvent> loadingTubeToLabEvent = HashMultimap.create();
        Multimap<LabVessel, BucketEntry> loadingTubeToBucketEntry = HashMultimap.create();
        Multimap<LabVessel, LabVessel> loadingTubeToControl = HashMultimap.create();
        boolean foundOutOfDateEvents = false;

        // Iterates on the loading tubes specified by barcode by the user.
        for (LabVessel loadingTube : loadingTubeToLcset.keySet()) {
            for (LabEvent labEvent : loadingTube.getInPlaceAndTransferToEvents()) {
                if (labEvent.getLabEventType() == LabEventType.DENATURE_TRANSFER ||
                    labEvent.getLabEventType() == LabEventType.NORMALIZATION_TRANSFER ||
                    labEvent.getLabEventType() == LabEventType.POOLING_TRANSFER) {

                    loadingTubeToLabEvent.put(loadingTube, labEvent);
                }
            }

            if (CollectionUtils.isNotEmpty(loadingTubeToLabEvent.get(loadingTube))) {
                // The tube's single LCSET has already been determined by traversal or been chosen by the user.
                LabBatch lcset = loadingTubeToLcset.get(loadingTube);
                Set<LabVessel> startingVessels = new HashSet<>();

                for (SampleInstanceV2 sampleInstance : loadingTube.getSampleInstancesV2()) {
                    if (sampleInstance.getSingleBatch() != null &&
                        sampleInstance.getSingleBatch().equals(lcset) ||
                        sampleInstance.getSingleBatch() == null &&
                        sampleInstance.getAllWorkflowBatches().contains(lcset)) {

                        // Finds starting vessels for the loading tube in this lcset.
                        for (LabBatchStartingVessel startingVessel :
                                sampleInstance.getAllBatchVessels(lcset.getLabBatchType())) {
                            if (lcset.equals(startingVessel.getLabBatch())) {
                                startingVessels.add(startingVessel.getLabVessel());
                            }
                        }

                        // Finds bucket entries for this lcset.
                        for (BucketEntry bucketEntry : sampleInstance.getAllBucketEntries()) {
                            if (lcset.equals(bucketEntry.getLabBatch())) {
                                loadingTubeToBucketEntry.put(loadingTube, bucketEntry);
                            }
                        }
                    }
                }

                // A starting vessel with no bucket entry is a positive control.
                Set<LabVessel> controls = new HashSet<>(startingVessels);
                for (BucketEntry bucketEntry : loadingTubeToBucketEntry.get(loadingTube)) {
                    controls.remove(bucketEntry.getLabVessel());
                }
                loadingTubeToControl.putAll(loadingTube, controls);

            } else {
                addMessage("Tube " + loadingTube.getLabel() + " has no denature, norm, or pooling event.");
            }
        }

        // Iterates on the LCSETs specified by the user.
        for (LabBatch targetLcset : loadLcsets) {
            // Finds the loading tubes for each of the lcset's starting batch vessels. When a starting
            // tube is reworked it may have multiple loading tubes. Each of these loading tube will
            // need to be checked for the best lcset(s), and only be used if the target lcset is among
            // them. Typically there is only one best lcset but a pooled tube may be in two equivalently
            // good lcsets so that case must be handled correctly.
            for (LabVessel startingBatchVessel : targetLcset.getStartingBatchLabVessels()) {
                Map<LabEvent, Set<LabVessel>> loadingEventsAndVessels =
                        startingBatchVessel.findVesselsForLabEventType(selectedEventType, true, DESCENDANTS);

                for (LabEvent targetEvent : loadingEventsAndVessels.keySet()) {
                    for (LabVessel loadingTube : loadingEventsAndVessels.get(targetEvent)) {
                        // Only processes a loading tube for this lcset once.
                        if (!targetLcset.equals(loadingTubeToLcset.get(loadingTube))) {
                            Pair<Set<LabBatch>, Set<SampleInstanceV2>> loadingTubeTraversal =
                                    DesignationUtils.findBestLcsets(loadingTube);
                            Set<LabBatch> loadingTubeLcsets = loadingTubeTraversal.getLeft();
                            Set<SampleInstanceV2> loadingTubeSampleInstances = loadingTubeTraversal.getRight();

                            if (loadingTubeLcsets.contains(targetLcset)) {
                                loadingTubeToLcset.put(loadingTube, targetLcset);
                                loadingTubeToLabEvent.put(loadingTube, targetEvent);

                                for (SampleInstanceV2 sampleInstance : loadingTubeSampleInstances) {
                                    if (sampleInstance.getSingleBatch() != null &&
                                        sampleInstance.getSingleBatch().equals(targetLcset) ||
                                        sampleInstance.getSingleBatch() == null &&
                                        sampleInstance.getAllWorkflowBatches().contains(targetLcset)) {

                                        for (BucketEntry bucketEntry : sampleInstance.getAllBucketEntries()) {
                                            if (targetLcset.equals(bucketEntry.getLabBatch())) {
                                                loadingTubeToBucketEntry.put(loadingTube, bucketEntry);
                                            }
                                        }

                                        // A starting vessel with no bucket entry is a positive control.
                                        if (sampleInstance.getAllBucketEntries().isEmpty()) {
                                            for (LabBatchStartingVessel vessel : sampleInstance.getAllBatchVessels()) {
                                                loadingTubeToControl.put(loadingTube, vessel.getLabVessel());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Makes the UI table row dto for each loading tube.
        for (LabVessel loadingTube : loadingTubeToLabEvent.keySet()) {
            LabBatch lcset = loadingTubeToLcset.get(loadingTube);

            DesignationDto newDto = DesignationUtils.makeDesignationDto(loadingTube,
                    loadingTubeToLcset.get(loadingTube), loadingTubeToLabEvent.get(loadingTube),
                    loadingTubeToBucketEntry.get(loadingTube), loadingTubeToControl.get(loadingTube), null);

            // Persists the lcset choice made by the user.
            for (DesignationUtils.LcsetAssignmentDto assignmentDto : tubeLcsetAssignments) {
                if (assignmentDto.getBarcode().equals(loadingTube.getLabel()) &&
                    lcset.getBatchName().equals(assignmentDto.getSelectedLcsetName())) {
                    newDto.setChosenLcset(lcset.getBatchName());
                }
            }
            dtos.add(newDto);
        }
        if (foundOutOfDateEvents) {
            addMessage("Excluded some loading tube " + selectedEventType + " events not in the date range.");
        }
    }


    /**
     * Finds the single lcset for barcoded tubes, possibly relying on user to select one.
     *
     * @return map of tube to its lcset when a single lcset is found. The tubes with multiple
     * lcsets are put in tubeLcsetAssigntments.
     */
    private Map<LabVessel, LabBatch> linkLcsetToSpecifiedTubes() {
        tubeLcsetAssignments.clear();
        Map<LabVessel, LabBatch> loadingTubeLcset = new HashMap<>();

        for (LabVessel targetTube : loadTubes) {
            // Attempts to find the single lcset for the tube. Multiple lcsets will need to be
            // resolved by the user.
            Set<LabBatch> lcsets = DesignationUtils.findBestLcsets(targetTube).getLeft();
            if (CollectionUtils.isNotEmpty(lcsets)) {
                if (lcsets.size() == 1) {
                    loadingTubeLcset.put(targetTube, lcsets.iterator().next());
                } else {
                    // Tube-lcset combinations to be resolved by the user.
                    addTubeLcsetAssignment(targetTube, lcsets);
                }
            }
        }
        DesignationUtils.LcsetAssignmentDto.sort(tubeLcsetAssignments);
        return loadingTubeLcset;
    }

    /** Adds a tube-lcset assignment dto or merges into an existing one. Dtos should be unique on tube barcode. */
    private void addTubeLcsetAssignment(LabVessel tube, Set<LabBatch> lcsets) {
        boolean tubeIsPresent = false;
        for (DesignationUtils.LcsetAssignmentDto dto : tubeLcsetAssignments) {
            if (dto.getBarcode().equals(tube.getLabel())) {
                tubeIsPresent = true;
                // Adds the lcset name if it's not already present.
                for (LabBatch lcset : lcsets) {
                    boolean lcsetIsPresent = false;
                    for (Pair<String, String> lcsetNameUrl : dto.getLcsetNameUrls()) {
                        if (lcsetNameUrl.getLeft().equals(lcset.getBatchName())) {
                            lcsetIsPresent = true;
                        }
                    }
                    if (!lcsetIsPresent) {
                        dto.getLcsetNameUrls().add(Pair.of(lcset.getBatchName(),
                                lcset.getJiraTicket().getBrowserUrl()));
                    }
                }
            }
        }
        if (!tubeIsPresent) {
            tubeLcsetAssignments.add(new DesignationUtils.LcsetAssignmentDto(tube, lcsets));
        }
    }

    public String getLcsetsBarcodes() {
        return lcsetsBarcodes;
    }

    public void setLcsetsBarcodes(String lcsetsBarcodes) {
        this.lcsetsBarcodes = lcsetsBarcodes;
    }

    public List<DesignationDto> getDtos() {
        return dtos;
    }

    public void setDtos(List<DesignationDto> dtos) {
        this.dtos = dtos;
    }

    public DesignationDto getMultiEdit() {
        return multiEdit;
    }

    public void setMultiEdit(DesignationDto multiEdit) {
        this.multiEdit = multiEdit;
    }

    public Set<LabBatch> getLoadLcsets() {
        return loadLcsets;
    }

    public void setLoadLcsets(Set<LabBatch> loadLcsets) {
        this.loadLcsets = loadLcsets;
    }

    public Set<LabVessel> getLoadTubes() {
        return loadTubes;
    }

    public void setLoadTubes(Set<LabVessel> loadTubes) {
        this.loadTubes = loadTubes;
    }

    public List<DesignationUtils.LcsetAssignmentDto> getTubeLcsetAssignments() {
        return tubeLcsetAssignments;
    }

    public void setTubeLcsetAssignments(List<DesignationUtils.LcsetAssignmentDto> tubeLcsetAssignments) {
        this.tubeLcsetAssignments = tubeLcsetAssignments;
    }

    public boolean getShowProcessed() {
        return showProcessed;
    }

    public void setShowProcessed(boolean showProcessed) {
        this.showProcessed = showProcessed;
    }

    public boolean getShowAbandoned() {
        return showAbandoned;
    }

    public void setShowAbandoned(boolean showAbandoned) {
        this.showAbandoned = showAbandoned;
    }

    public boolean getShowQueued() {
        return showQueued;
    }

    public void setShowQueued(boolean showQueued) {
        this.showQueued = showQueued;
    }

    public boolean isAppend() {
        return append;
    }

    public void setAppend(boolean append) {
        this.append = append;
    }

    public void setDateRangeStart(Date start) {
        getDateRange().setStart(start);
    }

    public void setDateRangeEnd(Date end) {
        getDateRange().setEnd(end);
    }

    public void setDateRangeSelector(int rangeSelector) {
        getDateRange().setRangeSelector(rangeSelector);
    }

    public DesignationUtils getUtils() {
        return utils;
    }
}

