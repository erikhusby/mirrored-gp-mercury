package org.broadinstitute.gpinformatics.infrastructure.squid;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;

import java.io.Serializable;

/**
 * Sends message to BettaLIMS, the deck message processor in the Squid suite of applications.
 */
public interface SquidConnector extends Serializable {
    class SquidResponse {
        private int code;
        private String message;

        public SquidResponse(int code, String message) {
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

    SquidResponse createRun(SolexaRunBean runInformation);
}
