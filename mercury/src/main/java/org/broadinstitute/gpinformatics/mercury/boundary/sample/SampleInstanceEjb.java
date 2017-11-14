package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUtil;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleInstanceEntityDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleKitRequestDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessor;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VesselPooledTubesProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntity;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntityTsk;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleKitRequest;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
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

public class SampleInstanceEjb {
    static final String CONFLICT_MESSAGE = "Conflicting value of %s (found \"%s\", expected \"%s\") %s at row %d";
    static final String MISSING_MESSAGE = "Missing value of %s at row %d";
    static final String NUMBER_MESSAGE = "%s must be a number. Found at row %d";
    static final String UNKNOWN_MESSAGE = "The value for %s is unknown in %s. Found at row %d";
    static final String DUPLICATE_MESSAGE = "Duplicate value for %s %s found at row %d";
    public static final String SUCCESS_MESSAGE = "Spreadsheet with %d rows successfully uploaded";
    // The only characters allowed in a library name.
    private final String LIBRARY_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-_";

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

    public SampleInstanceEjb() {
    }

    /** Constructor used for unit testing. */
    public SampleInstanceEjb(MolecularIndexingSchemeDao molecularIndexingSchemeDao, JiraService jiraService,
            ReagentDesignDao reagentDesignDao, LabVesselDao labVesselDao, MercurySampleDao mercurySampleDao,
            SampleInstanceEntityDao sampleInstanceEntityDao, ProductOrderDao productOrderDao,
            SampleKitRequestDao sampleKitRequestDao, SampleDataFetcher sampleDataFetcher) {
        this.molecularIndexingSchemeDao = molecularIndexingSchemeDao;
        this.jiraService = jiraService;
        this.reagentDesignDao = reagentDesignDao;
        this.labVesselDao = labVesselDao;
        this.mercurySampleDao = mercurySampleDao;
        this.sampleInstanceEntityDao = sampleInstanceEntityDao;
        this.productOrderDao = productOrderDao;
        this.sampleKitRequestDao = sampleKitRequestDao;
        this.sampleDataFetcher = sampleDataFetcher;
    }

    /**
     * Verifies the spreadsheet contents and if it's ok, persists the data.
     */
    public void verifyAndPersistPooledTubeSpreadsheet(VesselPooledTubesProcessor processor,
            MessageCollection messageCollection, boolean overWriteFlag) {

        // Adds any validation
        // rowOffset converts row index to the 1-based row number shown by Excel.
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
                                messageCollection.addError(String.format(CONFLICT_MESSAGE,
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
                            messageCollection.addError(String.format(NUMBER_MESSAGE,
                                    VesselPooledTubesProcessor.Headers.FRAGMENT_SIZE.getText(), rowIndex + rowOffset));
                        } else {
                            BigDecimal bigDecimal = MathUtils.scaleTwoDecimalPlaces(new BigDecimal(fragmentSizeValue));
                            if (bigDecimal.compareTo(BigDecimal.ZERO) > 0) {
                                if (mapBarcodeToFragmentSize.containsKey(barcode) &&
                                        mapBarcodeToFragmentSize.get(barcode).compareTo(bigDecimal) != 0) {
                                    messageCollection.addError(String.format(CONFLICT_MESSAGE,
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
                    messageCollection.addError(String.format(MISSING_MESSAGE,
                            VesselPooledTubesProcessor.Headers.VOLUME.getText(), rowIndex + rowOffset));
                    break;
                }
            }
        }
        for (String barcode : CollectionUtils.subtract(uniqueBarcodes, mapBarcodeToFragmentSize.keySet())) {
            for (int rowIndex = 0; rowIndex < processor.getBarcodes().size(); ++rowIndex) {
                if (processor.getBarcodes().get(rowIndex).equals(barcode)) {
                    // Shows the error only for the first row containing the barcode.
                    messageCollection.addError(String.format(MISSING_MESSAGE,
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
                            messageCollection.addError(String.format(CONFLICT_MESSAGE, actualExpectedHeaders[i][2],
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
                    messageCollection.addError(String.format(MISSING_MESSAGE,
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
                    messageCollection.addError(String.format(MISSING_MESSAGE,
                            VesselPooledTubesProcessor.Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText(),
                            rowDto.getRowNumber()));
                } else {
                    if (!uniqueLibraryNames.add(libraryName)) {
                        messageCollection.addError(String.format(DUPLICATE_MESSAGE,
                                VesselPooledTubesProcessor.Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText(), "",
                                rowDto.getRowNumber()));
                    }
                    if (sampleInstanceEntityDao.findByName(libraryName) != null && !overWriteFlag) {
                        messageCollection.addError("Library \"" + libraryName + "\" at row " + rowDto.getRowNumber() +
                                " already exists. Set Overwrite checkbox to re-upload it.");
                    }
                }

                rowDto.setBarcode(barcode);
                if (isBlank(barcode)) {
                    messageCollection.addError(String.format(MISSING_MESSAGE,
                            VesselPooledTubesProcessor.Headers.TUBE_BARCODE.getText(), rowDto.getRowNumber()));
                } else if (mapBarcodeToVessel.get(barcode) != null && !overWriteFlag) {
                    messageCollection.addError("Tube \"" + barcode + "\" at row " + rowDto.getRowNumber() +
                            " already exists. Set Overwrite checkbox to re-upload it.");
                }

                rowDto.setBroadSampleId(broadSampleName);
                if (isBlank(broadSampleName)) {
                    messageCollection.addError(String.format(MISSING_MESSAGE,
                            VesselPooledTubesProcessor.Headers.BROAD_SAMPLE_ID.getText(), rowDto.getRowNumber()));
                } else {
                    if (!sampleMap.containsKey(broadSampleName) && BSPUtil.isInBspFormat(broadSampleName)) {
                        // Errors if it's a new Broad Sample name that could collide with a future BSP SM-id.
                        messageCollection.addError("A new Broad Sample \"" + broadSampleName + "\"" +
                                " must not have a BSP sample name format. Found at row " + rowDto.getRowNumber());
                    }
                }

                String rootSampleName = processor.getRootSampleId().get(rowIndex);
                rowDto.setRootSampleId(rootSampleName);

                //Does molecular index scheme exist.
                String misName = processor.getMolecularIndexingScheme().get(rowIndex);
                if (isBlank(misName)) {
                    messageCollection.addError(String.format(MISSING_MESSAGE,
                            VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText(),
                            rowDto.getRowNumber()));
                } else {
                    MolecularIndexingScheme molecularIndexingScheme = molecularIndexingSchemeDao.findByName(misName);
                    rowDto.setMolecularIndexSchemes(molecularIndexingScheme);
                    if (molecularIndexingScheme == null) {
                        messageCollection.addError(String.format(UNKNOWN_MESSAGE,
                                VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText(), "Mercury",
                                rowDto.getRowNumber()));
                    }
                    // Errors if a tube has duplicate Molecular Index Scheme.
                    if (!barcodeAndMis.add(barcode + "_" + misName)) {
                        messageCollection.addError(String.format(DUPLICATE_MESSAGE,
                                VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText(),
                                "in tube " + barcode, rowDto.getRowNumber()));
                    }
                    // Warns if the spreadsheet has duplicate combination of Broad Sample and Molecular Index Scheme
                    // (in different tubes). It's not an error as long as the tubes don't get pooled later on, which
                    // isn't known at upload time.
                    String sampleMis = broadSampleName + ", " + misName;
                    if (!uniqueSampleMis.add(sampleMis)) {
                        messageCollection.addWarning(String.format(DUPLICATE_MESSAGE,
                                VesselPooledTubesProcessor.Headers.BROAD_SAMPLE_ID.getText() + " and " +
                                        VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText(),
                                "(" + sampleMis + ")", rowDto.getRowNumber()));
                    }
                }

                // Either bait or cat may be specified, or neither.
                String bait = processor.getBait().get(rowIndex);
                String cat = processor.getCat().get(rowIndex);
                ReagentDesign reagentDesign = null;
                if (!bait.isEmpty() && !cat.isEmpty()) {
                    messageCollection.addError(String.format(CONFLICT_MESSAGE,
                            VesselPooledTubesProcessor.Headers.BAIT.getText() + " and " +
                                    VesselPooledTubesProcessor.Headers.CAT.getText(),
                            "both", "only one", "", rowDto.getRowNumber()));
                } else if (!bait.isEmpty()) {
                    reagentDesign = reagentDesignDao.findByBusinessKey(bait);
                    if (reagentDesign == null) {
                        messageCollection.addError(String.format(UNKNOWN_MESSAGE,
                                VesselPooledTubesProcessor.Headers.BAIT.getText(), "Mercury", rowDto.getRowNumber()));
                    }
                } else if (!cat.isEmpty()) {
                    reagentDesign = reagentDesignDao.findByBusinessKey(cat);
                    if (reagentDesign == null) {
                        messageCollection.addError(String.format(UNKNOWN_MESSAGE,
                                VesselPooledTubesProcessor.Headers.CAT.getText(), "Mercury", rowDto.getRowNumber()));
                    }
                }
                rowDto.setReagent(reagentDesign);

                // Collects the Jira ticket experiment and Jira sub-task conditions, to validate later.
                String experiment = processor.getExperiment().get(rowIndex);
                rowDto.setExperiment(experiment);
                if (isBlank(experiment)) {
                    messageCollection.addError(String.format(MISSING_MESSAGE,
                            VesselPooledTubesProcessor.Headers.EXPERIMENT.getText(), rowDto.getRowNumber()));
                }

                List<String> conditions = processor.getConditions().get(rowIndex);
                rowDto.setConditions(conditions);
                if (CollectionUtils.isEmpty(conditions)) {
                    messageCollection.addError(String.format(MISSING_MESSAGE,
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
                        messageCollection.addError(String.format(NUMBER_MESSAGE,
                                VesselPooledTubesProcessor.Headers.READ_LENGTH.getText(), rowDto.getRowNumber()));
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
                        messageCollection.addError(String.format(UNKNOWN_MESSAGE,
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
                    messageCollection.addError(String.format(UNKNOWN_MESSAGE,
                            VesselPooledTubesProcessor.Headers.CONDITIONS.getText(), "sub-tasks of " + experiment,
                            rowNumber));
                }
            }
        }

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
            persistPooledTubes(rowDtos, mapBarcodeToVessel, mapBarcodeToVolume, mapBarcodeToFragmentSize,
                    sampleMap, sampleDataDtoMap, messageCollection);
        }
        Collections.sort(messageCollection.getErrors(), BY_ROW_NUMBER);
    }

    /**
     * Saves PooledTube uploads into LabVessels and SampleInstanceEntities.
     *
     * @param mapBarcodeToVessel       contain only tube barcodes of existing lab vessels.
     * @param mapBarcodeToVolume       contains all tube barcodes and their volumes.
     * @param mapBarcodeToFragmentSize contains all tube barcodes and their fragment size.
     * @param combinedSampleMap        contains all broad and root sample names and their mercury samples.
     * @param sampleDataDtoMap         contains sample data for samples which need to be created, both with
     *                                 and without metadata.
     */
    private void persistPooledTubes(List<RowDto> rowDtos, Map<String, LabVessel> mapBarcodeToVessel,
            Map<String, BigDecimal> mapBarcodeToVolume, Map<String, BigDecimal> mapBarcodeToFragmentSize,
            Map<String, MercurySample> combinedSampleMap, Map<String, SampleDataDto> sampleDataDtoMap,
            MessageCollection messageCollection) {

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
            sampleInstanceEntity.setUploadDate();
            sampleInstanceEntity.removeSubTasks();

            // Persists the experiment conditions in the order they appeared on the row.
            for (String subTask : rowDto.getConditions()) {
                SampleInstanceEntityTsk sampleInstanceEntityTsk = new SampleInstanceEntityTsk();
                sampleInstanceEntityTsk.setSubTask(subTask);
                sampleInstanceEntity.addSubTasks(sampleInstanceEntityTsk);
            }

            sampleInstanceEntities.add(sampleInstanceEntity);

            // Also puts the sample in tube.
            mapBarcodeToVessel.get(rowDto.getBarcode()).addSample(sampleInstanceEntity.getMercurySample());
        }

        labVesselDao.persistAll(newLabVessels);
        sampleInstanceEntityDao.persistAll(sampleInstanceEntities);

        if (!rowDtos.isEmpty()) {
            messageCollection.addInfo(String.format(SUCCESS_MESSAGE, rowDtos.size()));
        } else {
            messageCollection.addError("No valid data found.");
        }
    }

     /**
     * Adds library size metric to the given lab vessel.
     */
    private void addLibrarySize(LabVessel labVessel, BigDecimal librarySize) {
        // Does not add the same value if it already exists for the that vessel.
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

   /** Returns true if the spreadsheet has any sample metadata for the given sample name. */
    private boolean dataIsPresent(VesselPooledTubesProcessor processor, String sampleName) {
        for (int rowIndex = 0; rowIndex < processor.getBarcodes().size(); ++rowIndex) {
            if (processor.getBroadSampleId().get(rowIndex).equals(sampleName) &&
                    (isNotBlank(processor.getCollaboratorSampleId().get(rowIndex)) ||
                            isNotBlank(processor.getCollaboratorParticipantId().get(rowIndex)) ||
                            isNotBlank(processor.getBroadParticipantId().get(rowIndex)) ||
                            isNotBlank(processor.getGender().get(rowIndex)) ||
                            isNotBlank(processor.getSpecies().get(rowIndex)) ||
                            isNotBlank(processor.getLsid().get(rowIndex)))) {
                return true;
            }
        }
        return false;
    }

    /** The sample metadata needed for new samples. */
    private class SampleDataDto {
        private String rootSampleName;
        private String collaboratorSampleId;
        private String collaboratorParticipantId;
        private String broadParticipantId;
        private String gender;
        private String species;
        private String lsid;

        public SampleDataDto(VesselPooledTubesProcessor processor, int rowIndex, int rowOffset,
                MessageCollection messageCollection) {

            rootSampleName = processor.getRootSampleId().get(rowIndex);
            collaboratorSampleId = processor.getCollaboratorSampleId().get(rowIndex);
            collaboratorParticipantId = processor.getCollaboratorParticipantId().get(rowIndex);
            broadParticipantId = processor.getBroadParticipantId().get(rowIndex);
            gender = processor.getGender().get(rowIndex);
            species = processor.getSpecies().get(rowIndex);
            lsid = processor.getLsid().get(rowIndex);

            // Expects the first occurrance of sample metadata in the spreadsheet to be complete.
            if (isBlank(rootSampleName)) {
                messageCollection.addError(String.format(MISSING_MESSAGE,
                        VesselPooledTubesProcessor.Headers.ROOT_SAMPLE_ID.getText(), rowIndex + rowOffset));
            }
            if (isBlank(collaboratorSampleId)) {
                messageCollection.addError(String.format(MISSING_MESSAGE,
                        VesselPooledTubesProcessor.Headers.COLLABORATOR_SAMPLE_ID.getText(), rowIndex + rowOffset));
            }
            if (isNotBlank(collaboratorParticipantId) && !this.collaboratorParticipantId
                    .equals(collaboratorParticipantId)) {
                messageCollection.addError(String.format(MISSING_MESSAGE,
                        VesselPooledTubesProcessor.Headers.COLLABORATOR_PARTICIPANT_ID.getText(), rowIndex + rowOffset));
            }
            if (isNotBlank(broadParticipantId) && !this.broadParticipantId.equals(broadParticipantId)) {
                messageCollection.addError(String.format(MISSING_MESSAGE,
                        VesselPooledTubesProcessor.Headers.BROAD_PARTICIPANT_ID.getText(), rowIndex + rowOffset));
            }
            if (isNotBlank(gender) && !this.gender.equals(gender)) {
                messageCollection.addError(String.format(MISSING_MESSAGE,
                        VesselPooledTubesProcessor.Headers.GENDER.getText(), rowIndex + rowOffset));
            }
            if (isNotBlank(species) && !this.species.equals(species)) {
                messageCollection.addError(String.format(MISSING_MESSAGE,
                        VesselPooledTubesProcessor.Headers.SPECIES.getText(), rowIndex + rowOffset));
            }
            if (isNotBlank(lsid) && !this.lsid.equals(lsid)) {
                messageCollection.addError(String.format(MISSING_MESSAGE,
                        VesselPooledTubesProcessor.Headers.LSID.getText(), rowIndex + rowOffset));
            }
        }

        /** Adds error messages if row has non-blank values that don't match previous found values. */
        public void compareToRow(VesselPooledTubesProcessor processor, int rowIndex, int rowOffset,
                MessageCollection messageCollection) {

            String[][] actualExpectedHeader = {{processor.getRootSampleId().get(rowIndex), getRootSampleName(),
                    VesselPooledTubesProcessor.Headers.ROOT_SAMPLE_ID.getText()},
                    {processor.getCollaboratorSampleId().get(rowIndex), getCollaboratorSampleId(),
                            VesselPooledTubesProcessor.Headers.COLLABORATOR_SAMPLE_ID.getText()},
                    {processor.getCollaboratorParticipantId().get(rowIndex), getCollaboratorParticipantId(),
                            VesselPooledTubesProcessor.Headers.COLLABORATOR_PARTICIPANT_ID.getText()},
                    {processor.getBroadParticipantId().get(rowIndex), getBroadParticipantId(),
                            VesselPooledTubesProcessor.Headers.BROAD_PARTICIPANT_ID.getText()},
                    {processor.getGender().get(rowIndex), getGender(),
                            VesselPooledTubesProcessor.Headers.GENDER.getText()},
                    {processor.getSpecies().get(rowIndex), getSpecies(),
                            VesselPooledTubesProcessor.Headers.SPECIES.getText()},
                    {processor.getLsid().get(rowIndex), getLsid(),
                            VesselPooledTubesProcessor.Headers.LSID.getText()}};

            for (int i = 0; i < actualExpectedHeader.length; ++i) {
                if (isNotBlank(actualExpectedHeader[i][0]) && !actualExpectedHeader[i][0]
                        .equals(actualExpectedHeader[i][1])) {
                    messageCollection.addError(String.format(CONFLICT_MESSAGE, actualExpectedHeader[i][2],
                            actualExpectedHeader[i][0], actualExpectedHeader[i][1], "", rowIndex + rowOffset));
                }
            }
        }

        public String getRootSampleName() {
            return rootSampleName;
        }

        public String getCollaboratorSampleId() {
            return collaboratorSampleId;
        }

        public String getCollaboratorParticipantId() {
            return collaboratorParticipantId;
        }

        public String getBroadParticipantId() {
            return broadParticipantId;
        }

        public String getGender() {
            return gender;
        }

        public String getSpecies() {
            return species;
        }

        public String getLsid() {
            return lsid;
        }
    }

    /** Dto containing data in a spreadsheet row. */
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

        public String getBarcode() {
            return barcode;
        }

        public void setBarcode(String barcode) {
            this.barcode = barcode;
        }

        public MolecularIndexingScheme getMolecularIndexSchemes() {
            return molecularIndexSchemes;
        }

        public void setMolecularIndexSchemes(MolecularIndexingScheme molecularIndexSchemes) {
            this.molecularIndexSchemes = molecularIndexSchemes;
        }

        public ReagentDesign getReagent() {
            return reagent;
        }

        public void setReagent(ReagentDesign reagent) {
            this.reagent = reagent;
        }

        public String getBroadSampleId() {
            return broadSampleId;
        }

        public void setBroadSampleId(String broadSampleId) {
            this.broadSampleId = broadSampleId;
        }

        public String getRootSampleId() {
            return rootSampleId;
        }

        public void setRootSampleId(String rootSampleId) {
            this.rootSampleId = rootSampleId;
        }

        public String getLibraryName() {
            return libraryName;
        }

        public void setLibraryName(String libraryName) {
            this.libraryName = libraryName;
        }

        public BigDecimal getVolume() {
            return volume;
        }

        public void setVolume(BigDecimal volume) {
            this.volume = volume;
        }

        public String getExperiment() {
            return experiment;
        }

        public void setExperiment(String experiment) {
            this.experiment = experiment;
        }

        public List<String> getConditions() {
            return conditions;
        }

        public void setConditions(List<String> conditions) {
            this.conditions = conditions;
        }

        public Integer getReadLength() {
            return readLength;
        }

        public void setReadLength(Integer readLength) {
            this.readLength = readLength;
        }

        public int getRowNumber() {
            return rowNumber;
        }
    }

    /**
     * Verifies and persists External Library Upload spreadsheet.
     */
    public void verifyAndPersistExternalLibrary(ExternalLibraryProcessor processor,
            MessageCollection messageCollection, boolean overWriteFlag) {
        int entityCount = processor.getSingleSampleLibraryName().size();

        // Adds any missing column validations that were already done by the TableProcessor superclass.
        messageCollection.addErrors(processor.getMessages());

        // These lists correspond 1-to-1 with processor rows.
        List<ResearchProject> researchProjects = new ArrayList<>(entityCount);
        List<ProductOrder> productOrders = new ArrayList<>(entityCount);
        List<MolecularIndexingScheme> molecularIndexingSchemes = new ArrayList<>(entityCount);
        List<LabVessel> labVessels = new ArrayList<>(entityCount);
        List<MercurySample> mercurySamples = new ArrayList<>(entityCount);
        List<SampleInstanceEntity> sampleInstanceEntities = new ArrayList<>(entityCount);

        for (int index = 0; index < entityCount; ++index) {
            // Converts the 0-based index into the spreadsheet row number shown at the far left side in Excel.
            int rowNumber = index + processor.getHeaderRowIndex() + 2;

            if (!NumberUtils.isNumber(processor.getTotalLibraryVolume().get(index))) {
                messageCollection.addError("Tube volume is not a valid number. Found at row " + rowNumber);
            }
            if (!NumberUtils.isNumber(processor.getTotalLibraryConcentration().get(index))) {
                messageCollection.addError("Tube concentration is not a valid number. Found at row " + rowNumber);
            }

            MolecularIndexingScheme molecularIndexingScheme = molecularIndexingSchemeDao.findByName(
                    processor.getMolecularBarcodeName().get(index));
            if (molecularIndexingScheme == null) {
                messageCollection.addError(String.format(UNKNOWN_MESSAGE, "Molecular Barcode Name", "Mercury",
                        rowNumber));
            } else {
                molecularIndexingSchemes.add(index, molecularIndexingScheme);
            }

            ProductOrder productOrder = productOrderDao.findByTitle(processor.getProjectTitle().get(index));
            if (productOrder == null) {
                messageCollection.addError(String.format(UNKNOWN_MESSAGE, "Project Title", "Mercury", rowNumber));
            } else {
                productOrders.add(index, productOrder);

                String productAnalysisType = productOrder.getProduct() == null ?
                        "null" : productOrder.getProduct().getAnalysisTypeKey();
                if (!processor.getDataAnalysisType().get(index).equalsIgnoreCase(productAnalysisType)) {
                    messageCollection.addError(String.format(CONFLICT_MESSAGE, "Data Analysis Type",
                            processor.getDataAnalysisType().get(index), productAnalysisType,
                            " since product is " + productOrder.getProduct().getDisplayName(), rowNumber));
                }

                ResearchProject researchProject = productOrder.getResearchProject();
                if (researchProject == null) {
                    messageCollection.addError("ResearchProject is null in " + productOrder.getJiraTicketKey() +
                            ". Referenced by Project Title at row " + rowNumber);
                } else {
                    researchProjects.add(index, researchProject);
                    if (CollectionUtils.isNotEmpty(processor.getIrbNumber())) {
                        String irbNumber = processor.getIrbNumber().get(index);
                        if (!researchProject.getIrbNumbers().contains(irbNumber)) {
                            messageCollection.addError(String.format(CONFLICT_MESSAGE, "IRB Number", irbNumber,
                                    "\"" + StringUtils.join(researchProject.getIrbNumbers(), "\" or \"") + "\"",
                                    (researchProject.hasJiraTicketKey() ?
                                            " from " + researchProject.getJiraTicketKey() : ""), rowNumber));
                        }
                    }
                }
            }

            // Library name is guaranteed to be unique on this spreadsheet, and expected to be universally
            // unique. If one of the given library names is found already in Mercury then the user is
            // expected to have clicked "overwrite".
            // Library name will be used as the sample name and also the tube barcode, so it must
            // have a format that prevents collisions with "SM-nnnn", "SP-nnnn", and 10-digit numbers
            // which already exist in Mercury. Furthermore we restrict the character set.
            String libraryName = processor.getSingleSampleLibraryName().get(index);
            if (!StringUtils.containsOnly(libraryName, LIBRARY_CHARS)) {
                messageCollection.addError("Library Name must contain only the characters '" + LIBRARY_CHARS +
                        "'. Found at row " + rowNumber);
            }
            if (BSPUtil.isInBspFormat(libraryName)) {
                messageCollection.addError(
                        "Library Name must not start with 'SM-' or 'SP-', nor be 10 digits. Found at row " + rowNumber);
            }
            SampleInstanceEntity sampleInstanceEntity = sampleInstanceEntityDao.findByName(libraryName);
            if (sampleInstanceEntity != null && !overWriteFlag) {
                messageCollection.addError("Library " + libraryName +
                        " already exists in Mercury. Found at row " + rowNumber +
                        ". Set Overwrite checkbox to re-upload it.");
            }
            sampleInstanceEntities.add(index, sampleInstanceEntity);

            String barcode = (CollectionUtils.isNotEmpty(processor.getBarcodes()) &&
                    isNotBlank(processor.getBarcodes().get(index))) ? processor.getBarcodes().get(index) : libraryName;
            LabVessel labVessel = labVesselDao.findByIdentifier(barcode);
            if (labVessel != null && !overWriteFlag) {
                messageCollection.addError("A tube having barcode " + barcode +
                        " already exists in Mercury. Found at row " + rowNumber +
                        ". Set Overwrite checkbox to re-upload it.");
            }
            labVessels.add(index, labVessel);

            MercurySample mercurySample = mercurySampleDao.findBySampleKey(libraryName);
            if (mercurySample != null && !overWriteFlag) {
                messageCollection.addError("A sample named " + libraryName +
                        " already exists in Mercury. Found at row " + rowNumber +
                        ". Set Overwrite checkbox to re-upload it.");
            }
            mercurySamples.add(index, mercurySample);
        }

        if (CollectionUtils.isEmpty(processor.getSingleSampleLibraryName())) {
            messageCollection.addWarning("Spreadsheet contains no data.");
        }
        if (!messageCollection.hasErrors()) {
            persistExternalLibraries(processor, researchProjects, productOrders, molecularIndexingSchemes,
                    labVessels, mercurySamples, sampleInstanceEntities, messageCollection);
        } else {
            Collections.sort(messageCollection.getErrors(), BY_ROW_NUMBER);
        }
    }

    /**
     * Persists External Library uploads.
     */
    private void persistExternalLibraries(ExternalLibraryProcessor processor, List<ResearchProject> researchProjects,
            List<ProductOrder> productOrders, List<MolecularIndexingScheme> molecularIndexingSchemes,
            List<LabVessel> labVessels, List<MercurySample> mercurySamples,
            List<SampleInstanceEntity> sampleInstanceEntities, MessageCollection messageCollection) {

        // Captures the pre-row one-off spreadsheet data in a "kit". This is just the upload manifest
        // with no connection with the actual shipped and received kit.
        SampleKitRequest kit = new SampleKitRequest();
        kit.setCollaboratorName(processor.getFirstName() + " " + processor.getLastName());
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

        Collection<Object> newObjects = new ArrayList<>();
        int numberOfEntities = processor.getSingleSampleLibraryName().size();
        for (int index = 0; index < numberOfEntities; ++index) {
            String libraryName = processor.getSingleSampleLibraryName().get(index);

            LabVessel labVessel = labVessels.get(index);
            if (labVessel == null) {
                String barcode = processor.getBarcodes().get(index);
                if (isBlank(barcode)) {
                    barcode = libraryName;
                }
                labVessel = new BarcodedTube(barcode, BarcodedTube.BarcodedTubeType.MatrixTube);
                newObjects.add(labVessel);
            }
            labVessel.setVolume(new BigDecimal(processor.getTotalLibraryVolume().get(index)));
            labVessel.setConcentration(new BigDecimal(processor.getTotalLibraryConcentration().get(index)));

            MercurySample mercurySample = mercurySamples.get(index);
            if (mercurySample == null) {
                mercurySample = new MercurySample(libraryName, MercurySample.MetadataSource.MERCURY);

                Set<Metadata> metadata = new HashSet<>();
                metadata.add(new Metadata(Metadata.Key.SAMPLE_ID, processor.getCollaboratorSampleId().get(index)));
                metadata.add(new Metadata(Metadata.Key.BROAD_PARTICIPANT_ID, processor.getIndividualName().get(index)));
                metadata.add(new Metadata(Metadata.Key.PATIENT_ID, processor.getIndividualName().get(index)));
                metadata.add(new Metadata(Metadata.Key.GENDER, processor.getSex().get(index)));
                metadata.add(new Metadata(Metadata.Key.STRAIN, processor.getStrain().get(index)));
                metadata.add(new Metadata(Metadata.Key.SPECIES, isNotBlank(processor.getOrganism().get(index)) ?
                        processor.getOrganism().get(index) : processor.getSpecies()));
                metadata.add(new Metadata(Metadata.Key.MATERIAL_TYPE, MaterialType.DNA.getDisplayName()));
                mercurySample.addMetadata(metadata);
                newObjects.add(mercurySample);
            }
            mercurySample.addLabVessel(labVessel);

            SampleInstanceEntity sampleInstanceEntity = sampleInstanceEntities.get(index);
            if (sampleInstanceEntities == null) {
                sampleInstanceEntity = new SampleInstanceEntity();
                newObjects.add(sampleInstanceEntity);
                sampleInstanceEntity.setSampleKitRequest(kit);
                sampleInstanceEntity.setSampleLibraryName(libraryName);
            }
            if (isNotBlank(processor.getPooled().get(index))) {
                sampleInstanceEntity.setPooled(processor.getPooled().get(index));
            }
            sampleInstanceEntity.setTissueType(processor.getTissueType().get(index));
            sampleInstanceEntity.setInsertSizeRange(processor.getInsertSizeRangeBp().get(index));
            sampleInstanceEntity.setLibrarySizeRange(processor.getLibrarySizeRangeBp().get(index));
            sampleInstanceEntity.setJumpSize(processor.getJumpSize().get(index));
            sampleInstanceEntity.setRestrictionEnzyme(processor.getRestrictionEnzymes().get(index));
            if (isNotBlank(processor.getDesiredReadLength().get(index))) {
                sampleInstanceEntity.setDesiredReadLength(new Integer(processor.getDesiredReadLength().get(index)));
            }
            sampleInstanceEntity.setReferenceSequence(processor.getReferenceSequence().get(index));
            sampleInstanceEntity.setMolecularIndexScheme(molecularIndexingSchemes.get(index));
            sampleInstanceEntity.setLibraryType(processor.getLibraryType().get(index));
            sampleInstanceEntity.setResearchProject(researchProjects.get(index));
            sampleInstanceEntity.setProductOrder(productOrders.get(index));
            sampleInstanceEntity.setUploadDate();
            sampleInstanceEntity.setLabVessel(labVessel);
            sampleInstanceEntity.setMercurySample(mercurySample);
        }
        sampleInstanceEntityDao.persistAll(newObjects);

        messageCollection.addInfo("Spreadsheet with " + numberOfEntities + " rows successfully uploaded.");
    }

    static Comparator<String> BY_ROW_NUMBER = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            // Does a numeric sort on the row number string, expected to be after the word "row".
            int rowCompare = Integer.parseInt(StringUtils.substringAfter(o1, " row ").split(" ")[0]) -
                    Integer.parseInt(StringUtils.substringAfter(o2, " row ").split(" ")[0]);
            return (rowCompare == 0) ? o1.compareTo(o2) : rowCompare;
        }
    };
}

