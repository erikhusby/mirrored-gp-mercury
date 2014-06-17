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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.Nonnull;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

// https://confluence.broadinstitute.org/display/BASS/Application+Programming+Interface
//https://bass.broadinstitute.org/list?project=G25727&sample=RAP_XistTimeCourse2-77&project=G25727&sample=RAP_XistTimeCourse2-77
public class BassDTO {
    public static final String FILE_TYPE_BAM = "bam";
    public static final String FILE_TYPE_READ_GROUP_BAM = "read_group_bam";
    public static final String FILE_TYPE_PICARD = "picard";
    static final String NULL = "[NULL]";
    private final SimpleDateFormat BASS_DATE_FORMAT = new SimpleDateFormat("y_M_d_k_m_s_z");

    private static final Log log = LogFactory.getLog(BassDTO.class);

    private final Map<BassResultColumn, String> columnToValue;

    public BassDTO(Map<BassResultColumn, String> columnToValue) {
        this.columnToValue = columnToValue;
    }

    public enum BassResultColumn {
        id,
        version,
        hold_count,
        organism,
        location,
        directory,
        rpname,
        rpid,
        datatype,
        project,
        initiative,
        path,
        rg_checksum,
        stored_by,
        sample,
        md5,
        stored_on,
        software,
        file_name,
        software_provider,
        gssr_barcode,
        run_name,
        screened,
        flowcell_barcode,
        run_barcode,
        lane,
        read_pairing_type,
        molecular_barcode_name,
        library_name,
        molecular_barcode_sequence,
        file_type
    }

    private String id;
    private String version;
    private String holdCount;
    //    private int version;
//    private int holdCount;
    private String organism;
    private String location;
    private String directory;
    private String project;
    private String initiative;
    private String path;
    private String storedBy;
    private String sample;
    private String md5;
    private String storedOn;
    //    private Date storedOn;
    private String software;
    private String fileName;
    private String softwareProvider;
    private String gssrBarcode;
    private String runName;
    private String screened;
    private String flowcellBarcode;
    private String runBarcode;
    private String lane;
    //    private int lane;
    private String readPairingType;
    private String molecularBarcodeName;
    private String libraryName;
    private String molecularBarcodeSequence;
    private String fileType;
    private String rpname;
 private String rpid;
 private String datatype;
    private String  rgChecksum;

    /**
     * Use these methods to access the data, so missing elements are returned as empty strings, which is what
     * the clients of this API expect.
     *
     * @param column column to look up
     *
     * @return value at column, or empty string if missing
     */
    @Nonnull
    public String getValue(BassResultColumn column) {
        String value = columnToValue.get(column);
        if (!(value == null || value.equals(NULL))) {
            return value;
        }
        return "";
    }

    private Integer getInt(BassResultColumn column) {
        String s = getValue(column);
        if (StringUtils.isNotBlank(s)) {
            return Integer.parseInt(s);
        }
        return null;
    }

    Date getDate(BassResultColumn column) {
        String s = getValue(column);
        Date resultDate = null;
        if (StringUtils.isNotBlank(s)) {
            try {
                resultDate = BASS_DATE_FORMAT.parse(s);
            } catch (final ParseException e) {
                log.error(String.format("Could not parse date '%s'", s));
            }
        }
        return resultDate;
    }

    public String getId() {
        return getValue(BassResultColumn.id);
    }

    public Integer getVersion() {
        return getInt(BassResultColumn.version);
    }

    public Integer getHoldCount() {
        return getInt(BassResultColumn.hold_count);
    }

    public String getOrganism() {
        return getValue(BassResultColumn.organism);
    }

    public String getLocation() {
        return getValue(BassResultColumn.location);
    }

    public String getDirectory() {
        return getValue(BassResultColumn.directory);
    }

    public String getProject() {
        return getValue(BassResultColumn.project);
    }

    public String getInitiative() {
        return getValue(BassResultColumn.initiative);
    }

    public String getPath() {
        return getValue(BassResultColumn.path);
    }

    public String getStoredBy() {
        return getValue(BassResultColumn.stored_by);
    }

    public String getSample() {
        return getValue(BassResultColumn.sample);
    }

    public String getMd5() {
        return getValue(BassResultColumn.md5);
    }

    public Date getStoredOn() {
        return getDate(BassResultColumn.stored_on);
    }

    public String getSoftware() {
        return getValue(BassResultColumn.software);
    }

    public String getFileName() {
        return getValue(BassResultColumn.file_name);
    }

    public String getSoftwareProvider() {
        return getValue(BassResultColumn.software_provider);
    }

    public String getGssrBarcode() {
        return getValue(BassResultColumn.gssr_barcode);
    }

    public String getRunName() {
        return getValue(BassResultColumn.run_name);
    }

    public String getScreened() {
        return getValue(BassResultColumn.screened);
    }

    public String getFlowcellBarcode() {
        return getValue(BassResultColumn.flowcell_barcode);
    }

    public String getRunBarcode() {
        return getValue(BassResultColumn.run_barcode);
    }

    public Integer getLane() {
        return getInt(BassResultColumn.lane);
    }

    public String getReadPairingType() {
        return getValue(BassResultColumn.read_pairing_type);
    }

    public String getMolecularBarcodeName() {
        return getValue(BassResultColumn.molecular_barcode_name);
    }

    public String getLibraryName() {
        return getValue(BassResultColumn.library_name);
    }

    public String getMolecularBarcodeSequence() {
        return getValue(BassResultColumn.molecular_barcode_sequence);
    }

    public String getFileType() {
        return getValue(BassResultColumn.file_type);
    }

    public String getRpname() {
        return getValue(BassResultColumn.rpname);
    }

    public String getRpid() {
        return getValue(BassResultColumn.rpid);
    }

    public String getDatatype() {
        return getValue(BassResultColumn.datatype);
    }

    public String getRgChecksum() {
        return getValue(BassResultColumn.rg_checksum);
    }
}
