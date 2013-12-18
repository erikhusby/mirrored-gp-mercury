package org.broadinstitute.gpinformatics.mercury.presentation;

import org.apache.commons.logging.Log;

import java.text.MessageFormat;

/**
 * Objects can implement this interface to allow callers to send messages to them, e.g. to provide user feedback.
 */
public interface MessageReporter {

    /**
     * Call this method to add a message.  Messages are passed to {@link MessageFormat#format(String, Object...)}.
     *
     * @param message The message to display
     * @param arguments if present, arguments to display in the message text using {@link MessageFormat#format(String, Object...)}
     *
     * @return the message that was reported
     */
    String addMessage(String message, Object... arguments);

    /**
     * Use this to avoid null checks for a reporter.
     */
    MessageReporter UNUSED = new MessageReporter() {
        @Override
        public String addMessage(String message, Object... arguments) {
            return null;
        }
    };

    /**
     * A MessageReporter wrapper for the logging API, useful when writing tests that call APIs that use MessageReporter.
     */
    class LogReporter implements MessageReporter {

        private final Log log;

        public LogReporter(Log log) {
            this.log = log;
        }

        @Override
        public String addMessage(String message, Object... arguments) {
            String formattedMessage = MessageFormat.format(message, arguments);
            log.info(formattedMessage);
            return formattedMessage;
        }
    }
}
