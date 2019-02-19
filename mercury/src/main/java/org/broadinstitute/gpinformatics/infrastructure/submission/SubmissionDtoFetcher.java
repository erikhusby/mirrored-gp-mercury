/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.submission;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTracker;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTuple;
import org.broadinstitute.gpinformatics.infrastructure.metrics.AggregationMetricsFetcher;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;
import org.jvnet.inflector.Noun;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Dependent
public class SubmissionDtoFetcher {
    private static final Log log = LogFactory.getLog(SubmissionDtoFetcher.class);
    private AggregationMetricsFetcher aggregationMetricsFetcher;
    private SubmissionsService submissionsService;
    private ProductOrderSampleDao productOrderSampleDao;

    @Inject
    public SubmissionDtoFetcher(AggregationMetricsFetcher aggregationMetricsFetcher,
                                SubmissionsService submissionsService,
                                ProductOrderSampleDao productOrderSampleDao) {
        this.aggregationMetricsFetcher = aggregationMetricsFetcher;
        this.submissionsService = submissionsService;
        this.productOrderSampleDao = productOrderSampleDao;
    }

    Map<String, Collection<ProductOrder>> getCollaboratorSampleNameToPdoMap(
            List<ProductOrderSample> productOrderSamples, MessageReporter messageReporter) {
        HashMultimap<String, ProductOrder> results = HashMultimap.create();
        Multimap<String, String> missingPdoSampleMap = HashMultimap.create();
        for (ProductOrderSample pdoSamples : productOrderSamples) {
            if (StringUtils.isNotBlank(pdoSamples.getSampleData().getCollaboratorsSampleName())) {
                results.put(pdoSamples.getSampleData().getCollaboratorsSampleName(), pdoSamples.getProductOrder());
            } else {
                missingPdoSampleMap.put(pdoSamples.getProductOrder().getBusinessKey(), pdoSamples.getBusinessKey());
            }
        }
        if (!missingPdoSampleMap.isEmpty()) {
            Set<String> notFoundMessages = new HashSet<>();
            for (String key : missingPdoSampleMap.keys()) {
                Collection<String> missingSamples = missingPdoSampleMap.get(key);
                notFoundMessages.add(String.format("%s: %s", key, missingSamples));
            }
            String somePreposition = missingPdoSampleMap.size() > 1 ? "some" : "a";
            String sampleNoun = Noun.pluralOf("sample", missingPdoSampleMap.size());
            messageReporter.addMessage("'Collaborator sample name' not found for {0} {1}<ul><li>{2}</ul>",
                    somePreposition, sampleNoun, StringUtils.join(notFoundMessages, "<li>"));
        }

        return results.asMap();
    }
    /**
     * Fetch information about files that are available for submission, including related information such as
     * aggregation metrics. The results may include files that have already been submitted.
     *
     * @param researchProject    the research project to find files for
     * @param messageReporter    a collector for messages related to finding submission files that a user might be interested in
     * @return a list of files for submission
     */
    public List<SubmissionDto> fetch(@Nonnull ResearchProject researchProject, MessageReporter messageReporter) {
        List<ProductOrderSample> productOrderSamples =
                productOrderSampleDao.findSubmissionSamples(researchProject.getJiraTicketKey());

        ProductOrder.loadCollaboratorSampleName(productOrderSamples);

        // Gather status for anything that has already been submitted
        Map<String, SubmissionStatusDetailBean> sampleSubmissionMap = new HashMap<>();
        try {
            sampleSubmissionMap.putAll(buildSampleToSubmissionMap(researchProject));
        } catch (Exception e) {
            messageReporter.addMessage(e.getLocalizedMessage());
        }

        /*
         * Since Mercury currently only works with BAM files, always fetch aggregation metrics. If Mercury needs to
         * support other file types in the future, the results will have to be split apart by file type so that
         * metrics can be conditionally fetched as appropriate for each type.
         */
        Map<SubmissionTuple, Aggregation> aggregationMap = fetchAggregationDtos(productOrderSamples);

        List<SubmissionDto> results = buildSubmissionDtosFromResults(sampleSubmissionMap, aggregationMap, researchProject, messageReporter);
        return results;
    }

    /**
     * Fetch aggregation metrics given PDO samples. This only applies to BAM files. Since Mercury is
     * currently only dealing with BAM files, this is always done. However, if Mercury needs to support more file types
     * in the future (e.g., VCF), then this will need to be revisited.
     *
     * While it is expected that there will be an aggregation record returned for each input, this is not guaranteed by
     * Mercury.
     *
     * @return a map of submission tuple to aggregation data
     */
    public Map<SubmissionTuple, Aggregation> fetchAggregationDtos(List<ProductOrderSample> productOrderSamples) {
        List<SubmissionTuple> tupleList = new ArrayList<>();
        for (ProductOrderSample productOrderSample : productOrderSamples) {
            String sampleName = productOrderSample.getSampleData().getCollaboratorsSampleName();
            String mercuryProject = productOrderSample.getProductOrder().getResearchProject().getJiraTicketKey();
            SubmissionTuple submissionTuple =
                new SubmissionTuple(mercuryProject, mercuryProject, sampleName, SubmissionTuple.VERSION_UNKNOWN, SubmissionTuple.PROCESSING_LOCATION_UNKNOWN,
                    SubmissionTuple.DATA_TYPE_UNKNOWN);
            tupleList.add(submissionTuple);
        }
        final Map<SubmissionTuple, Aggregation> aggregationMap = new HashMap<>();
        List<Aggregation> aggregations = aggregationMetricsFetcher.fetch(tupleList);
        aggregationMap.putAll(Maps.uniqueIndex(aggregations, new Function<Aggregation, SubmissionTuple>() {
            @Override
            public SubmissionTuple apply(@Nullable Aggregation aggregation) {
                return aggregation.getSubmissionTuple();
            }
        }));

        return aggregationMap;
    }

    public List<SubmissionDto> buildSubmissionDtosFromResults(
        Map<String, SubmissionStatusDetailBean> sampleSubmissionMap,
        Map<SubmissionTuple, Aggregation> aggregationMap,
        ResearchProject researchProject,
        MessageReporter messageReporter) {
        List<SubmissionDto> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (SubmissionTuple tuple : aggregationMap.keySet()) {
            Aggregation aggregation = aggregationMap.get(tuple);
            if (aggregation == null) {
                errors.add(String.format("%s v%s", tuple.getSampleName(), tuple.getVersion()));
            }
            SubmissionTracker submissionTracker = researchProject.getSubmissionTracker(tuple);
            SubmissionStatusDetailBean statusDetailBean = null;
            if (submissionTracker != null) {
                try {
                    statusDetailBean = sampleSubmissionMap.get(submissionTracker.createSubmissionIdentifier());
                } catch (Exception e) {
                    messageReporter.addMessage(e.getMessage());
                }
            }
            results.add(new SubmissionDto(aggregation, statusDetailBean));
        }
        if (!errors.isEmpty()) {
            messageReporter.addMessage("Picard data not found for samples<ul><li>{0}</ul>", errors);
        }
        return results;
    }

    public Map<String, SubmissionStatusDetailBean> buildSampleToSubmissionMap(ResearchProject researchProject) {
        Map<String, SubmissionTuple> submissionTupleMap = collectSubmissionIdentifiers(researchProject);
        Map<String, SubmissionStatusDetailBean> sampleSubmissionMap = new HashMap<>();

        Set<String> submissionIds = submissionTupleMap.keySet();
        if (CollectionUtils.isNotEmpty(submissionIds)) {
            Collection<SubmissionStatusDetailBean> submissionStatus =
                    submissionsService.getSubmissionStatus(submissionIds.toArray(new String[submissionIds.size()]));
            for (SubmissionStatusDetailBean submissionStatusDetailBean : submissionStatus) {
                if (hasSubmission(submissionStatusDetailBean)) {
                    SubmissionTuple submissionTuple = submissionTupleMap.get(submissionStatusDetailBean.getUuid());
                    submissionStatusDetailBean.setSubmittedVersion(submissionTuple.getVersion());
                    submissionStatusDetailBean.setSubmissionDatatype(submissionTuple.getDataType());
                    sampleSubmissionMap.put(submissionStatusDetailBean.getUuid(), submissionStatusDetailBean);
                }
            }
        }
        return sampleSubmissionMap;
    }

    private boolean hasSubmission(SubmissionStatusDetailBean submissionStatusDetailBean) {
        return submissionStatusDetailBean.getStatus() != null;
    }

    public void refreshSubmissionStatuses(ResearchProject editResearchProject, List<SubmissionDto> submissionDataList) {
        Map<String, SubmissionStatusDetailBean> sampleSubmissionMap = buildSampleToSubmissionMap(editResearchProject);
        for (SubmissionDto submissionDto : submissionDataList) {
            if (StringUtils.isNotBlank(submissionDto.getUuid())) {
                submissionDto.setStatusDetailBean(sampleSubmissionMap.get(submissionDto.getUuid()));
            }
        }
    }

    private Map<String, SubmissionTuple> collectSubmissionIdentifiers(ResearchProject researchProject) {
        Map<String, SubmissionTuple> submissionIds = new HashMap<>();
        /** SubmissionTracker uses sampleName for accessionIdentifier
         @see: org/broadinstitute/gpinformatics/athena/boundary/projects/ ResearchProjectEjb.java:243 **/
        for (SubmissionTracker submissionTracker : researchProject.getSubmissionTrackers()) {
            String uuid = submissionTracker.createSubmissionIdentifier();
            if (!submissionIds.containsKey(uuid)) {
                submissionIds.put(uuid, submissionTracker.getSubmissionTuple());
            }
        }
        return submissionIds;
    }
}
