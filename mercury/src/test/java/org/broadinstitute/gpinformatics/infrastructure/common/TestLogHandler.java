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

package org.broadinstitute.gpinformatics.infrastructure.common;

import org.apache.commons.collections.CollectionUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Class is used to test logging. add this Handler to a logger and log messages will be collected for later examination.
 *
 * @see TestLogHandlerTest
 * @see java.util.logging.Handler
 */
public class TestLogHandler extends Handler {
    private static final List<LogRecord> logs = new ArrayList<>();
    private static TestLogHandler instance;
    public static TestLogHandler newInstance() {
        if (instance == null) {
            instance = new TestLogHandler();
        }
        return instance;
    }

    @Override
    public synchronized void setLevel(Level newLevel) throws SecurityException {
        super.setLevel(newLevel);
    }

    @Override
    public boolean isLoggable(LogRecord record) {
        return super.isLoggable(record);
    }

    @Override
    protected void reportError(String msg, Exception ex, int code) {
        super.reportError(msg, ex, code);
    }

    protected TestLogHandler() {
        getLogs().clear();
    }

    /**
     * Scan collected logs for message with matching regex pattern.
     *
     * @param pattern regular expression used to match log message
     *
     * @return collection of logs matching expression.
     */
    public Collection<LogRecord> findLogs(String pattern) {
        List<LogRecord> foundRecords = new ArrayList<>();
        for (LogRecord record : getLogs()) {
            if (record.getMessage().matches(pattern)) {
                foundRecords.add(record);
            }
        }
        return foundRecords;
    }

    public boolean messageMatches(String pattern) {
        return CollectionUtils.isNotEmpty(findLogs(pattern));
    }

    @Override
    public void publish(LogRecord record) {
        if (!logs.contains(record)) {
            logs.add(record);
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

    @Nonnull
    public List<LogRecord> getLogs() {
        return logs;
    }
}
