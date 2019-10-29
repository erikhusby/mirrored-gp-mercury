package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchTerm;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueGrouping;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.DesignedReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.UMIReagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Expressions for configurable columns, grouped by ExpressionClass.
 */
public enum DisplayExpression {

    // SampleInstance
    ROOT_SAMPLE_ID("Root Sample ID", SampleInstanceV2.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            return sampleInstanceV2.getRootOrEarliestMercurySampleName();
        }
    }),
    ROOT_TUBE_BARCODE("Root Tube Barcode", SampleInstanceV2.class, new SearchTerm.Evaluator<Set<String>>() {
        @Override
        public Set<String> evaluate(Object entity, SearchContext context) {
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            Set<String> results = new HashSet<>();
            if(sampleInstanceV2.getRootOrEarliestMercurySample() != null){
                for (LabVessel rootSampleVessel : sampleInstanceV2.getRootOrEarliestMercurySample()
                        .getLabVessel()) {
                    results.add(rootSampleVessel.getLabel());
                }
            }
            return results;
        }
    }),
    NEAREST_SAMPLE_ID("Nearest Sample ID", SampleInstanceV2.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            return sampleInstanceV2.getNearestMercurySampleName();
        }
    }),
    LCSET("LCSET", SampleInstanceV2.class, new SearchTerm.Evaluator<List<String>>() {
        @Override
        public List<String> evaluate(Object entity, SearchContext context) {
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            List<String> results = new ArrayList<>();
            // todo jmt try getSingleBatch first
            for( LabBatch labBatch : sampleInstanceV2.getAllWorkflowBatches() ) {
                if( labBatch.getBatchName().startsWith("LCSET")) {
                    results.add(labBatch.getBatchName());
                }
            }
            return results;
        }
    }),
    ARRAY("ARRAY", SampleInstanceV2.class, new SearchTerm.Evaluator<List<String>>() {
        @Override
        public List<String> evaluate(Object entity, SearchContext context) {
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            List<String> results = new ArrayList<>();
            // todo jmt try getSingleBatch first
            for( LabBatch labBatch : sampleInstanceV2.getAllWorkflowBatches() ) {
                if( labBatch.getBatchName().startsWith("ARRAY")) {
                    results.add(labBatch.getBatchName());
                }
            }
            return results;
        }
    }),
    XTR("XTR", SampleInstanceV2.class, new SearchTerm.Evaluator<Set<String>>() {
        @Override
        public Set<String> evaluate(Object entity, SearchContext context) {
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            Set<String> results = new HashSet<>();
            // todo jmt try getSingleBatch first
            for( LabBatch labBatch : sampleInstanceV2.getAllWorkflowBatches() ) {
                if( labBatch.getBatchName().startsWith("XTR")) {
                    results.add(labBatch.getBatchName());
                }
            }
            return results;
        }
    }),
    PDO("PDO", SampleInstanceV2.class, new SearchTerm.Evaluator<List<String>>() {
        @Override
        public List<String> evaluate(Object entity, SearchContext context) {
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            List<String> results = new ArrayList<>();
            // todo jmt try getSingleProductOrderSample first
            for (ProductOrderSample productOrderSample : sampleInstanceV2.getAllProductOrderSamples() ) {
                results.add(productOrderSample.getProductOrder().getJiraTicketKey());
            }
            return results;
        }
    }),
    PROCEED_IF_OOS("Proceed if OOS", SampleInstanceV2.class, new SearchTerm.Evaluator<List<String>>() {
        @Override
        public List<String> evaluate(Object entity, SearchContext context) {
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            List<String> results = new ArrayList<>();
            // todo jmt try getSingleProductOrderSample first
            for (ProductOrderSample productOrderSample : sampleInstanceV2.getAllProductOrderSamples()) {
                ProductOrderSample.ProceedIfOutOfSpec proceedIfOutOfSpec =
                        productOrderSample.getProceedIfOutOfSpec();
                if (proceedIfOutOfSpec == null) {
                    proceedIfOutOfSpec = ProductOrderSample.ProceedIfOutOfSpec.NO;
                }
                results.add(proceedIfOutOfSpec.getDisplayName());
            }
            return results;
        }
    }),
    RESEARCH_PROJECT("Research Project", SampleInstanceV2.class, new SearchTerm.Evaluator<List<String>>() {
        @Override
        public List<String> evaluate(Object entity, SearchContext context) {
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            List<String> results = new ArrayList<>();
            // todo jmt try getSingleProductOrderSample first
            for (ProductOrderSample productOrderSample : sampleInstanceV2.getAllProductOrderSamples() ) {
                if( productOrderSample.getProductOrder().getResearchProject() != null
                        && productOrderSample.getProductOrder().getResearchProject().getName() != null) {
                    results.add(productOrderSample.getProductOrder().getResearchProject().getName());
                }
            }
            return results;
        }
    }),
    REGULATORY_DESIGNATION("Regulatory Designation", SampleInstanceV2.class, new SearchTerm.Evaluator<List<String>>() {
        @Override
        public List<String> evaluate(Object entity, SearchContext context) {
            List<String> results = new ArrayList<>();
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            ProductOrderSample singleProductOrderSample = sampleInstanceV2.getSingleProductOrderSample();
            if (singleProductOrderSample != null) {
                if (singleProductOrderSample.getProductOrder() != null &&
                    singleProductOrderSample.getProductOrder().getResearchProject() != null) {
                    results.add(singleProductOrderSample.getProductOrder().getResearchProject()
                            .getRegulatoryDesignationCodeForPipeline());
                    return results;
                }
            }
            for (ProductOrderSample productOrderSample : sampleInstanceV2.getAllProductOrderSamples() ) {
                if( productOrderSample.getProductOrder().getResearchProject() != null) {
                    results.add(productOrderSample.getProductOrder().getResearchProject()
                            .getRegulatoryDesignationCodeForPipeline());
                }
            }
            return results;
        }
    }),
    PRODUCT_NAME("Product", SampleInstanceV2.class, new SearchTerm.Evaluator<List<String>>() {
        @Override
        public List<String> evaluate(Object entity, SearchContext context) {
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            List<String> results = new ArrayList<>();
            ProductOrderSample pdoSampleForSingleBucket = sampleInstanceV2.getProductOrderSampleForSingleBucket();
            if (pdoSampleForSingleBucket == null) {
                for (ProductOrderSample productOrderSample : sampleInstanceV2.getAllProductOrderSamples()) {
                    if (productOrderSample.getProductOrder().getProduct() != null) {
                        results.add(productOrderSample.getProductOrder().getProduct().getName());
                    }
                }
            } else {
                results.add(pdoSampleForSingleBucket.getProductOrder().getProduct().getName());
            }
            return results;
        }
    }),
    MOLECULAR_INDEX("Molecular Index", SampleInstanceV2.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            MolecularIndexingScheme molecularIndexingScheme = sampleInstanceV2.getMolecularIndexingScheme();
            return molecularIndexingScheme == null ? null : molecularIndexingScheme.getName();
        }
    }),
    UNIQUE_MOLECULAR_IDENTIFIER("Unique Molecular Identifier", SampleInstanceV2.class, new SearchTerm.Evaluator<List<String>>() {
        @Override
        public List<String> evaluate(Object entity, SearchContext context) {
            List<String> results = new ArrayList<>();
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            for (UMIReagent umiReagent: sampleInstanceV2.getUmiReagents()) {
                results.add(umiReagent.getUniqueMolecularIdentifier().getDisplayName());
            }
            return results;
        }
    }),
    BAIT_OR_CAT_NAME("Bait/CAT Name", SampleInstanceV2.class, new SearchTerm.Evaluator<List<String>>() {
        @Override
        public List<String> evaluate(Object entity, SearchContext context) {
            List<String> results = new ArrayList<>();
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            for (ReagentDesign reagentDesign: sampleInstanceV2.getReagentsDesigns()) {
                results.add(reagentDesign.getName() + "(" + reagentDesign.getReagentType().toString() + ")");
            }
            return results;
        }
    }),
    BAIT_REAGENTS("Bait Reagents", SampleInstanceV2.class, new SearchTerm.Evaluator<Set<DesignedReagent>>() {
        @Override
        public Set<DesignedReagent> evaluate(Object entity, SearchContext context) {
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            return sampleInstanceV2.getDesignReagents();
        }
    }),
    METADATA(null, SampleInstanceV2.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            SearchTerm searchTerm = context.getSearchTerm();
            String metaName = searchTerm.getName();
            Metadata.Key key = Metadata.Key.fromDisplayName(metaName);
            if (key == Metadata.Key.MATERIAL_TYPE) {
                MaterialType materialType = sampleInstanceV2.getMaterialType();
                return materialType == null ? null : materialType.getDisplayName();
            }

            MercurySample mercurySample = sampleInstanceV2.getRootOrEarliestMercurySample();
            if (mercurySample != null) {
                for( Metadata meta : mercurySample.getMetadata()){
                    if( meta.getKey() == key ) {
                        return meta.getValue();
                    }
                }
            }
            return null;
        }
    }),
    REAGENT_METADATA(null, Reagent.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            Reagent reagent = (Reagent) entity;
            SearchTerm searchTerm = context.getSearchTerm();
            String metaName = searchTerm.getName();
            Metadata.Key key = Metadata.Key.fromDisplayName(metaName);
            for( Metadata meta : reagent.getMetadata()){
                if( meta.getKey() == key ) {
                    return meta.getValue();
                }
            }
            return null;
        }
    }),
    METADATA_SOURCE("Metadata Source", SampleInstanceV2.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            if (!sampleInstanceV2.isReagentOnly()) {
                return sampleInstanceV2.getRootOrEarliestMercurySample().getMetadataSource().getDisplayName();
            }
            return null;
        }
    }),

    // SampleData
    STOCK_SAMPLE("Stock Sample ID", SampleData.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleData sampleData = (SampleData) entity;
            return sampleData.getStockSample();
        }
    }, BSPSampleSearchColumn.STOCK_SAMPLE),
    COLLABORATOR_SAMPLE_ID("Collaborator Sample ID", SampleData.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {

            SampleData sampleData = (SampleData) entity;
            String collaboratorsSampleName = "";
            if (!context.getUserBean().isViewer()) {
                collaboratorsSampleName = sampleData.getCollaboratorsSampleName();
            }
            return collaboratorsSampleName;
        }
    }, BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID),
    COLLABORATOR_PARTICIPANT_ID("Collaborator Participant ID", SampleData.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {

            SampleData sampleData = (SampleData) entity;
            String collaboratorParticipantId = "";
            if (!context.getUserBean().isViewer()) {
                collaboratorParticipantId = sampleData.getCollaboratorParticipantId();
            }
            return collaboratorParticipantId;
        }
    }, BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID),
    SAMPLE_TYPE("Tumor / Normal", SampleData.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleData sampleData = (SampleData) entity;
            return sampleData.getSampleType();
        }
    }, BSPSampleSearchColumn.SAMPLE_TYPE),
    COLLECTION("Collection", SampleData.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleData sampleData = (SampleData) entity;
            return sampleData.getCollection();
        }
    }, BSPSampleSearchColumn.COLLECTION),
    ORIGINAL_MATERIAL_TYPE("Original Material Type", SampleData.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleData sampleData = (SampleData) entity;
            return sampleData.getOriginalMaterialType();
        }
    }, BSPSampleSearchColumn.ORIGINAL_MATERIAL_TYPE),
    MATERIAL_TYPE("Material Type", SampleData.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleData sampleData = (SampleData) entity;
            return sampleData.getMaterialType();
        }
    }, BSPSampleSearchColumn.MATERIAL_TYPE),
    SPECIES("Species", SampleData.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleData sampleData = (SampleData) entity;
            return sampleData.getOrganism();
        }
    }, BSPSampleSearchColumn.SPECIES),
    PATIENT("Participant ID", SampleData.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleData sampleData = (SampleData) entity;
            return sampleData.getPatientId();
        }
    }, BSPSampleSearchColumn.PARTICIPANT_ID),
    GENDER("Gender", SampleData.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleData sampleData = (SampleData) entity;
            return sampleData.getGender();
        }
    }, BSPSampleSearchColumn.GENDER),
    SALES_ORDER_NUMBER(SampleInstanceV2.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            LabVessel labVessel = ((SampleInstanceV2)entity).getInitialLabVessel();
            StaticPlate staticPlate = (labVessel == null ||
                    CollectionUtils.isEmpty(labVessel.getContainers()) ||
                    !OrmUtil.proxySafeIsInstance(labVessel.getContainers().iterator().next(), StaticPlate.class)) ?
                    null : OrmUtil.proxySafeCast(labVessel.getContainers().iterator().next(), StaticPlate.class);
            return (staticPlate == null) ? null : staticPlate.getSalesOrderNumber();
        }
    })
    ;

    private final String columnName;
    private final Class<?> expressionClass;
    private final SearchTerm.Evaluator<?> evaluator;
    // todo jmt the mapping between BspSampleData method and BSPSampleSearchColumn belongs in that package, not here.
    private BSPSampleSearchColumn bspSampleSearchColumn;

    DisplayExpression(String columnName, Class<?> expressionClass, SearchTerm.Evaluator<?> evaluator) {
        this.columnName = columnName;
        this.expressionClass = expressionClass;
        this.evaluator = evaluator;
    }

    DisplayExpression(String columnName, Class<SampleData> expressionClass, SearchTerm.Evaluator<?> evaluator, BSPSampleSearchColumn bspSampleSearchColumn) {
        this(columnName, expressionClass, evaluator);
        this.bspSampleSearchColumn = bspSampleSearchColumn;
    }

    public Class<?> getExpressionClass() {
        return expressionClass;
    }

    public SearchTerm.Evaluator<?> getEvaluator() {
        return evaluator;
    }

    public BSPSampleSearchColumn getBspSampleSearchColumn() {
        return bspSampleSearchColumn;
    }

    public String getColumnName() {
        return columnName;
    }

    public enum ExpressionGroup {
        SAMPLE_REPOSITORY,
        SAMPLE_PROCESSING
    }

    public static List<DisplayExpression> listByGroup(ExpressionGroup expressionGroup) {
        List<DisplayExpression> displayExpressions = new ArrayList<>();
        for (DisplayExpression displayExpression : DisplayExpression.values()) {
            switch (expressionGroup) {
            case SAMPLE_REPOSITORY:
                if (displayExpression.getExpressionClass().equals(SampleData.class)) {
                    displayExpressions.add(displayExpression);
                }
                break;
            case SAMPLE_PROCESSING:
                if (displayExpression.getExpressionClass().equals(SampleInstanceV2.class)) {
                    displayExpressions.add(displayExpression);
                }
                break;
            }
        }
        return displayExpressions;
    }

    /**
     * Navigates from an entity in a row to the entity on which to evaluate an expression.  The list returned from
     * this must be ordered deterministically.
     * @param rowObject object from result row
     * @param expressionClass class against which expression will be evaluated
     * @param context search parameters
     * @param <T> expression class
     * @return list of T classes, must be in same order for repeated calls
     */
    public static <T> List<T> rowObjectToExpressionObject(@Nonnull Object rowObject, Class<T> expressionClass,
                                                          SearchContext context) {
        if (OrmUtil.proxySafeIsInstance(rowObject, LabVessel.class) && expressionClass.isAssignableFrom(SampleInstanceV2.class)) {
            // LabVessel to SampleInstance
            LabVessel labVessel = (LabVessel) rowObject;
            return (List<T>) new ArrayList<>(labVessel.getSampleInstancesV2());

        } else if (OrmUtil.proxySafeIsInstance(rowObject, LabVessel.class) && expressionClass.isAssignableFrom(SampleData.class)) {
            // LabVessel to SampleData
            LabVessel labVessel = (LabVessel) rowObject;
            List<MercurySample> mercurySamples = sampleInstancesToMercurySamples(labVessel.getSampleInstancesV2());
            return (List<T>) mercurySampleToSampleData(context, mercurySamples);

        } else if (OrmUtil.proxySafeIsInstance(rowObject, LabEvent.class) && expressionClass.isAssignableFrom(SampleInstanceV2.class)) {
            // LabEvent to SampleInstance
            LabEvent labEvent = (LabEvent) rowObject;
            return (List<T>) new ArrayList<>(labEventToSampleInstances(labEvent));

        } else if (OrmUtil.proxySafeIsInstance(rowObject, LabEvent.class) && expressionClass.isAssignableFrom(SampleData.class)) {
            // LabEvent to SampleData
            LabEvent labEvent = (LabEvent) rowObject;
            Set<SampleInstanceV2> sampleInstances = labEventToSampleInstances(labEvent);
            List<MercurySample> mercurySamples = sampleInstancesToMercurySamples(sampleInstances);
            return (List<T>) mercurySampleToSampleData(context, mercurySamples);

        } else if (OrmUtil.proxySafeIsInstance(rowObject, LabMetric.class) && expressionClass.isAssignableFrom(SampleData.class)) {
            // LabMetric to SampleData
            LabMetric labMetric = (LabMetric) rowObject;
            List<MercurySample> mercurySamples = sampleInstancesToMercurySamples(labMetric.getLabVessel().getSampleInstancesV2());
            return (List<T>) mercurySampleToSampleData(context, mercurySamples);

        } else if (OrmUtil.proxySafeIsInstance(rowObject, LabMetric.class) && expressionClass.isAssignableFrom(SampleInstanceV2.class)) {
            // LabMetric to SampleInstance
            LabMetric labMetric = (LabMetric) rowObject;
            return (List<T>) new ArrayList<>(labMetric.getLabVessel().getSampleInstancesV2());

        } else if (OrmUtil.proxySafeIsInstance(rowObject, MercurySample.class) && expressionClass.isAssignableFrom(SampleInstanceV2.class)) {
            // MercurySample to SampleInstance
            MercurySample mercurySample = (MercurySample) rowObject;
            Set<SampleInstanceV2> sampleInstances = new TreeSet<>();
            for (LabVessel labVessel : mercurySample.getLabVessel()) {
                sampleInstances.addAll(labVessel.getSampleInstancesV2());
            }
            return (List<T>) new ArrayList<>(sampleInstances);

        } else if (OrmUtil.proxySafeIsInstance(rowObject, MercurySample.class) && expressionClass.isAssignableFrom(SampleData.class)) {
            MercurySample mercurySample = (MercurySample) rowObject;
            return (List<T>) mercurySampleToSampleData(context, Collections.singletonList(mercurySample));

        } else if (OrmUtil.proxySafeIsInstance(rowObject, QueueGrouping.class) && expressionClass.isAssignableFrom(SampleData.class)) {
            // QueueGrouping to SampleData
            QueueGrouping queueGrouping = (QueueGrouping) rowObject;
            List<MercurySample> mercurySamples = new ArrayList<>();
            for (QueueEntity queuedEntity : queueGrouping.getQueuedEntities()) {
                mercurySamples.addAll(queuedEntity.getLabVessel().getMercurySamples());
            }
            return (List<T>) mercurySampleToSampleData(context, mercurySamples);

        } else if (OrmUtil.proxySafeIsInstance(rowObject, QueueGrouping.class) && expressionClass.isAssignableFrom(SampleInstanceV2.class)) {
            // QueueGrouping to SampleInstance
            QueueGrouping queueGrouping = (QueueGrouping) rowObject;
            List<SampleInstanceV2> sampleInstances = new ArrayList<>();
            for (QueueEntity queuedEntity : queueGrouping.getQueuedEntities()) {
                sampleInstances.addAll(queuedEntity.getLabVessel().getSampleInstancesV2());
            }
            return (List<T>) sampleInstances;

        } else if (OrmUtil.proxySafeIsInstance(rowObject, Reagent.class)) {
            Reagent reagent = (Reagent) rowObject;
            return (List<T>) Arrays.asList(reagent);
        } else {
            throw new RuntimeException("Unexpected combination " + rowObject.getClass() + " to " + expressionClass);
        }
    }

    @NotNull
    private static List<MercurySample> sampleInstancesToMercurySamples(Set<SampleInstanceV2> sampleInstances) {
        List<MercurySample> mercurySamples = new ArrayList<>();
        for (SampleInstanceV2 sampleInstance : sampleInstances) {
            MercurySample mercurySample = sampleInstance.getNearestMercurySample();
            if (mercurySample != null) {
                mercurySamples.add(mercurySample);
            }
        }
        return mercurySamples;
    }


    private static List<SampleData> mercurySampleToSampleData(SearchContext context, List<MercurySample> mercurySamples) {
        List<SampleData> results = new ArrayList<>();
        if (!mercurySamples.isEmpty()) {
            SampleDataFetcherAddRowsListener bspColumns = (SampleDataFetcherAddRowsListener) context.getRowsListener(
                    SampleDataFetcherAddRowsListener.class.getSimpleName());
            for( MercurySample mercurySample : mercurySamples) {
                results.add(bspColumns.getSampleData(mercurySample.getSampleKey()));
            }
        }
        return results;
    }

    private static Set<SampleInstanceV2> labEventToSampleInstances(LabEvent labEvent) {
        LabVessel labVessel = labEvent.getInPlaceLabVessel();
        if (labVessel == null) {
            Set<LabVessel> labVessels;
            LabEventType.PlasticToValidate plasticToValidate = labEvent.getLabEventType().getPlasticToValidate();
            switch (plasticToValidate) {
            case BOTH:
            case SOURCE:
                labVessels = labEvent.getSourceLabVessels();
                break;
            case TARGET:
                labVessels = labEvent.getTargetLabVessels();
                break;
            default:
                throw new RuntimeException("Unexpected enum " + plasticToValidate);
            }
            Set<SampleInstanceV2> sampleInstances = new TreeSet<>();
            for (LabVessel vessel : labVessels) {
                sampleInstances.addAll(vessel.getSampleInstancesV2());
            }
            return sampleInstances;
        } else {
            return labVessel.getSampleInstancesV2();
        }
    }

}
