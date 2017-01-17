package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchTerm;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;

/**
 * Expressions for configurable columns, grouped by ExpressionClass.
 */
public enum DisplayExpression {
    ROOT_SAMPLE_ID(ExpressionClass.SAMPLE_INSTANCE, new SearchTerm.Evaluator<String>() {
        @Override
        public String evaluate(Object entity, SearchContext context) {
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            return sampleInstanceV2.getRootOrEarliestMercurySampleName();
        }
    }),
    NEAREST_SAMPLE_ID(ExpressionClass.SAMPLE_INSTANCE, new SearchTerm.Evaluator<Object>() {
        @Override
        public Object evaluate(Object entity, SearchContext context) {
            SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) entity;
            return sampleInstanceV2.getNearestMercurySampleName();
        }
    });

    private final ExpressionClass expressionClass;
    private final SearchTerm.Evaluator<?> displayExpression;

    DisplayExpression(ExpressionClass expressionClass, SearchTerm.Evaluator<?> displayExpression) {
        this.expressionClass = expressionClass;
        this.displayExpression = displayExpression;
    }

    public ExpressionClass getExpressionClass() {
        return expressionClass;
    }

    public SearchTerm.Evaluator<?> getDisplayExpression() {
        return displayExpression;
    }
}
