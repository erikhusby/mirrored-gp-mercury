package org.broadinstitute.gpinformatics.mercury.infrastructure.gap;

import org.broadinstitute.gpinformatics.athena.control.UsernameAndPassword;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/22/12
 * Time: 12:32 PM
 */
public interface GapConnectionParameters extends UsernameAndPassword {

    static final String GAP_BASE_URL = "/ws/project_management/";
    public static final String GAP_PLATFORMS_URL = GAP_BASE_URL + "get_experiment_platforms";
    public static final String GAP_USERS_URL = GAP_BASE_URL + "auth/get_users_by_role?role_name=Project%20Manager&domain=GAP";
    public static final String GAP_EXPERIMENTS_URL = GAP_BASE_URL + "get_experiment_plan";
    public static final String GAP_TECHNOLOGIES_URL = GAP_BASE_URL + "get_experiment_platforms";
    public static final String GAP_CREATE_EXPERIMENT_URL = GAP_BASE_URL + "create_experiment_plan";

//    gapEmail=onofrio@broadinstitute.org,mharden@broadinstitute.org,yvesboie@broadinstitute.org,crenshaw@broadinstitute.org,mccrory@broadinstitute.org


    String getUrl(String path);

    String getUsername();

    String getPassword();
}
