package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselAndPosition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.IndexPositionType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.IndexingSchemeType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.PlateTransferType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateLaneType;
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
     * @param id              can be an id for a flowcell barcode, a tube barcode, a strip tube barcode, or a miseq reagent kit
     * @param queryVesselType the type you are fetching. see id.
     * @param isPoolTest      TODO: what is the purpose of isPoolTest? Does it mean only search MiSeq??
     *
     * @see <a href= "https://confluence.broadinstitute.org/display/GPI/Mercury+V2+ExEx+Fate+of+Thrift+Services+and+Structs">Mercury V2 ExEx Fate of Thrift Services and Structs</a><br/>
     *      <a href= "https://gpinfojira.broadinstitute.org:8443/jira/browse/GPLIM-1309">Unified Loader Service v1</a>
     */
    public SequencingTemplateType fetchSequencingTemplate(String id, QueryVesselType queryVesselType,
                                                          boolean isPoolTest) {
        SequencingTemplateType sequencingTemplate = new SequencingTemplateType();
        sequencingTemplate.setBarcode(id);
        LabVessel labVessel = null;
        switch (queryVesselType) {
        case FLOWCELL:
            labVessel = illuminaFlowcellDao.findByBarcode(id);
            for (LabEvent labEvent : labVessel.getEvents()) {
                if (labEvent.getLabEventType() == LabEventType.DENATURE_TO_FLOWCELL_TRANSFER) {
                    for (VesselToSectionTransfer vesselToSectionTransfer : labEvent.getVesselToSectionTransfers()) {
                        final LabVessel sourceVessel = vesselToSectionTransfer.getSourceVessel();
                        final VesselGeometry targetGeometry =
                                vesselToSectionTransfer.getTargetVesselContainer().getEmbedder().getVesselGeometry();
                        for (VesselPosition targetPosition : targetGeometry.getVesselPositions()) {
                            SequencingTemplateLaneType lane = new SequencingTemplateLaneType();
                            lane.setLaneName(targetPosition.name());
                                for (MolecularIndexReagent molecularIndexReagent : labVessel.getIndexes()) {
                                    final MolecularIndexingScheme molecularIndexingScheme =
                                            molecularIndexReagent.getMolecularIndexingScheme();
                                    for (MolecularIndexingScheme.IndexPosition indexPosition : molecularIndexingScheme
                                            .getIndexes().keySet()) {
                                        IndexingSchemeType indexingSchemeType = new IndexingSchemeType();
                                        indexingSchemeType.setSequence(
                                                molecularIndexingScheme.getIndex(indexPosition).getSequence());
                                        indexingSchemeType.setPosition(
                                                IndexPositionType.fromValue(indexPosition.getPosition()));
                                        lane.getIndexingScheme().add(indexingSchemeType);
                                    }

                                }
                            lane.setLoadingVesselLabel(sourceVessel.getLabel());
                            sequencingTemplate.getLanes().add(lane);
                        }
                    }

                }
            }


            break;
        case TUBE:
        case STRIP_TUBE:
            labVessel = labVesselDao.findByIdentifier(id);
            break;
        case MISEQ_REAGENT_KIT:
            break;
        default:
            throw new RuntimeException(String.format("Sequencing template not available for %s.", queryVesselType));
        }
        return sequencingTemplate;
    }


    @Inject
    IlluminaFlowcellDao illuminaFlowcellDao;
    @Inject
    LabVesselDao labVesselDao;
    @Inject
    MercurySampleDao mercurySampleDao;

    /**
     * What you will be searching for with the ID parameter in fetchSequencingTemplate.
     * Yes, this is an enum of enums. Having a unique enum which was basically a subset of
     * LabVessel.ContainterType seemed creepy.
     * FTC Ticket names are not yet supported
     */
    public static enum QueryVesselType {
        FLOWCELL(LabVessel.ContainerType.FLOWCELL),
        TUBE(LabVessel.ContainerType.TUBE),
        STRIP_TUBE(LabVessel.ContainerType.STRIP_TUBE),
        MISEQ_REAGENT_KIT(LabVessel.ContainerType.MISEQ_REAGENT_KIT);

        private LabVessel.ContainerType value;

        QueryVesselType(LabVessel.ContainerType value) {
            this.value = value;
        }

        public LabVessel.ContainerType getValue() {
            return value;
        }
    }
}
