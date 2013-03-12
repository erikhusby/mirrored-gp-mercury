package org.broadinstitute.gpinformatics.infrastructure.tableau;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.annotation.Nullable;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents the properties found in mercury-config.yaml that
 * are in the section specified by the ConfigKey annotation below.
 */
@ConfigKey("tableau")
public class TableauConfig extends AbstractConfig implements Serializable {
    private static Log logger = LogFactory.getLog(TableauConfig.class);

    private String tableauServer;
    private List<Map<String, String>> reportUrls;
    private Map<String, String> reportUrlMap;

    /**
     * Used by DB Free tests.
     */
    public TableauConfig() {
        super(null);
    }

    @Inject
    public TableauConfig(@Nullable Deployment deployment) {
        super(deployment);
    }

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
                logger.warn("yaml contains misconfigured tableau reportUrls (name=" + name + ", url=" + url + ")");
            }
        }
    }

    public List<Map<String, String>> getReportUrls() {
        return reportUrls;
    }

    /** Gets the named report url. */
    public String getReportUrl(String reportName) {
        if (reportUrlMap == null) {
            logger.warn("Needs to be initialized from yaml config.");
            return null;
        }
        return reportUrlMap.get(reportName);
    }

}
