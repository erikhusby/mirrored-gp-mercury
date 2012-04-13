package org.broadinstitute.sequel.presentation;

import java.io.Serializable;

/**
 * @author breilly
 */
public class AbstractJsfBean implements Serializable {

    public String redirect(String result) {
        return result + "?faces-redirect=true&includeViewParams=true";
    }
}
