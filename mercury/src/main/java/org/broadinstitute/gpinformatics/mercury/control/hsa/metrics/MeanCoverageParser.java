package org.broadinstitute.gpinformatics.mercury.control.hsa.metrics;

import com.opencsv.CSVReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;

import javax.enterprise.context.Dependent;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

@Dependent
public class MeanCoverageParser {

    private static final Log log = LogFactory.getLog(DemultiplexStatsParser.class);

    public static Float parseMeanCoverage(InputStream inputStream, MessageCollection messageCollection) {
        Reader reader = new InputStreamReader(inputStream);
        CSVReader csvReader = new CSVReader(reader);
        try {
            String[] nextRecord = csvReader.readNext();
            return Float.valueOf(nextRecord[1]);
        } catch (Exception e) {
            log.error("Failed to parse file " + e.getCause().getMessage(), e);
            messageCollection.addError("Failed to parse input file: " + e.getCause().getMessage());
        }

        return null;
    }
}
