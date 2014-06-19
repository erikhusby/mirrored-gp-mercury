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

package org.broadinstitute.gpinformatics.infrastructure.bass;

import org.apache.commons.io.FileUtils;

import javax.enterprise.inject.Alternative;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Alternative
public class BassSearchFileService implements BassSearchService {
    static final String TEST_FILE = "src/test/resources/testdata/bass-webservice-result.txt";

    public String readFile() throws IOException {
        return FileUtils.readFileToString(new File(TEST_FILE));
    }

    @Override
    public List<BassDTO> runSearch(Map<BassDTO.BassResultColumn, List<String>> parameters) {
        BassResultsParser bassResultsParser = new BassResultsParser();
        List<BassDTO> results = new ArrayList<>();
        try {
            List<BassDTO> parsedFile = bassResultsParser.parse(readFile());
            for (BassDTO bassDTO : parsedFile) {
                for (Map.Entry<BassDTO.BassResultColumn, List<String>> queryColumnEntry : parameters.entrySet()) {
                    for (String queryValues : queryColumnEntry.getValue()) {
                        String returnedValue = bassDTO.getValue(queryColumnEntry.getKey());
                        if (queryValues.contains(returnedValue)) {
                            results.add(bassDTO);
                        }
                    }
                }

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return results;
    }

    @Override
    public List<BassDTO> runSearch(String researchProjectId) {
        Map<BassDTO.BassResultColumn, List<String>> parameters = new HashMap<>();
        parameters.put(BassDTO.BassResultColumn.rpid, Arrays.asList(researchProjectId));
        return runSearch(parameters);
    }

    @Override
    public List<BassDTO> runSearch(String researchProjectId, String ... collaboratorSampleId) {
        Map<BassDTO.BassResultColumn, List<String>> parameters = new HashMap<>();
        parameters.put(BassDTO.BassResultColumn.sample, Arrays.asList(collaboratorSampleId));
        return BassSearchUtils.filter(runSearch(parameters), researchProjectId);
    }
}
