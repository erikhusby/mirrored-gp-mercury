package org.broadinstitute.sequel.quotes;


import java.beans.ConstructorProperties;

public class QuoteConnectionParametersImpl implements QuoteConnectionParameters {

    private String url;
    private String username;
    private String password;

    @ConstructorProperties({"url", "username", "password"})
    public QuoteConnectionParametersImpl(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }



    public String getUrl() {
        return url;
    }


    public String getUsername() {
        return username;
    }


    public String getPassword() {
        return password;
    }


}
