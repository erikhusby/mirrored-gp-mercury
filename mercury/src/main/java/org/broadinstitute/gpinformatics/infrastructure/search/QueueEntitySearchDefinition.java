package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.columns.SampleDataFetcherAddRowsListener;
import org.broadinstitute.gpinformatics.infrastructure.search.queue.DNAQuantQueueSearchTerms;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QueueEntitySearchDefinition {

    public static final DNAQuantQueueSearchTerms QUEUE_ENTITY_SEARCH_TERMS = new DNAQuantQueueSearchTerms();

    /**
     * Build a UDS based off the search terms selected by the user.
     *
     * @return Constructed search definition.
     */
    public ConfigurableSearchDefinition buildSearchDefinition() {
        HashMap<String, List<SearchTerm>> mapGroupSearchTerms = new HashMap<>();

        mapGroupSearchTerms.put("IDs", new ArrayList<>(QUEUE_ENTITY_SEARCH_TERMS.getAllowedTerms()));

        List<ConfigurableSearchDefinition.CriteriaProjection> criteriaProjections = new ArrayList<>();

        // todo notes to understand. Projection is setting up the columns being returned...
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("ManufacturerBarcode", "labVessel",
        "labVessel", QueueEntity.class));

        // TODO I believe this need to do a queue type restriction by doing a 'and' based off queueGrouping that are in a specific queue type.
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("QueueType", "queueGrouping",
                "queueGrouping", QueueEntity.class));

        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("QueueEntityStatus", "queueStatus",
                "queueStatus", QueueEntity.class));

        // Note that RackOfTubes inherits from LabVessel. The container ID would be the label.
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("ContainerInfo", "label",
                "label", TubeFormation.class));


        ConfigurableSearchDefinition configurableSearchDefinition = new ConfigurableSearchDefinition(
                ColumnEntity.QUEUE_ENTITY, criteriaProjections, mapGroupSearchTerms);
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

    public Set<SearchTerm> getAllowedSearchTerms() {
        return QUEUE_ENTITY_SEARCH_TERMS.getAllowedTerms();
    }
}
