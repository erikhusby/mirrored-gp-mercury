package org.broadinstitute.sequel.infrastructure.squid;

import org.broadinstitute.sequel.boundary.squid.SquidTopicPortype;

import javax.enterprise.context.SessionScoped;
import java.io.Serializable;

/**
 * @author Scott Matthews
 *         Date: 6/22/12
 *         Time: 2:24 PM
 */
@SessionScoped
public class PassSquidWSConnector
        extends AbstractSquidWSConnector<SquidTopicPortype>
        implements Serializable {

    @Override
    protected String getNameSpace() {
        return "urn:SquidTopic";
    }

    @Override
    protected String getServiceName() {
        return "SquidTopicService";
    }

    @Override
    protected String getWsdlLocation() {
        return "services/SquidTopicService?WSDL";
    }

}
