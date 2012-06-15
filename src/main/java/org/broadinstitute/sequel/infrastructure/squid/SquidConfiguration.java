package org.broadinstitute.sequel.infrastructure.squid;


import org.broadinstitute.sequel.infrastructure.deployment.Deployment;

public class SquidConfiguration {

    private Deployment deployment;

    private String baseUrl;


    public SquidConfiguration(Deployment deployment, String baseUrl) {
        this.deployment = deployment;
        this.baseUrl = baseUrl;
    }


    public String getBaseUrl() {
        return baseUrl;
    }


    public Deployment getDeployment() {
        return deployment;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SquidConfiguration)) return false;

        SquidConfiguration that = (SquidConfiguration) o;

        if (deployment != that.deployment) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return deployment != null ? deployment.hashCode() : 0;
    }


    @Override
    public String toString() {
        return "SquidConfiguration{" +
                "deployment=" + deployment +
                ", baseUrl='" + baseUrl + '\'' +
                '}';
    }
}
