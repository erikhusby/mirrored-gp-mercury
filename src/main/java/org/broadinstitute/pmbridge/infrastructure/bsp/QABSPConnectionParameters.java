package org.broadinstitute.pmbridge.infrastructure.bsp;

import javax.enterprise.inject.Default;

@Default
public class QABSPConnectionParameters implements BSPConnectionParameters {

    private String superuserLogin = "pmbridge";
    private String superuserPassword = "bspbsp";
    private String hostname = "gapqa3.broadinstitute.org";
//    private String hostname = "gapdev2.broadinstitute.org";
    private int port = 8080;

    public QABSPConnectionParameters() {}


    @Override
	public String getSuperuserLogin() {
		return superuserLogin;
	}

    @Override
	public String getSuperuserPassword() {
		return superuserPassword;
	}

    @Override
	public String getHostname() {
		return hostname;
	}

    @Override
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