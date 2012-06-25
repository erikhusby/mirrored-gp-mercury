package org.broadinstitute.sequel.infrastructure.pmbridge;


public class DevPMBridgeConnectionParameters implements PMBridgeConnectionParameters {

    @Override
    public String getUrl() {
        return "http://pmbridgedev.broadinstitute.org/PMBridge";
    }
}
