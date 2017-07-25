package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

/**
 * Represents one row of the lane info table found in FCT or MISEQ Jira tickets.
 */
public class JiraLaneInfo {
    private String lane;
    private String loadingVessel;
    private String loadingConc;
    private String lcset;
    private String productNames;

    /**
     * Cracks one Jira field record into the individual values.
     * @param jiraFieldRecord may or may not end with '\n', and may or may not start or end with '|'.
     */
    public JiraLaneInfo(@Nullable String jiraFieldRecord) {
        String input = StringUtils.chomp(StringUtils.trimToEmpty(jiraFieldRecord));
        String[] tokens = StringUtils.removeEnd(StringUtils.removeStart(input, "|"), "|").split("\\|");
        lane = StringUtils.trimToNull(tokens[0]);
        loadingVessel = tokens.length > 1 ? tokens[1] : null;
        loadingConc = tokens.length > 2 ? tokens[2] : null;
        lcset = tokens.length > 3 ? tokens[3] : null;
        productNames = tokens.length > 4 ? tokens[4] : null;
    }

    @Nullable
    public String getLane() {
        return lane;
    }

    @Nullable
    public String getLoadingVessel() {
        return loadingVessel;
    }

    @Nullable
    public String getLoadingConc() {
        return loadingConc;
    }

    @Nullable
    public String getLcset() {
        return lcset;
    }

    @Nullable
    public String getProductNames() {
        return productNames;
    }

    @Override
    public String toString() {
        return "JiraLaneInfo{" +
                "lane='" + lane + '\'' +
                ", loadingVessel='" + loadingVessel + '\'' +
                ", loadingConc='" + loadingConc + '\'' +
                ", lcset='" + lcset + '\'' +
                ", productNames='" + productNames + '\'' +
                '}';
    }
}
