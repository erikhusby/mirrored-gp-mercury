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

import org.apache.commons.lang3.tuple.Pair;

import javax.enterprise.inject.Alternative;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Alternative
public class JustGiveMeSomeDataBassSearchService implements BassSearchService {
    @Override
    public List<Map<BassDTO.BassResultColumns, String>> runSearch(Pair<BassDTO.BassResultColumns, String> bassResult) {
        List<Map<BassDTO.BassResultColumns, String>> results = new ArrayList<>();
        Map<BassDTO.BassResultColumns, String> dataMap = new HashMap<BassDTO.BassResultColumns, String>() {{
            put(BassDTO.BassResultColumns.id, "BI6504697");
            put(BassDTO.BassResultColumns.version, "1");
            put(BassDTO.BassResultColumns.hold_count, "0");
            put(BassDTO.BassResultColumns.organism, "House mouse");
            put(BassDTO.BassResultColumns.location, "86625120");
            put(BassDTO.BassResultColumns.directory, "/seq/tier3b/picard_aggregation/G25727/RAP_XistTimeCourse2-77/v1");
            put(BassDTO.BassResultColumns.project, "G25727");
            put(BassDTO.BassResultColumns.initiative, "Mouse RAP Seq_Discretionary Fund");
            put(BassDTO.BassResultColumns.path,
                    "/seq/tier3b/picard_aggregation/G25727/RAP_XistTimeCourse2-77/v1/RAP_XistTimeCourse2-77.bam");
            put(BassDTO.BassResultColumns.stored_by, "[name: picard][host: node1173][IP: 69.173.75.173][OS: Linux]");
            put(BassDTO.BassResultColumns.sample, "RAP_XistTimeCourse2-77");
            put(BassDTO.BassResultColumns.md5, "8a11a9376768a8dfcd997a1d3fae3c43");
            put(BassDTO.BassResultColumns.stored_on, "2012_10_25_18_50_32_EDT");
            put(BassDTO.BassResultColumns.software, "PICARD");
            put(BassDTO.BassResultColumns.file_name, "RAP_XistTimeCourse2-77.bam");
            put(BassDTO.BassResultColumns.software_provider, "BROAD");
            put(BassDTO.BassResultColumns.file_type, "bam");
        }};
        results.add(dataMap);

        dataMap = new HashMap<BassDTO.BassResultColumns, String>() {{
            put(BassDTO.BassResultColumns.id, "BI6504777");
            put(BassDTO.BassResultColumns.version, "0");
            put(BassDTO.BassResultColumns.hold_count, "1");
            put(BassDTO.BassResultColumns.organism, "House mouse");
            put(BassDTO.BassResultColumns.location, "86625190");
            put(BassDTO.BassResultColumns.directory, "/seq/tier3b/picard_aggregation/G25727/RAP_XistTimeCourse2-77");
            put(BassDTO.BassResultColumns.project, "G25727");
            put(BassDTO.BassResultColumns.initiative, "Mouse RAP Seq_Discretionary Fund");
            put(BassDTO.BassResultColumns.path, "/seq/tier3b/picard_aggregation/G25727/RAP_XistTimeCourse2-77/v1");
            put(BassDTO.BassResultColumns.stored_by, "[name: picard][host: node1173][IP: 69.173.75.173][OS: Linux]");
            put(BassDTO.BassResultColumns.sample, "RAP_XistTimeCourse2-77");
            put(BassDTO.BassResultColumns.md5, "[NULL]");
            put(BassDTO.BassResultColumns.stored_on, "2012_10_25_19_02_02_EDT");
            put(BassDTO.BassResultColumns.software, "PICARD");
            put(BassDTO.BassResultColumns.file_name, "v1");
            put(BassDTO.BassResultColumns.software_provider, "BROAD");
            put(BassDTO.BassResultColumns.file_type, "picard");
        }};
        results.add(dataMap);

        //##FILE_TYPE=read_group_bam##
        dataMap = new HashMap<BassDTO.BassResultColumns, String>() {{
            put(BassDTO.BassResultColumns.id, "BI6504041");
            put(BassDTO.BassResultColumns.gssr_barcode, "251336.0");
            put(BassDTO.BassResultColumns.run_name, "121020_SL-HAP_0228_AFCC17VNACXX");
            put(BassDTO.BassResultColumns.version, "1351051200");
            put(BassDTO.BassResultColumns.organism, "House mouse");
            put(BassDTO.BassResultColumns.hold_count, "0");
            put(BassDTO.BassResultColumns.screened, "false");
            put(BassDTO.BassResultColumns.flowcell_barcode, "C17VNACXX");
            put(BassDTO.BassResultColumns.location, "86624470");
            put(BassDTO.BassResultColumns.run_barcode, "C17VNACXX121020");
            put(BassDTO.BassResultColumns.directory,
                    "/seq/tier3/picard/C17VNACXX/C1-58_2012-10-20_2012-10-24/8/Mitch_Guttman_RAP_XistTimeCourse2-77");
            put(BassDTO.BassResultColumns.lane, "8");
            put(BassDTO.BassResultColumns.project, "G25727");
            put(BassDTO.BassResultColumns.initiative, "Mouse RAP Seq_Discretionary Fund");
            put(BassDTO.BassResultColumns.read_pairing_type, "PAIRED");
            put(BassDTO.BassResultColumns.stored_by, "[name: picard][host: node1575][IP: 10.200.97.79][OS: Linux]");
            put(BassDTO.BassResultColumns.path,
                    "/seq/tier3/picard/C17VNACXX/C1-58_2012-10-20_2012-10-24/8/Mitch_Guttman_RAP_XistTimeCourse2-77/C17VNACXX.8.aligned.duplicates_marked.bam");
            put(BassDTO.BassResultColumns.sample, "RAP_XistTimeCourse2-77");
            put(BassDTO.BassResultColumns.molecular_barcode_name, "tagged_851");
            put(BassDTO.BassResultColumns.library_name, "Mitch Guttman_RAP_XistTimeCourse2-77");
            put(BassDTO.BassResultColumns.md5, "90eae433029612834b155bb80d0fd1e0");
            put(BassDTO.BassResultColumns.stored_on, "2012_10_25_17_17_15_EDT");
            put(BassDTO.BassResultColumns.software, "PICARD");
            put(BassDTO.BassResultColumns.molecular_barcode_sequence, "TCCAGCAA");
            put(BassDTO.BassResultColumns.file_name, "C17VNACXX.8.aligned.duplicates_marked.bam");
            put(BassDTO.BassResultColumns.software_provider, "BROAD");
            put(BassDTO.BassResultColumns.file_type, "read_group_bam");
        }};
        results.add(dataMap);
        //##FILE_TYPE=read_group_bam##
        dataMap = new HashMap<BassDTO.BassResultColumns, String>() {{
            put(BassDTO.BassResultColumns.id, "BI6504245");
            put(BassDTO.BassResultColumns.gssr_barcode, "251336.0");
            put(BassDTO.BassResultColumns.run_name, "121020_SL-HAP_0228_AFCC17VNACXX");
            put(BassDTO.BassResultColumns.version, "1351051200");
            put(BassDTO.BassResultColumns.organism, "House mouse");
            put(BassDTO.BassResultColumns.hold_count, "0");
            put(BassDTO.BassResultColumns.screened, "false");
            put(BassDTO.BassResultColumns.flowcell_barcode, "C17VNACXX");
            put(BassDTO.BassResultColumns.location, "86624672");
            put(BassDTO.BassResultColumns.run_barcode, "C17VNACXX121020");
            put(BassDTO.BassResultColumns.directory,
                    "/seq/tier3/picard/C17VNACXX/C1-58_2012-10-20_2012-10-24/7/Mitch_Guttman_RAP_XistTimeCourse2-77");
            put(BassDTO.BassResultColumns.lane, "7");
            put(BassDTO.BassResultColumns.project, "G25727");
            put(BassDTO.BassResultColumns.initiative, "Mouse RAP Seq_Discretionary Fund");
            put(BassDTO.BassResultColumns.read_pairing_type, "PAIRED");
            put(BassDTO.BassResultColumns.stored_by, "[name: picard][host: node1575][IP: 10.200.97.79][OS: Linux]");
            put(BassDTO.BassResultColumns.path,
                    "/seq/tier3/picard/C17VNACXX/C1-58_2012-10-20_2012-10-24/7/Mitch_Guttman_RAP_XistTimeCourse2-77/C17VNACXX.7.aligned.duplicates_marked.bam");
            put(BassDTO.BassResultColumns.sample, "RAP_XistTimeCourse2-77");
            put(BassDTO.BassResultColumns.molecular_barcode_name, "tagged_851");
            put(BassDTO.BassResultColumns.library_name, "Mitch Guttman_RAP_XistTimeCourse2-77");
            put(BassDTO.BassResultColumns.md5, "56b947184f7f5d9522412267f784ae77");
            put(BassDTO.BassResultColumns.stored_on, "2012_10_25_17_46_11_EDT");
            put(BassDTO.BassResultColumns.software, "PICARD");
            put(BassDTO.BassResultColumns.molecular_barcode_sequence, "TCCAGCAA");
            put(BassDTO.BassResultColumns.file_name, "C17VNACXX.7.aligned.duplicates_marked.bam");
            put(BassDTO.BassResultColumns.software_provider, "BROAD");
            put(BassDTO.BassResultColumns.file_type, "read_group_bam");
        }};
        results.add(dataMap);

        return results;
    }
}
