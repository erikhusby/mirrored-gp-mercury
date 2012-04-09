package org.broadinstitute.sequel.presentation;

/**
 * @author breilly
 */
public class AbstractJsfBean {

    public String redirect(String result) {
        return result + "?faces-redirect=true&includeViewParams=true";
    }
}
