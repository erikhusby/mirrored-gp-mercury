package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchTerm;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.DesignedReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.UMIReagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

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
    ROOT_SAMPLE_ID(SampleInstanceV2.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            return sampleInstanceV2.getRootOrEarliestMercurySampleName();
        }
    }),
    ROOT_TUBE_BARCODE(SampleInstanceV2.class, new SearchTerm.Evaluator<Set<String>>() {
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
    NEAREST_SAMPLE_ID(SampleInstanceV2.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            return sampleInstanceV2.getNearestMercurySampleName();
        }
    }),
    LCSET(SampleInstanceV2.class, new SearchTerm.Evaluator<List<String>>() {
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
    ARRAY(SampleInstanceV2.class, new SearchTerm.Evaluator<List<String>>() {
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
    XTR(SampleInstanceV2.class, new SearchTerm.Evaluator<Set<String>>() {
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
    PDO(SampleInstanceV2.class, new SearchTerm.Evaluator<List<String>>() {
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
    PROCEED_IF_OOS(SampleInstanceV2.class, new SearchTerm.Evaluator<List<String>>() {
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
    RESEARCH_PROJECT(SampleInstanceV2.class, new SearchTerm.Evaluator<List<String>>() {
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
    REGULATORY_DESIGNATION(SampleInstanceV2.class, new SearchTerm.Evaluator<List<String>>() {
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
    PRODUCT_NAME(SampleInstanceV2.class, new SearchTerm.Evaluator<List<String>>() {
        @Override
        public List<String> evaluate(Object entity, SearchContext context) {
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            List<String> results = new ArrayList<>();
            ProductOrderSample pdoSampleForSingleBucket = sampleInstanceV2.getProductOrderSampleForSingleBucket();
            if (pdoSampleForSingleBucket == null) {
                for (ProductOrderSample productOrderSample : sampleInstanceV2.getAllProductOrderSamples()) {
                    if (productOrderSample.getProductOrder().getProduct() != null) {
                        results.add(productOrderSample.getProductOrder().getProduct().getDisplayName());
                    }
                }
            } else {
                results.add(pdoSampleForSingleBucket.getProductOrder().getProduct().getDisplayName());
            }
            return results;
        }
    }),
    MOLECULAR_INDEX(SampleInstanceV2.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            MolecularIndexingScheme molecularIndexingScheme = sampleInstanceV2.getMolecularIndexingScheme();
            return molecularIndexingScheme == null ? null : molecularIndexingScheme.getName();
        }
    }),
    UNIQUE_MOLECULAR_IDENTIFIER(SampleInstanceV2.class, new SearchTerm.Evaluator<List<String>>() {
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
    BAIT_OR_CAT_NAME(SampleInstanceV2.class, new SearchTerm.Evaluator<List<String>>() {
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
    BAIT_REAGENTS(SampleInstanceV2.class, new SearchTerm.Evaluator<Set<DesignedReagent>>() {
        @Override
        public Set<DesignedReagent> evaluate(Object entity, SearchContext context) {
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            return sampleInstanceV2.getDesignReagents();
        }
    }),
    METADATA(SampleInstanceV2.class, new SearchTerm.Evaluator<String>() {
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
    REAGENT_METADATA(Reagent.class, new SearchTerm.Evaluator<String>() {
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
    METADATA_SOURCE(SampleInstanceV2.class, new SearchTerm.Evaluator<String>() {
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
    STOCK_SAMPLE(SampleData.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleData sampleData = (SampleData) entity;
            return sampleData.getStockSample();
        }
    }),
    COLLABORATOR_SAMPLE_ID(SampleData.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            
            SampleData sampleData = (SampleData) entity;
            String collaboratorsSampleName = "";
            if (!context.getUserBean().isViewer()) {
                collaboratorsSampleName = sampleData.getCollaboratorsSampleName();
            }
            return collaboratorsSampleName;
        }
    }),
    COLLABORATOR_PARTICIPANT_ID(SampleData.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {

            SampleData sampleData = (SampleData) entity;
            String collaboratorParticipantId = "";
            if (!context.getUserBean().isViewer()) {
                collaboratorParticipantId = sampleData.getCollaboratorParticipantId();
            }
            return collaboratorParticipantId;
        }
    }),
    SAMPLE_TYPE(SampleData.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleData sampleData = (SampleData) entity;
            return sampleData.getSampleType();
        }
    }),
    COLLECTION(SampleData.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleData sampleData = (SampleData) entity;
            return sampleData.getCollection();
        }
    }),
    ORIGINAL_MATERIAL_TYPE(SampleData.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleData sampleData = (SampleData) entity;
            return sampleData.getOriginalMaterialType();
        }
    }),
    MATERIAL_TYPE(SampleData.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleData sampleData = (SampleData) entity;
            return sampleData.getMaterialType();
        }
    }),
    SPECIES(SampleData.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleData sampleData = (SampleData) entity;
            return sampleData.getOrganism();
        }
    }),
    PATIENT(SampleData.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleData sampleData = (SampleData) entity;
            return sampleData.getPatientId();
        }
    }),
    GENDER(SampleData.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleData sampleData = (SampleData) entity;
            return sampleData.getGender();
        }
    });

    private final Class<?> expressionClass;
    private final SearchTerm.Evaluator<?> evaluator;

    DisplayExpression(Class<?> expressionClass, SearchTerm.Evaluator<?> evaluator) {
        this.expressionClass = expressionClass;
        this.evaluator = evaluator;
    }

    public Class<?> getExpressionClass() {
        return expressionClass;
    }

    public SearchTerm.Evaluator<?> getEvaluator() {
        return evaluator;
    }

    /**
     * Navigates from an entity in a row to the entity on which to evaulate an expression.  The list returned from
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
            List<MercurySample> mercurySamples = new ArrayList<>();
            for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                MercurySample mercurySample = sampleInstanceV2.getNearestMercurySample();
                if (mercurySample != null) {
                    mercurySamples.add(mercurySample);
                }
            }

            List<SampleData> results = mercurySampleToSampleData(context, mercurySamples);
            return (List<T>) results;

        } else if (OrmUtil.proxySafeIsInstance(rowObject, LabEvent.class) && expressionClass.isAssignableFrom(SampleInstanceV2.class)) {
            // LabEvent to SampleInstance
            LabEvent labEvent = (LabEvent) rowObject;
            return (List<T>) new ArrayList<>(labEventToSampleInstances(labEvent));

        } else if (OrmUtil.proxySafeIsInstance(rowObject, LabEvent.class) && expressionClass.isAssignableFrom(SampleData.class)) {
            // LabEvent to SampleData
            LabEvent labEvent = (LabEvent) rowObject;
            Set<SampleInstanceV2> sampleInstances = labEventToSampleInstances(labEvent);

            List<MercurySample> mercurySamples = new ArrayList<>();
            for (SampleInstanceV2 sampleInstance : sampleInstances) {
                MercurySample mercurySample = sampleInstance.getNearestMercurySample();
                if (mercurySample != null) {
                    mercurySamples.add(mercurySample);
                }
            }
            return (List<T>) mercurySampleToSampleData(context, mercurySamples);

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

        } else if (OrmUtil.proxySafeIsInstance(rowObject, Reagent.class)) {
            Reagent reagent = (Reagent) rowObject;
            return (List<T>) Arrays.asList(reagent);
        } else {
            throw new RuntimeException("Unexpected combination " + rowObject.getClass() + " to " + expressionClass);
        }
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

    private static <T> Set<SampleInstanceV2> labEventToSampleInstances(LabEvent labEvent) {
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
