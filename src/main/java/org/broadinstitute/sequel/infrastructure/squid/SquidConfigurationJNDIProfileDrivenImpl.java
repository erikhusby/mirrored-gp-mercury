package org.broadinstitute.sequel.infrastructure.squid;

import javax.enterprise.inject.Default;

/**
 *
 * Doesn't actually work as its name would imply quite yet, there are a lot of details
 * we need to flesh out per 2012-05-25 meeting.  Basically we want to have a generic SequeL build that
 * contains all the configuration information to run in any deployment (DEV, BUILD, QA, PROD), but
 * chooses which configuration to use based on a "master switch" JNDI property looked up at runtime.
 *
 */
@Default
public class SquidConfigurationJNDIProfileDrivenImpl implements SquidConfiguration {

    @Override
    /**
     * Hardcode the integration build Squid for now
     */
    public String getBaseURL() {
        return "http://prodinfobuild.broadinstitute.org:8020/squid/";
    }
}
