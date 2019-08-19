package org.broadinstitute.gpinformatics.mercury.control.hsa.metrics;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;

import javax.enterprise.context.Dependent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

@Dependent
public class DemultiplexStatsParser {

    private static final Log log = LogFactory.getLog(DemultiplexStatsParser.class);

    public List<DemultiplexStats> parseStats(InputStream inputStream, MessageCollection messageCollection) {
        CsvToBean<DemultiplexStats> builder = new CsvToBeanBuilder<DemultiplexStats>(
                new BufferedReader(new InputStreamReader(inputStream))).
                withSeparator(',').
                withQuoteChar('\'').
                withType(DemultiplexStats.class).
                build();
        try {
            return builder.parse();
        } catch (Exception e) {
            log.error("Failed to parse file " + e.getCause().getMessage(), e);
            messageCollection.addError("Failed to parse input file: " + e.getCause().getMessage());
        }

        return null;
    }

    public DragenReplayInfo parseReplayInfo(InputStream inputStream, MessageCollection messageCollection) {
        try {
            DragenReplayInfo dragenReplayInfo = new ObjectMapper().readValue(inputStream, DragenReplayInfo.class);
            return dragenReplayInfo;
        } catch (JsonParseException e) {
            String errMsg = "Failed to parse JSON Replay metrics";
            log.error(errMsg, e);
            messageCollection.addError(errMsg);
        } catch (JsonMappingException e) {
            String errMsg = "Failed to map JSON Replay metrics";
            log.error(errMsg, e);
            messageCollection.addError(errMsg);
        } catch (IOException e) {
            String errMsg = "Failed to read JSON Replay metrics";
            log.error(errMsg, e);
            messageCollection.addError(errMsg);
        }
        return null;
    }
}
