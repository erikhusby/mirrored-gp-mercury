package org.broadinstitute.gpinformatics.infrastructure.gap;

import javax.enterprise.inject.Default;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/22/12
 * Time: 12:35 PM
 */
@Default
public class GapQAConnectionParameters implements GapConnectionParameters {

    private final String username = "athena";
    private final String password = "bspbsp";
    private String QA_HOST = "http://gapdevanalysis.broadinstitute.org:8080";
//    private String QA_HOST = "http://gapqaanalysis.broadinstitute.org:8080";

    public GapQAConnectionParameters() {}

    public String getUrl(String path) {
        return QA_HOST + path;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

}
