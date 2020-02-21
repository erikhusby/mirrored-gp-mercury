package org.broadinstitute.gpinformatics.infrastructure.spreadsheet;

import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import org.apache.poi.ss.usermodel.Workbook;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Utility class to stream a {@link Workbook} directly to the {@link HttpServletResponse}.
 */
public class StreamCreatedSpreadsheetUtil {

    public static final String XLS_MIME_TYPE = "application/vnd.ms-excel";

    public static Resolution streamSpreadsheet(final Workbook workbook, String filename) {
        return new StreamingResolution(XLS_MIME_TYPE) {
            @Override
            public void stream(HttpServletResponse response) {
                try {
                    workbook.write(response.getOutputStream());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        }.setFilename(filename);
    }

    /**
     * Create a streamed Excel spreadsheet.
     *
     * @param rows     array of rows of cells
     * @param fileName name of the file being outputted
     * @return streamed spreadsheet
     */
    public static Resolution streamSpreadsheet(Object[][] rows, String fileName) {
        return streamSpreadsheet(SpreadsheetCreator.createSpreadsheet("Sample Info", rows, SpreadsheetCreator.Type.XLSX), fileName);
    }
}