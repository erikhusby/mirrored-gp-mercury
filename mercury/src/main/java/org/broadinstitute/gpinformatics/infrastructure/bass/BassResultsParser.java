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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BassResultsParser {
    private static final String FILETYPE_DELIMITER = "^##FILE_TYPE=\\w+##";

    public List<BassDTO> parse(String bassResponse) {
        List<BassDTO> result = new ArrayList<>();

        String[] lines = bassResponse.split("\n");
        String[] headers = null;
        String[] splitLine = null;
        for (String line : lines) {
            if (line.matches(FILETYPE_DELIMITER)) {
                headers = null;

            } else if (headers == null) {
                headers = line.split("\t");
            } else {
                splitLine = line.split("\t");
                Map<BassDTO.BassResultColumn, String> resultsMap = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    BassDTO.BassResultColumn bassResultColumn = BassDTO.BassResultColumn.valueOf(headers[i]);
                    if (splitLine[i] != null) {
                        resultsMap.put(bassResultColumn, splitLine[i]);
                    }
                }
                if (!resultsMap.isEmpty()) {
                    result.add(new BassDTO(resultsMap));
                }

            }
        }

        return result;
    }
}
