package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UrlBinding("/workflow/CreateFCT.action")
public class CreateFCTActionBean extends CoreActionBean {
    private static final String VIEW_PAGE = "/workflow/create_fct.jsp";
    public static final String LOAD_DENATURE = "loadDenature";
    public static final String LOAD_NORM = "loadNorm";
    public static final String LOAD_POOLNORM = "loadPoolNorm";

    public static final List<IlluminaFlowcell.FlowcellType> FLOWCELL_TYPES;

    static {
        FLOWCELL_TYPES = new ArrayList<>();
        for (IlluminaFlowcell.FlowcellType type : IlluminaFlowcell.FlowcellType.values()) {
            if (type.getCreateFct() == IlluminaFlowcell.CreateFct.YES) {
                FLOWCELL_TYPES.add(type);
            }
        }
    }

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private JiraService jiraService;

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private LabBatchEjb labBatchEjb;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private BucketEntryDao bucketEntryDao;

    @Validate(required = true, on = {LOAD_DENATURE,LOAD_NORM,LOAD_POOLNORM})
    private String lcsetNames;
    private IlluminaFlowcell.FlowcellType selectedFlowcellType;
    private LabEventType selectedEventType;
    private String selectedEventTypeDisplay;
    private List<RowDto> rowDtos = new ArrayList<>();
    private BigDecimal defaultLoadingConc = BigDecimal.ZERO;
    private String hasCrsp = "none";
    private Format dateFormat = FastDateFormat.getInstance("yyyy-MM-dd hh:mm a");

    private static final VesselPosition[] VESSEL_POSITIONS = {VesselPosition.LANE1, VesselPosition.LANE2,
            VesselPosition.LANE3, VesselPosition.LANE4, VesselPosition.LANE5, VesselPosition.LANE6,
            VesselPosition.LANE7, VesselPosition.LANE8};

    public enum LoadingTubeType {
        // These names are also used in GPUITEST FCTCreationPage.
        DENATURE("Denature", LabEventType.DENATURE_TRANSFER.getName()),
        NORM("Norm", LabEventType.NORMALIZATION_TRANSFER.getName()),
        POOLED_NORM("Pooled norm", LabEventType.POOLING_TRANSFER.getName());

        private String displayName;
        private String eventName;

        LoadingTubeType(@Nonnull String displayName, @Nonnull String eventName) {
            this.displayName = displayName;
            this.eventName = eventName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static LoadingTubeType getLoadingTubeTypeForEvent (String eventName) {
            for (LoadingTubeType loadingTubeType : LoadingTubeType.values()) {
                if (loadingTubeType.eventName.equals(eventName)) {
                    return loadingTubeType;
                }
            }
            throw new RuntimeException("No LoadingTypeType found for '" + eventName + "'");
        }
    }

    @HandlesEvent(VIEW_ACTION)
    @DefaultHandler
    public Resolution view() {
        return new ForwardResolution(VIEW_PAGE);
    }

    /**
     * Makes the list of tubes to selected from.
     * @return A forward resolution to the current page.
     */
    @HandlesEvent(LOAD_DENATURE)
    public Resolution loadDenature() {
        selectedEventType = LabEventType.DENATURE_TRANSFER;
        selectedEventTypeDisplay =
                LoadingTubeType.getLoadingTubeTypeForEvent(selectedEventType.getName()).getDisplayName();
        clearValidationErrors();
        return loadTubes();
    }

    /**
     * Makes the list of tubes to selected from.
     * @return A forward resolution to the current page.
     */
    @HandlesEvent(LOAD_NORM)
    public Resolution loadNorm() {
        selectedEventType = LabEventType.NORMALIZATION_TRANSFER;
        selectedEventTypeDisplay =
                LoadingTubeType.getLoadingTubeTypeForEvent(selectedEventType.getName()).getDisplayName();
        clearValidationErrors();
        return loadTubes();
    }

    /**
     * Makes the list of tubes to selected from.
     * @return A forward resolution to the current page.
     */
    @HandlesEvent(LOAD_POOLNORM)
    public Resolution loadPoolNorm() {
        selectedEventType = LabEventType.POOLING_TRANSFER;
        selectedEventTypeDisplay =
                LoadingTubeType.getLoadingTubeTypeForEvent(selectedEventType.getName()).getDisplayName();
        clearValidationErrors();
        return loadTubes();
    }

    /**
     * Populates the UI's table of tubes by iterating over the starting vessels for each of
     * the lcsets and finding all descendant tubes for the event type selected in the UI.
     */
    private Resolution loadTubes() {
        // Keeps any existing rowDtos (regardless of current lcset names) and only adds to them.
        Set<String> existingLcsetsAndEventTypes = RowDto.allLcsetsAndTubeTypes(rowDtos);
        for (LabBatch labBatch : loadLcsets()) {
            // Skips tube lookup if this labBatch and tube type (event type) was already loaded.
            if (!existingLcsetsAndEventTypes.contains(labBatch.getBatchName() + selectedEventTypeDisplay)) {
                int previousRowDtoCount = rowDtos.size();

                // Inverts the mappings to get event(s) and product(s) per loading tube, not per starting vessel.
                Multimap<LabVessel, LabEvent> vesselToEvents = HashMultimap.create();
                Multimap<LabVessel, BucketEntry> vesselToBucketEntries = HashMultimap.create();
                for (LabVessel startingBatchVessel : labBatch.getStartingBatchLabVessels()) {
                    for (Map.Entry<LabEvent, Set<LabVessel>> entry :
                            startingBatchVessel.findVesselsForLabEventType(selectedEventType, true).entrySet()) {
                        for (LabVessel loadingTube : entry.getValue()) {
                            vesselToEvents.put(loadingTube, entry.getKey());
                            for (BucketEntry bucketEntry : labBatch.getBucketEntries()) {
                                if (bucketEntry.getLabVessel().equals(startingBatchVessel)) {
                                    vesselToBucketEntries.put(loadingTube, bucketEntry);
                                }
                            }
                        }
                    }
                }

                // Disallows a mix of clinical and research samples.
                for (BucketEntry bucketEntry : vesselToBucketEntries.values()) {
                    String foundCrsp = String.valueOf(bucketEntry.getProductOrder().getResearchProject().
                            getRegulatoryDesignation().isClinical());
                    if (CollectionUtils.isEmpty(rowDtos)) {
                        hasCrsp = foundCrsp;
                    } else if (!hasCrsp.equals(foundCrsp)) {
                        addValidationError(lcsetNames,
                                "Cannot mix clinical and research samples (" + labBatch.getBatchName() + ")");
                        return new ForwardResolution(VIEW_PAGE);
                    }
                }

                String lcsetUrl = labBatch.getJiraTicket().getBrowserUrl();

                for (LabVessel loadingTube : vesselToEvents.keySet()) {

                    // Makes a line-break separated list of products and starting batch vessels.
                    Set<String> productNames = new HashSet<>();
                    Set<String> startingBatchVessels = new HashSet<>();
                    for (BucketEntry bucketEntry : vesselToBucketEntries.get(loadingTube)) {
                        productNames.add(bucketEntry.getProductOrder().getProduct() != null ?
                                bucketEntry.getProductOrder().getProduct().getProductName() :
                                "[No product for " + bucketEntry.getProductOrder().getJiraTicketKey() + "]");
                        startingBatchVessels.add(bucketEntry.getLabVessel().getLabel());
                    }

                    // Makes a line-break separated list of event dates.
                    Set<String> eventDates = new HashSet<>();
                    for (LabEvent labEvent : vesselToEvents.get(loadingTube)) {
                        eventDates.add(dateFormat.format(labEvent.getEventDate()));
                    }

                    RowDto rowDto = new RowDto(loadingTube.getLabel(), labBatch.getBusinessKey(),
                            StringUtils.join(eventDates, "<br/>"), StringUtils.join(productNames, "<br/>"),
                            StringUtils.join(startingBatchVessels, "<br/>"), selectedEventTypeDisplay,
                            defaultLoadingConc, lcsetUrl);
                    if (!rowDtos.contains(rowDto)) {
                        rowDtos.add(rowDto);
                    }
                }
                if (rowDtos.size() == previousRowDtoCount) {
                    addMessage("No " + selectedEventTypeDisplay + " tubes found for " + labBatch.getBatchName());
                }
            }
        }
        Collections.sort(rowDtos, RowDto.BY_BARCODE);
        return new ForwardResolution(VIEW_PAGE);
    }

    /** Validates the list of lcset names, and returns their lab batches. */
    private Set<LabBatch> loadLcsets() {
        Set<LabBatch> labBatches = new HashSet<>();
        // Keeps only the decimal digits, ascii letters, hyphen, and space delimiters.
        String[] batchNames = lcsetNames.replaceAll("[^\\p{Nd}\\p{Ll}\\p{Lu}\\p{Pd}]", " ").
                replaceAll("[^\\x20-\\x7E]", "").toUpperCase().split(" ");
        for (String batchName : batchNames) {
            if (StringUtils.isNotBlank(batchName)) {
                if (StringUtils.isNumeric(batchName)) {
                    batchName = "LCSET-" + batchName;
                }
                LabBatch batch = labBatchDao.findByBusinessKey(batchName);
                if (batch != null) {
                    labBatches.add(batch);
                } else {
                    addValidationError("lcsetText", "Could not find " + batchName);
                }
            }
        }
        return labBatches;
    }

    /**
     * This method creates FCT tickets in JIRA and persists the relevant lab batches.
     *
     * @return A redirect resolution back to the current page.
     */
    @HandlesEvent(SAVE_ACTION)
    public Resolution createFCTTicket() {
        // Collects all the selected rowDtos and their loading tubes.
        List<Pair<RowDto, LabVessel>> rowDtoLabVessels = new ArrayList<>();
        for (RowDto rowDto : rowDtos) {
            if (rowDto.getNumberLanes() > 0) {
                LabVessel labVessel = labVesselDao.findByIdentifier(rowDto.getBarcode());
                rowDtoLabVessels.add(Pair.of(rowDto, labVessel));
            }
        }
        if (rowDtoLabVessels.size() == 0) {
            addMessage("No lanes were selected.");
        } else {
            List<Pair<LabBatch, Set<String>>> fctBatches = makeFctDaoFree(rowDtoLabVessels, selectedFlowcellType);
            if (fctBatches.size() == 0) {
                addMessage("No FCTs were created.");
            } else {
                StringBuilder createdBatchLinks = new StringBuilder("<ol>");
                // For each batch, pushes the FCT to JIRA, makes the parent-child JIRA links,
                // and makes a UI message.
                for (Pair<LabBatch, Set<String>> pair : fctBatches) {
                    LabBatch fctBatch = pair.getLeft();
                    labBatchEjb.createLabBatch(fctBatch, userBean.getLoginUserName(),
                            selectedFlowcellType.getIssueType(), this);
                    for (String lcset : pair.getRight()) {
                        labBatchEjb.linkJiraBatchToTicket(lcset, fctBatch);
                    }
                    createdBatchLinks.append("<li><a target=\"JIRA\" href=\"");
                    createdBatchLinks.append(fctBatch.getJiraTicket().getBrowserUrl());
                    createdBatchLinks.append("\" class=\"external\" target=\"JIRA\">");
                    createdBatchLinks.append(fctBatch.getBusinessKey());
                    createdBatchLinks.append("</a></li>");
                }
                createdBatchLinks.append("</ol>");
                addMessage("Created {0} FCT tickets: {1}", fctBatches.size(), createdBatchLinks.toString());
            }
        }
        return new RedirectResolution(CreateFCTActionBean.class, VIEW_ACTION);
    }

    /**
     * Allocates the loading tubes to flowcells.  A given lane only contains material from one
     * tube.  But a tube may span multiple lanes and multiple flowcells, depending on the
     * number of lanes requested for the tube.
     *
     * @param rowDtoLabVessels the loading tubes' RowDtos and corresponding lab vessels.
     * @param flowcellType  the type of flowcells to create.
     * @return  the fct batches to be persisted, plus the set of lcset names to be linked to it.
     */
    public List<Pair<LabBatch, Set<String>>> makeFctDaoFree(List<Pair<RowDto, LabVessel>> rowDtoLabVessels,
                                                            IlluminaFlowcell.FlowcellType flowcellType) {
        final List<Pair<LabBatch, Set<String>>> fctBatches = new ArrayList<>();
        final int lanesPerFlowcell = flowcellType.getVesselGeometry().getRowCount();
        // These are per-flowcell accumulations, for one or more loading vessels.
        int laneIndex = 0;
        Set<String> linkedLcsets = new HashSet<>();
        List<LabBatch.VesselToLanesInfo> fctVesselLaneInfo = new ArrayList<>();

        // Iterates on the RowDtos which represent the loading tubes. For each one, keeps allocating
        // lanes until the tube's requested Number of Lanes is fulfilled. When enough lanes exist an
        // FCT is allocated and put in the return multimap.
        for (Pair<RowDto, LabVessel> rowDtoLabVessel : rowDtoLabVessels) {
            RowDto rowDto = rowDtoLabVessel.getLeft();
            LabBatch.VesselToLanesInfo vesselLaneInfo = null;
            linkedLcsets.add(rowDto.getLcset());

            // Accumulates the lanes.
            for (int i = 0; i < rowDto.getNumberLanes(); ++i) {
                if (vesselLaneInfo == null) {
                    vesselLaneInfo = new LabBatch.VesselToLanesInfo(new ArrayList<VesselPosition>(),
                            rowDto.getLoadingConc(), rowDtoLabVessel.getRight());
                    fctVesselLaneInfo.add(vesselLaneInfo);
                }
                if (linkedLcsets.size() ==  0) {
                    linkedLcsets.add(rowDto.getLcset());
                }
                vesselLaneInfo.getVesselPositions().add(VESSEL_POSITIONS[laneIndex++]);
                // Are there are enough lanes to make a new FCT?
                if (laneIndex == lanesPerFlowcell) {
                    LabBatch fctBatch = new LabBatch(rowDto.getBarcode() + " FCT ticket", fctVesselLaneInfo,
                            flowcellType.getBatchType(), flowcellType);
                    fctBatch.setBatchDescription(fctBatch.getBatchName());
                    fctBatches.add(Pair.of(fctBatch, linkedLcsets));
                    // Resets the accumulations.
                    laneIndex = 0;
                    fctVesselLaneInfo = new ArrayList<>();
                    linkedLcsets = new HashSet<>();
                    vesselLaneInfo = null;
                }
            }
        }

        if (laneIndex != 0) {
            throw new RuntimeException("A partially filled flowcell is not supported.");
        }
        return fctBatches;
    }

    public List<RowDto> getRowDtos() {
        return rowDtos;
    }

    public String getLcsetNames() {
        return lcsetNames;
    }

    public void setLcsetNames(String lcsetNames) {
        this.lcsetNames = lcsetNames;
    }

    public void setRowDtos(List<RowDto> rowDtos) {
        this.rowDtos = rowDtos;
    }

    public IlluminaFlowcell.FlowcellType getSelectedFlowcellType() {
        return selectedFlowcellType;
    }

    public void setSelectedFlowcellType(
            IlluminaFlowcell.FlowcellType selectedFlowcellType) {
        this.selectedFlowcellType = selectedFlowcellType;
    }

    public LabEventType getSelectedEventType() {
        return selectedEventType;
    }

    public void setSelectedEventType(
            LabEventType selectedEventType) {
        this.selectedEventType = selectedEventType;
    }

    public List<IlluminaFlowcell.FlowcellType> getFlowcellTypes() {
        return FLOWCELL_TYPES;
    }

    public BigDecimal getDefaultLoadingConc() {
        return defaultLoadingConc;
    }

    public void setDefaultLoadingConc(BigDecimal defaultLoadingConc) {
        this.defaultLoadingConc = defaultLoadingConc;
    }

    public String getHasCrsp() {
        return hasCrsp;
    }

    public void setHasCrsp(String hasCrsp) {
        this.hasCrsp = hasCrsp;
    }
}

