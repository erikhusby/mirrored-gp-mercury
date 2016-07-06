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
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassSearchService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUtil;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
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
    private BSPSampleDataFetcher bspSampleDataFetcher;
    private SubmissionsService submissionsService;
    private ProductOrderSampleDao productOrderSampleDao;
    // TODO: fix tests so that this isn't needed
    private BSPConfig bspConfig;

    public SubmissionDtoFetcher() {
    }

    @Inject
    public SubmissionDtoFetcher(AggregationMetricsFetcher aggregationMetricsFetcher,
                                BassSearchService bassSearchService, BSPSampleDataFetcher bspSampleDataFetcher,
                                SubmissionsService submissionsService, BSPConfig bspConfig,
                                ProductOrderSampleDao productOrderSampleDao) {
        this.aggregationMetricsFetcher = aggregationMetricsFetcher;
        this.bassSearchService = bassSearchService;
        this.bspSampleDataFetcher = bspSampleDataFetcher;
        this.submissionsService = submissionsService;
        this.bspConfig = bspConfig;
        this.productOrderSampleDao = productOrderSampleDao;
    }

    private void updateBulkBspSampleInfo(Collection<ProductOrderSample> samples) {
        Set<String> sampleList = new HashSet<>();

        for (ProductOrderSample sample : samples) {
            String sampleName = sample.getName();
            if (BSPUtil.isInBspFormat(sampleName)) {
                sampleList.add(sampleName);
            }
        }

        Map<String, BspSampleData> bulkInfo =
                bspSampleDataFetcher.fetchSampleData(sampleList, BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID);

        for (final ProductOrderSample sample : samples) {
            SampleData sampleData = bulkInfo.get(sample.getName());
            if (sampleData != null) {
                sample.setSampleData(sampleData);
            }
        }
    }

    public List<SubmissionDto> fetch(@Nonnull ResearchProject researchProject) {
        return fetch(researchProject, MessageReporter.UNUSED);
    }

    public List<SubmissionDto> fetch(@Nonnull ResearchProject researchProject, MessageReporter messageReporter) {
        Map<String, Collection<ProductOrder>> sampleNameToPdos = getSamplesForProject(researchProject, messageReporter);

        Set<String> sampleNames = sampleNameToPdos.keySet();

        Map<String, SubmissionTuple> submissionIds = collectSubmissionIdentifiers(researchProject);

        Map<String, SubmissionStatusDetailBean> sampleSubmissionMap = buildSampleToSubmissionMap(submissionIds);

        Map<String, BassDTO> bassDTOMap =
                fetchBassDtos(researchProject, sampleNames.toArray(new String[sampleNames.size()]));

        Map<String, Aggregation> aggregationMap = fetchAggregationDtos(researchProject, bassDTOMap);

        List<SubmissionDto> results = new ArrayList<>();
        buildSubmissionDtosFromResults(results, sampleNameToPdos, sampleSubmissionMap, bassDTOMap, aggregationMap,
                researchProject);
        return results;
    }

    public Map<String, BassDTO> fetchBassDtos(ResearchProject researchProject, String ... samples) {
        List<BassDTO> bassDTOs = bassSearchService.runSearch(researchProject.getBusinessKey(),samples);
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
                if (input != null) {
                    return !input.getProductOrder().isDraft();
                }
                return false;
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

    private Map<String, BassDTO> buildBassDtoMap(List<BassDTO> bassDTOs) {
        Map<String, BassDTO> bassDTOMap = new HashMap<>();
        for (BassDTO bassDTO : bassDTOs) {
            if (bassDTOMap.containsKey(bassDTO.getSample())) {
                log.debug("The bassDTO Map already contains an index for: " + bassDTO.getSample());
            }
            bassDTOMap.put(bassDTO.getSample(), bassDTO);
        }
        return bassDTOMap;
    }

    public Map<String, Aggregation> fetchAggregationDtos(ResearchProject researchProject,
                                                         Map<String, BassDTO> bassDTOMap) {
        Map<String, Aggregation> aggregationMap;

        List<String> projects = new ArrayList<>();
        List<String> samples = new ArrayList<>();
        List<Integer> versions = new ArrayList<>();
        for (BassDTO bassDTO : bassDTOMap.values()) {
            log.debug(String.format("Fetching Metrics aggregations for project: %s, sample: %s, version: %d",
                    researchProject.getBusinessKey(), bassDTO.getSample(), bassDTO.getVersion()));
            projects.add(bassDTO.getProject());
            samples.add(bassDTO.getSample());
            versions.add(bassDTO.getVersion());
        }

        List<Aggregation> aggregation = aggregationMetricsFetcher.fetch(projects, samples, versions);
        aggregationMap = Maps.uniqueIndex(aggregation, new Function<Aggregation, String>() {
            @Override
            public String apply(@Nullable Aggregation aggregation) {
                return aggregation.getSample();
            }
        });
        return aggregationMap;
    }

    public void buildSubmissionDtosFromResults(List<SubmissionDto> results,
                                               Map<String, Collection<ProductOrder>> sampleNameToPdos,
                                               Map<String, SubmissionStatusDetailBean> sampleSubmissionMap,
                                               Map<String, BassDTO> bassDTOMap,
                                               Map<String, Aggregation> aggregationMap,
                                               ResearchProject researchProject) {
        for (Map.Entry<String, Collection<ProductOrder>> sampleListMap : sampleNameToPdos.entrySet()) {
            String collaboratorSampleId = sampleListMap.getKey();
            if (aggregationMap.containsKey(collaboratorSampleId) && bassDTOMap.containsKey(collaboratorSampleId)) {
                Aggregation aggregation = aggregationMap.get(collaboratorSampleId);
                BassDTO bassDTO = bassDTOMap.get(collaboratorSampleId);
                SubmissionTracker submissionTracker = researchProject.getSubmissionTracker(bassDTO.getTuple());
                SubmissionStatusDetailBean statusDetailBean = null;
                if (submissionTracker != null) {
                    statusDetailBean = sampleSubmissionMap.get(submissionTracker.createSubmissionIdentifier());
                }
                results.add(new SubmissionDto(bassDTO, aggregation, sampleListMap.getValue(), statusDetailBean));
            }
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
        Map<String, SubmissionDto> uuIdSubmissionDataMap = new HashMap<>(submissionDataList.size());
        for (SubmissionDto submissionDto : submissionDataList) {
            if (StringUtils.isNotBlank(submissionDto.getUuid())) {
                uuIdSubmissionDataMap.put(submissionDto.getUuid(), submissionDto);
            }
        }
        Set<String> uuIds = uuIdSubmissionDataMap.keySet();
        Map<String, SubmissionTuple> submissionTupleMap = collectSubmissionIdentifiers(editResearchProject);
        Map<String, SubmissionStatusDetailBean> sampleSubmissionMap = buildSampleToSubmissionMap(submissionTupleMap);
        if (!uuIds.isEmpty()) {
            for (String uuId : uuIds) {
                SubmissionDto submissionDto = uuIdSubmissionDataMap.get(uuId);
                SubmissionStatusDetailBean statusDetailBean = sampleSubmissionMap.get(uuId);
                submissionDto.setStatusDetailBean(statusDetailBean);
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
