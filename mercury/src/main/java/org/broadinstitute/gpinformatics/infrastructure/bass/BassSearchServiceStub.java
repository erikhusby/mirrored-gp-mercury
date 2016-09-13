/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2016 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.bass;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

import javax.enterprise.inject.Alternative;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Stub
@Alternative
public class BassSearchServiceStub implements BassSearchService {
    @Override
    public List<BassDTO> runSearch(Map<BassDTO.BassResultColumn, List<String>> parameters, BassFileType fileType) {
        List<BassDTO> bassDTOs = new ArrayList<>();
        for (final Map.Entry<BassDTO.BassResultColumn, List<String>> bassResultColumnListEntry : parameters
                .entrySet()) {
            for (final String s : bassResultColumnListEntry.getValue()) {
                Map<BassDTO.BassResultColumn, String> map = new HashMap<BassDTO.BassResultColumn, String>() {
                    private static final long serialVersionUID = 6704516729381001312L;
                    { put(bassResultColumnListEntry.getKey(), s); }
                };

                bassDTOs.add(new BassDTO(map));
            }
        }
        return bassDTOs;
    }

    @Override
    public List<BassDTO> runSearch(String researchProjectId, BassFileType fileType) {
        return runSearch(researchProjectId);
    }

    @Override
    public List<BassDTO> runSearch(String researchProjectId) {
        int sampleCount = 50;
        return runSearch(researchProjectId, getSamples(sampleCount).toArray(new String[sampleCount]));
    }

    private List<String> getSamples(int sampleCount) {
        List<String> samples = new ArrayList<>();
        for (int i = 0; i < sampleCount; i++) {
            samples.add("SM-ABC" + i);
        }
        return samples;
    }

    @Override
    public List<BassDTO> runSearch(String researchProjectId, String... collaboratorSampleId) {
        List<BassDTO> bassDtoList = new ArrayList<>(collaboratorSampleId.length);
        for (String sampleId : collaboratorSampleId) {
            bassDtoList.add(BassDtoTestFactory.buildBassResults(researchProjectId, sampleId));
        }
        return bassDtoList;
    }
}
