package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.columns.BspSampleSearchAddRowsListener;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
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

        // Are there alternatives to search terms that aren't searchable?  Should they be in a different structure, then merged with search terms for display?

        // How to batch BSP access?  Need a pre-process phase: if there are any BSP columns then gather sample IDs and all columns; make web service call and cache results.

        // XX version - from workflow? 3.2 doesn't seem to be in XML
        // Start date - LabBatch.createdOn? usually 1 day before "scheduled to start"
        // Due date - LabBatch.dueDate is transient!
        // Stock sample ID - from BSP, not searchable
        searchTerm = new SearchTerm();
        searchTerm.setName("Label");
        searchTerm.setDisplayExpression(new SearchTerm.Evaluator<Object>() {
            @Override
            public Object evaluate(Object entity, Map<String, Object> context) {
                LabVessel labVessel = (LabVessel) entity;
                MercurySample mercurySample = labVessel.getMercurySamples().iterator().next();
                BspSampleSearchAddRowsListener bspColumns = (BspSampleSearchAddRowsListener) context.get(
                        BspSampleSearchAddRowsListener.BSP_LISTENER);
                return bspColumns.getColumn(mercurySample.getSampleKey(), BSPSampleSearchColumn.STOCK_SAMPLE);
            }
        });
        searchTerms.add(searchTerm);

        // Raising volume to 65ul - sample annotation?
        // Sample History (1st Plating, Rework from Shearing, Rework from LC, Rework from Pooling, 2nd Plating) - from bucket entries?
        // Collaborator sample ID - from BSP
        // Collaborator Participant ID - from BSP
        // Tumor / Normal - from BSP
        // Collection - from BSP
        // PDO - from bucket entry
        // Original Material Type - from BSP
        // Stock Sample Initial ng - from BSP?
        // Billing Risk - from PDO
        // Original LCSET - if rework, get non-rework bucket entry
        // Kapa QC Score - uploaded
        // Pull sample if low input and Kapa QC? - sample annotation?
        // Exported Sample Well - from export lab batch
        // Exported/Daughter Sample Tube Barcode - ditto
        // Exported/Daughter Sample ID - ditto
        // Exported/Daughter Volume - ?
        // Exported/Daughter Concentration - ?
        // Calculated ng into shearing - ?
        // Lab Risk - sample annotation?
        // Requested Sequencing Deliverable - from PDO?
        // Pond Sample Well - from lab event
        // Pond Tube Barcode - ditto
        // Pond Quant - upload
        // Rework low pond - sample annotation?
        // Index adapter - reagent?
        // Remaining Pond Volume after Plex / SPRI Concentration - ?
        // Plex Tube Barcode - lab event
        // Plexed SPRI Concentration Tube Barcode - lab event
        // MiSeq Sample Barcode - lab event
        // Per Pool Eco Quant for MiSeq Sample - lab event
        // Normalization Tube Barcode for MiSeq Sample - lab event
        // Denature Barcode for MiSeq Sample - lab event
        // % representation of each sample within each plex - ?
        // SPRI Concentration tube / Catch Tube Pooling Penalty - ?
        // Catch Sample Well - lab event
        // Catch Tube Barcode - lab event
        // Catch Quant - upload
        // Rework Low Catch - sample annotation?
        // Per Plex Eco Quant for Catch Tube - ?
        // Normalization Tube Barcode for Catch Tube - lab event
        // Denature IDs and which samples are in those tubes - lab event
        // Total lanes sequenced per Denature tube - ?
        // HiSeq FCTs and Barcode and which samples were on those - partly lab batch
        // Mean Target Coverage - PDO?
        // % Target Bases 20x - PDO?
        // Penalty 20x - PDO?
        // HS Library Size - ?
        // PF Reads - ?
        // % Duplication - ?
        // % Contamination - ?
        // Status on requested coverage - ?
        // Total Lanes Sequenced per Sample - ?
        // Comments - sample annotation?

        List<ConfigurableSearchDefinition.CriteriaProjection> criteriaProjections = new ArrayList<>();
        criteriaProjections.add(new ConfigurableSearchDefinition.CriteriaProjection("bucketEntries", "labVesselId",
                "labVessel", BucketEntry.class.getName()));
        ConfigurableSearchDefinition configurableSearchDefinition = new ConfigurableSearchDefinition(
                "LabVessel", LabVessel.class.getName(), "label", 50, criteriaProjections, mapGroupSearchTerms);
        mapNameToDef.put(configurableSearchDefinition.getName(), configurableSearchDefinition);
        return configurableSearchDefinition;
    }
}
