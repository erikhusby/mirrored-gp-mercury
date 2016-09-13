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

import java.util.List;
import java.util.Map;

public interface BassSearchService {
    List<BassDTO> runSearch(Map<BassDTO.BassResultColumn, List<String>> parameters, BassFileType fileType);

    List<BassDTO> runSearch(String researchProjectId, BassFileType fileType);

    List<BassDTO> runSearch(String researchProjectId);

    List<BassDTO> runSearch(String researchProjectId, String... collaboratorSampleId);
}
