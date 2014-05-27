package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configurable search definition for LabVessel.
 */
public class SearchDefinitionFactory {

    private Map<String, ConfigurableSearchDefinition> mapNameToDef = new HashMap<>();

    public ConfigurableSearchDefinition getForEntity(String entity) {
        if (mapNameToDef.isEmpty()) {
            ConfigurableSearchDefinition configurableSearchDefinition = buildLabVesselSearchDef();
            mapNameToDef.put(configurableSearchDefinition.getName(), configurableSearchDefinition);
        }
        return mapNameToDef.get(entity);
    }

    @SuppressWarnings("FeatureEnvy")
    public ConfigurableSearchDefinition buildLabVesselSearchDef() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("LCSET");
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("bucketEntries", "labBatch"));
        criteriaPath.setPropertyName("batchName");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                List<String> results = new ArrayList<>();
                LabVessel labVessel = (LabVessel) entity;
                for (BucketEntry bucketEntry : labVessel.getBucketEntries()) {
                    if (bucketEntry.getLabBatch() != null) {
                        results.add(bucketEntry.getLabBatch().getBatchName());
                    }
                }
                return results;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Label");
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setPropertyName("label");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                LabVessel labVessel = (LabVessel) entity;
                return labVessel.getLabel();
            }
        });
        searchTerms.add(searchTerm);

        Map<String, List<SearchTerm>> mapGroupSearchTerms = new HashMap<>();
        mapGroupSearchTerms.put("IDs", searchTerms);

        List<ConfigurableSearchDefinition.CriteriaProjection> criteriaProjections = new ArrayList<>();
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("bucketEntries", "labVesselId",
                "labVessel", BucketEntry.class.getName()));
        ConfigurableSearchDefinition configurableSearchDefinition = new ConfigurableSearchDefinition(
                "LabVessel", LabVessel.class.getName(), "label", 50, criteriaProjections, mapGroupSearchTerms);
        mapNameToDef.put(configurableSearchDefinition.getName(), configurableSearchDefinition);
        return configurableSearchDefinition;
    }
}
