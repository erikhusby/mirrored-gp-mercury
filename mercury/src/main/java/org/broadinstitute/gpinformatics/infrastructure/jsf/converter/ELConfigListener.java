package org.broadinstitute.gpinformatics.infrastructure.jsf.converter;

/**
 * This class sets a system property that does not coerce a jsf integer
 * components null value to zero.
 * Other servlet container properties can be set in here too.
 *
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 11/2/12
 * Time: 4:03 PM
 *
 * @author breilly
 */

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class ELConfigListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        System.setProperty("org.apache.el.parser.COERCE_TO_ZERO", "false");
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // NO-OP
    }
}
