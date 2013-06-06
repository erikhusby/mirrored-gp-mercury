package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselAndPosition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.LibraryDataType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.PlateTransferType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SampleInfoType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.WellAndSourceTubeType;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mercury-based implementations of services provided by LimsQueryResource.
 *
 * @author breilly
 */
public class LimsQueries {

    private static final String NOT_FOUND =  "NOT_FOUND";

    private StaticPlateDAO staticPlateDAO;
    private LabVesselDao labVesselDao;
    private TwoDBarcodedTubeDAO twoDBarcodedTubeDAO;

    @Inject
    public LimsQueries(StaticPlateDAO staticPlateDAO, LabVesselDao labVesselDao,
                       TwoDBarcodedTubeDAO twoDBarcodedTubeDAO) {
        this.staticPlateDAO = staticPlateDAO;
        this.labVesselDao = labVesselDao;
        this.twoDBarcodedTubeDAO = twoDBarcodedTubeDAO;
    }

    /**
     * Fetch library details for given tube barcodes.  Used by automation engineering "scan and save" application.
     * Library has less meaning in Mercury than in Squid, so many of the DTO fields are null.
     * @param tubeBarcodes from deck
     * @param includeWorkRequestDetails work request has no meaning in Mercury, so this is ignored
     * @return list of DTOs
     */
    public List<LibraryDataType> fetchLibraryDetailsByTubeBarcode(List<String> tubeBarcodes,
                                                                  boolean includeWorkRequestDetails) {
        Map<String,LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(tubeBarcodes);
        return fetchLibraryDetailsByTubeBarcode(mapBarcodeToVessel);
    }

    /**
     * DaoFree implementation of {@link #fetchLibraryDetailsByTubeBarcode(java.util.List, boolean)}
     * @param mapBarcodeToVessel    key is barcode, entry is vessel.  Entry is null if not found in the database
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
                    libraryDataType.getSampleDetails().add(sampleInfoType);
                }
            }
        }
        return libraryDataTypes;
    }

    /**
     * Determines whether all tube barcodes are in the database
     * @param barcodes list of tube barcodes
     * @return true if all tube barcodes are in the database
     */
    public boolean doesLimsRecognizeAllTubes(List<String> barcodes) {
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = twoDBarcodedTubeDAO.findByBarcodes(barcodes);
        for (Map.Entry<String, TwoDBarcodedTube> stringTwoDBarcodedTubeEntry : mapBarcodeToTube.entrySet()) {
            if (stringTwoDBarcodedTubeEntry.getValue() == null) {
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
        StaticPlate plate = staticPlateDAO.findByBarcode(plateBarcode);
        if (plate == null) {
            throw new RuntimeException("Plate not found for barcode: " + plateBarcode);
        }
        return findImmediatePlateParents(plate);
    }

    /**
     * DaoFree implementation of {@link #findImmediatePlateParents(String)}
     * @param plate entity
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
        StaticPlate plate = staticPlateDAO.findByBarcode(plateBarcode);
        if (plate == null) {
            throw new RuntimeException("Plate not found for barcode: " + plateBarcode);
        }
        return fetchParentRackContentsForPlate(plate);
    }

    /**
     * DaoFree implementation of {@link #fetchParentRackContentsForPlate(String)}
     * @param plate entity
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
        StaticPlate plate = staticPlateDAO.findByBarcode(plateBarcode);
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
     * @param plateBarcode barcode of plate for which to fetch transfers
     * @param depth how many levels of transfer to recurse
     * @return plate transfer details
     */
    public List<PlateTransferType> fetchTransfersForPlate(String plateBarcode, int depth) {
        StaticPlate plate = staticPlateDAO.findByBarcode(plateBarcode);
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
     * @param vesselContainer entity
     * @param wellAndSourceTubeTypes list to which to add DTOs
     */
    @DaoFree
    private void addWellsAndTubes(VesselContainer vesselContainer, List<WellAndSourceTubeType> wellAndSourceTubeTypes) {
        if (OrmUtil.proxySafeIsInstance(vesselContainer.getEmbedder(), TubeFormation.class)) {
            TubeFormation tubeFormation = OrmUtil.proxySafeCast(vesselContainer.getEmbedder(),
                    TubeFormation.class);
            for (Map.Entry<VesselPosition, TwoDBarcodedTube> vesselPositionTwoDBarcodedTubeEntry : tubeFormation
                    .getContainerRole().getMapPositionToVessel().entrySet()) {
                WellAndSourceTubeType wellAndSourceTubeType = new WellAndSourceTubeType();
                wellAndSourceTubeType.setTubeBarcode(vesselPositionTwoDBarcodedTubeEntry.getValue().getLabel());
                wellAndSourceTubeType.setWellName(vesselPositionTwoDBarcodedTubeEntry.getKey().name());
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
            Collection<LabMetric> metrics =
                    vessel.getNearestMetricsOfType(LabMetric.MetricType.getByDisplayName(quantType));
            if (metrics != null && metrics.size() == 1) {
                return metrics.iterator().next().getValue().doubleValue();
            } else {
                throw new RuntimeException(
                        "Got more than one quant for barcode:" + tubeBarcode + ", quant type: " + quantType);
            }
        }
        throw new RuntimeException(
                "Tube or quant not found for barcode: " + tubeBarcode + ", quant type: " + quantType);
    }
}
