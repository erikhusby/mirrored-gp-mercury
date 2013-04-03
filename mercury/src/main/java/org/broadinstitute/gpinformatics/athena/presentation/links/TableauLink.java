package org.broadinstitute.gpinformatics.athena.presentation.links;

import org.broadinstitute.gpinformatics.infrastructure.tableau.TableauConfig;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * This class is used to generate Tableau links for the UI.
 */
public class TableauLink {

    private TableauConfig tableauConfig;

    @Inject
    public void setTableauConfig(TableauConfig config) {
        tableauConfig = config;
    }

    /**
     * Returns a url to the tableau report.
     * @param reportName the lookup key for tableau urls in yaml config file
     * @param param1 the parameter to pass to tableau
     * @return url to the report
     */
    public String tableauReportUrl(String reportName, String param1) {
        String url = null;
        try {
            url = tableauConfig.getTableauServer() +
                    tableauConfig.getReportUrl(reportName) +
                    URLEncoder.encode(param1, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // This can't happen, UTF-8 is always supported.
        }
        return url;
    }
}