package org.broadinstitute.gpinformatics.infrastructure.tableau;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This class represents the properties found in mercury-config.yaml that
 * are in the section specified by the ConfigKey annotation below.
 */
@ConfigKey("tableau")
public class TableauConfig extends AbstractConfig implements Serializable {
    private static Logger logger = Logger.getLogger(TableauConfig.class.getName());

    private String tableauServer;
    private List<Map<String, String>> reportUrls;
    private Map<String, String> reportUrlMap;

    public String getTableauServer() {
        return tableauServer;
    }

    public void setTableauServer(String s) {
        tableauServer = s;
    }

    /** Populates the multi-key reportUrlMap from yaml's list of map<name, url> */
    public void setReportUrls(List<Map<String, String>> list) {
        reportUrls = list;

        reportUrlMap = new HashMap<String, String>();
        for (Map<String, String> map : reportUrls) {
            String name = map.get("name");
            String url = map.get("url");
            if (name != null && url != null) {
                reportUrlMap.put(name, url);
            } else {
                logger.warning("yaml contains misconfigured tableau reportUrls (name=" + name + ", url=" + url + ")");
            }
        }
    }

    public List<Map<String, String>> getReportUrls() {
        return reportUrls;
    }

    /** Gets the named report url. */
    public String getReportUrl(String reportName) {
        if (reportUrlMap == null) {
            logger.warning("Needs to be initialized from yaml config.");
            return null;
        }
        return reportUrlMap.get(reportName);
    }
}
