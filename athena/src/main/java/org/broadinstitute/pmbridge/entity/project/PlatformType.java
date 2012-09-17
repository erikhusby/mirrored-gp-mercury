package org.broadinstitute.pmbridge.entity.project;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/26/12
 * Time: 3:32 PM
 */
public enum PlatformType {

    BSP("BSP", "Biological Samples Platform", "Biological Samples", "BSP"),
    GSP("GSP", "Genome Sequencing Platform", "DNA Sequencing", "PASS"),
    GAP("GAP", "Genetic Analysis Platform", "Genetic Analysis", "GAP");

    private String shortName;
    private String longName;
    private String quoteServerPlatformName;
    private String systemPrefix;

    private PlatformType(String shortName, String longName, String quoteServerPlatformName, String systemPrefix) {
        this.shortName = shortName;
        this.longName = longName;
        this.quoteServerPlatformName = quoteServerPlatformName;
        this.systemPrefix = systemPrefix;
    }

    public String getShortName() {
        return shortName;
    }

    public String getLongName() {
        return longName;
    }

    public String getQuoteServerPlatformName() {
        return quoteServerPlatformName;
    }

    public String getSystemPrefix() {
        return systemPrefix;
    }

}
