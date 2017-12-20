package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
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
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VesselPooledTubesProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntity;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntityTsk;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleData;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
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

@Stateful
@RequestScoped
public class SampleInstanceEjb {
    static final String CONFLICT_MESSAGE = "Conflicting value of %s (found \"%s\", expected \"%s\"%s) %s at row %d";
    static final String MISSING_MESSAGE = "Missing value of %s at row %d";
    static final String NUMBER_MESSAGE = "%s must be a number. Found at row %d";
    static final String UNKNOWN_MESSAGE = "The value for %s is unknown in %s. Found at row %d";
    static final String DUPLICATE_MESSAGE = "Duplicate value for %s %s found at row %d";

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
    private SampleDataFetcher sampleDataFetcher;

    /**
     * Verifies the spreadsheet contents and if it's ok, persists the data.
     */
    public void verifyAndPersistSpreadsheet(VesselPooledTubesProcessor processor,
            MessageCollection messageCollection, boolean overWriteFlag) {

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
                    if (isNotBlank(volumeValue) && StringUtils.isNumeric(volumeValue)) {
                        BigDecimal bigDecimal = MathUtils.scaleTwoDecimalPlaces(new BigDecimal(volumeValue));
                        if (bigDecimal.compareTo(BigDecimal.ZERO) > 0) {
                            if (mapBarcodeToVolume.containsKey(barcode) &&
                                    mapBarcodeToVolume.get(barcode).compareTo(bigDecimal) != 0) {
                                messageCollection.addError(String.format(CONFLICT_MESSAGE,
                                        VesselPooledTubesProcessor.Headers.VOLUME.getText(),
                                        bigDecimal.toPlainString(), mapBarcodeToVolume.get(barcode).toPlainString(),
                                        " or blank", "", rowIndex + rowOffset));
                            } else {
                                mapBarcodeToVolume.put(barcode, bigDecimal);
                            }
                        }
                    }
                    String fragmentSizeValue = processor.getFragmentSize().get(rowIndex);
                    if (isNotBlank(fragmentSizeValue)) {
                        if (!StringUtils.isNumeric(fragmentSizeValue)) {
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
                                            mapBarcodeToFragmentSize.get(barcode).toPlainString(), " or blank", "",
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

        Set<String> rootSampleNames = new HashSet<>(processor.getRootSampleId());
        sampleNames.removeAll(Collections.singletonList(""));
        Map<String, MercurySample> rootSampleMap = mercurySampleDao.findMapIdToMercurySample(rootSampleNames);

        Set<String> sampleDataLookups = new HashSet<>();
        Map<String, SampleDataDto> sampleDataDtoMap = new HashMap<>();
        for (int rowIndex = 0; rowIndex < processor.getBarcodes().size(); ++rowIndex) {
            String sampleName = processor.getBroadSampleId().get(rowIndex);
            if (isBlank(sampleName)) {
                continue;
            }
            // For new samples, sample metadata must be supplied in the spreadsheet.
            if (!sampleMap.containsKey(sampleName)) {
                SampleDataDto sampleDataDto = sampleDataDtoMap.get(sampleName);
                String rootSampleName = processor.getRootSampleId().get(rowIndex);
                DtoType dtoType = sampleName.equals(rootSampleName) ? DtoType.BOTH : DtoType.SAMPLE;
                if (sampleDataDto == null) {
                    // Collects metadata and add error messages for any missing fields.
                    sampleDataDtoMap.put(sampleName,
                            new SampleDataDto(dtoType, processor, rowIndex, rowOffset, messageCollection));
                } else {
                    // This new sample has appeared previously in the spreadsheet. The metadata fields
                    // must match previously supplied fields, or be blank.
                    SampleDataDto actual = new SampleDataDto(dtoType, processor, rowIndex);
                    validateMetadata(actual, sampleDataDto, rowIndex + rowOffset, messageCollection);
                }
            } else {
                // For existing samples, the spreadsheet should have blank metadata, and if non-blank
                // the sample name is saved so the existing values can be fetched.
                if (dataIsPresent(processor, sampleName)) {
                    sampleDataLookups.add(sampleName);
                }
            }
        }

        // Collects root sample metadata. Broad samples may be root samples for another spreadsheet row.
        for (int rowIndex = 0; rowIndex < processor.getBarcodes().size(); ++rowIndex) {
            String sampleName = processor.getBroadSampleId().get(rowIndex);
            if (isBlank(sampleName)) {
                continue;
            }
            boolean broadSampleExists = sampleMap.containsKey(sampleName);
            String rootSampleName = processor.getRootSampleId().get(rowIndex);
            boolean rootSampleExists = rootSampleMap.containsKey(rootSampleName);
            if (!broadSampleExists) {
                if (!rootSampleExists) {
                    // Adds the root sample metadata, or checks metadata fields against previously supplied fields.
                    SampleDataDto rootDataDto = sampleDataDtoMap.get(rootSampleName);
                    if (rootDataDto == null) {
                        sampleDataDtoMap.put(rootSampleName,
                                new SampleDataDto(DtoType.ROOT, processor, rowIndex, rowOffset, messageCollection));
                    } else {
                        SampleDataDto actual = new SampleDataDto(DtoType.ROOT, processor, rowIndex);
                        validateMetadata(actual, rootDataDto, rowIndex + rowOffset, messageCollection);
                    }
                } else {
                    // If spreadsheet metadata is non-blank then a lookup will be needed to check the metadata.
                    if (dataIsPresent(processor, rootSampleName)) {
                        sampleDataLookups.add(rootSampleName);
                    }
                }
            } else {
                // If the Broad sample is known then the root is unneeded. If a root sample name is given
                // it must exist and match the Broad sample's root sample.
                if (isNotBlank(rootSampleName)) {
                    if (!rootSampleExists) {
                        messageCollection.addError(String.format(UNKNOWN_MESSAGE,
                                VesselPooledTubesProcessor.Headers.ROOT_SAMPLE_ID.getText(),
                                "Mercury, and cannot be added because the Broad Sample already exists",
                                rowIndex + rowOffset));
                    } else {
                        String expected = sampleMap.get(sampleName).getSampleData().getRootSample();
                        if (isBlank(expected)) {
                            // A blank root sample means the sample serves as its own root.
                            expected = sampleName;
                        }
                        if (!rootSampleName.equals(expected)) {
                            messageCollection.addError(String.format(CONFLICT_MESSAGE,
                                    VesselPooledTubesProcessor.Headers.ROOT_SAMPLE_ID.getText(),
                                    rootSampleExists, expected, " or blank", "", rowIndex + rowOffset));
                        }
                    }
                }
            }
        }


        // Fetches existing metadata for existing Broad or root samples having non-blank metadata.
        // Non-blank spreadsheet values must match existing values, because for this first version
        // the sample metadata cannot be updated from spreadsheet values, even when doing overwriting.
        Map<String, SampleData> fetchedData = sampleDataFetcher.fetchSampleData(sampleDataLookups);
        for (int rowIndex = 0; rowIndex < processor.getBarcodes().size(); ++rowIndex) {
            String sampleName = processor.getBroadSampleId().get(rowIndex);
            String rootSampleName = processor.getRootSampleId().get(rowIndex);
            if (fetchedData.containsKey(sampleName)) {
                String sampleDataRoot = sampleMap.get(sampleName).getSampleData().getRootSample();
                boolean isAlsoRoot = sampleName.equals(rootSampleName) || sampleName.equals(sampleDataRoot) ||
                        isBlank(sampleDataRoot);
                SampleData sampleData = fetchedData.get(sampleName);
                DtoType dtoType = isAlsoRoot ? DtoType.BOTH : DtoType.SAMPLE;
                validateSampleData(new SampleDataDto(dtoType, processor, rowIndex), sampleData,
                        rowIndex + rowOffset,  messageCollection);
            } else if (fetchedData.containsKey(rootSampleName)) {
                validateSampleData(new SampleDataDto(DtoType.ROOT, processor, rowIndex),
                        fetchedData.get(rootSampleName), rowIndex + rowOffset,  messageCollection);
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
                    messageCollection.addError("Must not have both " +
                            VesselPooledTubesProcessor.Headers.BAIT.getText() + " and " +
                            VesselPooledTubesProcessor.Headers.CAT.getText() + " at row " + rowDto.getRowNumber());
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
                    if (StringUtils.isNumeric(processor.getReadLength().get(rowIndex))) {
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

        if (!messageCollection.hasErrors()) {
            // Passes a combined map of Broad and root samples.
            sampleMap.putAll(rootSampleMap);

            // If there are no errors attempt save the data to the database.
            persistResults(rowDtos, mapBarcodeToVessel, mapBarcodeToVolume, mapBarcodeToFragmentSize,
                    sampleMap, sampleDataDtoMap, messageCollection);
        } else {
            // Displays errors in order of increasing row number.
            Collections.sort(messageCollection.getErrors(), new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    int rowCompare = StringUtils.substringAfter(o1, " row ").compareTo(
                            StringUtils.substringAfter(o2, " row "));
                    return (rowCompare == 0) ? o1.compareTo(o2) : rowCompare;
                }
            });
        }
    }

    /**
     * Saves uploaded data after it has been verified.
     *
     * @param mapBarcodeToVessel       contain only tube barcodes of existing lab vessels.
     * @param mapBarcodeToVolume       contains all tube barcodes and their volumes.
     * @param mapBarcodeToFragmentSize contains all tube barcodes and their fragment size.
     * @param combinedSampleMap        contains all broad and root sample names and their mercury samples.
     * @param sampleDataDtoMap         contains sample data for samples which need to be created.
     */
    private void persistResults(List<RowDto> rowDtos, Map<String, LabVessel> mapBarcodeToVessel,
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
            final SampleDataDto sampleDataDto = sampleDataDtoMap.get(sampleName);
            Set<Metadata> metadata = new HashSet<Metadata>() {{
                add(new Metadata(Metadata.Key.MATERIAL_TYPE, MaterialType.DNA.getDisplayName()));
                add(new Metadata(Metadata.Key.SAMPLE_ID, sampleDataDto.getCollaboratorSampleId()));
                add(new Metadata(Metadata.Key.BROAD_PARTICIPANT_ID, sampleDataDto.getBroadParticipantId()));
                add(new Metadata(Metadata.Key.PATIENT_ID, sampleDataDto.getCollaboratorParticipantId()));
                add(new Metadata(Metadata.Key.GENDER, sampleDataDto.getGender()));
                add(new Metadata(Metadata.Key.LSID, sampleDataDto.getLsid()));
                add(new Metadata(Metadata.Key.SPECIES, sampleDataDto.getSpecies()));
            }};
            combinedSampleMap.put(sampleName, new MercurySample(sampleName, metadata));
        }

        List<SampleInstanceEntity> sampleInstanceEntities = new ArrayList<>();
        for (RowDto rowDto : rowDtos) {
            String sampleName = rowDto.getBroadSampleId();
            MercurySample mercurySample = combinedSampleMap.get(sampleName);
            // Adds the root sample linkage to newly created Mercury samples.
            if (sampleDataDtoMap.keySet().contains(sampleName)) {
                ((MercurySampleData)mercurySample.getSampleData()).setRootSampleId(rowDto.getRootSampleId());
            }

            SampleInstanceEntity sampleInstanceEntity = sampleInstanceEntityDao.findByName(rowDto.getLibraryName());
            if (sampleInstanceEntity == null) {
                sampleInstanceEntity = new SampleInstanceEntity();
            }
            sampleInstanceEntity.setMercurySample(mercurySample);
            sampleInstanceEntity.setRootSample(combinedSampleMap.get(rowDto.getRootSampleId()));
            sampleInstanceEntity.setMolecularIndexScheme(rowDto.getMolecularIndexSchemes());
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
            messageCollection.addInfo("Spreadsheet with " + rowDtos.size() + " rows successfully uploaded!");
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
        // Checks for Broad sample metadata or Root sample metadata on each row.
        for (int rowIndex = 0; rowIndex < processor.getBarcodes().size(); ++rowIndex) {
            if ((processor.getBroadSampleId().get(rowIndex).equals(sampleName) &&
                    (isNotBlank(processor.getGender().get(rowIndex)) ||
                            isNotBlank(processor.getSpecies().get(rowIndex)) ||
                            isNotBlank(processor.getLsid().get(rowIndex))))
                    ||
                    (processor.getRootSampleId().get(rowIndex).equals(sampleName) &&
                            (isNotBlank(processor.getCollaboratorSampleId().get(rowIndex)) ||
                                    isNotBlank(processor.getCollaboratorParticipantId().get(rowIndex)) ||
                                    isNotBlank(processor.getBroadParticipantId().get(rowIndex)) ||
                                    isNotBlank(processor.getGender().get(rowIndex)) ||
                                    isNotBlank(processor.getSpecies().get(rowIndex))))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compares previously supplied metadata to the current row's metadata and generates
     * error messages on miscompares.
     */
    private void validateMetadata(SampleDataDto actual, SampleDataDto expected, int rowNumber,
            MessageCollection messages) {
        validateValue(actual.getBroadParticipantId(), expected.getBroadParticipantId(),
                VesselPooledTubesProcessor.Headers.BROAD_PARTICIPANT_ID.getText(), rowNumber, messages);
        validateValue(actual.getRootSampleName(), expected.getRootSampleName(),
                VesselPooledTubesProcessor.Headers.ROOT_SAMPLE_ID.getText(), rowNumber, messages);
        validateValue(actual.getCollaboratorSampleId(), expected.getCollaboratorSampleId(),
                VesselPooledTubesProcessor.Headers.COLLABORATOR_SAMPLE_ID.getText(), rowNumber, messages);
        validateValue(actual.getCollaboratorParticipantId(), expected.getCollaboratorParticipantId(),
                VesselPooledTubesProcessor.Headers.COLLABORATOR_PARTICIPANT_ID.getText(), rowNumber, messages);
        validateValue(actual.getGender(), expected.getGender(),
                VesselPooledTubesProcessor.Headers.GENDER.getText(), rowNumber, messages);
        validateValue(actual.getSpecies(), expected.getSpecies(),
                VesselPooledTubesProcessor.Headers.SPECIES.getText(), rowNumber, messages);
        validateValue(actual.getLsid(), expected.getLsid(),
                VesselPooledTubesProcessor.Headers.LSID.getText(), rowNumber, messages);
    }

    /**
     * Compares non-null values and adds an error message if unequal. This works on different
     * dto types (sample, root, or both) by ignoring the comparison if either the actual or the
     * expected value is null, since a true null value is never present in spreadsheet data.
     * @param actual value to compare
     * @param expected value to compare
     * @param column name of the column, for error message
     * @param rowNumber spreadsheet row number, for error message
     * @param messages the collection of error messages to add to
     */
    private void validateValue(String actual, String expected, String column, int rowNumber,
            MessageCollection messages) {
        if (expected != null && actual != null && !expected.equals(actual)) {
            messages.addError(String.format(CONFLICT_MESSAGE, column, actual, expected, "", "", rowNumber));
        }
    }

    /**
     * Compares existing metadata to the spreadsheet metadata.
     * @param actual what the spreadsheet has.
     * @param expected what the existing value are.
     * @param rowNumber spreadsheet row number.
     * @param messages any errors found are added to the MessageCollection.
     */
    private void validateSampleData(SampleDataDto actual, SampleData expected, int rowNumber,
            MessageCollection messages) {
        validateSampleDataValue(actual.getBroadParticipantId(), expected.getPatientId(),
                VesselPooledTubesProcessor.Headers.BROAD_PARTICIPANT_ID.getText(), rowNumber, messages);
        validateSampleDataValue(actual.getRootSampleName(), expected.getRootSample(),
                VesselPooledTubesProcessor.Headers.ROOT_SAMPLE_ID.getText(), rowNumber, messages);
        validateSampleDataValue(actual.getCollaboratorSampleId(), expected.getCollaboratorsSampleName(),
                VesselPooledTubesProcessor.Headers.COLLABORATOR_SAMPLE_ID.getText(), rowNumber, messages);
        validateSampleDataValue(actual.getCollaboratorParticipantId(), expected.getCollaboratorParticipantId(),
                VesselPooledTubesProcessor.Headers.COLLABORATOR_PARTICIPANT_ID.getText(), rowNumber, messages);
        validateSampleDataValue(actual.getGender(), expected.getGender(),
                VesselPooledTubesProcessor.Headers.GENDER.getText(), rowNumber, messages);
        validateSampleDataValue(actual.getSpecies(), expected.getOrganism(),
                VesselPooledTubesProcessor.Headers.SPECIES.getText(), rowNumber, messages);
        validateSampleDataValue(actual.getLsid(), expected.getSampleLsid(),
                VesselPooledTubesProcessor.Headers.LSID.getText(), rowNumber, messages);
    }

    private void validateSampleDataValue(String actual, String expected, String column, int rowNumber,
            MessageCollection messages) {
        // In this case if spreadsheet data is blank it means the metadata is not supplied by the spreadsheet,
        // and should not be compared.
        if (isNotBlank(actual) && !actual.equals(expected)) {
            messages.addError(String.format(CONFLICT_MESSAGE, column, actual, expected, "", "", rowNumber));
        }
    }

    /** The SampleDataDto types. */
    enum DtoType {SAMPLE, ROOT, BOTH};

    /**
     * This DTO is for sample metadata needed for newly created samples.
     * Expects the first row of sample metadata in the spreadsheet have all the fields.
     */
    class SampleDataDto {
        private final String sampleName;
        private final String rootSampleName;
        private final String collaboratorSampleId;
        private final String collaboratorParticipantId;
        private final String broadParticipantId;
        private final String gender;
        private final String species;
        private final String lsid;

        public SampleDataDto(DtoType dtoType, VesselPooledTubesProcessor processor, int rowIndex) {
            sampleName = (dtoType == DtoType.ROOT) ?
                    processor.getRootSampleId().get(rowIndex) : processor.getBroadSampleId().get(rowIndex);
            collaboratorSampleId = (dtoType == DtoType.ROOT || dtoType == DtoType.BOTH) ?
                    processor.getCollaboratorSampleId().get(rowIndex) : null;
            collaboratorParticipantId = (dtoType == DtoType.ROOT || dtoType == DtoType.BOTH) ?
                    processor.getCollaboratorParticipantId().get(rowIndex) : null;
            broadParticipantId = (dtoType == DtoType.ROOT || dtoType == DtoType.BOTH) ?
                    processor.getBroadParticipantId().get(rowIndex) : null;
            gender = processor.getGender().get(rowIndex);
            species = processor.getSpecies().get(rowIndex);
            rootSampleName = (dtoType == DtoType.SAMPLE) ? processor.getRootSampleId().get(rowIndex) : null;
            lsid = (dtoType == DtoType.SAMPLE || dtoType == DtoType.BOTH) ? processor.getLsid().get(rowIndex) : null;
	}

        public SampleDataDto(DtoType dtoType, VesselPooledTubesProcessor processor, int rowIndex,
                int rowOffset, MessageCollection messageCollection) {

            this(dtoType, processor, rowIndex);

            final int rowNumber = rowIndex + rowOffset;

            // An empty string value indicates a missing value in the spreadsheet; null value indicates
            // the value does not apply to this dto type and should be skipped.
            if ("".equals(rootSampleName)) {
                messageCollection.addError(String.format(MISSING_MESSAGE,
                        VesselPooledTubesProcessor.Headers.ROOT_SAMPLE_ID.getText(), rowNumber));
            }
            if ("".equals(collaboratorSampleId)) {
                messageCollection.addError(String.format(MISSING_MESSAGE,
                        VesselPooledTubesProcessor.Headers.COLLABORATOR_SAMPLE_ID.getText(), rowNumber));
            }
            if ("".equals(collaboratorParticipantId)) {
                messageCollection.addError(String.format(MISSING_MESSAGE,
                        VesselPooledTubesProcessor.Headers.COLLABORATOR_PARTICIPANT_ID.getText(), rowNumber));
            }
            if ("".equals(broadParticipantId)) {
                messageCollection.addError(String.format(MISSING_MESSAGE,
                        VesselPooledTubesProcessor.Headers.BROAD_PARTICIPANT_ID.getText(), rowNumber));
            }
            if ("".equals(gender)) {
                messageCollection.addError(String.format(MISSING_MESSAGE,
                        VesselPooledTubesProcessor.Headers.GENDER.getText(), rowNumber));
            }
            if ("".equals(species)) {
                messageCollection.addError(String.format(MISSING_MESSAGE,
                        VesselPooledTubesProcessor.Headers.SPECIES.getText(), rowNumber));
            }
            if ("".equals(lsid)) {
                messageCollection.addError(String.format(MISSING_MESSAGE,
                        VesselPooledTubesProcessor.Headers.LSID.getText(), rowNumber));
            }
        }

        public String getSampleName() {
            return sampleName;
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

}