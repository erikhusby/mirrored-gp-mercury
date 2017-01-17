package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import com.google.common.collect.Sets;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.bsp.GetSampleDetails;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.generated.LibraryBeansType;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.generated.LibraryQuantBeanType;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.generated.LibraryQuantRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.generated.QpcrRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.sample.QuantificationEJB;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabMetricRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.BSPRestSender;
import org.broadinstitute.gpinformatics.mercury.control.sample.SampleVesselProcessor;
import org.broadinstitute.gpinformatics.mercury.control.vessel.CaliperPlateProcessor;
import org.broadinstitute.gpinformatics.mercury.control.vessel.LabVesselFactory;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanPlateProcessor;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanRowParser;
import org.broadinstitute.gpinformatics.mercury.control.vessel.WallacPlateProcessor;
import org.broadinstitute.gpinformatics.mercury.control.vessel.WallacRowParser;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricDecision;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

import static org.broadinstitute.gpinformatics.mercury.entity.OrmUtil.proxySafeCast;

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

    @Inject
    private QuantificationEJB quantificationEjb;

    @Resource
    private EJBContext ejbContext;

    @Inject
    private BSPRestSender bspRestSender;

    /**
     * Registers {@code BarcodedTube}s for all specified {@param tubeBarcodes} as well as
     * registering any required {@code MercurySample}s.  No new {@code MercurySample}s will be
     * created for sample names that already have {@code MercurySample} instances, but both
     * preexisting and newly created {@code MercurySample}s will be associated with the created
     * {@code BarcodedTube}s.  The sample names are gotten from the
     * {@code GetSampleDetails.SampleInfo} values of the {@param sampleInfoMap}.
     *
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
                                               MessageCollection messageCollection,
                                               SampleVesselProcessor sampleVesselProcessor)
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

            List<MercurySample> mercurySamples =
                    mercurySampleDao.findBySampleKeys(sampleVesselProcessor.getSampleIds());
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
     *
     * @return Triple of LabMetricRun, the label of the tubeFormation that sourced the plates listed in the upload,
     * and set of the microfluor plates. Returns null in case of error.
     * In case of a duplicate upload the returned LabMetricRun is the previously uploaded one.
     */
    public Triple<LabMetricRun, String, Set<StaticPlate>> createVarioskanRun(InputStream varioskanSpreadsheet,
            LabMetric.MetricType metricType, Long decidingUser, MessageCollection messageCollection,
            boolean acceptRePico) {
        try {
            Workbook workbook = WorkbookFactory.create(varioskanSpreadsheet);
            VarioskanRowParser varioskanRowParser = new VarioskanRowParser(workbook);
            Map<VarioskanRowParser.NameValue, String> mapNameValueToValue = varioskanRowParser.getValues();

            VarioskanPlateProcessor varioskanPlateProcessor = new VarioskanPlateProcessor(
                    VarioskanRowParser.QUANTITATIVE_CURVE_FIT1_TAB, metricType);
            PoiSpreadsheetParser parser = new PoiSpreadsheetParser(Collections.<String, TableProcessor>emptyMap());
            parser.processRows(workbook.getSheet(VarioskanRowParser.QUANTITATIVE_CURVE_FIT1_TAB),
                    varioskanPlateProcessor);

            // Fetch the plates
            Map<String, StaticPlate> mapBarcodeToPlate = new HashMap<>();
            Triple<LabMetricRun, String, Set<StaticPlate>> triple = null;

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
                Set<StaticPlate> microfluorPlates = new HashSet<>(mapBarcodeToPlate.values());
                TubeFormation tubeFormation = getNearestTubeFormation(mapBarcodeToPlate.values());
                if (tubeFormation == null) {
                    throw new RuntimeException("Cannot find the tube formation upstream of plate " +
                                               mapBarcodeToPlate.values().iterator().next().getLabel());
                } else {
                    // Run name must be unique.
                    String runName = mapNameValueToValue.get(VarioskanRowParser.NameValue.RUN_NAME);
                    LabMetricRun labMetricRun = labMetricRunDao.findByName(runName);
                    if (labMetricRun != null) {
                        messageCollection.addError("This run has been uploaded previously.");
                        triple = Triple.of(labMetricRun, tubeFormation.getLabel(), microfluorPlates);
                    } else {
                        // Run date must be unique so that a search can reveal the latest quant.
                        List<LabMetricRun> sameDateRuns = labMetricRunDao.findSameDateRuns(
                                parseRunDate(mapNameValueToValue));
                        if (CollectionUtils.isNotEmpty(sameDateRuns)) {
                            messageCollection.addError("A previous upload has the same Run Started timestamp.");
                            triple = Triple.of(sameDateRuns.iterator().next(), tubeFormation.getLabel(),
                                    microfluorPlates);
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
                                LabMetricRun run = createVarioskanRunDaoFree(mapNameValueToValue, metricType,
                                        varioskanPlateProcessor, mapBarcodeToPlate, decidingUser, messageCollection);
                                triple = Triple.of(run, tubeFormation.getLabel(), microfluorPlates);
                                if (messageCollection.hasErrors()) {
                                    ejbContext.setRollbackOnly();
                                } else {
                                    labMetricRunDao.persist(run);
                                    quantificationEjb.updateRisk(triple.getLeft().getLabMetrics(), metricType,
                                            messageCollection);
                                }
                            }
                        }
                    }
                }
            }
            return triple;
        } catch (IOException | InvalidFormatException | ValidationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a LabMetricRun from a Varioskan spreadsheet.
     */
    @DaoFree
    public LabMetricRun createVarioskanRunDaoFree(
            Map<VarioskanRowParser.NameValue, String> mapNameValueToValue, LabMetric.MetricType metricType,
            VarioskanPlateProcessor varioskanPlateProcessor, Map<String, StaticPlate> mapBarcodeToPlate,
            Long decidingUser, MessageCollection messageCollection) {

        Map<LabVessel, List<BigDecimal>> mapTubeToListValues = new HashMap<>();
        Map<LabVessel, VesselPosition> mapTubeToPosition = new HashMap<>();

        // Create the run
        Date runStarted = parseRunDate(mapNameValueToValue);
        LabMetricRun labMetricRun = new LabMetricRun(mapNameValueToValue.get(VarioskanRowParser.NameValue.RUN_NAME),
                runStarted, metricType);

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

        // The source tube formation and tube position for each position in each microfluor plate.
        Map<StaticPlate, Map<VesselPosition, Pair<TubeFormation, VesselPosition>>> plateWellTubePosition =
                new HashMap<>();

        for (VarioskanPlateProcessor.PlateWellResult plateWellResult : varioskanPlateProcessor.getPlateWellResults()) {
            // Puts raw values in the lab metric run.
            LabMetric labMetric = new LabMetric(plateWellResult.getResult(), metricType, LabMetric.LabUnit.NG_PER_UL,
                    plateWellResult.getVesselPosition().name(), runStarted);
            labMetricRun.addMetric(labMetric);

            // Stores raw values for each plate well.
            StaticPlate staticPlate = mapBarcodeToPlate.get(plateWellResult.getPlateBarcode());
            PlateWell plateWell = staticPlate.getContainerRole().getVesselAtPosition(
                    plateWellResult.getVesselPosition());
            if (plateWell == null) {
                plateWell = new PlateWell(staticPlate, plateWellResult.getVesselPosition());
                staticPlate.getContainerRole().addContainedVessel(plateWell, plateWellResult.getVesselPosition());
            }
            plateWell.addMetric(labMetric);

            // Collects raw values for each source tube.
            Map<VesselPosition, Pair<TubeFormation, VesselPosition>> wellTubePosition =
                    plateWellTubePosition.get(staticPlate);
            if (wellTubePosition == null) {
                wellTubePosition = staticPlate.nearestFormationAndTubePositionByWell();
                plateWellTubePosition.put(staticPlate, wellTubePosition);
            }
            if (wellTubePosition.containsKey(plateWellResult.getVesselPosition())) {
                TubeFormation tubeFormation = wellTubePosition.get(plateWellResult.getVesselPosition()).getLeft();
                VesselPosition tubePosition = wellTubePosition.get(plateWellResult.getVesselPosition()).getRight();
                LabVessel sourceTube = tubeFormation.getContainerRole().getVesselAtPosition(tubePosition);

                if (sourceTube != null) {
                    mapTubeToPosition.put(sourceTube, tubePosition);
                    List<BigDecimal> valuesList = mapTubeToListValues.get(sourceTube);
                    if (valuesList == null) {
                        valuesList = new ArrayList<>();
                        mapTubeToListValues.put(sourceTube, valuesList);
                    }
                    valuesList.add(plateWellResult.getResult());
                } else {
                    messageCollection.addError("Failed to find source tube for " + plateWellResult.getPlateBarcode() +
                                               " " + plateWellResult.getVesselPosition());
                }
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
            average = MathUtils.scaleTwoDecimalPlaces(average.divide(new BigDecimal(values.size()),
                    RoundingMode.HALF_UP));
            LabMetric labMetric = new LabMetric(average, metricType, LabMetric.LabUnit.NG_PER_UL,
                    mapTubeToPosition.get(tube).name(), runStarted);

            if (tube.getVolume() == null) {
                messageCollection.addError("No volume for tube " + tube.getLabel());
            } else {
                labMetric.getMetadataSet().add(new Metadata(Metadata.Key.TOTAL_NG,
                        labMetric.getValue().multiply(tube.getVolume())));
            }
            LabMetric.Decider decider = metricType.getDecider();
            LabMetricDecision decision = null;
            if (runFailed) {
                decision = new LabMetricDecision(
                        LabMetricDecision.Decision.RUN_FAILED, new Date(), decidingUser, labMetric);
            } else if (decider != null) {
                decision = decider.makeDecision(tube, labMetric, decidingUser);
            }
            if (decision != null) {
                labMetric.setLabMetricDecision(decision);
            }
            tube.addMetric(labMetric);
            labMetricRun.addMetric(labMetric);
        }

        return labMetricRun;
    }

    /**
     * Create a LabMetricRun from a Wallac spreadsheet.  This method assumes that a rack of tubes was
     * transferred into one or more plates, and the plates are in the spreadsheet.
     *
     * @param acceptRePico indicates when previous quants should be ignored and new quants processed.
     *
     * @return Pair of LabMetricRun and the label of the tubeFormation that sourced the plates listed in the upload,
     * or null in case of error.
     * In case of a duplicate upload the returned LabMetricRun is the previously uploaded one.
     */
    public Pair<LabMetricRun, String> createWallacRun(InputStream wallacSpreadsheet, String runName,
                                                      LabMetric.MetricType metricType, Long decidingUser,
                                                      MessageCollection messageCollection, boolean acceptRePico) {
        try {
            Workbook workbook = WorkbookFactory.create(wallacSpreadsheet);
            WallacRowParser wallacRowParser = new WallacRowParser(workbook);
            Map<WallacRowParser.NameValue, String> mapNameValueToValue = wallacRowParser.getValues();

            String plateBarcode1 = mapNameValueToValue.get(WallacRowParser.NameValue.PLATE_BARCODE_1);
            String plateBarcode2 = mapNameValueToValue.get(WallacRowParser.NameValue.PLATE_BARCODE_2);
            WallacPlateProcessor wallacPlateProcessor =
                    new WallacPlateProcessor(WallacRowParser.RESULTS_TABLE_TAB, plateBarcode1, plateBarcode2);
            PoiSpreadsheetParser parser = new PoiSpreadsheetParser(Collections.<String, TableProcessor>emptyMap());
            parser.processRows(workbook.getSheet(WallacRowParser.RESULTS_TABLE_TAB), wallacPlateProcessor);

            // Fetch the plates
            Map<String, StaticPlate> mapBarcodeToPlate = new HashMap<>();
            Pair<LabMetricRun, String> pair = null;

            if (wallacPlateProcessor.getPlateWellResults().isEmpty()) {
                messageCollection.addError("Didn't find any plate barcodes in the spreadsheet.");
            } else {

                for (VarioskanPlateProcessor.PlateWellResult plateWellResult :
                        wallacPlateProcessor.getPlateWellResults()) {
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
                TubeFormation tubeFormation = getNearestTubeFormation(mapBarcodeToPlate.values());
                if (tubeFormation == null) {
                    throw new RuntimeException("Cannot find the tube formation upstream of plate " +
                                               mapBarcodeToPlate.values().iterator().next().getLabel());
                } else {
                    LabMetricRun labMetricRun = labMetricRunDao.findByName(runName);
                    if (labMetricRun != null) {
                        messageCollection.addError("This run has been uploaded previously.");
                        pair = Pair.of(labMetricRun, tubeFormation.getLabel());
                    } else {
                        // Run date must be unique so that a search can reveal the latest quant.
                        SimpleDateFormat simpleDateFormat =
                                new SimpleDateFormat(WallacRowParser.NameValue.RUN_STARTED.getDateFormat());
                        Date runDate = parseRunDate(simpleDateFormat, mapNameValueToValue.get(
                                WallacRowParser.NameValue.RUN_STARTED));
                        List<LabMetricRun> sameDateRuns = labMetricRunDao.findSameDateRuns(runDate);
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
                                pair = createWallacRunDaoFree(mapNameValueToValue, metricType, wallacPlateProcessor,
                                        mapBarcodeToPlate, decidingUser, messageCollection, runName);
                                if (messageCollection.hasErrors()) {
                                    ejbContext.setRollbackOnly();
                                } else {
                                    labMetricRunDao.persist(pair.getLeft());
                                    quantificationEjb.updateRisk(pair.getLeft().getLabMetrics(), metricType,
                                            messageCollection);
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

    private Pair<LabMetricRun, String> createWallacRunDaoFree(
            Map<WallacRowParser.NameValue, String> mapNameValueToValue,
            LabMetric.MetricType metricType, WallacPlateProcessor wallacPlateProcessor,
            Map<String, StaticPlate> mapBarcodeToPlate, Long decidingUser,
            MessageCollection messageCollection, String runName) {
        SimpleDateFormat simpleDateFormat =
                new SimpleDateFormat(WallacRowParser.NameValue.RUN_STARTED.getDateFormat());
        Date runStarted = parseRunDate(
                simpleDateFormat, mapNameValueToValue.get(WallacRowParser.NameValue.RUN_STARTED));
        LabMetricRun labMetricRun = new LabMetricRun(runName, runStarted, metricType);

        labMetricRun.getMetadata().add(new Metadata(Metadata.Key.INSTRUMENT_NAME,
                mapNameValueToValue.get(WallacRowParser.NameValue.INSTRUMENT_NAME)));

        // Store raw values against plate wells
        List<VarioskanPlateProcessor.PlateWellResult> plateWellResults = wallacPlateProcessor.getPlateWellResults();
        String tubeFormationLabel = addPlateWellResults(labMetricRun, mapBarcodeToPlate, plateWellResults, false,
                runStarted, metricType, decidingUser, messageCollection);

        return Pair.of(labMetricRun, tubeFormationLabel);
    }

    private String addPlateWellResults(LabMetricRun labMetricRun, Map<String, StaticPlate> mapBarcodeToPlate,
                                       List<VarioskanPlateProcessor.PlateWellResult> plateWellResults,
                                       boolean runFailed,
                                       Date runStarted, LabMetric.MetricType metricType, long decidingUser,
                                       MessageCollection messageCollection) {
        Map<LabVessel, List<BigDecimal>> mapTubeToListValues = new HashMap<>();
        Map<LabVessel, VesselPosition> mapTubeToPosition = new HashMap<>();
        String tubeFormationLabel = null;
        for (VarioskanPlateProcessor.PlateWellResult plateWellResult : plateWellResults) {
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
            boolean inSection = false;
            for (SectionTransfer sectionTransfer : staticPlate.getContainerRole().getSectionTransfersTo()) {
                int sectionIndex = sectionTransfer.getTargetSection().getWells().indexOf(
                        plateWellResult.getVesselPosition());
                if (sectionIndex > -1) {
                    inSection = true;
                    VesselPosition sourcePosition = sectionTransfer.getSourceSection().getWells().get(sectionIndex);
                    VesselContainer vesselContainer = sectionTransfer.getSourceVesselContainer();
                    if (tubeFormationLabel == null &&
                        vesselContainer.getEmbedder().getType() == LabVessel.ContainerType.TUBE_FORMATION) {
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
                // RIBO includes the curve samples in the destination plate, but they are not in the sections involved
                // in the transfers from the source tubes, so they should be ignored.
                if (metricType == LabMetric.MetricType.PLATING_RIBO && !inSection) {
                    continue;
                }
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
            average = MathUtils.scaleTwoDecimalPlaces(average.divide(new BigDecimal(values.size()),
                    RoundingMode.HALF_UP));
            LabMetric labMetric = new LabMetric(average, metricType, LabMetric.LabUnit.NG_PER_UL,
                    mapTubeToPosition.get(tube).name(), runStarted);

            if (tube.getVolume() == null) {
                messageCollection.addError("No volume for tube " + tube.getLabel());
            } else {
                labMetric.getMetadataSet().add(new Metadata(Metadata.Key.TOTAL_NG,
                        labMetric.getValue().multiply(tube.getVolume())));
            }
            LabMetric.Decider decider = metricType.getDecider();
            LabMetricDecision decision = null;
            if (runFailed) {
                decision = new LabMetricDecision(
                        LabMetricDecision.Decision.RUN_FAILED, new Date(), decidingUser, labMetric);
            } else if (decider != null) {
                decision = decider.makeDecision(tube, labMetric, decidingUser);
            }
            if (decision != null) {
                labMetric.setLabMetricDecision(decision);
            }
            tube.addMetric(labMetric);
            labMetricRun.addMetric(labMetric);
        }

        return tubeFormationLabel;
    }

    // Returns the quant spreadsheet's run started value as a Date.
    private Date parseRunDate(Map<VarioskanRowParser.NameValue, String> mapNameValueToValue) {
        SimpleDateFormat simpleDateFormat =
                new SimpleDateFormat(VarioskanRowParser.NameValue.RUN_STARTED.getDateFormat());
        return parseRunDate(simpleDateFormat, mapNameValueToValue.get(VarioskanRowParser.NameValue.RUN_STARTED));
    }

    private Date parseRunDate(SimpleDateFormat simpleDateFormat, String runDate) {
        try {
            return simpleDateFormat.parse(runDate);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    // Returns the nearest upstream tube formation for the given collection of Pico plates. Expects
    // only section transfers (possibly more than one) between the tube formation and the plate.
    private TubeFormation getNearestTubeFormation(Collection<StaticPlate> plates) {
        Set<LabVessel> sources = new HashSet<>();
        for (StaticPlate plate : plates) {
            for (SectionTransfer sectionTransfer : plate.getContainerRole().getSectionTransfersTo()) {
                VesselContainer<?> vesselContainer = sectionTransfer.getSourceVesselContainer();
                sources.add(vesselContainer.getEmbedder());
            }
        }
        if (sources.isEmpty()) {
            return null;
        }
        TubeFormation tubeFormation = null;
        Set<StaticPlate> sourcePlates = new HashSet<>();
        for (LabVessel source : sources) {
            if (source.getType() == LabVessel.ContainerType.TUBE_FORMATION) {
                if (tubeFormation == null) {
                    tubeFormation = OrmUtil.proxySafeCast(source, TubeFormation.class);
                } else if (tubeFormation != OrmUtil.proxySafeCast(source, TubeFormation.class)) {
                    throw new RuntimeException("Found multiple tube formations " + tubeFormation.getLabel() +
                                               ", " + source.getLabel());
                }
            } else if (source.getType() == LabVessel.ContainerType.STATIC_PLATE) {
                sourcePlates.add(proxySafeCast(source, StaticPlate.class));
            }
        }
        return (tubeFormation != null) ? tubeFormation : getNearestTubeFormation(sourcePlates);
    }

    /**
     * Create a LabMetricRun from a Caliper csv.  This method assumes that a rack of tubes was
     * transferred into one plate, and the plates are in the csv.
     *
     * @param acceptReCaliper indicates when previous quants should be ignored and new quants processed.
     *
     * @return Pair of LabMetricRun and the label of the tubeFormation that sourced the plates listed in the upload,
     * or null in case of error.
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
                TubeFormation tubeFormation = getNearestTubeFormation(mapBarcodeToPlate.values());
                if (tubeFormation == null) {
                    throw new RuntimeException("Cannot find the tube formation upstream of plate " +
                                               mapBarcodeToPlate.values().iterator().next().getLabel());
                } else {
                    // Run name must be unique.
                    String runName = caliperRun.getRunName();
                    LabMetricRun labMetricRun = labMetricRunDao.findByName(runName);
                    if (labMetricRun != null) {
                        messageCollection.addError("This run has been uploaded previously.");
                        pair = Pair.of(labMetricRun, tubeFormation.getLabel());
                    } else {
                        // Run date must be unique so that a search can reveal the latest quant.
                        List<LabMetricRun> sameDateRuns = labMetricRunDao.findSameDateRuns(caliperRun.getRunDate());
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
     *
     * @return Pair of LabMetricRun and the label of the tubeFormation that sourced the plates listed in the csv.
     */
    @DaoFree
    public Pair<LabMetricRun, String> createRNACaliperRunDaoFree(LabMetric.MetricType metricType,
                                                                 CaliperPlateProcessor.CaliperRun caliperRun,
                                                                 Map<String, StaticPlate> mapBarcodeToPlate,
                                                                 long decidingUser,
                                                                 MessageCollection messageCollection) {
        LabMetricRun labMetricRun = new LabMetricRun(caliperRun.getRunName(),
                caliperRun.getRunDate(), metricType);
        String tubeFormationLabel = null;

        labMetricRun.getMetadata().add(new Metadata(Metadata.Key.INSTRUMENT_NAME, caliperRun.getInstrumentName()));

        // Store raw values against plate wells
        boolean requiresReview = false;
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
                    if (tubeFormationLabel == null &&
                        vesselContainer.getEmbedder().getType() == LabVessel.ContainerType.TUBE_FORMATION) {
                        tubeFormationLabel = vesselContainer.getEmbedder().getLabel();
                    }
                    sourceTube = vesselContainer.getVesselAtPosition(sourcePosition);
                    if (sourceTube != null) {
                        LabMetric sourceVesselLabMetric = new LabMetric(plateWellResult.getResult(),
                                metricType, LabMetric.LabUnit.RQS,
                                sourcePosition.name(), caliperRun.getRunDate());
                        sourceVesselLabMetric.getMetadataSet().add(new Metadata(Metadata.Key.DV_200,
                                new BigDecimal(plateWellResult.getDv200TotalArea())));
                        sourceVesselLabMetric.getMetadataSet().add(new Metadata(Metadata.Key.LOWER_MARKER_TIME,
                                new BigDecimal(plateWellResult.getLowerMarkerTime())));
                        sourceVesselLabMetric.getMetadataSet().add(new Metadata(Metadata.Key.NA,
                                String.valueOf(plateWellResult.isNan())));

                        LabMetric.Decider decider = metricType.getDecider();
                        LabMetricDecision decision = null;
                        if (decider != null) {
                            decision = decider.makeDecision(sourceTube, sourceVesselLabMetric, decidingUser);
                        }
                        if (decision != null) {
                            sourceVesselLabMetric.setLabMetricDecision(decision);
                            if (decision.isNeedsReview())
                                requiresReview = true;
                        }

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

        if (requiresReview) {
            messageCollection.addWarning("Rows highlighted in yellow will require review.");
        }

        return Pair.of(labMetricRun, tubeFormationLabel);
    }

    public LabMetricRun createLibraryQuantsFromRunBean(LibraryQuantRunBean libraryQuantRun,
                                                       MessageCollection messageCollection) {
        LabMetricRun labMetricRun = labMetricRunDao.findByName(libraryQuantRun.getRunName());
        if (labMetricRun != null) {
            messageCollection.addError("This run has been uploaded previously.");
        } else {
            List<String> tubeBarcodes = new ArrayList<>();
            Map<String, LibraryQuantBeanType> mapBarcodeToLibraryBean = new HashMap<>();
            for (LibraryQuantBeanType libraryBean : libraryQuantRun.getLibraryQuantBeans()) {
                tubeBarcodes.add(libraryBean.getTubeBarcode());
                mapBarcodeToLibraryBean.put(libraryBean.getTubeBarcode(), libraryBean);
            }
            Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(tubeBarcodes);

            List<LabMetricRun> sameDateRuns = labMetricRunDao.findSameDateRuns(libraryQuantRun.getRunDate());
            if (CollectionUtils.isNotEmpty(sameDateRuns)) {
                messageCollection.addError("A previous upload has the same Run Started timestamp.");
            } else {
                LabMetric.MetricType metricType = LabMetric.MetricType.getByDisplayName(libraryQuantRun.getQuantType());
                labMetricRun =
                        createLibraryQuantsFromRunBeanDaoFree(mapBarcodeToVessel, mapBarcodeToLibraryBean, metricType,
                                messageCollection, libraryQuantRun);
                if (messageCollection.hasErrors()) {
                    ejbContext.setRollbackOnly();
                } else {
                    labMetricRunDao.persist(labMetricRun);
                }
            }
        }

        return labMetricRun;
    }

    @DaoFree
    private LabMetricRun createLibraryQuantsFromRunBeanDaoFree(Map<String, LabVessel> mapBarcodeToVessel,
                                                               Map<String, LibraryQuantBeanType> mapBarcodeToLibraryBean,
                                                               LabMetric.MetricType metricType,
                                                               MessageCollection messageCollection,
                                                               LibraryQuantRunBean libraryQuantRun) {
        LabMetricRun labMetricRun =
                new LabMetricRun(libraryQuantRun.getRunName(), libraryQuantRun.getRunDate(), metricType);
        for (Map.Entry<String, LibraryQuantBeanType> barcodeAndQuant : mapBarcodeToLibraryBean.entrySet()) {
            String vesselLabel = barcodeAndQuant.getKey();
            LibraryQuantBeanType libraryBeans = barcodeAndQuant.getValue();
            LabVessel labVessel = mapBarcodeToVessel.get(vesselLabel);
            VesselPosition vesselPosition = VesselPosition.getByName(libraryBeans.getRackPositionName());
            if (vesselPosition == null) {
                messageCollection.addError("Failed to find position " + libraryBeans.getRackPositionName());
                continue;
            }

            LabMetric labMetric = new LabMetric(libraryBeans.getValue(), metricType,
                    LabMetric.LabUnit.NG_PER_UL, libraryBeans.getRackPositionName(), libraryQuantRun.getRunDate());
            labMetricRun.addMetric(labMetric);
            labVessel.addMetric(labMetric);
        }
        return labMetricRun;
    }

    public LabMetricRun createQpcrRunFromRunBean(QpcrRunBean qpcrRunBean, MessageCollection messageCollection,
                                                 Long userId) {
        LabMetricRun labMetricRun = labMetricRunDao.findByName(qpcrRunBean.getRunName());
        if (labMetricRun != null) {
            messageCollection.addError("This run has been uploaded previously.");
        } else {
            List<String> tubeBarcodes = new ArrayList<>();
            Map<String, LibraryBeansType> mapBarcodeToLibraryBean = new HashMap<>();
            for (LibraryBeansType libraryBean : qpcrRunBean.getLibraryBeans()) {
                tubeBarcodes.add(libraryBean.getTubeBarcode());
                mapBarcodeToLibraryBean.put(libraryBean.getTubeBarcode(), libraryBean);
            }
            Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(tubeBarcodes);

            List<LabMetricRun> sameDateRuns = labMetricRunDao.findSameDateRuns(qpcrRunBean.getRunDate());
            if (CollectionUtils.isNotEmpty(sameDateRuns)) {
                messageCollection.addError("A previous upload has the same Run Started timestamp.");
            } else {
                labMetricRun = createQpcrRunDaoFree(mapBarcodeToVessel, mapBarcodeToLibraryBean,
                        LabMetric.MetricType.VIIA_QPCR, userId, messageCollection, qpcrRunBean);
                if (messageCollection.hasErrors()) {
                    ejbContext.setRollbackOnly();
                } else {
                    labMetricRunDao.persist(labMetricRun);
                }
            }
        }

        return labMetricRun;
    }

    /**
     * Create a LabMetricRun from a QpcrRunBean.
     */
    @DaoFree
    public LabMetricRun createQpcrRunDaoFree(Map<String, LabVessel> mapBarcodeToVessel,
                                             Map<String, LibraryBeansType> mapBarcodeToLibraryBean,
                                             LabMetric.MetricType metricType,
                                             Long decidingUser, MessageCollection messageCollection,
                                             QpcrRunBean qpcrRunBean) {
        LabMetricRun labMetricRun = new LabMetricRun(qpcrRunBean.getRunName(), qpcrRunBean.getRunDate(), metricType);
        for (Map.Entry<String, LibraryBeansType> barcodeAndQuant : mapBarcodeToLibraryBean.entrySet()) {
            String vesselLabel = barcodeAndQuant.getKey();
            LibraryBeansType libraryBeans = barcodeAndQuant.getValue();
            LabVessel labVessel = mapBarcodeToVessel.get(vesselLabel);
            VesselPosition vesselPosition = VesselPosition.getByName(libraryBeans.getWell());
            if (vesselPosition == null) {
                messageCollection.addError("Failed to find position " + libraryBeans.getWell());
                continue;
            }

            LabMetric labMetric = new LabMetric(libraryBeans.getConcentration(), metricType,
                    LabMetric.LabUnit.NG_PER_UL, libraryBeans.getWell(), qpcrRunBean.getRunDate());
            labMetricRun.addMetric(labMetric);
            labVessel.addMetric(labMetric);

            LabMetricDecision labMetricDecision = null;
            if (libraryBeans.isPass()) {
                labMetricDecision = new LabMetricDecision(
                        LabMetricDecision.Decision.PASS, qpcrRunBean.getRunDate(), decidingUser, labMetric);
            } else {
                labMetricDecision = new LabMetricDecision(
                        LabMetricDecision.Decision.FAIL, qpcrRunBean.getRunDate(), decidingUser, labMetric);
            }
            labMetric.setLabMetricDecision(labMetricDecision);
        }
        return labMetricRun;
    }

    /**
     * Forwards any non-clinical quants to BSP. This is done by removing rows from the quant spreadsheet
     * when the microfluor wells were derived from a tube containing a clinical sample.
     */
    public void nonClinicalsToBsp(InputStream quantStream, String tubeFormationLabel,
            Set<StaticPlate> microfluorPlates, String bspUsername, String filename, MessageCollection messages)
            throws Exception {

        // Finds the positions of clinical samples from the tube formation.
        if (StringUtils.isBlank(tubeFormationLabel)) {
            messages.addError("Tube formation label is blank");
            return;
        }
        TubeFormation tubeFormation = proxySafeCast(labVesselDao.findByIdentifier(tubeFormationLabel),
                TubeFormation.class);
        if (tubeFormation == null) {
            messages.addError("No tube formation found for label " + tubeFormationLabel);
            throw new Exception("No tube formation found for label " + tubeFormationLabel);
        }

        Set<VesselPosition> clinicalWellPositions = new HashSet<>();
        boolean hasNonClinicalPositionToTube = false;
        nextPosition:
        for (BarcodedTube tube : tubeFormation.getContainerRole().getMapPositionToVessel().values()) {
            for (MercurySample mercurySample : tube.getMercurySamples()) {
                if (mercurySample.canSampleBeUsedForClinical()) {
                    for (LabVessel descendent : tube.getDescendantVessels()) {
                        if (OrmUtil.proxySafeIsInstance(descendent, PlateWell.class)) {
                            PlateWell plateWell = OrmUtil.proxySafeCast(descendent, PlateWell.class);
                            if (microfluorPlates.contains(plateWell.getPlate())) {
                                clinicalWellPositions.add(plateWell.getVesselPosition());
                            }
                        }
                    }
                    continue nextPosition;
                }
            }
            hasNonClinicalPositionToTube = true;
        }
        if (hasNonClinicalPositionToTube) {
            InputStream filteredQuantStream;
            try {
                filteredQuantStream = filterOutRows(quantStream, clinicalWellPositions);
            } catch (Exception e) {
                messages.addError("Cannot process spreadsheet: " + e.toString());
                throw e;
            }
            try {
                bspRestSender.postToBsp(bspUsername, filename, filteredQuantStream, BSPRestSender.BSP_UPLOAD_QUANT_URL);
            } catch (Exception e) {
                messages.addError("Cannot send research quants to BSP: " + e.toString());
                throw e;
            }
        }
    }

    /**
     * Parses the spreadsheet stream and removes rows containing specified cell names.
     * @return a stream of the filtered spreadsheet
     */
    public InputStream filterOutRows(InputStream quantStream, Set<VesselPosition> excludedPositions)
            throws Exception {
        Workbook workbook = WorkbookFactory.create(quantStream);
        List<Row> toBeRemoved = new ArrayList<>();
        Sheet curveSheet = workbook.getSheet(VarioskanRowParser.QUANTITATIVE_CURVE_FIT1_TAB);
        if (curveSheet == null) {
            throw new Exception("Missing spreadsheet page " + VarioskanRowParser.QUANTITATIVE_CURVE_FIT1_TAB);
        }
        final int COLUMN_CONTAINING_VESSEL_POSITION = 1;
        for (int i = 1; i <= curveSheet.getLastRowNum(); i++) {
            Row row = curveSheet.getRow(i);
            String well = (row != null && row.getCell(COLUMN_CONTAINING_VESSEL_POSITION) != null) ?
                    row.getCell(COLUMN_CONTAINING_VESSEL_POSITION).getStringCellValue().trim() : null;
            if (StringUtils.isNotBlank(well) && excludedPositions.contains(VesselPosition.getByName(well))) {
                toBeRemoved.add(row);
            }
        }
        for (Row row : toBeRemoved) {
            curveSheet.removeRow(row);
        }

        File tempFile = File.createTempFile("FilteredVarioskan", ".xls");
        workbook.write(new FileOutputStream(tempFile));
        return new FileInputStream(tempFile);
    }
}