package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationErrors;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.presentation.links.QuoteLink;
import org.broadinstitute.gpinformatics.athena.presentation.links.SapQuoteLink;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableListFactory;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraConfig;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchDefinitionFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.QueueEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.sample.QuantificationEJB;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.VesselEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabMetricDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabMetricRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TubeFormationDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.BSPRestSender;
import org.broadinstitute.gpinformatics.mercury.control.vessel.GeminiPlateProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricDecision;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate.TubeFormationByWellCriteria.Result;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.presentation.sample.PicoDispositionActionBean;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@UrlBinding(value = "/view/uploadQuants.action")
@Dependent // To support injection into PicoToBspContainerTest
public class UploadQuantsActionBean extends CoreActionBean {
    private static final Log logger = LogFactory.getLog(UploadQuantsActionBean.class);

    public static final String ENTITY_NAME = "LabMetric";

    public enum QuantFormat {
        VARIOSKAN("Varioskan"),
        GEMINI("Gemini"),
        WALLAC("Wallac"),
        CALIPER("Caliper"),
        GENERIC("Generic"),
        LUNATIC("Lunatic");

        private String displayName;

        QuantFormat(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private static final String VIEW_PAGE = "/vessel/upload_quants.jsp";
    public static final String UPLOAD_QUANT = "uploadQuant";
    public static final String SAVE_METRICS = "saveMetrics";

    @Inject
    private QuantificationEJB quantEJB;
    @Inject
    private VesselEjb vesselEjb;
    @Inject
    private QueueEjb queueEjb;
    @Inject
    private LabMetricRunDao labMetricRunDao;
    @Inject
    private LabMetricDao labMetricDao;
    @Inject
    private ConfigurableListFactory configurableListFactory;
    @Inject
    private BSPUserList bspUserList;
    @Inject
    private BSPRestSender bspRestSender;
    @Inject
    private TubeFormationDao tubeFormationDao;
    @Inject
    private JiraConfig jiraConfig;
    @Inject
    private PriceListCache priceListCache;
    @Inject
    private QuoteLink quoteLink;
    @Inject
    private SapQuoteLink sapQuoteLink;

    @Validate(required = true, on = UPLOAD_QUANT)
    private FileBean quantSpreadsheet;
    private LabMetric.MetricType quantType;
    private Set<LabMetric> labMetrics;
    private QuantFormat quantFormat;
    private LabMetricRun labMetricRun;
    private Long labMetricRunId;
    private List<Long> selectedConditionalIds = new ArrayList<>();
    private String overrideReason;
    private LabMetricDecision.Decision overrideDecision;
    private List<String> tubeFormationLabels;
    /** acceptRePico indicates the user wishes to process the new pico regardless of existing quants. */
    private boolean acceptRePico;
    private ConfigurableList.ResultList resultList;

    private static final int STRING_LIMIT = 255;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        if (labMetricRunId != null) {
            labMetricRun = labMetricRunDao.findById(LabMetricRun.class, labMetricRunId);
            buildColumns();
        }
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(UPLOAD_QUANT)
    public Resolution uploadQuant() {
        if (quantFormat == QuantFormat.GENERIC) {
            MessageCollection messageCollection = new MessageCollection();
            quantEJB.storeQuants(labMetrics, quantType, messageCollection);
            addMessages(messageCollection);
        }
        if (getValidationErrors().isEmpty()) {
            addMessage("Successfully uploaded quant.");
        }
        buildColumns();
        return new ForwardResolution(VIEW_PAGE);
    }

    @ValidationMethod(on = UPLOAD_QUANT)
    public void validateNoExistingQuants(ValidationErrors errors) {
        InputStream quantStream = null;
        try {
            quantStream = quantSpreadsheet.getInputStream();

            switch (quantFormat) {
            case VARIOSKAN: {
                MessageCollection messageCollection = new MessageCollection();

                Pair<LabMetricRun, List<TubeFormation>> pair = spreadsheetToMercuryAndBsp(messageCollection,
                        quantStream, getQuantType(), userBean, acceptRePico);
                if (pair != null) {
                    labMetricRun = pair.getLeft();
                    tubeFormationLabels = pair.getRight().stream().map(LabVessel::getLabel).collect(Collectors.toList());

                    handleQueues(messageCollection, pair.getRight());
                }
                addMessages(messageCollection);
                break;
            }
            case WALLAC: {
                String filename = quantSpreadsheet.getFileName();
                MessageCollection messageCollection = new MessageCollection();
                Pair<LabMetricRun, String> pair = vesselEjb.createWallacRun(quantStream, filename, getQuantType(),
                        userBean.getBspUser().getUserId(), messageCollection, acceptRePico);
                if (pair != null) {
                    labMetricRun = pair.getLeft();
                    tubeFormationLabels = Collections.singletonList(pair.getRight());
                }
                addMessages(messageCollection);
                break;
            }
            case CALIPER: {
                MessageCollection messageCollection = new MessageCollection();
                Pair<LabMetricRun, String> pair = vesselEjb.createRNACaliperRun(quantStream, getQuantType(),
                        userBean.getBspUser().getUserId(), messageCollection, acceptRePico);
                if (pair != null) {
                    labMetricRun = pair.getLeft();
                    tubeFormationLabels = Collections.singletonList(pair.getRight());
                }
                addMessages(messageCollection);
                break;
            }
            case GEMINI: {
                MessageCollection messageCollection = new MessageCollection();
                Triple<LabMetricRun, List<Result>, Map<String, String>> triple = vesselEjb.createGeminiRun(
                        quantStream, quantSpreadsheet.getFileName(), getQuantType(), userBean.getBspUser().getUserId(),
                        messageCollection, acceptRePico);
                if (triple != null) {
                    labMetricRun = triple.getLeft();
                    if (triple.getMiddle() != null) {
                        tubeFormationLabels = triple.getMiddle().stream()
                                .map(r -> r.getTubeFormation().getLabel()).collect(Collectors.toList());
                    }
                    if (quantType == LabMetric.MetricType.PLATING_PICO ||
                            quantType == LabMetric.MetricType.INITIAL_PICO) {
                        sendTubeQuantsToBsp(triple.getRight(),
                                GeminiPlateProcessor.RUN_DATE_FORMAT.format(labMetricRun.getRunDate()),
                                messageCollection);
                    }

                    handleQueues(messageCollection, triple.getMiddle().stream().map(Result::getTubeFormation).collect(Collectors.toList()));
                }

                addMessages(messageCollection);
                break;
            }
            case LUNATIC: {
                MessageCollection messageCollection = new MessageCollection();
                Triple<LabMetricRun, List<Result>, Map<String, String>> triple = vesselEjb.createLunaticRun(
                        quantStream, quantSpreadsheet.getFileName(), getQuantType(),
                        userBean.getBspUser().getUserId(), messageCollection, acceptRePico);
                if (triple != null) {
                    labMetricRun = triple.getLeft();
                    if (triple.getMiddle() != null) {
                        tubeFormationLabels = triple.getMiddle().stream()
                                .map(r -> r.getTubeFormation().getLabel()).collect(Collectors.toList());
                    }

                    queueEjb.dequeueLabVessels(labMetricRun, QueueType.DNA_QUANT, messageCollection);
                }
                addMessages(messageCollection);
                break;
            }
            case GENERIC:
                labMetrics = quantEJB.validateQuantsDontExist(quantStream, quantType, acceptRePico);
                break;
            }
        } catch (IOException e) {
            errors.add("quantSpreadsheet", new SimpleError("IO exception while parsing upload. " + e.getMessage()));
        } catch (InvalidFormatException e) {
            errors.add("quantSpreadsheet", new SimpleError("Invalid format exception while parsing upload. " + e.getMessage()));
        } catch (ValidationException e) {
            StringBuilder errorBuilder = new StringBuilder();
            errorBuilder.append("Errors parsing uploaded file : <ul>");
            for (String error : e.getValidationMessages()) {
                errorBuilder.append("<li>");
                errorBuilder.append(error);
                errorBuilder.append("</li>");
            }
            errorBuilder.append("</ul>");
            errors.add("quantSpreadsheet", new SimpleError(errorBuilder.toString()));
        } catch (Exception e) {
            errors.add("quantSpreadsheet", new SimpleError("Exception while parsing upload. " + e.getMessage()));
            logger.error("Exception while parsing upload", e);
        } finally {
            IOUtils.closeQuietly(quantStream);
            try {
                quantSpreadsheet.delete();
            } catch (IOException ignored) {
                // If cannot delete, oh well.
            }
        }
    }

    private void handleQueues(MessageCollection messageCollection, List<TubeFormation> tubeFormations) {
        // We don't know whether the rack of tubes or the dilution plate was queued, so dequeue both, one will fail
        // silently
        queueEjb.dequeueLabVessels(labMetricRun, QueueType.DNA_QUANT, messageCollection);
        Set<LabVessel> dilutionPlates = tubeFormations.stream().
                flatMap(tubeFormation -> tubeFormation.getTransfersFrom().stream()).
                filter(labEvent -> labEvent.getLabEventType() == LabEventType.PICO_DILUTION_TRANSFER_FORWARD_BSP).
                flatMap(labEvent -> labEvent.getTargetLabVessels().stream()).
                collect(Collectors.toSet());
        queueEjb.dequeueLabVessels(dilutionPlates, QueueType.DNA_QUANT, messageCollection, null);
    }

    public void sendTubeQuantsToBsp(Map<String, String> tubeBarcodeToQuantValue, String runDate,
            MessageCollection messageCollection) {
        List<String> tubesNotSent = new ArrayList<>();
        List<BarcodedTube> tubes = new ArrayList<>();
        tubeFormationDao.findByLabels(tubeFormationLabels).stream().
                flatMap(tubeFormation -> tubeFormation.getContainerRole().getContainedVessels().stream()).
                filter(tube -> tubeBarcodeToQuantValue.containsKey(tube.getLabel())).
                forEach(tube -> {
                    // Clinical sample tubes should not go to BSP.  Can't use isClinicalSample due to positive controls.
                    if (tube.getMercurySamples().stream().anyMatch(
                            mercurySample -> mercurySample.getMetadataSource() == MercurySample.MetadataSource.BSP)) {
                        tubes.add(tube);
                    } else {
                        tubesNotSent.add(tube.getLabel());
                    }
                });
        if (!tubesNotSent.isEmpty()) {
            tubesNotSent.sort(Comparator.naturalOrder());
            messageCollection.addInfo("Mercury withheld " + tubesNotSent.size() + " CRSP samples from BSP (" +
                    StringUtils.join(tubesNotSent, " ") + ").");
        }
        if (!tubes.isEmpty()) {
            List<String> tubeBarcodes = tubes.stream().
                    map(BarcodedTube::getLabel).collect(Collectors.toList());
            List<String> quants = tubeBarcodes.stream().
                    map(label -> tubeBarcodeToQuantValue.get(label)).collect(Collectors.toList());
            List<String> volumes = tubes.stream()
                    .map(tube -> tube.getVolume().toPlainString()).collect(Collectors.toList());
            String user = userBean.getBspUser().getUsername();
            BSPRestSender.TubeQuants tubeQuants = new BSPRestSender.TubeQuants(user, tubeBarcodes, quants, volumes,
                    runDate);
            try {
                bspRestSender.postToBsp(tubeQuants, BSPRestSender.BSP_UPDATE_TUBE_QUANTS_URL, messageCollection);
            } catch (Exception e) {
                messageCollection.addError("Failed to send quants to BSP: " + e.toString());
            }
        }
    }

    @HandlesEvent(SAVE_METRICS)
    public Resolution saveMetrics() {
        MessageCollection messageCollection = new MessageCollection();
        if (selectedConditionalIds.isEmpty()) {
            messageCollection.addWarning("Check at least one box.");
        } else if (overrideReason == null || overrideReason.trim().isEmpty()) {
            messageCollection.addWarning("Override reason is required.");
        } else if (overrideReason.length() > STRING_LIMIT) {
            messageCollection.addWarning("Override reason is too long. Limit is 255 characters.");
        } else {
            List<LabMetric> selectedLabMetrics = labMetricDao.findListByList(LabMetric.class, LabMetric_.labMetricId,
                    selectedConditionalIds);
            Date now = new Date();
            for (LabMetric selectedLabMetric : selectedLabMetrics) {
                LabMetricDecision labMetricDecision = selectedLabMetric.getLabMetricDecision();
                if (labMetricDecision.getDecision().isEditable()) {
                    labMetricDecision.setDecidedDate(now);
                    labMetricDecision.setDeciderUserId(userBean.getBspUser().getUserId());
                    labMetricDecision.setDecision(overrideDecision);
                    labMetricDecision.setOverrideReason(overrideReason);
                    labMetricDecision.setNeedsReview(LabMetricDecision.NeedsReview.FALSE);
                }
            }

            labMetricDao.flush();
            addMessage("Successfully saved metrics.");
        }
        if( labMetricRunId != null ) {
            labMetricRun = labMetricRunDao.findById(LabMetricRun.class, labMetricRunId);
            buildColumns();
        }
        if (messageCollection.hasWarnings()) {
            addMessages(messageCollection);
        }
        return new ForwardResolution(VIEW_PAGE);
    }

    /**
     * Persists the spreadsheet as a lab metrics run in Mercury and sends a filtered version of the spreadsheet
     * containing only the research sample quants to BSP.
     */
    public Pair<LabMetricRun, List<TubeFormation>> spreadsheetToMercuryAndBsp(MessageCollection messageCollection,
            InputStream quantStream, LabMetric.MetricType quantType, UserBean userBean, boolean acceptRedoPico)
            throws Exception {

        byte[] quantStreamBytes = IOUtils.toByteArray(quantStream);
        // Also updates BSP Sample Data with the uploaded quants.
        Triple<LabMetricRun, List<Result>, Set<StaticPlate>> runAndRackOfTubes = vesselEjb.createVarioskanRun(
                new ByteArrayInputStream(quantStreamBytes), quantType, userBean.getBspUser().getUserId(),
                messageCollection, acceptRedoPico);
        if (messageCollection.hasErrors()) {
            return null;
        }

        List<Result> traverserResults = runAndRackOfTubes.getMiddle();
        if (quantType == LabMetric.MetricType.INITIAL_PICO) {
            Set<VesselPosition> researchWellPositions = new HashSet<>();
            for (Result traverserResult : traverserResults) {

                Set<VesselPosition> researchTubePositions = new HashSet<>();
                // Must re-fetch to avoid lazy evaluation exception on tube.getSampleInstanceV2()
                TubeFormation tubeFormation =
                        tubeFormationDao.findByDigest(traverserResult.getTubeFormation().getDigest());
                for (Map.Entry<VesselPosition, BarcodedTube> entry :
                        tubeFormation.getContainerRole().getMapPositionToVessel().entrySet()) {
                    VesselPosition tubePosition = entry.getKey();
                    BarcodedTube tube = entry.getValue();
                    for (SampleInstanceV2 sampleInstance : tube.getSampleInstancesV2()) {
                        if (sampleInstance.getRootOrEarliestMercurySample().getMetadataSource() == MercurySample.MetadataSource.BSP) {
                            researchTubePositions.add(tubePosition);
                            break;
                        }
                    }
                }

                for (VesselPosition wellPosition : traverserResult.getWellToTubePosition().keySet()) {
                    VesselPosition tubePosition = traverserResult.getWellToTubePosition().get(wellPosition);
                    if (researchTubePositions.contains(tubePosition)) {
                        researchWellPositions.add(wellPosition);
                    }
                }
            }
            if (!researchWellPositions.isEmpty()) {
                InputStream filteredQuantStream;
                try {
                    filteredQuantStream = VesselEjb.filterOutRows(new ByteArrayInputStream(quantStreamBytes),
                            researchWellPositions);
                } catch (Exception e) {
                    messageCollection.addError("Cannot process spreadsheet: " + e.toString());
                    throw e;
                }
                try {
                    String filename = "SonicRack" + traverserResults.iterator().next().getTubeFormation().getLabel() + ".xls";
                    bspRestSender.postToBsp(userBean.getBspUser().getUsername(),
                            filename, filteredQuantStream, BSPRestSender.BSP_UPLOAD_QUANT_URL);
                } catch (Exception e) {
                    messageCollection.addError("Cannot send research quants to BSP: " + e.toString());
                    throw e;
                }
            }
        }

        List<TubeFormation> tubeFormations =
                traverserResults.stream().map(Result::getTubeFormation).collect(Collectors.toList());
        return Pair.of(runAndRackOfTubes.getLeft(), tubeFormations);
    }

    private void buildColumns() {
        if (labMetricRun == null) {
            return;
        }
        List<LabMetric> labMetricList = new ArrayList<>();
        Map<String, LabMetric> mapIdToMetric = new HashMap<>();
        for (LabMetric labMetric : labMetricRun.getLabMetrics()) {
            if (labMetric.getLabMetricDecision() != null) {
                // todo jmt linked set?
                labMetricList.add(labMetric);
                mapIdToMetric.put(labMetric.getLabMetricId().toString(), labMetric);
            }
        }

        SearchContext searchContext = new SearchContext();
        searchContext.setBspUserList(bspUserList);
        searchContext.setUserBean(userBean);
        searchContext.setJiraConfig(jiraConfig);
        searchContext.setPriceListCache(priceListCache);
        searchContext.setQuoteLink(quoteLink);
        searchContext.setSapQuoteLink(sapQuoteLink);
        ConfigurableList configurableList = configurableListFactory.create(labMetricList, "Default",
                ColumnEntity.LAB_METRIC, searchContext,
                SearchDefinitionFactory.getForEntity(ColumnEntity.LAB_METRIC.getEntityName()));

        resultList = configurableList.getResultList();
        resultList.setConditionalCheckboxHeader("Override");
        for (ConfigurableList.ResultRow resultRow : resultList.getResultRows()) {
            LabMetric labMetric = mapIdToMetric.get(resultRow.getResultId());
            if (labMetric.getLabMetricDecision().isNeedsReview()) {
                resultRow.setCssStyles("warning");
            }
            if (labMetric.getLabMetricDecision().getDecision().isEditable()) {
                resultRow.setConditionalCheckbox(true);
            }
        }
    }

    public FileBean getQuantSpreadsheet() {
        return quantSpreadsheet;
    }

    public void setQuantSpreadsheet(FileBean quantSpreadsheet) {
        this.quantSpreadsheet = quantSpreadsheet;
    }

    public LabMetric.MetricType getQuantType() {
        return quantType;
    }

    public void setQuantType(LabMetric.MetricType quantType) {
        this.quantType = quantType;
    }

    public List<LabMetric.MetricType> getUploadEnabledMetricTypes() {
        return LabMetric.MetricType.getUploadSupportedMetrics();
    }

    public QuantFormat getQuantFormat() {
        return quantFormat;
    }

    public void setQuantFormat(QuantFormat quantFormat) {
        this.quantFormat = quantFormat;
    }

    public LabMetricRun getLabMetricRun() {
        return labMetricRun;
    }

    public Long getLabMetricRunId() {
        return labMetricRunId;
    }

    public void setLabMetricRunId(Long labMetricRunId) {
        this.labMetricRunId = labMetricRunId;
    }

    public List<Long> getSelectedConditionalIds() {
        return selectedConditionalIds;
    }

    public void setSelectedConditionalIds(List<Long> selectedConditionalIds) {
        this.selectedConditionalIds = selectedConditionalIds;
    }

    public String getOverrideReason() {
        return overrideReason;
    }

    public void setOverrideReason(String overrideReason) {
        this.overrideReason = overrideReason;
    }

    public LabMetricDecision.Decision getOverrideDecision() {
        return overrideDecision;
    }

    public void setOverrideDecision(LabMetricDecision.Decision overrideDecision) {
        this.overrideDecision = overrideDecision;
    }

    public List<LabMetricDecision.Decision> getEditableDecisions() {
        return LabMetricDecision.Decision.getEditableDecisions();
    }

    public String getPicoDispositionActionBeanUrl() {
        return PicoDispositionActionBean.ACTION_BEAN_URL;
    }

    public List<String> getTubeFormationLabels() {
        return tubeFormationLabels;
    }

    public void setTubeFormationLabels(List<String> tubeFormationLabels) {
        this.tubeFormationLabels = tubeFormationLabels;
    }

    public boolean getAcceptRePico() {
        return acceptRePico;
    }

    public void setAcceptRePico(boolean acceptRePico) {
        this.acceptRePico = acceptRePico;
    }

    public ConfigurableList.ResultList getResultList() {
        return resultList;
    }

    public String getEntityName() {
        return ENTITY_NAME;
    }

    public String getSessionKey() {
        return null;
    }

    public String getColumnSetName() {
        return null;
    }

    public String getDownloadColumnSets() {
        return null;
    }

    public UserBean getUserBean() {
        return userBean;
    }
}
