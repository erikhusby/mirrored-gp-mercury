package org.broadinstitute.gpinformatics.athena;

/**
 * Utility methods shared by tests
 */
@Deprecated
public class TestUtilities {

    public static org.broadinstitute.gpinformatics.athena.WeldUtil bootANewWeld() {
//        WeldContainer weld = new Weld().initialize();
        //service = weld.instance().select(BSPCohortSearchService.class).get();
//        return new WeldUtil(weld);
        return new org.broadinstitute.gpinformatics.athena.WeldUtil();
    }

}
