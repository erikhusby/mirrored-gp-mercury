package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.After;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
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

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
    private List<RowDto> rowDtos = new ArrayList<>();

    private static final VesselPosition[] lanePositions = {VesselPosition.LANE1, VesselPosition.LANE2,
            VesselPosition.LANE3, VesselPosition.LANE4, VesselPosition.LANE5, VesselPosition.LANE6,
            VesselPosition.LANE7, VesselPosition.LANE8};

    /**
     * This method reloads the LCSET after validation on the save action so that we have some fields to hang our
     * validation errors on.
     */
    @After(stages = LifecycleStage.BindingAndValidation, on = SAVE_ACTION)
    public void init() {
// todo is this needed?        loadTubes();
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
        clearValidationErrors();
        return loadTubes();
    }

    /**
     * Populates the UI's table of tubes by iterating over the starting vessels for each of
     * the lcsets and finding all descendant tubes for the event type selected in the UI.
     */
    private Resolution loadTubes() {
        Set<RowDto> rowDtoSet = new HashSet<>();
        for (LabBatch labBatch : loadLcsets()) {
            boolean foundEligibleTube = false;
            for (LabVessel startingBatchVessel : labBatch.getStartingBatchLabVessels()) {
                List<BucketEntry> bucketEntries = bucketEntryDao.findByVesselAndBatch(startingBatchVessel, labBatch);
                for (Map.Entry<LabEvent, Set<LabVessel>> entry :
                        startingBatchVessel.findVesselsForLabEventType(selectedEventType, true).entrySet()) {
                    for (LabVessel eventVessel : entry.getValue()) {
                        // Logically there can only be one tube added to the flowcell regardless of how many
                        // times it was bucketed, but all choices get displayed.
                        for (BucketEntry bucketEntry : bucketEntries) {
                            foundEligibleTube = true;
                            rowDtoSet.add(new RowDto(eventVessel.getLabel(), labBatch.getBusinessKey(),
                                    entry.getKey().getEventDate(),
                                    bucketEntry.getProductOrder().getProduct() != null ?
                                            bucketEntry.getProductOrder().getProduct().getProductName() :
                                            "[No product for " +
                                                    bucketEntry.getProductOrder().getJiraTicketKey() + "]"));
                        }
                    }
                }
            }
            if (!foundEligibleTube) {
                addMessage("No " + selectedEventType.getName() + " tubes found for " + labBatch.getBatchName());
            }
        }
        rowDtos.clear();
        rowDtos.addAll(rowDtoSet);
        Collections.sort(rowDtos, RowDto.BY_BARCODE);
        return new ForwardResolution(VIEW_PAGE);
    }

    /** Cleans up and validates the list of lcset names, and returns the lab batches. */
    private List<LabBatch> loadLcsets() {
        List<LabBatch> labBatches = new ArrayList<>();
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
        // Gets the flowcell parameters.
        LabBatch.LabBatchType batchType = selectedFlowcellType.getBatchType();
        CreateFields.IssueType issueType = selectedFlowcellType.getIssueType();
        int lanesPerFlowcell = selectedFlowcellType.getVesselGeometry().getVesselPositions().length;
        if (lanesPerFlowcell < 1 || lanesPerFlowcell > lanePositions.length) {
            addMessage("ActionBean must be updated to support a flowcell lane count of " + lanesPerFlowcell);
            return new RedirectResolution(CreateFCTActionBean.class, VIEW_ACTION);
        }

        // Iterates on table rows for those with non-zero lane count.
        Map<LabVessel, RowDto> mapVesselToDto = new HashMap<>();
        int numberOfLanes = 0;
        for (RowDto rowDto : rowDtos) {
            if (rowDto.getNumberLanes() > 0) {
                LabVessel labVessel = labVesselDao.findByIdentifier(rowDto.getBarcode());
                mapVesselToDto.put(labVessel, rowDto);
                numberOfLanes += rowDto.getNumberLanes();
            }
        }
        if (numberOfLanes % lanesPerFlowcell > 0) {
            // Musn't start any FCT until all flowcells have full lanes.
            addMessage(numberOfLanes + " is not an exact multiple of " + lanesPerFlowcell + " flowcell lanes");
        } else if (numberOfLanes == 0) {
            addMessage("All tubes have zero lanes.");
        } else {
            List<LabBatch> createdFctBatches = new ArrayList<>();
            for (List<FlowcellMappingDto> laneDtos : makeFlowcellMappingList(mapVesselToDto, lanesPerFlowcell)) {

                Set<LabVessel> vesselSet = new HashSet<>();
                Set<String> lcsetSet = new HashSet<>();
                for (FlowcellMappingDto dto : laneDtos) {
                    vesselSet.add(dto.getLabVessel());
                    lcsetSet.add(dto.getRowDto().getLcset());
                }


                LabBatch fctBatch = new LabBatch(laneDtos.get(0).getLabVessel().getLabel() + " FCT ticket",
                        vesselSet, batchType, BigDecimal.ZERO, selectedFlowcellType);
                for (FlowcellMappingDto dto : laneDtos) {
                    fctBatch.addLabVessel(dto.getLabVessel(), dto.getRowDto().getLoadingConc(), dto.getPositions());
                }
                fctBatch.setBatchDescription(fctBatch.getBatchName());
                labBatchEjb.createLabBatch(fctBatch, userBean.getLoginUserName(), issueType, this);
                createdFctBatches.add(fctBatch);
                // Links LCSET tickets to FCT ticket.
                for (String lcset : lcsetSet) {
                    labBatchEjb.linkJiraBatchToTicket(lcset, fctBatch);
                }
            }

            StringBuilder createdBatchLinks = new StringBuilder("<ol>");
            for (LabBatch fctBatch : createdFctBatches) {
                createdBatchLinks.append("<li><a target=\"JIRA\" href=\"");
                createdBatchLinks.append(fctBatch.getJiraTicket().getBrowserUrl());
                createdBatchLinks.append("\" class=\"external\" target=\"JIRA\">");
                createdBatchLinks.append(fctBatch.getBusinessKey());
                createdBatchLinks.append("</a></li>");
            }
            createdBatchLinks.append("</ol>");
            addMessage("Created {0} FCT tickets: {1}", createdFctBatches.size(), createdBatchLinks.toString());
        }

        return new RedirectResolution(CreateFCTActionBean.class, VIEW_ACTION);
    }

    static class FlowcellMappingDto {
        private LabVessel labVessel;
        private RowDto rowDto;
        private List<VesselPosition> positions;

        FlowcellMappingDto(LabVessel labVessel, RowDto rowDto) {
            this.labVessel = labVessel;
            this.rowDto = rowDto;
            positions = new ArrayList<>();
        }

        public LabVessel getLabVessel() {
            return labVessel;
        }

        public RowDto getRowDto() {
            return rowDto;
        }

        public List<VesselPosition> getPositions() {
            return positions;
        }
    }

    /**
     * Maps out the associations from vessels to flowcell lanes to flowcells. The rules are
     * <br/> any given lane only has one vessel
     * <br/> a flowcell may have lanes from multiple vessels
     * <br/> a vessel may be on multiple lanes
     * <br/> a vessel may span multiple flowcells
     *
     * @return  a list representing the flowcells, each entry consisting of a list of
     *          vessel dtos occupying one or more of the flowcell lanes. The FlowcellMappingDto
     *          has a list of lane positions for each vessel on the flowcell.
     */
    public List<List<FlowcellMappingDto>> makeFlowcellMappingList(Map<LabVessel, RowDto> mapVesselToDto,
                                                                  int lanesPerFlowcell) {
        int filledLaneCount = 0;
        List<List<FlowcellMappingDto>> flowcellMappings = new ArrayList<>();
        List<FlowcellMappingDto> currentLaneVesselDtos = null;

        for (LabVessel labVessel : mapVesselToDto.keySet()) {
            RowDto rowDto = mapVesselToDto.get(labVessel);
            FlowcellMappingDto laneVesselDto = null;
            // Iterates until the the vessel's flowcell lane count is fulfilled.
            for (int i = 0; i < rowDto.getNumberLanes(); ++i) {
                // Initializes a new flowcell mapping when starting or when flowcell is full.
                if (filledLaneCount % lanesPerFlowcell == 0) {
                    currentLaneVesselDtos = new ArrayList<>();
                    flowcellMappings.add(currentLaneVesselDtos);
                }
                // Initializes a new lane vessel dto when starting a new vessel or when flowcell is full.
                if (i == 0 || filledLaneCount % lanesPerFlowcell == 0) {
                    laneVesselDto = new FlowcellMappingDto(labVessel, rowDto);
                    currentLaneVesselDtos.add(laneVesselDto);
                }
                laneVesselDto.getPositions().add(lanePositions[filledLaneCount % lanesPerFlowcell]);
                ++filledLaneCount;
            }
        }
        return flowcellMappings;
    }


    public List<IlluminaFlowcell.FlowcellType> getFlowcellTypes() {
        return FLOWCELL_TYPES;
    }

    public int getFlowcellLaneCount(String flowcellType) {
        for (IlluminaFlowcell.FlowcellType type : FLOWCELL_TYPES) {
            if (type.getDisplayName().equals(flowcellType)) {
                return type.getVesselGeometry().getColumnCount();
            }
        }
        return Integer.MAX_VALUE;
    }


    /** Returns the row dto for the given tube barcode. */
    private RowDto rowDtoForBarcode(String barcode) {
        for (RowDto rowDto : rowDtos) {
            if (rowDto.getBarcode().equals(barcode)) {
                return rowDto;
            }
        }
        String barcodes = "";
        for (RowDto rowDto : rowDtos) {
            barcodes += rowDto.getBarcode() + " ";
        }
        throw new RuntimeException("Could not find barcode '" + barcode + "' among '" + barcodes + "'");
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
}
