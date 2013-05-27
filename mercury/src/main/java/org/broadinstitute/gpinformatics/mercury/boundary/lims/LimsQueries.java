package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselAndPosition;
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

    @Inject
    public LimsQueries(StaticPlateDAO staticPlateDAO, LabVesselDao labVesselDao) {
        this.staticPlateDAO = staticPlateDAO;
        this.labVesselDao = labVesselDao;
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

    public boolean doesLimsRecognizeAllTubes(List<String> barcodes) {
        return false;
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
        List<StaticPlate> parents = plate.getImmediatePlateParents();
        List<String> parentPlateBarcodes = new ArrayList<String>();
        for (StaticPlate parent : parents) {
            parentPlateBarcodes.add(parent.getLabel());
        }
        return parentPlateBarcodes;
    }

    public Map<String, Boolean> fetchParentRackContentsForPlate(String plateBarcode) {
        Map<String, Boolean> map = new HashMap<String, Boolean>();
        StaticPlate plate = staticPlateDAO.findByBarcode(plateBarcode);
        Map<VesselPosition, Boolean> hasRackContentByWell = plate.getHasRackContentByWell();
        for (Map.Entry<VesselPosition, Boolean> entry : hasRackContentByWell.entrySet()) {
            map.put(entry.getKey().name(), entry.getValue());
        }
        return map;
    }

    public List<WellAndSourceTubeType> fetchSourceTubesForPlate(String plateBarcode) {
        List<WellAndSourceTubeType> results = new ArrayList<WellAndSourceTubeType>();
        StaticPlate plate = staticPlateDAO.findByBarcode(plateBarcode);
        for (VesselAndPosition vesselAndPosition : plate.getNearestTubeAncestors()) {
            WellAndSourceTubeType result = new WellAndSourceTubeType();
            result.setWellName(vesselAndPosition.getPosition().name());
            result.setTubeBarcode(vesselAndPosition.getVessel().getLabel());
            results.add(result);
        }
        return results;
    }

    public List<PlateTransferType> fetchTransfersForPlate(String plateBarcode, int depth) {
        List<PlateTransferType> results = new ArrayList<PlateTransferType>();
        StaticPlate plate = staticPlateDAO.findByBarcode(plateBarcode);
        List<SectionTransfer> transfers = plate.getUpstreamPlateTransfers(depth);
        for (SectionTransfer transfer : transfers) {
            PlateTransferType result = new PlateTransferType();
            result.setSourceBarcode(transfer.getSourceVesselContainer().getEmbedder().getLabCentricName());
            result.setSourceSection(transfer.getSourceSection().getSectionName());
            result.setDestinationBarcode(transfer.getTargetVesselContainer().getEmbedder().getLabel());
            result.setDestinationSection(transfer.getTargetSection().getSectionName());
            results.add(result);
        }
        return results;
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
