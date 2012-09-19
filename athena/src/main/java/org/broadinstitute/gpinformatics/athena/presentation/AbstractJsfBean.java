package org.broadinstitute.gpinformatics.athena.presentation;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 6/28/12
 * Time: 1:01 PM
 *
 * @author breilly
 */
public class AbstractJsfBean implements Serializable {

    public String redirect(String result) {
        return result + "?faces-redirect=true&includeViewParams=true";
    }
}
