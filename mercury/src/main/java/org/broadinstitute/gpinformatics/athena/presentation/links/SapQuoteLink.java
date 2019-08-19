package org.broadinstitute.gpinformatics.athena.presentation.links;

import org.broadinstitute.gpinformatics.infrastructure.sap.SapConfig;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

@Dependent
public class SapQuoteLink {
    private static final String SAP_DETAILS = "/my-quotes/";


    private SapConfig sapConfig;

    @Inject
    public SapQuoteLink(SapConfig sapConfig) {
        this.sapConfig = sapConfig;
    }

    public String sapUrl(String sapQuoteId) {
        return sapConfig.getUrl() + SAP_DETAILS + sapQuoteId + "/details";
    }
}
