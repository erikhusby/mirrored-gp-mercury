package org.broadinstitute.gpinformatics.athena.presentation.billing;

import net.sourceforge.stripes.action.Resolution;
import org.apache.commons.io.FileUtils;
import org.apache.poi.util.IOUtils;
import org.broadinstitute.gpinformatics.athena.boundary.orders.SampleLedgerExporter;
import org.broadinstitute.gpinformatics.athena.boundary.util.AbstractSpreadsheetExporter;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

/**
 * A Stripes Resolution for streaming a billing tracker. Uses a SampleLedgerExporter to generate the data. Upon
 * creation, the data is streamed to a temporary file in order to expose any errors in exporting. When Stripes executes
 * the resolution, the temporary file is streamed and deleted.
 */
public class BillingTrackerResolution implements Resolution {

    private final File tempFile;

    /**
     * Creates a resolution for streaming a billing tracker download. Creates a temporary file for the download from
     * data from the given SampleLedgerExporter.
     *
     * @param exporter    the exporter to fetch data from
     * @throws IOException if there is an error creating or writing to the download file
     */
    public BillingTrackerResolution(SampleLedgerExporter exporter) throws IOException {
        String filename =
                "BillingTracker-" + AbstractSpreadsheetExporter.DATE_FORMAT
                        .format(Calendar.getInstance().getTime());

        // Colon is a meta-character in Windows separating the drive letter from the rest of the path.
        filename = filename.replaceAll(":", "_");

        tempFile = File.createTempFile(filename, ".xls");
        try (OutputStream outputStream = new FileOutputStream(tempFile)) {
            exporter.writeToStream(outputStream);
        }
    }

    /**
     * Streams the previously generated temporary download file to the HTTP response. Deletes the temporary file when
     * done whether or not it was successfully written to the response.
     */
    @Override
    public void execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try (InputStream inputStream = new FileInputStream(tempFile)) {
            CoreActionBean.setFileDownloadHeaders("application/excel", tempFile.getName(), response);
            IOUtils.copy(inputStream, response.getOutputStream());
        } finally {
            FileUtils.deleteQuietly(tempFile);
        }
    }
}
