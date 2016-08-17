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
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
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
import org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassSearchService;
import org.broadinstitute.gpinformatics.infrastructure.metrics.AggregationMetricsFetcher;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;
import org.jvnet.inflector.Noun;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SubmissionDtoFetcher {
    private static final Log log = LogFactory.getLog(SubmissionDtoFetcher.class);
    private AggregationMetricsFetcher aggregationMetricsFetcher;
    private BassSearchService bassSearchService;
    private SubmissionsService submissionsService;
    private ProductOrderSampleDao productOrderSampleDao;

    @Inject
    public SubmissionDtoFetcher(AggregationMetricsFetcher aggregationMetricsFetcher,
                                BassSearchService bassSearchService, SubmissionsService submissionsService,
                                ProductOrderSampleDao productOrderSampleDao) {
        this.aggregationMetricsFetcher = aggregationMetricsFetcher;
        this.bassSearchService = bassSearchService;
        this.submissionsService = submissionsService;
        this.productOrderSampleDao = productOrderSampleDao;
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
        Map<String, Collection<ProductOrder>> sampleNameToPdos = getSamplesForProject(researchProject, messageReporter);

        Set<String> sampleNames = sampleNameToPdos.keySet();

        // Gather status for anything that has already been submitted
        Map<String, SubmissionTuple> submissionIds = collectSubmissionIdentifiers(researchProject);
        Map<String, SubmissionStatusDetailBean> sampleSubmissionMap = buildSampleToSubmissionMap(submissionIds);

        Map<SubmissionTuple, BassDTO> bassDTOMap =
                fetchBassDtos(researchProject.getBusinessKey(), sampleNames.toArray(new String[sampleNames.size()]));

        /*
         * Since Mercury currently only works with BAM files, always fetch aggregation metrics. If Mercury needs to
         * support other file types in the future, the Bass results will have to be split apart by file type so that
         * metrics can be conditionally fetched as appropriate for each type.
         */
        Map<SubmissionTuple, Aggregation> aggregationMap = fetchAggregationDtos(bassDTOMap.values());

        List<SubmissionDto> results = new ArrayList<>();
        buildSubmissionDtosFromResults(results, sampleNameToPdos, sampleSubmissionMap, bassDTOMap, aggregationMap,
                researchProject);
        return results;
    }

    public Map<SubmissionTuple, BassDTO> fetchBassDtos(String researchProjectBusinessKey, String... samples) {
        List<BassDTO> bassDTOs = bassSearchService.runSearch(researchProjectBusinessKey, samples);
        return buildBassDtoMap(bassDTOs);
    }

    private Map<String, Collection<ProductOrder>> getSamplesForProject(ResearchProject researchProject,
                                                                       MessageReporter messageReporter) {
        List<ProductOrderSample> unfilteredSamples =
                productOrderSampleDao.findByResearchProject(researchProject.getJiraTicketKey());
        List<ProductOrderSample> productOrderSamples
                = new ArrayList<>(Collections2.filter(unfilteredSamples, new Predicate<ProductOrderSample>() {
            @Override
            public boolean apply(@Nullable ProductOrderSample input) {
                return !(input.getProductOrder().isDraft() || input.getDeliveryStatus().isAbandoned());
            }
        }));
        ProductOrder.loadCollaboratorSampleName(productOrderSamples);

        return getCollaboratorSampleNameToPdoMap(productOrderSamples, messageReporter);
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

    private Map<SubmissionTuple, BassDTO> buildBassDtoMap(List<BassDTO> bassDTOs) {
        Map<SubmissionTuple, BassDTO> bassDTOMap = new HashMap<>();
        for (BassDTO bassDTO : bassDTOs) {
            SubmissionTuple tuple = bassDTO.getTuple();
            if (bassDTOMap.containsKey(tuple)) {
                throw new RuntimeException("The bassDTO Map already contains an index for: " + tuple);
            }
            bassDTOMap.put(tuple, bassDTO);
        }
        return bassDTOMap;
    }

    /**
     * Fetch aggregation metrics for a set of files from Bass. This only applies to BAM files. Since Mercury is
     * currently only dealing with BAM files, this is always done. However, if Mercury needs to support more file types
     * in the future (e.g., VCF), then this will need to be revisited.
     *
     * While it is expected that there will be an aggregation record returned for each input, this is not guaranteed by
     * Mercury.
     *
     * @param bassDTOs    Bass files to fetch aggregation metrics for; must be BAMs
     * @return a map of submission tuple to aggregation data
     */
    public Map<SubmissionTuple, Aggregation> fetchAggregationDtos(Collection<BassDTO> bassDTOs) {
        Map<SubmissionTuple, Aggregation> aggregationMap;

        List<SubmissionTuple> tuples = new ArrayList<>();
        for (BassDTO bassDTO : bassDTOs) {
            SubmissionTuple tuple = bassDTO.getTuple();
            log.debug(String.format("Fetching Metrics aggregations for tuple: %s", tuple));
            tuples.add(tuple);
        }

        List<Aggregation> aggregation = aggregationMetricsFetcher.fetch(tuples);
        aggregationMap = Maps.uniqueIndex(aggregation, new Function<Aggregation, SubmissionTuple>() {
            @Override
            public SubmissionTuple apply(@Nullable Aggregation aggregation) {
                return aggregation.getTuple();
            }
        });
        return aggregationMap;
    }

    public void buildSubmissionDtosFromResults(List<SubmissionDto> results,
                                               Map<String, Collection<ProductOrder>> sampleNameToPdos,
                                               Map<String, SubmissionStatusDetailBean> sampleSubmissionMap,
                                               Map<SubmissionTuple, BassDTO> bassDTOMap,
                                               Map<SubmissionTuple, Aggregation> aggregationMap,
                                               ResearchProject researchProject) {
        for (Map.Entry<SubmissionTuple, BassDTO> bassDTOEntry : bassDTOMap.entrySet()) {
            SubmissionTuple tuple = bassDTOEntry.getKey();
            BassDTO bassDTO = bassDTOEntry.getValue();
            Aggregation aggregation = aggregationMap.get(tuple);
            if (aggregation == null) {
                throw new RuntimeException("Could not find metrics for: " + tuple);
            }
            SubmissionTracker submissionTracker = researchProject.getSubmissionTracker(tuple);
            SubmissionStatusDetailBean statusDetailBean = null;
            if (submissionTracker != null) {
                statusDetailBean = sampleSubmissionMap.get(submissionTracker.createSubmissionIdentifier());
            }
            results.add(new SubmissionDto(bassDTO, aggregation, sampleNameToPdos.get(tuple.getSampleName()),
                    statusDetailBean));
        }
    }

    public Map<String, SubmissionStatusDetailBean> buildSampleToSubmissionMap(Map<String, SubmissionTuple> submissionTupleMap) {
        Map<String, SubmissionStatusDetailBean> sampleSubmissionMap = new HashMap<>();

        Set<String> submissionIds = submissionTupleMap.keySet();
        if (CollectionUtils.isNotEmpty(submissionIds)) {
            Collection<SubmissionStatusDetailBean> submissionStatus =
                    submissionsService.getSubmissionStatus(submissionIds.toArray(new String[submissionIds.size()]));
            for (SubmissionStatusDetailBean submissionStatusDetailBean : submissionStatus) {
                SubmissionTuple submissionTuple = submissionTupleMap.get(submissionStatusDetailBean.getUuid());
                submissionStatusDetailBean.setSubmittedVersion(submissionTuple.getVersion());
                sampleSubmissionMap.put(submissionStatusDetailBean.getUuid(), submissionStatusDetailBean);
            }
        }
        return sampleSubmissionMap;
    }

    public void refreshSubmissionStatuses(ResearchProject editResearchProject, List<SubmissionDto> submissionDataList) {
        Map<String, SubmissionTuple> submissionTupleMap = collectSubmissionIdentifiers(editResearchProject);
        Map<String, SubmissionStatusDetailBean> sampleSubmissionMap = buildSampleToSubmissionMap(submissionTupleMap);
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
            submissionIds.put(submissionTracker.createSubmissionIdentifier(), submissionTracker.getTuple());
        }
        return submissionIds;
    }
}
