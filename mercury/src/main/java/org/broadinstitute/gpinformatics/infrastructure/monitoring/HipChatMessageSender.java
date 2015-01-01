package org.broadinstitute.gpinformatics.infrastructure.monitoring;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJsonJerseyClientService;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Class which sends a message to hipchat.
 * See https://www.hipchat.com/docs/api/method/rooms/message
 * to get more details about the hipchat API.
 */
@Stateful
@RequestScoped
public class HipChatMessageSender implements Serializable {

    @Inject
    private HipChatConfig config;

    private final static Log log = LogFactory.getLog(HipChatMessageSender.class);

    private static final Integer CONNECTION_TIMEOUT = 2 * 1000;

    private static final String ROOM_KEY = "room_id";

    private static final String MESSAGE_KEY = "message";

    private static final String AUTHORIZATION_TOKEN_KEY = "auth_token";

    private static final String SENT_RESPONSE = "sent";

    private static final String FROM_KEY = "from";

    private static final String FROM = "mercury";

    /**
     * Contact IT to generate another token to track usage
     * for anything other than mercury
     */
    private static final String AUTHORIZATION_TOKEN = "db3e86c451ef8ac0cddb94133e0455";

    public HipChatMessageSender() {}

    public HipChatMessageSender(HipChatConfig config) {
        this.config = config;
    }

    /**
     * Posts a simple text message to the given room, using the default
     * color.  See https://www.hipchat.com/docs/api/method/rooms/message for
     * hipchat API details.
     * @param message a plain text message
     */
    public void postMessageToGpLims(String message) {
        new JsonClient().postMessage(message, config.getGpLimsRoom());
    }

    /**
     * Simple json client for hipchat message call.
     */
    private class JsonClient extends AbstractJsonJerseyClientService {
        @Override
        protected void customizeConfig(ClientConfig clientConfig) {
            supportJson(clientConfig);
        }

        @Override
        protected void customizeClient(Client client) {
            client.setConnectTimeout(CONNECTION_TIMEOUT);
            client.setReadTimeout(CONNECTION_TIMEOUT);
        }

        /**
         * Get the local ip address, or return a string
         * that says something like "can't find an ip address"
         * @return
         */
        private String getIPAddress() {
            String ipAddress = "no ip address";
            InetAddress inetAddress = null;
            try {
                inetAddress = InetAddress.getLocalHost();
            }
            catch(UnknownHostException e) {
                log.error("Failed to find ip address during hipchat message post.",e);
            }
            if (inetAddress != null) {
                ipAddress = inetAddress.getHostAddress();
            }
            return ipAddress;
        }

        /**
         * Post a message to the given room.
         * @param text
         * @param room
         */
        private void postMessage(String text,String room) {
            String message = text + "\nfrom " + getIPAddress();
            WebResource webResource = getJerseyClient().resource(config.getBaseUrl())
                .queryParam(ROOM_KEY,room)
                .queryParam(MESSAGE_KEY,message)
                .queryParam(AUTHORIZATION_TOKEN_KEY,config.getAuthorizationToken())
                .queryParam(FROM_KEY,FROM);

            HipchatResponse response = webResource.post(HipchatResponse.class);

            if (response == null) {
                throw new RuntimeException("Got no response from hipchat server.");
            }
            if (!SENT_RESPONSE.equals(response.getStatus())) {
                throw new RuntimeException("Got a bad response from hipchat server: " + response.getStatus());
            }
        }
    }

    /**
     * Simple response DTO from the hipchat message service
     */
    private static class HipchatResponse {

        public HipchatResponse() {}

        private String status;

        private String getStatus() {
            return status;
        }

        private void setStatus(String status) {
            this.status = status;
        }
    }
}
