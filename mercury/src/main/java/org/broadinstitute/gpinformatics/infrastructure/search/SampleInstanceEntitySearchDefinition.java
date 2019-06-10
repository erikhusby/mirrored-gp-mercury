package org.broadinstitute.gpinformatics.infrastructure.search;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnValueType;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.AnalysisType;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
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

    public ConfigurableSearchDefinition buildSearchDefinition() {
        Map<String, List<SearchTerm>> groupToSearchTerms = new LinkedHashMap<>();
        groupToSearchTerms.put(" ", sampleInstanceEntitySearchTerms());

        List<ConfigurableSearchDefinition.CriteriaProjection> criteriaJoins = new ArrayList<>();
        criteriaJoins.add(new ConfigurableSearchDefinition.CriteriaProjection("ByTubeLabel",
                "labVessel", "labVesselId", LabVessel.class));
        criteriaJoins.add(new ConfigurableSearchDefinition.CriteriaProjection("BySampleName",
                "mercurySample", "mercurySampleId", MercurySample.class));

        ConfigurableSearchDefinition configurableSearchDefinition = new ConfigurableSearchDefinition(
                ColumnEntity.EXTERNAL_LIBRARY, criteriaJoins, groupToSearchTerms);

        return configurableSearchDefinition;
    }

    private List<SearchTerm> sampleInstanceEntitySearchTerms() {
        List<SearchTerm> terms = new ArrayList<>();
        SearchTerm term;

        // Search on Sample Tube Barcode.
        term = vesselEvaluator("Sample Tube Barcode", LabVessel::getLabel);
        term.setCriteriaPaths(new ArrayList<SearchTerm.CriteriaPath>() {{
            add(new SearchTerm.CriteriaPath());
            get(0).setPropertyName("label");
            get(0).setCriteria(Collections.singletonList("ByTubeLabel"));
        }});
        term.setIsDefaultResultColumn(Boolean.TRUE);
        terms.add(term);

        // Search on Broad Sample ID.
        term = sampleInstanceEntityEvaluator("Broad Sample ID", entity -> entity.getMercurySample() == null ?
                "" : entity.getMercurySample().getSampleKey());
        term.setCriteriaPaths(new ArrayList<SearchTerm.CriteriaPath>() {{
            add(new SearchTerm.CriteriaPath());
            get(0).setPropertyName("sampleKey");
            get(0).setCriteria(Collections.singletonList("BySampleName"));
        }});
        term.setDbSortPath("mercurySample.sampleKey");
        terms.add(term);

        // Search on Library Name.
        term = sampleInstanceEntityEvaluator("Library Name", SampleInstanceEntity::getLibraryName);
        term.setCriteriaPaths(new ArrayList<SearchTerm.CriteriaPath>() {{
            add(new SearchTerm.CriteriaPath());
            get(0).setPropertyName("libraryName");
        }});
        term.setDbSortPath("libraryName");
        terms.add(term);

        // Search on aggregation particle.
        term = sampleInstanceEntityEvaluator("Data Aggregator/Project Title",
                SampleInstanceEntity::getAggregationParticle);
        term.setCriteriaPaths(new ArrayList<SearchTerm.CriteriaPath>() {{
            add(new SearchTerm.CriteriaPath());
            get(0).setPropertyName("aggregationParticle");
        }});
        terms.add(term);

        // Search on spreadsheet upload date.
        term = new SearchTerm();
        term.setName("Upload Date");
        term.setValueType(ColumnValueType.DATE);
        term.setCriteriaPaths(new ArrayList<SearchTerm.CriteriaPath>() {{
            add(new SearchTerm.CriteriaPath());
            get(0).setPropertyName("uploadDate");
        }});
        term.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Date evaluate(Object entity, SearchContext context) {
                return ((SampleInstanceEntity)entity).getUploadDate();
            }
        });
        terms.add(term);

        // The display-only terms.
        terms.add(sampleDataEvaluator("Collaborator Sample ID", Metadata.Key.SAMPLE_ID,
                SampleData::getCollaboratorsSampleName));
        terms.add(sampleDataEvaluator("Individual Name (Patient Id)", Metadata.Key.PATIENT_ID,
                SampleData::getCollaboratorParticipantId));
        terms.add(sampleDataEvaluator("Sex", Metadata.Key.GENDER, SampleData::getGender));
        terms.add(sampleDataEvaluator("Organism", Metadata.Key.SPECIES, SampleData::getOrganism));
        terms.add(sampleDataEvaluator("Root Sample", Metadata.Key.ROOT_SAMPLE, SampleData::getRootSample));
        terms.add(sampleInstanceEntityEvaluator("Molecular Barcode Name", sampleInstanceEntity -> {
            MolecularIndexingScheme mis = sampleInstanceEntity.getMolecularIndexingScheme();
            return mis == null ? "" : mis.getName();
        }));
        terms.add(sampleInstanceEntityEvaluator("Bait", SampleInstanceEntity::getBaitName));
        terms.add(sampleInstanceEntityEvaluator("Read Length 1", sampleInstanceEntity ->
                sampleInstanceEntity.getReadLength1() == null ?
                        "" : String.valueOf(sampleInstanceEntity.getReadLength1())));
        terms.add(sampleInstanceEntityEvaluator("Read Length 2", sampleInstanceEntity ->
                sampleInstanceEntity.getReadLength2() == null ?
                        "" : String.valueOf(sampleInstanceEntity.getReadLength2())));
        terms.add(sampleInstanceEntityEvaluator("Index Length 1", sampleInstanceEntity ->
                sampleInstanceEntity.getIndexLength1() == null ?
                        "" : String.valueOf(sampleInstanceEntity.getIndexLength1())));
        terms.add(sampleInstanceEntityEvaluator("Index Length 2", sampleInstanceEntity ->
                sampleInstanceEntity.getIndexLength2() == null ?
                        "" : String.valueOf(sampleInstanceEntity.getIndexLength2())));
        terms.add(sampleInstanceEntityEvaluator("Index type", sampleInstanceEntity ->
                sampleInstanceEntity.getIndexType() == null ?
                        "" : sampleInstanceEntity.getIndexType().getDisplayName()));
        terms.add(sampleInstanceEntityEvaluator("UMIs Present", sampleInstanceEntity ->
                BooleanUtils.isTrue(sampleInstanceEntity.getUmisPresent()) ? "Y" : "N"));
        terms.add(sampleInstanceEntityEvaluator("Paired End Read", sampleInstanceEntity ->
                BooleanUtils.isTrue(sampleInstanceEntity.getPairedEndRead()) ? "Y" : "N"));
        terms.add(sampleInstanceEntityEvaluator("Insert Size Range", SampleInstanceEntity::getInsertSize));
        terms.add(vesselEvaluator("Volume", tube ->
                (tube.getVolume() == null) ? "" : tube.getVolume().toPlainString()));
        terms.add(vesselEvaluator("Concentration (ng/uL)", tube ->
                (tube.getConcentration() == null) ? "" : tube.getConcentration().toPlainString()));
        terms.add(vesselEvaluator("Fragment Size", tube -> {
            LabMetric metric = tube.findMostRecentLabMetric(LabMetric.MetricType.FINAL_LIBRARY_SIZE);
            return (metric == null) ? "" : metric.getValue().toPlainString();
        }));
        terms.add(sampleInstanceEntityEvaluator("Reference Sequence", sampleInstanceEntity -> {
            ReferenceSequence referenceSequence = sampleInstanceEntity.getReferenceSequence();
            return referenceSequence == null ? "" : referenceSequence.getBusinessKey();
        }));
        terms.add(sampleInstanceEntityEvaluator("Sequencing Technology", sampleInstanceEntity -> {
            IlluminaFlowcell.FlowcellType flowcellType = sampleInstanceEntity.getSequencerModel();
            return flowcellType == null ? "" : flowcellType.getExternalUiName();
        }));
        terms.add(sampleInstanceEntityEvaluator("Aggregation Data Type", SampleInstanceEntity::getAggregationDataType));
        terms.add(sampleInstanceEntityEvaluator("Data Analysis Type", sampleInstanceEntity -> {
            AnalysisType analysisType = sampleInstanceEntity.getAnalysisType();
            return analysisType == null ? "" : analysisType.getBusinessKey();
        }));
        return terms;
    }

    /** Returns the display term that references the sampleInstanceEntity. */
    public SearchTerm sampleInstanceEntityEvaluator(String name, Function<SampleInstanceEntity, String> valueExtractor) {
        SearchTerm term = new SearchTerm();
        term.setName(name);
        term.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                return StringUtils.trimToEmpty(valueExtractor.apply((SampleInstanceEntity) entity));
            }
        });
        return term;
    }
    /** Returns the display term that references the labVessel. */
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

    /** Returns the display term for a sample metadata field. Handles both MercurySample and BSP sample metadata. */
    private SearchTerm sampleDataEvaluator(String name, Metadata.Key key, Function<SampleData, String> bspFunction) {
        SearchTerm term = new SearchTerm();
        term.setName(name);
        term.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public String evaluate(Object entity, SearchContext context) {
                MercurySample sample = ((SampleInstanceEntity) entity).getMercurySample();
                return sample == null ? "" :
                        StringUtils.trimToEmpty(sample.getMetadataSource() == MercurySample.MetadataSource.BSP ?
                                bspFunction.apply(sample.getSampleData()) : sample.getMetadataValueForKey(key));
            }});
        return term;
    }
}
