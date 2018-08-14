package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.AnalysisTypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.ReferenceSequenceDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleInstanceEntityDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleKitRequestDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessor;
import org.broadinstitute.gpinformatics.mercury.control.sample.VesselPooledTubesProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.AnalysisType;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntity;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleKitRequest;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.broadinstitute.gpinformatics.mercury.presentation.sample.WalkUpSequencing;
import org.broadinstitute.gpinformatics.mercury.presentation.workflow.CreateFCTActionBean;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class handles the external library creation from walkup sequencing web serivce calls, pooled tube
 * spreadsheet upload, and the various external library spreadsheet uploads.
 * In all cases a SampleInstanceEntity and associated MercurySample and LabVessel are created or overwritten.
 */
@Stateful
@RequestScoped
public class SampleInstanceEjb {
    // These are the only characters allowed in a library or sample name.
    public static final String RESTRICTED_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-_";
    public static final String RESTRICTED_MESSAGE = "a-z, A-Z, 0-9, '.', '-', or '_'";

    public static final String MISSING = "Row #%d is missing a value for %s.";
    public static final String INCONSISTENT_SAMPLE_DATA =
            "Row #%d value for %s is not consistent with row #%d (Sample %s).";
    public static final String INCONSISTENT_TUBE =
            "Row #%d value for %s is not consistent with row #%d (Tube Barcode %s).";
    public static final String INVALID_CHARS = "Row #%d %s characters must only be " + RESTRICTED_CHARS;
    public static final String MUST_NOT_HAVE_BOTH = "Row #%d must not have both %s and %s.";
    public static final String ALREADY_EXISTS = "Row #%d %s \"%s\" already exists in %s.";
    public static final String UNKNOWN = "Row #%d the value for %s is not in %s.";
    public static final String NONEXISTENT = "Row #%d the value for %s \"%s\" does not exist in %s.";
    public static final String UNKNOWN_COND = "Row #%d each Condition must be a Jira ticket id for a sub-task of %s.";
    public static final String DUPLICATE = "Row #%d duplicate value for %s.";
    public static final String DUPLICATE_IN_TUBE = "Row #%d has a duplicate value for %s in tube %s.";
    public static final String DUPLICATE_S_M =
            "Row #%d repeats the combination of sample %s and index %s and indicates these tubes should not be pooled.";
    public static final String BSP_FORMAT = "Row #%d the new %s \"%s\" must not have a BSP sample name format.";
    public static final String MERCURY_FORMAT =
            "Row #%d the %s \"%s\" has the 10 digit format used for a Matrix tube manufacturer barcode.";
    public static final String PREXISTING =
            "Row #%d %s named \"%s\" already exists in Mercury; set the Overwrite checkbox to re-upload.";
    public static final String PREXISTING_VALUES =
            "Row #%d values for %s already exist in Mercury; set the Overwrite checkbox to re-upload.";
    public static final String BSP_METADATA =
            "Row #%d values for %s should be blank because BSP data for sample %s cannot be updated.";
    public static final String CONFLICTING_SOURCE = "Row #%d sample \"%s\" has %s metadata source but is %s in BSP.";
    public static final String IGNORING_ROOT =
            "Row #%d sample has Mercury metadata source so the given root sample is ignored.";
    public static final String NONNEGATIVE_INTEGER = "Row #%d %s must be a non-negative integer number.";
    public static final String NONNEGATIVE_DECIMAL = "Row #%d %s must be a non-negative decimal number.";
    public static final String IS_SUCCESS = "Spreadsheet with %d rows successfully uploaded.";
    /**
     * A string of the available sequencer model names.
     */
    public static final String SEQUENCER_MODELS;


    private static final Map<String, IlluminaFlowcell.FlowcellType> mapSequencerToFlowcellType = new HashMap<>();
    private static final Log log = LogFactory.getLog(SampleInstanceEjb.class);

    @Inject
    private MolecularIndexingSchemeDao molecularIndexingSchemeDao;

    @Inject
    private JiraService jiraService;

    @Inject
    private ReagentDesignDao reagentDesignDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private SampleInstanceEntityDao sampleInstanceEntityDao;

    @Inject
    private AnalysisTypeDao analysisTypeDao;

    @Inject
    private SampleKitRequestDao sampleKitRequestDao;

    @Inject
    private SampleDataFetcher sampleDataFetcher;

    @Inject
    private ReferenceSequenceDao referenceSequenceDao;

    static {
        for (IlluminaFlowcell.FlowcellType flowcellType : CreateFCTActionBean.FLOWCELL_TYPES) {
            String sequencer = makeSequencerValue(flowcellType);
            mapSequencerToFlowcellType.put(sequencer, flowcellType);
        }
        SEQUENCER_MODELS = "\"" + StringUtils.join(mapSequencerToFlowcellType.keySet(), "\", \"") + "\"";
    }

    public SampleInstanceEjb() {
    }

    /**
     * Constructor used for unit testing.
     */
    public SampleInstanceEjb(MolecularIndexingSchemeDao molecularIndexingSchemeDao, JiraService jiraService,
            ReagentDesignDao reagentDesignDao, LabVesselDao labVesselDao, MercurySampleDao mercurySampleDao,
            SampleInstanceEntityDao sampleInstanceEntityDao, AnalysisTypeDao analysisTypeDao,
            SampleKitRequestDao sampleKitRequestDao, SampleDataFetcher sampleDataFetcher,
            ReferenceSequenceDao referenceSequenceDao) {
        this.molecularIndexingSchemeDao = molecularIndexingSchemeDao;
        this.jiraService = jiraService;
        this.reagentDesignDao = reagentDesignDao;
        this.labVesselDao = labVesselDao;
        this.mercurySampleDao = mercurySampleDao;
        this.sampleInstanceEntityDao = sampleInstanceEntityDao;
        this.analysisTypeDao = analysisTypeDao;
        this.sampleKitRequestDao = sampleKitRequestDao;
        this.sampleDataFetcher = sampleDataFetcher;
        this.reagentDesignDao = reagentDesignDao;
        this.referenceSequenceDao = referenceSequenceDao;
    }

    /**
     * Parses the uploaded spreadsheet, checks for correct headers and missing/incorrect data,
     * and if it's all ok then persists the SampleInstanceEntity and associated entities.
     * <p>
     * Uploads are either library uploads or tube uploads.
     * Tube uploads give the tube barcode and sample name, which may or may not be known to Mercury and BSP.
     * A pool of samples in one tube can be uploaded by repeating the barcode in multiple
     * spreadsheet rows, each row with a different sample name.
     * VesselPooledTube uploads can provide a JIRA DEV ticket and the relevant subtask ticket must be given.
     * <p>
     * Library uploads give no sample names, so the library name is used for that. All sample metadata
     * must be present on every row since samples are expected to be unknown to Mercury and BSP (except
     * when overwriting a previous external upload). The tube barcode is optional in the spreadsheet.
     * If it is given it must not already exist in Mercury (unless a previous upload is being
     * overwritten). If tube barcode is not given then the library name is used as the barcode.
     * The lab user can later do an ExternalLibraryBarcodeUpdate upload to reassign the tube barcode.
     *
     * @param inputStream the spreadsheet inputStream.
     * @param overwrite   specifies if existing entities should be overwritten.
     * @param processor   the TableProcessor subclass that should parse the spreadsheet.
     * @param messages    the errors, warnings, and info to be passed back.
     */
    public List<SampleInstanceEntity> doExternalUpload(InputStream inputStream, boolean overwrite,
            ExternalLibraryProcessor processor, MessageCollection messages) {

        messages.clearAll();
        if (processor == null) {
            messages.addError("Missing spreadsheet parser.");
        }
        try {
            PoiSpreadsheetParser.processSingleWorksheet(inputStream, processor);
            messages.addErrors(processor.getMessages());
            messages.addWarning(processor.getWarnings());
            List<RowDto> rowDtos = processor.parseUpload(inputStream, messages);
            if (rowDtos.isEmpty()) {
                messages.addWarning("Spreadsheet contains no valid data.");
            } else {
                // Makes maps of samples, vessels, and other primary entities.
                makeEntityMaps(processor, rowDtos, messages);
                // Validates the data.
                processor.validateAllRows(rowDtos, overwrite, messages);
                if (!messages.hasErrors()) {
                    // Creates or updates the SampleInstanceEntities, MercurySamples, LabVessels.
                    List<SampleInstanceEntity> instanceEntities = processor.makeOrUpdateEntities(rowDtos);
                    sampleInstanceEntityDao.persistAll(processor.getEntitiesToPersist());
                    sampleInstanceEntityDao.persistAll(instanceEntities);

                    if (!messages.hasErrors()) {
                        messages.addInfo(String.format(IS_SUCCESS, rowDtos.size()));
                        return instanceEntities;
                    }
                }
            }
        } catch (ValidationException e) {
            for (String msg : processor.getMessages()) {
                messages.addUniqueError(messages, msg);
            }
            messages.addWarning(processor.getWarnings());
        } catch (Exception e) {
            log.error("Failed to process SampleInstanceEntity upload.", e);
            messages.addError("Cannot process spreadsheet: " + e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        Collections.sort(messages.getErrors(), BY_ROW_NUMBER);
        return Collections.emptyList();
    }

    /**
     * After spreadsheet data is parsed, all entities referenced by the data is fetched and put in maps
     * on the dtos. All dtos have references to the same maps.
     */
    private void makeEntityMaps(ExternalLibraryProcessor processor, List<RowDto> rowDtos, MessageCollection messages) {
        Set<String> samplesToLookup = new HashSet<>();
        Set<String> barcodesToLookup = new HashSet<>();
        Exception jiraException = null;

        for (RowDto dto : rowDtos) {
            if (StringUtils.isNotBlank(dto.getSampleName())) {
                samplesToLookup.add(dto.getSampleName());
            }
            if (StringUtils.isNotBlank(dto.getRootSampleName())) {
                samplesToLookup.add(dto.getRootSampleName());
            }
            if (StringUtils.isNotBlank(dto.getBarcode())) {
                barcodesToLookup.add(dto.getBarcode());
            }
            if (StringUtils.isNotBlank(dto.getLibraryName())) {
                dto.setSampleInstanceEntity(sampleInstanceEntityDao.findByName(dto.getLibraryName()));
            }
            if (StringUtils.isNotBlank(dto.getMisName())) {
                processor.getMolecularIndexingSchemeMap().put(dto.getMisName(),
                        molecularIndexingSchemeDao.findByName(dto.getMisName()));
            }
            // If bait is given, uses it, otherwise if cat is given, uses that. Only one will be present.
            dto.setReagent(StringUtils.isNotBlank(dto.getBait()) ?
                    reagentDesignDao.findByBusinessKey(dto.getBait()) :
                    (StringUtils.isNotBlank(dto.getCat()) ?
                            reagentDesignDao.findByBusinessKey(dto.getCat()) : null));
            if (StringUtils.isNotBlank(dto.getExperiment())) {
                try {
                    JiraIssue jiraIssue = jiraService.getIssueInfo(dto.getExperiment(), (String[]) null);
                    processor.getJiraIssueMap().put(dto.getExperiment(), jiraIssue);
                } catch (Exception e) {
                    jiraException = e;
                }
            }

            if (StringUtils.isNotBlank(dto.getReferenceSequenceName())) {
                ReferenceSequence referenceSequence = dto.getReferenceSequenceName().contains("|") ?
                        referenceSequenceDao.findByBusinessKey(dto.getReferenceSequenceName()) :
                        referenceSequenceDao.findCurrent(dto.getReferenceSequenceName());
                if (referenceSequence != null) {
                    processor.getReferenceSequenceMap().put(dto.getReferenceSequenceName(), referenceSequence);
                }
            }

            if (StringUtils.isNotBlank(dto.getAnalysisTypeName())) {
                AnalysisType analysisType = analysisTypeDao.findByBusinessKey(dto.getAnalysisTypeName());
                if (analysisType != null) {
                    processor.getAnalysisTypeMap().put(dto.getAnalysisTypeName(), analysisType);
                }
            }

            if (StringUtils.isNotBlank(dto.getSequencerModelName())) {
                IlluminaFlowcell.FlowcellType sequencerModel = findFlowcellType(dto.getSequencerModelName());
                if (sequencerModel != null) {
                    processor.getSequencerModelMap().put(dto.getSequencerModelName(), sequencerModel);
                }
            }

        }
        processor.getSampleMap().putAll(mercurySampleDao.findMapIdToMercurySample(samplesToLookup));
        processor.getLabVesselMap().putAll(labVesselDao.findByBarcodes(new ArrayList<>(barcodesToLookup)));

        if (jiraException != null) {
            messages.addError("Failed to lookup Jira tickets: " + jiraException.toString());
        }

        // A BSP lookup is done on samples that are unknown in Mercury, or that have sampleData
        // source in BSP and either no root was given or spreadsheet metadata is present.
        // The BSP request is batched for efficiency.

        Set<String> bspSampleLookups = new HashSet<>();
        for (RowDto dto : rowDtos) {
            MercurySample mercurySample = processor.getSampleMap().get(dto.getSampleName());
            if (mercurySample == null ||
                    (mercurySample.getMetadataSource() == MercurySample.MetadataSource.BSP &&
                            (StringUtils.isBlank(dto.getRootSampleName()) || dto.isMetadataPresent()))) {
                if (StringUtils.isNotBlank(dto.getSampleName())) {
                    bspSampleLookups.add(dto.getSampleName());
                }
                if (StringUtils.isNotBlank(dto.getRootSampleName())) {
                    bspSampleLookups.add(dto.getRootSampleName());
                }
            }
        }
        processor.getFetchedData().putAll(sampleDataFetcher.fetchSampleData(bspSampleLookups));

        // The sample metadata cases:
        //
        // If the Broad sample doesn't exist in Mercury, i.e. there's no MercurySample for it:
        //
        //    If sample is found in BSP then the MercurySample is created with
        //    metadata source = BSP. Neither metadata nor root sample are needed in the
        //    spreadsheet but if present it's an error if they don't match BSP's data.
        //    MercurySample metadata is created from the BSP root metadata.
        //
        //    If the sample is not in BSP then a Mercury sample is created with metadata
        //    source = Mercury. The root sample name must appear in the spreadsheet.
        //
        //        If the root sample is not in BSP then a root MercurySample is created
        //        with metadata from the spreadsheet.
        //
        //        If the root is in BSP then MercurySample metadata is created from
        //        the BSP root metadata. It's an error if any spreadsheet metadata
        //        (except lsid) doesn't must match BSP's data.
        //
        // If the Broad sample does exist in Mercury:
        //
        //    If sample metadata source = BSP, neither metadata or root sample are needed in the
        //    spreadsheet. If present it's an error if they don't match what BSP has.
        //
        //    If metadata source = Mercury, neither metadata nor root sample name are needed
        //    but if present and values differ, then the Mercury sample data is updated.
        //
        // FYI for these uploaded samples the chain of custody returned by getSampleInstance()
        // is implemented by the link to the root sample found in SampleInstanceEntity.

        for (RowDto dto : rowDtos) {
            // Determines the source of sampleData for the Broad sample.
            MercurySample mercurySample = processor.getSampleMap().get(dto.getSampleName());
            if (mercurySample != null) {
                boolean sourceIsMercury = mercurySample.getMetadataSource() == MercurySample.MetadataSource.MERCURY;
                boolean hasBspData = processor.getFetchedData().containsKey(dto.getSampleName());
                if (sourceIsMercury == hasBspData) {
                    messages.addWarning(String.format(CONFLICTING_SOURCE, dto.getRowNumber(), dto.getSampleName(),
                            mercurySample.getMetadataSource().getDisplayName(), hasBspData ? "also" : "not"));
                }
                Boolean sampleHasMercuryData = processor.getSampleHasMercuryData().get(dto.getSampleName());
                if (sampleHasMercuryData == null) {
                    sampleHasMercuryData = sourceIsMercury && !hasBspData;
                    processor.getSampleHasMercuryData().put(dto.getSampleName(), sampleHasMercuryData);
                }

                MercurySample rootSample = processor.getSampleMap().get(dto.getRootSampleName());
                if (rootSample != null) {
                    boolean rootIsMercury = rootSample.getMetadataSource() == MercurySample.MetadataSource.MERCURY;
                    boolean rootHasBspData = processor.getFetchedData().containsKey(dto.getRootSampleName());
                    if (rootIsMercury == rootHasBspData) {
                        messages.addWarning(String.format(CONFLICTING_SOURCE, dto.getRowNumber(), dto.getRootSampleName(),
                                rootSample.getMetadataSource().getDisplayName(), rootHasBspData ? "also" : "not"));
                    }
                    Boolean rootHasMercuryData = processor.getSampleHasMercuryData().get(dto.getRootSampleName());
                    if (rootHasMercuryData == null) {
                        rootHasMercuryData = sampleHasMercuryData && rootIsMercury && !rootHasBspData;
                        processor.getSampleHasMercuryData().put(dto.getRootSampleName(), rootHasMercuryData);
                    }
                }
            }
        }

        if (processor.supportsSampleKitRequest()) {
            processor.setSampleKitRequest(sampleKitRequestDao.find(processor.getEmail(), processor.getOrganization(),
                    processor.getLastName(), processor.getFirstName()));
        }
    }

    /**
     * Validates the walkup sequencing submission and if ok then persists the data in new or existing
     * SampleInstanceEntity, MercurySample, LabVessel, etc.
     *
     * @param walkUpSequencing the data from a walkup sequencing submission.
     * @param messages         collected errors, warnings, info to be passed back.
     */
    public void verifyAndPersistSubmission(WalkUpSequencing walkUpSequencing, MessageCollection messages) {
        if (StringUtils.isBlank(walkUpSequencing.getEmailAddress())) {
            messages.addError("Email is missing");
        }
        if (StringUtils.isBlank(walkUpSequencing.getLabName())) {
            messages.addError("Lab Name is missing");
        }

        if (ExternalLibraryProcessor.asInteger(walkUpSequencing.getFragmentSize()) < 0) {
            messages.addError("Fragment Size must be a non-zero integer or blank");
        }

        if (StringUtils.isBlank(walkUpSequencing.getLibraryName())) {
            messages.addError("Library name (Mercury sample name) is missing.");
        }
        if (StringUtils.isBlank(walkUpSequencing.getTubeBarcode())) {
            messages.addError("Tube barcode is missing.");
        }

        if (StringUtils.isNotBlank(walkUpSequencing.getReadType()) &&
                !walkUpSequencing.getReadType().substring(0, 1).equalsIgnoreCase("s") &&
                !walkUpSequencing.getReadType().substring(0, 1).equalsIgnoreCase("p")) {
            messages.addError("Read Type must either be blank or start with \"S\" or \"P\".");
        }

        ReferenceSequence referenceSequence = StringUtils.isBlank(walkUpSequencing.getReferenceVersion()) ?
                referenceSequenceDao.findCurrent(walkUpSequencing.getReference()) :
                referenceSequenceDao.findByNameAndVersion(walkUpSequencing.getReference(),
                        walkUpSequencing.getReferenceVersion());
        if (StringUtils.isNotBlank(walkUpSequencing.getReference()) && referenceSequence == null) {
            messages.addError("Reference Sequence '" + walkUpSequencing.getReference() + "'" +
                    (StringUtils.isNotBlank(walkUpSequencing.getReferenceVersion()) ?
                            " version '" + walkUpSequencing.getReferenceVersion() + "'" : "") +
                    " is not known in Mercury.");
        }

        ReagentDesign reagentDesign = null;
        if (StringUtils.isNotBlank(walkUpSequencing.getBaitSetName())) {
            reagentDesign = reagentDesignDao.findByBusinessKey(walkUpSequencing.getBaitSetName());
            if (reagentDesign == null) {
                messages.addError("Unknown Bait Reagent '" + walkUpSequencing.getBaitSetName() + "'");
            }
        }

        IlluminaFlowcell.FlowcellType sequencerModel = null;
        if (StringUtils.isNotBlank(walkUpSequencing.getIlluminaTech())) {
            sequencerModel = SampleInstanceEjb.findFlowcellType(walkUpSequencing.getIlluminaTech());
            if (sequencerModel == null) {
                messages.addError("Unknown Sequencing Technology, must be one of " +
                        SampleInstanceEjb.SEQUENCER_MODELS + ", or blank");
            }
        }

        if (!messages.hasErrors()) {
            persistSubmission(walkUpSequencing, referenceSequence, reagentDesign, sequencerModel);
        }
    }

    private void persistSubmission(WalkUpSequencing walkUpSequencing, ReferenceSequence referenceSequence,
            ReagentDesign reagentDesign, IlluminaFlowcell.FlowcellType sequencerModel) {
        List<Object> newEntities = new ArrayList<>();

        LabVessel labVessel = labVesselDao.findByIdentifier(walkUpSequencing.getTubeBarcode());
        if (labVessel == null) {
            labVessel = new BarcodedTube(walkUpSequencing.getTubeBarcode(), BarcodedTube.BarcodedTubeType.MatrixTube);
            newEntities.add(labVessel);
        }
        SampleInstanceEjb.addLibrarySize(labVessel,
                ExternalLibraryProcessor.asNonNegativeBigDecimal(walkUpSequencing.getFragmentSize(), "", 0, null));
        labVessel.setVolume(
                ExternalLibraryProcessor.asNonNegativeBigDecimal(walkUpSequencing.getVolume(), "", 0, null));
        // Concentration units are either ng/ul or nM. Apparently the event context tells the users which one it is.
        labVessel.setConcentration(
                ExternalLibraryProcessor.asNonNegativeBigDecimal(walkUpSequencing.getConcentration(), "", 0, null));
        MercurySample mercurySample = mercurySampleDao.findBySampleKey(walkUpSequencing.getLibraryName());
        if (mercurySample == null) {
            mercurySample = new MercurySample(walkUpSequencing.getLibraryName(), MercurySample.MetadataSource.MERCURY);
            mercurySample.addMetadata(new HashSet<Metadata>() {{
                add(new Metadata(Metadata.Key.MATERIAL_TYPE, MaterialType.DNA.getDisplayName()));
            }});
        }
        mercurySample.addLabVessel(labVessel);
        newEntities.add(mercurySample);

        SampleKitRequest sampleKitRequest = sampleKitRequestDao.find(walkUpSequencing.getEmailAddress(),
                walkUpSequencing.getLabName());
        if (sampleKitRequest == null) {
            sampleKitRequest = new SampleKitRequest();
            sampleKitRequest.setEmail(walkUpSequencing.getEmailAddress());
            sampleKitRequest.setOrganization(walkUpSequencing.getLabName());
            newEntities.add(sampleKitRequest);
        }

        SampleInstanceEntity sampleInstanceEntity =
                sampleInstanceEntityDao.findByName(walkUpSequencing.getLibraryName());
        if (sampleInstanceEntity == null) {
            sampleInstanceEntity = new SampleInstanceEntity();
            sampleInstanceEntity.setSampleLibraryName(walkUpSequencing.getLibraryName());
            sampleInstanceEntity.setSampleKitRequest(sampleKitRequest);
            newEntities.add(sampleInstanceEntity);
        }
        sampleInstanceEntity.setPairedEndRead(StringUtils.startsWithIgnoreCase(walkUpSequencing.getReadType(), "p"));
        sampleInstanceEntity.setReferenceSequence(referenceSequence);
        sampleInstanceEntity.setUploadDate(walkUpSequencing.getSubmitDate());
        sampleInstanceEntity.setReadLength(Math.max(
                ExternalLibraryProcessor.asInteger(walkUpSequencing.getReadLength()),
                ExternalLibraryProcessor.asInteger(walkUpSequencing.getReadLength2())));
        sampleInstanceEntity.setNumberLanes(ExternalLibraryProcessor.asInteger(walkUpSequencing.getLaneQuantity()));
        sampleInstanceEntity.setComments(walkUpSequencing.getComments());
        sampleInstanceEntity.setReagentDesign(reagentDesign);
        sampleInstanceEntity.setLabVessel(labVessel);
        sampleInstanceEntity.setMercurySample(mercurySample);
        sampleInstanceEntity.setPooled(
                ExternalLibraryProcessor.isOneOf(walkUpSequencing.getPooledSample(), "y", "yes"));
        sampleInstanceEntity.setSequencerModel(sequencerModel);

        sampleInstanceEntityDao.persistAll(newEntities);
    }

    private static Comparator<String> BY_ROW_NUMBER = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            // Does a numeric sort on the row number string, expected to be after the word "Row #".
            int o1Row = o1.contains("Row #") ?
                    Integer.parseInt(StringUtils.substringAfter(o1, "Row #").split("[ \\.,;]")[0]) : -1;
            int o2Row = o2.contains("Row #") ?
                    Integer.parseInt(StringUtils.substringAfter(o2, "Row #").split("[ \\.,;]")[0]) : -1;
            return (o1Row == o2Row) ? o1.compareTo(o2) : (o1Row - o2Row);
        }
    };


    private static IlluminaFlowcell.FlowcellType findFlowcellType(String sequencerModel) {
        return mapSequencerToFlowcellType.get(sequencerModel);
    }

    /**
     * Adds library size metric to the given lab vessel, but does not add the same value
     * if it already exists for the that vessel.
     */
    public static void addLibrarySize(LabVessel labVessel, BigDecimal librarySize) {
        if (librarySize != null) {
            for (LabMetric labMetric : labVessel.getMetrics()) {
                if (labMetric.getName() == LabMetric.MetricType.FINAL_LIBRARY_SIZE &&
                        labMetric.getValue().equals(librarySize)) {
                    return;
                }
            }
            LabMetric finalLibrarySizeMetric = new LabMetric(librarySize, LabMetric.MetricType.FINAL_LIBRARY_SIZE,
                    LabMetric.LabUnit.UG_PER_ML, null, new Date());
            labVessel.addMetric(finalLibrarySizeMetric);
        }
    }

    /**
     * Returns the list element or null if the list or element doesn't exist
     */
    public static <T> T get(List<T> list, int index) {
        return (CollectionUtils.isNotEmpty(list) && list.size() > index) ? list.get(index) : null;
    }

    /** Returns a string that can be used as a sequencer value in the spreadsheet. */
    public static String makeSequencerValue(IlluminaFlowcell.FlowcellType flowcellType) {
        return StringUtils.substringBeforeLast(flowcellType.getDisplayName(), "Flowcell").trim();
    }

    /** Dto containing data from a spreadsheet row. */
    public static class RowDto {
        private String additionalAssemblyInformation;
        private String additionalSampleInformation;
        private String analysisTypeName;
        private String bait;
        private String barcode;
        private String cat;
        private String collaboratorParticipantId;
        private String collaboratorSampleId;
        private BigDecimal concentration;
        private List<String> conditions;
        private String dataAnalysisType;
        private String experiment;
        private BigDecimal fragmentSize;
        private Integer insertSize;
        private String irbNumber;
        private String libraryName;
        private String libraryType;
        private String lsid;
        private String misName;
        private Integer numberOfLanes;
        private String organism;
        private String participantId;
        private boolean pooled;
        private String aggregationParticle;
        private Integer readLength;
        private String referenceSequence;
        private String referenceSequenceName;
        private String rootSampleName;
        private String sampleName;
        private String sequencerModelName;
        private String sequencingTechnology;
        private String sex;
        private String singleDoubleStranded;
        private BigDecimal volume;

        private int rowNumber;
        private SampleInstanceEntity sampleInstanceEntity;
        private ReagentDesign reagent;

        public RowDto(int rowNumber) {
            this.rowNumber = rowNumber;
        }

        public String getAdditionalAssemblyInformation() {
            return additionalAssemblyInformation;
        }

        public void setAdditionalAssemblyInformation(String additionalAssemblyInformation) {
            this.additionalAssemblyInformation = StringUtils.trimToEmpty(additionalAssemblyInformation);
        }

        public String getAdditionalSampleInformation() {
            return additionalSampleInformation;
        }

        public void setAdditionalSampleInformation(String additionalSampleInformation) {
            this.additionalSampleInformation = StringUtils.trimToEmpty(additionalSampleInformation);
        }

        public String getAnalysisTypeName() {
            return analysisTypeName;
        }

        public void setAnalysisTypeName(String analysisTypeName) {
            this.analysisTypeName = analysisTypeName;
        }

        public String getBait() {
            return bait;
        }

        public void setBait(String bait) {
            this.bait = bait;
        }

        public String getBarcode() {
            return barcode;
        }

        public void setBarcode(String barcode) {
            this.barcode = barcode;
        }

        public String getRootSampleName() {
            return rootSampleName;
        }

        public void setRootSampleName(String rootSampleName) {
            this.rootSampleName = rootSampleName;
        }

        public String getCat() {
            return cat;
        }

        public void setCat(String cat) {
            this.cat = cat;
        }

        public String getCollaboratorParticipantId() {
            return collaboratorParticipantId;
        }

        public void setCollaboratorParticipantId(String collaboratorParticipantId) {
            this.collaboratorParticipantId = collaboratorParticipantId;
        }

        public String getCollaboratorSampleId() {
            return collaboratorSampleId;
        }

        public void setCollaboratorSampleId(String collaboratorSampleId) {
            this.collaboratorSampleId = collaboratorSampleId;
        }

        public BigDecimal getConcentration() {
            return concentration;
        }

        public void setConcentration(BigDecimal concentration) {
            this.concentration = concentration;
        }

        public List<String> getConditions() {
            return conditions;
        }

        public void setConditions(List<String> conditions) {
            this.conditions = conditions;
        }

        public String getDataAnalysisType() {
            return dataAnalysisType;
        }

        public void setDataAnalysisType(String dataAnalysisType) {
            this.dataAnalysisType = dataAnalysisType;
        }

        public String getExperiment() {
            return experiment;
        }

        public void setExperiment(String experiment) {
            this.experiment = experiment;
        }

        public BigDecimal getFragmentSize() {
            return fragmentSize;
        }

        public void setFragmentSize(BigDecimal fragmentSize) {
            this.fragmentSize = fragmentSize;
        }

        public Integer getInsertSize() {
            return insertSize;
        }

        public void setInsertSize(Integer insertSize) {
            this.insertSize = insertSize;
        }

        public String getIrbNumber() {
            return irbNumber;
        }

        public void setIrbNumber(String irbNumber) {
            this.irbNumber = irbNumber;
        }

        public String getLibraryName() {
            return libraryName;
        }

        public void setLibraryName(String libraryName) {
            this.libraryName = libraryName;
        }

        public String getLibraryType() {
            return libraryType;
        }

        public void setLibraryType(String libraryType) {
            this.libraryType = libraryType;
        }

        public String getLsid() {
            return lsid;
        }

        public void setLsid(String lsid) {
            this.lsid = lsid;
        }

        public String getMisName() {
            return misName;
        }

        public void setMisName(String misName) {
            this.misName = misName;
        }

        public Integer getNumberOfLanes() {
            return numberOfLanes;
        }

        public void setNumberOfLanes(Integer numberOfLanes) {
            this.numberOfLanes = numberOfLanes;
        }

        public String getOrganism() {
            return organism;
        }

        public void setOrganism(String organism) {
            this.organism = organism;
        }

        public String getParticipantId() {
            return participantId;
        }

        public void setParticipantId(String participantId) {
            this.participantId = participantId;
        }

        public boolean isPooled() {
            return pooled;
        }

        public void setPooled(boolean pooled) {
            this.pooled = pooled;
        }

        public String getAggregationParticle() {
            return aggregationParticle;
        }

        public void setAggregationParticle(String aggregationParticle) {
            this.aggregationParticle = aggregationParticle;
        }

        public Integer getReadLength() {
            return readLength;
        }

        public void setReadLength(Integer readLength) {
            this.readLength = readLength;
        }

        public String getReferenceSequence() {
            return referenceSequence;
        }

        public void setReferenceSequence(String referenceSequence) {
            this.referenceSequence = referenceSequence;
        }

        public String getReferenceSequenceName() {
            return referenceSequenceName;
        }

        public void setReferenceSequenceName(String referenceSequenceName) {
            this.referenceSequenceName = referenceSequenceName;
        }

        public String getSampleName() {
            return sampleName;
        }

        public void setSampleName(String sampleName) {
            this.sampleName = sampleName;
        }

        public String getSequencerModelName() {
            return sequencerModelName;
        }

        public void setSequencerModelName(String sequencerModelName) {
            this.sequencerModelName = sequencerModelName;
        }

        public String getSequencingTechnology() {
            return sequencingTechnology;
        }

        public void setSequencingTechnology(String sequencingTechnology) {
            this.sequencingTechnology = sequencingTechnology;
        }

        public String getSex() {
            return sex;
        }

        public void setSex(String sex) {
            this.sex = sex;
        }

        public String getSingleDoubleStranded() {
            return singleDoubleStranded;
        }

        public void setSingleDoubleStranded(String singleDoubleStranded) {
            this.singleDoubleStranded = singleDoubleStranded;
        }

        public BigDecimal getVolume() {
            return volume;
        }

        public void setVolume(BigDecimal volume) {
            this.volume = volume;
        }

        public String getMaterialType() {
            return MaterialType.DNA.getDisplayName();
        }

        public boolean isMetadataPresent() {
            return StringUtils.isNotBlank(collaboratorParticipantId) ||
                    StringUtils.isNotBlank(collaboratorSampleId) ||
                    StringUtils.isNotBlank(lsid) ||
                    StringUtils.isNotBlank(participantId);
        }

        public int getRowNumber() {
            return rowNumber;
        }

        public SampleInstanceEntity getSampleInstanceEntity() {
            return sampleInstanceEntity;
        }

        public void setSampleInstanceEntity(SampleInstanceEntity sampleInstanceEntity) {
            this.sampleInstanceEntity = sampleInstanceEntity;
        }

        public ReagentDesign getReagent() {
            return reagent;
        }

        public void setReagent(ReagentDesign reagent) {
            this.reagent = reagent;
        }

    }
}
