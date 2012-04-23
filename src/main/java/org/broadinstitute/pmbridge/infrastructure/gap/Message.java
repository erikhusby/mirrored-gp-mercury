package org.broadinstitute.pmbridge.infrastructure.gap;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="message")
public class Message {

    private MessageType type;
    private String message;
    private String field;
    private String data;

    public Message() {}

    @XmlAttribute(name="type", required=true)
    public MessageType getType() {
        return type;
    }
    public void setType(MessageType type) {
        this.type = type;
    }

    @XmlAttribute(name="message", required=true)
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    @XmlAttribute(name="field")
    public String getField() {
        return field;
    }
    public void setField(String field) {
        this.field = field;
    }

    @XmlAttribute(name="data")
    public String getData() {
        return data;
    }
    public void setData(String data) {
        this.data = data;
    }

}
