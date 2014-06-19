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
import java.util.List;

public class BassSearchUtils {
    public static List<BassDTO> filter(List<BassDTO> bassDTOs, String researchProjectId) {
        List<BassDTO> filteredResults=new ArrayList<>();
        for (BassDTO bassDTO : bassDTOs) {
            if (bassDTO.getRpid().equals(researchProjectId)){
                filteredResults.add(bassDTO);
            }
        }
        return filteredResults;
    }
}
