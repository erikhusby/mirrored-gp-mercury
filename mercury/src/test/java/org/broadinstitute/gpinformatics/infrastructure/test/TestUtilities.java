package org.broadinstitute.gpinformatics.infrastructure.test;

//import org.jboss.weld.environment.se.Weld;
//import org.jboss.weld.environment.se.WeldContainer;

/**
 * Utility methods shared by tests
 */
@Deprecated
public class TestUtilities {

    public static WeldUtil bootANewWeld() {
//        WeldContainer weld = new Weld().initialize();
        //service = weld.instance().select(BSPCohortSearchService.class).get();
//        return new WeldUtil(weld);
        return new WeldUtil();
    }

}
