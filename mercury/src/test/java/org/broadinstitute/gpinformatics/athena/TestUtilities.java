package org.broadinstitute.gpinformatics.athena;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

/**
 * Utility methods shared by tests
 */
public class TestUtilities {

    public static WeldUtil bootANewWeld() {
        WeldContainer weld = new Weld().initialize();
        return new WeldUtil(weld);
    }

}
