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

package org.broadinstitute.gpinformatics.athena.entity.project;

import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassSearchService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
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
    private ResearchProjectDao researchProjectDao;

    public SubmissionDtoFetcher() {
    }

    @Inject
    public SubmissionDtoFetcher(AggregationMetricsFetcher aggregationMetricsFetcher,
                                BassSearchService bassSearchService, BSPSampleDataFetcher bspSampleDataFetcher) {
        this.aggregationMetricsFetcher = aggregationMetricsFetcher;
        this.bassSearchService = bassSearchService;
        this.bspSampleDataFetcher = bspSampleDataFetcher;
    }

    public void updateBulkBspSampleInfo(Collection<ProductOrderSample> samples) {
        Set<String> sampleList = new HashSet<>();

        for(ProductOrderSample sample:samples) {
            sampleList.add(sample.getName());
        }

        Map<String, BSPSampleDTO> bulkInfo = bspSampleDataFetcher.fetchSamplesFromBSP(sampleList);

        for(ProductOrderSample sample:samples) {
            sample.setBspSampleDTO(bulkInfo.get(sample.getName()));
        }

    }

    public List<SubmissionDTO> fetch(@Nonnull ResearchProject researchProject, int version) {
        List<SubmissionDTO> results = new ArrayList<>();

        Set<ProductOrderSample> productOrderSamples = researchProject.collectSamples();
        updateBulkBspSampleInfo(productOrderSamples);
        Map<String, List<String>> sampleNameToPdos = new HashMap<>();
        for (ProductOrderSample productOrderSample : productOrderSamples) {
            String pdoSampleName = productOrderSample.getName();
            if (sampleNameToPdos.get(pdoSampleName) == null) {
                sampleNameToPdos.put(pdoSampleName, new ArrayList<String>());
            }
            sampleNameToPdos.get(pdoSampleName).add(productOrderSample.getProductOrder().getBusinessKey());
        }

        for (Map.Entry<String, List<String>> sampleListMap : sampleNameToPdos.entrySet()) {
            Aggregation metricsAggregation =
                    aggregationMetricsFetcher.fetch(researchProject.getBusinessKey(), sampleListMap.getKey(), version);
            List<BassDTO> bassDTOs =
                    bassSearchService.runSearch(researchProject.getBusinessKey(), sampleListMap.getKey());
            for (BassDTO bassDTO : bassDTOs) {
                if (bassDTO.getVersion() == version) {
                    results.add(new SubmissionDTO(bassDTO, metricsAggregation, sampleListMap.getValue()));
                }
            }
        }
        return results;
    }
}
