package org.broadinstitute.sequel.boundary.samplekit;

import org.broadinstitute.sequel.boundary.*;

import javax.jws.WebParam;
import javax.jws.WebService;


@WebService(targetNamespace = "urn:SpfSampleKit",
        portName = "SampleKitService",
        serviceName = "SampleKitService",
        name = "SampleKitService",
        endpointInterface = "org.broadinstitute.sequel.boundary.GSSRSampleKitPortType")
public class SampleKitSOAPServiceImpl implements GSSRSampleKitPortType {
    @Override
    public GSSRSampleKitResponse createGSSRSampleKit(@WebParam(name = "gssrSampleKitRequest", partName = "gssrSampleKitRequest") GSSRSampleKitRequest gssrSampleKitRequest) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public GSSRCollaboratorsResponse getAllGSSRCollaborators() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public GSSRBroadPIsResponse getAllGSSRBroadPIs() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
