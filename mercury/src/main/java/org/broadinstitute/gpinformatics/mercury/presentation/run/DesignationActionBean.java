package org.broadinstitute.gpinformatics.mercury.presentation.run;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private List<LcsetAssignmentDto> tubeLcsetAssignments = new ArrayList<>();
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
        if (StringUtils.isNotBlank(lcsetsBarcodes)) {
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
        utils.makeDtosFromDesignations(designationTubeEjb.existingDesignations(statusesToShow));
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
        for (LcsetAssignmentDto assignmentDto : tubeLcsetAssignments) {
            if (StringUtils.isNotBlank(assignmentDto.getSelectedLcsetName())) {
                LabVessel tube = barcodedTubeDao.findByBarcode(assignmentDto.getBarcode());
                LabBatch lcset = labBatchDao.findByName(assignmentDto.getSelectedLcsetName());
                loadingTubeLcset.put(tube, lcset);
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
        if (StringUtils.isNotBlank(lcsetsBarcodes)) {
            // Keeps only the decimal digits, ascii letters, hyphen, and space delimiters.
            String[] tokens = lcsetsBarcodes.replaceAll("[^\\p{Nd}\\p{Ll}\\p{Lu}\\p{Pd}]", " ").
                    replaceAll("[^\\x20-\\x7E]", "").toUpperCase().split(" ");
            for (String token : tokens) {
                if (StringUtils.isNotBlank(token)) {
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
     * @param loadingTubeLcset  Map of barcoded tube and its lcset.
     */
    private void makeDtos(@Nonnull Map<LabVessel, LabBatch> loadingTubeLcset) {
        Date now = new Date();
        Map<LabVessel, BucketEntry> batchStartingVesselToBucketEntry = new HashMap<>();
        Multimap<LabVessel, LabEvent> loadingTubeToLabEvent = HashMultimap.create();
        Multimap<LabVessel, BucketEntry> loadingTubeToBucketEntry = HashMultimap.create();
        Multimap<LabVessel, LabVessel> loadingTubeToUnbucketedBatchStartingVessel = HashMultimap.create();
        boolean foundOutOfDateEvents = false;

        // Iterates on the barcoded loading tubes specified by the user.
        for (LabVessel barcodedTube : loadingTubeLcset.keySet()) {
            for (LabEvent labEvent : barcodedTube.getInPlaceAndTransferToEvents()) {
                // Only uses loading tube events in the specified date range.
                if (labEvent.getLabEventType() == LabEventType.DENATURE_TRANSFER ||
                    labEvent.getLabEventType() == LabEventType.NORMALIZATION_TRANSFER ||
                    labEvent.getLabEventType() == LabEventType.POOLING_TRANSFER) {
                    if (isInDateRange(labEvent)) {
                        loadingTubeToLabEvent.put(barcodedTube, labEvent);
                    } else {
                        foundOutOfDateEvents = true;
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(loadingTubeToLabEvent.get(barcodedTube))) {
                // The single LCSET is already known.
                LabBatch lcset = loadingTubeLcset.get(barcodedTube);
                for (SampleInstanceV2 sampleInstance : barcodedTube.getSampleInstancesV2()) {
                    // Finds bucket entries.
                    for (BucketEntry bucketEntry : sampleInstance.getAllBucketEntries()) {
                        if (lcset.equals(bucketEntry.getLabBatch())) {
                            loadingTubeToBucketEntry.put(barcodedTube, bucketEntry);
                            batchStartingVesselToBucketEntry.put(bucketEntry.getLabVessel(), bucketEntry);
                        }
                    }
                    // Finds starting vessels.
                    Set<LabVessel> startingVessels = new HashSet<>();
                    for (LabBatchStartingVessel startingVessel : sampleInstance
                            .getAllBatchVessels(lcset.getLabBatchType())) {
                        if (lcset.equals(startingVessel.getLabBatch())) {
                            startingVessels.add(startingVessel.getLabVessel());
                        }
                    }
                    // A positive control has a batch starting vessel but no bucket entry.
                    if (CollectionUtils.isEmpty(loadingTubeToBucketEntry.get(barcodedTube))) {
                        loadingTubeToUnbucketedBatchStartingVessel.putAll(barcodedTube, startingVessels);
                    }
                }
            } else {
                addMessage("Tube " + barcodedTube.getLabel() + " has no denature, norm, or pooling event.");
            }
        }

        // Iterates on the LCSETs specified by the user.
        for (LabBatch targetLcset : loadLcsets) {
            for (BucketEntry bucketEntry : targetLcset.getBucketEntries()) {
                batchStartingVesselToBucketEntry.put(bucketEntry.getLabVessel(), bucketEntry);
            }
            // Iterates on the starting batch vessels to find loading tubes, and links them to a bucket entry.
            for (LabVessel startingBatchVessel : targetLcset.getStartingBatchLabVessels()) {
                BucketEntry bucketEntry = batchStartingVesselToBucketEntry.get(startingBatchVessel);
                Map<LabEvent, Set<LabVessel>> loadingEventsAndVessels = new HashMap<>(
                        startingBatchVessel.findVesselsForLabEventType(selectedEventType, true, DESCENDANTS));

                for (LabEvent targetEvent : loadingEventsAndVessels.keySet()) {
                    // Only uses loading tube events in the specified date range.
                    if (isInDateRange(targetEvent)) {
                        for (LabVessel loadingTube : loadingEventsAndVessels.get(targetEvent)) {
                            Set<LabBatch> bestLcsets = findBestLcset(Collections.singleton(targetLcset), loadingTube);
                            if (CollectionUtils.isNotEmpty(bestLcsets)) {
                                loadingTubeLcset.put(loadingTube, bestLcsets.iterator().next());
                                loadingTubeToLabEvent.put(loadingTube, targetEvent);
                                if (bucketEntry != null) {
                                    loadingTubeToBucketEntry.put(loadingTube, bucketEntry);
                                } else {
                                    // A positive control has a batch starting vessel but no bucket entry.
                                    loadingTubeToUnbucketedBatchStartingVessel.put(loadingTube, startingBatchVessel);
                                }
                            }
                        }
                    } else {
                        foundOutOfDateEvents = true;
                    }
                }
            }
        }

        // Makes the UI table row dto for each loading tube.
        for (LabVessel loadingTube : loadingTubeToLabEvent.keySet()) {
            Multimap<String, String> productToStartingVessel = HashMultimap.create();
            String regulatoryDesignation = null;
            int numberSamples = 0;
            Integer readLength = null;

            // Gets product, regulatory designation, and number of samples from the bucket entries.
            for (BucketEntry bucketEntry : loadingTubeToBucketEntry.get(loadingTube)) {
                Product product = bucketEntry.getProductOrder().getProduct();
                String productName = product != null ? bucketEntry.getProductOrder().getProduct().getProductName() :
                        "[No product for " + bucketEntry.getProductOrder().getJiraTicketKey() + "]";
                // todo emp get read length override when it exists in the ProductOrder.
                if (readLength == null && product != null && product.getReadLength() != null) {
                    readLength = product.getReadLength();
                }
                productToStartingVessel.put(productName, bucketEntry.getLabVessel().getLabel());
                // Sets the regulatory designation to Clinical, Research, or mixed.
                if (bucketEntry.getProductOrder().getResearchProject().getRegulatoryDesignation().
                        isClinical()) {
                    if (regulatoryDesignation == null) {
                        regulatoryDesignation = DesignationUtils.CLINICAL;
                    } else if (!regulatoryDesignation.equals(DesignationUtils.CLINICAL)) {
                        regulatoryDesignation = DesignationUtils.MIXED;
                    }
                } else {
                    if (regulatoryDesignation == null) {
                        regulatoryDesignation = DesignationUtils.RESEARCH;
                    } else if (!regulatoryDesignation.equals(DesignationUtils.RESEARCH)) {
                        regulatoryDesignation = DesignationUtils.MIXED;
                    }
                }
                numberSamples += bucketEntry.getLabVessel().getSampleNames().size();
            }

            // Adds in the unbucketed positive controls.
            for (LabVessel startingVessel : loadingTubeToUnbucketedBatchStartingVessel.get(loadingTube)) {
                productToStartingVessel.put(CONTROLS, startingVessel.getLabel());
                numberSamples += startingVessel.getSampleNames().size();
            }

            // For each loading tube, each product will show a corresponding list of starting vessels.
            List<String> productNames = new ArrayList<>(productToStartingVessel.keySet());
            Collections.sort(productNames);
            List<String> startingVesselList = new ArrayList<>();
            for (String productName : productNames) {
                List<String> startingVessels = new ArrayList<>(productToStartingVessel.get(productName));
                Collections.sort(startingVessels);
                startingVesselList.add((productName.equals(CONTROLS) ?
                        productName : "Bucketed tubes for " + productName) + ": " +
                                       StringUtils.join(startingVessels, ", "));
            }
            LabBatch primaryLcset = loadingTubeLcset.get(loadingTube);

            FlowcellDesignation.IndexType indexType = FlowcellDesignation.IndexType.DUAL;
            Integer numberCycles = readLength != null ? indexType.getIndexSize() + readLength : null;
            Integer numberLanes = null;
            BigDecimal loadingConc = BigDecimal.ZERO;
            Boolean poolTest = false;

            DesignationDto newDto = new DesignationDto(loadingTube, loadingTubeToLabEvent.get(loadingTube),
                    primaryLcset.getJiraTicket().getBrowserUrl(), primaryLcset,
                    productNames, StringUtils.join(startingVesselList, "\n"),
                    regulatoryDesignation, numberSamples, null, FlowcellDesignation.IndexType.DUAL,
                    numberCycles, numberLanes, readLength, loadingConc, poolTest, now,
                    FlowcellDesignation.Status.UNSAVED);

            dtos.add(newDto);
        }
        if (foundOutOfDateEvents) {
            addMessage("Excluded some loading tube " + selectedEventType + " events not in the date range.");
        }
    }

    /**
     * Finds the best lcset for barcoded tubes.
     *
     * @return map of tube to its lcset when a single lcset is found. The tubes with multiple
     * lcsets are put in tubeLcsetAssigntments.
     */
    private Map<LabVessel, LabBatch> linkLcsetToSpecifiedTubes() {
        tubeLcsetAssignments.clear();
        Map<LabVessel, LabBatch> loadingTubeLcset = new HashMap<>();
        boolean foundOutOfDateEvents = false;

        for (LabVessel targetTube : loadTubes) {
            for (LabEvent targetEvent : targetTube.getInPlaceAndTransferToEvents()) {
                // Only uses loading tube events in the specified date range.
                if (isInDateRange(targetEvent)) {
                    // Attempts to find the single lcset for the tube. Multiple lcsets will need to be
                    // resolved by the user.
                    Set<LabBatch> lcsets = findBestLcset(null, targetTube);
                    if (CollectionUtils.isNotEmpty(lcsets)) {
                        if (lcsets.size() == 1) {
                            loadingTubeLcset.put(targetTube, lcsets.iterator().next());
                        } else {
                            // Tube-lcset combinations to be resolved by the user.
                            addTubeLcsetAssignment(targetTube, lcsets);
                        }
                    }
                } else {
                    foundOutOfDateEvents = true;
                }
            }
        }
        LcsetAssignmentDto.sort(tubeLcsetAssignments);
        if (foundOutOfDateEvents) {
            addMessage("Excluded some loading tube " + selectedEventType + " events not in the date range.");
        }
        return loadingTubeLcset;
    }

    /** Finds the best lcset(s) for the loading tube. */
    private Set<LabBatch> findBestLcset(Set<LabBatch> targetLcsets, LabVessel loadingTube) {
        Set<LabBatch> bestLcsets = new HashSet<>();
        Set<LabBatch> allLcsets = new HashSet<>();
        for (SampleInstanceV2 sampleInstance : loadingTube.getSampleInstancesV2()) {
            LabBatch singleBatch = sampleInstance.getSingleBatch();
            if (singleBatch != null) {
                bestLcsets.add(singleBatch);
            } else {
                // Multiple lcsets can legitimately exist in one loading tube (e.g. norm tube 0185941254).
                allLcsets.addAll(sampleInstance.getAllWorkflowBatches());
            }
        }
        if (bestLcsets.isEmpty()) {
            bestLcsets.addAll(allLcsets);
        }
        // Only keeps the loading tube if it's in one of the target lcsets, if any were given.
        if (CollectionUtils.isNotEmpty(targetLcsets)) {
            bestLcsets.retainAll(targetLcsets);
        }
        return bestLcsets;
    }


    private boolean isInDateRange(LabEvent labEvent) {
        return getDateRange().getStartTime().before(labEvent.getEventDate()) &&
               getDateRange().getEndTime().after(labEvent.getEventDate());
    }

    /** Adds a tube-lcset assignment dto or merges into an existing one. Dtos should be unique on tube barcode. */
    private void addTubeLcsetAssignment(LabVessel tube, Set<LabBatch> lcsets) {
        boolean tubeIsPresent = false;
        for (LcsetAssignmentDto dto : tubeLcsetAssignments) {
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
            tubeLcsetAssignments.add(new LcsetAssignmentDto(tube, lcsets));
        }
    }

    /**
     * Represents a UI row when prompting user to associate an LCSET with a loading tube.
     */
    public static class LcsetAssignmentDto {
        private String barcode;
        private Date tubeDate;
        private List<Pair<String, String>> lcsetNameUrls = new ArrayList<>();
        private String selectedLcsetName = "";

        public LcsetAssignmentDto() {
        }

        public LcsetAssignmentDto(LabVessel targetTube, Set<LabBatch> lcsets) {
            barcode = targetTube.getLabel();
            tubeDate = targetTube.getCreatedOn();
            for (LabBatch lcset : lcsets) {
                lcsetNameUrls.add(Pair.of(lcset.getBatchName(), lcset.getJiraTicket().getBrowserUrl()));
            }
        }

        public String getBarcode() {
            return barcode;
        }

        public void setBarcode(String barcode) {
            this.barcode = barcode;
        }

        public Date getTubeDate() {
            return tubeDate;
        }

        public void setTubeDate(Date tubeDate) {
            this.tubeDate = tubeDate;
        }

        public List<Pair<String, String>> getLcsetNameUrls() {
            return lcsetNameUrls;
        }

        public String getSelectedLcsetName() {
            return selectedLcsetName;
        }

        public void setSelectedLcsetName(String selectedLcsetName) {
            this.selectedLcsetName = selectedLcsetName;
        }

        /** Sorts the assignment dtos and their lcsets. */
        public static void sort(List<LcsetAssignmentDto> assignmentDtos) {
            // Sorts the lcset list on each dto.
            for (LcsetAssignmentDto dto : assignmentDtos) {
                Collections.sort(dto.lcsetNameUrls, new Comparator<Pair<String, String>>() {
                    @Override
                    public int compare(Pair<String, String> o1, Pair<String, String> o2) {
                        return o1.getLeft().compareTo(o2.getLeft());
                    }
                });
            }
            // Sorts the dtos by barcode.
            Collections.sort(assignmentDtos, new Comparator<LcsetAssignmentDto>() {
                @Override
                public int compare(LcsetAssignmentDto o1, LcsetAssignmentDto o2) {
                    return o1.getBarcode().compareTo(o2.getBarcode());
                }
            });
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

    public List<LcsetAssignmentDto> getTubeLcsetAssignments() {
        return tubeLcsetAssignments;
    }

    public void setTubeLcsetAssignments(List<LcsetAssignmentDto> tubeLcsetAssignments) {
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

