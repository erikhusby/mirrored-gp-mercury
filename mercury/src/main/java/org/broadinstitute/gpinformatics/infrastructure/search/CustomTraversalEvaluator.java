package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;

import java.util.List;
import java.util.Set;

/**
 * Attached to a ConfigurableSearchDefinition to expand primary list of entity identifiers
 * to include other entities in the results (e.g. ancestors and/or descendants). <br/>
 * The CustomTraversalEvaluator allows logic to be attached to ancestor and or descendant traversals the user selects.
 */
public abstract class CustomTraversalEvaluator extends TraversalEvaluator {

    /**
     * This constructor used to produce vertical additional rows
     * @param label Display label
     * @param uiName UI form field name
     * @param resultParamConfiguration User selected customization options
     */
    protected CustomTraversalEvaluator(String label, String uiName, ResultParamConfiguration resultParamConfiguration){
        this( label, uiName );
        this.resultParamConfiguration = resultParamConfiguration;
    }

    /**
     * This constructor used for horizontal user defined columns
     * @param label Display label
     * @param uiName UI form field name
     */
    protected CustomTraversalEvaluator(String label, String uiName){
        super(label);
        this.uiName = uiName;
    }

    private String uiName;

    private ResultParamConfiguration resultParamConfiguration;

    @Override
    public Set<Object> evaluate(List<? extends Object> rootEntities, SearchInstance searchInstance){
        throw new UnsupportedOperationException("CustomTraversalEvaluator does not support evaluation without a TraversalDirection argument");
    }

    @Override
    public TransferTraverserCriteria.TraversalDirection getTraversalDirection(){
        throw new UnsupportedOperationException("TraversalDirection is dynamic in a CustomTraversalEvaluator");
    }

    public abstract Set<Object> evaluate(List<? extends Object> rootEntities
            , TransferTraverserCriteria.TraversalDirection traversalDirection, SearchInstance searchInstance);

    public String getUiName(){
        return uiName;
    }

    public ResultParamConfiguration getResultParamConfiguration() {
        return resultParamConfiguration;
    }

    public boolean isUserConfigurable(){
        return resultParamConfiguration != null;
    }

}