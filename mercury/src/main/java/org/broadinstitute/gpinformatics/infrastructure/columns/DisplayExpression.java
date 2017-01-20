package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchTerm;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Expressions for configurable columns, grouped by ExpressionClass.
 */
public enum DisplayExpression {
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
    PRODUCT_NAME(SampleInstanceV2.class, new SearchTerm.Evaluator<List<String>>() {
        @Override
        public List<String> evaluate(Object entity, SearchContext context) {
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            List<String> results = new ArrayList<>();
            // todo jmt try getSingleProductOrderSample first
            for (ProductOrderSample productOrderSample : sampleInstanceV2.getAllProductOrderSamples()) {
                if (productOrderSample.getProductOrder().getProduct() != null) {
                    results.add(productOrderSample.getProductOrder().getProduct().getDisplayName());
                }
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
            return sampleData.getCollaboratorsSampleName();
        }
    }),
    COLLABORATOR_PARTICIPANT_ID(SampleData.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleData sampleData = (SampleData) entity;
            return sampleData.getCollaboratorParticipantId();
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
    ;

    private final Class<?> expressionClass;
    private final SearchTerm.Evaluator<?> displayExpression;

    DisplayExpression(Class<?> expressionClass, SearchTerm.Evaluator<?> displayExpression) {
        this.expressionClass = expressionClass;
        this.displayExpression = displayExpression;
    }

    public Class<?> getExpressionClass() {
        return expressionClass;
    }

    public SearchTerm.Evaluator<?> getDisplayExpression() {
        return displayExpression;
    }
}
