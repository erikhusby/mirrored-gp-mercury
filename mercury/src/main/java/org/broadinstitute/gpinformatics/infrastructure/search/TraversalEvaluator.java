package org.broadinstitute.gpinformatics.infrastructure.search;

import java.util.List;
import java.util.Set;

/**
 * Attached to a ConfigurableSearchDefinition to expand primary list of entity identifiers
 *    to include other entities in the results (e.g. ancestors and/or descendants).
 * TraversalEvaluator instances are attached to a ConfigurableSearchDefinition and thus shared.
 * Any implementations must be thread-safe.
 */
public abstract class TraversalEvaluator {

    private String label;
    protected String helpNote;

    public TraversalEvaluator() {}

    public abstract Set<Object> evaluate(List<? extends Object> rootEntities);

    public abstract List<Object> buildEntityIdList( Set<? extends Object> entities );

    public String getLabel(){
        return label;
    }
    public void setLabel( String label ) {
        this.label = label;
    }

    public String getName(){
        return  this.getClass().getEnclosingClass().getSimpleName();
    }

    public String getHelpNote(){
        return helpNote;
    }

    public void setHelpNote(String helpNote){
        this.helpNote = helpNote;
    }

}