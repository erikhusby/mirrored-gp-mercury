package org.broadinstitute.pmbridge.infrastructure.quote;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "approvalStatus")
@XmlEnum
public enum ApprovalStatus {

        @XmlEnumValue("Pending")
        PENDING("Pending"),

        @XmlEnumValue("Submitted For Review")
        SUBMITTED_FOR_REVIEW("Submitted For Review"),

        @XmlEnumValue("Approved")
        APPROVED("Approved"),

        @XmlEnumValue("Issued")
        ISSUED("Issued"),

        @XmlEnumValue("Funded")
        FUNDED("Funded"),

        @XmlEnumValue("Done")
        TERMINATED("Done"),
                
        @XmlEnumValue("")
        BLANK("");

        private final String value;

        public String getValue() {
            return value;
        }

        ApprovalStatus(String value) {
            this.value = value;
        }
    
        public static ApprovalStatus fromValue(String v) {
           for (ApprovalStatus c: ApprovalStatus.values()) {
               if (c.value.equals(v)) {
                   return c;
               }
           }
           throw new IllegalArgumentException(v);
       }

}
