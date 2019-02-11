package org.broadinstitute.gpinformatics.infrastructure.search;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnValueType;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.AnalysisType;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntity;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Builds ConfigurableSearchDefinition for external library user defined search logic
 */
@SuppressWarnings("ReuseOfLocalVariable")
public class SampleInstanceEntitySearchDefinition {
    private final static FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("MM/dd/yyyy");

    public ConfigurableSearchDefinition buildSearchDefinition() {
        Map<String, List<SearchTerm>> groupToSearchTerms = new LinkedHashMap<>();
        groupToSearchTerms.put(" ", sampleInstanceEntitySearchTerms());

        List<ConfigurableSearchDefinition.CriteriaProjection> criteriaProjections = new ArrayList<>();
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("TUBE LABEL",
                "labVessel", "labVesselId", LabVessel.class));
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("SAMPLE NAME",
                "mercurySample", "mercurySampleId", MercurySample.class));

        ConfigurableSearchDefinition configurableSearchDefinition = new ConfigurableSearchDefinition(
                ColumnEntity.EXTERNAL_LIBRARY, criteriaProjections, groupToSearchTerms);

        return configurableSearchDefinition;
    }

    private List<SearchTerm> sampleInstanceEntitySearchTerms() {
        List<SearchTerm> terms = new ArrayList<>();
        SearchTerm term;

        // Search on sampleInstanceEntity.labVessel.label
        term = vesselEvaluator("Tube Barcode", LabVessel::getLabel);
        term.setCriteriaPaths(new ArrayList<SearchTerm.CriteriaPath>() {{
            SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
            criteriaPath.setPropertyName("label");
            criteriaPath.setCriteria(Collections.singletonList("TUBE LABEL"));
            add(criteriaPath);
        }});
        term.setIsDefaultResultColumn(Boolean.TRUE);
        terms.add(term);

        // Search on sampleInstanceEntity.mercurySample.sampleKey
        term = sampleEvaluator("Sample Name", MercurySample::getSampleKey);
        term.setCriteriaPaths(new ArrayList<SearchTerm.CriteriaPath>() {{
            SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
            criteriaPath.setPropertyName("sampleKey");
            criteriaPath.setCriteria(Collections.singletonList("SAMPLE NAME"));
            add(criteriaPath);
        }});
        term.setDbSortPath("mercurySample.sampleKey");
        terms.add(term);

        // Search on sampleInstanceEntity.libraryName which is a unique key, so one row is returned.
        term = new SearchTerm();
        term.setName("Library Name");
        term.setCriteriaPaths(new ArrayList<SearchTerm.CriteriaPath>() {{
            SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
            criteriaPath.setPropertyName("sampleLibraryName");
            add(criteriaPath);
        }});
        term.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                return ((SampleInstanceEntity)entity).getSampleLibraryName();
            }
        });
        term.setDbSortPath("sampleLibraryName");
        terms.add(term);

        // Search on sampleInstanceEntity.aggregationParticle
        term = new SearchTerm();
        term.setName("Aggregation Particle");
        term.setCriteriaPaths(new ArrayList<SearchTerm.CriteriaPath>() {{
            SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
            criteriaPath.setPropertyName("aggregationParticle");
            add(criteriaPath);
        }});
        term.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                return ((SampleInstanceEntity) entity).getAggregationParticle();
            }
        });
        terms.add(term);

        // Search on sampleInstanceEntity.uploadDate.
        term = new SearchTerm();
        term.setName("Upload Date");
        term.setCriteriaPaths(new ArrayList<SearchTerm.CriteriaPath>() {{
            SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
            criteriaPath.setPropertyName("uploadDate");
            add(criteriaPath);
        }});
        term.setValueType(ColumnValueType.DATE);
        term.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Date evaluate(Object entity, SearchContext context) {
                return ((SampleInstanceEntity)entity).getUploadDate();
            }
        });
        terms.add(term);

        // Column display terms.
        terms.add(sampleDataEvaluator("Collaborator Participant Id", Metadata.Key.PATIENT_ID,
                SampleData::getCollaboratorParticipantId));
        terms.add(sampleDataEvaluator("Collaborator Sample Id", Metadata.Key.SAMPLE_ID,
                SampleData::getCollaboratorsSampleName));
        terms.add(sampleDataEvaluator("Sex", Metadata.Key.GENDER, SampleData::getGender));
        terms.add(sampleDataEvaluator("Lsid", Metadata.Key.LSID, SampleData::getSampleLsid));
        terms.add(sampleDataEvaluator("Species", Metadata.Key.SPECIES, SampleData::getOrganism));
        terms.add(vesselEvaluator("Volume", tube ->
                (tube.getVolume() == null) ? "" : tube.getVolume().toPlainString()));
        terms.add(vesselEvaluator("Concentration", tube ->
                (tube.getConcentration() == null) ? "" : tube.getConcentration().toPlainString()));
        terms.add(vesselEvaluator("Fragment Size", tube -> {
            LabMetric metric = tube.findMostRecentLabMetric(LabMetric.MetricType.FINAL_LIBRARY_SIZE);
            return (metric == null) ? "" : metric.getValue().toPlainString();
        }));

        term = new SearchTerm();
        term.setName("Root Sample");
        term.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                MercurySample rootSample = ((SampleInstanceEntity) entity).getRootSample();
                return rootSample == null ? "" : rootSample.getSampleKey();
            }
        });
        terms.add(term);

        term = new SearchTerm();
        term.setName("Bait or Cat");
        term.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                ReagentDesign reagentDesign = ((SampleInstanceEntity) entity).getReagentDesign();
                return reagentDesign == null ? "" : reagentDesign.getDesignName();
            }
        });
        terms.add(term);

        term = new SearchTerm();
        term.setName("Molecular Index Name");
        term.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                MolecularIndexingScheme mis = ((SampleInstanceEntity) entity).getMolecularIndexingScheme();
                return mis == null ? "" : mis.getName();
            }
        });
        terms.add(term);

        term = new SearchTerm();
        term.setName("Analysis Type");
        term.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                AnalysisType analysisType = ((SampleInstanceEntity) entity).getAnalysisType();
                return analysisType == null ? "" : analysisType.getBusinessKey();
            }
        });
        terms.add(term);

        term = new SearchTerm();
        term.setName("Reference Sequence");
        term.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                ReferenceSequence referenceSequence = ((SampleInstanceEntity) entity).getReferenceSequence();
                return referenceSequence == null ? "" : referenceSequence.getBusinessKey();
            }
        });
        terms.add(term);


        term = new SearchTerm();
        term.setName("Sequencer Model");
        term.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                IlluminaFlowcell.FlowcellType flowcellType = ((SampleInstanceEntity) entity).getSequencerModel();
                return flowcellType == null ? "" : flowcellType.getSequencerModel();
            }
        });
        terms.add(term);

        term = new SearchTerm();
        term.setName("Library Type");
        term.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                return ((SampleInstanceEntity)entity).getLibraryType();
            }
        });
        terms.add(term);


        term = new SearchTerm();
        term.setName("Read Length");
        term.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                Integer value = ((SampleInstanceEntity)entity).getReadLength();
                return value == null ? "" : String.valueOf(value);
            }
        });
        terms.add(term);


        term = new SearchTerm();
        term.setName("Paired End Read");
        term.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                Boolean value = ((SampleInstanceEntity)entity).getPairedEndRead();
                return value == null ? "" : value.booleanValue() ? "Y" : "N";
            }
        });
        terms.add(term);


        term = new SearchTerm();
        term.setName("Number of Lanes");
        term.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                Integer value = ((SampleInstanceEntity)entity).getNumberLanes();
                return value == null ? "" : String.valueOf(value);
            }
        });
        terms.add(term);


        term = new SearchTerm();
        term.setName("Insert Size");
        term.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                return ((SampleInstanceEntity)entity).getInsertSize();
            }
        });
        terms.add(term);

        term = new SearchTerm();
        term.setName("Umis Present");
        term.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                Boolean value = ((SampleInstanceEntity)entity).getUmisPresent();
                return value == null ? "" : value.booleanValue() ? "Y" : "N";
            }
        });
        terms.add(term);


        term = new SearchTerm();
        term.setName("Comments");
        term.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                return ((SampleInstanceEntity)entity).getComments();
            }
        });
        terms.add(term);

        return terms;
    }

    public SearchTerm vesselEvaluator(String name, Function<LabVessel, String> valueExtractor) {
        SearchTerm term = new SearchTerm();
        term.setName(name);
        term.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                LabVessel labVessel = ((SampleInstanceEntity) entity).getLabVessel();
                return labVessel == null ? "" : StringUtils.trimToEmpty(valueExtractor.apply(labVessel));
            }
        });
        return term;
    }

    public SearchTerm sampleEvaluator(String name, Function<MercurySample, String> valueExtractor) {
        SearchTerm term = new SearchTerm();
        term.setName(name);
        term.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                MercurySample sample = ((SampleInstanceEntity) entity).getMercurySample();
                return sample == null ? "" : StringUtils.trimToEmpty(valueExtractor.apply(sample));
            }
        });
        return term;
    }

    public SearchTerm sampleDataEvaluator(String name, Metadata.Key key, Function<SampleData, String> bspFunction) {
        return sampleEvaluator(name, sample ->
            StringUtils.trimToEmpty(sample.getMetadataSource() == MercurySample.MetadataSource.BSP ?
                    bspFunction.apply(sample.getSampleData()) : sample.getMetadataValueForKey(key)));
    }
}
