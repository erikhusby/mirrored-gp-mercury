package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselAndPosition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ConcentrationAndVolumeAndWeightType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.LibraryDataType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.PlateTransferType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SampleInfoType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.WellAndSourceTubeType;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mercury-based implementations of services provided by LimsQueryResource.
 *
 * @author breilly
 */
public class LimsQueries {

    private static final String NOT_FOUND = "NOT_FOUND";

    private StaticPlateDao staticPlateDao;
    private LabVesselDao labVesselDao;
    private BarcodedTubeDao barcodedTubeDao;

    @Inject
    public LimsQueries(StaticPlateDao staticPlateDao, LabVesselDao labVesselDao,
                       BarcodedTubeDao barcodedTubeDao) {
        this.staticPlateDao = staticPlateDao;
        this.labVesselDao = labVesselDao;
        this.barcodedTubeDao = barcodedTubeDao;
    }

    /**
     * Fetch library details for given tube barcodes.  Used by automation engineering "scan and save" application.
     * Library has less meaning in Mercury than in Squid, so many of the DTO fields are null.
     *
     * @param tubeBarcodes              from deck
     * @param includeWorkRequestDetails work request has no meaning in Mercury, so this is ignored
     *
     * @return list of DTOs
     */
    public List<LibraryDataType> fetchLibraryDetailsByTubeBarcode(List<String> tubeBarcodes,
                                                                  boolean includeWorkRequestDetails) {
        Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(tubeBarcodes);
        return fetchLibraryDetailsByTubeBarcode(mapBarcodeToVessel);
    }

    /**
     * DaoFree implementation of {@link #fetchLibraryDetailsByTubeBarcode(java.util.List, boolean)}
     *
     * @param mapBarcodeToVessel key is barcode, entry is vessel.  Entry is null if not found in the database
     *
     * @return list of DTOs
     */
    @DaoFree
    List<LibraryDataType> fetchLibraryDetailsByTubeBarcode(Map<String, LabVessel> mapBarcodeToVessel) {
        List<LibraryDataType> libraryDataTypes = new ArrayList<>();
        for (Map.Entry<String, LabVessel> stringLabVesselEntry : mapBarcodeToVessel.entrySet()) {
            LibraryDataType libraryDataType = new LibraryDataType();
            libraryDataTypes.add(libraryDataType);
            libraryDataType.setLibraryName(stringLabVesselEntry.getKey());
            libraryDataType.setTubeBarcode(stringLabVesselEntry.getKey());
            if (stringLabVesselEntry.getValue() == null) {
                libraryDataType.setLibraryName(NOT_FOUND);
                libraryDataType.setWasFound(false);
            } else {
                libraryDataType.setWasFound(true);
                for (SampleInstance sampleInstance : stringLabVesselEntry.getValue().getSampleInstances()) {
                    SampleInfoType sampleInfoType = new SampleInfoType();
                    sampleInfoType.setSampleName(sampleInstance.getStartingSample().getSampleKey());
                    sampleInfoType.setLsid("not implemented yet");
                    libraryDataType.getSampleDetails().add(sampleInfoType);
                }
            }
        }
        return libraryDataTypes;
    }

    /**
     * Determines whether all tube barcodes are in the database
     *
     * @param barcodes list of tube barcodes
     *
     * @return true if all tube barcodes are in the database
     */
    public boolean doesLimsRecognizeAllTubes(List<String> barcodes) {
        Map<String, BarcodedTube> mapBarcodeToTube = barcodedTubeDao.findByBarcodes(barcodes);
        for (Map.Entry<String, BarcodedTube> stringBarcodedTubeEntry : mapBarcodeToTube.entrySet()) {
            if (stringBarcodedTubeEntry.getValue() == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a list of plate barcodes that had material directly transferred into the plate with the given barcode.
     *
     * @param plateBarcode the barcode of the plate to query
     *
     * @return the barcodes of the immediate parent plates
     */
    public List<String> findImmediatePlateParents(String plateBarcode) {
        StaticPlate plate = staticPlateDao.findByBarcode(plateBarcode);
        if (plate == null) {
            throw new RuntimeException("Plate not found for barcode: " + plateBarcode);
        }
        return findImmediatePlateParents(plate);
    }

    /**
     * DaoFree implementation of {@link #findImmediatePlateParents(String)}
     *
     * @param plate entity
     *
     * @return list of barcodes
     */
    @DaoFree
    public List<String> findImmediatePlateParents(StaticPlate plate) {
        List<StaticPlate> parents = plate.getImmediatePlateParents();
        List<String> parentPlateBarcodes = new ArrayList<>();
        for (StaticPlate parent : parents) {
            parentPlateBarcodes.add(parent.getLabel());
        }
        return parentPlateBarcodes;
    }

    /**
     * See {@link LimsQueryResource#fetchParentRackContentsForPlate(String)}.
     */
    public Map<String, Boolean> fetchParentRackContentsForPlate(String plateBarcode) {
        StaticPlate plate = staticPlateDao.findByBarcode(plateBarcode);
        if (plate == null) {
            throw new RuntimeException("Plate not found for barcode: " + plateBarcode);
        }
        return fetchParentRackContentsForPlate(plate);
    }

    /**
     * DaoFree implementation of {@link #fetchParentRackContentsForPlate(String)}
     *
     * @param plate entity
     *
     * @return map from well position to true if position occupied
     */
    @DaoFree
    public Map<String, Boolean> fetchParentRackContentsForPlate(StaticPlate plate) {
        Map<VesselPosition, Boolean> hasRackContentByWell = plate.getHasRackContentByWell();
        Map<String, Boolean> map = new HashMap<>();
        for (Map.Entry<VesselPosition, Boolean> entry : hasRackContentByWell.entrySet()) {
            map.put(entry.getKey().name(), entry.getValue());
        }
        return map;
    }

    public List<WellAndSourceTubeType> fetchSourceTubesForPlate(String plateBarcode) {
        List<WellAndSourceTubeType> results = new ArrayList<>();
        StaticPlate plate = staticPlateDao.findByBarcode(plateBarcode);
        if (plate == null) {
            throw new RuntimeException("Plate not found for barcode: " + plateBarcode);
        }
        for (VesselAndPosition vesselAndPosition : plate.getNearestTubeAncestors()) {
            WellAndSourceTubeType result = new WellAndSourceTubeType();
            result.setWellName(vesselAndPosition.getPosition().name());
            result.setTubeBarcode(vesselAndPosition.getVessel().getLabel());
            results.add(result);
        }
        return results;
    }

    /**
     * Fetch ancestor transfers for a plate
     *
     * @param plateBarcode barcode of plate for which to fetch transfers
     * @param depth        how many levels of transfer to recurse
     *
     * @return plate transfer details
     */
    public List<PlateTransferType> fetchTransfersForPlate(String plateBarcode, int depth) {
        StaticPlate plate = staticPlateDao.findByBarcode(plateBarcode);
        if (plate == null) {
            throw new RuntimeException("Plate not found for barcode: " + plateBarcode);
        }
        return fetchTransfersForPlate(plate, depth);
    }

    /**
     * DaoFree implementation of {@link #fetchTransfersForPlate(String, int)}
     */
    @DaoFree
    public List<PlateTransferType> fetchTransfersForPlate(StaticPlate plate, int depth) {
        List<PlateTransferType> results = new ArrayList<>();
        List<SectionTransfer> transfers = plate.getUpstreamPlateTransfers(depth);
        for (SectionTransfer transfer : transfers) {
            PlateTransferType result = new PlateTransferType();
            result.setSourceBarcode(transfer.getSourceVesselContainer().getEmbedder().getLabCentricName());
            result.setSourceSection(transfer.getSourceSection().getSectionName());
            addWellsAndTubes(transfer.getSourceVesselContainer(), result.getSourcePositionMap());

            result.setDestinationBarcode(transfer.getTargetVesselContainer().getEmbedder().getLabCentricName());
            result.setDestinationSection(transfer.getTargetSection().getSectionName());
            addWellsAndTubes(transfer.getTargetVesselContainer(), result.getDestinationPositionMap());
            results.add(result);
        }
        return results;
    }

    /**
     * Adds WellAndTube DTOs to {@link PlateTransferType}
     *
     * @param vesselContainer        entity
     * @param wellAndSourceTubeTypes list to which to add DTOs
     */
    @DaoFree
    private void addWellsAndTubes(VesselContainer vesselContainer, List<WellAndSourceTubeType> wellAndSourceTubeTypes) {
        if (OrmUtil.proxySafeIsInstance(vesselContainer.getEmbedder(), TubeFormation.class)) {
            TubeFormation tubeFormation = OrmUtil.proxySafeCast(vesselContainer.getEmbedder(),
                    TubeFormation.class);
            for (Map.Entry<VesselPosition, BarcodedTube> vesselPositionBarcodedTubeEntry : tubeFormation
                    .getContainerRole().getMapPositionToVessel().entrySet()) {
                WellAndSourceTubeType wellAndSourceTubeType = new WellAndSourceTubeType();
                wellAndSourceTubeType.setTubeBarcode(vesselPositionBarcodedTubeEntry.getValue().getLabel());
                wellAndSourceTubeType.setWellName(vesselPositionBarcodedTubeEntry.getKey().name());
                wellAndSourceTubeTypes.add(wellAndSourceTubeType);
            }
        }
    }

    /**
     * This method returns the double value of the nearest quant of type quantType from the vessel specified by the tubeBarcode.
     *
     * @param tubeBarcode The barcode of the tube to look up quants on.
     * @param quantType   The type of quant we are looking for.
     *
     * @return The double value of the quant we are looking for.
     */
    public Double fetchQuantForTube(String tubeBarcode, String quantType) {
        LabVessel vessel = labVesselDao.findByIdentifier(tubeBarcode);
        if (vessel != null) {
            List<LabMetric> metrics = vessel.getNearestMetricsOfType(LabMetric.MetricType.getByDisplayName(quantType));
            if (metrics != null && !metrics.isEmpty()) {
                return metrics.get(metrics.size() - 1).getValue().doubleValue();
            }
        }
        throw new RuntimeException(
                "Tube or quant not found for barcode: " + tubeBarcode + ", quant type: " + quantType);
    }

    /**
     * This method returns a mapping from tube barcode to the concentration and volume
     * for each tube barcode specified.
     *
     * @param tubeBarcodes The barcodes of the tubes to up concentration and volume for.
     *
     * @return Map of barcode to Concentration and Volume of the quant we are looking for.
     */
    public Map<String, ConcentrationAndVolumeAndWeightType> fetchConcentrationAndVolumeAndWeightForTubeBarcodes(
            List<String> tubeBarcodes) {
        Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(tubeBarcodes);
        return fetchConcentrationAndVolumeAndWeightForTubeBarcodes(mapBarcodeToVessel);
    }

    @DaoFree
    public Map<String, ConcentrationAndVolumeAndWeightType> fetchConcentrationAndVolumeAndWeightForTubeBarcodes(
            Map<String, LabVessel> mapBarcodeToVessel) {
        Map<String, ConcentrationAndVolumeAndWeightType> concentrationAndVolumeAndWeightTypeMap = new HashMap<>();
        for (Map.Entry<String, LabVessel> entry: mapBarcodeToVessel.entrySet()) {
            String tubeBarcode = entry.getKey();
            LabVessel labVessel = entry.getValue();
            ConcentrationAndVolumeAndWeightType concentrationAndVolumeAndWeightType =
                    new ConcentrationAndVolumeAndWeightType();
            concentrationAndVolumeAndWeightType.setTubeBarcode(tubeBarcode);
            if (entry.getValue() == null) {
                concentrationAndVolumeAndWeightType.setWasFound(false);
            } else {
                concentrationAndVolumeAndWeightType.setWasFound(true);
                if (labVessel.getReceptacleWeight() != null) {
                    concentrationAndVolumeAndWeightType.setWeight(labVessel.getReceptacleWeight());
                }
                if (labVessel.getVolume() != null) {
                    concentrationAndVolumeAndWeightType.setVolume(labVessel.getVolume());
                }

                Set<LabMetric> metrics = labVessel.getConcentrationMetrics();
                if (metrics != null && !metrics.isEmpty()) {
                    List<LabMetric> metricList = new ArrayList<>(metrics);
                    Collections.sort(metricList, new LabMetric.LabMetricRunDateComparator());
                    LabMetric.MetricType metricType = metricList.get(0).getName();
                    for (LabMetric labMetric : metricList) {
                        if (labMetric.getName() != metricType) {
                            throw new RuntimeException(
                                    "Got more than one quant for barcode:" + tubeBarcode);
                        }
                    }
                    LabMetric labMetric = metricList.get(0);
                    concentrationAndVolumeAndWeightType.setConcentration(labMetric.getValue());
                    concentrationAndVolumeAndWeightType
                            .setConcentrationUnits(labMetric.getUnits().getDisplayName());
                } else if (labVessel.getConcentration() != null) {
                    concentrationAndVolumeAndWeightType.setConcentration(labVessel.getConcentration());
                }
            }
            concentrationAndVolumeAndWeightTypeMap.put(tubeBarcode, concentrationAndVolumeAndWeightType);
        }

        return concentrationAndVolumeAndWeightTypeMap;
    }
}
