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
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.MiSeqReagentKitDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MiSeqReagentKit;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselAndPosition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.SequencingConfigDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateLaneType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateType;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SequencingTemplateFactory {
    @Inject
    IlluminaFlowcellDao illuminaFlowcellDao;
    @Inject
    MiSeqReagentKitDao miSeqReagentKitDao;

    @Inject
    LabVesselDao labVesselDao;

    /**
     * What you will be searching for with the ID parameter in fetchSequencingTemplate.
     * Yes, this is an enum of enums. Having a unique enum which was basically a subset of
     * LabVessel.ContainerType seemed creepy.
     * <p/>
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
        Set<VesselAndPosition> loadedVesselsAndPositions;
        switch (queryVesselType) {
        case FLOWCELL:
            IlluminaFlowcell illuminaFlowcell = illuminaFlowcellDao.findByBarcode(id);
            if (illuminaFlowcell == null) {
                throw new RuntimeException(String.format("Flowcell '%s' was not found.", id));
            }
            loadedVesselsAndPositions = getLoadingVessels(illuminaFlowcell);
            return getSequencingTemplate(illuminaFlowcell, loadedVesselsAndPositions, isPoolTest);
        case MISEQ_REAGENT_KIT:
            MiSeqReagentKit miSeqReagentKit = miSeqReagentKitDao.findByBarcode(id);
            if (miSeqReagentKit == null) {
                throw new RuntimeException(String.format("MiSeq Reagent Kit '%s' was not found.", id));
            }
            return getSequencingTemplate(miSeqReagentKit, isPoolTest);

        case TUBE:
            LabVessel denatureTube = labVesselDao.findByIdentifier(id);
            if (denatureTube == null) {
                throw new RuntimeException(String.format("Denature Tube '%s' was not found.", id));
            }
            return getSequencingTemplate(denatureTube, isPoolTest);
        // Don't support the following for now, so fall through and throw exception.
        case STRIP_TUBE:
        default:
            throw new RuntimeException(String.format("Sequencing template unavailable for %s.", queryVesselType));
        }
    }

    /**
     * get information on what vessels loaded given flowcell.
     *
     * @param flowcell the flowcell to get lane information on
     *
     * @return Map of VesselAndPosition representing what is loaded onto the flowcell
     */
    @DaoFree
    public Set<VesselAndPosition> getLoadingVessels(IlluminaFlowcell flowcell) {
        Set<VesselAndPosition> loadedVesselsAndPositions = new HashSet<>();
        for (Map.Entry<VesselPosition, LabVessel> vesselPositionEntry : flowcell.getNearestTubeAncestorsForLanes()
                .entrySet()) {
            loadedVesselsAndPositions.add(new VesselAndPosition(vesselPositionEntry.getValue(),
                    vesselPositionEntry.getKey()));
        }
        return loadedVesselsAndPositions;
    }


    private SequencingTemplateType getSequencingTemplate(LabVessel denatureTube, boolean isPoolTest) {
        SequencingConfigDef sequencingConfig = getSequencingConfig(isPoolTest);
        //get all flowcells for this denature tube
        Collection<IlluminaFlowcell> flowcells = denatureTube.getDescendantFlowcells();
        SequencingTemplateType sequencingTemplate = LimsQueryObjectFactory.createSequencingTemplate(null,
                null, isPoolTest, sequencingConfig.getInstrumentWorkflow().getValue(),
                sequencingConfig.getChemistry().getValue(), sequencingConfig.getReadStructure().getValue());

        // sequencingTemplate.getLanes().addAll(lanes);
        return null;
    }

    /**
     * Use information from the source and target lab vessels to populate a the sequencing template with lanes.
     *
     * @param flowcell                  the flowcell we are loading.
     * @param loadedVesselsAndPositions the lab vessels loading the flowcell.
     *
     * @return a populated Sequencing template
     */
    @DaoFree
    public SequencingTemplateType getSequencingTemplate(IlluminaFlowcell flowcell,
                                                        Set<VesselAndPosition> loadedVesselsAndPositions,
                                                        boolean isPoolTest) {

        List<SequencingTemplateLaneType> lanes = new ArrayList<>();

        for (VesselAndPosition vesselAndPosition : loadedVesselsAndPositions) {
            LabVessel sourceVessel = vesselAndPosition.getVessel();
            VesselPosition vesselPosition = vesselAndPosition.getPosition();
            Double concentration = getLoadingConcentrationForVessel(sourceVessel);
            SequencingTemplateLaneType lane =
                    LimsQueryObjectFactory.createSequencingTemplateLaneType(vesselPosition.name(), concentration,
                            sourceVessel.getLabel());
            lanes.add(lane);
        }

        if (lanes.isEmpty()) {
            // Do we need to create "null" lanes to satisfy the user requirement of returning null
            // when we don't have the data?
            SequencingTemplateLaneType lane = new SequencingTemplateLaneType();
            lanes.add(lane);
        }

        SequencingConfigDef sequencingConfig = getSequencingConfig(isPoolTest);

        SequencingTemplateType sequencingTemplate = LimsQueryObjectFactory.createSequencingTemplate(null,
                flowcell.getLabel(), isPoolTest, sequencingConfig.getInstrumentWorkflow().getValue(),
                sequencingConfig.getChemistry().getValue(), sequencingConfig.getReadStructure().getValue());

        sequencingTemplate.getLanes().addAll(lanes);
        return sequencingTemplate;
    }

    /**
     * This method gets the loading concentration from the batch/vessel relationship.
     *
     * @param sourceVessel The vessel to get the loading concentration of.
     *
     * @return The loading concentration for the vessel.
     */
    private Double getLoadingConcentrationForVessel(LabVessel sourceVessel) {
        Collection<LabBatch> batches = sourceVessel.getLabBatchesOfType(LabBatch.LabBatchType.FCT);
        Double concentration = null;
        for (LabBatch batch : batches) {
            for (LabBatchStartingVessel labBatchStartingVessel : batch.getLabBatchStartingVessels()) {
                //All the concentrations should match. If they don't throw an exception.
                if (concentration != null && concentration != (double) labBatchStartingVessel.getConcentration()) {
                    throw new RuntimeException("Found multiple concentrations that do no match.");
                }
                concentration = (double) labBatchStartingVessel.getConcentration();
            }
        }
        return concentration;
    }

    /**
     * Populate a the sequencing template with lanes.
     *
     * @param miSeqReagentKit the reagentKit we are querying.
     *
     * @return a populated Sequencing template
     */
    @DaoFree
    public SequencingTemplateType getSequencingTemplate(MiSeqReagentKit miSeqReagentKit, boolean isPoolTest) {
        SequencingConfigDef sequencingConfig = getSequencingConfig(isPoolTest);
        Double concentration = null;
        if (miSeqReagentKit.getConcentration() != null) {
            concentration = miSeqReagentKit.getConcentration().doubleValue();
        }
        List<SequencingTemplateLaneType> lanes = new ArrayList<>();
        for (String laneName : IlluminaFlowcell.FlowcellType.MiSeqFlowcell.getVesselGeometry().getRowNames()) {
            lanes.add(LimsQueryObjectFactory.createSequencingTemplateLaneType(
                    laneName, concentration, miSeqReagentKit.getLabel()));
        }
        return LimsQueryObjectFactory.createSequencingTemplate(null, null, isPoolTest,
                sequencingConfig.getInstrumentWorkflow().getValue(), sequencingConfig.getChemistry().getValue(),
                sequencingConfig.getReadStructure().getValue(),
                lanes.toArray(new SequencingTemplateLaneType[lanes.size()]));
    }

    private SequencingConfigDef getSequencingConfig(boolean isPoolTest) {
        WorkflowLoader workflowLoader = new WorkflowLoader();
        WorkflowConfig workflowConfig = workflowLoader.load();
        if (isPoolTest) {
            return workflowConfig.getSequencingConfigByName("Resequencing-Pool-Default");
        } else {
            return workflowConfig.getSequencingConfigByName("Resequencing-Production");
        }
    }
}
