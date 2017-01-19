package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchTerm;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;

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
    NEAREST_SAMPLE_ID(SampleInstanceV2.class, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            return sampleInstanceV2.getNearestMercurySampleName();
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
