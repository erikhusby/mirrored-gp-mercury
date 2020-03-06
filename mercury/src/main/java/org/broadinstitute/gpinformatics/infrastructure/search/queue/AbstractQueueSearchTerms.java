package org.broadinstitute.gpinformatics.infrastructure.search.queue;


import org.broadinstitute.gpinformatics.infrastructure.search.SearchTerm;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class AbstractQueueSearchTerms {


    protected Map<SearchTerm, String> termDescriptionMap = new HashMap<>();

    /**
     * Map from term name to search term, includes dependent terms
     */
    protected Map<String, SearchTerm> mapNameToSearchTerm = new HashMap<>();

    AbstractQueueSearchTerms() {
        addSearchTerms();
    }

    // todo unclear whether this should be loading base queue search terms or not (haven't seen any yet).
    protected abstract void addSearchTerms();

    // Search terms allowed to be shown and selectable by a user in a queue page.
    public abstract Set<String> getAllowedDisplaySearchTerms();
    // All possible search terms for UDS.
    public abstract Set<SearchTerm> getSearchTerms();

    public SearchTerm geSearchTerm(String term) {
        return mapNameToSearchTerm.get(term);
    }
}
