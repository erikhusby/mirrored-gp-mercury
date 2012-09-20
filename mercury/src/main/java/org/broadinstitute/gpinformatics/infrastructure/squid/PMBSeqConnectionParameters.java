package org.broadinstitute.gpinformatics.infrastructure.squid;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/17/12
 * Time: 10:04 AM
 */
public interface PMBSeqConnectionParameters {


    public final String SQUID_NAMESPACE = "urn:SquidTopic";
    public final String SQUID_TOPIC = "SquidTopicService";
    public final String SQUID_WSDL = "squid/services/SquidTopicService?WSDL";

    public String getSquidRoot();


}
