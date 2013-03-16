package org.broadinstitute.gpinformatics.infrastructure.squid;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Sends message to BettaLIMS, the deck message processor in the Squid suite of applications.
 */
public interface SquidConnector extends Serializable {

    @XmlRootElement
    public static class SquidResponse implements Serializable {
        private int code;
        private String message;

        public SquidResponse() {
        }

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

        public void setCode(int code) {
            this.code = code;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    SquidResponse createRun(SolexaRunBean runInformation);
}
