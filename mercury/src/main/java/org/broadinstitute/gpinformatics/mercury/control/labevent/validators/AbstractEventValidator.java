package org.broadinstitute.gpinformatics.mercury.control.labevent.validators;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;

/**
 * TODO scottmat fill in javadoc!!!
 */
public abstract class AbstractEventValidator {

    public abstract void validateEvent(LabEvent targetEvent) ;

    public static void executeValidation(LabEvent targetEvent){
        AbstractEventValidator validator = null;

        switch (targetEvent.getLabEventType()) {
        case DENATURE_TO_DILUTION_TRANSFER:
            validator = new DenatureToDilutionTubeValidator();

            break;
        case DENATURE_TO_FLOWCELL_TRANSFER:
            break;

        }

        if (validator != null) {
            validator.validateEvent(targetEvent);
        }
    }

}
