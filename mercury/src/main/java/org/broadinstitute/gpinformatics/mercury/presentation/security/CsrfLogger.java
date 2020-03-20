package org.broadinstitute.gpinformatics.mercury.presentation.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.owasp.csrfguard.log.ILogger;
import org.owasp.csrfguard.log.LogLevel;

import java.io.Serializable;

/** Converts CsrfLogger to Apache commons logger. */
public class CsrfLogger implements ILogger, Serializable {
    private static final long serialVersionUID = 20200317;

    private Log log = LogFactory.getLog(getClass());

	public void log(String msg) {
        log(LogLevel.Info, msg);
    }

	public void log(LogLevel level, String msg) {
        // Demotes this verbose message to DEBUG.
        if (msg.contains("CsrfGuard analyzing request")) {
            level = LogLevel.Debug;
        }
        if (level == LogLevel.Fatal) {
            log.fatal(msg);
        } else if (level == LogLevel.Error) {
            log.error(msg);
        } else if (level == LogLevel.Warning) {
            log.warn(msg);
        } else if (level == LogLevel.Info) {
            log.info(msg);
        } else if (level == LogLevel.Debug) {
            log.debug(msg);
        } else {
            log.trace(msg);
        }
    }

	public void log(Exception exception) {
        log(LogLevel.Info, exception);
    }

	public void log(LogLevel level, Exception exception) {
	    String msg = "";
        if (level == LogLevel.Fatal) {
            log.fatal(msg, exception);
        } else if (level == LogLevel.Error) {
            log.error(msg, exception);
        } else if (level == LogLevel.Warning) {
            log.warn(msg, exception);
        } else if (level == LogLevel.Info) {
            log.info(msg, exception);
        } else if (level == LogLevel.Debug) {
            log.debug(msg, exception);
        } else {
            log.trace(msg, exception);
        }
    }
}
