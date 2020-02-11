package org.broadinstitute.gpinformatics.infrastructure.search.queue;


import org.broadinstitute.gpinformatics.infrastructure.search.SearchTerm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    public abstract Set<SearchTerm> getAllowedTerms();
    public abstract List<String> getAllowedResultFields();
    public abstract List<String> getNotFoundResultRows();

    public Map<SearchTerm, String> getTermDescriptionMap() {
        return termDescriptionMap;
    }

    public SearchTerm geSearchTerm(String term) {
        return mapNameToSearchTerm.get(term);
    }

    public List<SearchTerm> getSelectedSearchTerms(List<String> selectedSearchTerms) {
        List<SearchTerm> terms = new ArrayList<>(selectedSearchTerms.size());
        for (String selectedSearchTerm : selectedSearchTerms) {
            terms.add(geSearchTerm(selectedSearchTerm));
        }
        return terms;
    }
}
