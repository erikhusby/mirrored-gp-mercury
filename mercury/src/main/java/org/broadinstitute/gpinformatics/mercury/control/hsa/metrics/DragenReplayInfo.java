package org.broadinstitute.gpinformatics.mercury.control.hsa.metrics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DragenReplayInfo {

    @JsonProperty("system")
    private System system;

    public System getSystem() {
        return system;
    }

    public void setSystem(System system) {
        this.system = system;
    }

    @JsonPropertyOrder({
            "dragen_version",
            "nodename",
            "kernel_release"
    })
    public static class System {

        @JsonProperty("dragen_version")
        private String dragenVersion;

        @JsonProperty("nodename")
        private String nodename;

        @JsonProperty("kernel_release")
        private String kernelRelease;

        public String getDragenVersion() {
            return dragenVersion;
        }

        public void setDragenVersion(String dragenVersion) {
            this.dragenVersion = dragenVersion;
        }

        public String getNodename() {
            return nodename;
        }

        public void setNodename(String nodename) {
            this.nodename = nodename;
        }

        public String getKernelRelease() {
            return kernelRelease;
        }

        public void setKernelRelease(String kernelRelease) {
            this.kernelRelease = kernelRelease;
        }
    }
}
