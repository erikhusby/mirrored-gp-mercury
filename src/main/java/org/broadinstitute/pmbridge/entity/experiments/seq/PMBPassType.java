package org.broadinstitute.pmbridge.entity.experiments.seq;

/**
 * TODO This enum can be deprecated and deleted once the PassType enum has been extended by changing the SQUID WSDL.
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/11/12
 * Time: 11:52 AM
 */
public enum PMBPassType {

    WG,
    DIRECTED,
    RNASeq;

    public String value() {
        return name();
    }

    public static PMBPassType fromValue(String v) {
        return valueOf(v);
    }

    public static PMBPassType convertToEnumElseNull(String str) {
        for (PMBPassType eValue : PMBPassType.values()) {
            if (eValue.name().equalsIgnoreCase(str))
                return eValue;
        }
        return null;
    }


}
