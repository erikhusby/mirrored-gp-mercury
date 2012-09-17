package org.broadinstitute.pmbridge.infrastructure.bsp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.enterprise.inject.Alternative;

@Alternative
public class QABSPConnector implements BSPConnector {

    private static Log gLog = LogFactory.getLog(QABSPConnector.class);

}
