package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselAndPosition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.PlateTransferType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateType;
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
            result.setSourceBarcode(transfer.getSourceVesselContainer().getEmbedder().getLabel());
            result.setSourceSection(transfer.getSourceSection().getSectionName());
            result.setDestinationBarcode(transfer.getTargetVesselContainer().getEmbedder().getLabel());
            result.setDestinationSection(transfer.getTargetSection().getSectionName());
            results.add(result);
        }
        return results;
    }

    /**
     * service call is fetchIlluminaSeqTemplate(String id,enum idType, boolean isPoolTest)
     * id can be a flowcell barcode, a tube barcode, a strip tube barcode, or a miseq reagent kit (no FCT ticket name)
     * see confluence page for more details:
     * <p/>
     * if fields in the spec are not in mercury, the service will return null values.
     * All field names must be in the returned json but their values can be null.
     *
     * @param id         can be an id for a flowcell barcode, a tube barcode, a strip tube barcode, or a miseq reagent kit
     * @param idType     the type you are fetching. see id.
     * @param isPoolTest
     *
     * @see <a href= "https://confluence.broadinstitute.org/display/GPI/Mercury+V2+ExEx+Fate+of+Thrift+Services+and+Structs">Mercury V2 ExEx Fate of Thrift Services and Structs</a><br/>
     *      <a href= "https://gpinfojira.broadinstitute.org:8443/jira/browse/GPLIM-1309">Unified Loader Service v1</a>
     */
    public SequencingTemplateType fetchIlluminaSeqTemplate(Long id, IdType idType, boolean isPoolTest) {
        SequencingTemplateType sequencingTemplate=null;
        switch (idType) {
        case FLOWCELL:
            break;
        case TUBE:
            break;
        case STRIP_TUBE:
            break;
        case MISEQ_REAGENT_KIT:
            break;
        case FCT:
            break;
        }
        // TODO: implement this!!
        return sequencingTemplate;
    }

    /**
     * The type object that will be returned for the given identifier.     *
     */
    public static enum IdType {
        FLOWCELL, TUBE, STRIP_TUBE, MISEQ_REAGENT_KIT, FCT;
    }
}
