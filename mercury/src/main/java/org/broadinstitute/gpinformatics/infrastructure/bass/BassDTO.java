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

import java.util.EnumSet;
import java.util.Map;

// https://confluence.broadinstitute.org/display/BASS/Application+Programming+Interface
public class BassDTO {
    public static final String FILE_TYPE_BAM="bam";
    public static final String FILE_TYPE_READ_GROUP_BAM="read_group_bam";
    public static final String FILE_TYPE_PICARD="picard";

    private final Map<BassResultColumns, String> columnToValueMap;

    public BassDTO(Map<BassResultColumns, String> columnToValueMap) {
        this.columnToValueMap = columnToValueMap;
    }

    public String getId() {
        return id;
    }

    public enum BassResultColumns {
        id, version, hold_count, organism, location, directory, project, initiative, path, stored_by, sample, md5,
        stored_on, software, file_name, software_provider, gssr_barcode, run_name, screened, flowcell_barcode,
        run_barcode, lane, read_pairing_type, molecular_barcode_name, library_name, molecular_barcode_sequence,
        file_type
    }

    public static final EnumSet<BassResultColumns> PICARD_COLUMNS = EnumSet.of(
            BassDTO.BassResultColumns.id,
            BassDTO.BassResultColumns.version,
            BassDTO.BassResultColumns.hold_count,
            BassDTO.BassResultColumns.organism,
            BassDTO.BassResultColumns.location,
            BassDTO.BassResultColumns.directory,
            BassDTO.BassResultColumns.project,
            BassDTO.BassResultColumns.initiative,
            BassDTO.BassResultColumns.path,
            BassDTO.BassResultColumns.stored_by,
            BassDTO.BassResultColumns.sample,
            BassDTO.BassResultColumns.md5,
            BassDTO.BassResultColumns.stored_on,
            BassDTO.BassResultColumns.software,
            BassDTO.BassResultColumns.file_name,
            BassDTO.BassResultColumns.software_provider,
            BassDTO.BassResultColumns.file_type
    );

    public static final EnumSet<BassResultColumns> BAM_COLUMNS = EnumSet.of(
            BassDTO.BassResultColumns.id,
            BassDTO.BassResultColumns.version,
            BassDTO.BassResultColumns.hold_count,
            BassDTO.BassResultColumns.organism,
            BassDTO.BassResultColumns.location,
            BassDTO.BassResultColumns.directory,
            BassDTO.BassResultColumns.project,
            BassDTO.BassResultColumns.initiative,
            BassDTO.BassResultColumns.path,
            BassDTO.BassResultColumns.stored_by,
            BassDTO.BassResultColumns.sample,
            BassDTO.BassResultColumns.md5,
            BassDTO.BassResultColumns.stored_on,
            BassDTO.BassResultColumns.software,
            BassDTO.BassResultColumns.file_name,
            BassDTO.BassResultColumns.software_provider,
            BassDTO.BassResultColumns.file_type
    );

    public static final EnumSet<BassResultColumns> READ_GROUP_BAM_COLUMNS = EnumSet.of(
            BassDTO.BassResultColumns.id,
            BassDTO.BassResultColumns.gssr_barcode,
            BassDTO.BassResultColumns.run_name,
            BassDTO.BassResultColumns.version,
            BassDTO.BassResultColumns.organism,
            BassDTO.BassResultColumns.hold_count,
            BassDTO.BassResultColumns.screened,
            BassDTO.BassResultColumns.flowcell_barcode,
            BassDTO.BassResultColumns.location,
            BassDTO.BassResultColumns.run_barcode,
            BassDTO.BassResultColumns.directory,
            BassDTO.BassResultColumns.lane,
            BassDTO.BassResultColumns.project,
            BassDTO.BassResultColumns.initiative,
            BassDTO.BassResultColumns.read_pairing_type,
            BassDTO.BassResultColumns.stored_by,
            BassDTO.BassResultColumns.path,
            BassDTO.BassResultColumns.sample,
            BassDTO.BassResultColumns.molecular_barcode_name,
            BassDTO.BassResultColumns.library_name,
            BassDTO.BassResultColumns.md5,
            BassDTO.BassResultColumns.stored_on,
            BassDTO.BassResultColumns.software,
            BassDTO.BassResultColumns.molecular_barcode_sequence,
            BassDTO.BassResultColumns.file_name,
            BassDTO.BassResultColumns.software_provider,
            BassDTO.BassResultColumns.file_type
    );

private String id;
private int version;
private int hold_count;
private String organism;
private String location;
private String directory;
private String project;
private String initiative;
private String path;
private String stored_by;
private String sample;
private String md5;
private String stored_on;
private String software;
private String file_NAME;
private String software_provider;
private String file_TYPE;
//
//    BI6504697
//1
//0
//House mouse
//86625120
///seq/tier3b/picard_aggregation/G25727/RAP_XistTimeCourse2-77/v1
//G25727
//Mouse
//            RAP Seq_Discretionary Fund
///seq/tier3b/picard_aggregation/G25727/RAP_XistTimeCourse2-77/v1/RAP_XistTimeCourse2-77.bam
//[name: picard][host: node1173][IP: 69.173.75.173][OS: Linux]
//RAP_XistTimeCourse2-77
//8a11a9376768a8dfcd997a1d3fae3c43
//2012_10_25_18_50_32_EDT
//            PICARD
//RAP_XistTimeCourse2-77.bam
//BROAD
//bam

}
