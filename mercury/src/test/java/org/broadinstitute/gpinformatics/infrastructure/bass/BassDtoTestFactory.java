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

import java.util.HashMap;
import java.util.Map;

public class BassDtoTestFactory {
    public static BassDTO buildBassResults(String project, String sample) {
        Map<BassDTO.BassResultColumn, String> resultsMap = new HashMap<>();
        resultsMap.put(BassDTO.BassResultColumn.sample, sample);
        resultsMap.put(BassDTO.BassResultColumn.datatype, BassDTO.DATA_TYPE_EXOME);
        resultsMap.put(BassDTO.BassResultColumn.project, project);
        resultsMap.put(BassDTO.BassResultColumn.rpid, project);
        resultsMap.put(BassDTO.BassResultColumn.file_type, BassFileType.BAM.getBassValue());
        resultsMap.put(BassDTO.BassResultColumn.path, "bambam.bam");
        resultsMap.put(BassDTO.BassResultColumn.version, "1");
        return new BassDTO(resultsMap);
    }
}
