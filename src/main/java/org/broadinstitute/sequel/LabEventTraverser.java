package org.broadinstitute.sequel;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.Collection;
import java.util.Collections;

public class LabEventTraverser {

    private static Log gLog = LogFactory.getLog(LabEventTraverser.class);

    /**
     * Walk the event graph and build an ordered list,
     * from root to leaf, of all the state changes
     * @param sheet
     * @param leaf
     * @return
     */
    public static Collection<StateChange> getStateChangesPriorToAndIncluding(SampleSheet sheet,
                                                                             LabVessel leaf) {
        gLog.error("I haven't been written yet.  but I'll give you null.");
        return Collections.emptyList();
    }

}
