package org.broadinstitute.pmbridge.infrastructure.squid;

import javax.enterprise.inject.Default;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/17/12
 * Time: 10:08 AM
 */
@Default
public class QASeqConnectionParameters implements SeqConnectionParameters {

//    private String squidRoot = "http://seq01.broadinstitute.org:8000/";
    private String squidRoot = "http://vsquidrc.broadinstitute.org:8000/";
//    private String squidRoot = "http://squid-ui.broadinstitute.org:8000/";
//

    @Override
    public String getSquidRoot() {
        return squidRoot;
    }
}
