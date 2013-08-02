package org.broadinstitute.gpinformatics.infrastructure.bettalims;

import java.io.Serializable;

/**
 * Sends message to BettaLIMS, the deck message processor in the Squid suite of applications.
 */
public interface BettalimsConnector extends Serializable {
    class BettalimsResponse {
        private int code;
        private String message;

        public BettalimsResponse(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }

    BettalimsResponse sendMessage(String message);
}
