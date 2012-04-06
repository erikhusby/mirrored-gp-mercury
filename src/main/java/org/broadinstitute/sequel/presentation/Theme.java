package org.broadinstitute.sequel.presentation;

import javax.inject.Named;

/**
 * @author breilly
 */
@Named
public class Theme {

    public String getTheme() {
        String theme;

        // default included with primefaces jar; no separate theme jar needed
//        theme = "aristo";

        // other themes that require dependency on theme jar
//        theme = "casablanca";
//        theme = "home";
//        theme = "hot-sneaks";
//        theme = "overcast";
        theme = "pepper-grinder";
//        theme = "smoothness";
//        theme = "sunny";
        return theme;
    }
}
