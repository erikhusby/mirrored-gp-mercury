/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.IndexPositionType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.IndexingSchemeType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateLaneType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateType;

import javax.inject.Inject;
import java.util.EnumSet;

public class UnifiedLoader {
    @Inject
    IlluminaFlowcellDao illuminaFlowcellDao;
    @Inject
    LabVesselDao labVesselDao;

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
        LabVessel labVessel = null;
        EnumSet<LabEventType> searchEvents = null;
        switch (queryVesselType) {
        case FLOWCELL:
            labVessel = illuminaFlowcellDao.findByBarcode(id);
            searchEvents = EnumSet.of(LabEventType.DENATURE_TO_FLOWCELL_TRANSFER, LabEventType.DENATURE_TRANSFER);
            break;
        case STRIP_TUBE:
            labVessel = labVesselDao.findByIdentifier(id);
            searchEvents = EnumSet.of(LabEventType.DENATURE_TRANSFER);
            break;
        case TUBE:
        case MISEQ_REAGENT_KIT:
        default:
            throw new RuntimeException(String.format("Sequencing template not available for %s.", queryVesselType));
        }

        SequencingTemplateType sequencingTemplate = null;
        if (!searchEvents.isEmpty()) {
            for (LabEvent labEvent : labVessel.getEvents()) {
                if (searchEvents.contains(labEvent.getLabEventType())) {
                    sequencingTemplate = getSequencingTemplate(labVessel, labEvent, queryVesselType);
                }
            }
        } else {
            sequencingTemplate = getSequencingTemplate(labVessel, labVessel.getLatestEvent(), queryVesselType);
        }

        return sequencingTemplate;
    }

    /**
     * Use information from the source and target lab vessels to populate a the sequencing template with lanes.
     *
     * @param labVessel  lab vessel from the original query.
     * @param labEvent   the lab event that populated the lab vessel.
     * @param vesselType type of vessel being queried.
     *
     * @return
     */
    private SequencingTemplateType getSequencingTemplate(LabVessel labVessel, LabEvent labEvent,
                                                         QueryVesselType vesselType) {
        LabVessel flowcell = null;

        SequencingTemplateType sequencingTemplate = new SequencingTemplateType();
        VesselGeometry targetGeometry = null;
        LabVessel sourceVessel = null;

        switch (vesselType) {
        case STRIP_TUBE:
            for (CherryPickTransfer transfer : labEvent.getCherryPickTransfers()) {
                sourceVessel = transfer.getSourceVesselContainer().getEmbedder();
                targetGeometry =
                        transfer.getTargetVesselContainer().getEmbedder().getVesselGeometry();
            }

            break;
        case FLOWCELL:
            flowcell = labVessel;
            for (VesselToSectionTransfer vesselToSectionTransfer : labEvent.getVesselToSectionTransfers()) {
                sourceVessel = vesselToSectionTransfer.getSourceVessel();
                targetGeometry = vesselToSectionTransfer.getTargetVesselContainer().getEmbedder().getVesselGeometry();
            }

            break;
        case TUBE:
            break;
        case MISEQ_REAGENT_KIT:
            break;
        default:
        }

        if (flowcell != null) {
            sequencingTemplate.setBarcode(flowcell.getLabel());
        }

        sequencingTemplate = populateTemplateLanes(sequencingTemplate, labVessel, sourceVessel, targetGeometry);

        return sequencingTemplate;
    }

    /**
     * Use information from the source and target lab vessels to populate a the sequencing template with lanes.
     *
     * @param sequencingTemplate template to populate.
     * @param labVessel          lab vessel from the original query.
     * @param sourceVessel       the lab vessel that populated the lab vessel being queried.
     * @param targetGeometry     the geometry of the lab vessel.
     *
     * @return
     */
    private SequencingTemplateType populateTemplateLanes(SequencingTemplateType sequencingTemplate, LabVessel labVessel,
                                                         LabVessel sourceVessel,
                                                         VesselGeometry targetGeometry) {
        for (VesselPosition targetPosition : targetGeometry.getVesselPositions()) {
            SequencingTemplateLaneType lane = new SequencingTemplateLaneType();
            lane.setLaneName(targetPosition.name());
            lane.setLoadingVesselLabel(sourceVessel.getLabel());

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
            sequencingTemplate.getLanes().add(lane);
        }
        return sequencingTemplate;
    }
}
