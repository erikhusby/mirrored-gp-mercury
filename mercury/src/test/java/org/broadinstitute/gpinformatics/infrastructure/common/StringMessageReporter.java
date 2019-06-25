/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2019 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.common;

import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class StringMessageReporter implements MessageReporter {

    private List<String> messages = new ArrayList<>();

    @Override
    public String addMessage(String message, Object... arguments) {
        String formattedMessage = MessageFormat.format(message, arguments);
        messages.add(formattedMessage);
        return formattedMessage;
    }

    public List<String> getMessages() {
        return messages;
    }
}
