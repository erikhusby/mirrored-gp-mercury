package org.broadinstitute.sequel.control.bsp;


import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BSPConnectionParametersImpl implements BSPConnectionParameters {

    private String superuserLogin = "seqsystem";
    private String superuserPassword = "bspbsp";
    private String hostname = "bsp.broadinstitute.org";
    private int port = 80;


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


}