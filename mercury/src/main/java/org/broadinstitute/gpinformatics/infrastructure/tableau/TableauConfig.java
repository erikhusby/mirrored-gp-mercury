package org.broadinstitute.gpinformatics.infrastructure.tableau;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.Serializable;
import java.util.logging.Logger;

/**
 * This class represents the properties found in mercury-config.yaml that
 * are in the section specified by the ConfigKey annotation below.
 */
@ConfigKey("tableau")
public class TableauConfig extends AbstractConfig implements Serializable {

    private String trustedTicketServer;
    private String username;
    private List<Map<String, String>> reportUrls;
    private Map<String, String> reportUrlMap;

    public TableauConfig() {}

    public String getTrustedTicketServer() {
        return trustedTicketServer;
    }

    public void setTrustedTicketServer(String s) {
        trustedTicketServer = s;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setReportUrls(List<Map<String, String>> list) {
        reportUrls = list;

        // Populates the multi-key reportUrlMap from yaml's list of map<name, url>.
        reportUrlMap = new HashMap<String, String>();
        for (Map<String, String> map : reportUrls) {
            // collects the name and url
            String name = null;
            String url = null;
            for (Map.Entry<String, String> ent: map.entrySet()) {
                if ("name".equals(ent.getKey())) {
                    name = ent.getValue();
                }
                if ("url".equals(ent.getKey())) {
                    url = ent.getValue();
                }
            }
            if (name != null && url != null) {
                reportUrlMap.put(name, url);
            } else {
                Logger.getLogger(this.getClass().getName()).warning
                        ("yaml has misconfigured tableau reportUrls (name=" + name + ", url=" + url + ")");
            }
        }
    }

    public List<Map<String, String>> getReportUrls() {
        return reportUrls;
    }

    /** Gets the named report url. */
    public String getReportUrl(String reportName) {
        return reportUrlMap.get(reportName);
    }
}
