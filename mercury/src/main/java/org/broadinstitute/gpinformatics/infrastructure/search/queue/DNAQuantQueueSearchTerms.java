package org.broadinstitute.gpinformatics.infrastructure.search.queue;


import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnValueType;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchDefinitionFactory;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchTerm;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;

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
        CONTAINER_INFO("Container Information"), // Displaying container information.
        CONTAINER_BARCODE("Container Barcode"); // Searching by container ID.

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

            termDescriptionMap.put(searchTerm, DNA_QUANT_TERMS.MANUFACTURER_BARCODE.getTerm());
            mapNameToSearchTerm.put(searchTerm.getName(), searchTerm);
        }
        {
            SearchTerm searchTerm = new SearchTerm();
            searchTerm.setName("Queue Type");
            searchTerm.setRackScanSupported(Boolean.FALSE);

            searchTerm.setConstrainedValuesExpression(new SearchDefinitionFactory.QueueTypeValuesExpression());
            searchTerm.setSearchValueConversionExpression( new SearchDefinitionFactory.QueueTypeValueConversionExpression());
            SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
            criteriaPath.setCriteria(Arrays.asList("QueueType", "queueGrouping", "associatedQueue"));
            criteriaPath.setPropertyName("queueType");
            searchTerm.setCriteriaPaths(Collections.singletonList(criteriaPath));

            searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
                @Override
                public String evaluate(Object entity, SearchContext context) {
                    QueueEntity queueEntity = (QueueEntity) entity;
                    return queueEntity.getQueueGrouping().getAssociatedQueue().getQueueType().toString();
                }
            });
            searchTerm.setUiDisplayOutputExpression(new SearchTerm.Evaluator<String>() {

                @Override
                public String evaluate(Object value, SearchContext context) {

                    return QueueType.valueOf((String)value).getTextName();
                }
            });

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


            termDescriptionMap.put(searchTerm, "Queue Status");
            mapNameToSearchTerm.put(searchTerm.getName(), searchTerm);
        }
        {
            SearchTerm searchTerm = new SearchTerm();
            searchTerm.setName(DNA_QUANT_TERMS.SAMPLE_ID.getTerm());
            SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();

            criteriaPath.setCriteria(Arrays.asList("SampleID", "labVessel", "mercurySamples"));
            criteriaPath.setPropertyName("sampleKey"); // might need to be lab_vessel_id

            searchTerm.setCriteriaPaths(Collections.singletonList(criteriaPath));

            searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
                @Override
                public List<String> evaluate(Object entity, SearchContext context) {
                    QueueEntity queueEntity = (QueueEntity) entity;

                    LabVessel labVessel = queueEntity.getLabVessel();
                    List<String> sampleIds = new ArrayList<>();
                    Set<MercurySample> mercurySamples = labVessel.getMercurySamples();
                    for (MercurySample mercurySample : mercurySamples) {
                        sampleIds.add(mercurySample.getSampleKey());
                    }

                    return sampleIds;
                }
            });

            termDescriptionMap.put(searchTerm, DNA_QUANT_TERMS.SAMPLE_ID.getTerm());
            mapNameToSearchTerm.put(searchTerm.getName(), searchTerm);
        }
        {
            SearchTerm searchTerm = new SearchTerm();
            searchTerm.setName(DNA_QUANT_TERMS.CONTAINER_INFO.getTerm());

            searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
                @Override
                public List<String> evaluate(Object entity, SearchContext context) {
                    String multiValueDelimiter = context.getMultiValueDelimiter();
                    QueueEntity queueEntity = (QueueEntity) entity;
                    List<String> results = new ArrayList<>();
                    LabVessel labVessel = queueEntity.getLabVessel();

                    context.setMultiValueDelimiter("<br/>");

                    for (LabVessel container : labVessel.getContainers()) {
                        TubeFormation containerTubeFormation = (TubeFormation) container;

                        TubeFormation tubeFormation = OrmUtil.proxySafeCast(container, TubeFormation.class);

                        String vesselPositionInContainer = tubeFormation.getContainerRole().getPositionOfVessel(labVessel).name();
                        Date createdOn = containerTubeFormation.getCreatedOn();

                        Set<RackOfTubes> racksOfTubes = containerTubeFormation.getRacksOfTubes();
                        for (RackOfTubes rackOfTubes : racksOfTubes) {
                            results.add(rackOfTubes.getLabel() + "/" + vesselPositionInContainer + ":" + ColumnValueType.DATE_TIME.format(createdOn, ""));
                        }
                    }
                    // Set the delimiter back.
                    context.setMultiValueDelimiter(multiValueDelimiter);
                    return results;
                }
            });
            searchTerm.setUiDisplayOutputExpression(new SearchTerm.Evaluator<String>() {

                @Override
                public String evaluate(Object value, SearchContext context) {
                    String results = null;
                    List<String> barcodes = (List<String>)value;

                    if( barcodes == null || barcodes.isEmpty() ) {
                        return results;
                    }

                    StringBuilder containerInformation = new StringBuilder();
                    for (String barcode : barcodes) {
                        containerInformation.append(barcode).append("<br/>");
                    }

                    containerInformation.delete(containerInformation.length()-5,containerInformation.length());
                    return containerInformation.toString();
                }
            });

            termDescriptionMap.put(searchTerm, "Container Information");
            mapNameToSearchTerm.put(searchTerm.getName(), searchTerm);
        }
//        {
//            SearchTerm searchTerm = new SearchTerm();
//            searchTerm.setName(DNA_QUANT_TERMS.CONTAINER_BARCODE.getTerm());
//            SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
//            // todo Get the container...
//            criteriaPath.setCriteria(Arrays.asList("ContainerBarcode", "tubeFormations", "vesselContainer"));
//
//            // TODO  get the vessels in the container... Not really sure how
//            SearchTerm.CriteriaPath criteriaPath2 = new SearchTerm.CriteriaPath();
//            criteriaPath2.setCriteria(Arrays.asList("ContainerVessel", "queueEntityId", "labVessel"));
//
//            // todo check the queue status for vessels
//            SearchTerm.CriteriaPath nestedCriteriaPath = new SearchTerm.CriteriaPath();
//            nestedCriteriaPath.setCriteria(Arrays.asList("QueueEntityVessel", "queueEntityId", "labVessel"));
//            criteriaPath.setNestedCriteriaPath(nestedCriteriaPath);
//            criteriaPath.setPropertyName("labVessel");
//            criteriaPath.setJoinFetch(Boolean.TRUE);
//            searchTerm.setCriteriaPaths(ImmutableList.of(criteriaPath, criteriaPath2));
//
//            searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
//                @Override
//                public List<String> evaluate(Object entity, SearchContext context) {
//                    String multiValueDelimiter = context.getMultiValueDelimiter();
//                    QueueEntity queueEntity = (QueueEntity) entity;
//                    List<String> results = new ArrayList<>();
//                    LabVessel labVessel = queueEntity.getLabVessel();
//
//                    context.setMultiValueDelimiter("<br/>");
//                    // TODO need to figure out how we want this to work. We want this to work like this:
//                    // TODO Searching by Container barcode, find all of the lab vessels that are NOT in the queue.
//
//                    for (LabVessel container : labVessel.getContainers()) {
//                        TubeFormation containerTubeFormation = (TubeFormation) container;
//
//                        TubeFormation tubeFormation = OrmUtil.proxySafeCast(container, TubeFormation.class);
//
//                        String vesselPositionInContainer = tubeFormation.getContainerRole().getPositionOfVessel(labVessel).name();
//
//                        Set<RackOfTubes> racksOfTubes = containerTubeFormation.getRacksOfTubes();
//                        for (RackOfTubes rackOfTubes : racksOfTubes) {
//                            results.add(rackOfTubes.getLabel() + "/" + vesselPositionInContainer);
//                        }
//                    }
//                    // Set the delimiter back.
//                    context.setMultiValueDelimiter(multiValueDelimiter);
//                    return results;
//                }
//            });
//
//            termDescriptionMap.put(searchTerm, "Container Barcode");
//            mapNameToSearchTerm.put(searchTerm.getName(), searchTerm);
//        }
    }

    /**
     * Utility method used to get allowed search terms for DNA Quant queue page. Note that SearchTerm with no criteria path isn't expected
     * to be used to search with and some terms aren't allowed to be changed (e.g. queue type and status).
     *
     * @return
     */
    @Override
    public Set<SearchTerm> getAllowedDisplaySearchTerms() {
        Set<SearchTerm> allowed = new HashSet();
        // Loop through the terms and if there is a criteria path, then add it to the allowed terms.
        for (SearchTerm searchTerm : termDescriptionMap.keySet()) {
            List<SearchTerm.CriteriaPath> criteriaPaths = searchTerm.getCriteriaPaths();
            if (criteriaPaths != null && !criteriaPaths.isEmpty() &&
                ((searchTerm.getName().compareToIgnoreCase("Queue Type") != 0) &&
                 (searchTerm.getName().compareToIgnoreCase("Queue Entity Status") != 0))) {
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
    public Set<SearchTerm> getSearchTerms() {
        Set<SearchTerm> allowed = new HashSet();
        // Loop through the terms and if there is a criteria path, then add it to the allowed terms.
        for (SearchTerm searchTerm : termDescriptionMap.keySet()) {
            allowed.add(searchTerm);
        }
        return allowed;
    }

    @Override
    public List<String> getAllowedResultFields() {
        ArrayList resultFields = new ArrayList();
        // todo they are all allowed based off what the user selects.
        return resultFields;
    }

    @Override
    public List<String> getNotFoundResultRows() {
        return null;
    }
}
