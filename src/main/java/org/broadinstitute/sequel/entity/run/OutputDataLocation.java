package org.broadinstitute.sequel.entity.run;

import java.net.URL;

/**
 * Some kind of abstraction around file
 * handles, URL, cloud data access, relational
 * storage, etc. for raw sequencer output.
 *
 * Relating a named run or a named flowcell/ptp
 * with "where the heck is the data for this run"
 * is what we're trying to abstract here.
 */
public interface OutputDataLocation {

    /**
     * Maybe even this isn't generic enough, but URl
     * seems to be a good place to start the conversation.
     * @return
     */
    public URL getDataLocation();
}
