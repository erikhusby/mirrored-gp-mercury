package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import com.google.common.collect.Sets;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.bsp.GetSampleDetails;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabMetricRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.control.sample.SampleVesselProcessor;
import org.broadinstitute.gpinformatics.mercury.control.vessel.CaliperPlateProcessor;
import org.broadinstitute.gpinformatics.mercury.control.vessel.LabVesselFactory;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanPlateProcessor;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanRowParser;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricDecision;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.annotation.Nonnull;
import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.validation.constraints.Null;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Stateful
@RequestScoped
public class VesselEjb {

    @Inject
    private BarcodedTubeDao barcodedTubeDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private LabVesselFactory labVesselFactory;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private StaticPlateDao staticPlateDao;

    @Inject
    private LabMetricRunDao labMetricRunDao;

    @Resource
    private EJBContext ejbContext;

    /**
     * Registers {@code BarcodedTube}s for all specified {@param tubeBarcodes} as well as
     * registering any required {@code MercurySample}s.  No new {@code MercurySample}s will be
     * created for sample names that already have {@code MercurySample} instances, but both
     * preexisting and newly created {@code MercurySample}s will be associated with the created
     * {@code BarcodedTube}s.  The sample names are gotten from the
     * {@code GetSampleDetails.SampleInfo} values of the {@param sampleInfoMap}.
     * @param tubeType either BarcodedTube.BarcodedTubeType.name or null (defaults to Matrix tube).
     */
    public void registerSamplesAndTubes(@Nonnull Collection<String> tubeBarcodes,
                                        @Null String tubeType,
                                        @Nonnull Map<String, GetSampleDetails.SampleInfo> sampleInfoMap) {

        // Determine which barcodes are already known to Mercury.
        List<BarcodedTube> previouslyRegisteredTubes = barcodedTubeDao.findListByBarcodes(tubeBarcodes);
        Set<String> previouslyRegisteredTubeBarcodes = new HashSet<>();
        for (BarcodedTube tube : previouslyRegisteredTubes) {
            previouslyRegisteredTubeBarcodes.add(tube.getLabel());
        }

        // The Set of tube barcodes that are known to BSP but not Mercury.
        Set<String> tubeBarcodesToRegister =
                Sets.difference(new HashSet<>(tubeBarcodes), previouslyRegisteredTubeBarcodes);

        // The Set of all sample names for the tubes to be registered.  This is needed
        // to associate MercurySamples to LabVessels irrespective of whether the MercurySamples
        // were created by this code.
        Set<String> sampleNamesToAssociate = new HashSet<>();
        for (String tubeBarcode : tubeBarcodesToRegister) {
            sampleNamesToAssociate.add(sampleInfoMap.get(tubeBarcode).getSampleId());
        }

        // The Set of MercurySample names that need to be registered.  These are all the sample
        // names for the tubes to be registered minus the sample names already known to Mercury.
        Set<String> previouslyRegisteredSampleNames = new HashSet<>();
        for (MercurySample mercurySample : mercurySampleDao.findBySampleKeys(sampleNamesToAssociate)) {
            previouslyRegisteredSampleNames.add(mercurySample.getSampleKey());
        }

        Set<String> sampleNamesToRegister =
                Sets.difference(sampleNamesToAssociate, previouslyRegisteredSampleNames);

        for (String sampleName : sampleNamesToRegister) {
            mercurySampleDao.persist(new MercurySample(sampleName, MercurySample.MetadataSource.BSP));
        }
        // Explicit flush required as Mercury runs in FlushModeType.COMMIT and we want to see the results of any
        // persists done in the loop above reflected in the query below.
        mercurySampleDao.flush();

        // Map all MercurySamples to be associated by sample name.
        Map<String, MercurySample> sampleNameToMercurySample = new HashMap<>();
        for (MercurySample mercurySample : mercurySampleDao.findBySampleKeys(sampleNamesToAssociate)) {
            sampleNameToMercurySample.put(mercurySample.getSampleKey(), mercurySample);
        }

        // Create BarcodedTubes for all tube barcodes to be registered and associate them with
        // the appropriate MercurySamples.
        BarcodedTube.BarcodedTubeType barcodedTubeType = null;
        if (StringUtils.isNotBlank(tubeType)) {
            barcodedTubeType = BarcodedTube.BarcodedTubeType.getByAutomationName(tubeType);
            if (barcodedTubeType == null) {
                throw new RuntimeException("No support for tube type '" + tubeType + "'.");
            }
        }
        for (String tubeBarcode : tubeBarcodesToRegister) {
            BarcodedTube tube = barcodedTubeType != null ?
                    new BarcodedTube(tubeBarcode, barcodedTubeType) : new BarcodedTube(tubeBarcode);
            String sampleId = sampleInfoMap.get(tube.getLabel()).getSampleId();
            tube.addSample(sampleNameToMercurySample.get(sampleId));
            mercurySampleDao.persist(tube);
        }

        mercurySampleDao.flush();
    }

    /**
     * Create LabVessels and MercurySamples from a spreadsheet (from BSP).
     */
    public List<LabVessel> createSampleVessels(InputStream samplesSpreadsheetStream, String loginUserName,
            MessageCollection messageCollection, SampleVesselProcessor sampleVesselProcessor)
            throws InvalidFormatException, IOException, ValidationException {
        messageCollection.addErrors(PoiSpreadsheetParser.processSingleWorksheet(samplesSpreadsheetStream,
                sampleVesselProcessor));

        List<LabVessel> labVessels = null;
        if (!messageCollection.hasErrors()) {
            Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(new ArrayList<>(
                    sampleVesselProcessor.getTubeBarcodes()));
            for (LabVessel labVessel : mapBarcodeToVessel.values()) {
                if (labVessel != null) {
                    messageCollection.addError("Tube " + labVessel.getLabel() + " is already in the database.");
                }
            }

            List<MercurySample> mercurySamples = mercurySampleDao.findBySampleKeys(sampleVesselProcessor.getSampleIds());
            for (MercurySample mercurySample : mercurySamples) {
                messageCollection.addError("Sample " + mercurySample.getSampleKey() + " is already in the database.");
            }

            if (!messageCollection.hasErrors()) {
                labVessels = labVesselFactory.buildLabVessels(sampleVesselProcessor.getParentVesselBeans(),
                        loginUserName, new Date(), null, MercurySample.MetadataSource.MERCURY);
                labVesselDao.persistAll(labVessels);
            }
        }
        return labVessels;
    }

    /**
     * Create a LabMetricRun from a Varioskan spreadsheet.  This method assumes that a rack of tubes was
     * transferred into one or more plates, and the plates are in the spreadsheet.
     *
     * @param acceptRePico indicates when previous quants should be ignored and new quants processed.
     * @return Pair of LabMetricRun and the label of the tubeFormation that sourced the plates listed in the upload,
     *         or null in case of error.
     * In case of a duplicate upload the returned LabMetricRun is the previously uploaded one.
     */
    public Pair<LabMetricRun, String> createVarioskanRun(InputStream varioskanSpreadsheet,
                                                         LabMetric.MetricType metricType, Long decidingUser,
                                                         MessageCollection messageCollection, boolean acceptRePico) {

        try {
            Workbook workbook = WorkbookFactory.create(varioskanSpreadsheet);
            VarioskanRowParser varioskanRowParser = new VarioskanRowParser(workbook);
            Map<VarioskanRowParser.NameValue, String> mapNameValueToValue = varioskanRowParser.getValues();

            VarioskanPlateProcessor varioskanPlateProcessor = new VarioskanPlateProcessor(
                    VarioskanRowParser.QUANTITATIVE_CURVE_FIT1_TAB);
            PoiSpreadsheetParser parser = new PoiSpreadsheetParser(Collections.<String, TableProcessor>emptyMap());
            parser.processRows(workbook.getSheet(VarioskanRowParser.QUANTITATIVE_CURVE_FIT1_TAB),
                    varioskanPlateProcessor);

            // Fetch the plates
            Map<String, StaticPlate> mapBarcodeToPlate = new HashMap<>();
            Pair<LabMetricRun, String> pair = null;

            if (varioskanPlateProcessor.getPlateWellResults().isEmpty()) {
                messageCollection.addError("Didn't find any plate barcodes in the spreadsheet.");
            } else {
                for (VarioskanPlateProcessor.PlateWellResult plateWellResult :
                        varioskanPlateProcessor.getPlateWellResults()) {
                    StaticPlate staticPlate = mapBarcodeToPlate.get(plateWellResult.getPlateBarcode());
                    if (staticPlate == null) {
                        staticPlate = staticPlateDao.findByBarcode(plateWellResult.getPlateBarcode());
                        if (staticPlate == null) {
                            messageCollection.addError("Failed to find plate " + plateWellResult.getPlateBarcode());
                        } else {
                            mapBarcodeToPlate.put(plateWellResult.getPlateBarcode(), staticPlate);
                        }
                    }
                }
                TubeFormation tubeFormation = getFirstTubeFormationFromPlates(mapBarcodeToPlate.values());
                if (tubeFormation == null) {
                    messageCollection.addError("Cannot find the tube formation upstream of plates " +
                                               StringUtils.join(mapBarcodeToPlate.values(), ", "));
                } else {
                    // Run name must be unique.
                    String runName = mapNameValueToValue.get(VarioskanRowParser.NameValue.RUN_NAME);
                    LabMetricRun labMetricRun = labMetricRunDao.findByName(runName);
                    if (labMetricRun != null) {
                        messageCollection.addError("This run has been uploaded previously.");
                        pair = Pair.of(labMetricRun, tubeFormation.getLabel());
                    } else {
                        // Run date must be unique so that a search can reveal the latest quant.
                        List<LabMetricRun> sameDateRuns = labMetricRunDao.findList(LabMetricRun.class,
                                LabMetricRun_.runDate, parseRunDate(mapNameValueToValue));
                        if (CollectionUtils.isNotEmpty(sameDateRuns)) {
                            messageCollection.addError("A previous upload has the same Run Started timestamp.");
                            pair = Pair.of(sameDateRuns.iterator().next(), tubeFormation.getLabel());
                        } else {
                            // It's an error if previous quants exist, unless told to accept the rePico.
                            List<String> previousQuantedTubes = null;
                            if (!acceptRePico) {
                                previousQuantedTubes = new ArrayList<>();
                                for (BarcodedTube tube : tubeFormation.getContainerRole().getContainedVessels()) {
                                    if (tube.findMostRecentLabMetric(metricType) != null) {
                                        previousQuantedTubes.add(tube.getLabel());
                                    }
                                }
                            }
                            if (!acceptRePico && CollectionUtils.isNotEmpty(previousQuantedTubes)) {
                                messageCollection.addError(metricType.getDisplayName() +
                                                           " was previously done on tubes " +
                                                           StringUtils.join(previousQuantedTubes, ", "));
                            } else {
                                pair = createVarioskanRunDaoFree(mapNameValueToValue, metricType,
                                        varioskanPlateProcessor, mapBarcodeToPlate, decidingUser, messageCollection);
                                if (messageCollection.hasErrors()) {
                                    ejbContext.setRollbackOnly();
                                } else {
                                    labMetricRunDao.persist(pair.getLeft());
                                }
                            }
                        }
                    }
                }
            }
            return pair;
        } catch (IOException | InvalidFormatException | ValidationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a LabMetricRun from a Varioskan spreadsheet.
     * @return Pair of LabMetricRun and the label of the tubeFormation that sourced the plates listed in the upload.
     */
    @DaoFree
    public Pair<LabMetricRun, String> createVarioskanRunDaoFree(
            Map<VarioskanRowParser.NameValue, String> mapNameValueToValue, LabMetric.MetricType metricType,
            VarioskanPlateProcessor varioskanPlateProcessor, Map<String, StaticPlate> mapBarcodeToPlate,
            Long decidingUser, MessageCollection messageCollection) {

        Map<LabVessel, List<BigDecimal>> mapTubeToListValues = new HashMap<>();
        Map<LabVessel, VesselPosition> mapTubeToPosition = new HashMap<>();

        // Create the run
        Date runStarted = parseRunDate(mapNameValueToValue);
        LabMetricRun labMetricRun = new LabMetricRun(mapNameValueToValue.get(VarioskanRowParser.NameValue.RUN_NAME),
                runStarted, metricType);
        String tubeFormationLabel = null;

        String r2 = mapNameValueToValue.get(VarioskanRowParser.NameValue.CORRELATION_COEFFICIENT_R2);
        labMetricRun.getMetadata().add(new Metadata(Metadata.Key.CORRELATION_COEFFICIENT_R2, r2));
        boolean runFailed = false;
        if (new BigDecimal(r2).compareTo(new BigDecimal("0.97")) == -1) {
            runFailed = true;
        }

        labMetricRun.getMetadata().add(new Metadata(Metadata.Key.INSTRUMENT_NAME,
                mapNameValueToValue.get(VarioskanRowParser.NameValue.INSTRUMENT_NAME)));
        labMetricRun.getMetadata().add(new Metadata(Metadata.Key.INSTRUMENT_SERIAL_NUMBER,
                mapNameValueToValue.get(VarioskanRowParser.NameValue.INSTRUMENT_SERIAL_NUMBER)));

        // Store raw values against plate wells
        for (VarioskanPlateProcessor.PlateWellResult plateWellResult : varioskanPlateProcessor.getPlateWellResults()) {
            StaticPlate staticPlate = mapBarcodeToPlate.get(plateWellResult.getPlateBarcode());
            LabMetric labMetric = new LabMetric(plateWellResult.getResult(), metricType, LabMetric.LabUnit.NG_PER_UL,
                    plateWellResult.getVesselPosition().name(), runStarted);
            labMetricRun.addMetric(labMetric);
            PlateWell plateWell = staticPlate.getContainerRole().getVesselAtPosition(
                    plateWellResult.getVesselPosition());
            if (plateWell == null) {
                plateWell = new PlateWell(staticPlate, plateWellResult.getVesselPosition());
                staticPlate.getContainerRole().addContainedVessel(plateWell, plateWellResult.getVesselPosition());
            }
            plateWell.addMetric(labMetric);

            LabVessel sourceTube = null;
            for (SectionTransfer sectionTransfer : staticPlate.getContainerRole().getSectionTransfersTo()) {
                int sectionIndex = sectionTransfer.getTargetSection().getWells().indexOf(
                        plateWellResult.getVesselPosition());
                if (sectionIndex > -1) {
                    VesselPosition sourcePosition = sectionTransfer.getSourceSection().getWells().get(sectionIndex);
                    VesselContainer vesselContainer = sectionTransfer.getSourceVesselContainer();
                    if (tubeFormationLabel == null) {
                        tubeFormationLabel = vesselContainer.getEmbedder().getLabel();
                    }
                    sourceTube = vesselContainer.getVesselAtPosition(sourcePosition);
                    if (sourceTube != null) {
                        mapTubeToPosition.put(sourceTube, sourcePosition);
                        List<BigDecimal> valuesList = mapTubeToListValues.get(sourceTube);
                        if (valuesList == null) {
                            valuesList = new ArrayList<>();
                            mapTubeToListValues.put(sourceTube, valuesList);
                        }
                        valuesList.add(plateWellResult.getResult());
                    }
                }
            }
            if (sourceTube == null) {
                messageCollection.addError("Failed to find source tube for " + plateWellResult.getPlateBarcode() +
                    " " + plateWellResult.getVesselPosition());
            }
        }

        // Store average values against source tubes
        for (Map.Entry<LabVessel, List<BigDecimal>> labVesselListEntry : mapTubeToListValues.entrySet()) {
            LabVessel tube = labVesselListEntry.getKey();
            List<BigDecimal> values = labVesselListEntry.getValue();
            BigDecimal average = new BigDecimal(0);
            for (BigDecimal value : values) {
                average = average.add(value);
            }
            average = MathUtils.scaleTwoDecimalPlaces(average.divide(new BigDecimal(values.size())));
            LabMetric labMetric = new LabMetric(average, metricType, LabMetric.LabUnit.NG_PER_UL,
                    mapTubeToPosition.get(tube).name(), runStarted);

            if (tube.getVolume() == null) {
                messageCollection.addError("No volume for tube " + tube.getLabel());
            } else {
                labMetric.getMetadataSet().add(new Metadata(Metadata.Key.TOTAL_NG,
                        labMetric.getValue().multiply(tube.getVolume())));
            }
            LabMetric.Decider decider = metricType.getDecider();
            LabMetricDecision.Decision decision = null;
            if (runFailed) {
                decision = LabMetricDecision.Decision.RUN_FAILED;
            } else if (decider != null) {
                decision = decider.makeDecision(tube, labMetric);
            }
            if (decision != null) {
                labMetric.setLabMetricDecision(
                        new LabMetricDecision(decision, new Date(), decidingUser, labMetric, null));
            }
            tube.addMetric(labMetric);
            labMetricRun.addMetric(labMetric);
        }

        return Pair.of(labMetricRun, tubeFormationLabel);
    }

    // Returns the quant spreadsheet's run started value as a Date.
    private Date parseRunDate(Map<VarioskanRowParser.NameValue, String> mapNameValueToValue) {
        SimpleDateFormat simpleDateFormat =
                new SimpleDateFormat(VarioskanRowParser.NameValue.RUN_STARTED.getDateFormat());
        try {
            return simpleDateFormat.parse(mapNameValueToValue.get(VarioskanRowParser.NameValue.RUN_STARTED));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    // Returns the immediately upstream tube formation for the given collection of Pico plates.
    // Assumes there will only be one upstream tube formation (since Next steps UI only can show one).
    private TubeFormation getFirstTubeFormationFromPlates(Collection<StaticPlate> plates) {
        for (StaticPlate plate : plates) {
            for (SectionTransfer sectionTransfer : plate.getContainerRole().getSectionTransfersTo()) {
                return (TubeFormation)sectionTransfer.getSourceVesselContainer().getEmbedder();
            }
        }
        return null;
    }


    /**
     * Create a LabMetricRun from a Caliper csv.  This method assumes that a rack of tubes was
     * transferred into one plate, and the plates are in the csv.
     *
     * @param acceptReCaliper indicates when previous quants should be ignored and new quants processed.
     * @return Pair of LabMetricRun and the label of the tubeFormation that sourced the plates listed in the upload,
     *         or null in case of error.
     * In case of a duplicate upload the returned LabMetricRun is the previously uploaded one.
     */
    public Pair<LabMetricRun, String> createRNACaliperRun(InputStream caliperCsvStream,
                                                          LabMetric.MetricType metricType,
                                                          long decidingUser, MessageCollection messageCollection,
                                                          boolean acceptReCaliper) {
        try {
            CaliperPlateProcessor caliperPlateProcessor = new CaliperPlateProcessor();
            CaliperPlateProcessor.CaliperRun caliperRun = caliperPlateProcessor.parse(caliperCsvStream);

            // Fetch the plate
            Map<String, StaticPlate> mapBarcodeToPlate = new HashMap<>();
            Pair<LabMetricRun, String> pair = null;

            if (caliperRun.getPlateWellResultMarkers().isEmpty()) {
                messageCollection.addError("Didn't find any plate barcodes in the spreadsheet.");
            } else {
                for (CaliperPlateProcessor.PlateWellResultMarker plateWellResult :
                        caliperRun.getPlateWellResultMarkers()) {
                    StaticPlate staticPlate = mapBarcodeToPlate.get(plateWellResult.getPlateBarcode());
                    if (staticPlate == null) {
                        staticPlate = staticPlateDao.findByBarcode(plateWellResult.getPlateBarcode());
                        if (staticPlate == null) {
                            messageCollection.addError("Failed to find plate " + plateWellResult.getPlateBarcode());
                        } else {
                            mapBarcodeToPlate.put(plateWellResult.getPlateBarcode(), staticPlate);
                        }
                    }
                }
                TubeFormation tubeFormation = getFirstTubeFormationFromPlates(mapBarcodeToPlate.values());
                if (tubeFormation == null) {
                    messageCollection.addError("Cannot find the tube formation upstream of plates " +
                                               StringUtils.join(mapBarcodeToPlate.values(), ", "));
                } else {
                    // Run name must be unique.
                    String runName = caliperRun.getRunName();
                    LabMetricRun labMetricRun = labMetricRunDao.findByName(runName);
                    if (labMetricRun != null) {
                        messageCollection.addError("This run has been uploaded previously.");
                        pair = Pair.of(labMetricRun, tubeFormation.getLabel());
                    } else {
                        // Run date must be unique so that a search can reveal the latest quant.
                        List<LabMetricRun> sameDateRuns = labMetricRunDao.findList(LabMetricRun.class,
                                LabMetricRun_.runDate, caliperRun.getRunDate());
                        if (CollectionUtils.isNotEmpty(sameDateRuns)) {
                            messageCollection.addError("A previous upload has the same Run Started timestamp.");
                            pair = Pair.of(sameDateRuns.iterator().next(), tubeFormation.getLabel());
                        } else {
                            // It's an error if previous quants exist, unless told to accept the reCaliper.
                            List<String> previousQuantedTubes = null;
                            if (!acceptReCaliper) {
                                previousQuantedTubes = new ArrayList<>();
                                for (BarcodedTube tube : tubeFormation.getContainerRole().getContainedVessels()) {
                                    if (tube.findMostRecentLabMetric(metricType) != null) {
                                        previousQuantedTubes.add(tube.getLabel());
                                    }
                                }
                            }
                            if (!acceptReCaliper && CollectionUtils.isNotEmpty(previousQuantedTubes)) {
                                messageCollection.addError(metricType.getDisplayName() +
                                                           " was previously done on tubes " +
                                                           StringUtils.join(previousQuantedTubes, ", "));
                            } else {
                                pair = createRNACaliperRunDaoFree(metricType, caliperRun, mapBarcodeToPlate,
                                        decidingUser, messageCollection);
                                if (messageCollection.hasErrors()) {
                                    ejbContext.setRollbackOnly();
                                } else {
                                    labMetricRunDao.persist(pair.getLeft());
                                }
                            }
                        }
                    }
                }
            }
            return pair;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a LabMetricRun from a RNA Caliper csv.
     * @return Pair of LabMetricRun and the label of the tubeFormation that sourced the plates listed in the csv.
     */
    @DaoFree
    public Pair<LabMetricRun, String> createRNACaliperRunDaoFree(LabMetric.MetricType metricType,
                                                                 CaliperPlateProcessor.CaliperRun caliperRun,
                                             Map<String, StaticPlate> mapBarcodeToPlate, long decidingUser,
                                             MessageCollection messageCollection) {
        LabMetricRun labMetricRun = new LabMetricRun(caliperRun.getRunName(),
                caliperRun.getRunDate(), metricType);
        String tubeFormationLabel = null;

        labMetricRun.getMetadata().add(new Metadata(Metadata.Key.INSTRUMENT_NAME, caliperRun.getInstrumentName()));

        // Store raw values against plate wells
        for (CaliperPlateProcessor.PlateWellResultMarker plateWellResult : caliperRun.getPlateWellResultMarkers()) {
            StaticPlate staticPlate = mapBarcodeToPlate.get(plateWellResult.getPlateBarcode());
            LabMetric labMetric = new LabMetric(plateWellResult.getResult(), metricType, LabMetric.LabUnit.RQS,
                    plateWellResult.getVesselPosition().name(), caliperRun.getRunDate());
            labMetricRun.addMetric(labMetric);
            PlateWell plateWell = staticPlate.getContainerRole().getVesselAtPosition(
                    plateWellResult.getVesselPosition());
            if (plateWell == null) {
                plateWell = new PlateWell(staticPlate, plateWellResult.getVesselPosition());
                staticPlate.getContainerRole().addContainedVessel(plateWell, plateWellResult.getVesselPosition());
            }
            plateWell.addMetric(labMetric);

            LabVessel sourceTube = null;
            for (SectionTransfer sectionTransfer : staticPlate.getContainerRole().getSectionTransfersTo()) {
                int sectionIndex = sectionTransfer.getTargetSection().getWells().indexOf(
                        plateWellResult.getVesselPosition());
                if (sectionIndex > -1) {
                    VesselPosition sourcePosition = sectionTransfer.getSourceSection().getWells().get(sectionIndex);
                    VesselContainer vesselContainer = sectionTransfer.getSourceVesselContainer();
                    if (tubeFormationLabel == null) {
                        tubeFormationLabel = vesselContainer.getEmbedder().getLabel();
                    }
                    sourceTube = vesselContainer.getVesselAtPosition(sourcePosition);
                    if (sourceTube != null) {
                        LabMetric sourceVesselLabMetric = new LabMetric(plateWellResult.getResult(),
                                metricType, LabMetric.LabUnit.RQS,
                                sourcePosition.name(), caliperRun.getRunDate());
                        LabMetricDecision.Decision decision;
                        String decisionNote = null;
                        if(plateWellResult.getLowerMarkerTime() < 28 || plateWellResult.getLowerMarkerTime() > 33) {
                            decision = LabMetricDecision.Decision.REPEAT;
                            decisionNote = "Lower Marker Time not in accepted 28-33 range.";
                        } else if(plateWellResult.getDv200TotalArea() <= 0 || plateWellResult.getDv200TotalArea() >= 1) {
                            decision = LabMetricDecision.Decision.REPEAT;
                            decisionNote = "DV200 not in accepted 0-1 range.";
                        } else {
                            decision = LabMetricDecision.Decision.PASS;
                        }
                        sourceVesselLabMetric.setLabMetricDecision(
                                new LabMetricDecision(decision, new Date(), decidingUser, labMetric, decisionNote));
                        sourceTube.addMetric(sourceVesselLabMetric);
                        labMetricRun.addMetric(sourceVesselLabMetric);
                    }
                }
            }
            if (sourceTube == null) {
                messageCollection.addError("Failed to find source tube for " + plateWellResult.getPlateBarcode() +
                                           " " + plateWellResult.getVesselPosition());
            }
        }

        return Pair.of(labMetricRun, tubeFormationLabel);
    }
}
