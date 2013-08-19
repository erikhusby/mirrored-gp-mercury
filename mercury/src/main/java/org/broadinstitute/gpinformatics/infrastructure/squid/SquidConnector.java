package org.broadinstitute.gpinformatics.infrastructure.squid;

import com.sun.jersey.api.client.UniformInterfaceException;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ReadStructureRequest;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Defines a set of Methods that provide a access to Squid JAX-RS Services
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

    /**
     * Encapsulates the logic for Mercury to be able to create a JAX-RS call for Squids createRun
     * @param runInformation JAXB bean that encapsulates all information necessary to register a new run
     * @return An object that represents the results of the JAX-RS Squid request
     */
    SquidResponse createRun(SolexaRunBean runInformation);

    /**
     * Sends the given read structure changes to squid's
     * solexa_run_synopsis table
     *
     * @param readStructureData
     * @param squidWSUrl
     */
    SquidResponse saveReadStructure(@Nonnull ReadStructureRequest readStructureData,
                           @Nonnull String squidWSUrl) throws UniformInterfaceException;


}
