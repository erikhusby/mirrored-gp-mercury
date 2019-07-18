package org.broadinstitute.gpinformatics.infrastructure.jmx;

import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * JMX Bean for enabling/disabling printing.
 */
@Singleton
@Startup
public class PrintingControl extends AbstractJMXRegister implements PrintingMXBean {

    private static Boolean printingManuallyEnabled = null;

    /**
     * Default is to not have set this variable and the application should print if the environment allows it. Otherwise
     * the printing can be enabled/disabled manually based on the state of this variable.
     * (Value of this variable indicates: null = use default environment state, true = allow print, false = do not print)
     */
    public Boolean getPrintingManuallyEnabled() {
        return printingManuallyEnabled;
    }

    public void setPrintingManuallyEnabled(Boolean printingEnabled) {
        printingManuallyEnabled = printingEnabled;
    }

    // Static accessor so that we can verify whether printing is manually enabled/disabled via jconsole.
    public static Boolean isPrintingEnabled(){ return printingManuallyEnabled; }
}
