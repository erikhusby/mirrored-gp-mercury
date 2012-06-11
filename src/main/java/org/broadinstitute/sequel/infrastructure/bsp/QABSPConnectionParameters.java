package org.broadinstitute.sequel.infrastructure.bsp;

import javax.enterprise.inject.Default;

@Default
public class QABSPConnectionParameters implements BSPConnectionParameters {

    private String superuserLogin = "seqsystem";
    private String superuserPassword = "bspbsp";
    private String hostname = "gapqa3.broadinstitute.org";
    private int port = 8080;


    public QABSPConnectionParameters() {}

	public String getSuperuserLogin() {
		return superuserLogin;
	}


	public String getSuperuserPassword() {
		return superuserPassword;
	}


	public String getHostname() {
		return hostname;
	}


	public int getPort() {
		return port;
	}


    @Override
    public String getUsername() {
        return getSuperuserLogin();
    }

    @Override
    public String getPassword() {
        return getSuperuserPassword();
    }
}