package org.broadinstitute.gpinformatics.infrastructure.test;

//import org.jboss.weld.environment.se.Weld;
//import org.jboss.weld.environment.se.WeldContainer;

/**
 * Utility methods shared by tests
 */
@Deprecated
public class TestUtilities {

    public static WeldUtil bootANewWeld() {
        return new WeldUtil();
    }

}
