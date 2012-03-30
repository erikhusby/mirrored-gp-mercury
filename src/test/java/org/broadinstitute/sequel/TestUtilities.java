package org.broadinstitute.sequel;

import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.easymock.EasyMock;
//import org.jboss.weld.environment.se.Weld;
//import org.jboss.weld.environment.se.WeldContainer;

/**
 * Utility methods shared by tests
 */
public class TestUtilities {

    public static WeldUtil bootANewWeld() {
//        WeldContainer weld = new Weld().initialize();
        //service = weld.instance().select(BSPSampleSearchService.class).get();
//        return new WeldUtil(weld);
        return new WeldUtil();
    }

}
