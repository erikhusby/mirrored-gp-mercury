package org.broadinstitute.gpinformatics.mercury.control.reagent;

import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ControlReagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Creates ControlReagent entities.
 */
public class ControlReagentFactory {
    @Inject
    private ControlDao controlDao;

    List<ControlReagent> make(Map<String, ControlReagentProcessor.ControlDto> mapTubeBarcodeToControl) {
        List<ControlReagent> controlReagents = new ArrayList<>();
        List<Control> controls = controlDao.findAllActive();
        for (ControlReagentProcessor.ControlDto controlDto : mapTubeBarcodeToControl.values()) {

        }

        for (Map.Entry<String, ControlReagentProcessor.ControlDto> stringControlDtoEntry :
                mapTubeBarcodeToControl.entrySet()) {
            stringControlDtoEntry.getKey();
            stringControlDtoEntry.getValue();
        }
        return controlReagents;
    }
    // Take DTOs from spreadsheet
    // verify control exists
    // create (or fetch?) control reagent
    // create (or fetch?) tubes, associate with reagents
}
