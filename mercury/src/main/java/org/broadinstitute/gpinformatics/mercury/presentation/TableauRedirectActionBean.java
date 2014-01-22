package org.broadinstitute.gpinformatics.mercury.presentation;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.tableau.TableauConfig;

import javax.inject.Inject;

/**
 * Action bean for generating URLs to Tableau to support links in Excel spreadsheets. Before Excel passes the URL to a
 * browser, it requests the URL itself (probably to perform some security checks on it). Tableau uses basic
 * authentication, which prompts Excel to prompt the user to log in, even if the user is already logged in from their
 * browser. Furthermore, the Excel login prompt asks for a login domain which, while it may be possible to figure out,
 * is not intuitive to users (it didn't work for me and I didn't spend much time trying to figure out what the domain
 * should be). Having Mercury return an unauthenticated response with a client-side redirect suppresses Excel from
 * trying to get the user to authenticate with the Tableau server.
 */
public class TableauRedirectActionBean extends CoreActionBean {

    @Inject
    private TableauConfig tableauConfig;

    public static String getPdoSequencingSamplesDashboardRedirectUrl(String pdoKey, AppConfig appConfig) {
        return appConfig.getUrl() + "/tableau/PDOSequencingSamplesDashboard.jsp?pdo=" + pdoKey;
    }

    public static String getPdoSequencingSampleDashboardUrl(String pdoKey, TableauConfig tableauConfig) {
        return tableauConfig.getUrl() + "/views/PDOSequencingSamples2/PDOSequencingSamplesDashboard?PDO=" + pdoKey;
    }

    public String getPdoSequencingSamplesDashboardUrl() {
        return getPdoSequencingSampleDashboardUrl(getContext().getRequest().getParameter("pdo"), tableauConfig);
    }
}
