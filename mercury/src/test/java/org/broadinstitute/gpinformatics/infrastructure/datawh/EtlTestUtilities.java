package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Date;
import java.util.regex.Pattern;

public class EtlTestUtilities {

    private static final Pattern RECORD_REGEX =
            Pattern.compile(ExtractTransform.DELIMITER + "(?=([^\"]*\"[^\"]*\")*[^\"]*$)");

    /**
     * Uses a regular expression string for splitting the records by the {@link ExtractTransform}.DELIMITER
     * that are not within an even number of double quotes.
     *
     * @return String array of records split based on the string pattern.
     */
    public static String[] splitRecords(CharSequence records) {
        return RECORD_REGEX.split(records);
    }

    /**
     * Deletes all the files written by these tests including .dat, isReady, and lastEtlRun files.
     */
    public static void deleteEtlFiles(String dir) {
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dirname, String filename) {
                return ((filename.length() > "20140807112133_X.dat".length()) &&
                        StringUtils.isNumeric(filename.substring(0,14)) &&
                        filename.startsWith("20") &&
                        filename.substring(14,15).equals("_") &&
                        (filename.endsWith(".dat") || filename.endsWith(ExtractTransform.READY_FILE_SUFFIX))
                       || filename.equals(ExtractTransform.LAST_ETL_FILE)
                       || filename.equals(ExtractTransform.LAST_WF_CONFIG_HASH_FILE));
            }
        };
        for (File file : new File(dir).listFiles(filter)) {
            FileUtils.deleteQuietly(file);
        }
    }

    /**
     * Returns all etl-related files in the given directory.
     */
    public static File[] getEtlFiles(String dir) {
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dirname, String filename) {
                return (filename.endsWith(".dat")
                        || filename.endsWith(ExtractTransform.READY_FILE_SUFFIX))
                       || filename.equals(ExtractTransform.LAST_ETL_FILE)
                       || filename.equals(ExtractTransform.LAST_WF_CONFIG_HASH_FILE);
            }
        };
        return new File(dir).listFiles(filter);
    }

    /**
     * Returns all files in the given directory, having filename timestamp in the given range.
     */
    public static File[] getDirFiles(String directoryName, long msecStart, long msecEnd) {
        String etlDateStart = ExtractTransform.formatTimestamp(new Date(msecStart));
        String etlDateEnd = ExtractTransform.formatTimestamp(new Date(msecEnd));
        return getDirFiles(directoryName, etlDateStart, etlDateEnd);
    }

    public static File[] getDirFiles(String directoryName, String etlDateStringStart,
                                     String etlDateStringEnd) {
        final long yyyymmddHHMMSSstart = Long.parseLong(etlDateStringStart);
        final long yyyymmddHHMMSSend = Long.parseLong(etlDateStringEnd);
        File dir = new File(directoryName);
        return dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dirname, String filename) {
                try {
                    // Only cares about files named <dateTime>_<*>
                    String s = filename.split("_")[0];
                    long timestamp = Long.parseLong(s);
                    return (timestamp >= yyyymmddHHMMSSstart && timestamp <= yyyymmddHHMMSSend);
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        });
    }

    public static String format(boolean b) {
        return b ? "T" : "F";
    }

    public static String format(Date d) {
        return ExtractTransform.formatTimestamp(d);
    }

    /**
     * Given a single SQL Loader record, verify that its values match the expected values.  If a non-matching value
     * is found an assertion is fired.
     *
     * @param record the record to verify, comma separated values
     * @param matchValues the values to match
     */
    static void verifyRecord(String record, String... matchValues) {
        int i = 0;
        String[] parts = record.split(",");
        for (String matchValue : matchValues) {
            Assert.assertEquals(parts[i++], matchValue);
        }
        Assert.assertEquals(parts.length, i);
    }
}