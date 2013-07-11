package org.broadinstitute.gpinformatics.mercury.control.labevent.validators;

import org.broadinstitute.gpinformatics.mercury.bettalims.generated.CherryPickSourceType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO scottmat fill in javadoc!!!
 */
public class DenatureToDilutionTubeValidator extends AbstractEventValidator{

    @Override
    public void validateEvent(LabEvent targetEvent) {

        PlateCherryPickEvent event = OrmUtil.proxySafeCast(targetEvent, PlateCherryPickEvent.class);

        Map<LabVessel, LabVessel> mapDilutionByDenature = new HashMap<>();

        for(CherryPickSourceType source : event.getSource()) {

        }


    }
}
