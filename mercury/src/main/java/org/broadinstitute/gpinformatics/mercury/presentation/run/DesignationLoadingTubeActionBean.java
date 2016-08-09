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
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateRangeSelector;
import org.broadinstitute.gpinformatics.mercury.boundary.run.DesignationLoadingTubeEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.run.DesignationLoadingTube;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UrlBinding("/run/DesignationLoadingTube.action")
public class DesignationLoadingTubeActionBean extends CoreActionBean {
    private static final String VIEW_PAGE = "/run/designate_loading_tubes.jsp";
    private static final String TUBE_LCSET_PAGE = "/run/select_tube_lcsets.jsp";

    public static final String LOAD_DENATURE_ACTION = "loadDenature";
    public static final String LOAD_NORM_ACTION = "loadNorm";
    public static final String LOAD_POOLNORM_ACTION = "loadPoolNorm";
    public static final String SUBMIT_TUBE_LCSET_ACTION = "submitTubeLcsets";
    public static final String SET_MULTIPLE_ACTION = "setMultiple";
    public static final String PENDING_ACTION = "pending";

    public static final String CLINICAL = "Clinical";
    public static final String RESEARCH = "Research";
    public static final String MIXED = "Clinical and Research";
    public static final String CONTROLS = "Controls";

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private JiraService jiraService;

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private LabBatchEjb labBatchEjb;

    @Inject
    private BarcodedTubeDao barcodedTubeDao;

    @Inject
    private BucketEntryDao bucketEntryDao;

    @Inject
    private DesignationLoadingTubeEjb designationTubeEjb;

    private String lcsetsBarcodes;
    private LabEventType selectedEventType;
    private List<DesignationRowDto> rowDtos = new ArrayList<>();
    private DesignationRowDto multiEdit = new DesignationRowDto();
    private List<String> createdFcts = new ArrayList<>();
    private Set<LabBatch> loadLcsets = new HashSet<>();
    private Set<LabVessel> loadTubes = new HashSet<>();
    private List<TubeLcsetDto> tubeLcsetSelections = new ArrayList<>();
    private boolean showProcessed;
    private boolean showAbandoned;
    private boolean showQueued;
    private boolean append;

    private static final EnumSet DESCENDENTS = EnumSet.of(TransferTraverserCriteria.TraversalDirection.Descendants);

    {
        setDateRange(new DateRangeSelector(DateRangeSelector.ONE_MONTH));
    }

    /**
     * Displays the page with the designation tubes, if any.
     */
    @HandlesEvent(VIEW_ACTION)
    @DefaultHandler
    public Resolution view() {
        DesignationRowDto prev = null;
        Collections.sort(rowDtos, DesignationRowDto.BY_BARCODE_ID_DATE);
        for (Iterator<DesignationRowDto> iter = rowDtos.iterator(); iter.hasNext(); ) {
            DesignationRowDto dto = iter.next();
            if (!showAbandoned && dto.getStatus() == DesignationLoadingTube.Status.ABANDONED ||
                !showProcessed && dto.getStatus() == DesignationLoadingTube.Status.IN_FCT ||
                !showQueued && dto.getStatus() == DesignationLoadingTube.Status.QUEUED) {
                iter.remove();
                continue;
            }
            if (prev != null && prev.getBarcode().equals(dto.getBarcode())) {
                // List is sorted by barcode and id. If adjacent rows have the same barcode, either
                // remove the second row if it has the same designationId, or mark it as a repeat.
                if (prev.getDesignationId() == null && dto.getDesignationId() == null ||
                    prev.getDesignationId() != null && prev.getDesignationId().equals(dto.getDesignationId())) {
                    iter.remove();
                    continue;
                }
                if (prev != null && prev.getBarcode().equals(dto.getBarcode())) {
                    dto.setRepeatedBarcode(true);
                }
            }
            prev = dto;
        }
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
        // Map of barcoded tube to lcset(s).
        Multimap<LabVessel, LabBatch> loadingTubeToLcsets = HashMultimap.create();
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
        if (CollectionUtils.isNotEmpty(loadTubes)) {
            loadingTubeToLcsets = linkLcsetToSpecifiedTubes();
            if (tubeLcsetSelections.size() > 0) {
                return new ForwardResolution(TUBE_LCSET_PAGE);
            }
        }
        if (!append) {
            rowDtos.clear();
        }
        makeRowDtos(loadingTubeToLcsets);
        return view();
    }

    /**
     * Displays the list of saved but unqueued designations.
     */
    @HandlesEvent(PENDING_ACTION)
    public Resolution loadPendingDesignations() {
        clearValidationErrors();
        if (!append) {
            rowDtos.clear();
        }
        makeRowDtosFromDesignations(designationTubeEjb.existingDesignations(new ArrayList<DesignationLoadingTube.Status>(){{
            add(DesignationLoadingTube.Status.SAVED);
            if (getShowAbandoned()) {
                add(DesignationLoadingTube.Status.ABANDONED);
            }
            if (getShowQueued()) {
                add(DesignationLoadingTube.Status.QUEUED);
            }
            if (getShowProcessed()) {
                add(DesignationLoadingTube.Status.IN_FCT);
            }
        }}));
        return view();
    }

    /**
     * Changes the UI rows and persists them.
     */
    @HandlesEvent(SET_MULTIPLE_ACTION)
    public Resolution setMultiple() {
        for (Iterator<DesignationRowDto> iter = rowDtos.iterator(); iter.hasNext(); ) {
            DesignationRowDto dto = iter.next();
            if (dto.isSelected() && dto.getStatus() != DesignationLoadingTube.Status.IN_FCT) {
                if (multiEdit.getStatus() != null) {
                    if (dto.getStatus() == null && multiEdit.getStatus() == DesignationLoadingTube.Status.ABANDONED) {
                        // Abandoning an unsaved dto just removes it from the list, nothing gets persisted.
                        iter.remove();
                        continue;
                    } else {
                        dto.setStatus(multiEdit.getStatus());
                    }
                } else if (dto.getStatus() == DesignationLoadingTube.Status.UNSAVED) {
                    dto.setStatus(DesignationLoadingTube.Status.SAVED);
                }
                if (multiEdit.getPriority() != null) {
                    dto.setPriority(multiEdit.getPriority());
                }
                if (multiEdit.getSequencerModel() != null) {
                    dto.setSequencerModel(multiEdit.getSequencerModel());
                }
                if (multiEdit.getNumberLanes() != null) {
                    dto.setNumberLanes(multiEdit.getNumberLanes());
                }
                if (multiEdit.getLoadingConc() != null) {
                    dto.setLoadingConc(multiEdit.getLoadingConc());
                }
                if (multiEdit.getReadLength() != null) {
                    dto.setReadLength(multiEdit.getReadLength());
                }
                if (multiEdit.getIndexType() != null) {
                    dto.setIndexType(multiEdit.getIndexType());
                }
                if (multiEdit.getNumberCycles() != null) {
                    dto.setNumberCycles(multiEdit.getNumberCycles());
                }
                if (multiEdit.getPoolTest() != null) {
                    dto.setPoolTest(multiEdit.getPoolTest());
                }
            }
        }
        multiEdit = new DesignationRowDto();
        return update();
    }

    /**
     * Updates designation loading tube parameters.
     */
    @HandlesEvent(SAVE_ACTION)
    public Resolution update() {
        // Persists the selected rows. After the hibernate flush the designation id can be updated on the dto.
        for (Map.Entry<DesignationRowDto, DesignationLoadingTube> dtoAndTube :
                designationTubeEjb.update(rowDtos).entrySet()) {
            DesignationRowDto rowDto = dtoAndTube.getKey();
            DesignationLoadingTube designation = dtoAndTube.getValue();
            rowDto.setDesignationId(designation.getDesignationId());
            rowDto.setSelected(false);
        }
        return view();
    }

    /**
     * This method is called after the user specifies the correct lcset(s)
     * for loading tube barcodes that had mulitple lcsets.
     */
    @HandlesEvent(SUBMIT_TUBE_LCSET_ACTION)
    public Resolution submitTubeLcsets() {
        clearValidationErrors();
        parseLcsetsBarcodes();
        // Collects the user-specified lcset mappings.
        Multimap<LabVessel, LabBatch> loadingTubeToLcsets = HashMultimap.create();
        for (TubeLcsetDto dto : tubeLcsetSelections) {
            if (dto.isSelected()) {
                LabVessel tube = barcodedTubeDao.findByBarcode(dto.getBarcode());
                LabBatch lcset = labBatchDao.findByName(dto.getLcsetName());
                loadingTubeToLcsets.put(tube, lcset);
            }
        }
        // Removes tubes that did not get an lcset assignment.
        loadTubes.retainAll(loadingTubeToLcsets.keySet());
        // Resumes making the designation dto's to display.
        makeRowDtos(loadingTubeToLcsets);
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

    /** Returns the primary and additional lcsets. */
    private Pair<LabBatch, List<String>> sortLcsets(Collection<LabBatch> lcsets) {
        List<String> lcsetNames = new ArrayList<>();
        for (LabBatch lcset : lcsets) {
            lcsetNames.add(lcset.getBatchName());
        }
        Collections.sort(lcsetNames);
        // Puts the url on the latest lcset when there are multiple.
        String primaryLcsetName = lcsetNames.remove(lcsetNames.size() - 1);
        LabBatch primaryLcset = null;
        for (LabBatch lcset : lcsets) {
            if (primaryLcsetName.equals(lcset.getBatchName())) {
                primaryLcset = lcset;
            }
        }
        return Pair.of(primaryLcset, lcsetNames);
    }

    /** Makes dtos from existing designations. */
    private void makeRowDtosFromDesignations(Collection<DesignationLoadingTube> designationLoadingTubes) {
        for (DesignationLoadingTube designation : designationLoadingTubes) {

            Pair<LabBatch, List<String>> pair = sortLcsets(designation.getLcsets());
            LabBatch primaryLcset = pair.getLeft();
            List<String> additionalLcsets = pair.getRight();

            Set<String> productNames = new HashSet<>();
            for (Product product : designation.getProducts()) {
                productNames.add(product.getProductName());
            }

            DesignationRowDto designationDto = new DesignationRowDto(designation.getLoadingTube(),
                    Collections.singletonList(designation.getLoadingTubeEvent()),
                    primaryLcset.getJiraTicket().getBrowserUrl(), primaryLcset, additionalLcsets,
                    productNames, "", designation.isClinical() ? CLINICAL : RESEARCH,
                    designation.getNumberSamples(), designation.getSequencerModel(), designation.getIndexType(),
                    designation.getNumberCycles(), designation.getNumberLanes(), designation.getReadLength(),
                    designation.getLoadingConc(), designation.isPoolTest(), designation.getCreatedOn(),
                    designation.getStatus());

            designationDto.setDesignationId(designation.getDesignationId());
            designationDto.setPriority(designation.getPriority());

            rowDtos.add(designationDto);
        }
    }


    /**
     * Populates the UI's table of designation tubes from LCSETs and/or loading tube barcodes.
     *
     * @param loadingTubeToLcsets  Map of user-specified barcoded tubes and their (possibly user-assigned) lcset.
     */
    private void makeRowDtos(@Nonnull Multimap<LabVessel, LabBatch> loadingTubeToLcsets) {
        Date now = new Date();
        Map<LabVessel, BucketEntry> batchStartingVesselToBucketEntry = new HashMap<>();
        Multimap<LabVessel, LabEvent> loadingTubeToLabEvent = HashMultimap.create();
        Multimap<LabVessel, BucketEntry> loadingTubeToBucketEntry = HashMultimap.create();
        Multimap<LabVessel, LabVessel> loadingTubeToUnbucketedBatchStartingVessel = HashMultimap.create();
        boolean foundOutOfDateEvents = false;

        // Iterates on the barcoded loading tubes specified by the user.
        for (LabVessel barcodedTube : loadingTubeToLcsets.keySet()) {
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
                LabBatch lcset = loadingTubeToLcsets.get(barcodedTube).iterator().next();
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
                        startingBatchVessel.findVesselsForLabEventType(selectedEventType, true, DESCENDENTS));

                for (LabEvent targetEvent : loadingEventsAndVessels.keySet()) {
                    // Only uses loading tube events in the specified date range.
                    if (isInDateRange(targetEvent)) {
                        for (LabVessel loadingTube : loadingEventsAndVessels.get(targetEvent)) {
                            Set<LabBatch> bestLcsets = findBestLcset(loadLcsets, loadingTube);
                            if (CollectionUtils.isNotEmpty(bestLcsets)) {
                                loadingTubeToLcsets.putAll(loadingTube, bestLcsets);
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
            Integer numberCycles = null;
            Integer numberLanes = null;
            BigDecimal loadingConc = BigDecimal.ZERO;
            Boolean poolTest = false;
            DesignationLoadingTube.IndexType indexType = DesignationLoadingTube.IndexType.DUAL;

            // Gets product, regulatory designation, and number of samples from the bucket entries.
            for (BucketEntry bucketEntry : loadingTubeToBucketEntry.get(loadingTube)) {
                Product product = bucketEntry.getProductOrder().getProduct();
                String productName = product != null ? bucketEntry.getProductOrder().getProduct().getProductName() :
                        "[No product for " + bucketEntry.getProductOrder().getJiraTicketKey() + "]";
                readLength = product != null ? product.getReadLength() : null;
                // todo emp check for PDO read length override when it exists.
                numberCycles = readLength != null ? indexType.getIndexSize() + readLength : null;
                productToStartingVessel.put(productName, bucketEntry.getLabVessel().getLabel());
                // Sets the regulatory designation to Clinical, Research, or mixed.
                if (bucketEntry.getProductOrder().getResearchProject().getRegulatoryDesignation().
                        isClinical()) {
                    if (regulatoryDesignation == null) {
                        regulatoryDesignation = CLINICAL;
                    } else if (!regulatoryDesignation.equals(CLINICAL)) {
                        regulatoryDesignation = MIXED;
                    }
                } else {
                    if (regulatoryDesignation == null) {
                        regulatoryDesignation = RESEARCH;
                    } else if (!regulatoryDesignation.equals(RESEARCH)) {
                        regulatoryDesignation = MIXED;
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
            Pair<LabBatch, List<String>> pair = sortLcsets(loadingTubeToLcsets.get(loadingTube));
            LabBatch primaryLcset = pair.getLeft();
            List<String> additionalLcsets = pair.getRight();

            DesignationRowDto newDto = new DesignationRowDto(loadingTube, loadingTubeToLabEvent.get(loadingTube),
                    primaryLcset.getJiraTicket().getBrowserUrl(), primaryLcset, additionalLcsets,
                    productNames, StringUtils.join(startingVesselList, "\n"),
                    regulatoryDesignation, numberSamples,
                    IlluminaFlowcell.FlowcellType.MiSeqFlowcell, DesignationLoadingTube.IndexType.DUAL,
                    numberCycles, numberLanes, readLength, loadingConc, poolTest, now,
                    DesignationLoadingTube.Status.UNSAVED);

            rowDtos.add(newDto);
        }
        if (foundOutOfDateEvents) {
            addMessage("Excluded some loading tube " + selectedEventType + " events not in the date range.");
        }
    }

    /**
     * Finds the best lcset for barcoded tubes. If there are multiple lcsets then the user must make the selection.
     */
    private Multimap<LabVessel, LabBatch> linkLcsetToSpecifiedTubes() {
        tubeLcsetSelections.clear();
        Multimap<LabVessel, LabBatch> loadingTubeToLcsets = HashMultimap.create();
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
                            loadingTubeToLcsets.putAll(targetTube, lcsets);
                        } else {
                            for (LabBatch labBatch : lcsets) {
                                // Tube-lcset combinations to be resolved by the user.
                                tubeLcsetSelections.add(new TubeLcsetDto(targetTube.getLabel(),
                                        targetTube.getCreatedOn(), labBatch.getBatchName(),
                                        labBatch.getJiraTicket().getBrowserUrl(), false));
                            }
                        }
                    }
                } else {
                    foundOutOfDateEvents = true;
                }
            }
        }
        // Sorts the tubeLcsetSelections and calculates rowspan for each block of adjacent
        // rows having the same barcode.
        Collections.sort(tubeLcsetSelections, new Comparator<TubeLcsetDto>() {
            @Override
            public int compare(TubeLcsetDto o1, TubeLcsetDto o2) {
                int barcodeCompare = o1.getBarcode().compareTo(o2.getBarcode());
                return barcodeCompare != 0 ? barcodeCompare : o1.getLcsetName().compareTo(o2.getLcsetName());
            }
        });
        int rowspanIdx = 0;
        for (int i = 1; i < tubeLcsetSelections.size(); ++i) {
            if (!tubeLcsetSelections.get(rowspanIdx).getBarcode().equals(tubeLcsetSelections.get(i).getBarcode())) {
                rowspanIdx = i;
            }
            tubeLcsetSelections.get(rowspanIdx).setRowspan(i - rowspanIdx + 1);
        }

        if (foundOutOfDateEvents) {
            addMessage("Excluded some loading tube " + selectedEventType + " events not in the date range.");
        }
        return loadingTubeToLcsets;
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

    public class TubeLcsetDto {
        private String barcode;
        private String lcsetName;
        private String lcsetUrl;
        private boolean selected = false;
        private Date tubeDate;
        private int rowspan = 0;

        public TubeLcsetDto() {
        }

        public TubeLcsetDto(String barcode, Date tubeDate, String lcsetName, String lcsetUrl, boolean selected) {
            this.barcode = barcode;
            this.lcsetName = lcsetName;
            this.selected = false;
            this.tubeDate = tubeDate;
            this.lcsetUrl = lcsetUrl;
            this.selected = selected;
        }

        public String getBarcode() {
            return barcode;
        }

        public void setBarcode(String barcode) {
            this.barcode = barcode;
        }

        public String getLcsetName() {
            return lcsetName;
        }

        public void setLcsetName(String lcsetName) {
            this.lcsetName = lcsetName;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public Date getTubeDate() {
            return tubeDate;
        }

        public void setTubeDate(Date tubeDate) {
            this.tubeDate = tubeDate;
        }

        public String getLcsetUrl() {
            return lcsetUrl;
        }

        public void setLcsetUrl(String lcsetUrl) {
            this.lcsetUrl = lcsetUrl;
        }

        public int getRowspan() {
            return rowspan;
        }

        public void setRowspan(int rowspan) {
            this.rowspan = rowspan;
        }
    }

    public DesignationLoadingTube.Priority[] getPriorityValues() {
        return DesignationLoadingTube.Priority.values();
    }

    /** Returns the status values for designations that the user can edit, i.e. not the completed ones. */
    public List<DesignationLoadingTube.Status> getStatusValues() {
        List<DesignationLoadingTube.Status> statusValues = new ArrayList<>();
        for (DesignationLoadingTube.Status status : DesignationLoadingTube.Status.values()) {
            if (status.isTargetable()) {
                statusValues.add(status);
            }
        }
        return statusValues;
    }

    public IlluminaFlowcell.FlowcellType[] getFlowcellValues() {
        return IlluminaFlowcell.FlowcellType.values();
    }

    public DesignationLoadingTube.IndexType[] getIndexTypes() {
        return DesignationLoadingTube.IndexType.values();
    }

    public String getLcsetsBarcodes() {
        return lcsetsBarcodes;
    }

    public void setLcsetsBarcodes(String lcsetsBarcodes) {
        this.lcsetsBarcodes = lcsetsBarcodes;
    }

    public List<DesignationRowDto> getRowDtos() {
        return rowDtos;
    }

    public void setDesignationRowDtos(List<DesignationRowDto> rowDtos) {
        this.rowDtos = rowDtos;
    }

    public DesignationRowDto getMultiEdit() {
        return multiEdit;
    }

    public void setMultiEdit(DesignationRowDto multiEdit) {
        this.multiEdit = multiEdit;
    }

    public List<String> getCreatedFcts() {
        return createdFcts;
    }

    public void setCreatedFcts(List<String> createdFcts) {
        this.createdFcts = createdFcts;
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

    public List<TubeLcsetDto> getTubeLcsetSelections() {
        return tubeLcsetSelections;
    }

    public void setTubeLcsetSelections(List<TubeLcsetDto> tubeLcsetSelections) {
        this.tubeLcsetSelections = tubeLcsetSelections;
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

    public void setTubeLcsetSelectionCount(String count) {
        // This is needed because Stripes is unable to create TubeLcsetDtos when it
        // writes the tubeLcsetSelections array from within jsp input elements.
        for (int i = 0; i < Integer.parseInt(count); ++i) {
            tubeLcsetSelections.add(new TubeLcsetDto());
        }
    }
}

