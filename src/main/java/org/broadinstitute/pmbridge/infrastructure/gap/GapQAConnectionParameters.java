package org.broadinstitute.pmbridge.infrastructure.gap;

import javax.enterprise.inject.Default;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/22/12
 * Time: 12:35 PM
 */
@Default
public class GapQAConnectionParameters implements GapConnectionParameters {

    private final String username = "pmbridge";
    private final String password = "bspbsp";
    private String QA_HOST = "http://gapdev.broadinstitute.org:8080";
//    private String QA_HOST = "http://gapqa.broadinstitute.org:8080";

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
