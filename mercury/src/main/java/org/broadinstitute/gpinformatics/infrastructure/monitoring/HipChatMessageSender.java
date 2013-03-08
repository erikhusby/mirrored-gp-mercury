package org.broadinstitute.gpinformatics.infrastructure.monitoring;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJsonJerseyClientService;

/**
 * Class which sends a message to hipchat.
 * See https://www.hipchat.com/docs/api/method/rooms/message
 * to get more details about the hipchat API.
 */
public class HipChatMessageSender {

    private final static Log log = LogFactory.getLog(HipChatMessageSender.class);

    // todo arz how to setup configuration parameters in yaml files?

    private static final long HIPCHAT_THREAD_TIMEOUT = 2 * 1000;

    private static final String HIPCHAT_BASE_URL = "https://www.hipchat.com/v1/rooms/message";

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

    /**
     * Posts a simple text message to the given room, using the default
     * color.  See https://www.hipchat.com/docs/api/method/rooms/message for
     * hipchat API details.
     * @param text
     * @param room
     */
    // todo arz how to wrap this thing an injected bean so that only production errors and dev errors go to the right rooms?
    public void postSimpleTextMessage(final String text,final String room) {
        Thread messageSendingThread = new Thread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        new JsonClient().postMessage(text,room);
                                                    }
                                                },
                                                "HipChat Message Sender");
        messageSendingThread.setDaemon(true);
        messageSendingThread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                log.error("Error sending hipchat message " + text + " to room " + room + ".",throwable);
            }
        });
        messageSendingThread.start();
        try {
            messageSendingThread.join(HIPCHAT_THREAD_TIMEOUT);
        }
        catch(InterruptedException e) {
            log.error("Thread posting message " + text + " to hipchat room " + room + " was interrupted.");
        }
    }

    /**
     * Simple json client for hipchat message call.
     */
    private static class JsonClient extends AbstractJsonJerseyClientService {
        @Override
        protected void customizeConfig(ClientConfig clientConfig) {
            supportJson(clientConfig);
        }

        @Override
        protected void customizeClient(Client client) {

        }

        public void postMessage(String text,String room) {
            WebResource webResource = getJerseyClient().resource(HIPCHAT_BASE_URL)
                .queryParam(ROOM_KEY,room)
                .queryParam(MESSAGE_KEY,text)
                .queryParam(AUTHORIZATION_TOKEN_KEY,AUTHORIZATION_TOKEN)
                .queryParam(FROM_KEY,FROM);

            HipchatResponse response = webResource.post(HipchatResponse.class);

            if (response != null) {
                if (!SENT_RESPONSE.equals(response.getStatus()))  {
                    throw new RuntimeException("Got a bad response from hipchat server: " + response.getStatus());
                }
                // else it's all good
            }
            else {
                throw new RuntimeException("Got no response from hipchat server.");
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
