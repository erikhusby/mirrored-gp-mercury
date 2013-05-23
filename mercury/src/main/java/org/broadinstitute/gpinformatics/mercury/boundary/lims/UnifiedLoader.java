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

import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.IndexPositionType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.IndexingSchemeType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateLaneType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateType;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        IlluminaFlowcell illuminaFlowcell = null;
        EnumSet<LabEventType> searchEvents = null;
        switch (queryVesselType) {
        case FLOWCELL:
            illuminaFlowcell = illuminaFlowcellDao.findByBarcode(id);
            searchEvents = EnumSet.of(LabEventType.DENATURE_TO_FLOWCELL_TRANSFER, LabEventType.DENATURE_TRANSFER);
            break;
        case TUBE:
        case STRIP_TUBE:
        case MISEQ_REAGENT_KIT:
        default:
            throw new RuntimeException(String.format("Sequencing template unavailable for %s.", queryVesselType));
        }

        Map<VesselPosition, LabVessel> loadedVessels = new HashMap<VesselPosition, LabVessel>();
        for (LabEvent labEvent : illuminaFlowcell.getEvents()) {
            if (searchEvents.contains(labEvent.getLabEventType())) {
                for (VesselToSectionTransfer vesselToSectionTransfer : labEvent.getVesselToSectionTransfers()) {
                    //region Description
                    for (VesselPosition vesselPosition : vesselToSectionTransfer.getTargetSection().getWells()) {
                        loadedVessels.put(vesselPosition, vesselToSectionTransfer.getSourceVessel());
                    }

                    //endregion
//                    loadedVessels.add(vesselToSectionTransfer.getSourceVessel());
                }

            }
        }
        return getSequencingTemplate(illuminaFlowcell, loadedVessels);
    }

    /**
     * Use information from the source and target lab vessels to populate a the sequencing template with lanes.
     *
     * @param flowcell      the flowcell we are loading.
     * @param sourceVessels the lab vessels loading the flowcell.
     *
     * @return a populated Sequencing template
     */
    @DaoFree
    public SequencingTemplateType getSequencingTemplate(IlluminaFlowcell flowcell,
                                                         Map<VesselPosition, LabVessel> sourceVessels) {
        SequencingTemplateType sequencingTemplate = new SequencingTemplateType();
        List<SequencingTemplateLaneType> lanes = new ArrayList<SequencingTemplateLaneType>();

        for (VesselPosition vesselPosition : sourceVessels.keySet()) {
            LabVessel sourceVessel = sourceVessels.get(vesselPosition);
            SequencingTemplateLaneType lane = new SequencingTemplateLaneType();
            lane.setLaneName(vesselPosition.name());
            lane.setLoadingVesselLabel(sourceVessel.getLabel());

            for (MolecularIndexReagent molecularIndexReagent : flowcell.getIndexes()) {
                final MolecularIndexingScheme molecularIndexingScheme = molecularIndexReagent.getMolecularIndexingScheme();
                for (MolecularIndexingScheme.IndexPosition indexPosition : molecularIndexingScheme.getIndexes().keySet()) {
                    IndexingSchemeType indexingSchemeType = new IndexingSchemeType();
                    indexingSchemeType.setSequence(molecularIndexingScheme.getIndex(indexPosition).getSequence());
                    indexingSchemeType.setPosition(IndexPositionType.fromValue(indexPosition.getPosition()));
                    lane.getIndexingScheme().add(indexingSchemeType);
                }
            }
            lanes.add(lane);
        }


        if (lanes.isEmpty()) {
            // Do we need to create "null" lanes to satisfy the user requirement of returning null
            // when we don't have the data?
            SequencingTemplateLaneType lane = new SequencingTemplateLaneType();
            lane.getIndexingScheme().add(new IndexingSchemeType());
            lanes.add(lane);
        }
        sequencingTemplate.getLanes().addAll(lanes);

        if (flowcell != null) {
            sequencingTemplate.setBarcode(flowcell.getLabel());
        }

        return sequencingTemplate;
    }
}
