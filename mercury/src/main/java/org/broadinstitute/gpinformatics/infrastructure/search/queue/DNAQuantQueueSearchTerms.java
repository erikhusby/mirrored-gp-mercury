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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class DNAQuantQueueSearchTerms extends AbstractQueueSearchTerms {

    public DNAQuantQueueSearchTerms() {
    }

    public enum DNA_QUANT_TERMS {
        BARCODE("Barcode"),                      // Used to search by manufacturer barcode.s
        MERCURY_SAMPLE_ID("Mercury Sample ID"),  // Used to actually search by Sample ID.
        NEAREST_SAMPLE_ID("Nearest Sample ID"),  // Used to return the nearest sample ID to the barcode scanned (for display of results only).
        CONTAINER_BARCODE("Container Barcode"),  // Searching by container ID.
        CONTAINER_INFO("Container Information"); // Displaying container information.

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
            searchTerm.setName(DNA_QUANT_TERMS.BARCODE.getTerm());
            searchTerm.setRackScanSupported(Boolean.TRUE);
            SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
            criteriaPath.setPropertyName("label"); // might need to be label

            searchTerm.setCriteriaPaths(Collections.singletonList(criteriaPath));

            searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
                @Override
                public String evaluate(Object entity, SearchContext context) {
                    LabVessel labVessel = (LabVessel) entity;
                    // If the lab vessel isn't found we'll return empty row.
                    if( labVessel != null) {
                        return String.valueOf(labVessel.getLabVesselId());
                    } else {
                        return null;
                    }
                }
            });


            termDescriptionMap.put(searchTerm, DNA_QUANT_TERMS.BARCODE.getTerm());
            mapNameToSearchTerm.put(searchTerm.getName(), searchTerm);
        }
        {
            SearchTerm searchTerm = new SearchTerm();
            searchTerm.setName("Queue Type");
            searchTerm.setRackScanSupported(Boolean.FALSE);

            searchTerm.setConstrainedValuesExpression(new SearchDefinitionFactory.QueueTypeValuesExpression());
            searchTerm.setSearchValueConversionExpression( new SearchDefinitionFactory.QueueTypeValueConversionExpression());
            SearchTerm.CriteriaPath criteriaPath = new SearchTerm.CriteriaPath();
            criteriaPath.setCriteria(Arrays.asList("QueueType", "queueEntities", "queueGrouping", "associatedQueue"));
            criteriaPath.setPropertyName("queueType");
            searchTerm.setCriteriaPaths(Collections.singletonList(criteriaPath));

            searchTerm.setDisplayValueExpression(new SearchTerm.Evaluator<Object>() {
                @Override
                public Set<String> evaluate(Object entity, SearchContext context) {
                    LabVessel labVessel = (LabVessel) entity;
                    Set<String> queueTypes = new HashSet<>();
                    for (QueueEntity queueEntity : labVessel.getQueueEntities()) {
                        queueTypes.add(queueEntity.getQueueGrouping().getAssociatedQueue().getQueueType().getTextName());
                    }

                    return queueTypes;
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

                    return queueEntity.getQueueStatus().getDisplayName();
                }
            });


            termDescriptionMap.put(searchTerm, "Queue Status");
            mapNameToSearchTerm.put(searchTerm.getName(), searchTerm);
        }
        {
            SearchTerm searchTerm = new SearchTerm();
            searchTerm.setName(DNA_QUANT_TERMS.NEAREST_SAMPLE_ID.getTerm());
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

            termDescriptionMap.put(searchTerm, DNA_QUANT_TERMS.NEAREST_SAMPLE_ID.getTerm());
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
                    TreeMap<String, String> sortedResults = new TreeMap<>();

                    for (LabVessel container : labVessel.getContainers()) {
                        TubeFormation containerTubeFormation = (TubeFormation) container;

                        TubeFormation tubeFormation = OrmUtil.proxySafeCast(container, TubeFormation.class);

                        String vesselPositionInContainer = tubeFormation.getContainerRole().getPositionOfVessel(labVessel).name();
                        Date createdOn = containerTubeFormation.getCreatedOn();

                        Set<RackOfTubes> racksOfTubes = containerTubeFormation.getRacksOfTubes();
                        for (RackOfTubes rackOfTubes : racksOfTubes) {
                            String formattedCreatedOn = ColumnValueType.DATE_TIME.format(createdOn, "");
                            String containerInfo = "<b>" + rackOfTubes.getLabel() + "</b>/<b>" + vesselPositionInContainer + "</b>:" + formattedCreatedOn;
                            sortedResults.put(formattedCreatedOn, containerInfo);
                        }
                    }
                    for (Map.Entry<String, String> containerInformation : sortedResults.entrySet()) {
                        results.add(containerInformation.getValue());
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
    }

    /**
     * Utility method used to get allowed search terms for DNA Quant queue page. Note that SearchTerm with no criteria path isn't expected
     * to be used to search with and some terms aren't allowed to be changed (e.g. queue type and status).
     *
     * @return
     */
    @Override
    public Set<String> getAllowedDisplaySearchTerms() {
        Set<String> allowed = new HashSet();
        // Loop through the terms and if there is a criteria path, then add it to the allowed terms.
        for (SearchTerm searchTerm : termDescriptionMap.keySet()) {
            List<SearchTerm.CriteriaPath> criteriaPaths = searchTerm.getCriteriaPaths();
            if (criteriaPaths != null && !criteriaPaths.isEmpty() &&
                ((searchTerm.getName().compareToIgnoreCase("Queue Type") != 0) &&
                 (searchTerm.getName().compareToIgnoreCase("Queue Entity Status") != 0) &&
                 (searchTerm.getName().compareToIgnoreCase(DNA_QUANT_TERMS.NEAREST_SAMPLE_ID.getTerm()) != 0))) {
                allowed.add(searchTerm.getName());
            }
        }
        allowed.add(DNA_QUANT_TERMS.CONTAINER_BARCODE.getTerm());
        allowed.add(DNA_QUANT_TERMS.MERCURY_SAMPLE_ID.getTerm());
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
}
