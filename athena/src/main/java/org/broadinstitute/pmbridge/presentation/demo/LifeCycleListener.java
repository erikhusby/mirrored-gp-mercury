package org.broadinstitute.pmbridge.presentation.demo;

import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 6/26/12
 * Time: 9:28 AM
 */


public class LifeCycleListener implements PhaseListener {

    public PhaseId getPhaseId() {
        return PhaseId.ANY_PHASE;
    }

    public void beforePhase(PhaseEvent event) {
        System.out.println("Start Phase " + event.getPhaseId());
    }

    public void afterPhase(PhaseEvent event) {
        System.out.println("End Phase " + event.getPhaseId());
    }

}
