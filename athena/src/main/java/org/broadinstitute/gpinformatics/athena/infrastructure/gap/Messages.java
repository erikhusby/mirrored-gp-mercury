package org.broadinstitute.gpinformatics.athena.infrastructure.gap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 2/8/12
 * Time: 4:20 PM
 */
@XmlRootElement(name="messages")
public class Messages {
    List<Message> messages = new ArrayList<Message>();

    @XmlElement(name="message")
    public List<Message> getMessages() {
        return messages;
    }
    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}
