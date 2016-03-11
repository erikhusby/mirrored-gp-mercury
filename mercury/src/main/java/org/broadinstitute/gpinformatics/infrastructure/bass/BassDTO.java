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
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTracker;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Holds values obtained from Bass
 *
 * @see <a href="https://confluence.broadinstitute.org/display/BASS/Application+Programming+Interface">Bass API Documentation</a>
 * @see <a href="https://bass.broadinstitute.org/list?rpid=RP-200">Example call to Bass WS</a>
 */
public class BassDTO {
    public static final String DATA_TYPE_EXOME = "Exome";
    public static final String DATA_TYPE_RNA = "RNA";
    public static final String DATA_TYPE_WGS = "WGS";

    public static final String FILETYPE = "file_type";
    static final String BASS_NULL_VALUE = "[NULL]";
    private final SimpleDateFormat BASS_DATE_FORMAT = new SimpleDateFormat("y_M_d_k_m_s_z");

    private static final Log log = LogFactory.getLog(BassDTO.class);

    private final Map<BassResultColumn, String> columnToValue;

    public BassDTO(Map<BassResultColumn, String> columnToValue) {
        this.columnToValue = columnToValue;
    }

// todo: should be in interface?
    public SubmissionTracker.Key getSubmissionKey(String repository, String libraryDescriptor) {
        return new SubmissionTracker.Key(getSample(), getFilePath(), getVersion().toString(), repository, libraryDescriptor);
    }

    private String getFilePath() {
        return getValue(BassResultColumn.path);
    }

    public enum FileType {
        BAM("bam"),
        PICARD("picard"),
        READ_GROUP_BAM("read_group_bam"),
        ALL("all");
        private String value;

        FileType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static FileType byValue(String fileType) {
            for (FileType type : FileType.values()) {
                if (fileType.equals(type.getValue())) {
                    return type;
                }
            }
            throw new IllegalArgumentException("No enum constant for " + fileType);
        }
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
        file_type;

        /**
         * Check if we have specified header.
         *
         * @param header header to check for
         *
         * @return true if we have an enum value for header or false otherwise;
         */
        public static boolean hasHeader(String header) {
            for (BassResultColumn bassResultColumn : BassResultColumn.values()) {
                if (bassResultColumn.name().equals(header)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * if the project starts with {@link ResearchProject#PREFIX} it is aggregated by {@link ResearchProject}
     * @return true if this BassDTO is aggregated by ResearchProject
     */
    public boolean isAggregatedByResearchProject() {
        return getProject().startsWith(ResearchProject.PREFIX);
    }

    /**
     * Get the value from supplied column.
     *
     * @param column to obtain data from,
     *
     * @return String representation of the value or an empty string if the field is empty.
     */
    @Nonnull
    String getValue(BassResultColumn column) {
        String value = columnToValue.get(column);
        if (!(value == null || value.equals(BASS_NULL_VALUE))) {
            return value;
        }
        return "";
    }

    /**
     * Get the Integer value from supplied column.
     *
     * @param column to obtain data from,
     *
     * @return Integer representation of the value or null if the field is empty.
     *
     * @throws java.lang.NumberFormatException if the value can not be parsed.
     */
    @Nullable
    private Integer getInt(@Nonnull BassResultColumn column) throws NumberFormatException {
        String value = getValue(column);
        if (StringUtils.isNotBlank(value)) {
            return Integer.parseInt(value);
        }
        return null;
    }

    /**
     * Get the Date value from supplied column.
     *
     * @param column to obtain data from,
     *
     * @return Date representation of the value or null if the field is empty.
     *
     * @see BassDTO#BASS_DATE_FORMAT
     */
    @Nullable
    private Date getDate(@Nonnull BassResultColumn column) {
        String value = getValue(column);
        Date resultDate = null;
        if (StringUtils.isNotBlank(value)) {
            try {
                resultDate = BASS_DATE_FORMAT.parse(value);
            } catch (ParseException e) {
                log.error(String.format("Could not parse date '%s'", value), e);
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

    public void setSample(String sample) {
            columnToValue.put(BassResultColumn.sample, sample);
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
