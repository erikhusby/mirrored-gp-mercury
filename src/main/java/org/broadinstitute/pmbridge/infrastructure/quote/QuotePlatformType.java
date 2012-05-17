package org.broadinstitute.pmbridge.infrastructure.quote;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/17/12
 * Time: 3:51 PM
 */
public enum QuotePlatformType {

    BSP("Biological+Samples"),
    SEQ("DNA+Sequencing"),
    GAP("Genome+Analysis");

    private String platformName;

    private QuotePlatformType(final String platformName) {
        this.platformName = platformName;
    }

    public String getPlatformName() {
        return platformName;
    }

}
