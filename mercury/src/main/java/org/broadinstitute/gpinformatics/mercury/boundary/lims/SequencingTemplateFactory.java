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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.search.LabVesselSearchDefinition;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FlowcellDesignationEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.MiSeqReagentKitDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.UMIReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.UniqueMolecularIdentifier;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MiSeqReagentKit;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselAndPosition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.SequencingConfigDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ProductType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateLaneType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateType;

import javax.annotation.Nonnull;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Dependent
public class SequencingTemplateFactory {
    @Inject
    private IlluminaFlowcellDao illuminaFlowcellDao;
    @Inject
    private MiSeqReagentKitDao miSeqReagentKitDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private LabBatchEjb labBatchEjb;

    @Inject
    private FlowcellDesignationEjb flowcellDesignationEjb;

    private WorkflowConfig workflowConfig;

    /** Defines the type of entity to query. */
    public static enum QueryVesselType {
        FLOWCELL(LabVessel.ContainerType.FLOWCELL),
        TUBE(LabVessel.ContainerType.TUBE),
        STRIP_TUBE(LabVessel.ContainerType.STRIP_TUBE),
        MISEQ_REAGENT_KIT(LabVessel.ContainerType.MISEQ_REAGENT_KIT),
        FLOWCELL_TICKET(LabVessel.ContainerType.TUBE);

        private LabVessel.ContainerType value;

        QueryVesselType(LabVessel.ContainerType value) {
            this.value = value;
        }

        public LabVessel.ContainerType getValue() {
            return value;
        }
    }

    /**
     * Returns flowcell information for the identified entity.
     *
     * @param id   Either a flowcell barcode, a tube barcode, a strip tube barcode, or a miseq reagent kit.
     * @param queryVesselType Determines the entity type for the id.
     * @param poolTestDefault  Provides a default value for pool test. It gets overridden by flowcell designation.
     */
    public SequencingTemplateType fetchSequencingTemplate(String id, QueryVesselType queryVesselType,
                                                          boolean poolTestDefault) {
        switch (queryVesselType) {
        case FLOWCELL:
            IlluminaFlowcell illuminaFlowcell = illuminaFlowcellDao.findByBarcode(id);
            if (illuminaFlowcell == null) {
                throw new InformaticsServiceException(String.format("Flowcell '%s' was not found.", id));
            }
            Set<VesselAndPosition> loadedVesselsAndPositions = illuminaFlowcell.getLoadingVessels();
            return getSequencingTemplate(illuminaFlowcell, loadedVesselsAndPositions, poolTestDefault);

        case MISEQ_REAGENT_KIT:
            MiSeqReagentKit miSeqReagentKit = miSeqReagentKitDao.findByBarcode(id);
            if (miSeqReagentKit == null) {
                throw new InformaticsServiceException(String.format("MiSeq Reagent Kit '%s' was not found.", id));
            }
            return getSequencingTemplate(miSeqReagentKit, poolTestDefault);

        case TUBE:
            LabVessel dilutionTube = labVesselDao.findByIdentifier(id);
            if (dilutionTube == null) {
                throw new InformaticsServiceException(String.format("Tube '%s' was not found.", id));
            }
            return getSequencingTemplate(dilutionTube, poolTestDefault);

        case FLOWCELL_TICKET:
            LabBatch fctTicket = labBatchDao.findByBusinessKey(id);
            return getSequencingTemplate(fctTicket, poolTestDefault);

        default:
            throw new InformaticsServiceException(
                    String.format("Sequencing template unavailable for %s.", queryVesselType));
        }
    }

    /**
     * This method builds a sequencing template object given a denature or dilution tube.
     *
     * @param templateTargetTube The Dilution tube to create the sequencing template for.
     * @param isMiSeq   Determines the type of flowcell lab batch to expect for this tube.
     *                  It also determines whether to look for pool test designations.
     *
     * @return Returns a populated sequencing template.
     */
    public SequencingTemplateType getSequencingTemplate(LabVessel templateTargetTube, boolean isMiSeq) {
        Collection<LabBatch> labBatches = new HashSet<>();

        Set<LabBatchStartingVessel> batchReferences = templateTargetTube.getDilutionReferences();
        if (batchReferences.isEmpty()) {
           labBatches = templateTargetTube.getAllLabBatches(
                   isMiSeq ? LabBatch.LabBatchType.MISEQ : LabBatch.LabBatchType.FCT);
        } else {
            for (LabBatchStartingVessel reference : batchReferences) {
                labBatches.add(reference.getLabBatch());
            }
        }
        if (labBatches.size() > 1 && !isMiSeq) {
            throw new InformaticsServiceException("Found more than one FCT batch for denature tube.");
        }
        if (labBatches.size() == 0) {
            throw new InformaticsServiceException(
                    "Could not find FCT batch for tube " + templateTargetTube.getLabel() + ".");
        }

        LabBatch fctBatch = labBatches.iterator().next();
        return getSequencingTemplate(fctBatch, isMiSeq);
    }

    public List<LabBatch> fetchFlowcellTicketForLabBatch(LabVessel labVessel) {
        List<LabBatch> results = new ArrayList<>();

        LabVesselSearchDefinition.VesselBatchTraverserCriteria downstreamBatchFinder =
                new LabVesselSearchDefinition.VesselBatchTraverserCriteria();
        if( labVessel.getContainerRole() != null ) {
            labVessel.getContainerRole().applyCriteriaToAllPositions(
                    downstreamBatchFinder, TransferTraverserCriteria.TraversalDirection.Ancestors);
        } else {
            labVessel.evaluateCriteria(
                    downstreamBatchFinder, TransferTraverserCriteria.TraversalDirection.Ancestors);
        }

        for ( LabBatch labBatch : downstreamBatchFinder.getLabBatches() ) {
            if( labBatch.getLabBatchType() == LabBatch.LabBatchType.FCT
                || labBatch.getLabBatchType() == LabBatch.LabBatchType.MISEQ ) {
                results.add(labBatch);
            }
        }
        return results;
    }

    public SequencingTemplateType getSequencingTemplate(LabBatch fctBatch, boolean isPoolTest) {
        String sequencingTemplateName = null;
        if (fctBatch.getLabBatchType() != LabBatch.LabBatchType.FCT) {
            sequencingTemplateName = fctBatch.getBatchName();
        }

        // If it exists, uses a designation for obtaining flowcell parameters.
        // Uses the most recent designation for each tube after filtering for pool test.
        Map<LabVessel, FlowcellDesignation> flowcellDesignationMap = findDesignations(fctBatch, isPoolTest);

        Set<LabBatchStartingVessel> startingFCTVessels = fctBatch.getLabBatchStartingVessels();
        List<SequencingTemplateLaneType> lanes = new ArrayList<>();
        Set<String> regulatoryDesignations = new HashSet<>();
        Set<Product> products = new HashSet<>();
        Set<Integer> readLengths = new HashSet<>();
        Set<Boolean> pairedEnds = new HashSet<>();
        Set<String> molecularIndexReadStructures = new HashSet<>();
        Map<String, Set<UniqueMolecularIdentifier>> mapLaneToUmi = new HashMap<>();
        BigDecimal loadingConcentration = null;
        for (LabBatchStartingVessel startingVessel : startingFCTVessels) {
            Set<UniqueMolecularIdentifier> umiReagents = new HashSet<>();
            FlowcellDesignation flowcellDesignation = flowcellDesignationMap.get(startingVessel.getLabVessel());
            extractInfo(startingVessel.getLabVessel().getSampleInstancesV2(), flowcellDesignation,
                    regulatoryDesignations, products, readLengths, pairedEnds,
                    molecularIndexReadStructures, umiReagents);
            if (umiReagents.size() > 2) {
                throw new InformaticsServiceException("More than two UMI Reagent found for lab batch " +
                                                      startingVessel.getLabBatch().getBatchName());
            }

            if (startingVessel.getVesselPosition() != null) {
                if (flowcellDesignation == null) {
                    loadingConcentration = startingVessel.getConcentration();
                } else {
                    loadingConcentration = flowcellDesignation.getLoadingConc();
                }
                SequencingTemplateLaneType lane = LimsQueryObjectFactory.createSequencingTemplateLaneType(
                        startingVessel.getVesselPosition().name(), loadingConcentration, "",
                        startingVessel.getLabVessel().getLabel());
                //TODO Assert umiReagents at most == 1
                mapLaneToUmi.put(lane.getLaneName(), umiReagents);
                lanes.add(lane);
            } else {
                if (loadingConcentration == null) {
                    loadingConcentration = startingVessel.getConcentration();
                }
                if (startingFCTVessels.size() != 1) {
                    throw new InformaticsServiceException(
                            String.format("More than one starting denature tube for FCT ticket %s",
                                    fctBatch.getBatchName()));
                }
                Iterator<String> positionNames;
                if (fctBatch.getFlowcellType() != null) {
                    positionNames = fctBatch.getFlowcellType().getVesselGeometry().getPositionNames();
                } else if (isPoolTest) {
                    positionNames = IlluminaFlowcell.FlowcellType.MiSeqFlowcell.getVesselGeometry().getPositionNames();
                } else {
                    positionNames =
                            IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell.getVesselGeometry().getPositionNames();
                }
                while (positionNames.hasNext()) {
                    String vesselPosition = positionNames.next();
                    SequencingTemplateLaneType lane =
                            LimsQueryObjectFactory.createSequencingTemplateLaneType(vesselPosition,
                                    loadingConcentration, "",
                                    startingVessel.getLabVessel().getLabel());
                    mapLaneToUmi.put(lane.getLaneName(), umiReagents);
                    lanes.add(lane);
                }
            }
        }

        validateCollections(regulatoryDesignations, products, molecularIndexReadStructures);
        int readLength = readLengths.size() == 1 ? readLengths.iterator().next() : 0;
        // Paired end read should default to true.
        boolean isPairedEnd = pairedEnds.size() != 1 || Boolean.TRUE.equals(pairedEnds.iterator().next());
        SequencingConfigDef sequencingConfig = getSequencingConfig(isPoolTest);
        String readStructure =  makeReadStructure(readLength, isPoolTest, molecularIndexReadStructures, isPairedEnd, null);
        if (StringUtils.isBlank(readStructure)) {
            readStructure = sequencingConfig.getReadStructure().getValue();
        }

        for (SequencingTemplateLaneType laneType: lanes) {
            Set<UniqueMolecularIdentifier> umiReagentSet = mapLaneToUmi.get(laneType.getLaneName());
            String laneReadStructure = makeReadStructure(readLength, isPoolTest, molecularIndexReadStructures,
                    isPairedEnd, umiReagentSet);
            laneType.setReadStructure(laneReadStructure);
        }

        SequencingTemplateType sequencingTemplate = LimsQueryObjectFactory.createSequencingTemplate(
                sequencingTemplateName, null, isPairedEnd, sequencingConfig.getInstrumentWorkflow().getValue(),
                sequencingConfig.getChemistry().getValue(), readStructure);
        sequencingTemplate.getProducts().addAll(makeProductTypes(products));
        if (loadingConcentration != null) {
            sequencingTemplate.setConcentration(loadingConcentration);
        }
        sequencingTemplate.getRegulatoryDesignation().addAll(regulatoryDesignations);
        sequencingTemplate.getLanes().addAll(lanes);
        return sequencingTemplate;
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

        // Finds the FCT for the flowcell.
        LabBatch fctBatch = null;
        List<LabBatch> labBatches = fetchFlowcellTicketForLabBatch(flowcell);
        if (labBatches.isEmpty()) {
            TransferTraverserCriteria.NearestLabBatchFinder batchCriteria =
                    new TransferTraverserCriteria.NearestLabBatchFinder(LabBatch.LabBatchType.FCT,
                            TransferTraverserCriteria.NearestLabBatchFinder.AssociationType.DILUTION_VESSEL);
            flowcell.getContainerRole().applyCriteriaToAllPositions(batchCriteria,
                    TransferTraverserCriteria.TraversalDirection.Ancestors);

            fctBatch = CollectionUtils.isNotEmpty(batchCriteria.getAllLabBatches()) ?
                    batchCriteria.getAllLabBatches().iterator().next() :
                    (CollectionUtils.isNotEmpty(flowcell.getAllLabBatches(LabBatch.LabBatchType.FCT)) ?
                            flowcell.getAllLabBatches(LabBatch.LabBatchType.FCT).iterator().next() : null);
        } else {
            fctBatch = labBatches.get(0);
        }

        Set<String> regulatoryDesignations = new HashSet<>();
        Set<Product> products = new HashSet<>();
        Set<Integer> readLengths = new HashSet<>();
        Set<Boolean> pairedEnds = new HashSet<>();
        Set<String> molecularIndexReadStructures = new HashSet<>();
        Map<String, Set<UniqueMolecularIdentifier>> mapLaneToUmi = new HashMap<>();
        Map<LabVessel, FlowcellDesignation> flowcellDesignationMap = Collections.emptyMap();
        if (fctBatch != null) {
            // Uses flowcell designations for the flowcell parameters and loading concentration.
            // Finds the most recent designation for each tube after filtering for pool test.
            flowcellDesignationMap = findDesignations(fctBatch, isPoolTest);
            for (LabBatchStartingVessel startingVessel : fctBatch.getLabBatchStartingVessels()) {
                Set<UniqueMolecularIdentifier> umiReagents = new HashSet<>();
                extractInfo(startingVessel.getLabVessel().getSampleInstancesV2(),
                        flowcellDesignationMap.get(startingVessel.getLabVessel()), regulatoryDesignations,
                        products, readLengths, pairedEnds, molecularIndexReadStructures, umiReagents);
                if (umiReagents.size() > 1) {
                    //Check if they are in the same position
                    Set<UniqueMolecularIdentifier.UMILocation> umiLocations = new HashSet<>();
                    for (UniqueMolecularIdentifier umiReagent: umiReagents) {
                        if (!umiLocations.add(umiReagent.getLocation())) {
                            throw new InformaticsServiceException(
                                    "More than one UMI Reagent found at same location for lab batch " +
                                    startingVessel.getLabBatch().getBatchName());
                        }
                    }
                }
                if (startingVessel.getVesselPosition() != null) {
                    mapLaneToUmi.put(startingVessel.getVesselPosition().name(), umiReagents);
                }
            }
        } else {
            Set<UniqueMolecularIdentifier> umiReagents = new HashSet<>();
            extractInfo(flowcell.getSampleInstancesV2(), null, regulatoryDesignations,
                    products, readLengths, pairedEnds, molecularIndexReadStructures, umiReagents);
        }

        validateCollections(regulatoryDesignations, products, molecularIndexReadStructures);
        int readLength = readLengths.size() == 1 ? readLengths.iterator().next() : 0;
        // Paired end read should default to true.
        boolean isPairedEnd = pairedEnds.size() != 1 || Boolean.TRUE.equals(pairedEnds.iterator().next());

        List<SequencingTemplateLaneType> lanes = new ArrayList<>();
        BigDecimal loadingConcentration = null;
        if (fctBatch != null) {
            // todo this could be replaced using the LabBatch.VesselToLanesInfo
            for (VesselAndPosition vesselAndPosition : loadedVesselsAndPositions) {
                LabVessel sourceVessel = vesselAndPosition.getVessel();
                VesselPosition vesselPosition = vesselAndPosition.getPosition();
                LabVessel startingVessel = fctBatch.getStartingVesselByPosition(vesselPosition);
                if (flowcellDesignationMap.get(startingVessel) != null) {
                    loadingConcentration = flowcellDesignationMap.get(startingVessel).getLoadingConc();
                }
                if (loadingConcentration == null) {
                    loadingConcentration = getLoadingConcentrationForVessel(sourceVessel);
                }
                SequencingTemplateLaneType lane = LimsQueryObjectFactory.createSequencingTemplateLaneType(
                        vesselPosition.name(), loadingConcentration, sourceVessel.getLabel(),
                        startingVessel.getLabel());
                lanes.add(lane);
            }
        }

        if (lanes.isEmpty()) {
            // A flowcell having no FCT lab batch will still return a template with one lane on it
            // containing info from the sample instances.
            lanes.add(new SequencingTemplateLaneType());
        }

        for (SequencingTemplateLaneType laneType: lanes) {
            Set<UniqueMolecularIdentifier> umiReagentSet = mapLaneToUmi.get(laneType.getLaneName());
            if (umiReagentSet != null) {
                String laneReadStructure = makeReadStructure(readLength, isPoolTest, molecularIndexReadStructures,
                        isPairedEnd, umiReagentSet);
                laneType.setReadStructure(laneReadStructure);
            }
        }

        SequencingConfigDef sequencingConfig = getSequencingConfig(isPoolTest);
        String readStructure =  makeReadStructure(readLength, isPoolTest, molecularIndexReadStructures, isPairedEnd, null);
        if (StringUtils.isBlank(readStructure)) {
            readStructure = sequencingConfig.getReadStructure().getValue();
        }

        String sequencingTemplateName = fctBatch == null ? null : fctBatch.getBatchName();
        SequencingTemplateType sequencingTemplate = LimsQueryObjectFactory.createSequencingTemplate(
                sequencingTemplateName, flowcell.getLabel(), isPairedEnd,
                sequencingConfig.getInstrumentWorkflow().getValue(), sequencingConfig.getChemistry().getValue(),
                readStructure);
        sequencingTemplate.getLanes().addAll(lanes);
        sequencingTemplate.getProducts().addAll(makeProductTypes(products));
        sequencingTemplate.getRegulatoryDesignation().addAll(regulatoryDesignations);
        return sequencingTemplate;
    }

    private List<ProductType> makeProductTypes(Set<Product> products) {
        List<String> names = new ArrayList<>();
        for (Product product : products) {
            names.add(product.getName());
        }
        Collections.sort(names);
        List<ProductType> productTypes = new ArrayList<>();
        for (String name : names) {
            ProductType productType = new ProductType();
            productType.setName(name);
            productTypes.add(productType);
        }
        return productTypes;
    }


    /**
     * Gets the loading concentration from any neighboring vessel in the batch.
     *
     * @param sourceVessel The vessel whose FCT batch is examined.
     * @return The loading concentration for a vessel in FCT batch.
     */
    private BigDecimal getLoadingConcentrationForVessel(LabVessel sourceVessel) {
        for (LabBatch batch : sourceVessel.getAllLabBatches(LabBatch.LabBatchType.FCT)) {
            for (LabBatchStartingVessel labBatchStartingVessel : batch.getLabBatchStartingVessels()) {
                if (labBatchStartingVessel.getConcentration() != null) {
                    return labBatchStartingVessel.getConcentration();
                }
            }
        }
        return null;
    }

    /**
     * Populate a the sequencing template with lanes. When it's not a pool test the returned read structure
     * is a canned value instead of one computed from sample instances from the Miseq batch.
     *
     * @param miSeqReagentKit the reagentKit we are querying.
     *
     * @return a populated Sequencing template
     */
    @DaoFree
    public SequencingTemplateType getSequencingTemplate(MiSeqReagentKit miSeqReagentKit, boolean isPoolTest) {

        List<LabBatch> miseqBatches = new ArrayList<>(miSeqReagentKit.getAllLabBatches(LabBatch.LabBatchType.MISEQ));
        Set<LabVessel> startingTubes = new HashSet<>();
        for (LabBatch batch : miseqBatches) {
            startingTubes.add(batch.getStartingVesselByPosition(VesselPosition.LANE1));
        }
        if (startingTubes.size() > 1) {
            throw new InformaticsServiceException(String.format("There are more than one MiSeq Batches " +
                                                                "associated with %s", miSeqReagentKit.getLabel()));
        }
        // Finds any flowcell designation on the fct batch.
        FlowcellDesignation flowcellDesignation = findDesignations(miseqBatches.get(0), isPoolTest).values().
                stream().findFirst().orElse(null);
        BigDecimal loadingConcentration;
        Boolean isPairedEnd;
        if (flowcellDesignation != null) {
            loadingConcentration = flowcellDesignation.getLoadingConc();
            isPairedEnd = flowcellDesignation.isPairedEndRead();
        } else {
            loadingConcentration = miSeqReagentKit.getConcentration();
            isPairedEnd = true;
        }

        List<SequencingTemplateLaneType> lanes = new ArrayList<>();
        lanes.add(LimsQueryObjectFactory.createSequencingTemplateLaneType(
                IlluminaFlowcell.FlowcellType.MiSeqFlowcell.getVesselGeometry().getRowNames()[0],
                loadingConcentration, "", startingTubes.iterator().next().getLabel()));

        SequencingConfigDef sequencingConfig = getSequencingConfig(isPoolTest);

        return LimsQueryObjectFactory.createSequencingTemplate(null, null, isPairedEnd,
                sequencingConfig.getInstrumentWorkflow().getValue(), sequencingConfig.getChemistry().getValue(),
                sequencingConfig.getReadStructure().getValue(),
                lanes.toArray(new SequencingTemplateLaneType[lanes.size()]));
    }

    /** Iterates on the sample instances and adds product defaults and the molecular indexes to the collections. */
    private void extractInfo(Set<SampleInstanceV2> sampleInstances, FlowcellDesignation flowcellDesignation,
            Set<String> regulatoryDesignations, Set<Product> products, Set<Integer> readLengths, Set<Boolean> pairedEnds,
            Set<String> molecularIndexReadStructures, Set<UniqueMolecularIdentifier> umiReagents) {

        for(SampleInstanceV2 sampleInstance: sampleInstances) {
            // Controls don't have bucket entries, but assumes that the non-control samples will be present.
            if (sampleInstance.getSingleBucketEntry() != null) {
                ProductOrder productOrder = sampleInstance.getSingleBucketEntry().getProductOrder();
                if (StringUtils.isNotBlank(productOrder.getResearchProject().getRegulatoryDesignation().name())) {
                    regulatoryDesignations.add(productOrder.getResearchProject().getRegulatoryDesignation().name());
                }
                Product product = productOrder.getProduct();
                if (product != null) {
                    products.add(product);
                }
                // For read length and paired end first look to a flowcell designation,
                // then the external sample upload, finally the product.
                Integer readLength = (flowcellDesignation != null && flowcellDesignation.getReadLength() != null) ?
                        flowcellDesignation.getReadLength() : (sampleInstance.getReadLength1() != null ?
                                sampleInstance.getReadLength1() : product.getReadLength());
                if (readLength != null) {
                    readLengths.add(readLength);
                }
                Boolean pairedEnd = flowcellDesignation != null ? flowcellDesignation.isPairedEndRead() :
                        sampleInstance.getPairedEndRead() != null ? sampleInstance.getPairedEndRead() :
                                product.getPairedEndRead();
                if (pairedEnd != null) {
                    pairedEnds.add(pairedEnd);
                }
                // For molecular indexes first look to the sample instance, then flowcell designation,
                // then external upload.
                if (sampleInstance.getMolecularIndexingScheme() != null) {
                    String indexReadStructure = "";
                    for (MolecularIndex index : sampleInstance.getMolecularIndexingScheme().getIndexes().values()) {
                        indexReadStructure += index.getSequence().length() + "B";
                    }
                    molecularIndexReadStructures.add(indexReadStructure);
                } else {
                    FlowcellDesignation.IndexType indexType = flowcellDesignation != null ?
                            flowcellDesignation.getIndexType() : sampleInstance.getIndexType() != null ?
                            sampleInstance.getIndexType() : FlowcellDesignation.IndexType.NONE;
                    switch (indexType) {
                    case DUAL:
                        molecularIndexReadStructures.add(String.format("%dB%dB",
                                sampleInstance.getIndexLength1() != null ? sampleInstance.getIndexLength1() : 8,
                                sampleInstance.getIndexLength1() != null ? sampleInstance.getIndexLength1() : 8));
                        break;
                    case SINGLE:
                        molecularIndexReadStructures.add(String.format("%dB",
                                sampleInstance.getIndexLength1() != null ? sampleInstance.getIndexLength1() : 8));
                        break;
                    case NONE:
                        break;
                    }
                }
                // Gets UMIs from sampleInstance.
                for (Reagent reagent : sampleInstance.getReagents()) {
                    if (OrmUtil.proxySafeIsInstance(reagent, UMIReagent.class)) {
                        UMIReagent umiReagent = OrmUtil.proxySafeCast(reagent, UMIReagent.class);
                        umiReagents.add(umiReagent.getUniqueMolecularIdentifier());
                    }
                }
            }
        }

        // If mix of single and dual index, then choose dual index read structure
        Optional<String> dualIndexReadStructure = molecularIndexReadStructures.stream().
                filter(s -> s.split("B").length == 2).findFirst();
        if (dualIndexReadStructure.isPresent()) {
            molecularIndexReadStructures.clear();
            molecularIndexReadStructures.add(dualIndexReadStructure.get());
        }
    }

    private void validateCollections(Set<String> designations, Set<Product> products, Set<String> structures) {
        if (designations.isEmpty()) {
            throw new InformaticsServiceException("Could not find regulatory designation.");
        }
        if (products.isEmpty()) {
            throw new InformaticsServiceException("Could not find any products.");
        }
        boolean mixedFlowcellOk = false;
        for (Product product : products) {
            if (Objects.equals(product.getAggregationDataType(), Aggregation.DATA_TYPE_WGS)) {
                mixedFlowcellOk = true;
                break;
            }
        }
        if(!mixedFlowcellOk &&
                designations.contains(ResearchProject.RegulatoryDesignation.RESEARCH_ONLY.name())
                && (designations.contains(ResearchProject.RegulatoryDesignation.GENERAL_CLIA_CAP.name()) ||
                    designations.contains(ResearchProject.RegulatoryDesignation.CLINICAL_DIAGNOSTICS.name()))){
            throw new InformaticsServiceException("Template tube has mix of Research and Clinical regulatory designations");
        }
    }

    private String makeReadStructure(Integer readLength, boolean isPoolTest, Set<String> molecularIndexReadStructures,
                                     @Nonnull Boolean isPairedEnd, Set<UniqueMolecularIdentifier> umiReagents) {
        String strandCode = (readLength != null && !isPoolTest) ? readLength.intValue() + "T" : "";
        String indexCode = molecularIndexReadStructures.isEmpty() ? "" : molecularIndexReadStructures.iterator().next();

        if (umiReagents == null || umiReagents.isEmpty()) {
            return strandCode + indexCode + (isPairedEnd  ? strandCode : "");
        }

        List<UniqueMolecularIdentifier> umiReagentsList = new ArrayList<>(umiReagents);
        Collections.sort(umiReagentsList, new Comparator<UniqueMolecularIdentifier>() {
            @Override
            public int compare(UniqueMolecularIdentifier umi1, UniqueMolecularIdentifier umi2) {
                return umi1.getLocation().compareTo(umi2.getLocation());
            }
        });
        Map<UniqueMolecularIdentifier.UMILocation, UniqueMolecularIdentifier> umiLocationUMIReagentMap = new HashMap<>();
        for (UniqueMolecularIdentifier umiReagent: umiReagentsList) {
            umiLocationUMIReagentMap.put(umiReagent.getLocation(), umiReagent);
        }

        String readStructure = "";
        long readLengthDifference = 0;
        if (umiLocationUMIReagentMap.containsKey(UniqueMolecularIdentifier.UMILocation.BEFORE_FIRST_READ)) {
            UniqueMolecularIdentifier umiReagent = umiLocationUMIReagentMap.get(UniqueMolecularIdentifier.UMILocation.BEFORE_FIRST_READ);
            readStructure = umiReagent.getLength() + "M" + umiReagent.getSpacerLength() + "S";
            readLengthDifference = umiReagent.getLength() + umiReagent.getSpacerLength();
            strandCode = (readLength != null && !isPoolTest) ? readLength.intValue() - readLengthDifference  + "T" : "";
        }
        if (umiLocationUMIReagentMap.containsKey(UniqueMolecularIdentifier.UMILocation.INLINE_FIRST_READ)) {
            UniqueMolecularIdentifier umiReagent = umiLocationUMIReagentMap.get(UniqueMolecularIdentifier.UMILocation.INLINE_FIRST_READ);
            readStructure = readStructure + umiReagent.getLength() + "M" + umiReagent.getSpacerLength() + "S";
            readLengthDifference = umiReagent.getLength() + umiReagent.getSpacerLength();
            strandCode = (readLength != null && !isPoolTest) ? readLength.intValue() - readLengthDifference  + "T" : "";
        }
        readStructure = readStructure + strandCode; //Now Either 8M76T ot 76T
        if (umiLocationUMIReagentMap.containsKey(UniqueMolecularIdentifier.UMILocation.BEFORE_FIRST_INDEX_READ)) {
            UniqueMolecularIdentifier umiReagent = umiLocationUMIReagentMap.get(UniqueMolecularIdentifier.UMILocation.BEFORE_FIRST_INDEX_READ);
            readStructure = readStructure + umiReagent.getLength() + "M" + umiReagent.getSpacerLength() + "S";
        }

        //Add first index if any
        boolean dualIndex = indexCode.matches("^\\d+B\\d+B$");
        boolean hasIndex = indexCode.matches(".*\\d+B.*");
        if (hasIndex) {
            if (dualIndex) {
                String indexPattern = (dualIndex) ? "(\\d+B)(\\d+B)" : "(\\d+B)";
                Pattern r = Pattern.compile(indexPattern);
                Matcher m = r.matcher(indexCode);
                if (m.find()) {
                    if (m.groupCount() == 2) {
                        indexCode = m.group(1);
                        readStructure += indexCode;
                        if (umiLocationUMIReagentMap.containsKey(UniqueMolecularIdentifier.UMILocation.BEFORE_SECOND_INDEX_READ)) {
                            UniqueMolecularIdentifier umiReagent =
                                    umiLocationUMIReagentMap.get(UniqueMolecularIdentifier.UMILocation.BEFORE_SECOND_INDEX_READ);
                            readStructure += umiReagent.getLength() + "M" + umiReagent.getSpacerLength() + "S";
                        }
                        readStructure += indexCode;
                    } else {
                        throw new RuntimeException("Failed to parse index length from " + indexCode);
                    }
                } else {
                    throw new RuntimeException("Failed to find index code when expected from " + indexCode);
                }
            } else {
                readStructure += indexCode;
            }
        }

        // reset strand code for the second read
        strandCode = (readLength != null && !isPoolTest) ? readLength.intValue() + "T" : "";
        if (umiLocationUMIReagentMap.containsKey(UniqueMolecularIdentifier.UMILocation.BEFORE_SECOND_READ)) {
            UniqueMolecularIdentifier umiReagent = umiLocationUMIReagentMap.get(UniqueMolecularIdentifier.UMILocation.BEFORE_SECOND_READ);
            readLengthDifference = umiReagent.getLength() + umiReagent.getSpacerLength();
            strandCode = (readLength != null && !isPoolTest) ? readLength.intValue() - readLengthDifference  + "T" : "";
            readStructure = readStructure + umiReagent.getLength() + "M" + umiReagent.getSpacerLength() + "S";
        }

        if (umiLocationUMIReagentMap.containsKey(UniqueMolecularIdentifier.UMILocation.INLINE_SECOND_READ)) {
            UniqueMolecularIdentifier umiReagent = umiLocationUMIReagentMap.get(UniqueMolecularIdentifier.UMILocation.INLINE_SECOND_READ);
            readLengthDifference = umiReagent.getLength() + umiReagent.getSpacerLength();
            strandCode = (readLength != null && !isPoolTest) ? readLength.intValue() - readLengthDifference  + "T" : "";
            readStructure = readStructure + (isPairedEnd ? strandCode : "");
            readStructure = readStructure + umiReagent.getLength() + "M" + umiReagent.getSpacerLength() + "S";
        } else {
            readStructure = readStructure + (isPairedEnd ? strandCode : "");
        }
        return readStructure;
    }

    private SequencingConfigDef getSequencingConfig(boolean isPoolTest) {
        if (isPoolTest) {
            return workflowConfig.getSequencingConfigByName("Resequencing-Pool-Default");
        } else {
            return workflowConfig.getSequencingConfigByName("Resequencing-Production");
        }
    }

    /** Use only for dao-free testing. */
    public void setFlowcellDesignationEjb(FlowcellDesignationEjb testEjb) {
        flowcellDesignationEjb = testEjb;
    }

    @Inject
    public void setWorkflowConfig(WorkflowConfig workflowConfig) {
        this.workflowConfig = workflowConfig;
    }

    /** Matches designations with the pool test flag and finds most recent designation for each starting tube. */
    private Map<LabVessel, FlowcellDesignation> findDesignations(LabBatch fctBatch, boolean isPoolTest) {
        return flowcellDesignationEjb.getFlowcellDesignations(fctBatch).stream().
                filter(designation -> designation.isPoolTest() == isPoolTest).
                collect(Collectors.toMap(FlowcellDesignation::getStartingTube, Function.identity(), (fd1, fd2) -> fd2));
    }

}
