package org.broadinstitute.sequel.bsp.conf;

public class BSPConnectionParametersImpl implements BSPConnectionParameters {

    private String superuserLogin = "seqsystem";
    private String superuserPassword = "bspbsp";
    private String hostname = "bsp.broadinstitute.org";
    private int port = 80;


    public BSPConnectionParametersImpl(String superuserLogin, String superuserPassword, String hostname, int port) {
        this.superuserLogin = superuserLogin;
        this.superuserPassword = superuserPassword;
        this.hostname = hostname;
        this.port = port;
    }


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