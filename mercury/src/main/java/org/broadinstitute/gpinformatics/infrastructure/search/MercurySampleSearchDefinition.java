package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.SampleMetadataPlugin;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds ConfigurableSearchDefinition for mercury sample user defined search logic
 */
public class MercurySampleSearchDefinition extends EntitySearchDefinition {

    @Override
    public ConfigurableSearchDefinition buildSearchDefinition() {
        Map<String, List<SearchTerm>> mapGroupSearchTerms = new LinkedHashMap<>();

        List<SearchTerm> searchTerms = buildSampleBatch();
        mapGroupSearchTerms.put("Batches", searchTerms);

        searchTerms = buildSampleSearch();
        mapGroupSearchTerms.put("Mercury Samples", searchTerms);

        searchTerms = buildMultiColumnSearch();
        mapGroupSearchTerms.put("Multi-Columns", searchTerms);

        List<ConfigurableSearchDefinition.CriteriaProjection> criteriaProjections = new ArrayList<>();

        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("PDOSamples",
                "mercurySampleId", "productOrderSamples", MercurySample.class));

        criteriaProjections.add( new ConfigurableSearchDefinition.CriteriaProjection("SampleBucketEntries",
                "mercurySampleId", "labVessel", MercurySample.class));

        criteriaProjections.add( new ConfigurableSearchDefinition.CriteriaProjection("SampleID",
                "mercurySampleId", "mercurySampleId", MercurySample.class));

        ConfigurableSearchDefinition configurableSearchDefinition = new ConfigurableSearchDefinition(
                ColumnEntity.MERCURY_SAMPLE, 100, criteriaProjections, mapGroupSearchTerms);

        return configurableSearchDefinition;
    }

    private List<SearchTerm> buildSampleBatch() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("PDO");
        searchTerm.setValueConversionExpression(getPdoInputConverter());
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria( Arrays.asList( "PDOSamples", "productOrderSamples", "productOrder" ) );
        criteriaPath.setPropertyName("jiraTicketKey");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                MercurySample sample = (MercurySample) entity;
                return sample.getProductOrderSamples().iterator().next().getProductOrder().getJiraTicketKey();
            }
        });
        searchTerms.add(searchTerm);


        searchTerm = new SearchTerm();
        searchTerm.setName("LCSET");
        searchTerm.setValueConversionExpression(getLcsetInputConverter());
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("SampleBucketEntries", "labVessel", "bucketEntries", "labBatch"));
        criteriaPath.setPropertyName("batchName");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                MercurySample sample = (MercurySample) entity;
                // Assumption that sample can't be added to the same batch more than once
                List<String> results = new ArrayList<>();
                for( LabVessel sampleVessel : sample.getLabVessel() ) {
                    for( BucketEntry bucket : sampleVessel.getBucketEntries() ) {
                        LabBatch batch = bucket.getLabBatch();
                        if( batch != null ) {
                            results.add(batch.getBatchName());
                        }
                    }
                }
                return results;
            }
        });
        searchTerms.add(searchTerm);

        return searchTerms;
    }

    private List<SearchTerm> buildSampleSearch() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("Mercury Sample ID");
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("SampleID"));
        criteriaPath.setPropertyName("sampleKey");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                MercurySample sample = (MercurySample) entity;

                return sample.getSampleKey();
            }
        });
        searchTerms.add(searchTerm);

        return searchTerms;
    }

    private List<SearchTerm> buildMultiColumnSearch() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("All Sample Metadata");
        searchTerm.setPluginClass(SampleMetadataPlugin.class);
        searchTerms.add(searchTerm);

        return searchTerms;
    }
}
