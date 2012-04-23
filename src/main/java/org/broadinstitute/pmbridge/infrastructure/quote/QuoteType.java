package org.broadinstitute.pmbridge.infrastructure.quote;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "quoteType")
@XmlEnum
public enum QuoteType {

        @XmlEnumValue("Open-Ended")
        OPEN_ENDED("Open-Ended"),

        @XmlEnumValue("Standard")
        STANDARD("Standard"),

        @XmlEnumValue("Re-issuable")
        REUSEABLE("Re-issuable"),

        @XmlEnumValue("")
        BLANK("");

        private final String value;

        public String getValue() {
            return value;
        }

        QuoteType(String value) {
            this.value = value;
        }
    
        public static QuoteType fromValue(String v) {
           for (QuoteType c: QuoteType.values()) {
               if (c.value.equals(v)) {
                   return c;
               }
           }
           throw new IllegalArgumentException(v);
       }

}
