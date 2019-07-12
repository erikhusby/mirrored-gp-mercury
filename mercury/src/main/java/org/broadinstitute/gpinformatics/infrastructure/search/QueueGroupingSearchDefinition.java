package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.columns.SampleDataFetcherAddRowsListener;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueGrouping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class QueueGroupingSearchDefinition {

    ConfigurableSearchDefinition buildSearchDefinition() {
        HashMap<String, List<SearchTerm>> mapGroupSearchTerms = new HashMap<>();

        mapGroupSearchTerms.put("IDs", buildIds());

        SearchTerm.CriteriaPath sampleKeyCriteriaPath = new SearchTerm.CriteriaPath();
        sampleKeyCriteriaPath.setCriteria(Arrays.asList("queuedEntities", "queueGrouping", "queuedEntities", "labVessel", "mercurySamples"));
        sampleKeyCriteriaPath.setPropertyName("sampleKey");

        mapGroupSearchTerms.put("BSP", LabMetricSearchDefinition.buildBsp(sampleKeyCriteriaPath));
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
        Drill down to lab vessel
         */
        List<ConfigurableSearchDefinition.CriteriaProjection> criteriaProjections = new ArrayList<>();
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("queuedEntities", "queueGroupingId",
                "queueGrouping", QueueEntity.class));
        ConfigurableSearchDefinition configurableSearchDefinition = new ConfigurableSearchDefinition(
                ColumnEntity.QUEUE_GROUPING, criteriaProjections, mapGroupSearchTerms);
        configurableSearchDefinition.setAddRowsListenerFactory(
                new ConfigurableSearchDefinition.AddRowsListenerFactory() {
                    @Override
                    public Map<String, ConfigurableList.AddRowsListener> getAddRowsListeners() {
                        Map<String, ConfigurableList.AddRowsListener> listeners = new HashMap<>();
                        listeners.put(SampleDataFetcherAddRowsListener.class.getSimpleName(),
                                new SampleDataFetcherAddRowsListener());
                        return listeners;
                    }
                });

        return configurableSearchDefinition;
    }

    private List<SearchTerm> buildIds() {
        List<SearchTerm> searchTerms = new ArrayList<>();
        {
            SearchTerm searchTerm = new SearchTerm();
            searchTerm.setName("Text");
            searchTerm.setIsDefaultResultColumn(Boolean.TRUE);
            SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
            criteriaPath.setPropertyName("queueGroupingText");
            searchTerm.setCriteriaPaths(Collections.singletonList(criteriaPath));
            searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
                @Override
                public String evaluate(Object entity, SearchContext context) {
                    QueueGrouping queueGrouping = (QueueGrouping) entity;
                    return queueGrouping.getQueueGroupingText();
                }
            });
            searchTerms.add(searchTerm);
        }
        {
            SearchTerm searchTerm = new SearchTerm();
            searchTerm.setName("Barcode");
            searchTerm.setRackScanSupported(Boolean.TRUE);
            SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
            criteriaPath.setCriteria(Arrays.asList("queuedEntities", "queueGrouping", "queuedEntities", "labVessel"));
            criteriaPath.setPropertyName("label");
            searchTerm.setCriteriaPaths(Collections.singletonList(criteriaPath));
            searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
                @Override
                public List<String> evaluate(Object entity, SearchContext context) {
                    QueueGrouping queueGrouping = (QueueGrouping) entity;
                    List<String> results = new ArrayList<>();
                    for (QueueEntity queuedEntity : queueGrouping.getQueuedEntities()) {
                        results.add(queuedEntity.getLabVessel().getLabel());
                    }

                    return results;
                }
            });
            searchTerms.add(searchTerm);
        }

        return searchTerms;
    }
}
