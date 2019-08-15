package org.broadinstitute.gpinformatics.infrastructure.jmx;


public interface PrintingMXBean {

    public Boolean getPrintingManuallyEnabled();

    public void setPrintingManuallyEnabled(Boolean enabled);

    public void reloadPrintingSettings();
}
