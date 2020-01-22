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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
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
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.run.DesignationActionBean;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@UrlBinding("/workflow/CreateFCT.action")
public class CreateFCTActionBean extends CoreActionBean {
    private static final String VIEW_PAGE = "/workflow/create_fct.jsp";
    public static final String LOAD_DENATURE = "loadDenature";
    public static final String LOAD_NORM = "loadNorm";
    public static final String LOAD_POOLNORM = "loadPoolNorm";

    public static final String CLINICAL = "Clinical";
    public static final String RESEARCH = "Research";
    public static final String MIXED = "Clinical and Research";
    public static final String CONTROLS = "Controls";

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

    @Inject
    private ProductDao productDao;

    @Validate(required = true, on = {LOAD_DENATURE,LOAD_NORM,LOAD_POOLNORM})
    private String lcsetNames;
    private IlluminaFlowcell.FlowcellType selectedFlowcellType;
    private LabEventType selectedEventType;
    private String selectedEventTypeDisplay;
    private List<CreateFctDto> createFctDtos = new ArrayList<>();
    private BigDecimal defaultLoadingConc = BigDecimal.ZERO;
    private String hasCrsp = "none";
    private Format dateFormat = FastDateFormat.getInstance("yyyy-MM-dd hh:mm a");

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
            throw new RuntimeException("No LoadingTubeType found for '" + eventName + "'");
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
        Map<LabVessel, LabBatch> loadingTubeToLcset = new HashMap<>();
        Multimap<LabVessel, LabBatch> loadingTubeToLcsets = HashMultimap.create();
        Multimap<LabVessel, BucketEntry> loadingTubeToBucketEntry = HashMultimap.create();
        Multimap<LabVessel, LabVessel> loadingTubeToControl = HashMultimap.create();
        Multimap<LabVessel, LabEvent> loadingTubeToLabEvent = HashMultimap.create();

        List<String> noLoadingTubeLcsets = DesignationActionBean.loadingTubeDtoForLcsets(selectedEventType,
                loadLcsets(), loadingTubeToLcset, loadingTubeToLcsets, loadingTubeToLabEvent,
                loadingTubeToBucketEntry, loadingTubeToControl);
        if (!noLoadingTubeLcsets.isEmpty()) {
            addMessage("No " + selectedEventType + " found tubes in " + StringUtils.join(noLoadingTubeLcsets, ", "));
        }

        // Makes UI table rows for the loading tubes.
        for (LabVessel loadingTube : loadingTubeToLabEvent.keySet()) {
            Multimap<String, String> productToStartingVessel = HashMultimap.create();
            Set<String> sampleNames = new HashSet<>();
            Set<LabVessel> bucketedVessels = new HashSet<>();
            String regulatoryDesignation = null;

            // Gets product, regulatory designation, and number of samples from the bucket entries.
            // For each loading tube, all ancestor samples must be found in order to correctly
            // display pools from multiple LCSETs (SUPPORT-3578).
            for (BucketEntry bucketEntry : loadingTubeToBucketEntry.get(loadingTube)) {
                String productName = bucketEntry.getProductOrder().getProduct() != null ?
                        bucketEntry.getProductOrder().getProduct().getProductName() :
                        "[No product for " + bucketEntry.getProductOrder().getJiraTicketKey() + "]";
                productToStartingVessel.put(productName, bucketEntry.getLabVessel().getLabel());
                // Sets the regulatory designation to Clinical, Research, or mixed.
                if (bucketEntry.getProductOrder().getResearchProject().getRegulatoryDesignation().isClinical()) {
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
                bucketedVessels.add(bucketEntry.getLabVessel());
                sampleNames.addAll(bucketEntry.getLabVessel().getSampleNames());
            }
            for (LabVessel startingTube : loadingTubeToControl.get(loadingTube)) {
                productToStartingVessel.put(CONTROLS, startingTube.getLabel());
                sampleNames.addAll(startingTube.getSampleNames());
            }

            // For each loading tube a per-product list of starting vessels is shown in a tooltip.
            List<String> productNameList = new ArrayList<>(productToStartingVessel.keySet());
            Collections.sort(productNameList);
            List<String> startingVesselList = new ArrayList<>();
            for (String productName : productNameList) {
                List<String> startingVessels = new ArrayList<>(productToStartingVessel.get(productName));
                Collections.sort(startingVessels);
                startingVesselList.add((productName.equals(CONTROLS) ?
                        productName : "Bucketed tubes for " + productName) + ": " +
                        StringUtils.join(startingVessels, ", "));
            }

            // Gets event dates.
            Set<String> eventDates = new HashSet<>();
            for (LabEvent labEvent : loadingTubeToLabEvent.get(loadingTube)) {
                eventDates.add(dateFormat.format(labEvent.getEventDate()));
            }

            LabBatch primaryLcset = loadingTubeToLcset.get(loadingTube);
            String additionalLcsets = "";
            for (LabBatch additionalLcset : loadingTubeToLcsets.get(loadingTube)) {
                if (!additionalLcset.equals(primaryLcset)) {
                    additionalLcsets += "<br/>" + additionalLcset.getBatchName();
                }
            }

            CreateFctDto createFctDto = new CreateFctDto(loadingTube.getLabel(), primaryLcset.getBatchName(),
                    additionalLcsets, StringUtils.join(eventDates, "<br/>"), productNameList,
                    StringUtils.join(startingVesselList, "\n"),
                    selectedEventTypeDisplay, defaultLoadingConc, primaryLcset.getJiraTicket().getBrowserUrl(),
                    regulatoryDesignation, sampleNames.size());

            // Each tube barcode should only be present once, so this tube merges into an existing
            // dto if it exists and hasn't already been selected.
            CreateFctDto existingDto = null;
            for (CreateFctDto dto : createFctDtos) {
                if (dto.getBarcode().equals(createFctDto.getBarcode())) {
                    existingDto = dto;
                }
            }
            if (existingDto == null) {
                createFctDtos.add(createFctDto);
            } else {
                if (existingDto.getNumberLanes() == 0) {
                    createFctDtos.remove(existingDto);
                    createFctDtos.add(createFctDto);
                } else if (!existingDto.getLcset().equals(createFctDto.getLcset())) {
                    addMessage("Tube " + existingDto.getBarcode() + " is already selected in " +
                            existingDto.getLcset() + " and will not be updated to " + createFctDto.getLcset());
                }
            }
        }
        Collections.sort(createFctDtos, (d0, d1) -> d0.getBarcode().compareTo(d1.getBarcode()));
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
        String regulatoryDesignation = null;
        // Only uses the selected dtos.
        final List<CreateFctDto> dtos = createFctDtos.stream().
                filter(dto -> dto != null && dto.getNumberLanes() > 0).
                collect(Collectors.toList());
        // Checks selected tubes for a mix of regulatory designations.
        for (CreateFctDto createFctDto : dtos) {
            if (regulatoryDesignation == null) {
                regulatoryDesignation = createFctDto.getRegulatoryDesignation();
            }
            if (!regulatoryDesignation.equals(createFctDto.getRegulatoryDesignation()) ||
                    regulatoryDesignation.equals(MIXED)) {
                boolean mixedFlowcellOk = labBatchEjb.isMixedFlowcellOk(createFctDto);
                if (!mixedFlowcellOk) {
                    addGlobalValidationError("Cannot mix Clinical and Research on a flowcell.");
                    return new ForwardResolution(VIEW_PAGE);
                }
            }
        }
        labBatchEjb.makeFcts(dtos, selectedFlowcellType, userBean.getLoginUserName(), this);
        return new RedirectResolution(CreateFCTActionBean.class, VIEW_ACTION);
    }

    public List<CreateFctDto> getCreateFctDtos() {
        return createFctDtos;
    }

    public String getLcsetNames() {
        return lcsetNames;
    }

    public void setLcsetNames(String lcsetNames) {
        this.lcsetNames = lcsetNames;
    }

    public void setCreateFctDtos(List<CreateFctDto> createFctDtos) {
        this.createFctDtos = createFctDtos;
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

