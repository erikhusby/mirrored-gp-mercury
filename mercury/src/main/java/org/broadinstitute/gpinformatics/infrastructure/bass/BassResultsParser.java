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

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible for parsing data obtained from Bass.
 *
 * @see <a href="https://confluence.broadinstitute.org/display/BASS/Application+Programming+Interface">Bass API Documentation</a>
 * @see <a href="https://bass.broadinstitute.org/list?rpid=RP-200">Example call to Bass WS</a>
 */
public class BassResultsParser {
    private static final String FILETYPE_DELIMITER = "^##FILE_TYPE=\\w+##";

    public static List<BassDTO> parse(String bassResponse) {
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
                    String header = headers[i];
                    if (BassDTO.BassResultColumn.hasHeader(header)) {
                        BassDTO.BassResultColumn bassResultColumn = BassDTO.BassResultColumn.valueOf(header);
                        String value = splitLine[i];
                        if (value != null) {
                            resultsMap.put(bassResultColumn, value);
                        }
                    }
                }
                if (!resultsMap.isEmpty()) {
                    result.add(new BassDTO(resultsMap));
                }

            }
        }

        return result;
    }

    public static List<BassDTO> parse(InputStream inputStream) {
        StringWriter writer = new StringWriter();
        List<BassDTO> results;
        try {
            IOUtils.copy(inputStream, writer);
            String theString = writer.toString();
            results = BassResultsParser.parse(theString);
        } catch (IOException e) {
            throw new RuntimeException("Error reading data from server." + e.getMessage(), e);
        }
        return results;
    }
}
