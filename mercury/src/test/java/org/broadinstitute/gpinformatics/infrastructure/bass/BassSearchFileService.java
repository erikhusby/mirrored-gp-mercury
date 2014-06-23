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
import org.testng.Assert;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BassSearchFileService extends BassSearchService {
    static final String TEST_FILE = "src/test/resources/testdata/bass-webservice-result.txt";
    private Map<BassDTO.BassResultColumn, List<String>> parameters = new HashMap<>();

    public String readFile() throws IOException {
        return FileUtils.readFileToString(new File(TEST_FILE));
    }

    @Override
    public List<BassDTO> runSearch(Map<BassDTO.BassResultColumn, List<String>> parameters) {
        List<BassDTO> results = new ArrayList<>();
        try {
            List<BassDTO> parsedFile = BassResultsParser.parse(readFile());
            for (BassDTO bassDTO : parsedFile) {
                boolean containsAllValues = true;
                for (Map.Entry<BassDTO.BassResultColumn, List<String>> queryColumnEntry : parameters.entrySet()) {
                    for (String queryValues : queryColumnEntry.getValue()) {
                        String returnedValue = bassDTO.getValue(queryColumnEntry.getKey());
                        if (!queryValues.contains(returnedValue)) {
                            containsAllValues = false;
                        }
                    }
                }
                if (containsAllValues) {
                    results.add(bassDTO);
                }
            }
        } catch (IOException e) {
            Assert.fail("Could not read results file: ", e);
        }
        return results;
    }
}
