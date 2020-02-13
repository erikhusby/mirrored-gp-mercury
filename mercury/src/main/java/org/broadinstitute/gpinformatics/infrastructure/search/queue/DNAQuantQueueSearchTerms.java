package org.broadinstitute.gpinformatics.infrastructure.search.queue;


import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchDefinitionFactory;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchTerm;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DNAQuantQueueSearchTerms extends AbstractQueueSearchTerms {

    public DNAQuantQueueSearchTerms() {
    }

    public enum DNA_QUANT_TERMS {
        MANUFACTURER_BARCODE("Manufacturer Barcode"),
        SAMPLE_ID("Sample ID"),
        CONTAINER_INFO("Container Information");

        String term;

        DNA_QUANT_TERMS(String term) {
            this.term = term;
        }

        public String getTerm() {
            return term;
        }
    }

    /**
     * Add the search terms that are allowed
     */
    protected void addSearchTerms() {
        {
            SearchTerm searchTerm = new SearchTerm();
            searchTerm.setName(DNA_QUANT_TERMS.MANUFACTURER_BARCODE.getTerm());
            searchTerm.setRackScanSupported(Boolean.TRUE);
            SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();

            searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
                @Override
                public String evaluate(Object entity, SearchContext context) {
                    QueueEntity queueEntity = (QueueEntity) entity;

                    return queueEntity.getLabVessel().getLabel();
                }
            });

            criteriaPath.setCriteria(Arrays.asList("ManufacturerBarcode", "labVessel"));
            criteriaPath.setPropertyName("label"); // might need to be lab_vessel_id

            searchTerm.setCriteriaPaths(Collections.singletonList(criteriaPath));

            termDescriptionMap.put(searchTerm, "Manufacturer Barcode");
            mapNameToSearchTerm.put(searchTerm.getName(), searchTerm);
        }
        {
            SearchTerm searchTerm = new SearchTerm();
            searchTerm.setName("Queue Type");
            searchTerm.setRackScanSupported(Boolean.FALSE);

            searchTerm.setConstrainedValuesExpression(new SearchDefinitionFactory.QueueTypeValuesExpression());
            searchTerm.setSearchValueConversionExpression( new SearchDefinitionFactory.QueueTypeValueConversionExpression());
            SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();

            searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
                @Override
                public String evaluate(Object entity, SearchContext context) {
                    QueueEntity queueEntity = (QueueEntity) entity;
                    return queueEntity.getQueueGrouping().getAssociatedQueue().getQueueType().toString();
                }
            });

            criteriaPath.setCriteria(Arrays.asList("QueueType", "queueGrouping", "associatedQueue"));
            criteriaPath.setPropertyName("queueType");
            searchTerm.setCriteriaPaths(Collections.singletonList(criteriaPath));

            termDescriptionMap.put(searchTerm, "Queue Type");
            mapNameToSearchTerm.put(searchTerm.getName(), searchTerm);
        }
        {
            SearchTerm searchTerm = new SearchTerm();
            searchTerm.setName("Queue Entity Status");
            searchTerm.setRackScanSupported(Boolean.FALSE);
            searchTerm.setConstrainedValuesExpression(new SearchDefinitionFactory.QueueStatusValuesExpression());
            searchTerm.setSearchValueConversionExpression( new SearchDefinitionFactory.QueueStatusValueConversionExpression());

            SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();

            criteriaPath.setPropertyName("queueStatus");
            searchTerm.setCriteriaPaths(Collections.singletonList(criteriaPath));
            searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
                @Override
                public String evaluate(Object entity, SearchContext context) {
                    QueueEntity queueEntity = (QueueEntity) entity;

                    return queueEntity.getQueueStatus().getName();
                }
            });

            searchTerm.setCriteriaPaths(Collections.singletonList(criteriaPath));

            termDescriptionMap.put(searchTerm, "Queue Status");
            mapNameToSearchTerm.put(searchTerm.getName(), searchTerm);
        }
        {
            SearchTerm searchTerm = new SearchTerm();
            searchTerm.setName("Barcode");
            searchTerm.setRackScanSupported(Boolean.TRUE);
            List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
            SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
            criteriaPath.setCriteria(Collections.singletonList("labVessel"));
            criteriaPath.setPropertyName("label");
            criteriaPaths.add(criteriaPath);
            searchTerm.setCriteriaPaths(criteriaPaths);
            searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
                @Override
                public String evaluate(Object entity, SearchContext context) {
                    QueueEntity queueEntity = (QueueEntity) entity;
                    return queueEntity.getLabVessel().getLabel();
                }
            });
            termDescriptionMap.put(searchTerm, "Barcode");
            mapNameToSearchTerm.put(searchTerm.getName(), searchTerm);
        }
        {
            SearchTerm searchTerm = new SearchTerm();
            searchTerm.setName(DNA_QUANT_TERMS.SAMPLE_ID.getTerm());
            searchTerm.setRackScanSupported(Boolean.TRUE);
            List<SearchTerm.CriteriaPath> criteriaPaths = new ArrayList<>();
            SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
            criteriaPath.setCriteria(Arrays.asList("labVessel", "mercurySamples"));
            criteriaPath.setPropertyName("sampleKey");
            criteriaPaths.add(criteriaPath);
            searchTerm.setCriteriaPaths(criteriaPaths);
            searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
                @Override
                public List<String> evaluate(Object entity, SearchContext context) {
                    List<String> results = new ArrayList<>();
                    QueueEntity queueEntity = (QueueEntity) entity;
                    for (MercurySample mercurySample : queueEntity.getLabVessel().getMercurySamples()) {
                        results.add(mercurySample.getSampleKey());
                    }
                    return results;
                }
            });
            termDescriptionMap.put(searchTerm, "Barcode");
            mapNameToSearchTerm.put(searchTerm.getName(), searchTerm);
        }
        {
            SearchTerm searchTerm = new SearchTerm();
            searchTerm.setName(DNA_QUANT_TERMS.CONTAINER_INFO.getTerm());
            SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
            criteriaPath.setCriteria(Arrays.asList("ContainerInfo", "rackOfTubes", "tubeFormation", "labVessel", "queuedEntities"));

            searchTerm.setCriteriaPaths(Collections.singletonList(criteriaPath));
            searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
                @Override
                public List<String> evaluate(Object entity, SearchContext context) {
                    QueueEntity queueEntity = (QueueEntity) entity;
                    List<String> results = new ArrayList<>();
                    LabVessel labVessel = queueEntity.getLabVessel();

                    SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm");
                    context.setMultiValueDelimiter(", ");
                    context.setResultCellTargetPlatform(SearchContext.ResultCellTargetPlatform.WEB);

                    for (LabVessel container : labVessel.getContainers()) {
                        TubeFormation containerTubeFormation = (TubeFormation) container;

                        TubeFormation tubeFormation = OrmUtil.proxySafeCast(container, TubeFormation.class);

                        String vesselPositionInContainer = tubeFormation.getContainerRole().getPositionOfVessel(labVessel).name();
                        Date createdOn = containerTubeFormation.getCreatedOn();

                        Set<RackOfTubes> racksOfTubes = containerTubeFormation.getRacksOfTubes();
                        for (RackOfTubes rackOfTubes : racksOfTubes) {
                            results.add(rackOfTubes.getLabel() + "/" + vesselPositionInContainer + ":" + dateFormat.format(createdOn));
                        }
                    }

                    return results;
                }
            });
            termDescriptionMap.put(searchTerm, "Container Information");
            mapNameToSearchTerm.put(searchTerm.getName(), searchTerm);
        }
    }

    /**
     *
     * @return
     */
    public Set<SearchTerm> getAllowedSearchTerms() {
        Set<SearchTerm> allowed = new HashSet();
        // Loop through the terms and if there is a criteria path, then add it to the allowed terms.
        for (SearchTerm searchTerm : termDescriptionMap.keySet()) {
            List<SearchTerm.CriteriaPath> criteriaPaths = searchTerm.getCriteriaPaths();
            if(criteriaPaths != null && !criteriaPaths.isEmpty()) {
                allowed.add(searchTerm);
            }
        }
        return allowed;
    }

    /**
     * Utility method used to get all possible search terms. Note that SearchTerm with no criteriapath isn't expected
     * to be used to search with.
     *
     * @return
     */
    @Override
    public Set<SearchTerm> getAllowedTerms() {
        return termDescriptionMap.keySet();
    }

    @Override
    public List<String> getAllowedResultFields() {
        ArrayList resultFields = new ArrayList();
        resultFields.add("Rack Position");
        return resultFields;
    }

    @Override
    public List<String> getNotFoundResultRows() {
        return null;
    }
}
