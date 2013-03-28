package org.broadinstitute.gpinformatics.infrastructure.squid;

import org.broadinstitute.gpinformatics.mercury.boundary.squid.GSSRSampleKitPortType;
import org.broadinstitute.gpinformatics.mercury.boundary.squid.GSSRSampleKitRequest;
import org.broadinstitute.gpinformatics.mercury.boundary.squid.GSSRSampleKitResponse;

import javax.inject.Inject;

/**
 * @author breilly
 */
public class SampleKitSOAPServiceImpl
        extends SquidWebServiceClient<GSSRSampleKitPortType>
        implements SampleKitSOAPService {

    private SquidConfig squidConfig;

    // TODO: see about injecting this directly into SquidWebServiceClient
    public SampleKitSOAPServiceImpl(SquidConfig squidConfig) {
        this.squidConfig = squidConfig;
    }

    @Override
    public GSSRSampleKitResponse createGSSRSampleKit(GSSRSampleKitRequest request) {
        return squidCall().createGSSRSampleKit(request);
    }

    @Override
    protected SquidConfig getSquidConfig() {
        return squidConfig;
    }

    @Override
    protected String getNameSpace() {
        return "urn:SpfSampleKit";
    }

    @Override
    protected String getServiceName() {
        return "SampleKitService";
    }

    @Override
    protected String getWsdlLocation() {
        return "/services/SampleKitService?WSDL";
    }
}
