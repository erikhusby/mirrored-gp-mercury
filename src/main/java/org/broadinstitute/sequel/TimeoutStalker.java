package org.broadinstitute.sequel;


import java.util.Date;

public class TimeoutStalker extends AbstractStalker {

    /**
     * Creates a new one, using alertDate
     * as the date in the future at which
     * start sending timeout alerts.
     * @param alertDate
     */
    public TimeoutStalker(Date alertDate) {

    }

    @Override
    public void stalk(LabEvent event) {

    }


    @Override
    public void stalk() {

    }
}
