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

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassSearchService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUtil;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.metrics.AggregationMetricsFetcher;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SubmissionDtoFetcher {
    private AggregationMetricsFetcher aggregationMetricsFetcher;
    private BassSearchService bassSearchService;
    private BSPSampleDataFetcher bspSampleDataFetcher;

    public SubmissionDtoFetcher() {
    }

    @Inject
    public SubmissionDtoFetcher(AggregationMetricsFetcher aggregationMetricsFetcher,
                                BassSearchService bassSearchService, BSPSampleDataFetcher bspSampleDataFetcher) {
        this.aggregationMetricsFetcher = aggregationMetricsFetcher;
        this.bassSearchService = bassSearchService;
        this.bspSampleDataFetcher = bspSampleDataFetcher;
    }

    private void updateBulkBspSampleInfo(Collection<ProductOrderSample> samples) {
        Set<String> sampleList = new HashSet<>();

        for(ProductOrderSample sample:samples) {
            String sampleName = sample.getName();
            if (BSPUtil.isInBspFormat(sampleName)) {
                sampleList.add(sampleName);
            }
        }

        Map<String, BSPSampleDTO> bulkInfo = bspSampleDataFetcher.fetchSamplesFromBSP(sampleList);

        for(final ProductOrderSample sample:samples) {
            BSPSampleDTO bspSampleDTO = bulkInfo.get(sample.getName());
            if (bspSampleDTO!=null) {
                sample.setBspSampleDTO(bspSampleDTO);
                // In non-production environments bogus samples are often created so we will
                // only create a new BSPSampleDTO in those cases
            } else if (bspSampleDataFetcher.getBspConfig().getMercuryDeployment() != Deployment.PROD) {
                sample.setBspSampleDTO(new BSPSampleDTO(new HashMap<BSPSampleSearchColumn, String>() {{
                    put(BSPSampleSearchColumn.SAMPLE_ID, sample.getName());
                }}));
            }
        }
    }

    public List<SubmissionDto> fetch(@Nonnull ResearchProject researchProject) {
        return fetch(researchProject, 1);
    }

    public List<SubmissionDto> fetch(@Nonnull ResearchProject researchProject, int version) {
        List<SubmissionDto> results = new ArrayList<>();

        Set<ProductOrderSample> productOrderSamples = researchProject.collectSamples();
        updateBulkBspSampleInfo(productOrderSamples);
        Map<String, Set<ProductOrder>> sampleNameToPdos = new HashMap<>();
        for (ProductOrderSample productOrderSample : productOrderSamples) {
            String pdoSampleName = productOrderSample.getBspSampleDTO().getCollaboratorsSampleName();
            if (!pdoSampleName.isEmpty()) {
                if (sampleNameToPdos.get(pdoSampleName) == null) {
                    sampleNameToPdos.put(pdoSampleName, new HashSet<ProductOrder>());
                }
                sampleNameToPdos.get(pdoSampleName).add(productOrderSample.getProductOrder());
            }
        }

        for (Map.Entry<String, Set<ProductOrder>> sampleListMap : sampleNameToPdos.entrySet()) {
            String collaboratorParticipantId = sampleListMap.getKey();

            Aggregation metricsAggregation =
                    aggregationMetricsFetcher.fetch(researchProject.getBusinessKey(), collaboratorParticipantId,
                            version);

            List<BassDTO> bassDTOs =
                    bassSearchService.runSearch(researchProject.getBusinessKey(), collaboratorParticipantId);
            for (BassDTO bassDTO : bassDTOs) {
                if (bassDTO.getVersion() == version) {
                    results.add(new SubmissionDto(bassDTO, metricsAggregation, sampleListMap.getValue()));
                }

            }
        }
        return results;
    }
}
