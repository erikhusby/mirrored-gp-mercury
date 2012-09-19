package org.broadinstitute.gpinformatics.mercury.infrastructure.gap;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "type")
@XmlEnum
public enum MessageType {

        @XmlEnumValue("info")
        INFO("info"),

        @XmlEnumValue("warning")
        WARNING("warning"),

        @XmlEnumValue("error")
        ERROR("error");


        private final String value;

        public String getValue() {
            return value;
        }

        MessageType(String value) {
            this.value = value;
        }
    
        public static MessageType fromValue(String v) {
           for (MessageType c: MessageType.values()) {
               if (c.value.equals(v)) {
                   return c;
               }
           }
           throw new IllegalArgumentException(v);
       }
}
