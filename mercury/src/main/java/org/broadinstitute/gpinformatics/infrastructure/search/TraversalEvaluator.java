package org.broadinstitute.gpinformatics.infrastructure.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Attached to a ConfigurableSearchDefinition to expand list of entity identifiers
 *    to include (primarily) ancestors and/or descendants, but support other configured options.
 */
public abstract class TraversalEvaluator {

    private String title;
    protected String helpHeader;
    private List<TraversalOption> traversalOptions;

    public TraversalEvaluator(String title) {
        this.title = title;
    }

    public abstract Set<Object> evaluate(List<? extends Object> rootEntities, Map<String,Boolean> evaluatorOptionValues);

    public abstract List<Object> buildEntityIdList( Set<? extends Object> entities );

    public String getTitle(){
        return  title;
    }
    public String getName(){
        return  this.getClass().getEnclosingClass().getSimpleName();
    }

    public String getHelpHeader(){
        return  helpHeader;
    }

    public void addTraversalOption( TraversalOption traversalOption ) {
        if( traversalOptions == null ) {
            traversalOptions = new ArrayList<TraversalOption>();
        }
        traversalOptions.add(traversalOption);
    }

    public List<TraversalOption> getTraversalOptions(){
        return traversalOptions;
    }

    /**
     *
     */
    public static class TraversalOption {
        public String label;
        public String id;
        public String helpNote;

        public TraversalOption(){
        }

        public TraversalOption( String label, String id, String helpNote ){
            this.label = label;
            this.id = id;
            this.helpNote = helpNote;
        }

        public String getLabel(){
            return label;
        }

        public String getId(){
            return id;
        }
        public String getHelpNote(){
            return helpNote;
        }
    }
}