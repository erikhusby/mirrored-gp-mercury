package org.broadinstitute.gpinformatics.infrastructure.bettalims;

import org.broadinstitute.gpinformatics.mercury.boundary.lims.generated.LibraryQuantRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.generated.QpcrRunBean;

import javax.ws.rs.core.Response;
import java.io.Serializable;

/**
 * Sends message to BettaLIMS, the deck message processor in the Squid suite of applications.
 */
public interface BettaLimsConnector extends Serializable {
    class BettaLimsResponse {
        private int code;
        private String message;

        public BettaLimsResponse(int code, String message) {
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

    BettaLimsResponse sendMessage(String message);

    Response createQpcrRun(QpcrRunBean qpcrRunBean);

    Response createLibraryQuants(LibraryQuantRunBean qpcrRunBean);
}
