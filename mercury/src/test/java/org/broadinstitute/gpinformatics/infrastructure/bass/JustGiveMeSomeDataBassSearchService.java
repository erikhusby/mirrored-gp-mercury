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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

@Alternative
public class JustGiveMeSomeDataBassSearchService implements BassSearchService {
    @Override
    public List<BassDTO> runSearch(Collection<Pair<BassDTO.BassResultColumn, String>> bassResult) {
        List<BassDTO> results = new ArrayList<>();
        BassDTO bassDTO = new BassDTO(new HashMap<BassDTO.BassResultColumn, String>() {{
            put(BassDTO.BassResultColumn.id, "BI6504697");
            put(BassDTO.BassResultColumn.version, "1");
            put(BassDTO.BassResultColumn.hold_count, "0");
            put(BassDTO.BassResultColumn.organism, "House mouse");
            put(BassDTO.BassResultColumn.location, "86625120");
            put(BassDTO.BassResultColumn.directory, "/seq/tier3b/picard_aggregation/G25727/RAP_XistTimeCourse2-77/v1");
            put(BassDTO.BassResultColumn.project, "G25727");
            put(BassDTO.BassResultColumn.initiative, "Mouse RAP Seq_Discretionary Fund");
            put(BassDTO.BassResultColumn.path,
                    "/seq/tier3b/picard_aggregation/G25727/RAP_XistTimeCourse2-77/v1/RAP_XistTimeCourse2-77.bam");
            put(BassDTO.BassResultColumn.stored_by, "[name: picard][host: node1173][IP: 69.173.75.173][OS: Linux]");
            put(BassDTO.BassResultColumn.sample, "RAP_XistTimeCourse2-77");
            put(BassDTO.BassResultColumn.md5, "8a11a9376768a8dfcd997a1d3fae3c43");
            put(BassDTO.BassResultColumn.stored_on, "2012_10_25_18_50_32_EDT");
            put(BassDTO.BassResultColumn.software, "PICARD");
            put(BassDTO.BassResultColumn.file_name, "RAP_XistTimeCourse2-77.bam");
            put(BassDTO.BassResultColumn.software_provider, "BROAD");
            put(BassDTO.BassResultColumn.file_type, "bam");
            put(BassDTO.BassResultColumn.rpname, "Resistance NHGRI CIP");
            put(BassDTO.BassResultColumn.rpid, "RP-12");
            put(BassDTO.BassResultColumn.datatype, "Exome");
            put(BassDTO.BassResultColumn.rg_checksum, "6f19d677a50872004395ba3e39265164");
        }});
        results.add(bassDTO);

        bassDTO = new BassDTO(new HashMap<BassDTO.BassResultColumn, String>() {{
            put(BassDTO.BassResultColumn.id, "BI6504777");
            put(BassDTO.BassResultColumn.version, "0");
            put(BassDTO.BassResultColumn.hold_count, "1");
            put(BassDTO.BassResultColumn.organism, "House mouse");
            put(BassDTO.BassResultColumn.location, "86625190");
            put(BassDTO.BassResultColumn.directory, "/seq/tier3b/picard_aggregation/G25727/RAP_XistTimeCourse2-77");
            put(BassDTO.BassResultColumn.project, "G25727");
            put(BassDTO.BassResultColumn.initiative, "Mouse RAP Seq_Discretionary Fund");
            put(BassDTO.BassResultColumn.path, "/seq/tier3b/picard_aggregation/G25727/RAP_XistTimeCourse2-77/v1");
            put(BassDTO.BassResultColumn.stored_by, "[name: picard][host: node1173][IP: 69.173.75.173][OS: Linux]");
            put(BassDTO.BassResultColumn.sample, "RAP_XistTimeCourse2-77");
            put(BassDTO.BassResultColumn.md5, "[NULL]");
            put(BassDTO.BassResultColumn.stored_on, "2012_10_25_19_02_02_EDT");
            put(BassDTO.BassResultColumn.software, "PICARD");
            put(BassDTO.BassResultColumn.file_name, "v1");
            put(BassDTO.BassResultColumn.software_provider, "BROAD");
            put(BassDTO.BassResultColumn.file_type, "picard");
            put(BassDTO.BassResultColumn.rpname, "Resistance NHGRI CIP");
            put(BassDTO.BassResultColumn.rpid, "RP-12");
            put(BassDTO.BassResultColumn.datatype, "Exome");
            put(BassDTO.BassResultColumn.rg_checksum, BassDTO.NULL);

        }});
        results.add(bassDTO);

        //##FILE_TYPE=read_group_bam##
        bassDTO = new BassDTO(new HashMap<BassDTO.BassResultColumn, String>() {{
            put(BassDTO.BassResultColumn.id, "BI6504041");
            put(BassDTO.BassResultColumn.gssr_barcode, "251336.0");
            put(BassDTO.BassResultColumn.run_name, "121020_SL-HAP_0228_AFCC17VNACXX");
            put(BassDTO.BassResultColumn.version, "1351051200");
            put(BassDTO.BassResultColumn.organism, "House mouse");
            put(BassDTO.BassResultColumn.hold_count, "0");
            put(BassDTO.BassResultColumn.screened, "false");
            put(BassDTO.BassResultColumn.flowcell_barcode, "C17VNACXX");
            put(BassDTO.BassResultColumn.location, "86624470");
            put(BassDTO.BassResultColumn.run_barcode, "C17VNACXX121020");
            put(BassDTO.BassResultColumn.directory,
                    "/seq/tier3/picard/C17VNACXX/C1-58_2012-10-20_2012-10-24/8/Mitch_Guttman_RAP_XistTimeCourse2-77");
            put(BassDTO.BassResultColumn.lane, "8");
            put(BassDTO.BassResultColumn.project, "G25727");
            put(BassDTO.BassResultColumn.initiative, "Mouse RAP Seq_Discretionary Fund");
            put(BassDTO.BassResultColumn.read_pairing_type, "PAIRED");
            put(BassDTO.BassResultColumn.stored_by, "[name: picard][host: node1575][IP: 10.200.97.79][OS: Linux]");
            put(BassDTO.BassResultColumn.path,
                    "/seq/tier3/picard/C17VNACXX/C1-58_2012-10-20_2012-10-24/8/Mitch_Guttman_RAP_XistTimeCourse2-77/C17VNACXX.8.aligned.duplicates_marked.bam");
            put(BassDTO.BassResultColumn.sample, "RAP_XistTimeCourse2-77");
            put(BassDTO.BassResultColumn.molecular_barcode_name, "tagged_851");
            put(BassDTO.BassResultColumn.library_name, "Mitch Guttman_RAP_XistTimeCourse2-77");
            put(BassDTO.BassResultColumn.md5, "90eae433029612834b155bb80d0fd1e0");
            put(BassDTO.BassResultColumn.stored_on, "2012_10_25_17_17_15_EDT");
            put(BassDTO.BassResultColumn.software, "PICARD");
            put(BassDTO.BassResultColumn.molecular_barcode_sequence, "TCCAGCAA");
            put(BassDTO.BassResultColumn.file_name, "C17VNACXX.8.aligned.duplicates_marked.bam");
            put(BassDTO.BassResultColumn.software_provider, "BROAD");
            put(BassDTO.BassResultColumn.file_type, "read_group_bam");
            put(BassDTO.BassResultColumn.rpname, "Resistance NHGRI CIP");
            put(BassDTO.BassResultColumn.rpid, "RP-12");
            put(BassDTO.BassResultColumn.datatype, "Exome");
            put(BassDTO.BassResultColumn.rg_checksum, "0b58d1fd9bd971999d98a9cfa4215443");
        }});
        results.add(bassDTO);
        //##FILE_TYPE=read_group_bam##

        bassDTO = new BassDTO(new HashMap<BassDTO.BassResultColumn, String>() {{
            put(BassDTO.BassResultColumn.id, "BI6504245");
            put(BassDTO.BassResultColumn.gssr_barcode, "251336.0");
            put(BassDTO.BassResultColumn.run_name, "121020_SL-HAP_0228_AFCC17VNACXX");
            put(BassDTO.BassResultColumn.version, "1351051200");
            put(BassDTO.BassResultColumn.organism, "House mouse");
            put(BassDTO.BassResultColumn.hold_count, "0");
            put(BassDTO.BassResultColumn.screened, "false");
            put(BassDTO.BassResultColumn.flowcell_barcode, "C17VNACXX");
            put(BassDTO.BassResultColumn.location, "86624672");
            put(BassDTO.BassResultColumn.run_barcode, "C17VNACXX121020");
            put(BassDTO.BassResultColumn.directory,
                    "/seq/tier3/picard/C17VNACXX/C1-58_2012-10-20_2012-10-24/7/Mitch_Guttman_RAP_XistTimeCourse2-77");
            put(BassDTO.BassResultColumn.lane, "7");
            put(BassDTO.BassResultColumn.project, "G25727");
            put(BassDTO.BassResultColumn.initiative, "Mouse RAP Seq_Discretionary Fund");
            put(BassDTO.BassResultColumn.read_pairing_type, "PAIRED");
            put(BassDTO.BassResultColumn.stored_by, "[name: picard][host: node1575][IP: 10.200.97.79][OS: Linux]");
            put(BassDTO.BassResultColumn.path,
                    "/seq/tier3/picard/C17VNACXX/C1-58_2012-10-20_2012-10-24/7/Mitch_Guttman_RAP_XistTimeCourse2-77/C17VNACXX.7.aligned.duplicates_marked.bam");
            put(BassDTO.BassResultColumn.sample, "RAP_XistTimeCourse2-77");
            put(BassDTO.BassResultColumn.molecular_barcode_name, "tagged_851");
            put(BassDTO.BassResultColumn.library_name, "Mitch Guttman_RAP_XistTimeCourse2-77");
            put(BassDTO.BassResultColumn.md5, "56b947184f7f5d9522412267f784ae77");
            put(BassDTO.BassResultColumn.stored_on, "2012_10_25_17_46_11_EDT");
            put(BassDTO.BassResultColumn.software, "PICARD");
            put(BassDTO.BassResultColumn.molecular_barcode_sequence, "TCCAGCAA");
            put(BassDTO.BassResultColumn.file_name, "C17VNACXX.7.aligned.duplicates_marked.bam");
            put(BassDTO.BassResultColumn.software_provider, "BROAD");
            put(BassDTO.BassResultColumn.file_type, "read_group_bam");
            put(BassDTO.BassResultColumn.rpname, "Resistance NHGRI CIP");
            put(BassDTO.BassResultColumn.rpid, "RP-12");
            put(BassDTO.BassResultColumn.datatype, "Exome");
            put(BassDTO.BassResultColumn.rg_checksum, BassDTO.NULL);
        }});
        results.add(bassDTO);

        return results;
    }
}
