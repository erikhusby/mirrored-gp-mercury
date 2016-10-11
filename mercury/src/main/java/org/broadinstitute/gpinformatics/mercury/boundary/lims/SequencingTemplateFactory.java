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
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FlowcellDesignationEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.MiSeqReagentKitDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
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

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

    public SequencingTemplateType getSequencingTemplate(LabBatch fctBatch, boolean poolTestDefault) {
        String sequencingTemplateName = null;
        if (fctBatch.getLabBatchType() != LabBatch.LabBatchType.FCT) {
            sequencingTemplateName = fctBatch.getBatchName();
        }

        // If it exists, uses a designation for obtaining flowcell parameters. All of the flowcell's
        // designations will have the same flowcell parameters (read length, number of cycles, etc).
        Integer readLength = null;
        BigDecimal loadingConcentration = null;
        boolean isPoolTest = poolTestDefault;
        Boolean isPairedEnd = null;
        List<FlowcellDesignation> designations = flowcellDesignationEjb.getFlowcellDesignations(fctBatch);
        if (fctBatch != null && !designations.isEmpty()) {
            FlowcellDesignation designation = designations.get(0);
            readLength = designation.getReadLength();
            loadingConcentration = designation.getLoadingConc();
            isPoolTest = designation.isPoolTest();
            isPairedEnd = designation.isPairedEndRead();
        }

        Set<LabBatchStartingVessel> startingFCTVessels = fctBatch.getLabBatchStartingVessels();
        List<SequencingTemplateLaneType> lanes = new ArrayList<>();
        Set<String> regulatoryDesignations = new HashSet<>();
        Set<Product> products = new HashSet<>();
        Set<Integer> productReadLengths = new HashSet<>();
        Set<Boolean> productPairedEnds = new HashSet<>();
        Set<String> molecularIndexReadStructures = new HashSet<>();

        for (LabBatchStartingVessel startingVessel : startingFCTVessels) {
            if (loadingConcentration == null) {
                loadingConcentration = startingVessel.getConcentration();
            }
            extractInfo(startingVessel.getLabVessel().getSampleInstancesV2(), regulatoryDesignations, products,
                    productReadLengths, productPairedEnds, molecularIndexReadStructures);

            if (startingVessel.getVesselPosition() != null) {
                SequencingTemplateLaneType lane = LimsQueryObjectFactory.createSequencingTemplateLaneType(
                        startingVessel.getVesselPosition().name(), loadingConcentration, "",
                        startingVessel.getLabVessel().getLabel());
                lanes.add(lane);
            } else {
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
                    lanes.add(lane);
                }
            }
        }

        validateCollections(regulatoryDesignations, products, molecularIndexReadStructures);
        // Provides defaults from product when there is no designation.
        if (readLength == null && productReadLengths.size() == 1) {
            readLength = productReadLengths.iterator().next();
        }
        if (isPairedEnd == null && productPairedEnds.size() == 1) {
            isPairedEnd = productPairedEnds.iterator().next();
        }
        SequencingConfigDef sequencingConfig = getSequencingConfig(isPoolTest);
        String readStructure =  makeReadStructure(readLength, isPoolTest, molecularIndexReadStructures, isPairedEnd);
        if (StringUtils.isBlank(readStructure)) {
            readStructure = sequencingConfig.getReadStructure().getValue();
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
                                                        boolean poolTestDefault) {

        // Finds the FCT for the flowcell.
        TransferTraverserCriteria.NearestLabBatchFinder batchCriteria =
                new TransferTraverserCriteria.NearestLabBatchFinder(LabBatch.LabBatchType.FCT,
                        TransferTraverserCriteria.NearestLabBatchFinder.AssociationType.DILUTION_VESSEL);
        flowcell.getContainerRole().applyCriteriaToAllPositions(batchCriteria,
                TransferTraverserCriteria.TraversalDirection.Ancestors);

        LabBatch fctBatch = CollectionUtils.isNotEmpty(batchCriteria.getAllLabBatches()) ?
                batchCriteria.getAllLabBatches().iterator().next() :
                (CollectionUtils.isNotEmpty(flowcell.getAllLabBatches(LabBatch.LabBatchType.FCT)) ?
                        flowcell.getAllLabBatches(LabBatch.LabBatchType.FCT).iterator().next() : null);

        // If it exists, uses a designation for obtaining flowcell parameters. All of the flowcell's
        // designations will have the same flowcell parameters (read length, number of cycles, etc).
        Integer readLength = null;
        BigDecimal loadingConcentration = null;
        boolean isPoolTest = poolTestDefault;
        Boolean isPairedEnd = null;
        List<FlowcellDesignation> designations = flowcellDesignationEjb.getFlowcellDesignations(fctBatch);
        if (fctBatch != null && !designations.isEmpty()) {
            FlowcellDesignation designation = designations.get(0);
            readLength = designation.getReadLength();
            loadingConcentration = designation.getLoadingConc();
            isPoolTest = designation.isPoolTest();
            isPairedEnd = designation.isPairedEndRead();
        }

        List<SequencingTemplateLaneType> lanes = new ArrayList<>();
        if (fctBatch != null) {
            // todo this could be replaced using the LabBatch.VesselToLanesInfo
            for (VesselAndPosition vesselAndPosition : loadedVesselsAndPositions) {
                LabVessel sourceVessel = vesselAndPosition.getVessel();
                VesselPosition vesselPosition = vesselAndPosition.getPosition();
                if (loadingConcentration == null) {
                    loadingConcentration = getLoadingConcentrationForVessel(sourceVessel);
                }
                SequencingTemplateLaneType lane = LimsQueryObjectFactory.createSequencingTemplateLaneType(
                        vesselPosition.name(), loadingConcentration, sourceVessel.getLabel(),
                        fctBatch.getStartingVesselByPosition(vesselPosition).getLabel());
                lanes.add(lane);
            }
        }

        if (lanes.isEmpty()) {
            // A flowcell having no FCT lab batch will still return a template with one lane on it
            // containing info from the sample instances.
            lanes.add(new SequencingTemplateLaneType());
        }

        Set<String> regulatoryDesignations = new HashSet<>();
        Set<Product> products = new HashSet<>();
        Set<Integer> productReadLengths = new HashSet<>();
        Set<Boolean> productPairedEnds = new HashSet<>();
        Set<String> molecularIndexReadStructures = new HashSet<>();

        extractInfo(flowcell.getSampleInstancesV2(), regulatoryDesignations, products,
                productReadLengths, productPairedEnds, molecularIndexReadStructures);

        validateCollections(regulatoryDesignations, products, molecularIndexReadStructures);
        // Provides defaults from product when there is no designation.
        if (readLength == null && productReadLengths.size() == 1) {
            readLength = productReadLengths.iterator().next();
        }
        if (isPairedEnd == null && productPairedEnds.size() == 1) {
            isPairedEnd = productPairedEnds.iterator().next();
        }
        SequencingConfigDef sequencingConfig = getSequencingConfig(isPoolTest);
        String readStructure =  makeReadStructure(readLength, isPoolTest, molecularIndexReadStructures, isPairedEnd);
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
    public SequencingTemplateType getSequencingTemplate(MiSeqReagentKit miSeqReagentKit, boolean poolTestDefault) {

        List<LabBatch> miseqBatches = new ArrayList<>(miSeqReagentKit.getAllLabBatches(LabBatch.LabBatchType.MISEQ));
        Set<LabVessel> loadingTubes = new HashSet<>();
        for (LabBatch batch : miseqBatches) {
            loadingTubes.add(batch.getStartingVesselByPosition(VesselPosition.LANE1));
        }
        if (loadingTubes.size() > 1) {
            throw new InformaticsServiceException(String.format("There are more than one MiSeq Batches " +
                                                                "associated with %s", miSeqReagentKit.getLabel()));
        }

        // Uses a designation for obtaining flowcell parameters. If there are multiple batches,
        // in absence of better information, uses the latest one that has a designation.
        List<FlowcellDesignation> flowcellDesignations = flowcellDesignationEjb.getFlowcellDesignations(loadingTubes);
        BigDecimal loadingConcentration = miSeqReagentKit.getConcentration();
        boolean isPoolTest = poolTestDefault;
        Boolean isPairedEnd = null;
        if (!flowcellDesignations.isEmpty()) {
            FlowcellDesignation designation = flowcellDesignations.iterator().next();
            loadingConcentration = designation.getLoadingConc();
            isPoolTest = designation.isPoolTest();
            isPairedEnd = designation.isPairedEndRead();
        }

        List<SequencingTemplateLaneType> lanes = new ArrayList<>();
        lanes.add(LimsQueryObjectFactory.createSequencingTemplateLaneType(
                IlluminaFlowcell.FlowcellType.MiSeqFlowcell.getVesselGeometry().getRowNames()[0],
                loadingConcentration, "", loadingTubes.iterator().next().getLabel()));

        SequencingConfigDef sequencingConfig = getSequencingConfig(isPoolTest);

        return LimsQueryObjectFactory.createSequencingTemplate(null, null, isPairedEnd,
                sequencingConfig.getInstrumentWorkflow().getValue(), sequencingConfig.getChemistry().getValue(),
                sequencingConfig.getReadStructure().getValue(),
                lanes.toArray(new SequencingTemplateLaneType[lanes.size()]));
    }

    /** Iterates on the sample instances and adds product defaults and the molecular indexes to the collections. */
    private void extractInfo(Set<SampleInstanceV2> sampleInstances,  Set<String> regulatoryDesignations,
                             Set<Product> products, Set<Integer> readLengths, Set<Boolean> pairedEnds,
                             Set<String> molecularIndexReadStructures) {

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
                    if (product.getReadLength() != null) {
                        readLengths.add(product.getReadLength());
                    }
                    if (product.getPairedEndRead() != null) {
                        pairedEnds.add(product.getPairedEndRead());
                    }
                }
                if (sampleInstance.getMolecularIndexingScheme() != null) {
                    String indexReadStructure = "";
                    for (MolecularIndex index : sampleInstance.getMolecularIndexingScheme().getIndexes().values()) {
                        indexReadStructure += index.getSequence().length() + "B";
                    }
                    molecularIndexReadStructures.add(indexReadStructure);
                }
            }
        }
    }

    private void validateCollections(Set<String> designations, Set<Product> products, Set<String> structures) {
        if (designations.isEmpty()) {
            throw new InformaticsServiceException("Could not find regulatory designation.");
        } else if(designations.contains(ResearchProject.RegulatoryDesignation.RESEARCH_ONLY.name())
                && (designations.contains(ResearchProject.RegulatoryDesignation.GENERAL_CLIA_CAP.name()) ||
                    designations.contains(ResearchProject.RegulatoryDesignation.CLINICAL_DIAGNOSTICS.name()))){
            throw new InformaticsServiceException("Template tube has mix of Research and Clinical regulatory designations");
        }
        if (products.isEmpty()) {
            throw new InformaticsServiceException("Could not find any products.");
        }
        if (structures.size() > 1) {
            throw new InformaticsServiceException("Found mix of different index lengths.");
        }
    }

    private String makeReadStructure(Integer readLength, boolean isPoolTest, Set<String> molecularIndexReadStructures,
                                     Boolean isPairedEnd) {
        String strandCode = (readLength != null && !isPoolTest) ? readLength.intValue() + "T" : "";
        String indexCode = molecularIndexReadStructures.isEmpty() ? "" : molecularIndexReadStructures.iterator().next();
        return strandCode + indexCode + ((isPairedEnd == null || isPairedEnd) ? strandCode : "");
    }

    private static SequencingConfigDef getSequencingConfig(boolean isPoolTest) {
        WorkflowLoader workflowLoader = new WorkflowLoader();
        WorkflowConfig workflowConfig = workflowLoader.load();
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
}
