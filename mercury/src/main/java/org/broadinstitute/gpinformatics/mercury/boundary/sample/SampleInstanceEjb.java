package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.AnalysisTypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.ReferenceSequenceDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleInstanceEntityDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleKitRequestDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryBarcodeUpdate;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessor;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorEzPass;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorNewTech;
import org.broadinstitute.gpinformatics.mercury.control.sample.VesselPooledTubesProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.AnalysisType;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntity;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntityTsk;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleKitRequest;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.broadinstitute.gpinformatics.mercury.presentation.sample.WalkUpSequencing;
import org.broadinstitute.gpinformatics.mercury.presentation.workflow.CreateFCTActionBean;
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleData;

import javax.inject.Inject;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUtil.isInBspFormat;

/**
 * This class handles the external library creation from walkup sequencing web serivce calls, pooled tube
 * spreadsheet upload, and the various external library spreadsheet uploads.
 * In all cases a SampleInstanceEntity and associated MercurySample and LabVessel are created or overwritten.
 */
public class SampleInstanceEjb {
    static final String CONFLICT = "Row #%d conflicting value of %s (found \"%s\", expected \"%s\" %s).";
    static final String MISSING = "Row #%d missing value of %s.";
    static final String WRONG_TYPE = "Row #%d %s must be %s.";
    static final String MUST_NOT_BE = "Row #%d %s must not be %s.";
    static final String ALREADY_EXISTS = "Row #%d %s \"%s\" already exists in %s.";
    static final String UNKNOWN = "Row #%d the value for %s is not in %s.";
    static final String NONEXISTENT = "Row #%d the value for %s \"%s\" does not exist in %s.";
    static final String UNKNOWN_COND = "Row #%d the value for " +
            VesselPooledTubesProcessor.Headers.CONDITIONS.getText() +
            " is not a ticket id for one of the sub-task of %s.";
    static final String DUPLICATE = "Row #%d duplicate value for %s.";
    static final String DUPLICATE_S_M =
            "Row #%d repeats the combination of sample %s and index %s and indicates these tubes should not be pooled.";
    static final String BSP_FORMAT = "Row #%d the new %s \"%s\" must not have a BSP sample name format.";
    static final String MERCURY_FORMAT = "Row #%d the new %s \"%s\" must not have a Mercury barcode format (10 digits).";
    static final String PREXISTING =
            "Row #%d %s named \"%s\" already exists in Mercury; set the Overwrite checkbox to re-upload.";
    static final String PREXISTING_VALUES =
            "Row #%d values for %s already exist in Mercury; set the Overwrite checkbox to re-upload.";
    static final String BSP_METADATA =
            "Row #%d values for %s should be blank because BSP data for sample %s cannot be updated.";
    static final String CONFLICTING_SOURCE = "Row #%d sample \"%s\" has %s metadata source but is %s in BSP.";

    public static final String IS_SUCCESS = "Spreadsheet with %d rows successfully uploaded.";
    public static final String IS_SUCCESS2 = "%d tube barcodes were updated.";
    /** A string of the available sequencer model names. */
    public static final String SEQUENCER_MODELS;

    // These are the only characters allowed in a library or sample name.
    private static final String RESTRICTED_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-_";
    static final String RESTRICTED_MESSAGE = "a-z, A-Z, 0-9, '.', '-', or '_'";

    // A possible value in the IRB Number spreadsheet column.
    public static final String IRB_EXEMPT = "IRB Exempt";

    private static final Map<String, IlluminaFlowcell.FlowcellType> mapSequencerToFlowcellType = new HashMap<>();

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

    /** Constructor used for unit testing. */
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
     *  @param inputStream the spreadsheet inputStream.
     * @param overwrite specifies if existing entities should be overwritten.
     * @param processor the TableProcessor subclass that should parse the spreadsheet.
     * @param messages the errors, warnings, and info to be passed back.
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
            if (processor instanceof VesselPooledTubesProcessor) {
                return processPooledTube((VesselPooledTubesProcessor)processor, messages, overwrite);
            } else if (processor instanceof ExternalLibraryProcessorEzPass) {
                return processLibraries((ExternalLibraryProcessorEzPass)processor, messages, overwrite);
            } else if (processor instanceof ExternalLibraryProcessorNewTech) {
                return processLibraries((ExternalLibraryProcessorNewTech)processor, messages, overwrite);
            } else if (processor instanceof ExternalLibraryBarcodeUpdate) {
                return processBarcodeUpdate((ExternalLibraryBarcodeUpdate)processor, messages, overwrite);
            } else {
                throw new RuntimeException("Unsuported processor type: " + processor.getClass().getCanonicalName());
            }
        } catch (ValidationException e) {
            if (processor.getMessages().isEmpty()) {
                messages.addErrors(Arrays.asList(e.toString().split("\\n")));
            }
            messages.addErrors(processor.getMessages());
            messages.addWarning(processor.getWarnings());
        } catch (Exception e) {
            messages.addError("Cannot process spreadsheet: " + e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return null;
    }

    /**
     * Pooled Tube uploads create SampleInstanceEntities from the user provided spreadsheet. Any errors are
     * put in UI error messages. The main differences between External Library uploads and Pooled tube uploads
     * are that pooled tube spreadsheet must give the tube barcode and sample name, which may or may not be
     * known to Mercury and BSP; a JIRA DEV ticket and the relevant subtask ticket must be given; and a pool
     * can be uploaded by repeating the barcode in multiple spreadsheet rows with different sample names.
     */
    private List<SampleInstanceEntity> processPooledTube(VesselPooledTubesProcessor processor,
            MessageCollection messages, boolean overWriteFlag) {

        // rowOffset converts row index to the 1-based row number shown by Excel on the far left of each row.
        int rowOffset = processor.getHeaderRowIndex() + 2;

        // The Broad sample names.
        Set<String> sampleNames = new HashSet<>(processor.getBroadSampleId());
        sampleNames.removeAll(Collections.singletonList(""));
        // The existing samples.
        Map<String, MercurySample> sampleMap = mercurySampleDao.findMapIdToMercurySample(sampleNames);

        // Maps the first occurrence of a sample name to its root sample name. Subsequent occurrences
        // can have a blank root sample name. Self-consistency of the rows is checked later.
        Map<String, String> mapSampleNameToRootName = new HashMap<>();
        for (int rowIndex = 0; rowIndex < processor.getBarcodes().size(); ++rowIndex) {
            String sampleName = StringUtils.trimToEmpty(get(processor.getBroadSampleId(), rowIndex));
            if (isNotBlank(sampleName)) {
                String rootSampleName = StringUtils.trimToEmpty(get(processor.getRootSampleId(), rowIndex));
                mapSampleNameToRootName.put(sampleName, rootSampleName);
            }
        }
        // The existing rootSamples.
        Map<String, MercurySample> rootSampleMap =
                mercurySampleDao.findMapIdToMercurySample(new HashSet<>(mapSampleNameToRootName.values()));

        // A BSP lookup is done on samples that are unknown in Mercury, or that have sampleData
        // source in BSP and either no root was given or spreadsheet metadata is present.
        // The BSP request is batched for efficiency.
        Set<String> bspSampleLookups = new HashSet<>();
        for (int rowIndex = 0; rowIndex < processor.getBarcodes().size(); ++rowIndex) {
            if (!processor.getRequiredValuesPresent().get(rowIndex)) {
                // TableProcessor handles the errors for missing required values. Skip those rows.
                continue;
            }
            String sampleName = get(processor.getBroadSampleId(), rowIndex);
            MercurySample mercurySample = sampleMap.get(sampleName);
            String rootSampleName = mapSampleNameToRootName.get(sampleName);
            if (mercurySample == null || mercurySample.getMetadataSource() == MercurySample.MetadataSource.BSP &&
                    (rootSampleName == null || dataIsPresent(processor, rowIndex))) {
                bspSampleLookups.add(sampleName);
                if (isNotBlank(rootSampleName)) {
                    bspSampleLookups.add(rootSampleName);
                }
            }
        }
        Map<String, SampleData> fetchedData = sampleDataFetcher.fetchSampleData(bspSampleLookups);

        // Maps sample name to a dto of the spreadsheet's sample data.
        Map<String, SampleDataDto> sampleDataDtoMap = new HashMap<>();
        // Map of samples to be created or updated.
        Map<String, SampleDataDto> createOrUpdateSample = new HashMap<>();
        // The unique barcodes in the spreadsheet.
        Set<String> uniqueBarcodes = new HashSet<>(processor.getBarcodes());
        uniqueBarcodes.removeAll(Collections.singletonList(""));
        // Maps tube barcode to existing tubes.
        Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(new ArrayList<>(uniqueBarcodes));
        // Maps to collect the volume and fragment size which must be given for each tube.
        Map<String, BigDecimal> mapBarcodeToVolume = new HashMap<>();
        Map<String, BigDecimal> mapBarcodeToFragmentSize = new HashMap<>();
        // For checking uniqueness of library names.
        Set<String> uniqueLibraryNames = new HashSet<>();
        // For checking molecular index is unique for each sample in a pooled tube.
        Set<String> barcodeAndMis = new HashSet<>();
        Set<String> uniqueSampleMis = new HashSet<>();
        // Maps of the DEV tickets and subtickets.
        Multimap<String, String> experimentConditions = HashMultimap.create();
        Multimap<String, Integer> experimentConditionsRow = HashMultimap.create();
        // The row dtos are the spreadsheet abstraction that are used in the persist phase.
        List<RowDto> rowDtos = new ArrayList<>();

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

        for (int rowIndex = 0; rowIndex < processor.getBarcodes().size(); ++rowIndex) {
            if (!processor.getRequiredValuesPresent().get(rowIndex)) {
                // TableProcessor handles the errors for missing required values. Skip those rows.
                continue;
            }
            // Dto of spreadsheet data for the SampleInstanceEntity.
            RowDto rowDto = new RowDto(rowIndex + rowOffset);
            rowDtos.add(rowDto);

            // Makes a dto of sampleData (metadata) from this spreadsheet row.
            SampleDataDto sampleDataDto = new SampleDataDto(processor, rowIndex);
            String sampleName = sampleDataDto.getSampleName();
            rowDto.setBroadSampleId(sampleName);

            if (isBlank(sampleDataDto.getRootSampleName())) {
                sampleDataDto.setRootSampleName(mapSampleNameToRootName.get(sampleName));
            }
            String rootSampleName = sampleDataDto.getRootSampleName();
            rowDto.setRootSampleId(rootSampleName);

            // Determines the source of sampleData for the Broad sample.
            MercurySample mercurySample = sampleMap.get(sampleName);
            boolean sampleHasMercuryData = (mercurySample != null &&
                    mercurySample.getMetadataSource() == MercurySample.MetadataSource.MERCURY) ||
                    !fetchedData.containsKey(sampleName);
            if (sampleHasMercuryData && fetchedData.containsKey(sampleName)) {
                messages.addWarning(String.format(CONFLICTING_SOURCE, rowIndex + rowOffset, sampleName,
                        "Mercury", "also"));
            } else if (!sampleHasMercuryData && !fetchedData.containsKey(sampleName)) {
                messages.addError(String.format(CONFLICTING_SOURCE, rowIndex + rowOffset, sampleName,
                        "BSP", "not"));
            }
            MercurySample rootSample = rootSampleMap.get(rootSampleName);
            boolean rootHasMercuryData = sampleHasMercuryData &&
                    ((rootSample != null && rootSample.getMetadataSource() == MercurySample.MetadataSource.MERCURY) ||
                            !fetchedData.containsKey(rootSampleName));
            if (rootHasMercuryData && fetchedData.containsKey(rootSampleName)) {
                messages.addWarning(String.format(CONFLICTING_SOURCE, rowIndex + rowOffset, rootSampleName,
                        "Mercury", "also"));
            } else if (!rootHasMercuryData && fetchedData.get(rootSampleName) == null) {
                messages.addError(String.format(CONFLICTING_SOURCE, rowIndex + rowOffset, rootSampleName,
                        "BSP", "not"));
            }

            // If a non-BSP Broad Sample needs to be created its name must not collide with a future BSP SM-id.
            if (mercurySample == null && sampleHasMercuryData && isInBspFormat(sampleName)) {
                messages.addError(String.format(BSP_FORMAT, rowDto.getRowNumber(), "Broad Sample", sampleName));
            }

            // If the sample appears in multiple spreadsheet rows, the sample data values after the first
            // occurrence must match, or be blank. The first occurrence must have all of the sample data fields.
            if (sampleDataDtoMap.containsKey(sampleName)) {
                sampleDataDto.compareTo(sampleDataDtoMap.get(sampleName), rowIndex + rowOffset, messages);
                sampleDataDto = sampleDataDtoMap.get(sampleName);
            } else {
                // Sets the MercurySample sampleData source.
                sampleDataDto.setMetadataSource(sampleHasMercuryData ?
                        MercurySample.MetadataSource.MERCURY : MercurySample.MetadataSource.BSP);
                sampleDataDtoMap.put(sampleName, sampleDataDto);
            }

            // Checks existing sampleData against spreadsheet metadata. If there are differences, it's an
            // error if the sample is in BSP (i.e. a MercurySample with BSP metadata source), or if the
            // sample's root is in BSP (i.e. a MercurySample with Mercury metadata having a root sample
            // that has BSP metadata source) except in this case the lsid can be be different on the
            // Broad sample.
            //
            // For Mercury sampleData and having a root with Mercury sampleData, any metadata differences
            // in the spreadsheet will cause an update, provided the overwrite flag is set.
            //
            // A blank spreadsheet value is ignored and is not a difference.

            SampleDataDto existingSampleData = new SampleDataDto();
            if (mercurySample != null) {
                existingSampleData.addMissingValues(mercurySample.getSampleData());
            }
            existingSampleData.addMissingValues(fetchedData.get(sampleName));
            if (rootSample != null) {
                existingSampleData.addMissingValues(rootSample.getSampleData());
            }
            existingSampleData.addMissingValues(fetchedData.get(rootSampleName));

            Map<VesselPooledTubesProcessor.Headers, String> sampleDataDiffs =
                    sampleDataDto.sampleDataDiffs(existingSampleData);
            if (!sampleDataDiffs.isEmpty()) {
                boolean allowUpdates = sampleHasMercuryData && rootHasMercuryData;
                boolean allowLsidUpdates = sampleHasMercuryData;
                if (!allowUpdates) {
                    Set<VesselPooledTubesProcessor.Headers> headers = new HashSet<>(sampleDataDiffs.keySet());
                    if (allowLsidUpdates) {
                        headers.remove(VesselPooledTubesProcessor.Headers.LSID);
                    }
                    if (!headers.isEmpty()) {
                        messages.addError(String.format(BSP_METADATA, rowIndex + rowOffset,
                                collectHeaderNames(headers), sampleName));
                    }
                } else if (sampleMap.containsKey(sampleName)) {
                    if (!overWriteFlag) {
                        messages.addError(String.format(PREXISTING_VALUES, rowIndex + rowOffset,
                                collectHeaderNames(sampleDataDiffs.keySet())));
                    } else {
                        createOrUpdateSample.put(sampleName, sampleDataDto.makeOnlyDiffs(sampleDataDiffs));
                    }
                }
            }

            // Indicates a MercurySample needs to be created for the Broad sample.
            if (!sampleMap.containsKey(sampleName)) {
                sampleDataDto.addMissingValues(existingSampleData);
                createOrUpdateSample.put(sampleName, sampleDataDto);
                if (sampleDataDto.getMetadataSource() == MercurySample.MetadataSource.MERCURY) {
                    sampleDataDto.validate(rowIndex + rowOffset, messages);
                    // Indicates a MercurySample needs to be created for the root sample.
                    if (!fetchedData.containsKey(rootSampleName) && !rootSampleMap.containsKey(rootSampleName)) {
                        createOrUpdateSample.put(rootSampleName, sampleDataDto);
                    }
                }
            }

            String libraryName = processor.getLibraryName().get(rowIndex);
            rowDto.setLibraryName(libraryName);
            if (!uniqueLibraryNames.add(libraryName)) {
                messages.addError(String.format(DUPLICATE, rowDto.getRowNumber(),
                        VesselPooledTubesProcessor.Headers.LIBRARY_NAME.getText()));
            }
            if (sampleInstanceEntityDao.findByName(libraryName) != null && !overWriteFlag) {
                messages.addError(String.format(PREXISTING, rowDto.getRowNumber(), "Library", libraryName));
            }

            String barcode = processor.getBarcodes().get(rowIndex);
            rowDto.setBarcode(barcode);
            LabVessel tube = mapBarcodeToVessel.get(barcode);
            if (tube != null) {
                if (!overWriteFlag) {
                    messages.addError(String.format(PREXISTING, rowDto.getRowNumber(), "Tube", barcode));
                }
            } else {
                // A new tube must not have a barcode that may someday collide with Mercury, so
                // disallow a 10-digit barcode. The barcode character set is restricted.
                if (!StringUtils.containsOnly(barcode, RESTRICTED_CHARS)) {
                    messages.addError(String.format(WRONG_TYPE, rowDto.getRowNumber(), "Tube barcode",
                            "composed of " + RESTRICTED_MESSAGE));
                }
                if (barcode.length() == 10 && barcode.matches("[0-9]+")) {
                    messages.addError(String.format(MERCURY_FORMAT, rowDto.getRowNumber(), "Tube barcode", barcode));
                }
            }

            String volume = get(processor.getVolume(), rowIndex);
            if (isNotBlank(volume)) {
                if (!NumberUtils.isNumber(volume) || NumberUtils.createDouble(volume) <= 0) {
                    messages.addError(String.format(WRONG_TYPE, rowIndex + rowOffset,
                            VesselPooledTubesProcessor.Headers.VOLUME.getText(), "a positive number"));
                    // Adds barcode to prevent the error for missing volume.
                    mapBarcodeToVolume.put(barcode, null);
                } else {
                    BigDecimal bigDecimal = MathUtils.scaleTwoDecimalPlaces(new BigDecimal(volume));
                    // Checks for consistency with previous value for this tube found in the spreadsheet.
                    if (mapBarcodeToVolume.containsKey(barcode)) {
                        if (!bigDecimal.equals(mapBarcodeToVolume.get(barcode))) {
                            messages.addError(String.format(CONFLICT, rowIndex + rowOffset,
                                    VesselPooledTubesProcessor.Headers.VOLUME.getText(), bigDecimal.toPlainString(),
                                    mapBarcodeToVolume.get(barcode).toPlainString(), ""));
                        }
                    } else {
                        mapBarcodeToVolume.put(barcode, bigDecimal);
                    }
                }
            }

            String fragmentSize = get(processor.getFragmentSize(), rowIndex);
            if (isNotBlank(fragmentSize)) {
                if (!NumberUtils.isNumber(fragmentSize) || NumberUtils.createDouble(fragmentSize) <= 0) {
                    messages.addError(String.format(WRONG_TYPE, rowIndex + rowOffset,
                            VesselPooledTubesProcessor.Headers.FRAGMENT_SIZE.getText(), "a positive number"));
                    // Adds barcode to prevent the error for missing fragment size.
                    mapBarcodeToFragmentSize.put(barcode, null);
                } else {
                    BigDecimal bigDecimal = MathUtils.scaleTwoDecimalPlaces(new BigDecimal(fragmentSize));
                    // Checks for consistency with previous value for this tube found in the spreadsheet.
                    if (mapBarcodeToFragmentSize.containsKey(barcode)) {
                        if (!bigDecimal.equals(mapBarcodeToFragmentSize.get(barcode))) {
                            messages.addError(String.format(CONFLICT, rowIndex + rowOffset,
                                    VesselPooledTubesProcessor.Headers.FRAGMENT_SIZE.getText(),
                                    bigDecimal.toPlainString(),
                                    mapBarcodeToFragmentSize.get(barcode).toPlainString(), ""));
                        }
                    } else {
                        mapBarcodeToFragmentSize.put(barcode, bigDecimal);
                    }
                }
            }

            //Does molecular index scheme exist.
            String misName = processor.getMolecularBarcodeName().get(rowIndex);
            MolecularIndexingScheme molecularIndexingScheme = molecularIndexingSchemeDao.findByName(misName);
            rowDto.setMolecularIndexSchemes(molecularIndexingScheme);
            if (molecularIndexingScheme == null) {
                messages.addError(String.format(UNKNOWN, rowDto.getRowNumber(),
                        VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText(), "Mercury"));
            }
            // Errors if a tube has duplicate Molecular Index Scheme.
            if (!barcodeAndMis.add(barcode + " " + misName)) {
                messages.addError(String.format(DUPLICATE, rowDto.getRowNumber(),
                        VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText() +
                                " in tube " + barcode));
            }
            // Warns if the spreadsheet has duplicate combination of Broad Sample and Molecular Index Scheme
            // (in different tubes). It's not an error as long as the tubes don't get pooled later on, which
            // isn't known at upload time.
            String sampleMis = sampleName + " " + misName;
            if (!uniqueSampleMis.add(sampleMis)) {
                messages.addWarning(String.format(DUPLICATE_S_M, rowDto.getRowNumber(), sampleName, misName));
            }

            // Either bait or cat may be specified, or neither.
            String bait = get(processor.getBait(), rowIndex);
            String cat = get(processor.getCat(), rowIndex);
            ReagentDesign reagentDesign = null;
            if (!bait.isEmpty() && !cat.isEmpty()) {
                messages.addError(String.format(MUST_NOT_BE, rowDto.getRowNumber(),
                        VesselPooledTubesProcessor.Headers.BAIT.getText() + " and " +
                                VesselPooledTubesProcessor.Headers.CAT.getText(), "both defined"));

            } else if (!bait.isEmpty()) {
                reagentDesign = reagentDesignDao.findByBusinessKey(bait);
                if (reagentDesign == null) {
                    messages.addError(String.format(UNKNOWN, rowDto.getRowNumber(),
                            VesselPooledTubesProcessor.Headers.BAIT.getText(), "Mercury"));
                }
            } else if (!cat.isEmpty()) {
                reagentDesign = reagentDesignDao.findByBusinessKey(cat);
                if (reagentDesign == null) {
                    messages.addError(String.format(UNKNOWN, rowDto.getRowNumber(),
                            VesselPooledTubesProcessor.Headers.CAT.getText(), "Mercury"));
                }
            }
            rowDto.setReagent(reagentDesign);

            // Collects the Jira ticket experiment and Jira sub-task conditions, to validate later.
            String experiment = get(processor.getExperiment(), rowIndex);
            rowDto.setExperiment(experiment);
            if (isBlank(experiment)) {
                messages.addError(String.format(MISSING, rowDto.getRowNumber(),
                        VesselPooledTubesProcessor.Headers.EXPERIMENT.getText()));
            }

            List<String> conditions = get(processor.getConditions(), rowIndex);
            rowDto.setConditions(conditions);
            if (CollectionUtils.isEmpty(conditions)) {
                messages.addError(String.format(MISSING, rowDto.getRowNumber(),
                        VesselPooledTubesProcessor.Headers.CONDITIONS.getText()));
            } else {
                experimentConditions.putAll(experiment, conditions);
                // Collects all row numbers that have the same experiment & condition.
                for (String condition : conditions) {
                    experimentConditionsRow.put(experiment + " " + condition, rowIndex + rowOffset);
                }
            }

            if (isNotBlank(get(processor.getReadLength(), rowIndex))) {
                if (NumberUtils.isNumber(processor.getReadLength().get(rowIndex)) &&
                        Integer.parseInt(processor.getReadLength().get(rowIndex)) > 0) {
                    rowDto.setReadLength(Integer.parseInt(processor.getReadLength().get(rowIndex)));
                } else {
                    messages.addError(String.format(WRONG_TYPE, rowDto.getRowNumber(),
                            VesselPooledTubesProcessor.Headers.READ_LENGTH.getText(), "a positive integer"));
                }
            }
        }

        // Each tube must have a value for volume and fragment size.
        for (int rowIndex = 0; rowIndex < processor.getBarcodes().size(); ++rowIndex) {
            if (!processor.getRequiredValuesPresent().get(rowIndex)) {
                // TableProcessor handles the errors for missing required values. Skip those rows.
                continue;
            }
            String barcode = processor.getBarcodes().get(rowIndex);
            if (!mapBarcodeToVolume.containsKey(barcode)) {
                messages.addError(String.format(MISSING, rowIndex + rowOffset,
                        VesselPooledTubesProcessor.Headers.VOLUME.getText()));
                // Adds barcode so the error only is shown for the first row encountered.
                mapBarcodeToVolume.put(barcode, null);
            }
            if (!mapBarcodeToFragmentSize.containsKey(barcode)) {
                messages.addError(String.format(MISSING, rowIndex + rowOffset,
                        VesselPooledTubesProcessor.Headers.FRAGMENT_SIZE.getText()));
                // Adds barcode so the error only is shown for the first row encountered.
                mapBarcodeToFragmentSize.put(barcode, null);
            }
        }

        // Iterates on experiment and its conditions and checks for their existence.
        for (String experiment : experimentConditions.keySet()) {
            // Gets the Experiment JIRA ticket.
            JiraIssue jiraIssue = null;
            try {
                jiraIssue = jiraService.getIssueInfo(experiment, (String[]) null);
            } catch (Exception e) {
                // Silently ignores any Jira exception.
            }
            if (jiraIssue == null) {
                // Errors the rows that have this unknown experiment.
                for (int i = 0; i < rowDtos.size(); ++i) {
                    if (experiment.equals(rowDtos.get(i).getExperiment())) {
                        messages.addError(String.format(UNKNOWN, i + rowOffset,
                                VesselPooledTubesProcessor.Headers.EXPERIMENT.getText(), "JIRA DEV"));
                    }
                }
            } else {
                // Checks that the Conditions are listed as sub-tasks on the Experiment JIRA ticket.
                Collection<String> jiraSubTasks = CollectionUtils.emptyIfNull(jiraIssue.getSubTaskKeys());
                Set<String> invalidConditions = new HashSet<>(experimentConditions.get(experiment));
                invalidConditions.removeAll(jiraSubTasks);
                SortedSet<Integer> rowNumbers = new TreeSet<>();
                for (String condition : invalidConditions) {
                    rowNumbers.addAll(experimentConditionsRow.get(experiment + " " + condition));
                }
                for (Integer rowNumber : rowNumbers) {
                    messages.addError(String.format(UNKNOWN_COND, rowNumber, experiment));
                }
            }
        }

        List<SampleInstanceEntity> sampleInstanceEntities;
        if (!messages.hasErrors()) {
            // Passes a combined map of Broad and root samples.
            sampleMap.putAll(rootSampleMap);
            sampleInstanceEntities = persistPooledTubes(rowDtos, mapBarcodeToVessel, mapBarcodeToVolume,
                    mapBarcodeToFragmentSize, sampleMap, createOrUpdateSample, messages);
        } else {
            sampleInstanceEntities = Collections.emptyList();
        }
        Collections.sort(messages.getErrors(), BY_ROW_NUMBER);
        return sampleInstanceEntities;
    }

    /**
     * Saves PooledTube uploads into LabVessels and SampleInstanceEntities.
     * @param mapBarcodeToVessel       contain only tube barcodes of existing lab vessels.
     * @param mapBarcodeToVolume       contains all tube barcodes and their volumes.
     * @param mapBarcodeToFragmentSize contains all tube barcodes and their fragment size.
     * @param combinedSampleMap        contains all broad and root sample names and their mercury samples.
     * @param createOrUpdateSample     the sample data for samples which need to be created as MercurySample.
     */
    private List<SampleInstanceEntity> persistPooledTubes(List<RowDto> rowDtos,
            Map<String, LabVessel> mapBarcodeToVessel, Map<String, BigDecimal> mapBarcodeToVolume,
            Map<String, BigDecimal> mapBarcodeToFragmentSize, Map<String, MercurySample> combinedSampleMap,
            Map<String, SampleDataDto> createOrUpdateSample, MessageCollection messages) {

        // Creates any tubes needed. Sets tube volume and fragment size on all tubes.
        List<LabVessel> newLabVessels = new ArrayList<>();
        for (String barcode : mapBarcodeToVolume.keySet()) {
            LabVessel labVessel = mapBarcodeToVessel.get(barcode);
            if (labVessel == null) {
                labVessel = new BarcodedTube(barcode, BarcodedTube.BarcodedTubeType.MatrixTube);
                newLabVessels.add(labVessel);
                mapBarcodeToVessel.put(barcode, labVessel);
            } else {
                labVessel.getMercurySamples().clear();
            }
            labVessel.setVolume(mapBarcodeToVolume.get(barcode));
            addLibrarySize(labVessel, mapBarcodeToFragmentSize.get(barcode));
        }

        // Creates the new samples and updates existing samples. Updates will add new metadata,
        // replace existing metadata values, but will not remove existing keys. Does the roots first.
        Set<String> rootNames = new HashSet<>();
        for (Map.Entry<String, SampleDataDto> entry : createOrUpdateSample.entrySet()) {
            String rootSampleName = entry.getValue().getRootSampleName();
            if (isNotBlank(rootSampleName)) {
                rootNames.add(rootSampleName);
            }
        }

        for (boolean doRoots : new boolean[]{true, false}) {
            for (String name : createOrUpdateSample.keySet()) {
                if (doRoots == rootNames.contains(name)) {
                    SampleDataDto sampleDataDto = createOrUpdateSample.get(name);
                    MercurySample mercurySample = combinedSampleMap.get(name);

                    Set<Metadata> metadata = new HashSet<>();
                    if (mercurySample == null) {
                        metadata.add(new Metadata(Metadata.Key.MATERIAL_TYPE, MaterialType.DNA.getDisplayName()));
                    }
                    if (isNotBlank(sampleDataDto.getCollaboratorSampleId())) {
                        metadata.add(new Metadata(Metadata.Key.SAMPLE_ID, sampleDataDto.getCollaboratorSampleId()));
                    }
                    if (isNotBlank(sampleDataDto.getBroadParticipantId())) {
                        metadata.add(new Metadata(Metadata.Key.BROAD_PARTICIPANT_ID,
                                sampleDataDto.getBroadParticipantId()));
                    }
                    if (isNotBlank(sampleDataDto.getCollaboratorParticipantId())) {
                        metadata.add(new Metadata(Metadata.Key.PATIENT_ID,
                                sampleDataDto.getCollaboratorParticipantId()));
                    }
                    if (isNotBlank(sampleDataDto.getGender())) {
                        metadata.add(new Metadata(Metadata.Key.GENDER, sampleDataDto.getGender()));
                    }
                    if (isNotBlank(sampleDataDto.getSpecies())) {
                        metadata.add(new Metadata(Metadata.Key.SPECIES, sampleDataDto.getSpecies()));
                    }
                    if (isNotBlank(sampleDataDto.getLsid())) {
                        metadata.add(new Metadata(Metadata.Key.LSID, sampleDataDto.getLsid()));
                    }

                    if (mercurySample == null) {
                        mercurySample = new MercurySample(name, metadata);
                    } else {
                        // Adds new values or replaces existing values in the map of metadata.
                        mercurySample.updateMetadata(metadata);
                    }

                    // Links to the root, but not when this sample is its own root.
                    String rootSampleName = sampleDataDto.getRootSampleName();
                    if (isNotBlank(rootSampleName) && !rootSampleName.equals(name)) {
                        ((MercurySampleData) mercurySample.getSampleData()).setRootSampleId(rootSampleName);
                    }

                    combinedSampleMap.put(name, mercurySample);
                }
            }
        }

        List<SampleInstanceEntity> sampleInstanceEntities = new ArrayList<>();

        for (RowDto rowDto : rowDtos) {
            String sampleName = rowDto.getBroadSampleId();
            MercurySample mercurySample = combinedSampleMap.get(sampleName);
            LabVessel labVessel = mapBarcodeToVessel.get(rowDto.getBarcode());

            SampleInstanceEntity sampleInstanceEntity = sampleInstanceEntityDao.findByName(rowDto.getLibraryName());
            if (sampleInstanceEntity == null) {
                sampleInstanceEntity = new SampleInstanceEntity();
                sampleInstanceEntity.setUploadDate(new Date());
            }
            sampleInstanceEntity.setMercurySample(combinedSampleMap.get(rowDto.getBroadSampleId()));
            sampleInstanceEntity.setRootSample(combinedSampleMap.get(rowDto.getRootSampleId()));
            sampleInstanceEntity.setMolecularIndexingScheme(rowDto.getMolecularIndexSchemes());
            sampleInstanceEntity.setSampleLibraryName(rowDto.getLibraryName());
            sampleInstanceEntity.setReagentDesign(rowDto.getReagent());
            sampleInstanceEntity.setReadLength(rowDto.getReadLength());
            sampleInstanceEntity.setExperiment(rowDto.getExperiment());
            sampleInstanceEntity.setLabVessel(labVessel);

            // Persists the experiment conditions in the order they appeared on the row.
            sampleInstanceEntity.removeSubTasks();
            int orderOfCreation = 0;
            for (String subTask : rowDto.getConditions()) {
                SampleInstanceEntityTsk sampleInstanceEntityTsk = new SampleInstanceEntityTsk();
                sampleInstanceEntityTsk.setSubTask(subTask);
                sampleInstanceEntityTsk.setOrderOfCreation(orderOfCreation++);
                sampleInstanceEntity.addSubTasks(sampleInstanceEntityTsk);
            }

            sampleInstanceEntities.add(sampleInstanceEntity);

            // Puts links in the lab vessel.
            labVessel.addSample(mercurySample);
            labVessel.getSampleInstanceEntities().add(sampleInstanceEntity);
        }

        labVesselDao.persistAll(newLabVessels);
        sampleInstanceEntityDao.persistAll(sampleInstanceEntities);

        if (!rowDtos.isEmpty()) {
            messages.addInfo(String.format(IS_SUCCESS, rowDtos.size()));
        } else {
            messages.addError("No valid data found.");
        }
        return sampleInstanceEntities;
    }


    /**
     * Verifies and persists External Library Upload spreadsheet. Unlike PooledTube uploads, External
     * Library uploads have no sample names, so the library name is used for that. All sample metadata
     * must be present on every row since samples are expected to be unknown to Mercury and BSP (except
     * when overwriting a previous external upload). The tube barcode is optional in the spreadsheet.
     * If it is provided it must not already exist in Mercury (unless a previous upload is being
     * overwritten), and if not provided then the library name is used, though it's likely that the
     * tube barcode will be updated by the lab user to the actual value in a later step.
     */
    private List<SampleInstanceEntity> processLibraries(ExternalLibraryProcessor processor,
            MessageCollection messages, boolean overWriteFlag) {

        // Includes error messages due to missing header or value that were discovered when parsing the spreadsheet.
        messages.addErrors(processor.getMessages());

        Set<String> uniqueBarcodes = new HashSet<>();
        Set<String> uniqueLibraryNames = new HashSet<>();

        Map<Integer, AnalysisType> analysisTypes = new HashMap<>();
        Map<Integer, MolecularIndexingScheme> molecularIndexingSchemes = new HashMap<>();
        Map<Integer, LabVessel> labVessels = new HashMap<>();
        Map<Integer, MercurySample> mercurySamples = new HashMap<>();
        Map<Integer, SampleInstanceEntity> sampleInstanceEntities = new HashMap<>();
        Map<Integer, ReferenceSequence> referenceSequences = new HashMap<>();
        Map<Integer, IlluminaFlowcell.FlowcellType> sequencerModels = new HashMap<>();

        int entityCount = processor.getLibraryName().size();
        for (int index = 0; index < entityCount; ++index) {
            // Converts the 0-based index into the spreadsheet row number shown at the far left side in Excel.
            int rowNumber = index + processor.getHeaderRowIndex() + 2;

            // Library name must only appear in one row, and is assumed to be universally unique. If it is
            // reused then it's unusable unless Overwrite is set. The pipeline and elsewhere require a
            // simple name so disallow whitespace and anything that might cause trouble.
            String libraryName = get(processor.getLibraryName(), index);
            if (!uniqueLibraryNames.add(libraryName)) {
                messages.addError(String.format(DUPLICATE, rowNumber, "Library Name"));
            }
            if (!StringUtils.containsOnly(libraryName, RESTRICTED_CHARS)) {
                messages.addError(String.format(WRONG_TYPE, rowNumber, "Library Name",
                        "composed of " + RESTRICTED_MESSAGE));
            }
            // Disallow a library name that could cause the sample to collide with an existing or future BSP sample.
            if (isInBspFormat(libraryName)) {
                messages.addError(String.format(BSP_FORMAT, rowNumber, "Library Name", libraryName));
            }

            SampleInstanceEntity sampleInstanceEntity = sampleInstanceEntityDao.findByName(libraryName);
            if (sampleInstanceEntity != null) {
                sampleInstanceEntities.put(index, sampleInstanceEntity);
                if (!overWriteFlag) {
                    messages.addError(String.format(PREXISTING, rowNumber, "Library", libraryName));
                }
            }

            // Library name is used as the sample name.
            MercurySample mercurySample = mercurySampleDao.findBySampleKey(libraryName);
            if (mercurySample != null) {
                mercurySamples.put(index, mercurySample);
                if (!overWriteFlag) {
                    messages.addError(String.format(PREXISTING, rowNumber, "Sample", libraryName));
                }
            }

            String barcode = get(processor.getBarcodes(), index);
            // Uses the libraryName if a barcode is not present.
            boolean barcodeIsLibrary = isBlank(barcode);
            if (barcodeIsLibrary) {
                barcode = libraryName;
            }
            // Tube barcode must be unique in the spreadsheet.
            if (!uniqueBarcodes.add(barcode)) {
                messages.addError(String.format(DUPLICATE, rowNumber,
                        ExternalLibraryProcessorEzPass.Headers.TUBE_BARCODE.getText()));
            }
            LabVessel labVessel = labVesselDao.findByIdentifier(barcode);
            if (labVessel != null) {
                labVessels.put(index, labVessel);
                if (!overWriteFlag) {
                    messages.addError(String.format(PREXISTING, rowNumber,
                            ExternalLibraryProcessorEzPass.Headers.TUBE_BARCODE.getText(), barcode));
                }
            } else {
                // A new tube must not have a barcode that may someday collide with Mercury, so
                // disallow a 10-digit barcode. The barcode character set is restricted.
                if (!StringUtils.containsOnly(barcode, RESTRICTED_CHARS)) {
                    messages.addError(String.format(WRONG_TYPE, rowNumber, "Tube barcode",
                            "composed of " + RESTRICTED_MESSAGE));
                }
                if (barcode.length() == 10 && barcode.matches("[0-9]+")) {
                    messages.addError(String.format(MERCURY_FORMAT, rowNumber,
                            (barcodeIsLibrary ? "(the Library Name)" : "")));
                }
            }
            if (isNotBlank(get(processor.getVolume(), index)) &&
                    !NumberUtils.isNumber(processor.getVolume().get(index))) {
                messages.addError(String.format(WRONG_TYPE, rowNumber, "Volume", "numeric"));
            }
            if (isNotBlank(get(processor.getConcentration(), index)) &&
                    !NumberUtils.isNumber(processor.getConcentration().get(index))) {
                messages.addError(String.format(WRONG_TYPE, rowNumber, "Concentration", "numeric"));
            }

            nonNegativeOrBlank(get(processor.getReadLength(), index), "Read Length", rowNumber, messages);
            nonNegativeOrBlank(get(processor.getNumberOfLanes(), index), "Numer of Lanes", rowNumber, messages);
            nonNegativeOrBlank(get(processor.getInsertSize(), index), "Insert Size", rowNumber, messages);
            String referenceSequenceName = get(processor.getReferenceSequence(), index);
            if (isNotBlank(referenceSequenceName)) {
                ReferenceSequence referenceSequence = referenceSequenceName.contains("|") ?
                        referenceSequenceDao.findByBusinessKey(referenceSequenceName) :
                        referenceSequenceDao.findCurrent(referenceSequenceName);
                if (referenceSequence == null) {
                    messages.addError(String.format(UNKNOWN, rowNumber, "Reference Sequence", "Mercury"));
                } else {
                    referenceSequences.put(index, referenceSequence);
                }
            }

            String misName = get(processor.getMolecularBarcodeName(), index);
            MolecularIndexingScheme molecularIndexingScheme = molecularIndexingSchemeDao.findByName(misName);
            if (molecularIndexingScheme != null) {
                molecularIndexingSchemes.put(index, molecularIndexingScheme);
            } else if (isNotBlank(misName)) {
                messages.addError(String.format(UNKNOWN, rowNumber, "Molecular Barcode Name", "Mercury"));
            }

            String analysisTypeName = get(processor.getDataAnalysisType(), index);
            if (isNotBlank(analysisTypeName)) {
                AnalysisType analysisType = analysisTypeDao.findByBusinessKey(analysisTypeName);
                if (analysisType == null) {
                    messages.addError(String.format(UNKNOWN, rowNumber, "Data Analysis Type", "Mercury"));
                } else {
                    analysisTypes.put(index, analysisType);
                }
            }

            if (isNotBlank(get(processor.getSequencingTechnology(), index))) {
                IlluminaFlowcell.FlowcellType sequencerModel =
                        findFlowcellType(get(processor.getSequencingTechnology(), index));
                if (sequencerModel == null) {
                    messages.addError(String.format(UNKNOWN, rowNumber, "Sequencing Technology", "Mercury"));
                } else {
                    sequencerModels.put(index, sequencerModel);
                }
            }

        }
        if (entityCount == 0) {
            messages.addWarning("Spreadsheet contains no data.");
        }
        if (!messages.hasErrors()) {
            persistExternalLibraries(processor, analysisTypes, molecularIndexingSchemes, labVessels,
                    mercurySamples, sampleInstanceEntities, referenceSequences, sequencerModels, messages);
        } else {
            Collections.sort(messages.getErrors(), BY_ROW_NUMBER);
        }
        // Returns a list of Sample Instance Entities ordered by row number.
        List<SampleInstanceEntity> list = new ArrayList<>(entityCount);
        for (int index = 0; index < entityCount; ++index) {
            list.add(sampleInstanceEntities.get(index));
        }
        return list;
    }

    /**
     * Persists External Library uploads.
     */
    private void persistExternalLibraries(ExternalLibraryProcessor processor,
            Map<Integer, AnalysisType> analysisTypes, Map<Integer, MolecularIndexingScheme> molecularIndexingSchemes,
            Map<Integer, LabVessel> labVessels, Map<Integer, MercurySample> mercurySamples,
            Map<Integer, SampleInstanceEntity> sampleInstanceEntities,
            Map<Integer, ReferenceSequence> referenceSequences,
            Map<Integer, IlluminaFlowcell.FlowcellType> sequencerModels, MessageCollection messages) {

        // Captures the pre-row one-off spreadsheet data in a "kit request", i.e. the upload manifest.
        SampleKitRequest kit = sampleKitRequestDao.find(processor.getEmail(), processor.getOrganization(),
                processor.getLastName(), processor.getFirstName());
        if (kit == null) {
            kit = new SampleKitRequest();
            kit.setFirstName(processor.getFirstName());
            kit.setLastName(processor.getLastName());
            kit.setOrganization(processor.getOrganization());
            kit.setAddress(processor.getAddress());
            kit.setCity(processor.getCity());
            kit.setState(processor.getState());
            kit.setPostalCode(processor.getZip());
            kit.setCountry(processor.getCountry());
            kit.setPhone(processor.getPhone());
            kit.setEmail(processor.getEmail());
            kit.setCommonName(processor.getCommonName());
            kit.setGenus(processor.getGenus());
            kit.setSpecies(processor.getSpecies());
            kit.setIrbApprovalRequired(processor.getIrbRequired());
            sampleKitRequestDao.persist(kit);
        }

        Collection<Object> newObjects = new ArrayList<>();
        int numberOfEntities = processor.getLibraryName().size();
        for (int index = 0; index < numberOfEntities; ++index) {
            String libraryName = processor.getLibraryName().get(index);

            LabVessel labVessel = labVessels.get(index);
            if (labVessel == null) {
                String barcode = get(processor.getBarcodes(), index);
                if (isBlank(barcode)) {
                    barcode = libraryName;
                }
                labVessel = new BarcodedTube(barcode, BarcodedTube.BarcodedTubeType.MatrixTube);
                newObjects.add(labVessel);
            }
            labVessel.setVolume(MathUtils.scaleTwoDecimalPlaces(new BigDecimal(
                    asNumber(get(processor.getVolume(), index)))));
            labVessel.setConcentration(MathUtils.scaleTwoDecimalPlaces(new BigDecimal(
                    asNumber(get(processor.getConcentration(), index)))));
            if (asInteger(get(processor.getLibrarySize(), index)) > 0) {
                addLibrarySize(labVessel, MathUtils.scaleTwoDecimalPlaces(new BigDecimal(
                        get(processor.getLibrarySize(), index))));
            }

            MercurySample mercurySample = mercurySamples.get(index);
            if (mercurySample == null) {
                mercurySample = new MercurySample(libraryName, MercurySample.MetadataSource.MERCURY);
                newObjects.add(mercurySample);
            }
            Set<Metadata> metadata = new HashSet<>();
            metadata.add(new Metadata(Metadata.Key.SAMPLE_ID, get(processor.getCollaboratorSampleId(), index)));
            String patientId = StringUtils.trimToEmpty(get(processor.getIndividualName(), index));
            metadata.add(new Metadata(Metadata.Key.BROAD_PARTICIPANT_ID, patientId));
            // Ideally collaborator participant id goes into patient id. This is a necessary stand-in.
            metadata.add(new Metadata(Metadata.Key.PATIENT_ID, patientId));

            if (isNotBlank(get(processor.getSex(), index))) {
                metadata.add(new Metadata(Metadata.Key.GENDER, get(processor.getSex(), index)));
            }
            metadata.add(new Metadata(Metadata.Key.SPECIES,
                    StringUtils.trimToEmpty(processor.getGenus() + " " + processor.getSpecies())));
            if (isNotBlank(MaterialType.DNA.getDisplayName())) {
                metadata.add(new Metadata(Metadata.Key.MATERIAL_TYPE, MaterialType.DNA.getDisplayName()));
            }
            mercurySample.addMetadata(metadata);
            mercurySample.addLabVessel(labVessel);

            SampleInstanceEntity sampleInstanceEntity = sampleInstanceEntities.get(index);
            if (sampleInstanceEntity == null) {
                sampleInstanceEntity = new SampleInstanceEntity();
                sampleInstanceEntity.setUploadDate(new Date());
                sampleInstanceEntities.put(index, sampleInstanceEntity);
                newObjects.add(sampleInstanceEntity);
                sampleInstanceEntity.setSampleLibraryName(libraryName);
            }
            // If doing a re-upload of an existing Sample Instance Entity all fields are
            // overwritten except library name.
            sampleInstanceEntity.setSampleKitRequest(kit);
            if (isNotBlank(get(processor.getPooled(), index))) {
                sampleInstanceEntity.setPooled(isTrue(processor.getPooled().get(index)));
            }
            if (isNotBlank(get(processor.getReadLength(), index))) {
                sampleInstanceEntity.setReadLength(asInteger(get(processor.getReadLength(), index)));
            }
            sampleInstanceEntity.setNumberLanes(asInteger(get(processor.getNumberOfLanes(), index)));
            String comments = StringUtils.trimToEmpty(get(processor.getAdditionalSampleInformation(), index));
            if (isNotBlank(get(processor.getAdditionalAssemblyInformation(), index))) {
                if (isNotBlank(comments)) {
                    comments += "; ";
                }
                comments += get(processor.getAdditionalAssemblyInformation(), index);
            }
            sampleInstanceEntity.setComments(comments);
            sampleInstanceEntity.setLibraryType(get(processor.getLibraryType(), index));
            sampleInstanceEntity.setReferenceSequence(referenceSequences.get(index));
            sampleInstanceEntity.setMolecularIndexScheme(molecularIndexingSchemes.get(index));
            sampleInstanceEntity.setLibraryType(get(processor.getLibraryType(), index));
            sampleInstanceEntity.setAnalysisType(analysisTypes.get(index));
            sampleInstanceEntity.setAggregationParticle(get(processor.getProjectTitle(), index));
            sampleInstanceEntity.setLabVessel(labVessel);
            sampleInstanceEntity.setMercurySample(mercurySample);
            sampleInstanceEntity.setSequencerModel(sequencerModels.get(index));
        }
        sampleInstanceEntityDao.persistAll(newObjects);

        messages.addInfo(String.format(IS_SUCCESS, numberOfEntities));
    }

    /**
     * Validates the walkup sequencing submission and if ok then persists the data in new or existing
     * SampleInstanceEntity, MercurySample, LabVessel, etc.
     *
     * @param walkUpSequencing the data from a walkup sequencing submission.
     * @param messages collected errors, warnings, info to be passed back.
     */
    public void verifyAndPersistSubmission(WalkUpSequencing walkUpSequencing, MessageCollection messages) {
        if (StringUtils.isBlank(walkUpSequencing.getEmailAddress())) {
            messages.addError("Email is missing");
        }
        if (StringUtils.isBlank(walkUpSequencing.getLabName())) {
            messages.addError("Lab Name is missing");
        }

        if (asInteger(walkUpSequencing.getFragmentSize()) < 0) {
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
        if (StringUtils.isNotBlank(walkUpSequencing.getFragmentSize())) {
            SampleInstanceEjb.addLibrarySize(labVessel, MathUtils.scaleTwoDecimalPlaces(new BigDecimal(
                    asNumber(walkUpSequencing.getFragmentSize()))));
        }
        labVessel.setVolume(MathUtils.scaleTwoDecimalPlaces(new BigDecimal(asNumber(walkUpSequencing.getVolume()))));
        // Concentration units are either ng/ul or nM. Apparently the event context tells the users which one it is.
        labVessel.setConcentration(MathUtils.scaleTwoDecimalPlaces(new BigDecimal(
                asNumber(walkUpSequencing.getConcentration()))));
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

        SampleInstanceEntity sampleInstanceEntity = sampleInstanceEntityDao.findByName(walkUpSequencing.getLibraryName());
        if (sampleInstanceEntity == null) {
            sampleInstanceEntity = new SampleInstanceEntity();
            sampleInstanceEntity.setSampleLibraryName(walkUpSequencing.getLibraryName());
            sampleInstanceEntity.setSampleKitRequest(sampleKitRequest);
            newEntities.add(sampleInstanceEntity);
        }
        sampleInstanceEntity.setPairedEndRead(StringUtils.startsWithIgnoreCase(walkUpSequencing.getReadType(), "p"));
        sampleInstanceEntity.setReferenceSequence(referenceSequence);
        sampleInstanceEntity.setUploadDate(walkUpSequencing.getSubmitDate());
        sampleInstanceEntity.setReadLength(Math.max(asInteger(walkUpSequencing.getReadLength()),
                asInteger(walkUpSequencing.getReadLength2())));
        sampleInstanceEntity.setNumberLanes(new Integer(asNumber(walkUpSequencing.getLaneQuantity())));
        sampleInstanceEntity.setComments(walkUpSequencing.getComments());
        sampleInstanceEntity.setReagentDesign(reagentDesign);
        sampleInstanceEntity.setLabVessel(labVessel);
        sampleInstanceEntity.setMercurySample(mercurySample);
        sampleInstanceEntity.setPooled(SampleInstanceEjb.isOneOf(walkUpSequencing.getPooledSample(), "y", "yes"));
        sampleInstanceEntity.setSequencerModel(sequencerModel);

        sampleInstanceEntityDao.persistAll(newEntities);
    }

    /** Returns "0" for a blank or non-numeric input. */
    private static String asNumber(String input) {
        return NumberUtils.isNumber(StringUtils.trimToEmpty(input)) ? input : "0";
    }

    /** Converts to integer and returns 0 for a blank or non-numeric input. */
    private static Integer asInteger(String input) {
        return StringUtils.isNumeric(StringUtils.trimToEmpty(input)) ? new Integer(input) : 0;
    }

    /** Returns the list element or null if the list or element doesn't exist */
    private <T> T get(List<T> list, int index) {
        return (CollectionUtils.isNotEmpty(list) && list.size() > index) ? list.get(index) : null;
    }

    private void validateIrbNumber(String irbNumber, ResearchProject researchProject, int rowNumber,
            MessageCollection messages) {
        if (isNotBlank(irbNumber) && !IRB_EXEMPT.equalsIgnoreCase(irbNumber)) {
            if (!researchProject.getIrbNumbers(false).contains(irbNumber)) {
                String identifier = researchProject.hasJiraTicketKey() ?
                        researchProject.getJiraTicketKey() :
                        String.valueOf(researchProject.getResearchProjectId());
                messages.addError(String.format(CONFLICT, rowNumber, "IRB Number", irbNumber,
                        StringUtils.join(researchProject.getIrbNumbers(), "\" or \""),
                        "from ResearchProject " + identifier));
            }
        }
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

    private void nonNegativeOrBlank(String value, String header, int rowNumber, MessageCollection messages) {
        if (isNotBlank(value) && (!StringUtils.isNumeric(value) || Integer.parseInt(value) < 0)) {
            messages.addError(String.format(WRONG_TYPE, rowNumber, header, "either a positive integer or blank"));
        }
    }

    /**
     * Returns true if string indicates a true value, meaning it's a non-zero integer value,
     * or it's one of "t", "true", "y", "yes", ignoring case.
     */
    private static boolean isTrue(String value) {
        return isOneOf(value, "y", "yes", "t", "true") ||
                (StringUtils.isNumeric(value) && Integer.parseInt(value) != 0);
    }

    /** Returns true if string is one of the given testString, ignoring case. */
    private static boolean isOneOf(String value, String... testStrings) {
        for (String testString : testStrings) {
            if (testString.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private static IlluminaFlowcell.FlowcellType findFlowcellType(String sequencerModel) {
        return mapSequencerToFlowcellType.get(sequencerModel);
    }

    /**
     * Adds library size metric to the given lab vessel, but does not add the same value
     * if it already exists for the that vessel.
     */
    private static void addLibrarySize(LabVessel labVessel, BigDecimal librarySize) {
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

    /** Returns true if the Pooled Tube spreadsheet defines any sample metadata for the row. */
    private boolean dataIsPresent(VesselPooledTubesProcessor processor, int rowIndex) {
        return  isNotBlank(get(processor.getCollaboratorSampleId(), rowIndex)) ||
                isNotBlank(get(processor.getCollaboratorParticipantId(), rowIndex)) ||
                isNotBlank(get(processor.getBroadParticipantId(), rowIndex)) ||
                isNotBlank(get(processor.getSex(), rowIndex)) ||
                isNotBlank(get(processor.getOrganism(), rowIndex)) ||
                isNotBlank(get(processor.getLsid(), rowIndex));
    }

    /**
     * Updates existing SampleInstanceEntity tube barcode labels.
     */
    private List<SampleInstanceEntity> processBarcodeUpdate(ExternalLibraryBarcodeUpdate processor,
            MessageCollection messages, boolean overwrite) {

        final int rowOffset = processor.getHeaderRowIndex() + 1;
        Map<LabVessel, String> tubeToNewBarcode = new HashMap<>();
        List<SampleInstanceEntity> sampleInstanceEntities = new ArrayList<>();

        if (!overwrite) {
            messages.addError("Set the Overwrite checkbox to update tube barcodes.");
        }
        for (int rowIdx = 0; rowIdx < processor.getLibraryName().size(); ++rowIdx) {
            String libraryName = get(processor.getLibraryName(), rowIdx);
            String newBarcode = get(processor.getBarcodes(), rowIdx);
            if (isNotBlank(libraryName) && isNotBlank(newBarcode)) {
                SampleInstanceEntity sampleInstanceEntity = sampleInstanceEntityDao.findByName(libraryName);
                if (sampleInstanceEntity == null) {
                    messages.addError(String.format(NONEXISTENT, rowIdx + rowOffset,
                            ExternalLibraryBarcodeUpdate.Headers.LIBRARY_NAME.getText(),
                            libraryName, "Mercury"));
                } else {
                    if (labVesselDao.findByIdentifier(newBarcode) != null) {
                        messages.addError(String.format(ALREADY_EXISTS, rowIdx + rowOffset,
                                ExternalLibraryBarcodeUpdate.Headers.TUBE_BARCODE, newBarcode, "Mercury"));
                    }
                    sampleInstanceEntities.add(sampleInstanceEntity);
                    tubeToNewBarcode.put(sampleInstanceEntity.getLabVessel(), newBarcode);
                }
            }
        }

        // Persist changes if there are no errors.
        if (messages.getErrors().isEmpty()) {
            for (Map.Entry<LabVessel, String> mapEntry : tubeToNewBarcode.entrySet()) {
                mapEntry.getKey().setLabel(mapEntry.getValue());
            }
            labVesselDao.persistAll(tubeToNewBarcode.keySet());
            messages.addInfo(String.format(IS_SUCCESS2, tubeToNewBarcode.size()));
        }

        return sampleInstanceEntities;
    }

    /** Returns a string that can be used as a sequencer value in the spreadsheet. */
    public static String makeSequencerValue(IlluminaFlowcell.FlowcellType flowcellType) {
        return StringUtils.substringBeforeLast(flowcellType.getDisplayName(), "Flowcell").trim();
    }

    /** DTO for sample metadata found in the spreadsheet. */
    class SampleDataDto implements Cloneable {
        private String sampleName;
        private String rootSampleName;
        private String collaboratorSampleId;
        private String collaboratorParticipantId;
        private String broadParticipantId;
        private String gender;
        private String species;
        private String lsid;
        private MercurySample.MetadataSource metadataSource;

        public SampleDataDto() {
        }

        public SampleDataDto(VesselPooledTubesProcessor processor, int rowIndex) {
            sampleName = processor.getBroadSampleId().get(rowIndex);
            rootSampleName = processor.getRootSampleId().get(rowIndex);
            collaboratorSampleId = processor.getCollaboratorSampleId().get(rowIndex);
            collaboratorParticipantId = processor.getCollaboratorParticipantId().get(rowIndex);
            broadParticipantId = processor.getBroadParticipantId().get(rowIndex);
            gender = get(processor.getSex(), rowIndex);
            species = get(processor.getOrganism(), rowIndex);
            lsid = get(processor.getLsid(), rowIndex);
        }

        /** Generates errors and warnings for blank fields. */
        public void validate(int rowNumber, MessageCollection messages) {
            if (StringUtils.isBlank(getSampleName())) {
                messages.addError(String.format(MISSING, rowNumber,
                        VesselPooledTubesProcessor.Headers.LIBRARY_NAME.getText()));
            }
            if (StringUtils.isBlank(getRootSampleName())) {
                messages.addError(String.format(MISSING, rowNumber,
                        VesselPooledTubesProcessor.Headers.ROOT_SAMPLE_ID.getText()));
            }
            if (StringUtils.isBlank(getCollaboratorSampleId())) {
                messages.addError(String.format(MISSING, rowNumber,
                        VesselPooledTubesProcessor.Headers.COLLABORATOR_SAMPLE_ID.getText()));
            }
            if (StringUtils.isBlank(getCollaboratorParticipantId())) {
                messages.addError(String.format(MISSING, rowNumber,
                        VesselPooledTubesProcessor.Headers.COLLABORATOR_PARTICIPANT_ID.getText()));
            }
            if (StringUtils.isBlank(getBroadParticipantId())) {
                messages.addError(String.format(MISSING, rowNumber,
                        VesselPooledTubesProcessor.Headers.BROAD_PARTICIPANT_ID.getText()));
            }
            if (StringUtils.isBlank(getSpecies())) {
                messages.addInfo(String.format(MISSING, rowNumber,
                        VesselPooledTubesProcessor.Headers.SPECIES.getText()));
            }
            if (StringUtils.isBlank(getGender())) {
                messages.addInfo(String.format(MISSING, rowNumber,
                        VesselPooledTubesProcessor.Headers.GENDER.getText()));
            }
            if (StringUtils.isBlank(getLsid())) {
                messages.addInfo(String.format(MISSING, rowNumber,
                        VesselPooledTubesProcessor.Headers.LSID.getText()));
            }
        }

        /**
         * Generates error messages on miscompares.
         */
        public void compareTo(SampleDataDto expected, int rowNumber, MessageCollection messages) {
            compareNonBlankValues(getBroadParticipantId(), expected.getBroadParticipantId(),
                    VesselPooledTubesProcessor.Headers.BROAD_PARTICIPANT_ID.getText(), rowNumber, messages);
            compareNonBlankValues(getRootSampleName(), expected.getRootSampleName(),
                    VesselPooledTubesProcessor.Headers.ROOT_SAMPLE_ID.getText(), rowNumber, messages);
            compareNonBlankValues(getCollaboratorSampleId(), expected.getCollaboratorSampleId(),
                    VesselPooledTubesProcessor.Headers.COLLABORATOR_SAMPLE_ID.getText(), rowNumber, messages);
            compareNonBlankValues(getCollaboratorParticipantId(), expected.getCollaboratorParticipantId(),
                    VesselPooledTubesProcessor.Headers.COLLABORATOR_PARTICIPANT_ID.getText(), rowNumber, messages);
            compareNonBlankValues(getGender(), expected.getGender(),
                    VesselPooledTubesProcessor.Headers.GENDER.getText(), rowNumber, messages);
            compareNonBlankValues(getSpecies(), expected.getSpecies(),
                    VesselPooledTubesProcessor.Headers.SPECIES.getText(), rowNumber, messages);
            compareNonBlankValues(getLsid(), expected.getLsid(),
                    VesselPooledTubesProcessor.Headers.LSID.getText(), rowNumber, messages);
        }

        /** Helper method to compare non-blank actual values with expecteds, and generate error messages. */
        private void compareNonBlankValues(String actual, String expected, String column, int rowNumber,
                MessageCollection messages) {
            if (isNotBlank(actual) && !actual.equals(expected)) {
                messages.addError(String.format(CONFLICT, rowNumber, column, actual, expected, ""));
            }
        }

        /** Compares existing metadata to the spreadsheet metadata and returns any differences. */
        public Map<VesselPooledTubesProcessor.Headers, String> sampleDataDiffs(SampleDataDto expected) {
            Map<VesselPooledTubesProcessor.Headers, String> diffs = new HashMap<>();
            if (expected != null) {
                putDiffs(getBroadParticipantId(), expected.getBroadParticipantId(), diffs,
                        VesselPooledTubesProcessor.Headers.BROAD_PARTICIPANT_ID);
                putDiffs(getRootSampleName(), expected.getRootSampleName(), diffs,
                        VesselPooledTubesProcessor.Headers.ROOT_SAMPLE_ID);
                putDiffs(getCollaboratorSampleId(), expected.getCollaboratorSampleId(), diffs,
                        VesselPooledTubesProcessor.Headers.COLLABORATOR_SAMPLE_ID);
                putDiffs(getCollaboratorParticipantId(), expected.getCollaboratorParticipantId(), diffs,
                        VesselPooledTubesProcessor.Headers.COLLABORATOR_PARTICIPANT_ID);
                putDiffs(getGender(), expected.getGender(), diffs,
                        VesselPooledTubesProcessor.Headers.GENDER);
                putDiffs(getSpecies(), expected.getSpecies(), diffs,
                        VesselPooledTubesProcessor.Headers.SPECIES);
                putDiffs(getLsid(), expected.getLsid(), diffs,
                        VesselPooledTubesProcessor.Headers.LSID);
            }
            return diffs;
        }

        /** Helper method that adds the column name and value to the map when there is difference. */
        private void putDiffs(String spreadsheetValue, String existingValue,
                Map<VesselPooledTubesProcessor.Headers, String> diffs, VesselPooledTubesProcessor.Headers header) {
            // A blank spreadsheet value should not be compared.
            if (isNotBlank(spreadsheetValue) && !spreadsheetValue.equals(existingValue)) {
                diffs.put(header, spreadsheetValue);
            }
        }

        /** Converts sample diffs into a sparse-valued dto used for updating existing sample data. */
        public SampleDataDto makeOnlyDiffs(Map<VesselPooledTubesProcessor.Headers, String> sampleDataDiffs) {
            SampleDataDto dto;
            try {
                dto = (SampleDataDto)clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
            for (VesselPooledTubesProcessor.Headers header : sampleDataDiffs.keySet()) {
                String value = sampleDataDiffs.get(header);
                if (isNotBlank(value)) {
                    switch (header) {
                    case BROAD_PARTICIPANT_ID:
                        dto.setBroadParticipantId(value);
                        break;
                    case ROOT_SAMPLE_ID:
                        dto.setRootSampleName(value);
                        break;
                    case COLLABORATOR_SAMPLE_ID:
                        dto.setCollaboratorSampleId(value);
                        break;
                    case COLLABORATOR_PARTICIPANT_ID:
                        dto.setCollaboratorParticipantId(value);
                        break;
                    case GENDER:
                        dto.setGender(value);
                        break;
                    case SPECIES:
                        dto.setSpecies(value);
                        break;
                    case LSID:
                        dto.setLsid(value);
                        break;
                    }
                }
            }
            return dto;
        }

        /** Fills in any missing values from Mercury or BSP sampleData. */
        public void addMissingValues(SampleData sampleData) {
            if (sampleData != null) {
                if (isBlank(rootSampleName)) {
                    setRootSampleName(sampleData.getRootSample());
                }
                if (isBlank(broadParticipantId)) {
                    setBroadParticipantId(sampleData.getPatientId());
                }
                if (isBlank(collaboratorSampleId)) {
                    setCollaboratorSampleId(sampleData.getCollaboratorsSampleName());
                }
                if (isBlank(collaboratorParticipantId)) {
                    setCollaboratorParticipantId(sampleData.getCollaboratorParticipantId());
                }
                if (isBlank(gender)) {
                    setGender(sampleData.getGender());
                }
                if (isBlank(species)) {
                    setSpecies(sampleData.getOrganism());
                }
                if (isBlank(lsid)) {
                    setLsid(sampleData.getSampleLsid());
                }
            }
        }

        /** Fills in any missing values from another sampleDataDto. */
        public void addMissingValues(SampleDataDto other) {
            if (other != null) {
                if (isBlank(broadParticipantId)) {
                    setBroadParticipantId(other.getCollaboratorParticipantId());
                }
                if (isBlank(collaboratorSampleId)) {
                    setCollaboratorSampleId(other.getCollaboratorSampleId());
                }
                if (isBlank(collaboratorParticipantId)) {
                    setCollaboratorParticipantId(other.getCollaboratorParticipantId());
                }
                if (isBlank(gender)) {
                    setGender(other.getGender());
                }
                if (isBlank(species)) {
                    setSpecies(other.getSpecies());
                }
            }
        }

        String getRootSampleName() {
            return rootSampleName;
        }

        String getCollaboratorSampleId() {
            return collaboratorSampleId;
        }

        String getCollaboratorParticipantId() {
            return collaboratorParticipantId;
        }

        String getBroadParticipantId() {
            return broadParticipantId;
        }

        String getGender() {
            return gender;
        }

        String getSpecies() {
            return species;
        }

        public String getLsid() {
            return lsid;
        }

        public MercurySample.MetadataSource getMetadataSource() {
            return metadataSource;
        }

        public void setRootSampleName(String rootSampleName) {
            this.rootSampleName = rootSampleName;
        }

        public void setCollaboratorSampleId(String collaboratorSampleId) {
            this.collaboratorSampleId = collaboratorSampleId;
        }

        public void setCollaboratorParticipantId(String collaboratorParticipantId) {
            this.collaboratorParticipantId = collaboratorParticipantId;
        }

        public void setBroadParticipantId(String broadParticipantId) {
            this.broadParticipantId = broadParticipantId;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public void setSpecies(String species) {
            this.species = species;
        }

        public void setLsid(String lsid) {
            this.lsid = lsid;
        }

        public void setMetadataSource(MercurySample.MetadataSource metadataSource) {
            this.metadataSource = metadataSource;
        }

        String getSampleName() {
            return sampleName;
        }
    }

    /** Returns a comma delimited string of the sorted header names. */
    private String collectHeaderNames(Set<VesselPooledTubesProcessor.Headers> headers) {
        List<String> columns = new ArrayList<>();
        for (ColumnHeader header : headers) {
            columns.add(header.getText());
        }
        Collections.sort(columns);
        return StringUtils.join(columns, ", ");
    }

    /** Dto containing data in a PooledTube spreadsheet row. */
    private class RowDto {
        private String broadSampleId;
        private String rootSampleId;
        private String barcode;
        private String libraryName;
        private BigDecimal volume;
        private String experiment;
        private List<String> conditions;
        private Integer readLength;
        private MolecularIndexingScheme molecularIndexSchemes;
        private ReagentDesign reagent;
        private int rowNumber;

        RowDto(int rowNumber) {
            this.rowNumber = rowNumber;
        }

        String getBarcode() {
            return barcode;
        }

        void setBarcode(String barcode) {
            this.barcode = barcode;
        }

        MolecularIndexingScheme getMolecularIndexSchemes() {
            return molecularIndexSchemes;

        }

        void setMolecularIndexSchemes(MolecularIndexingScheme molecularIndexSchemes) {
            this.molecularIndexSchemes = molecularIndexSchemes;
        }

        ReagentDesign getReagent() {
            return reagent;
        }

        void setReagent(ReagentDesign reagent) {
            this.reagent = reagent;
        }

        String getBroadSampleId() {
            return broadSampleId;
        }

        void setBroadSampleId(String broadSampleId) {
            this.broadSampleId = broadSampleId;
        }

        String getRootSampleId() {
            return rootSampleId;
        }

        void setRootSampleId(String rootSampleId) {
            this.rootSampleId = rootSampleId;
        }

        String getLibraryName() {
            return libraryName;
        }

        void setLibraryName(String libraryName) {
            this.libraryName = libraryName;
        }

        BigDecimal getVolume() {
            return volume;
        }

        void setVolume(BigDecimal volume) {
            this.volume = volume;
        }

        String getExperiment() {
            return experiment;
        }

        void setExperiment(String experiment) {
            this.experiment = experiment;
        }

        List<String> getConditions() {
            return conditions;
        }

        void setConditions(List<String> conditions) {
            this.conditions = conditions;
        }

        Integer getReadLength() {
            return readLength;
        }

        void setReadLength(Integer readLength) {
            this.readLength = readLength;
        }

        int getRowNumber() {
            return rowNumber;
        }
    }
}
