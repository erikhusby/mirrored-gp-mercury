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
import com.google.common.collect.Maps;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTracker;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassSearchService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUtil;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.metrics.AggregationMetricsFetcher;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;

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

    // TODO: fix tests so that this isn't needed
    private BSPConfig bspConfig;

    public SubmissionDtoFetcher() {
    }

    @Inject
    public SubmissionDtoFetcher(AggregationMetricsFetcher aggregationMetricsFetcher,
                                BassSearchService bassSearchService, BSPSampleDataFetcher bspSampleDataFetcher,
                                SubmissionsService submissionsService, BSPConfig bspConfig) {
        this.aggregationMetricsFetcher = aggregationMetricsFetcher;
        this.bassSearchService = bassSearchService;
        this.bspSampleDataFetcher = bspSampleDataFetcher;
        this.submissionsService = submissionsService;
        this.bspConfig = bspConfig;
    }

    private void updateBulkBspSampleInfo(Collection<ProductOrderSample> samples) {
        Set<String> sampleList = new HashSet<>();

        for (ProductOrderSample sample : samples) {
            String sampleName = sample.getName();
            if (BSPUtil.isInBspFormat(sampleName)) {
                sampleList.add(sampleName);
            }
        }

        Map<String, BSPSampleDTO> bulkInfo =
                bspSampleDataFetcher.fetchSampleData(sampleList, BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID);

        for (final ProductOrderSample sample : samples) {
            SampleData sampleData = bulkInfo.get(sample.getName());
            if (sampleData != null) {
                sample.setSampleData(sampleData);
                // In non-production environments bogus samples are often created so we will
                // only create a new BSPSampleDTO in those cases
            } else if (bspConfig.getMercuryDeployment() != Deployment.PROD) {
                sample.setSampleData(new BSPSampleDTO(new HashMap<BSPSampleSearchColumn, String>() {{
                    put(BSPSampleSearchColumn.SAMPLE_ID, sample.getName());
                }}));
            }
        }
    }

    public List<SubmissionDto> fetch(@Nonnull ResearchProject researchProject) {
        List<SubmissionDto> results = new ArrayList<>();

        Map<String, Set<ProductOrder>> sampleNameToPdos = buildSampleToPdoMap(researchProject);

        List<String> submissionIds = collectSubmissionIdentifiers(researchProject);

        Map<String, SubmissionStatusDetailBean> sampleSubmissionMap = buildSampleToSubmissionMap(submissionIds);

        Map<String, BassDTO> bassDTOMap = fetchBassDtos(researchProject);

        Map<String, Aggregation> aggregationMap = fetchAggregationDtos(researchProject, bassDTOMap);

        buildSubmissionDtosFromResults(results, sampleNameToPdos, sampleSubmissionMap, bassDTOMap, aggregationMap,
                researchProject);

        return results;
    }

    public Map<String, BassDTO> fetchBassDtos(ResearchProject researchProject) {
        log.debug(String.format("Fetching bassDTOs for %s", researchProject.getBusinessKey()));
        List<BassDTO> bassDTOs = bassSearchService.runSearch(researchProject.getBusinessKey());
        log.debug(String.format("Fetched %d bassDTOs", bassDTOs.size()));
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
            log.info(String.format("Fetching Metrics aggregations for project: %s, sample: %s, version: %d",
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
                                               Map<String, Set<ProductOrder>> sampleNameToPdos,
                                               Map<String, SubmissionStatusDetailBean> sampleSubmissionMap,
                                               Map<String, BassDTO> bassDTOMap,
                                               Map<String, Aggregation> aggregationMap,
                                               ResearchProject researchProject) {
        for (Map.Entry<String, Set<ProductOrder>> sampleListMap : sampleNameToPdos.entrySet()) {
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

    public Map<String, SubmissionStatusDetailBean> buildSampleToSubmissionMap(List<String> submissionIds) {
        Map<String, SubmissionStatusDetailBean> sampleSubmissionMap = new HashMap<>();

        if (CollectionUtils.isNotEmpty(submissionIds)) {
            Collection<SubmissionStatusDetailBean> submissionStatus =
                    submissionsService.getSubmissionStatus(submissionIds.toArray(new String[submissionIds.size()]));
            for (SubmissionStatusDetailBean submissionStatusDetailBean : submissionStatus) {
                sampleSubmissionMap.put(submissionStatusDetailBean.getUuid(), submissionStatusDetailBean);
            }
        }
        return sampleSubmissionMap;
    }

    public List<String> collectSubmissionIdentifiers(ResearchProject researchProject) {
        List<String> submissionIds = new ArrayList<>();
        /** SubmissionTracker uses sampleName for accessionIdentifier
         @see: org/broadinstitute/gpinformatics/athena/boundary/projects/ ResearchProjectEjb.java:243 **/
        for (SubmissionTracker submissionTracker : researchProject.getSubmissionTrackers()) {
            submissionIds.add(submissionTracker.createSubmissionIdentifier());
        }
        return submissionIds;
    }

    public Map<String, Set<ProductOrder>> buildSampleToPdoMap(ResearchProject researchProject) {
        Set<ProductOrderSample> productOrderSamples = researchProject.collectSamples();
        updateBulkBspSampleInfo(productOrderSamples);
        Map<String, Set<ProductOrder>> sampleNameToPdos = new HashMap<>();
        for (ProductOrderSample productOrderSample : productOrderSamples) {
            String pdoSampleName = productOrderSample.getSampleData().getCollaboratorsSampleName();
            if (!pdoSampleName.isEmpty()) {
                if (sampleNameToPdos.get(pdoSampleName) == null) {
                    sampleNameToPdos.put(pdoSampleName, new HashSet<ProductOrder>());
                }
                sampleNameToPdos.get(pdoSampleName).add(productOrderSample.getProductOrder());
            }
        }
        return sampleNameToPdos;
    }
}
