package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Date;
import java.util.regex.Pattern;

public class EtlTestUtilities {

    public static final Pattern RECORD_REGEX =
            Pattern.compile(ExtractTransform.DELIM + "(?=([^\"]*\"[^\"]*\")*[^\"]*$)");

    /**
     * Uses a regular expression string for splitting the records by the {@link ExtractTransform}.DELIM that are not within an even number of double quotes.
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
                return (filename.endsWith(".dat")
                        || filename.endsWith(ExtractTransform.READY_FILE_SUFFIX))
                       || filename.equals(ExtractTransform.LAST_ETL_FILE)
                       || filename.equals(ExtractTransform.LAST_WF_CONFIG_HASH_FILE);
            }
        };
        for (File file : new File(dir).listFiles(filter)) {
            FileUtils.deleteQuietly(file);
        }
    }

    /**
     * Returns all files in the given directory, having filename timestamp in the given range.
     */
    public static File[] getDirFiles(String directoryName, long msecStart, long msecEnd) {
        String etlDateStart = ExtractTransform.secTimestampFormat.format(new Date(msecStart));
        String etlDateEnd = ExtractTransform.secTimestampFormat.format(new Date(msecEnd));
        return getDirFiles(directoryName, etlDateStart, etlDateEnd);
    }

    public static File[] getDirFiles(String directoryName, final String etlDateStringStart,
                                     final String etlDateStringEnd) {
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
        return ExtractTransform.secTimestampFormat.format(d);
    }
}