package org.broadinstitute.gpinformatics.mercury.control.hsa.metrics;

import org.apache.commons.io.input.ReversedLinesFileReader;

import javax.enterprise.context.Dependent;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads Slurm log file of a GsUtil upload task to get completion percentage.
 */
@Dependent
public class GsUtilLogReader {

    private final static Pattern UPLOAD_PATTERN = Pattern.compile(".*\\[(\\d+ files)\\]\\[ (\\d+\\.\\d+) (\\w+)\\/ (\\d+\\.\\d+) (\\w+)\\]  (\\d+\\.\\d+ \\w+\\/\\w+)");

    public static Result parseTransferStatus(File logFile) throws IOException {
        if (!logFile.exists()) {
            return null;
        }
        ReversedLinesFileReader reversedLinesFileReader = new ReversedLinesFileReader(logFile);
        int nLines = 10; //Read the last 10 lines at the most
        int counter = 0;
        while(counter < nLines) {
            String line = reversedLinesFileReader.readLine();
            if (line == null) {
                break;
            }
            line = line.trim();
            Matcher matcher = UPLOAD_PATTERN.matcher(line);
            if (matcher.matches()) {
                String numFiles = matcher.group(1);
                float uploaded = Float.parseFloat(matcher.group(2));
                String uploadedSizeScale = matcher.group(3);
                float fileSize = Float.parseFloat(matcher.group(4));
                String fileSizeScale = matcher.group(5);
                String rate = matcher.group(6);
                return new Result(line, numFiles, rate, uploaded, uploadedSizeScale, fileSize, fileSizeScale);
            }
            counter++;
        }
        return null;
    }

    public static class Result {
        private final String line;
        private String numFiles;
        private String rate;
        private float uploaded;
        private final String uploadedScale;
        private float fileSize;
        private final String fileSizeScale;

        public Result(String line, String numFiles, String rate, float uploaded, String uploadedScale, float fileSize,
                      String fileSizeScale) {
            this.line = line;
            this.numFiles = numFiles;
            this.rate = rate;
            this.uploaded = uploaded;
            this.uploadedScale = uploadedScale;
            this.fileSize = fileSize;
            this.fileSizeScale = fileSizeScale;
        }

        public String getLine() {
            return line;
        }

        public String getNumFiles() {
            return numFiles;
        }

        public String getRate() {
            return rate;
        }

        public float getUploaded() {
            return uploaded;
        }

        public float getFileSize() {
            return fileSize;
        }

        public String getUploadedScale() {
            return uploadedScale;
        }

        public String getFileSizeScale() {
            return fileSizeScale;
        }
    }
}
