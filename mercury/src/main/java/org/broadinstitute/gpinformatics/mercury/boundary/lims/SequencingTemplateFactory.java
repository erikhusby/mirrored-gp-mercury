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

import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.MiSeqReagentKitDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
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

    /**
     * What you will be searching for with the ID parameter in fetchSequencingTemplate.
     * Yes, this is an enum of enums. Having a unique enum which was basically a subset of
     * LabVessel.ContainerType seemed creepy.
     * <p/>
     * FCT Ticket names are not yet supported
     */
    public enum QueryVesselType {
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
                throw new InformaticsServiceException(String.format("Flowcell '%s' was not found.", id));
            }
            loadedVesselsAndPositions = illuminaFlowcell.getLoadingVessels();
            return getSequencingTemplate(illuminaFlowcell, loadedVesselsAndPositions, isPoolTest);
        case MISEQ_REAGENT_KIT:
            MiSeqReagentKit miSeqReagentKit = miSeqReagentKitDao.findByBarcode(id);
            if (miSeqReagentKit == null) {
                throw new InformaticsServiceException(String.format("MiSeq Reagent Kit '%s' was not found.", id));
            }
            return getSequencingTemplate(miSeqReagentKit, isPoolTest);
        case TUBE:
            LabVessel dilutionTube = labVesselDao.findByIdentifier(id);
            if (dilutionTube == null) {
                throw new InformaticsServiceException(String.format("Tube '%s' was not found.", id));
            }
            return getSequencingTemplate(dilutionTube, isPoolTest);

        case FLOWCELL_TICKET:
            LabBatch fctTicket = labBatchDao.findByBusinessKey(id);
            return getSequencingTemplate(fctTicket, isPoolTest);
        // Don't support the following for now, so fall through and throw exception.
        case STRIP_TUBE:
        default:
            throw new InformaticsServiceException(
                    String.format("Sequencing template unavailable for %s.", queryVesselType));
        }
    }

    /**
     * This method builds a sequencing template object given a denature or dilution tube.
     *
     * @param templateTargetTube The Dilution tube to create the sequencing template for.
     * @param isPoolTest         A boolean to determine if this is a MiSeq pool test run or not.
     *
     * @return Returns a populated sequencing template.
     */
    public SequencingTemplateType getSequencingTemplate(LabVessel templateTargetTube, boolean isPoolTest) {
        SequencingConfigDef sequencingConfig = getSequencingConfig(isPoolTest);

        Set<LabBatchStartingVessel> batchReferences;
        Collection<LabBatch> labBatches = new HashSet<>();

        batchReferences = templateTargetTube.getDilutionReferences();
        if (batchReferences.isEmpty()) {
            if (isPoolTest) {
                labBatches = templateTargetTube.getAllLabBatches(LabBatch.LabBatchType.MISEQ);
            } else {
                labBatches = templateTargetTube.getAllLabBatches(LabBatch.LabBatchType.FCT);
            }
        } else {
            for (LabBatchStartingVessel reference : batchReferences) {
                labBatches.add(reference.getLabBatch());
            }
        }
        if (labBatches.size() > 1 && !isPoolTest) {
            throw new InformaticsServiceException("Found more than one FCT batch for denature tube.");
        }
        if (!labBatches.isEmpty()) {
            LabBatch fctBatch = labBatches.iterator().next();
            return getSequencingTemplateByLabBatch(sequencingConfig, fctBatch, isPoolTest);
        } else {
            throw new InformaticsServiceException(
                    "Could not find FCT batch for tube " + templateTargetTube.getLabel() + ".");
        }
    }

    public SequencingTemplateType getSequencingTemplate(LabBatch labBatch, boolean isPoolTest) {
        SequencingConfigDef sequencingConfig = getSequencingConfig(isPoolTest);

        return getSequencingTemplateByLabBatch(sequencingConfig, labBatch, isPoolTest);
    }

    private SequencingTemplateType getSequencingTemplateByLabBatch(SequencingConfigDef sequencingConfig,
                                                                   LabBatch fctBatch, boolean isPoolTest) {
        String sequencingTemplateName = null;
        if (fctBatch.getLabBatchType() != LabBatch.LabBatchType.FCT) {
            sequencingTemplateName = fctBatch.getBatchName();
        }

        Set<LabBatchStartingVessel> startingFCTVessels = fctBatch.getLabBatchStartingVessels();
        List<SequencingTemplateLaneType> lanes = new ArrayList<>();
        Set<String> regulatoryDesignations = new HashSet<>();
        Set<String> productNames = new HashSet<>();
        List<ProductType> productTypes = new ArrayList<>();
        Set<String> readStructures = new HashSet<>();
        BigDecimal concentration = null;
        for (LabBatchStartingVessel startingVessel: startingFCTVessels) {
            concentration = startingVessel.getConcentration();
            Set<SampleInstanceV2> sampleInstances = startingVessel.getLabVessel().getSampleInstancesV2();
            attachRegulatoryDesignationAndProductOrder(sampleInstances, regulatoryDesignations, productNames,
                    productTypes, readStructures, isPoolTest);
            if (startingVessel.getVesselPosition() != null) {
                SequencingTemplateLaneType lane =
                        LimsQueryObjectFactory.createSequencingTemplateLaneType(
                                startingVessel.getVesselPosition().name(),
                                concentration, "",
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
                                    concentration, "",
                                    startingVessel.getLabVessel().getLabel());
                    lanes.add(lane); 
                }
            }
        }
        if (readStructures.size() > 1) {
            throw new InformaticsServiceException("Mixture of readStructures: " + readStructures);
        }
        String readStructure = readStructures.size() == 1 ? readStructures.iterator().next() :
                sequencingConfig.getReadStructure().getValue();
        SequencingTemplateType sequencingTemplate = LimsQueryObjectFactory.createSequencingTemplate(
                sequencingTemplateName, null, isPoolTest, sequencingConfig.getInstrumentWorkflow().getValue(),
                sequencingConfig.getChemistry().getValue(), readStructure);
        sequencingTemplate.getProducts().addAll(productTypes);
        if (concentration != null) {
            sequencingTemplate.setConcentration(concentration);
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

        /** FIXME: Temporary Hack for Exome Express until we get lane to starting vessel tracking **/
        List<SequencingTemplateLaneType> lanes = new ArrayList<>();

        TransferTraverserCriteria.NearestLabBatchFinder batchCriteria =
                new TransferTraverserCriteria.NearestLabBatchFinder(LabBatch.LabBatchType.FCT,
                        TransferTraverserCriteria.NearestLabBatchFinder.AssociationType.DILUTION_VESSEL);
        flowcell.getContainerRole().applyCriteriaToAllPositions(batchCriteria,
                TransferTraverserCriteria.TraversalDirection.Ancestors);
        Collection<LabBatch> prodFlowcellBatches = batchCriteria.getAllLabBatches();
        if (prodFlowcellBatches.isEmpty()) {
            prodFlowcellBatches = flowcell.getAllLabBatches(LabBatch.LabBatchType.FCT);
        }

        /** END Temp Hack **/

        if (!prodFlowcellBatches.isEmpty()) {
            for (VesselAndPosition vesselAndPosition : loadedVesselsAndPositions) {
                LabVessel sourceVessel = vesselAndPosition.getVessel();
                VesselPosition vesselPosition = vesselAndPosition.getPosition();
                BigDecimal concentration = getLoadingConcentrationForVessel(sourceVessel);
                SequencingTemplateLaneType lane = LimsQueryObjectFactory.createSequencingTemplateLaneType(
                        vesselPosition.name(), concentration, sourceVessel.getLabel(),
                        prodFlowcellBatches.iterator().next().getStartingVesselByPosition(vesselPosition).getLabel());
                lanes.add(lane);
            }
        }

        if (lanes.isEmpty()) {
            // Do we need to create "null" lanes to satisfy the user requirement of returning null
            // when we don't have the data?
            SequencingTemplateLaneType lane = new SequencingTemplateLaneType();
            lanes.add(lane);
        }
        String sequencingTemplateName = null;
        if (!prodFlowcellBatches.isEmpty()) {
            sequencingTemplateName = prodFlowcellBatches.iterator().next().getBatchName();
        }
        SequencingConfigDef sequencingConfig = getSequencingConfig(isPoolTest);

        Set<SampleInstanceV2> sampleInstances = flowcell.getSampleInstancesV2();
        Set<String> regulatoryDesignations = new HashSet<>();
        Set<String> productNames = new HashSet<>();
        List<ProductType> productTypes = new ArrayList<>();
        Set<String> readStructures = new HashSet<>();
        attachRegulatoryDesignationAndProductOrder(sampleInstances, regulatoryDesignations, productNames,
                productTypes, readStructures, isPoolTest);
        if (readStructures.size() > 1) {
            throw new InformaticsServiceException("Mixture of readStructures: " + readStructures);
        }
        String readStructure = readStructures.size() == 1 ? readStructures.iterator().next() :
                sequencingConfig.getReadStructure().getValue();
        SequencingTemplateType sequencingTemplate = LimsQueryObjectFactory.createSequencingTemplate(
                sequencingTemplateName, flowcell.getLabel(), isPoolTest,
                sequencingConfig.getInstrumentWorkflow().getValue(), sequencingConfig.getChemistry().getValue(),
                readStructure);
        sequencingTemplate.getLanes().addAll(lanes);
        sequencingTemplate.getProducts().addAll(productTypes);
        sequencingTemplate.getRegulatoryDesignation().addAll(regulatoryDesignations);
        return sequencingTemplate;
    }

    /**
     * This method gets the loading concentration from the batch/vessel relationship.
     *
     *
     * @param sourceVessel The vessel to get the loading concentration of.
     *
     * @return The loading concentration for the vessel.
     */
    private BigDecimal getLoadingConcentrationForVessel(LabVessel sourceVessel) {
        Collection<LabBatch> batches = sourceVessel.getAllLabBatches(LabBatch.LabBatchType.FCT);
        BigDecimal concentration = null;
        for (LabBatch batch : batches) {
            for (LabBatchStartingVessel labBatchStartingVessel : batch.getLabBatchStartingVessels()) {
                concentration = labBatchStartingVessel.getConcentration();
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
        BigDecimal concentration = null;
        if (miSeqReagentKit.getConcentration() != null) {
            concentration = miSeqReagentKit.getConcentration();
        }
        List<SequencingTemplateLaneType> lanes = new ArrayList<>();

        Collection<LabBatch> miseqBatches = miSeqReagentKit.getAllLabBatches(LabBatch.LabBatchType.MISEQ);

        Set<String> denatureBarcodes = new HashSet<>();
        for (LabBatch miseqBatch : miseqBatches) {
            denatureBarcodes.add(miseqBatch.getStartingVesselByPosition(VesselPosition.LANE1).getLabel());
        }

        if (denatureBarcodes.size() > 1) {
            throw new InformaticsServiceException(String.format("There are more than one MiSeq Batches " +
                                                                "associated with %s", miSeqReagentKit.getLabel()));
        }

        lanes.add(LimsQueryObjectFactory.createSequencingTemplateLaneType(
                IlluminaFlowcell.FlowcellType.MiSeqFlowcell.getVesselGeometry().getRowNames()[0],
                concentration, "",
                denatureBarcodes.iterator().next()));
        return LimsQueryObjectFactory.createSequencingTemplate(null, null, isPoolTest,
                sequencingConfig.getInstrumentWorkflow().getValue(), sequencingConfig.getChemistry().getValue(),
                sequencingConfig.getReadStructure().getValue(),
                lanes.toArray(new SequencingTemplateLaneType[lanes.size()]));
    }

    private void attachRegulatoryDesignationAndProductOrder(Set<SampleInstanceV2> sampleInstances,
            Set<String> regulatoryDesignations, Set<String> productNames, List<ProductType> productTypes,
            Set<String> readStructures, boolean isPoolTest) {
        for(SampleInstanceV2 sampleInstance: sampleInstances) {
            // Controls don't have bucket entries, but we assume that the non-control samples dominate.
            if (sampleInstance.getSingleBucketEntry() != null) {
                String regulatoryDesignation = sampleInstance.getSingleBucketEntry().
                        getProductOrder().getResearchProject().getRegulatoryDesignation().name();
                regulatoryDesignations.add(regulatoryDesignation);
                Product product = sampleInstance.getSingleBucketEntry().getProductOrder().getProduct();
                String productName = product.getName();
                if (!productNames.contains(productName)) {
                    ProductType productType = new ProductType();
                    productType.setName(productName);
                    productTypes.add(productType);
                    productNames.add(productName);
                }
                StringBuilder readStructure = new StringBuilder();
                if (product.getReadLength() != null) {
                    if (!isPoolTest) {
                        readStructure.append(product.getReadLength()).append("T");
                    }
                    MolecularIndexingScheme molecularIndexingScheme = sampleInstance.getMolecularIndexingScheme();
                    if (molecularIndexingScheme != null) {
                        for (MolecularIndex molecularIndex : molecularIndexingScheme.getIndexes().values()) {
                            readStructure.append(molecularIndex.getSequence().length()).append("B");
                        }
                    }
                    if (!isPoolTest) {
                        readStructure.append(product.getReadLength()).append("T");
                    }
                }
                if (readStructure.length() != 0) {
                    readStructures.add(readStructure.toString());
                }
            }
        }

        if(regulatoryDesignations.isEmpty()) {
            throw new InformaticsServiceException("Could not find regulatory designation.");
        } else if(regulatoryDesignations.contains(ResearchProject.RegulatoryDesignation.RESEARCH_ONLY.name())
                && (regulatoryDesignations.contains(ResearchProject.RegulatoryDesignation.GENERAL_CLIA_CAP.name()) ||
                    regulatoryDesignations.contains(ResearchProject.RegulatoryDesignation.CLINICAL_DIAGNOSTICS.name()))){
            throw new InformaticsServiceException("Template tube has mix of Research and Clinical regulatory designations");
        }

        if(productNames.isEmpty()) {
            throw new InformaticsServiceException("Could not find any products.");
        }

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
}
