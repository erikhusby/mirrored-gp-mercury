package org.broadinstitute.gpinformatics.athena.infrastructure.squid;

import javax.enterprise.inject.Default;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/17/12
 * Time: 10:08 AM
 */
@Default
public class QASeqConnectionParameters implements SeqConnectionParameters {

    private String squidRoot = "http://seq01.broadinstitute.org:8020/";    // dev
//    private String squidRoot = "http://vsquidrc.broadinstitute.org:8000/";    // qa
//    private String squidRoot = "http://squid-ui.broadinstitute.org:8000/";    // prod

    @Override
    public String getSquidRoot() {
        return squidRoot;
    }
}
