package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.ReferenceSequenceDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleInstanceEntityDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleKitRequestDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessor;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorEzPass;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorNonPooled;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorPooled;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorPooledMultiOrganism;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VesselPooledTubesProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
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

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
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
    static final String IS_CONFLICT = "Conflicting value of %s (found \"%s\", expected \"%s\") %s at row %d";
    static final String IS_MISSING = "Missing value of %s at row %d";
    static final String IS_WRONG_TYPE = "%s must be %s, found at row %d";
    static final String IS_UNKNOWN = "The value for %s is not in %s, found at row %d";
    static final String IS_DUPLICATE = "Duplicate value for %s found at row %d";
    static final String IS_BSP_FORMAT = "The new %s \"%s\" must not have a BSP sample name format, found at row %d";
    static final String IS_PREXISTING =
            "A %s named \"%s\" already exists in Mercury, found at row %d; set the Overwrite checkbox to re-upload.";
    private static final String NANOMOLAR = "nM";

    public static final String IS_SUCCESS = "Spreadsheet with %d rows successfully uploaded";
    /** A string of the available sequencer model names. */
    public static final String SEQUENCER_MODELS;

    // These are the only characters allowed in a library or sample name.
    private static final String RESTRICTED_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-_";
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
    private ProductOrderDao productOrderDao;

    @Inject
    private SampleKitRequestDao sampleKitRequestDao;

    @Inject
    private SampleDataFetcher sampleDataFetcher;

    @Inject
    private ReferenceSequenceDao referenceSequenceDao;

    static {
        for (IlluminaFlowcell.FlowcellType flowcellType : CreateFCTActionBean.FLOWCELL_TYPES) {
            String sequencer = StringUtils.substringBeforeLast(flowcellType.getDisplayName(), "Flowcell").trim();
            mapSequencerToFlowcellType.put(sequencer, flowcellType);
        }
        SEQUENCER_MODELS = "\"" + StringUtils.join(mapSequencerToFlowcellType.keySet(), "\", \"") + "\"";
    }

    public SampleInstanceEjb() {
    }

    /** Constructor used for unit testing. */
    public SampleInstanceEjb(MolecularIndexingSchemeDao molecularIndexingSchemeDao, JiraService jiraService,
            ReagentDesignDao reagentDesignDao, LabVesselDao labVesselDao, MercurySampleDao mercurySampleDao,
            SampleInstanceEntityDao sampleInstanceEntityDao, ProductOrderDao productOrderDao,
            SampleKitRequestDao sampleKitRequestDao, SampleDataFetcher sampleDataFetcher,
            ReferenceSequenceDao referenceSequenceDao) {
        this.molecularIndexingSchemeDao = molecularIndexingSchemeDao;
        this.jiraService = jiraService;
        this.reagentDesignDao = reagentDesignDao;
        this.labVesselDao = labVesselDao;
        this.mercurySampleDao = mercurySampleDao;
        this.sampleInstanceEntityDao = sampleInstanceEntityDao;
        this.productOrderDao = productOrderDao;
        this.sampleKitRequestDao = sampleKitRequestDao;
        this.sampleDataFetcher = sampleDataFetcher;
        this.reagentDesignDao = reagentDesignDao;
        this.referenceSequenceDao = referenceSequenceDao;
    }

    /**
     * Determines the best parser for the uploaded spreadsheet. Does data checking on the data
     * and if ok then persists it as Mercury entities.
     *
     * @param inputStream the spreadsheet inputStream.
     * @param overwrite specifies if existing entities should be overwritten.
     * @param messages the errors, warnings, and info to be passed back.
     */
    public void doExternalUpload(InputStream inputStream, boolean overwrite, MessageCollection messages)
            throws IOException, InvalidFormatException, ValidationException {

        Map<TableProcessor, List<String>> errorMap = new HashMap<>();
        byte[] inputStreamBytes = IOUtils.toByteArray(inputStream);
        for (TableProcessor processor : Arrays.asList(new VesselPooledTubesProcessor("Sheet1"),
                new ExternalLibraryProcessorEzPass("Sheet1"),
                new ExternalLibraryProcessorPooledMultiOrganism("Sheet1"),
                new ExternalLibraryProcessorPooled("Sheet1"),
                new ExternalLibraryProcessorNonPooled("Sheet1"))) {
            errorMap.put(processor, PoiSpreadsheetParser.processSingleWorksheet(
                    new ByteArrayInputStream(inputStreamBytes), processor));
        }
        // Uses the TableProcessor having all the required headers, or if none then
        // reports the validation problems of the processor with the fewest errors.
        int fewestErrors = Integer.MAX_VALUE;
        for (List<String> errors : errorMap.values()) {
            fewestErrors = Math.min(fewestErrors, errors.size());
        }
        for (TableProcessor processor : errorMap.keySet()) {
            if (errorMap.get(processor).size() == fewestErrors) {
                messages.addErrors(errorMap.get(processor));
                if (processor instanceof VesselPooledTubesProcessor) {
                    verifyAndPersistSpreadsheet((VesselPooledTubesProcessor) processor, messages, overwrite);
                } else {
                    verifyAndPersistSpreadsheet((ExternalLibraryProcessor) processor, messages, overwrite);
                }
            }
        }
    }

    /**
     * Verifies the spreadsheet contents and if it's ok, persists the data.
     */
    public List<SampleInstanceEntity> verifyAndPersistSpreadsheet(VesselPooledTubesProcessor processor,
            MessageCollection messageCollection, boolean overWriteFlag) {

        // Adds any validation.
        // rowOffset converts row index to the 1-based row number shown by Excel on the far left of each row.
        int rowOffset = processor.getHeaderRowIndex() + 2;

        // Maps tube barcode to volume, and maps barcode to fragment size.
        Map<String, BigDecimal> mapBarcodeToVolume = new HashMap<>();
        Map<String, BigDecimal> mapBarcodeToFragmentSize = new HashMap<>();
        Set<String> uniqueBarcodes = new HashSet<>(processor.getBarcodes());
        uniqueBarcodes.removeAll(Collections.singletonList(""));
        for (String barcode : uniqueBarcodes) {
            // For each barcode, spins through the rows looking for the first nonzero volume
            // and fragment size. Errors if conflicts are found.
            for (int rowIndex = 0; rowIndex < processor.getBarcodes().size(); ++rowIndex) {
                if (processor.getBarcodes().get(rowIndex).equals(barcode)) {
                    String volumeValue = processor.getVolume().get(rowIndex);
                    if (isNotBlank(volumeValue) && NumberUtils.isNumber(volumeValue)) {
                        BigDecimal bigDecimal = MathUtils.scaleTwoDecimalPlaces(new BigDecimal(volumeValue));
                        if (bigDecimal.compareTo(BigDecimal.ZERO) > 0) {
                            if (mapBarcodeToVolume.containsKey(barcode) &&
                                    mapBarcodeToVolume.get(barcode).compareTo(bigDecimal) != 0) {
                                messageCollection.addError(String.format(IS_CONFLICT,
                                        VesselPooledTubesProcessor.Headers.VOLUME.getText(),
                                        bigDecimal.toPlainString(), mapBarcodeToVolume.get(barcode).toPlainString(), "",
                                        rowIndex + rowOffset));
                            } else {
                                mapBarcodeToVolume.put(barcode, bigDecimal);
                            }
                        }
                    }
                    String fragmentSizeValue = processor.getFragmentSize().get(rowIndex);
                    if (isNotBlank(fragmentSizeValue)) {
                        if (!NumberUtils.isNumber(fragmentSizeValue)) {
                            messageCollection.addError(String.format(IS_WRONG_TYPE,
                                    VesselPooledTubesProcessor.Headers.FRAGMENT_SIZE.getText(), "numeric",
                                    rowIndex + rowOffset));
                        } else {
                            BigDecimal bigDecimal = MathUtils.scaleTwoDecimalPlaces(new BigDecimal(fragmentSizeValue));
                            if (bigDecimal.compareTo(BigDecimal.ZERO) > 0) {
                                if (mapBarcodeToFragmentSize.containsKey(barcode) &&
                                        mapBarcodeToFragmentSize.get(barcode).compareTo(bigDecimal) != 0) {
                                    messageCollection.addError(String.format(IS_CONFLICT,
                                            VesselPooledTubesProcessor.Headers.FRAGMENT_SIZE.getText(),
                                            bigDecimal.toPlainString(),
                                            mapBarcodeToFragmentSize.get(barcode).toPlainString(), "",
                                            rowIndex + rowOffset));
                                } else {
                                    mapBarcodeToFragmentSize.put(barcode, bigDecimal);
                                }
                            }
                        }
                    }
                }
            }
        }
        // Errors if any tube is missing a value for volume or fragment size.
        for (String barcode : CollectionUtils.subtract(uniqueBarcodes, mapBarcodeToVolume.keySet())) {
            for (int rowIndex = 0; rowIndex < processor.getBarcodes().size(); ++rowIndex) {
                if (processor.getBarcodes().get(rowIndex).equals(barcode)) {
                    // Shows the error only for the first row containing the barcode.
                    messageCollection.addError(String.format(IS_MISSING,
                            VesselPooledTubesProcessor.Headers.VOLUME.getText(), rowIndex + rowOffset));
                    break;
                }
            }
        }
        for (String barcode : CollectionUtils.subtract(uniqueBarcodes, mapBarcodeToFragmentSize.keySet())) {
            for (int rowIndex = 0; rowIndex < processor.getBarcodes().size(); ++rowIndex) {
                if (processor.getBarcodes().get(rowIndex).equals(barcode)) {
                    // Shows the error only for the first row containing the barcode.
                    messageCollection.addError(String.format(IS_MISSING,
                            VesselPooledTubesProcessor.Headers.FRAGMENT_SIZE.getText(), rowIndex + rowOffset));
                    break;
                }
            }
        }

        // Maps the Broad sample names and root sample names to existing MercurySamples.
        Set<String> sampleNames = new HashSet<>(processor.getBroadSampleId());
        sampleNames.removeAll(Collections.singletonList(""));
        Map<String, MercurySample> sampleMap = mercurySampleDao.findMapIdToMercurySample(sampleNames);
        Set<String> sampleDataLookups = new HashSet<>();

        Set<String> rootSampleNames = new HashSet<>(processor.getRootSampleId());
        sampleNames.removeAll(Collections.singletonList(""));
        rootSampleNames.removeAll(sampleNames);
        Map<String, MercurySample> rootSampleMap = mercurySampleDao.findMapIdToMercurySample(rootSampleNames);

        // Sample metadata must be supplied for any new MercurySamples needed for Broad samples.
        Map<String, SampleDataDto> sampleDataDtoMap = new HashMap<>();
        for (String sampleName : sampleNames) {
            if (!sampleMap.containsKey(sampleName)) {
                for (int rowIndex = 0; rowIndex < processor.getBarcodes().size(); ++rowIndex) {
                    // Spins through the rows to find the sample metadata. The first row of the sample
                    // is expected to have all sample metadata fields. Subsequent rows of the sample
                    // are checked that the values are blank, or the same.
                    if (processor.getBroadSampleId().get(rowIndex).equals(sampleName)) {
                        SampleDataDto sampleDataDto = sampleDataDtoMap.get(sampleName);
                        if (sampleDataDto == null) {
                            sampleDataDto = new SampleDataDto(processor, rowIndex, rowOffset, messageCollection);
                            sampleDataDtoMap.put(sampleName, sampleDataDto);
                        } else {
                            sampleDataDto.compareToRow(processor, rowIndex, rowOffset, messageCollection);
                        }
                    }
                }
            } else {
                // Collects the names of existing samples that have non-blank metadata values.
                if (dataIsPresent(processor, sampleName)) {
                    sampleDataLookups.add(sampleName);
                }
            }
        }
        // For samples already in Mercury the metadata fields are best left blank. If non-blank values
        // are found then the code has to make sure they match existing values, because the sample
        // metadata cannot be updated from spreadsheet values, even when doing overwriting.
        Map<String, SampleData> sampleDataMap = sampleDataFetcher.fetchSampleData(sampleDataLookups);
        for (String sampleName : sampleDataMap.keySet()) {
            SampleData sampleData = sampleDataMap.get(sampleName);
            for (int rowIndex = 0; rowIndex < processor.getBarcodes().size(); ++rowIndex) {
                if (processor.getBroadSampleId().get(rowIndex).equals(sampleName)) {
                    String[][] actualExpectedHeaders = {
                            {processor.getRootSampleId().get(rowIndex), sampleData.getRootSample(),
                                    VesselPooledTubesProcessor.Headers.ROOT_SAMPLE_ID.getText(),},
                            {processor.getCollaboratorSampleId().get(rowIndex), sampleData.getCollaboratorsSampleName(),
                                    VesselPooledTubesProcessor.Headers.COLLABORATOR_PARTICIPANT_ID.getText()},
                            {processor.getCollaboratorParticipantId().get(rowIndex), sampleData.getCollaboratorParticipantId(),
                                    VesselPooledTubesProcessor.Headers.COLLABORATOR_SAMPLE_ID.getText()},
                            {processor.getBroadParticipantId().get(rowIndex), sampleData.getPatientId(),
                                    VesselPooledTubesProcessor.Headers.BROAD_PARTICIPANT_ID.getText()},
                            {processor.getGender().get(rowIndex), sampleData.getGender(),
                                    VesselPooledTubesProcessor.Headers.GENDER.getText()},
                            {processor.getSpecies().get(rowIndex), sampleData.getOrganism(),
                                    VesselPooledTubesProcessor.Headers.SPECIES.getText()},
                            {processor.getLsid().get(rowIndex), sampleData.getSampleLsid(),
                                    VesselPooledTubesProcessor.Headers.LSID.getText()}};
                    for (int i = 0; i < actualExpectedHeaders.length; ++i) {
                        if (isNotBlank(actualExpectedHeaders[i][0]) &&
                                !actualExpectedHeaders[i][0].equals(actualExpectedHeaders[i][1])) {
                            messageCollection.addError(String.format(IS_CONFLICT, actualExpectedHeaders[i][2],
                                    actualExpectedHeaders[i][0], actualExpectedHeaders[i][1],
                                    "for existing Mercury Sample", rowIndex + rowOffset));
                        }
                    }
                }
            }
        }
        // Finds new Broad Samples that don't have sample metadata. (Root samples are ok having no metadata.)
        for (String sampleName : CollectionUtils.subtract(CollectionUtils.subtract(sampleNames, sampleMap.keySet()),
                sampleDataDtoMap.keySet())) {
            for (int rowIndex = 0; rowIndex < processor.getBarcodes().size(); ++rowIndex) {
                if (processor.getBroadSampleId().get(rowIndex).equals(sampleName)) {
                    // Shows the error only for the first row containing the sample name.
                    messageCollection.addError(String.format(IS_MISSING,
                            VesselPooledTubesProcessor.Headers.ROOT_SAMPLE_ID.getText() + ", " +
                                    VesselPooledTubesProcessor.Headers.COLLABORATOR_PARTICIPANT_ID.getText() +
                                    VesselPooledTubesProcessor.Headers.COLLABORATOR_SAMPLE_ID.getText() + ", " +
                                    VesselPooledTubesProcessor.Headers.BROAD_PARTICIPANT_ID.getText() + ", " +
                                    VesselPooledTubesProcessor.Headers.GENDER.getText() + ", " +
                                    VesselPooledTubesProcessor.Headers.SPECIES.getText() + ", " +
                                    VesselPooledTubesProcessor.Headers.LSID.getText(), rowIndex + rowOffset));
                    break;
                }
            }
        }

        // Maps tube barcode to existing tubes.
        Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(new ArrayList<>(uniqueBarcodes));

        Set<String> uniqueLibraryNames = new HashSet<>();
        Set<String> barcodeAndMis = new HashSet<>();
        Set<String> uniqueSampleMis = new HashSet<>();
        Multimap<String, String> experimentConditions = HashMultimap.create();
        Multimap<String, Integer> experimentConditionsRow = HashMultimap.create();
        List<RowDto> rowDtos = new ArrayList<>();

        for (int rowIndex = 0; rowIndex < processor.getBarcodes().size(); ++rowIndex) {
            String libraryName = processor.getSingleSampleLibraryName().get(rowIndex);
            String barcode = processor.getBarcodes().get(rowIndex);
            String broadSampleName = processor.getBroadSampleId().get(rowIndex);
            // Ignores the row if several key fields are blank.
            if (isNotBlank(libraryName) || isNotBlank(barcode) || isNotBlank(broadSampleName)) {
                RowDto rowDto = new RowDto(rowIndex + rowOffset);
                rowDtos.add(rowDto);

                rowDto.setLibraryName(libraryName);
                if (isBlank(libraryName)) {
                    messageCollection.addError(String.format(IS_MISSING,
                            VesselPooledTubesProcessor.Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText(),
                            rowDto.getRowNumber()));
                } else {
                    if (!uniqueLibraryNames.add(libraryName)) {
                        messageCollection.addError(String.format(IS_DUPLICATE,
                                VesselPooledTubesProcessor.Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText(),
                                rowDto.getRowNumber()));
                    }
                    if (sampleInstanceEntityDao.findByName(libraryName) != null && !overWriteFlag) {
                        messageCollection.addError(String.format(IS_PREXISTING, "Library", libraryName,
                                rowDto.getRowNumber()));
                    }
                }

                rowDto.setBarcode(barcode);
                if (isBlank(barcode)) {
                    messageCollection.addError(String.format(IS_MISSING,
                            VesselPooledTubesProcessor.Headers.TUBE_BARCODE.getText(), rowDto.getRowNumber()));
                }
                if (mapBarcodeToVessel.get(barcode) != null) {
                    if (!overWriteFlag) {
                        messageCollection.addError(String.format(IS_PREXISTING, "Tube", barcode,
                                rowDto.getRowNumber()));
                    }
                } else {
                    // A new tube must not have a barcode that may someday collide with Mercury, so
                    // disallow a 10-digit barcode. The barcode character set is restricted.
                    if (!StringUtils.containsOnly(barcode, RESTRICTED_CHARS)) {
                        messageCollection.addError(String.format(IS_WRONG_TYPE, "Tube barcode",
                                "composed of {" + RESTRICTED_CHARS + "}", rowDto.getRowNumber()));
                    }
                    if (barcode.length() == 10 && barcode.matches("[0-9]+")) {
                        messageCollection.addError(
                                "A new tube must not have a 10 digit barcode (reserved for Mercury), found at row " +
                                        rowDto.getRowNumber());
                    }
                }

                rowDto.setBroadSampleId(broadSampleName);
                if (isBlank(broadSampleName)) {
                    messageCollection.addError(String.format(IS_MISSING,
                            VesselPooledTubesProcessor.Headers.BROAD_SAMPLE_ID.getText(), rowDto.getRowNumber()));
                } else {
                    if (!sampleMap.containsKey(broadSampleName) && isInBspFormat(broadSampleName)) {
                        // Errors if it's a new Broad Sample name that could collide with a future BSP SM-id.
                        messageCollection.addError(String.format(IS_BSP_FORMAT, "Broad Sample", broadSampleName,
                                rowDto.getRowNumber()));
                    }
                }

                String rootSampleName = processor.getRootSampleId().get(rowIndex);
                rowDto.setRootSampleId(rootSampleName);

                //Does molecular index scheme exist.
                String misName = processor.getMolecularIndexingScheme().get(rowIndex);
                if (isBlank(misName)) {
                    messageCollection.addError(String.format(IS_MISSING,
                            VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText(),
                            rowDto.getRowNumber()));
                } else {
                    MolecularIndexingScheme molecularIndexingScheme = molecularIndexingSchemeDao.findByName(misName);
                    rowDto.setMolecularIndexSchemes(molecularIndexingScheme);
                    if (molecularIndexingScheme == null) {
                        messageCollection.addError(String.format(IS_UNKNOWN,
                                VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText(), "Mercury",
                                rowDto.getRowNumber()));
                    }
                    // Errors if a tube has duplicate Molecular Index Scheme.
                    if (!barcodeAndMis.add(barcode + "_" + misName)) {
                        messageCollection.addError(String.format(IS_DUPLICATE,
                                VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText() +
                                        " in tube " + barcode, rowDto.getRowNumber()));
                    }
                    // Warns if the spreadsheet has duplicate combination of Broad Sample and Molecular Index Scheme
                    // (in different tubes). It's not an error as long as the tubes don't get pooled later on, which
                    // isn't known at upload time.
                    String sampleMis = broadSampleName + ", " + misName;
                    if (!uniqueSampleMis.add(sampleMis)) {
                        messageCollection.addWarning(String.format(IS_DUPLICATE,
                                VesselPooledTubesProcessor.Headers.BROAD_SAMPLE_ID.getText() + " and " +
                                        VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText() +
                                        " (" + sampleMis + ")", rowDto.getRowNumber()));
                    }
                }

                // Either bait or cat may be specified, or neither.
                String bait = processor.getBait().get(rowIndex);
                String cat = processor.getCat().get(rowIndex);
                ReagentDesign reagentDesign = null;
                if (!bait.isEmpty() && !cat.isEmpty()) {
                    messageCollection.addError(String.format(IS_CONFLICT,
                            VesselPooledTubesProcessor.Headers.BAIT.getText() + " and " +
                                    VesselPooledTubesProcessor.Headers.CAT.getText(),
                            "both", "only one", "", rowDto.getRowNumber()));
                } else if (!bait.isEmpty()) {
                    reagentDesign = reagentDesignDao.findByBusinessKey(bait);
                    if (reagentDesign == null) {
                        messageCollection.addError(String.format(IS_UNKNOWN,
                                VesselPooledTubesProcessor.Headers.BAIT.getText(), "Mercury", rowDto.getRowNumber()));
                    }
                } else if (!cat.isEmpty()) {
                    reagentDesign = reagentDesignDao.findByBusinessKey(cat);
                    if (reagentDesign == null) {
                        messageCollection.addError(String.format(IS_UNKNOWN,
                                VesselPooledTubesProcessor.Headers.CAT.getText(), "Mercury", rowDto.getRowNumber()));
                    }
                }
                rowDto.setReagent(reagentDesign);

                // Collects the Jira ticket experiment and Jira sub-task conditions, to validate later.
                String experiment = processor.getExperiment().get(rowIndex);
                rowDto.setExperiment(experiment);
                if (isBlank(experiment)) {
                    messageCollection.addError(String.format(IS_MISSING,
                            VesselPooledTubesProcessor.Headers.EXPERIMENT.getText(), rowDto.getRowNumber()));
                }

                List<String> conditions = processor.getConditions().get(rowIndex);
                rowDto.setConditions(conditions);
                if (CollectionUtils.isEmpty(conditions)) {
                    messageCollection.addError(String.format(IS_MISSING,
                            VesselPooledTubesProcessor.Headers.CONDITIONS.getText(), rowDto.getRowNumber()));
                } else {
                    experimentConditions.putAll(experiment, conditions);
                    // Collects all row numbers that have the same experiment & condition.
                    for (String condition : conditions) {
                        experimentConditionsRow.put(experiment + " " + condition, rowIndex + rowOffset);
                    }
                }

                if (isNotBlank(processor.getReadLength().get(rowIndex))) {
                    if (NumberUtils.isNumber(processor.getReadLength().get(rowIndex))) {
                        rowDto.setReadLength(Integer.parseInt(processor.getReadLength().get(rowIndex)));
                    } else {
                        messageCollection.addError(String.format(IS_WRONG_TYPE,
                                VesselPooledTubesProcessor.Headers.READ_LENGTH.getText(), "numeric",
                                rowDto.getRowNumber()));
                    }
                }
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
                    if (rowDtos.get(i).getExperiment().equals(experiment)) {
                        messageCollection.addError(String.format(IS_UNKNOWN,
                                VesselPooledTubesProcessor.Headers.EXPERIMENT.getText(), "JIRA DEV", i + rowOffset));
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
                    messageCollection.addError(String.format(IS_UNKNOWN,
                            VesselPooledTubesProcessor.Headers.CONDITIONS.getText(), "sub-tasks of " + experiment,
                            rowNumber));
                }
            }
        }
        List<SampleInstanceEntity> sampleInstanceEntities;
        // If there are no errors attempt save the data to the database.
        if (!messageCollection.hasErrors()) {
            // Passes a combined map of Broad and root samples.
            sampleMap.putAll(rootSampleMap);
            // Adds new root samples to the sampleDataDtoMap so they'll be created, but with no metadata.
            for (String rootSampleName : rootSampleNames) {
                if (!rootSampleMap.containsKey(rootSampleName)) {
                    sampleDataDtoMap.put(rootSampleName, null);
                }
            }
            sampleInstanceEntities = persistPooledTubes(rowDtos, mapBarcodeToVessel, mapBarcodeToVolume,
                    mapBarcodeToFragmentSize, sampleMap, sampleDataDtoMap, messageCollection);
        } else {
            sampleInstanceEntities = Collections.emptyList();
        }
        Collections.sort(messageCollection.getErrors(), BY_ROW_NUMBER);
        return sampleInstanceEntities;
    }

    /**
     * Saves PooledTube uploads into LabVessels and SampleInstanceEntities.
     * @param mapBarcodeToVessel       contain only tube barcodes of existing lab vessels.
     * @param mapBarcodeToVolume       contains all tube barcodes and their volumes.
     * @param mapBarcodeToFragmentSize contains all tube barcodes and their fragment size.
     * @param combinedSampleMap        contains all broad and root sample names and their mercury samples.
     * @param sampleDataDtoMap         contains sample data for samples which need to be created, both with
     */
    private List<SampleInstanceEntity> persistPooledTubes(List<RowDto> rowDtos,
            Map<String, LabVessel> mapBarcodeToVessel, Map<String, BigDecimal> mapBarcodeToVolume,
            Map<String, BigDecimal> mapBarcodeToFragmentSize, Map<String, MercurySample> combinedSampleMap,
            Map<String, SampleDataDto> sampleDataDtoMap, MessageCollection messageCollection) {

        // Creates any tubes needed. Sets tube volume and fragment size on all tubes.
        List<LabVessel> newLabVessels = new ArrayList<>();
        for (String barcode : mapBarcodeToVolume.keySet()) {
            LabVessel labVessel = mapBarcodeToVessel.get(barcode);
            if (labVessel == null) {
                labVessel = new BarcodedTube(barcode, BarcodedTube.BarcodedTubeType.MatrixTube);
                newLabVessels.add(labVessel);
                mapBarcodeToVessel.put(barcode, labVessel);
            }
            labVessel.setVolume(mapBarcodeToVolume.get(barcode));
            addLibrarySize(labVessel, mapBarcodeToFragmentSize.get(barcode));
        }

        // Creates any samples needed.
        for (String sampleName : sampleDataDtoMap.keySet()) {
            MercurySample mercurySample;
            SampleDataDto sampleDataDto = sampleDataDtoMap.get(sampleName);
            if (sampleDataDto == null) {
                // Missing metadata means this is a new root sample.
                mercurySample = new MercurySample(sampleName, MercurySample.MetadataSource.MERCURY);
            } else {
                Set<Metadata> metadata = new HashSet<>();
                metadata.add(new Metadata(Metadata.Key.SAMPLE_ID, sampleDataDto.getCollaboratorSampleId()));
                metadata.add(new Metadata(Metadata.Key.BROAD_PARTICIPANT_ID, sampleDataDto.getBroadParticipantId()));
                metadata.add(new Metadata(Metadata.Key.PATIENT_ID, sampleDataDto.getCollaboratorParticipantId()));
                metadata.add(new Metadata(Metadata.Key.GENDER, sampleDataDto.getGender()));
                metadata.add(new Metadata(Metadata.Key.LSID, sampleDataDto.getLsid()));
                metadata.add(new Metadata(Metadata.Key.SPECIES, sampleDataDto.getSpecies()));
                metadata.add(new Metadata(Metadata.Key.MATERIAL_TYPE, MaterialType.DNA.getDisplayName()));
                mercurySample = new MercurySample(sampleName, metadata);
            }
            combinedSampleMap.put(sampleName, mercurySample);
        }

        List<SampleInstanceEntity> sampleInstanceEntities = new ArrayList<>();
        for (RowDto rowDto : rowDtos) {
            SampleInstanceEntity sampleInstanceEntity = sampleInstanceEntityDao.findByName(rowDto.getLibraryName());
            if (sampleInstanceEntity == null) {
                sampleInstanceEntity = new SampleInstanceEntity();
            }
            sampleInstanceEntity.setMercurySample(combinedSampleMap.get(rowDto.getBroadSampleId()));
            sampleInstanceEntity.setRootSample(combinedSampleMap.get(rowDto.getRootSampleId()));
            sampleInstanceEntity.setMolecularIndexingScheme(rowDto.getMolecularIndexSchemes());
            sampleInstanceEntity.setSampleLibraryName(rowDto.getLibraryName());
            sampleInstanceEntity.setReagentDesign(rowDto.getReagent());
            sampleInstanceEntity.setReadLength(rowDto.getReadLength());
            sampleInstanceEntity.setExperiment(rowDto.getExperiment());
            sampleInstanceEntity.setLabVessel(mapBarcodeToVessel.get(rowDto.getBarcode()));
            sampleInstanceEntity.removeSubTasks();

            // Persists the experiment conditions in the order they appeared on the row.
            int order = 0;
            for (String subTask : rowDto.getConditions()) {
                SampleInstanceEntityTsk sampleInstanceEntityTsk = new SampleInstanceEntityTsk();
                sampleInstanceEntityTsk.setSubTask(subTask);
                sampleInstanceEntityTsk.setOrder(order++);
                sampleInstanceEntity.addSubTasks(sampleInstanceEntityTsk);
            }

            sampleInstanceEntities.add(sampleInstanceEntity);

            // Also puts the sample in tube.
            mapBarcodeToVessel.get(rowDto.getBarcode()).addSample(sampleInstanceEntity.getMercurySample());
        }

        labVesselDao.persistAll(newLabVessels);
        sampleInstanceEntityDao.persistAll(sampleInstanceEntities);

        if (!rowDtos.isEmpty()) {
            messageCollection.addInfo(String.format(IS_SUCCESS, rowDtos.size()));
        } else {
            messageCollection.addError("No valid data found.");
        }
        return sampleInstanceEntities;
    }


    /**
     * Verifies and persists External Library Upload spreadsheet. This differs from the
     * PooledTube upload in that there is no explicit barcode nor sample name, and the
     * library name which is unique on the spreadsheet is used for both. This means no
     * pooling can be done for tubes by repeating their barcode in rows of the spreadsheet,
     * and all sample metadata must be present on every row. There is no need for this code
     * to make dtos to track aggregation, since every row will become a sampleInstanceEntity.
     */
    public List<SampleInstanceEntity> verifyAndPersistSpreadsheet(ExternalLibraryProcessor processor,
            MessageCollection messageCollection, boolean overWriteFlag) {
        int entityCount = processor.getSingleSampleLibraryName().size();

        // Includes error messages due to missing header or value that were discovered when parsing the spreadsheet.
        messageCollection.addErrors(processor.getMessages());

        Set<String> uniqueBarcodes = new HashSet<>();
        Set<String> uniqueLibraryNames = new HashSet<>();

        Map<Integer, ResearchProject> researchProjects = new HashMap<>();
        Map<Integer, ProductOrder> productOrders = new HashMap<>();
        Map<Integer, MolecularIndexingScheme> molecularIndexingSchemes = new HashMap<>();
        Map<Integer, LabVessel> labVessels = new HashMap<>();
        Map<Integer, MercurySample> mercurySamples = new HashMap<>();
        Map<Integer, SampleInstanceEntity> sampleInstanceEntities = new HashMap<>();
        Map<Integer, ReferenceSequence> referenceSequences = new HashMap<>();
        Map<Integer, IlluminaFlowcell.FlowcellType> sequencerModels = new HashMap<>();

        for (int index = 0; index < entityCount; ++index) {
            // Converts the 0-based index into the spreadsheet row number shown at the far left side in Excel.
            int rowNumber = index + processor.getHeaderRowIndex() + 2;

            // Library name must only appear in one row, and is assumed to be universally unique. If it is
            // reused then it's unusable unless Overwrite is set. The pipeline and elsewhere require a
            // simple name so disallow whitespace and anything that might cause trouble.
            String libraryName = get(processor.getSingleSampleLibraryName(), index);
            assert (isNotBlank(libraryName));
            // The library name must only appear in one row.
            if (!uniqueLibraryNames.add(libraryName)) {
                messageCollection.addError(String.format(IS_DUPLICATE, "Library Name", rowNumber));
            }
            if (!StringUtils.containsOnly(libraryName, RESTRICTED_CHARS)) {
                messageCollection.addError(String.format(IS_WRONG_TYPE, "Library Name",
                        "composed of {" + RESTRICTED_CHARS + "}", rowNumber));
            }
            SampleInstanceEntity sampleInstanceEntity = sampleInstanceEntityDao.findByName(libraryName);
            if (sampleInstanceEntity != null) {
                sampleInstanceEntities.put(index, sampleInstanceEntity);
                if (!overWriteFlag) {
                    messageCollection.addError(String.format(IS_PREXISTING, "Library", libraryName, rowNumber));
                }
            }

            // Library name is used as the sample name, and if the sample doesn't already exist then
            // disallow a library name that could cause the sample to collide with a future BSP sample.
            MercurySample mercurySample = mercurySampleDao.findBySampleKey(libraryName);
            if (mercurySample != null) {
                mercurySamples.put(index, mercurySample);
                if (!overWriteFlag) {
                    messageCollection.addError(String.format(IS_PREXISTING, "Sample", libraryName, rowNumber));
                }
            } else if (isInBspFormat(libraryName)) {
                messageCollection.addError(String.format(IS_BSP_FORMAT, "Library Name", libraryName,
                        rowNumber));
            }

            String barcode = get(processor.getBarcodes(), index);
            // Uses the libraryName if a barcode is not present.
            boolean barcodeIsLibrary = barcode == null;
            if (barcodeIsLibrary) {
                barcode = libraryName;
            }
            // Tube barcode must only appear in one row.
            if (!uniqueBarcodes.add(barcode)) {
                messageCollection.addError(String.format(IS_DUPLICATE, "Tube Barcode", rowNumber));
            }
            LabVessel labVessel = labVesselDao.findByIdentifier(barcode);
            if (labVessel != null) {
                labVessels.put(index, labVessel);
                if (!overWriteFlag) {
                    messageCollection.addError(String.format(IS_PREXISTING, "Tube", barcode, rowNumber));
                }
            } else {
                // A new tube must not have a barcode that may someday collide with Mercury, so
                // disallow a 10-digit barcode. The barcode character set is restricted.
                if (!StringUtils.containsOnly(barcode, RESTRICTED_CHARS)) {
                    messageCollection.addError(String.format(IS_WRONG_TYPE, "Tube barcode",
                            "composed of {" + RESTRICTED_CHARS + "}", rowNumber));
                }
                if (barcode.length() == 10 && barcode.matches("[0-9]+")) {
                    messageCollection.addError("A new tube must not have a barcode " +
                            (barcodeIsLibrary ? "(the Library Name)" : "") +
                            " that is 10 digits (reserved for Mercury), found at row " + rowNumber);
                }
            }
            if (isNotBlank(get(processor.getTotalLibraryVolume(), index)) &&
                    !NumberUtils.isNumber(processor.getTotalLibraryVolume().get(index))) {
                messageCollection.addError(String.format(IS_WRONG_TYPE, "Volume", "numeric", rowNumber));
            }
            if (isNotBlank(get(processor.getTotalLibraryConcentration(), index)) &&
                    !NumberUtils.isNumber(processor.getTotalLibraryConcentration().get(index))) {
                messageCollection.addError(String.format(IS_WRONG_TYPE, "Concentration", "numeric", rowNumber));
            }

            nonNegativeOrBlank(get(processor.getReadLength(), index), "Read Length", rowNumber,
                    messageCollection);
            nonNegativeOrBlank(get(processor.getNumberOfLanes(), index), "Numer of Lanes", rowNumber, messageCollection);
            nonNegativeOrBlank(get(processor.getInsertSize(), index), "Insert Size", rowNumber,
                    messageCollection);
            String referenceSequenceName = get(processor.getReferenceSequence(), index);
            if (isNotBlank(referenceSequenceName)) {
                ReferenceSequence referenceSequence = referenceSequenceDao.findCurrent(referenceSequenceName);
                if (referenceSequence == null) {
                    messageCollection.addError(String.format(IS_UNKNOWN, "Reference Sequence", "Mercury", rowNumber));
                } else {
                    referenceSequences.put(index, referenceSequence);
                }
            }

            String misName = get(processor.getMolecularBarcodeName(), index);
            MolecularIndexingScheme molecularIndexingScheme = molecularIndexingSchemeDao.findByName(misName);
            if (molecularIndexingScheme == null) {
                if (isNotBlank(misName)) {
                    messageCollection.addError(String.format(IS_UNKNOWN, "Molecular Barcode Name", "Mercury",
                            rowNumber));
                }
            } else {
                molecularIndexingSchemes.put(index, molecularIndexingScheme);
            }

            String pdoTitle = get(processor.getProjectTitle(), index);
            if (isNotBlank(pdoTitle)) {
                ProductOrder productOrder = productOrderDao.findByTitle(pdoTitle);
                if (productOrder == null) {
                    messageCollection.addError(String.format(IS_UNKNOWN, "Project Title", "Mercury", rowNumber));
                } else {
                    productOrders.put(index, productOrder);
                    if (productOrder.getProduct() == null) {
                        messageCollection.addError("No Product defined for Product Order entitled \"" +
                                pdoTitle + "\" found at row " + rowNumber);
                    } else {
                        String productAnalysisType =
                                StringUtils.trimToEmpty(productOrder.getProduct().getAnalysisTypeKey());
                        String analysisType = StringUtils.trimToEmpty(get(processor.getDataAnalysisType(), index));
                        if (!analysisType.equalsIgnoreCase(productAnalysisType)) {
                            messageCollection.addError(String.format(IS_CONFLICT, "Data Analysis Type", analysisType,
                                    productAnalysisType, "defined in Product " +
                                            productOrder.getProduct().getPartNumber(), rowNumber));
                        }
                    }
                    ResearchProject researchProject = productOrder.getResearchProject();
                    if (researchProject == null) {
                        messageCollection.addError("No Research Project defined for Product Order entitled \"" +
                                pdoTitle + "\" found at row " + rowNumber);
                    } else {
                        researchProjects.put(index, researchProject);
                        validateIrbNumber(get(processor.getIrbNumber(), index), researchProject, rowNumber,
                                messageCollection);
                    }
                }
            }
            if (isNotBlank(get(processor.getSequencingTechnology(), index))) {
                IlluminaFlowcell.FlowcellType sequencerModel =
                        findFlowcellType(get(processor.getSequencingTechnology(), index));
                if (sequencerModel == null) {
                    messageCollection.addError(String.format(IS_WRONG_TYPE, "Sequencing Technology",
                            SEQUENCER_MODELS + ", or blank", rowNumber));
                } else {
                    sequencerModels.put(index, sequencerModel);
                }
            }

        }
        if (entityCount == 0) {
            messageCollection.addWarning("Spreadsheet contains no data.");
        }
        if (!messageCollection.hasErrors()) {
            persistExternalLibraries(processor, researchProjects, productOrders, molecularIndexingSchemes,
                    labVessels, mercurySamples, sampleInstanceEntities, referenceSequences, sequencerModels,
                    messageCollection);
        } else {
            Collections.sort(messageCollection.getErrors(), BY_ROW_NUMBER);
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
            Map<Integer, ResearchProject> researchProjects, Map<Integer, ProductOrder> productOrders,
            Map<Integer, MolecularIndexingScheme> molecularIndexingSchemes,
            Map<Integer, LabVessel> labVessels, Map<Integer, MercurySample> mercurySamples,
            Map<Integer, SampleInstanceEntity> sampleInstanceEntities,
            Map<Integer, ReferenceSequence> referenceSequences,
            Map<Integer, IlluminaFlowcell.FlowcellType> sequencerModels, MessageCollection messageCollection) {

        // Captures the pre-row one-off spreadsheet data in a "kit". This is an unfortunate naming of
        // the upload manifest because it has no connection with the actual shipped and received kit.
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
        }

        Collection<Object> newObjects = new ArrayList<>();
        int numberOfEntities = processor.getSingleSampleLibraryName().size();
        for (int index = 0; index < numberOfEntities; ++index) {
            String libraryName = processor.getSingleSampleLibraryName().get(index);

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
                    asNumber(get(processor.getTotalLibraryVolume(), index)))));
            labVessel.setConcentration(MathUtils.scaleTwoDecimalPlaces(new BigDecimal(
                    asNumber(get(processor.getTotalLibraryConcentration(), index)))));
            if (asInteger(get(processor.getLibrarySize(), index)) > 0) {
                addLibrarySize(labVessel, MathUtils.scaleTwoDecimalPlaces(new BigDecimal(
                        get(processor.getLibrarySize(), index))));
            }

            MercurySample mercurySample = mercurySamples.get(index);
            if (mercurySample == null) {
                mercurySample = new MercurySample(libraryName, MercurySample.MetadataSource.MERCURY);

                Set<Metadata> metadata = new HashSet<>();
                metadata.add(new Metadata(Metadata.Key.SAMPLE_ID, get(processor.getCollaboratorSampleId(), index)));
                metadata.add(new Metadata(Metadata.Key.BROAD_PARTICIPANT_ID, get(processor.getIndividualName(), index)));
                metadata.add(new Metadata(Metadata.Key.PATIENT_ID, get(processor.getIndividualName(), index)));
                metadata.add(new Metadata(Metadata.Key.GENDER, get(processor.getSex(), index)));
                metadata.add(new Metadata(Metadata.Key.STRAIN, get(processor.getStrain(), index)));
                String organism = get(processor.getOrganism(), index);
                metadata.add(new Metadata(Metadata.Key.SPECIES, isNotBlank(organism) ?
                        organism : (isNotBlank(processor.getSpecies()) ? processor.getSpecies() : "")));
                metadata.add(new Metadata(Metadata.Key.MATERIAL_TYPE, MaterialType.DNA.getDisplayName()));
                mercurySample.addMetadata(metadata);
                newObjects.add(mercurySample);
            }
            mercurySample.addLabVessel(labVessel);

            SampleInstanceEntity sampleInstanceEntity = sampleInstanceEntities.get(index);
            if (sampleInstanceEntity == null) {
                sampleInstanceEntity = new SampleInstanceEntity();
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
            sampleInstanceEntity.setComments(
                    StringUtils.trimToEmpty(get(processor.getAdditionalSampleInformation(), index)) + "; " +
                            StringUtils.trimToEmpty(get(processor.getAdditionalAssemblyInformation(), index)));
            sampleInstanceEntity.setLibraryType(get(processor.getLibraryType(), index));
            sampleInstanceEntity.setReferenceSequence(referenceSequences.get(index));
            sampleInstanceEntity.setMolecularIndexScheme(molecularIndexingSchemes.get(index));
            sampleInstanceEntity.setLibraryType(get(processor.getLibraryType(), index));
            sampleInstanceEntity.setResearchProject(researchProjects.get(index));
            sampleInstanceEntity.setProductOrder(productOrders.get(index));
            sampleInstanceEntity.setLabVessel(labVessel);
            sampleInstanceEntity.setMercurySample(mercurySample);
            sampleInstanceEntity.setSequencerModel(sequencerModels.get(index));
        }
        sampleInstanceEntityDao.persistAll(newObjects);

        messageCollection.addInfo("Spreadsheet with " + numberOfEntities + " rows successfully uploaded.");
    }

    /**
     * Validates the walkup sequencing submission and if ok then persists the data in new or existing
     * SampleInstanceEntity, MercurySample, LabVessel, etc.
     *
     * @param walkUpSequencing the data from a walkup sequencing submission.
     * @param messageCollection collected errors, warnings, info to be passed back.
     */
    public void verifyAndPersistSubmission(WalkUpSequencing walkUpSequencing, MessageCollection messageCollection) {
        if (StringUtils.isBlank(walkUpSequencing.getEmailAddress())) {
            messageCollection.addError("Email is missing");
        }
        if (StringUtils.isBlank(walkUpSequencing.getLabName())) {
            messageCollection.addError("Lab Name is missing");
        }

        if (NANOMOLAR.equalsIgnoreCase(walkUpSequencing.getConcentrationUnit()) &&
                asInteger(walkUpSequencing.getFragmentSize()) <= 0) {
            messageCollection.addError("Conversion from nM to ng/ul requires a positive integer value of Fragment Size. Found '" +
                    walkUpSequencing.getFragmentSize() + "'");
        }

        if (StringUtils.isBlank(walkUpSequencing.getLibraryName())) {
            messageCollection.addError("Library name (Mercury sample name) is missing.");
        }
        if (StringUtils.isBlank(walkUpSequencing.getTubeBarcode())) {
            messageCollection.addError("Tube barcode is missing.");
        }

        if (StringUtils.isNotBlank(walkUpSequencing.getReadType()) &&
                !walkUpSequencing.getReadType().substring(0, 1).equalsIgnoreCase("s") &&
                !walkUpSequencing.getReadType().substring(0, 1).equalsIgnoreCase("p")) {
            messageCollection.addError("Read Type must either be blank or start with \"S\" or \"P\".");
        }

        ReferenceSequence referenceSequence = StringUtils.isBlank(walkUpSequencing.getReferenceVersion()) ?
                referenceSequenceDao.findCurrent(walkUpSequencing.getReference()) :
                referenceSequenceDao.findByNameAndVersion(walkUpSequencing.getReference(),
                        walkUpSequencing.getReferenceVersion());
        if (StringUtils.isNotBlank(walkUpSequencing.getReference()) && referenceSequence == null) {
            messageCollection.addError("Reference Sequence '" + walkUpSequencing.getReference() + "'" +
                    (StringUtils.isNotBlank(walkUpSequencing.getReferenceVersion()) ?
                            " version '" + walkUpSequencing.getReferenceVersion() + "'" : "") +
                    " is not known in Mercury.");
        }

        ReagentDesign reagentDesign = null;
        if (StringUtils.isNotBlank(walkUpSequencing.getBaitSetName())) {
            reagentDesign = reagentDesignDao.findByBusinessKey(walkUpSequencing.getBaitSetName());
            if (reagentDesign == null) {
                messageCollection.addError("Unknown Bait Reagent '" + walkUpSequencing.getBaitSetName() + "'");
            }
        }

        IlluminaFlowcell.FlowcellType sequencerModel = null;
        if (StringUtils.isNotBlank(walkUpSequencing.getIlluminaTech())) {
            sequencerModel = SampleInstanceEjb.findFlowcellType(walkUpSequencing.getIlluminaTech());
            if (sequencerModel == null) {
                messageCollection.addError("Unknown Sequencing Technology, must be one of " +
                        SampleInstanceEjb.SEQUENCER_MODELS + ", or blank");
            }
        }

        if (!messageCollection.hasErrors()) {
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
        // Concentration is only stored as ng/ul. If given nM, convert it based on
        // www.kumc.edu/Documents/gsf/nM%20Conversion%20Calculator.xlsx
        // (ng/ul) = nM * (avgSizeInBasePairs * 607.4 + 157.9) / 1e6
        BigDecimal concentration = NANOMOLAR.equalsIgnoreCase(walkUpSequencing.getConcentrationUnit()) ?
                new BigDecimal(Double.parseDouble(asNumber(walkUpSequencing.getConcentration()))
                        * (asInteger(walkUpSequencing.getFragmentSize()) * 607.4 + 157.9) / 1000000d) :
                new BigDecimal(asNumber(walkUpSequencing.getConcentration()));
        labVessel.setConcentration(MathUtils.scaleTwoDecimalPlaces(concentration));

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
        sampleInstanceEntity.setSubmitDate(walkUpSequencing.getSubmitDate());
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
            MessageCollection messageCollection) {
        if (isNotBlank(irbNumber) && !IRB_EXEMPT.equalsIgnoreCase(irbNumber)) {
            if (!researchProject.getIrbNumbers(false).contains(irbNumber)) {
                String identifier = researchProject.hasJiraTicketKey() ?
                        researchProject.getJiraTicketKey() :
                        String.valueOf(researchProject.getResearchProjectId());
                messageCollection.addError(String.format(IS_CONFLICT, "IRB Number", irbNumber,
                        StringUtils.join(researchProject.getIrbNumbers(), "\" or \""),
                        "defined in ResearchProject " + identifier, rowNumber));
            }
        }
    }

    private static Comparator<String> BY_ROW_NUMBER = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            // Does a numeric sort on the row number string, expected to be after the word "row".
            int rowCompare = Integer.parseInt(StringUtils.substringAfter(o1, " row ").split(" ")[0]) -
                    Integer.parseInt(StringUtils.substringAfter(o2, " row ").split(" ")[0]);
            return (rowCompare == 0) ? o1.compareTo(o2) : rowCompare;
        }
    };

    private void nonNegativeOrBlank(String value, String header, int rowNumber, MessageCollection messageCollection) {
        if (isNotBlank(value) && (!StringUtils.isNumeric(value) || Integer.parseInt(value) < 0)) {
            messageCollection.addError(String.format(IS_WRONG_TYPE, header,
                    "either a positive integer or blank", rowNumber));
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

    /** Returns true if the Pooled Tube spreadsheet defines any sample metadata for the given sample name. */
    private boolean dataIsPresent(VesselPooledTubesProcessor processor, String sampleName) {
        for (int rowIndex = 0; rowIndex < processor.getBarcodes().size(); ++rowIndex) {
            if (sampleName.equals(get(processor.getBroadSampleId(), rowIndex)) &&
                    (isNotBlank(get(processor.getCollaboratorSampleId(), rowIndex)) ||
                            isNotBlank(get(processor.getCollaboratorParticipantId(), rowIndex)) ||
                            isNotBlank(get(processor.getBroadParticipantId(), rowIndex)) ||
                            isNotBlank(get(processor.getGender(), rowIndex)) ||
                            isNotBlank(get(processor.getSpecies(), rowIndex)) ||
                            isNotBlank(get(processor.getLsid(), rowIndex)))) {
                return true;
            }
        }
        return false;
    }

    /** Dto containing sample metadata for one or more PooledTube spreadsheet rows. */
    private class SampleDataDto {
        private String rootSampleName;
        private String collaboratorSampleId;
        private String collaboratorParticipantId;
        private String broadParticipantId;
        private String gender;
        private String species;
        private String lsid;

        SampleDataDto(VesselPooledTubesProcessor processor, int rowIndex, int rowOffset,
                MessageCollection messageCollection) {

            rootSampleName = get(processor.getRootSampleId(), rowIndex);
            collaboratorSampleId = get(processor.getCollaboratorSampleId(), rowIndex);
            collaboratorParticipantId = get(processor.getCollaboratorParticipantId(), rowIndex);
            broadParticipantId = get(processor.getBroadParticipantId(), rowIndex);
            gender = get(processor.getGender(), rowIndex);
            species = get(processor.getSpecies(), rowIndex);
            lsid = get(processor.getLsid(), rowIndex);

            // Expects the first occurrance of sample metadata in the spreadsheet to be complete.
            if (isBlank(rootSampleName)) {
                messageCollection.addError(String.format(IS_MISSING,
                        VesselPooledTubesProcessor.Headers.ROOT_SAMPLE_ID.getText(), rowIndex + rowOffset));
            }
            if (isBlank(collaboratorSampleId)) {
                messageCollection.addError(String.format(IS_MISSING,
                        VesselPooledTubesProcessor.Headers.COLLABORATOR_SAMPLE_ID.getText(), rowIndex + rowOffset));
            }
            if (isNotBlank(collaboratorParticipantId) && !this.collaboratorParticipantId
                    .equals(collaboratorParticipantId)) {
                messageCollection.addError(String.format(IS_MISSING,
                        VesselPooledTubesProcessor.Headers.COLLABORATOR_PARTICIPANT_ID.getText(), rowIndex + rowOffset));
            }
            if (isNotBlank(broadParticipantId) && !this.broadParticipantId.equals(broadParticipantId)) {
                messageCollection.addError(String.format(IS_MISSING,
                        VesselPooledTubesProcessor.Headers.BROAD_PARTICIPANT_ID.getText(), rowIndex + rowOffset));
            }
            if (isNotBlank(gender) && !this.gender.equals(gender)) {
                messageCollection.addError(String.format(IS_MISSING,
                        VesselPooledTubesProcessor.Headers.GENDER.getText(), rowIndex + rowOffset));
            }
            if (isNotBlank(species) && !this.species.equals(species)) {
                messageCollection.addError(String.format(IS_MISSING,
                        VesselPooledTubesProcessor.Headers.SPECIES.getText(), rowIndex + rowOffset));
            }
            if (isNotBlank(lsid) && !this.lsid.equals(lsid)) {
                messageCollection.addError(String.format(IS_MISSING,
                        VesselPooledTubesProcessor.Headers.LSID.getText(), rowIndex + rowOffset));
            }
        }

        /** Adds error messages if row has non-blank values that don't match previous found values. */
        void compareToRow(VesselPooledTubesProcessor processor, int rowIndex, int rowOffset,
                MessageCollection messageCollection) {

            String[][] tests = {{get(processor.getRootSampleId(), rowIndex), getRootSampleName(),
                    VesselPooledTubesProcessor.Headers.ROOT_SAMPLE_ID.getText()},
                    {get(processor.getCollaboratorSampleId(), rowIndex), getCollaboratorSampleId(),
                            VesselPooledTubesProcessor.Headers.COLLABORATOR_SAMPLE_ID.getText()},
                    {get(processor.getCollaboratorParticipantId(), rowIndex), getCollaboratorParticipantId(),
                            VesselPooledTubesProcessor.Headers.COLLABORATOR_PARTICIPANT_ID.getText()},
                    {get(processor.getBroadParticipantId(), rowIndex), getBroadParticipantId(),
                            VesselPooledTubesProcessor.Headers.BROAD_PARTICIPANT_ID.getText()},
                    {get(processor.getGender(), rowIndex), getGender(),
                            VesselPooledTubesProcessor.Headers.GENDER.getText()},
                    {get(processor.getSpecies(), rowIndex), getSpecies(),
                            VesselPooledTubesProcessor.Headers.SPECIES.getText()},
                    {get(processor.getLsid(), rowIndex), getLsid(),
                            VesselPooledTubesProcessor.Headers.LSID.getText()}};

            for (String[] test : tests) {
                String actual = test[0];
                String expected = test[1];
                if (isNotBlank(actual) && !actual.equals(expected)) {
                    messageCollection.addError(String.format(IS_CONFLICT, test[2], actual, expected, "",
                            rowIndex + rowOffset));
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

        String getLsid() {
            return lsid;
        }
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

