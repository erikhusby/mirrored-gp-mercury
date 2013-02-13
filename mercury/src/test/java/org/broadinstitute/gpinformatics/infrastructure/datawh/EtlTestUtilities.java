package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EtlTestUtilities {

    /**
     * Deletes all the files written by these tests including .dat, isReady, and lastEtlRun files.
     */
    public static void deleteEtlFiles(String dir) {
        // Uses current year month day to determine whether to delete a file.
        final String yyyymmdd = (new SimpleDateFormat("yyyyMMdd")).format(new Date());
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dirname, String filename) {
                return (filename.startsWith(yyyymmdd) && filename.endsWith(".dat")
                        || filename.startsWith(yyyymmdd) && filename.endsWith(ExtractTransform.READY_FILE_SUFFIX))
                        || filename.equals(ExtractTransform.LAST_ETL_FILE);
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
        final long yyyymmddHHMMSSstart = Long.parseLong(ExtractTransform.secTimestampFormat.format(new Date(msecStart)));
        final long yyyymmddHHMMSSend = Long.parseLong(ExtractTransform.secTimestampFormat.format(new Date(msecEnd)));
        File dir = new File(directoryName);
        File[] list = dir.listFiles(new FilenameFilter() {
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
        return list;
    }

    public static String format(boolean b) {
        return b ? "T" : "F";
    }

    public static String format(Date d) {
        return ExtractTransform.secTimestampFormat.format(d);
    }
}