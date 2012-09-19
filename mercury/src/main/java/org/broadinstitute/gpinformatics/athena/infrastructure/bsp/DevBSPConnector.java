package org.broadinstitute.gpinformatics.athena.infrastructure.bsp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.enterprise.inject.Alternative;

@Alternative // used to connect to the live dev BSP server
public class DevBSPConnector implements BSPConnector {

    private static Log gLog = LogFactory.getLog(DevBSPConnector.class);


}
