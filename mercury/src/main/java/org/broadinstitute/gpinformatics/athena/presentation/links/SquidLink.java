package org.broadinstitute.gpinformatics.athena.presentation.links;

import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConfig;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Dependent
public class SquidLink {

    private static final String SQUID_BASE_DETAILS = "/app?service=external";

    @Inject
    private SquidConfig squidConfig;

    public String workRequestUrl(String workRequestId) {
        return squidConfig.getUrl() + SQUID_BASE_DETAILS + "&page=workrequest/WorkRequestHistoryDetail&sp=S"
               + workRequestId;
    }

}
