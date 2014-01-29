package org.broadinstitute.gpinformatics.athena.presentation;

import net.sourceforge.stripes.action.Resolution;

/**
 * Callback interface used to do db-free testing
 * of action bean handlers
 */
public interface ResolutionCallback {

    /**
     * Returns a resolution from an ActionBean.  Typical
     * implementation is a single line call
     * on an ActionBean method that returns
     * the resolution.
     * @return
     * @throws Exception
     */
    public Resolution getResolution() throws Exception;
}