package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class QueueGroupingSearchDefinition {
    ConfigurableSearchDefinition buildSearchDefinition() {
        ArrayList<ConfigurableSearchDefinition.CriteriaProjection> criteriaProjections = new ArrayList<>();
        HashMap<String, List<SearchTerm>> mapGroupSearchTerms = new HashMap<>();
        mapGroupSearchTerms.put("IDs", x());
        /*
        PM
        WR requestor
        Collection
        Pico JIRA ticket
        Kit WRID
        PDO
        WRID
        Sample ID
        Added date
         */
        return new ConfigurableSearchDefinition(ColumnEntity.QUEUE_GROUPING, criteriaProjections, mapGroupSearchTerms);
    }

    List<SearchTerm> x() {
        List<SearchTerm> searchTerms = new ArrayList<>();
        SearchTerm searchTerm = new SearchTerm();
        searchTerms.add(searchTerm);
        searchTerm.setName();
        return searchTerms;
    }
}
