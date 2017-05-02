package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;

import java.util.List;
import java.util.Set;

/**
 * Attached to a ConfigurableSearchDefinition to expand primary list of entity identifiers
 *    to include other entities in the results (e.g. ancestors and/or descendants). <br/>
 * The CustomTraversalEvaluator allows logic to be attached to ancestor and or descendant traversals the user selects.
 * TraversalEvaluator instances are attached to a ConfigurableSearchDefinition and thus shared.
 * Any implementations must be thread-safe.
 */
public abstract class CustomTraversalEvaluator extends TraversalEvaluator {

    private String uiName;

    public CustomTraversalEvaluator(String label, String uiName){
        super(label);
        this.uiName = uiName;
    }

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

}