package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleData;

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
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUtil.isInBspFormat;
import static org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation_.barcode;

/**
 * This class handles the external library creation from walkup sequencing web serivce calls, pooled tube
 * spreadsheet upload, and the various external library spreadsheet uploads.
 * In all cases a SampleInstanceEntity and associated MercurySample and LabVessel are created or overwritten.
 */
public class SampleInstanceEjb {
    static final String CONFLICT = "Row #%d conflicting value of %s (found \"%s\", expected \"%s\"%s) %s";
    static final String MISSING = "Row #%d missing value of %s";
    static final String WRONG_TYPE = "Row #%d %s must be %s";
    static final String MUST_NOT_BE = "Row #%d %s must not be %s";
    static final String UNKNOWN = "Row #%d the value for %s is not in %s";
    static final String DUPLICATE = "Row #%d duplicate value for %s";
    static final String BSP_FORMAT = "Row #%d the new %s \"%s\" must not have a BSP sample name format";
    static final String MERCURY_FORMAT = "Row #%d the new %s \"%s\" must not have a Mercury barcode format (10 digits)";
    static final String PREXISTING = "Row #%d %s named \"%s\" already exists in Mercury; set the Overwrite checkbox to re-upload.";
    static final String PDO_PROBLEM = "Row #%d no %s is defined for Product Order \"%s\"";

    public static final String IS_SUCCESS = "Spreadsheet with %d rows successfully uploaded";
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
     * Parses the uploaded spreadsheet, checks for correct headers and missing/incorrect data,
     * and if it's all ok then persists the SampleInstanceEntity and associated entities.
     *
     * @param inputStream the spreadsheet inputStream.
     * @param overwrite specifies if existing entities should be overwritten.
     * @param messages the errors, warnings, and info to be passed back.
     */
    public Pair<TableProcessor, List<SampleInstanceEntity>> doExternalUpload(InputStream inputStream, boolean overwrite,
            MessageCollection messages) throws IOException, InvalidFormatException, ValidationException {

        Pair<TableProcessor, List<String>> pair = bestProcessor(inputStream);
        TableProcessor processor = pair.getLeft();
        messages.addErrors(pair.getRight());
        List<SampleInstanceEntity> entities = (processor instanceof VesselPooledTubesProcessor) ?
                verifyAndPersistSpreadsheet((VesselPooledTubesProcessor) processor, messages, overwrite) :
                verifyAndPersistSpreadsheet((ExternalLibraryProcessor) processor, messages, overwrite);

        return Pair.of(processor, entities);
    }

    /**
     * Parses the spreadsheet and returns the best processor for it.
     *
     * @param inputStream the spreadsheet.
     * @return Pair of TableProcessor and the list of errors found when parsing it (ideally none).
     */
    public Pair<TableProcessor, List<String>> bestProcessor(InputStream inputStream) throws IOException {

        SortedMap<Integer, Pair<TableProcessor, List<String>>> processorMap = new TreeMap<>();
        byte[] inputStreamBytes = IOUtils.toByteArray(inputStream);
        for (TableProcessor processor : Arrays.asList(
                new VesselPooledTubesProcessor(null),
                new ExternalLibraryProcessorEzPass(null),
                new ExternalLibraryProcessorPooledMultiOrganism(null),
                new ExternalLibraryProcessorNonPooled(null))) {
            int rank;
            List<String> messages = new ArrayList<>();
            try {
                messages.addAll(PoiSpreadsheetParser.singleWorksheetHeaderErrors(
                        new ByteArrayInputStream(inputStreamBytes), processor));
                // Use warnings about extraneous headers in case multiple processors find all of the required headers.
                rank = processor.getMessages().size() * 100 + processor.getWarnings().size();
                if (processor.getMessages().isEmpty()) {
                    messages.addAll(processor.getWarnings());
                }
            } catch (ValidationException e) {
                messages.addAll(e.getValidationMessages());
                rank = e.getValidationMessages().size() * 100 + processor.getWarnings().size();
            } catch (InvalidFormatException e) {
                messages.add(e.getMessage());
                rank = Integer.MAX_VALUE;
            }
            processorMap.put(rank, Pair.of(processor, messages));
        }
        // The first one has the lowest error count.
        return processorMap.entrySet().iterator().next().getValue();
    }

    /**
     * Verifies the spreadsheet contents and if it's ok, persists the data. If not, adds error messages.
     */
    private List<SampleInstanceEntity> verifyAndPersistSpreadsheet(VesselPooledTubesProcessor processor,
            MessageCollection messages, boolean overWriteFlag) {

        Set<String> volumeErrorReportedForBarcode = new HashSet<>();
        Set<String> fragmentErrorReportedForBarcode = new HashSet<>();

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
                    if (isNotBlank(volumeValue)) {
                        if (!NumberUtils.isNumber(volumeValue)) {
                            messages.addError(String.format(WRONG_TYPE, rowIndex + rowOffset,
                                    VesselPooledTubesProcessor.Headers.VOLUME.getText(), "numeric"));
                            volumeErrorReportedForBarcode.add(barcode);
                        } else {
                            BigDecimal bigDecimal = MathUtils.scaleTwoDecimalPlaces(new BigDecimal(volumeValue));
                            if (bigDecimal.compareTo(BigDecimal.ZERO) > 0) {
                                if (mapBarcodeToVolume.containsKey(barcode) &&
                                        mapBarcodeToVolume.get(barcode).compareTo(bigDecimal) != 0) {
                                    messages.addError(String.format(CONFLICT, rowIndex + rowOffset,
                                            VesselPooledTubesProcessor.Headers.VOLUME.getText(),
                                            bigDecimal.toPlainString(),
                                            mapBarcodeToVolume.get(barcode).toPlainString(), "", ""));
                                    volumeErrorReportedForBarcode.add(barcode);
                                } else {
                                    mapBarcodeToVolume.put(barcode, bigDecimal);
                                }
                            }
                        }
                    }
                    String fragmentSizeValue = processor.getFragmentSize().get(rowIndex);
                    if (isNotBlank(fragmentSizeValue)) {
                        if (!NumberUtils.isNumber(fragmentSizeValue)) {
                            messages.addError(String.format(WRONG_TYPE, rowIndex + rowOffset,
                                    VesselPooledTubesProcessor.Headers.FRAGMENT_SIZE.getText(), "numeric"));
                            fragmentErrorReportedForBarcode.add(barcode);
                        } else {
                            BigDecimal bigDecimal = MathUtils.scaleTwoDecimalPlaces(new BigDecimal(fragmentSizeValue));
                            if (bigDecimal.compareTo(BigDecimal.ZERO) > 0) {
                                if (mapBarcodeToFragmentSize.containsKey(barcode) &&
                                        mapBarcodeToFragmentSize.get(barcode).compareTo(bigDecimal) != 0) {
                                    messages.addError(String.format(CONFLICT, rowIndex + rowOffset,
                                            VesselPooledTubesProcessor.Headers.FRAGMENT_SIZE.getText(),
                                            bigDecimal.toPlainString(),
                                            mapBarcodeToFragmentSize.get(barcode).toPlainString(), "", ""));
                                    fragmentErrorReportedForBarcode.add(barcode);
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
        for (int rowIndex = 0; rowIndex < processor.getBarcodes().size(); ++rowIndex) {
            if (!processor.getRequiredValuesPresent().get(rowIndex)) {
                continue;
            }
            String barcode = get(processor.getBarcodes(), rowIndex);
            if (!mapBarcodeToVolume.containsKey(barcode) && volumeErrorReportedForBarcode.add(barcode)) {
                // Shows the error only for the first row containing the barcode.
                messages.addError(String.format(MISSING, rowIndex + rowOffset,
                        VesselPooledTubesProcessor.Headers.VOLUME.getText()));
            }
            if (!mapBarcodeToFragmentSize.containsKey(barcode) && fragmentErrorReportedForBarcode.add(barcode)) {
                // Shows the error only for the first row containing the barcode.
                messages.addError(String.format(MISSING, rowIndex + rowOffset,
                        VesselPooledTubesProcessor.Headers.FRAGMENT_SIZE.getText()));
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
            if (!processor.getRequiredValuesPresent().get(rowIndex)) {
                continue;
            }
            String sampleName = processor.getBroadSampleId().get(rowIndex);
            // For new samples, sample metadata must be supplied in the spreadsheet.
            if (!sampleMap.containsKey(sampleName)) {

                SampleDataDto sampleDataDto = sampleDataDtoMap.get(sampleName);
                String rootSampleName = processor.getRootSampleId().get(rowIndex);
                DtoType dtoType = sampleName.equals(rootSampleName) ? DtoType.BOTH : DtoType.SAMPLE;
                if (sampleDataDto == null) {
                    // Collects metadata and add error messages for any missing fields.
                    sampleDataDtoMap.put(sampleName,
                            new SampleDataDto(dtoType, processor, rowIndex, rowOffset, messages));
                } else {
                    // This new sample has appeared previously in the spreadsheet. The metadata fields
                    // must match previously supplied fields, or be blank.
                    SampleDataDto actual = new SampleDataDto(dtoType, processor, rowIndex);
                    validateMetadata(actual, sampleDataDto, rowIndex + rowOffset, messages);
                }
            } else {
                // For existing samples, the spreadsheet should have blank metadata, and if non-blank
                // the sample name is saved so the existing values can be fetched.
                if (dataIsPresent(processor, sampleName)) {
                    sampleDataLookups.add(sampleName);
                }
            }
        }
        // todo emp XXX update existing metadata when changes are found

        // Collects root sample metadata. Broad samples may be root samples for another spreadsheet row.
        for (int rowIndex = 0; rowIndex < processor.getBarcodes().size(); ++rowIndex) {
            if (!processor.getRequiredValuesPresent().get(rowIndex)) {
                continue;
            }
            String sampleName = processor.getBroadSampleId().get(rowIndex);
            boolean broadSampleExists = sampleMap.containsKey(sampleName);
            String rootSampleName = processor.getRootSampleId().get(rowIndex);
            boolean rootSampleExists = rootSampleMap.containsKey(rootSampleName);
            if (!broadSampleExists) {
                if (!rootSampleExists) {
                    // Adds the root sample metadata, or checks metadata fields against previously supplied fields.
                    SampleDataDto rootDataDto = sampleDataDtoMap.get(rootSampleName);
                    if (rootDataDto == null) {
                        sampleDataDtoMap.put(rootSampleName,
                                new SampleDataDto(DtoType.ROOT, processor, rowIndex, rowOffset, messages));
                    } else {
                        SampleDataDto actual = new SampleDataDto(DtoType.ROOT, processor, rowIndex);
                        validateMetadata(actual, rootDataDto, rowIndex + rowOffset, messages);
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
                        messages.addError(String.format(UNKNOWN, rowIndex + rowOffset,
                                VesselPooledTubesProcessor.Headers.ROOT_SAMPLE_ID.getText(),
                                "Mercury, and cannot be added because the Broad Sample already exists"));
                    } else {
                        String expected = sampleMap.get(sampleName).getSampleData().getRootSample();
                        if (isBlank(expected)) {
                            // A blank root sample means the sample serves as its own root.
                            expected = sampleName;
                        }
                        if (!rootSampleName.equals(expected)) {
                            messages.addError(String.format(CONFLICT, rowIndex + rowOffset,
                                    VesselPooledTubesProcessor.Headers.ROOT_SAMPLE_ID.getText(),
                                    rootSampleName, expected, " or blank", ""));
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
            if (!processor.getRequiredValuesPresent().get(rowIndex)) {
                continue;
            }
            String sampleName = processor.getBroadSampleId().get(rowIndex);
            String rootSampleName = processor.getRootSampleId().get(rowIndex);
            if (fetchedData.containsKey(sampleName)) {
                String sampleDataRoot = sampleMap.get(sampleName).getSampleData().getRootSample();
                boolean isAlsoRoot = sampleName.equals(rootSampleName) || sampleName.equals(sampleDataRoot) ||
                        isBlank(sampleDataRoot);
                SampleData sampleData = fetchedData.get(sampleName);
                DtoType dtoType = isAlsoRoot ? DtoType.BOTH : DtoType.SAMPLE;
                validateSampleData(new SampleDataDto(dtoType, processor, rowIndex), sampleData,
                        rowIndex + rowOffset,  messages);
            } else if (fetchedData.containsKey(rootSampleName)) {
                validateSampleData(new SampleDataDto(DtoType.ROOT, processor, rowIndex),
                        fetchedData.get(rootSampleName), rowIndex + rowOffset,  messages);

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
            if (!processor.getRequiredValuesPresent().get(rowIndex)) {
                continue;
            }
            String libraryName = processor.getSingleSampleLibraryName().get(rowIndex);
            String barcode = processor.getBarcodes().get(rowIndex);
            String broadSampleName = processor.getBroadSampleId().get(rowIndex);
            RowDto rowDto = new RowDto(rowIndex + rowOffset);
            rowDtos.add(rowDto);

            rowDto.setLibraryName(libraryName);
            if (isBlank(libraryName)) {
                messages.addError(String.format(MISSING, rowDto.getRowNumber(),
                        VesselPooledTubesProcessor.Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText()));
            } else {
                if (!uniqueLibraryNames.add(libraryName)) {
                    messages.addError(String.format(DUPLICATE, rowDto.getRowNumber(),
                            VesselPooledTubesProcessor.Headers.SINGLE_SAMPLE_LIBRARY_NAME.getText()));
                }
                if (sampleInstanceEntityDao.findByName(libraryName) != null && !overWriteFlag) {
                    messages.addError(String.format(PREXISTING, rowDto.getRowNumber(), "Library", libraryName));
                }
            }

            rowDto.setBarcode(barcode);
            if (isBlank(barcode)) {
                messages.addError(String.format(MISSING, rowDto.getRowNumber(),
                        VesselPooledTubesProcessor.Headers.TUBE_BARCODE.getText()));
            }
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

            rowDto.setBroadSampleId(broadSampleName);
            if (isBlank(broadSampleName)) {
                messages.addError(String.format(MISSING, rowDto.getRowNumber(),
                        VesselPooledTubesProcessor.Headers.BROAD_SAMPLE_ID.getText()));
            } else {
                if (!sampleMap.containsKey(broadSampleName) && isInBspFormat(broadSampleName)) {
                    // Errors if it's a new Broad Sample name that could collide with a future BSP SM-id.
                    messages.addError(String.format(BSP_FORMAT, rowDto.getRowNumber(),
                            "Broad Sample", broadSampleName));
                }
            }

            String rootSampleName = processor.getRootSampleId().get(rowIndex);
            rowDto.setRootSampleId(rootSampleName);

            //Does molecular index scheme exist.
            String misName = processor.getMolecularIndexingScheme().get(rowIndex);
            if (isBlank(misName)) {
                messages.addError(String.format(MISSING, rowDto.getRowNumber(),
                        VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText()));
            } else {
                MolecularIndexingScheme molecularIndexingScheme = molecularIndexingSchemeDao.findByName(misName);
                rowDto.setMolecularIndexSchemes(molecularIndexingScheme);
                if (molecularIndexingScheme == null) {
                    messages.addError(String.format(UNKNOWN, rowDto.getRowNumber(),
                            VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText(), "Mercury"));
                }
                // Errors if a tube has duplicate Molecular Index Scheme.
                if (!barcodeAndMis.add(barcode + "_" + misName)) {
                    messages.addError(String.format(DUPLICATE, rowDto.getRowNumber(),
                            VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText() +
                                    " in tube " + barcode));
                }
                // Warns if the spreadsheet has duplicate combination of Broad Sample and Molecular Index Scheme
                // (in different tubes). It's not an error as long as the tubes don't get pooled later on, which
                // isn't known at upload time.
                String sampleMis = broadSampleName + ", " + misName;
                if (!uniqueSampleMis.add(sampleMis)) {
                    messages.addWarning(String.format(DUPLICATE, rowDto.getRowNumber(),
                            VesselPooledTubesProcessor.Headers.BROAD_SAMPLE_ID.getText() + " and " +
                                    VesselPooledTubesProcessor.Headers.MOLECULAR_INDEXING_SCHEME.getText() +
                                    " (" + sampleMis + ")"));
                }
            }

            // Either bait or cat may be specified, or neither.
            String bait = processor.getBait().get(rowIndex);
            String cat = processor.getCat().get(rowIndex);
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
            String experiment = processor.getExperiment().get(rowIndex);
            rowDto.setExperiment(experiment);
            if (isBlank(experiment)) {
                messages.addError(String.format(MISSING, rowDto.getRowNumber(),
                        VesselPooledTubesProcessor.Headers.EXPERIMENT.getText()));
            }

            List<String> conditions = processor.getConditions().get(rowIndex);
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

            if (isNotBlank(processor.getReadLength().get(rowIndex))) {
                if (NumberUtils.isNumber(processor.getReadLength().get(rowIndex))) {
                    rowDto.setReadLength(Integer.parseInt(processor.getReadLength().get(rowIndex)));
                } else {
                    messages.addError(String.format(WRONG_TYPE, rowDto.getRowNumber(),
                            VesselPooledTubesProcessor.Headers.READ_LENGTH.getText(), "numeric"));
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
                    messages.addError(String.format(UNKNOWN, rowNumber,
                            VesselPooledTubesProcessor.Headers.CONDITIONS.getText(), "sub-tasks of " + experiment));
                }
            }
        }

        List<SampleInstanceEntity> sampleInstanceEntities;
        if (!messages.hasErrors()) {
            // Passes a combined map of Broad and root samples.
            sampleMap.putAll(rootSampleMap);
            sampleInstanceEntities = persistPooledTubes(rowDtos, mapBarcodeToVessel, mapBarcodeToVolume,
                    mapBarcodeToFragmentSize, sampleMap, sampleDataDtoMap, messages);
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
     * @param sampleDataDtoMap         contains sample data for samples which need to be created.
     */
    private List<SampleInstanceEntity> persistPooledTubes(List<RowDto> rowDtos,
            Map<String, LabVessel> mapBarcodeToVessel, Map<String, BigDecimal> mapBarcodeToVolume,
            Map<String, BigDecimal> mapBarcodeToFragmentSize, Map<String, MercurySample> combinedSampleMap,
            Map<String, SampleDataDto> sampleDataDtoMap, MessageCollection messages) {

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
            LabVessel labVessel = mapBarcodeToVessel.get(rowDto.getBarcode());

            // Adds the root sample linkage to newly created Mercury samples.
            if (sampleDataDtoMap.keySet().contains(sampleName)) {
                ((MercurySampleData) mercurySample.getSampleData()).setRootSampleId(rowDto.getRootSampleId());
            }

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
     * Verifies and persists External Library Upload spreadsheet. This differs from the
     * PooledTube upload in that there is no explicit barcode nor sample name, and the
     * library name which is unique on the spreadsheet is used for both. This means no
     * pooling can be done for tubes by repeating their barcode in rows of the spreadsheet,
     * and all sample metadata must be present on every row. There is no need for this code
     * to make dtos to track aggregation, since every row will become a sampleInstanceEntity.
     */
    private List<SampleInstanceEntity> verifyAndPersistSpreadsheet(ExternalLibraryProcessor processor,
            MessageCollection messages, boolean overWriteFlag) {

        // Includes error messages due to missing header or value that were discovered when parsing the spreadsheet.
        messages.addErrors(processor.getMessages());

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

        int entityCount = processor.getSingleSampleLibraryName().size();
        for (int index = 0; index < entityCount; ++index) {
            // Converts the 0-based index into the spreadsheet row number shown at the far left side in Excel.
            int rowNumber = index + processor.getHeaderRowIndex() + 2;

            // Library name must only appear in one row, and is assumed to be universally unique. If it is
            // reused then it's unusable unless Overwrite is set. The pipeline and elsewhere require a
            // simple name so disallow whitespace and anything that might cause trouble.
            String libraryName = get(processor.getSingleSampleLibraryName(), index);
            if (!uniqueLibraryNames.add(libraryName)) {
                messages.addError(String.format(DUPLICATE, rowNumber, "Library Name"));
            }
            if (!StringUtils.containsOnly(libraryName, RESTRICTED_CHARS)) {
                messages.addError(String.format(WRONG_TYPE, rowNumber, "Library Name",
                        "composed of " + RESTRICTED_MESSAGE));
            }
            SampleInstanceEntity sampleInstanceEntity = sampleInstanceEntityDao.findByName(libraryName);
            if (sampleInstanceEntity != null) {
                sampleInstanceEntities.put(index, sampleInstanceEntity);
                if (!overWriteFlag) {
                    messages.addError(String.format(PREXISTING, rowNumber, "Library", libraryName));
                }
            }

            // Library name is used as the sample name, and if the sample doesn't already exist then
            // disallow a library name that could cause the sample to collide with a future BSP sample.
            MercurySample mercurySample = mercurySampleDao.findBySampleKey(libraryName);
            if (mercurySample != null) {
                mercurySamples.put(index, mercurySample);
                if (!overWriteFlag) {
                    messages.addError(String.format(PREXISTING, rowNumber, "Sample", libraryName));
                }
            } else if (isInBspFormat(libraryName)) {
                messages.addError(String.format(BSP_FORMAT, rowNumber, "Library Name", libraryName));
            }

            String barcode = get(processor.getBarcodes(), index);
            // Uses the libraryName if a barcode is not present.
            boolean barcodeIsLibrary = barcode == null;
            if (barcodeIsLibrary) {
                barcode = libraryName;
            }
            // Tube barcode must only appear in one row.
            if (!uniqueBarcodes.add(barcode)) {
                messages.addError(String.format(DUPLICATE, rowNumber, "Tube Barcode"));
            }
            LabVessel labVessel = labVesselDao.findByIdentifier(barcode);
            if (labVessel != null) {
                labVessels.put(index, labVessel);
                if (!overWriteFlag) {
                    messages.addError(String.format(PREXISTING, rowNumber, "Tube", barcode));
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
            if (isNotBlank(get(processor.getTotalLibraryVolume(), index)) &&
                    !NumberUtils.isNumber(processor.getTotalLibraryVolume().get(index))) {
                messages.addError(String.format(WRONG_TYPE, rowNumber, "Volume", "numeric"));
            }
            if (isNotBlank(get(processor.getTotalLibraryConcentration(), index)) &&
                    !NumberUtils.isNumber(processor.getTotalLibraryConcentration().get(index))) {
                messages.addError(String.format(WRONG_TYPE, rowNumber, "Concentration", "numeric"));
            }

            nonNegativeOrBlank(get(processor.getReadLength(), index), "Read Length", rowNumber, messages);
            nonNegativeOrBlank(get(processor.getNumberOfLanes(), index), "Numer of Lanes", rowNumber, messages);
            nonNegativeOrBlank(get(processor.getInsertSize(), index), "Insert Size", rowNumber, messages);
            String referenceSequenceName = get(processor.getReferenceSequence(), index);
            if (isNotBlank(referenceSequenceName)) {
                ReferenceSequence referenceSequence = referenceSequenceDao.findCurrent(referenceSequenceName);
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

            String pdoTitle = get(processor.getProjectTitle(), index);
            if (isNotBlank(pdoTitle)) {
                ProductOrder productOrder = productOrderDao.findByTitle(pdoTitle);
                if (productOrder == null) {
                    messages.addError(String.format(UNKNOWN, rowNumber, "Project Title", "Mercury"));
                } else {
                    productOrders.put(index, productOrder);
                    if (productOrder.getProduct() == null) {
                        messages.addError(String.format(PDO_PROBLEM, rowNumber, "Product", pdoTitle));
                    } else {
                        String productAnalysisType =
                                StringUtils.trimToEmpty(productOrder.getProduct().getAnalysisTypeKey());
                        String analysisType = StringUtils.trimToEmpty(get(processor.getDataAnalysisType(), index));
                        if (!analysisType.equalsIgnoreCase(productAnalysisType)) {
                            messages.addError(String.format(CONFLICT, rowNumber, "Data Analysis Type",
                                    analysisType, productAnalysisType, "defined in Product " +
                                            productOrder.getProduct().getPartNumber()));
                        }
                    }
                    ResearchProject researchProject = productOrder.getResearchProject();
                    if (researchProject == null) {
                        messages.addError(String.format(PDO_PROBLEM, rowNumber, "Research Project", pdoTitle));
                    } else {
                        researchProjects.put(index, researchProject);
                        validateIrbNumber(get(processor.getIrbNumber(), index), researchProject, rowNumber,
                                messages);
                    }
                }
            }
            if (isNotBlank(get(processor.getSequencingTechnology(), index))) {
                IlluminaFlowcell.FlowcellType sequencerModel =
                        findFlowcellType(get(processor.getSequencingTechnology(), index));
                if (sequencerModel == null) {
                    messages.addError(String.format(WRONG_TYPE, rowNumber, "Sequencing Technology",
                            SEQUENCER_MODELS + ", or blank"));
                } else {
                    sequencerModels.put(index, sequencerModel);
                }
            }

        }
        if (entityCount == 0) {
            messages.addWarning("Spreadsheet contains no data.");
        }
        if (!messages.hasErrors()) {
            persistExternalLibraries(processor, researchProjects, productOrders, molecularIndexingSchemes, labVessels,
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
            Map<Integer, ResearchProject> researchProjects, Map<Integer, ProductOrder> productOrders,
            Map<Integer, MolecularIndexingScheme> molecularIndexingSchemes,
            Map<Integer, LabVessel> labVessels, Map<Integer, MercurySample> mercurySamples,
            Map<Integer, SampleInstanceEntity> sampleInstanceEntities,
            Map<Integer, ReferenceSequence> referenceSequences,
            Map<Integer, IlluminaFlowcell.FlowcellType> sequencerModels, MessageCollection messages) {

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
                metadata.add(new Metadata(Metadata.Key.GENDER, get(processor.getSex(), index)));
                metadata.add(new Metadata(Metadata.Key.STRAIN, get(processor.getStrain(), index)));
                String organism = get(processor.getOrganism(), index);
                if (isNotBlank(organism)) {
                    metadata.add(new Metadata(Metadata.Key.ORGANISM, organism));
                }
                metadata.add(new Metadata(Metadata.Key.SPECIES, processor.getSpecies()));
                metadata.add(new Metadata(Metadata.Key.MATERIAL_TYPE, MaterialType.DNA.getDisplayName()));
                mercurySample.addMetadata(metadata);
                newObjects.add(mercurySample);
            }
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
            sampleInstanceEntity.setResearchProject(researchProjects.get(index));
            sampleInstanceEntity.setProductOrder(productOrders.get(index));
            sampleInstanceEntity.setLabVessel(labVessel);
            sampleInstanceEntity.setMercurySample(mercurySample);
            sampleInstanceEntity.setSequencerModel(sequencerModels.get(index));
        }
        sampleInstanceEntityDao.persistAll(newObjects);

        messages.addInfo("Spreadsheet with " + numberOfEntities + " rows successfully uploaded.");
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
                        "defined in ResearchProject " + identifier));
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

    /** Returns true if the Pooled Tube spreadsheet defines any sample metadata for the given sample name. */
    private boolean dataIsPresent(VesselPooledTubesProcessor processor, String sampleName) {
        for (int rowIndex = 0; rowIndex < processor.getBarcodes().size(); ++rowIndex) {
            if ((processor.getBroadSampleId().get(rowIndex).equals(sampleName) &&
                    (isNotBlank(processor.getGender().get(rowIndex)) ||
                            isNotBlank(get(processor.getSpecies(), rowIndex)) ||
                            isNotBlank(get(processor.getLsid(), rowIndex))))
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
            messages.addError(String.format(CONFLICT, rowNumber, column, actual, expected, "", ""));
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
            messages.addError(String.format(CONFLICT, rowNumber, column, actual, expected, "", ""));
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

            gender = get(processor.getGender(), rowIndex);
            species = get(processor.getSpecies(), rowIndex);
            rootSampleName = (dtoType == DtoType.SAMPLE) ? processor.getRootSampleId().get(rowIndex) : null;
            lsid = (dtoType == DtoType.SAMPLE || dtoType == DtoType.BOTH) ? processor.getLsid().get(rowIndex) : null;
        }

        public SampleDataDto(DtoType dtoType, VesselPooledTubesProcessor processor, int rowIndex,
                int rowOffset, MessageCollection messages) {

            this(dtoType, processor, rowIndex);

            final int rowNumber = rowIndex + rowOffset;

            // An empty string value indicates a missing value in the spreadsheet; null value indicates
            // the value does not apply to this dto type and should be skipped.
            if ("".equals(rootSampleName)) {
                messages.addError(String.format(MISSING, rowNumber,
                        VesselPooledTubesProcessor.Headers.ROOT_SAMPLE_ID.getText()));
            }
            if ("".equals(collaboratorSampleId)) {
                messages.addError(String.format(MISSING, rowNumber,
                        VesselPooledTubesProcessor.Headers.COLLABORATOR_SAMPLE_ID.getText()));
            }
            if ("".equals(collaboratorParticipantId)) {
                messages.addError(String.format(MISSING, rowNumber,
                        VesselPooledTubesProcessor.Headers.COLLABORATOR_PARTICIPANT_ID.getText()));
            }
            if ("".equals(broadParticipantId)) {
                messages.addError(String.format(MISSING, rowNumber,
                        VesselPooledTubesProcessor.Headers.BROAD_PARTICIPANT_ID.getText()));
            }
            if ("".equals(gender)) {
                messages.addError(String.format(MISSING, rowNumber,
                        VesselPooledTubesProcessor.Headers.GENDER.getText()));
            }
            if ("".equals(species)) {
                messages.addError(String.format(MISSING, rowNumber,
                        VesselPooledTubesProcessor.Headers.SPECIES.getText()));
            }
            if ("".equals(lsid)) {
                messages.addError(String.format(MISSING, rowNumber,
                        VesselPooledTubesProcessor.Headers.LSID.getText()));
            }
        }

        String getSampleName() {
            return sampleName;
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
