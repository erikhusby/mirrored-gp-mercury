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
import org.apache.commons.lang3.tuple.Pair;

import javax.enterprise.inject.Alternative;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Alternative
public class BassSearchFileService implements BassSearchService {
    static final String TEST_FILE = "src/test/resources/testdata/bass-webservice-result.txt";


    public String readFile() throws IOException {
        return FileUtils.readFileToString(new File(TEST_FILE));
    }

    @Override
    public List<BassDTO> runSearch(
            Collection<Pair<BassDTO.BassResultColumn, String>> searchTerms) {
        BassResultsParser bassResultsParser=new BassResultsParser();
        List<BassDTO> results=new ArrayList<>();
        try {
            List<BassDTO> parsedFile = bassResultsParser.parse(readFile());
            for (BassDTO bassDTO : parsedFile) {
                for (Pair<BassDTO.BassResultColumn, String> searchTerm : searchTerms) {
                    String columnValue = bassDTO.getValue(searchTerm.getKey());
                    if (columnValue.equals(searchTerm.getRight())) {
                        results.add(bassDTO);
                    }
                }

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return results;
    }

}
