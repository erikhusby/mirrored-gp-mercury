package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.template.EmailSender;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.AnalysisTypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.ReferenceSequenceDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleInstanceEntityDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.AnalysisType;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntity;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.presentation.sample.WalkUpSequencing;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class handles the external library creation from walkup sequencing web serivce calls, pooled tube
 * spreadsheet upload, and EZPass/New Tech external library spreadsheet uploads.
 * In all cases a SampleInstanceEntity and associated MercurySample and LabVessel are created or overwritten.
 */
@Stateful
@RequestScoped
public class SampleInstanceEjb {
    // These are the only characters allowed in a library or sample name.
    public static final String RESTRICTED_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-_";
    // These are the only characters allowed in an alias (Collab Sample Id, Individual Id, etc).
    public static final String ALIAS_CHARS = RESTRICTED_CHARS + " @#&*()[]|;:<>,?/=+\"'";

    public static final String BAD_RANGE = "Row #%d %s must contain integer-integer (such as 225-350).";
    public static final String BSP_METADATA = "Row #%d overwriting BSP sample metadata %s cannot be done here.";
    public static final String DUPLICATE = "Row #%d duplicate value for %s.";
    public static final String DUPLICATE_IN_TUBE = "Row #%d has a duplicate value for %s in tube %s.";
    public static final String DUPLICATE_S_M =
            "Row #%d repeats the combination of sample %s and index %s and indicates these tubes should not be pooled.";
    public static final String INCONSISTENT_SAMPLE_DATA =
            "Row #%d value for %s is not consistent with row #%d (Sample %s).";
    public static final String INCONSISTENT_TUBE =
            "Row #%d value for %s is not consistent with row #%d (Tube Barcode %s).";
    public static final String INVALID_CHARS = "Row #%d %s characters must only be %s";
    public static final String IS_SUCCESS = "Spreadsheet with %d rows successfully uploaded.";
    public static final String MISSING = "Row #%d is missing a value for %s.";
    public static final String NONNEGATIVE_DECIMAL = "Row #%d %s must be a non-negative decimal number.";
    public static final String NONNEGATIVE_INTEGER = "Row #%d %s must be a non-negative integer number.";
    public static final String PREXISTING =
            "Row #%d %s named \"%s\" already exists in Mercury; set the Overwrite checkbox to re-upload.";
    public static final String MERCURY_METADATA = "Row #%d requires Overwrite checkbox to be set for " +
            "updating Mercury sample metadata %s.";
    public static final String TOO_LONG = "Row #$d the value for %s is too long (limit is %d).";
    public static final String UNKNOWN = "Row #%d the value for %s is not in %s.";

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
    private SampleDataFetcher sampleDataFetcher;

    @Inject
    private ReferenceSequenceDao referenceSequenceDao;

    @Inject
    private EmailSender emailSender;

    @Inject
    private UserBean userBean;

    @Inject
    private Deployment deployment;

    @Inject
    private ProductDao productDao;

    public SampleInstanceEjb() {
    }

    /**
     * Constructor used for unit testing.
     */
    public SampleInstanceEjb(MolecularIndexingSchemeDao molecularIndexingSchemeDao, JiraService jiraService,
            ReagentDesignDao reagentDesignDao, LabVesselDao labVesselDao, MercurySampleDao mercurySampleDao,
            SampleInstanceEntityDao sampleInstanceEntityDao, AnalysisTypeDao analysisTypeDao,
            SampleDataFetcher sampleDataFetcher, ReferenceSequenceDao referenceSequenceDao, ProductDao productDao) {
        this.molecularIndexingSchemeDao = molecularIndexingSchemeDao;
        this.jiraService = jiraService;
        this.reagentDesignDao = reagentDesignDao;
        this.labVesselDao = labVesselDao;
        this.mercurySampleDao = mercurySampleDao;
        this.sampleInstanceEntityDao = sampleInstanceEntityDao;
        this.analysisTypeDao = analysisTypeDao;
        this.sampleDataFetcher = sampleDataFetcher;
        this.reagentDesignDao = reagentDesignDao;
        this.referenceSequenceDao = referenceSequenceDao;
        this.productDao = productDao;
    }

    /**
     * Parses the uploaded spreadsheet, checks for correct headers and missing/incorrect data, and if it's
     * all ok then persists the SampleInstanceEntities and associated Sample, LabVessel, and other entities.
     *
     * @param inputStream the spreadsheet inputStream.
     * @param overwrite   specifies if existing entities should be overwritten.
     * @param processor   the TableProcessor subclass that should parse the spreadsheet.
     * @param messages    the errors, warnings, and info to be passed back.
     * @param afterParse  a callback that lets test code to change spreadsheet data after it's been parsed.
     */
    public List<SampleInstanceEntity> doExternalUpload(InputStream inputStream, boolean overwrite,
            ExternalLibraryProcessor processor, MessageCollection messages, Runnable afterParse) {

        messages.clearAll();
        if (processor == null) {
            messages.addError("Missing spreadsheet parser.");
        }
        try {
            PoiSpreadsheetParser.processSingleWorksheet(inputStream, processor);
            if (afterParse != null) {
                afterParse.run();
            }
            messages.addErrors(processor.getMessages());
            messages.addWarning(processor.getWarnings());
            List<RowDto> rowDtos = processor.makeDtos(messages);
            if (rowDtos.isEmpty()) {
                messages.addWarning("Spreadsheet contains no valid data.");
            } else {
                // Makes maps of samples, vessels, and other primary entities.
                makeEntityMaps(processor, rowDtos);
                // Validates the character set used in spreadsheet values that get passed on as-is.
                processor.validateCharacterSet(rowDtos, messages);
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
        } catch (Throwable e) {
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
    private void makeEntityMaps(ExternalLibraryProcessor processor, List<RowDto> rowDtos) {
        Set<String> samplesToLookup = new HashSet<>();
        Set<String> barcodesToLookup = new HashSet<>();

        for (RowDto dto : rowDtos) {
            if (StringUtils.isNotBlank(dto.getSampleName())) {
                samplesToLookup.add(dto.getSampleName());
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
            if (StringUtils.isNotBlank(dto.getBait())) {
                dto.setReagent(reagentDesignDao.findByBusinessKey(dto.getBait()));
            }
            if (StringUtils.isNotBlank(dto.getReferenceSequence())) {
                ReferenceSequence referenceSequence = dto.getReferenceSequence().contains("|") ?
                        referenceSequenceDao.findByBusinessKey(dto.getReferenceSequence()) :
                        referenceSequenceDao.findCurrent(dto.getReferenceSequence());
                if (referenceSequence != null) {
                    processor.getReferenceSequenceMap().put(dto.getReferenceSequence(), referenceSequence);
                }
            }

            if (StringUtils.isNotBlank(dto.getAnalysisTypeName())) {
                AnalysisType analysisType = analysisTypeDao.findByBusinessKey(dto.getAnalysisTypeName());
                if (analysisType != null) {
                    processor.getAnalysisTypeMap().put(dto.getAnalysisTypeName(), analysisType);
                }
            }
        }
        processor.getSampleMap().putAll(mercurySampleDao.findMapIdToMercurySample(samplesToLookup));
        processor.getLabVesselMap().putAll(labVesselDao.findByBarcodes(new ArrayList<>(barcodesToLookup)));

        // A sample data lookup is done on all samples. This returns either Mercury or BSP metadata
        // for each sample, depending on the sample metadata source.
        processor.getFetchedData().putAll(sampleDataFetcher.fetchSampleData(samplesToLookup));

        // Fetches the valid aggregation data type values.
        // todo emp GPLIM-6001 should make this map be String->Entity for only the values present.
        processor.getAggregationDataTypeMap().putAll(productDao.findAggregationDataTypes().stream().
                collect(Collectors.toMap(Function.identity(), Function.identity())));
    }

    /**
     * Validates the walkup sequencing submission and if ok then persists the data in new or existing
     * SampleInstanceEntity, MercurySample, LabVessel, etc.
     *
     * @param walkUpSequencing the data from a walkup sequencing submission.
     * @param messages         collected errors, warnings, info to be passed back.
     */
    public void verifyAndPersistSubmission(WalkUpSequencing walkUpSequencing, MessageCollection messages) {
        if (ExternalLibraryProcessor.asInteger(walkUpSequencing.getFragmentSize()) < 0) {
            messages.addError("Fragment Size must be a non-zero integer or blank");
        }
        if (StringUtils.isBlank(walkUpSequencing.getLibraryName())) {
            messages.addError("Sample name is missing.");
        }
        if (StringUtils.isBlank(walkUpSequencing.getTubeBarcode())) {
            messages.addError("Tube barcode is missing.");
        }

        ReferenceSequence referenceSequence = StringUtils.isBlank(walkUpSequencing.getReferenceVersion()) ?
                referenceSequenceDao.findCurrent(walkUpSequencing.getReference()) :
                referenceSequenceDao.findByNameAndVersion(walkUpSequencing.getReference(),
                        walkUpSequencing.getReferenceVersion());
        if (StringUtils.isNotBlank(walkUpSequencing.getReference()) && referenceSequence == null) {
            messages.addError("Unknown Reference Sequence '" + walkUpSequencing.getReference() + "'" +
                    (StringUtils.isNotBlank(walkUpSequencing.getReferenceVersion()) ?
                            " version '" + walkUpSequencing.getReferenceVersion() + "'" : ""));
        }

        ReagentDesign baitSet = null;
        if (StringUtils.isNotBlank(walkUpSequencing.getBaitSetName())) {
            baitSet = reagentDesignDao.findByBusinessKey(walkUpSequencing.getBaitSetName());
            if (baitSet == null) {
                messages.addError("Unknown Bait Reagent '" + walkUpSequencing.getBaitSetName() + "'");
            }
        }

        IlluminaFlowcell.FlowcellType sequencerModel = null;
        if (StringUtils.isNotBlank(walkUpSequencing.getIlluminaTech())) {
            sequencerModel = IlluminaFlowcell.FlowcellType.getByTechnology(walkUpSequencing.getIlluminaTech());
            if (sequencerModel == null) {
                messages.addError("Unknown Sequencing Technology '" + walkUpSequencing.getIlluminaTech() + "'");
            }
        }

        AnalysisType analysisType = null;
        if (StringUtils.isNotBlank(walkUpSequencing.getAnalysisType())) {
            analysisType = analysisTypeDao.findByBusinessKey(walkUpSequencing.getAnalysisType());
            if (analysisType == null) {
                messages.addError("Unknown Data Analysis Type '" + walkUpSequencing.getAnalysisType() + "'");
            }
        }

        if (!messages.hasErrors()) {
            List<Object> newEntities = new ArrayList<>();

            LabVessel labVessel = labVesselDao.findByIdentifier(walkUpSequencing.getTubeBarcode());
            if (labVessel == null) {
                labVessel = new BarcodedTube(walkUpSequencing.getTubeBarcode(),
                        BarcodedTube.BarcodedTubeType.MatrixTube);
                newEntities.add(labVessel);
            }
            SampleInstanceEjb.addLibrarySize(labVessel, ExternalLibraryProcessor.asNonNegativeBigDecimal(
                    walkUpSequencing.getFragmentSize(), "", 0, null));
            labVessel.setVolume(ExternalLibraryProcessor.asNonNegativeBigDecimal(walkUpSequencing.getVolume(),
                    "", 0, null));
            // Concentration units are either ng/ul or nM. Apparently the event context tells the users which one it is.
            labVessel.setConcentration(ExternalLibraryProcessor.asNonNegativeBigDecimal(
                    walkUpSequencing.getConcentration(), "", 0, null));
            MercurySample mercurySample = mercurySampleDao.findBySampleKey(walkUpSequencing.getLibraryName());
            if (mercurySample == null) {
                mercurySample = new MercurySample(walkUpSequencing.getLibraryName(),
                        MercurySample.MetadataSource.MERCURY);
                mercurySample.addMetadata(new HashSet<Metadata>() {{
                    add(new Metadata(Metadata.Key.MATERIAL_TYPE, MaterialType.DNA.getDisplayName()));
                }});
            }
            mercurySample.addLabVessel(labVessel);
            newEntities.add(mercurySample);

            SampleInstanceEntity sampleInstanceEntity = sampleInstanceEntityDao.findByName(
                    walkUpSequencing.getLibraryName());
            if (sampleInstanceEntity == null) {
                sampleInstanceEntity = new SampleInstanceEntity();
                sampleInstanceEntity.setSampleLibraryName(walkUpSequencing.getLibraryName());
                newEntities.add(sampleInstanceEntity);
            }
            sampleInstanceEntity.setPairedEndRead(StringUtils.startsWithIgnoreCase(walkUpSequencing.getReadType(),
                    "p"));
            sampleInstanceEntity.setReferenceSequence(referenceSequence);
            sampleInstanceEntity.setUploadDate(walkUpSequencing.getSubmitDate());
            sampleInstanceEntity.setReadLength(Math.max(
                    ExternalLibraryProcessor.asInteger(walkUpSequencing.getReadLength()),
                    ExternalLibraryProcessor.asInteger(walkUpSequencing.getReadLength2())));
            sampleInstanceEntity.setNumberLanes(ExternalLibraryProcessor.asInteger(walkUpSequencing.getLaneQuantity()));
            sampleInstanceEntity.setReagentDesign(baitSet);
            sampleInstanceEntity.setLabVessel(labVessel);
            sampleInstanceEntity.setMercurySample(mercurySample);
            sampleInstanceEntity.setSequencerModel(sequencerModel);
            sampleInstanceEntity.setInsertSize(walkUpSequencing.getFragmentSize());
            sampleInstanceEntity.setAnalysisType(analysisType);

            sampleInstanceEntityDao.persistAll(newEntities);
        }
    }

    private static Comparator<String> BY_ROW_NUMBER = (o1, o2) -> {
        // Does a numeric sort on the row number string, expected to be after the word "Row #".
        int o1Row = o1.contains("Row #") ?
                Integer.parseInt(StringUtils.substringAfter(o1, "Row #").split("[ \\.,;]")[0]) : -1;
        int o2Row = o2.contains("Row #") ?
                Integer.parseInt(StringUtils.substringAfter(o2, "Row #").split("[ \\.,;]")[0]) : -1;
        return (o1Row == o2Row) ? o1.compareTo(o2) : (o1Row - o2Row);
    };

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

    /** Dto containing data from a spreadsheet row. */
    public static class RowDto {
        private String aggregationDataType;
        private String aggregationParticle;
        private String bait;
        private String barcode;
        private String collaboratorParticipantId;
        private String collaboratorSampleId;
        private BigDecimal concentration;
        private String analysisTypeName;
        private Integer fragmentSize;
        private String insertSize;
        private String libraryName;
        private String misName;
        private String organism;
        private Integer readLength;
        private String referenceSequence;
        private String rootSampleName;
        private String sampleName;
        private String sequencingTechnology;
        private String sex;
        private Boolean umisPresent; // null if not given in spreadsheet
        private BigDecimal volume;
        private boolean impliedSampleName = false;

        private int rowNumber;
        private SampleInstanceEntity sampleInstanceEntity;
        private ReagentDesign reagent;

        public RowDto(int rowNumber) {
            this.rowNumber = rowNumber;
        }

        public Integer getFragmentSize() {
            return fragmentSize;
        }

        public void setFragmentSize(Integer fragmentSize) {
            this.fragmentSize = fragmentSize;
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

        public String getInsertSize() {
            return insertSize;
        }

        public void setInsertSize(String insertSize) {
            this.insertSize = insertSize;
        }

        public String getLibraryName() {
            return libraryName;
        }

        public void setLibraryName(String libraryName) {
            this.libraryName = libraryName;
        }

        public String getMisName() {
            return misName;
        }

        public void setMisName(String misName) {
            this.misName = misName;
        }

        public String getOrganism() {
            return organism;
        }

        public void setOrganism(String organism) {
            this.organism = organism;
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

        public String getSampleName() {
            return sampleName;
        }

        public void setSampleName(String sampleName) {
            this.sampleName = sampleName;
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

        public BigDecimal getVolume() {
            return volume;
        }

        public void setVolume(BigDecimal volume) {
            this.volume = volume;
        }

        public String getMaterialType() {
            return MaterialType.DNA.getDisplayName();
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

        public Boolean getUmisPresent() {
            return umisPresent;
        }

        public void setUmisPresent(String umisPresent) {
            this.umisPresent = ExternalLibraryProcessor.asBoolean(umisPresent);
        }

        public String getAggregationDataType() {
            return aggregationDataType;
        }

        public void setAggregationDataType(String aggregationDataType) {
            this.aggregationDataType = aggregationDataType;
        }

        public boolean isImpliedSampleName() {
            return impliedSampleName;
        }

        public void setImpliedSampleName(boolean impliedSampleName) {
            this.impliedSampleName = impliedSampleName;
        }
    }
}
