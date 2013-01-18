package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.PlateTransferType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.WellAndSourceTubeType;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mercury-based implementations of services provided by LimsQueryResource.
 *
 * @author breilly
 */
public class LimsQueries {

    private StaticPlateDAO staticPlateDAO;

    @Inject
    public LimsQueries(StaticPlateDAO staticPlateDAO) {
        this.staticPlateDAO = staticPlateDAO;
    }

    public boolean doesLimsRecognizeAllTubes(List<String> barcodes) {
        return false;
    }

    /**
     * Returns a list of plate barcodes that had material directly transferred into the plate with the given barcode.
     *
     * @param plateBarcode the barcode of the plate to query
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
        List<WellAndSourceTubeType> tubes = new ArrayList<WellAndSourceTubeType>();
        return tubes;
    }

    public List<PlateTransferType> fetchTransfersForPlate(String plateBarcode, int depth) {
        List<PlateTransferType> transfers = new ArrayList<PlateTransferType>();
        // TODO: implement using StaticPlate.getUpstreamPlateTransfers()
        return transfers;
    }
}
