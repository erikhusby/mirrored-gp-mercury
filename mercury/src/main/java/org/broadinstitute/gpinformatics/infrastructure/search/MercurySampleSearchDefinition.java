package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnValueType;
import org.broadinstitute.gpinformatics.infrastructure.columns.SampleMetadataPlugin;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds ConfigurableSearchDefinition for mercury sample user defined search logic
 */
public class MercurySampleSearchDefinition {

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

        criteriaProjections.add( new ConfigurableSearchDefinition.CriteriaProjection("BatchVessels",
                "mercurySampleId", "labVessel", MercurySample.class));

        criteriaProjections.add( new ConfigurableSearchDefinition.CriteriaProjection("SampleID",
                "mercurySampleId", "mercurySampleId", MercurySample.class));

        criteriaProjections.add( new ConfigurableSearchDefinition.CriteriaProjection("mercurySample",
                "mercurySampleId", "metadata", MercurySample.class));

        ConfigurableSearchDefinition configurableSearchDefinition = new ConfigurableSearchDefinition(
                ColumnEntity.MERCURY_SAMPLE, 100, criteriaProjections, mapGroupSearchTerms);

        return configurableSearchDefinition;
    }

    private List<SearchTerm> buildSampleBatch() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("PDO");
        searchTerm.setSearchValueConversionExpression(SearchDefinitionFactory.getPdoInputConverter());
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria( Arrays.asList( "PDOSamples", "productOrderSamples", "productOrder" ) );
        criteriaPath.setPropertyName("jiraTicketKey");
        criteriaPaths.add(criteriaPath);

        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public List<String> evaluate(Object entity, SearchContext context) {
                MercurySample sample = (MercurySample) entity;
                Set<ProductOrderSample> productOrderSamples = sample.getProductOrderSamples();
                if (!productOrderSamples.isEmpty()) {
                    List<String> results = new ArrayList<>();
                    for( ProductOrderSample productOrderSample : productOrderSamples ) {
                        results.add( productOrderSample.getProductOrder().getJiraTicketKey() );
                    }
                    return results;
                }
                return null;
            }
        });
        searchTerms.add(searchTerm);


        searchTerm = new SearchTerm();
        searchTerm.setName("LCSET");
        searchTerm.setSearchValueConversionExpression(SearchDefinitionFactory.getLcsetInputConverter());
        criteriaPaths = new ArrayList<>();
        // Non-reworks
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("BatchVessels", "labVessel", "labBatches", "labBatch"));
        criteriaPath.setPropertyName("batchName");
        criteriaPaths.add(criteriaPath);
        // Reworks
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("BatchVessels", "labVessel", "reworkLabBatches"));
        criteriaPath.setPropertyName("batchName");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                MercurySample sample = (MercurySample) entity;
                Set<String> results = new HashSet<>();
                for( LabVessel sampleVessel : sample.getLabVessel() ) {
                    for (SampleInstanceV2 sampleInstanceV2 : sampleVessel.getSampleInstancesV2()) {
                        for( LabBatch labBatch : sampleInstanceV2.getAllWorkflowBatches() ) {
                            results.add(labBatch.getBatchName());
                        }
                    }
                }
                return results;
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Bucket Count");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Long evaluate(Object entity, SearchContext context) {
                MercurySample sample = (MercurySample) entity;
                int result = 0;
                for (LabVessel sampleVessel : sample.getLabVessel()) {
                    result += sampleVessel.getBucketEntriesCount();
                }
                return new Long(result);
            }
        });
        searchTerms.add(searchTerm);

        return searchTerms;
    }

    private List<SearchTerm> buildSampleSearch() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("Mercury Sample ID");
        searchTerm.setDbSortPath("sampleKey");
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("SampleID"));
        criteriaPath.setPropertyName("sampleKey");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                MercurySample sample = (MercurySample) entity;
                return sample.getSampleKey();
            }
        });
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Mercury Sample Tube Barcode");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public List<String> evaluate(Object entity, SearchContext context) {
                List<String> values = new ArrayList<>();
                MercurySample sample = (MercurySample) entity;
                for( LabVessel vessel : sample.getLabVessel() ){
                    values.add(vessel.getLabel());
                }
                return values;
            }
        });
        searchTerms.add(searchTerm);

        // ***** Build sample metadata child search term (the metadata value) ***** //
        List<SearchTerm> childSearchTerms = new ArrayList<>();
        searchTerm = new SearchTerm();
        searchTerm.setName("Metadata Value");
        // This is needed to show the selected metadata column term in the results.
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                String values = "";

                SearchInstance.SearchValue searchValue = context.getSearchValue();
                String header = searchValue.getParent().getValues().get(0);
                Metadata.Key key = Metadata.Key.valueOf(header);

                MercurySample sample = (MercurySample) entity;
                Set<Metadata> metadataSet = sample.getMetadata();
                for (Metadata meta : metadataSet) {
                    if (meta.getKey() == key) {
                        values += meta.getValue();
                        break;
                    }
                }
                return values;
            }
        });
        searchTerm.setViewHeaderExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, SearchContext context) {
                String header;
                SearchInstance.SearchValue searchValue = context.getSearchValue();
                header = searchValue.getParent().getValues().get(0);
                Metadata.Key key = Metadata.Key.valueOf(header);
                if( key != null ) {
                    return key.getDisplayName();
                } else {
                    return header;
                }
            }
        });
        searchTerm.setValueTypeExpression(new SearchTerm.Evaluator<ColumnValueType>() {
            @Override
            public ColumnValueType evaluate(Object entity, SearchContext context) {
                SearchInstance.SearchValue searchValue = context.getSearchValue();
                String metaName = searchValue.getParent().getValues().get(0);
                Metadata.Key key = Metadata.Key.valueOf(metaName);
                switch (key.getDataType()) {
                case STRING:
                    return ColumnValueType.STRING;
                case NUMBER:
                    return ColumnValueType.TWO_PLACE_DECIMAL;
                case DATE:
                    return ColumnValueType.DATE;
                }
                throw new RuntimeException("Unhandled data type " + key.getDataType());
            }
        });
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("mercurySample", "metadata"));
        criteriaPath.setPropertyNameExpression(new SearchTerm.Evaluator<String>() {
            @Override
            // Defensive coding
            //   - as of 10/02/2014 sample metadata values are stored in value column (JPA aliased as "stringValue").
            public String evaluate(Object entity, SearchContext context) {
                SearchInstance.SearchValue searchValue = context.getSearchValue();
                String metaName = searchValue.getParent().getValues().get(0);
                Metadata.Key key = Metadata.Key.valueOf(metaName);
                switch (key.getDataType()) {
                case STRING:
                    return "stringValue";
                case NUMBER:
                    return "numberValue";
                case DATE:
                    return "dateValue";
                }
                throw new RuntimeException("Unhandled data type " + key.getDataType());
            }
        });
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        childSearchTerms.add(searchTerm);

        // *****  Build parent search term (the metadata name) ***** //
        searchTerm = new SearchTerm();
        searchTerm.setName("Sample Metadata");
        searchTerm.setConstrainedValuesExpression(new SearchTerm.Evaluator<List<ConstrainedValue>>() {
            @Override
            public List<ConstrainedValue> evaluate(Object entity, SearchContext context) {
                List<ConstrainedValue> constrainedValues = new ArrayList<>();
                for (Metadata.Key meta : Metadata.Key.values()) {
                    if (meta.getCategory() == Metadata.Category.SAMPLE) {
                        constrainedValues.add(new ConstrainedValue(meta.toString(), meta.getDisplayName()));
                    }
                }
                Collections.sort(constrainedValues);
                return constrainedValues;
            }
        });
        searchTerm.setSearchValueConversionExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Metadata.Key evaluate(Object entity, SearchContext context) {
                return Enum.valueOf(Metadata.Key.class, context.getSearchValueString());
            }
        });
        // Don't want this option in selectable columns
        searchTerm.setIsExcludedFromResultColumns(Boolean.TRUE);
        searchTerm.setDependentSearchTerms(childSearchTerms);
        searchTerm.setAddDependentTermsToSearchTermList(Boolean.TRUE);
        criteriaPaths = new ArrayList<>();
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("mercurySample", "metadata"));
        criteriaPath.setPropertyName("key");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        // ******** Allow individual selectable result columns for each sample metadata value *******
        SearchDefinitionFactory.SampleMetadataDisplayExpression sampleMetadataDisplayExpression = new SearchDefinitionFactory.SampleMetadataDisplayExpression();
        for (Metadata.Key meta : Metadata.Key.values()) {
            if (meta.getCategory() == Metadata.Category.SAMPLE) {
                searchTerm = new SearchTerm();
                searchTerm.setName(meta.getDisplayName());
                searchTerm.setDisplayValueExpression(sampleMetadataDisplayExpression);
                searchTerms.add(searchTerm);
            }
        }

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
