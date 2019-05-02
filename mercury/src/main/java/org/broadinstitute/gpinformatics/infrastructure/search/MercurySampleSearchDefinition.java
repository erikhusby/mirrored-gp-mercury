package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnValueType;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.columns.DisplayExpression;
import org.broadinstitute.gpinformatics.infrastructure.columns.PassingFingerprintPlugin;
import org.broadinstitute.gpinformatics.infrastructure.columns.SampleDataFetcherAddRowsListener;
import org.broadinstitute.gpinformatics.infrastructure.columns.SampleMetadataPlugin;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint;
import org.broadinstitute.gpinformatics.mercury.entity.run.FpGenotype;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Builds ConfigurableSearchDefinition for mercury sample user defined search logic
 */
@SuppressWarnings("ReuseOfLocalVariable")
public class MercurySampleSearchDefinition {

    public ConfigurableSearchDefinition buildSearchDefinition() {
        Map<String, List<SearchTerm>> mapGroupSearchTerms = new LinkedHashMap<>();

        List<SearchTerm> searchTerms = buildSampleBatch();
        mapGroupSearchTerms.put("Batches", searchTerms);

        searchTerms = LabVesselSearchDefinition.buildBsp();
        mapGroupSearchTerms.put("BSP", searchTerms);

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
                ColumnEntity.MERCURY_SAMPLE, criteriaProjections, mapGroupSearchTerms);

        configurableSearchDefinition.setAddRowsListenerFactory(
                new ConfigurableSearchDefinition.AddRowsListenerFactory() {
                    @Override
                    public Map<String, ConfigurableList.AddRowsListener> getAddRowsListeners() {
                        Map<String, ConfigurableList.AddRowsListener> listeners = new HashMap<>();
                        listeners.put(SampleDataFetcherAddRowsListener.class.getSimpleName(), new SampleDataFetcherAddRowsListener());
                        return listeners;
                    }
                });
        return configurableSearchDefinition;
    }

    private List<SearchTerm> buildSampleBatch() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("PDO");
        searchTerm.setSearchValueConversionExpression(SearchDefinitionFactory.getPdoInputConverter());
        searchTerm.setDisplayValueExpression(new SamplePdoDisplayExpression(false));
        List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
        SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria( Arrays.asList( "PDOSamples", "productOrderSamples", "productOrder" ) );
        criteriaPath.setPropertyName("jiraTicketKey");
        criteriaPaths.add(criteriaPath);
        searchTerm.setCriteriaPaths(criteriaPaths);
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("PDO->(Sample Status)");
        searchTerm.setDisplayValueExpression(new SamplePdoDisplayExpression(true));
        searchTerms.add(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Lab Batch");
        criteriaPaths = new ArrayList<>();

        // Mercury only cares about workflow batches
        SearchTerm.ImmutableTermFilter workflowOnlyFilter = new SearchTerm.ImmutableTermFilter(
                "labBatchType", SearchInstance.Operator.EQUALS, LabBatch.LabBatchType.WORKFLOW);

        // Non-reworks
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("BatchVessels", "labVessel", "labBatches", "labBatch"));
        criteriaPath.setPropertyName("batchName");
        criteriaPath.addImmutableTermFilter(workflowOnlyFilter);
        criteriaPaths.add(criteriaPath);
        // Reworks
        criteriaPath = new SearchTerm.CriteriaPath();
        criteriaPath.setCriteria(Arrays.asList("BatchVessels", "labVessel", "reworkLabBatches"));
        criteriaPath.setPropertyName("batchName");
        criteriaPath.addImmutableTermFilter(workflowOnlyFilter);
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

        searchTerm = new SearchTerm();
        searchTerm.setName("Research Project");
        searchTerm.setDisplayValueExpression(new SamplePdoDisplayExpression() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                MercurySample sample = (MercurySample) entity;

                Set<ProductOrderSample> productOrderSamples = findPdoSamples(sample);

                Set<String> rpValues = new TreeSet<>();
                ResearchProject rp;

                for( ProductOrderSample productOrderSample : productOrderSamples ) {
                    rp = productOrderSample.getProductOrder().getResearchProject();
                    if( rp != null ) {
                        rpValues.add( rp.getName() + "[" + rp.getBusinessKey() + "]");
                    }
                }

                return rpValues;
            }
        });
        searchTerms.add(searchTerm);

        return searchTerms;
    }

    private List<SearchTerm> buildSampleSearch() {
        List<SearchTerm> searchTerms = new ArrayList<>();

        SearchTerm searchTerm = new SearchTerm();
        searchTerm.setName("Mercury Sample ID");
        searchTerm.setIsDefaultResultColumn(Boolean.TRUE);
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
        searchTerm.setName("Root Sample ID");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Set<String> evaluate(Object entity, SearchContext context) {
                Set<String> values = new HashSet<>();
                MercurySample sample = (MercurySample) entity;
                for( LabVessel sampleVessel : sample.getLabVessel() ){
                    for( SampleInstanceV2 sampleInstance : sampleVessel.getSampleInstancesV2()) {
                        values.add(sampleInstance.getRootOrEarliestMercurySampleName());
                    }
                }
                return values;
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
        for (Metadata.Key meta : Metadata.Key.values()) {
            if (meta.getCategory() == Metadata.Category.SAMPLE &&
                    BSPSampleSearchColumn.getByName(meta.getDisplayName()) == null) {
                searchTerm = new SearchTerm();
                searchTerm.setName(meta.getDisplayName());
                searchTerm.setDisplayExpression(DisplayExpression.METADATA);
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

        SearchTerm parentSearchTerm = new SearchTerm();
        parentSearchTerm.setName("Fingerprints");
        parentSearchTerm.setIsNestedParent(Boolean.TRUE);
        parentSearchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, SearchContext context) {
                MercurySample mercurySample = (MercurySample) entity;
                return mercurySample.getFingerprints().stream().sorted(
                        Comparator.comparing(Fingerprint::getDateGenerated)).collect(Collectors.toList());
            }
        });
        searchTerms.add(parentSearchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Date Generated");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, SearchContext context) {
                Fingerprint fingerprint = (Fingerprint) entity;
                return fingerprint.getDateGenerated();
            }
        });
        parentSearchTerm.addNestedEntityColumn(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("FP Genotype");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, SearchContext context) {
                Fingerprint fingerprint = (Fingerprint) entity;
                List<FpGenotype> fpGenotypesOrdered = fingerprint.getFpGenotypesOrdered();
                StringBuilder genotype = new StringBuilder(fpGenotypesOrdered.size() * 2);
                for (FpGenotype fpGenotype : fpGenotypesOrdered) {
                    if (fpGenotype != null) {
                        genotype.append(fpGenotype.getGenotype());
                    }
                }
                return genotype.toString();
            }
        });
        parentSearchTerm.addNestedEntityColumn(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Pass / Fail");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, SearchContext context) {
                Fingerprint fingerprint = (Fingerprint) entity;
                return fingerprint.getDisposition();
            }
        });
        parentSearchTerm.addNestedEntityColumn(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Gender");
        searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, SearchContext context) {
                Fingerprint fingerprint = (Fingerprint) entity;
                return fingerprint.getGender();
            }
        });
        parentSearchTerm.addNestedEntityColumn(searchTerm);

        searchTerm = new SearchTerm();
        searchTerm.setName("Passing Initial Fingerprint");
        searchTerm.setPluginClass(PassingFingerprintPlugin.class);
        searchTerms.add(searchTerm);

        return searchTerms;
    }

    /**
     * Share logic to find sample PDO with the added functionality for PDO result column to include delivery status
     */
    private class SamplePdoDisplayExpression extends SearchTerm.Evaluator<Object> {

        private boolean includeSampleStatus = false;

        public SamplePdoDisplayExpression(boolean includeSampleStatus) {
            this.includeSampleStatus = includeSampleStatus;
        }

        public SamplePdoDisplayExpression(){}

        @Override
        public Set<String> evaluate(Object entity, SearchContext context) {
            MercurySample sample = (MercurySample) entity;

            Set<ProductOrderSample> productOrderSamples = findPdoSamples(sample);

            Set<String> results = new TreeSet<>();
            String jiraTicketKey;
            String sampleDeliveryStatus;

            for( ProductOrderSample productOrderSample : productOrderSamples ) {
                jiraTicketKey = productOrderSample.getProductOrder().getJiraTicketKey();
                sampleDeliveryStatus = productOrderSample.getDeliveryStatus().getDisplayName();
                if( includeSampleStatus && !sampleDeliveryStatus.isEmpty()) {
                    results.add(jiraTicketKey + "->(" + sampleDeliveryStatus + ")");
                } else {
                    results.add( jiraTicketKey );
                }
            }

            return results;
        }

        protected Set<ProductOrderSample> findPdoSamples(MercurySample sample){

            // If sample is directly associated with a PDO, only use the direct association
            Set<ProductOrderSample> productOrderSamples = sample.getProductOrderSamples();
            if (!productOrderSamples.isEmpty()) {
                return productOrderSamples;
            } else {
                // Otherwise, use all PDO samples found via SampleInstanceV2 ancestry
                Set<LabVessel> sampleVessels = sample.getLabVessel();
                for( LabVessel sampleVessel : sampleVessels ) {
                    for( SampleInstanceV2 sampleInstanceV2 : sampleVessel.getSampleInstancesV2() ) {
                        productOrderSamples.addAll( sampleInstanceV2.getAllProductOrderSamples() );
                    }
                }
            }
            return productOrderSamples;
        }
    }
}
