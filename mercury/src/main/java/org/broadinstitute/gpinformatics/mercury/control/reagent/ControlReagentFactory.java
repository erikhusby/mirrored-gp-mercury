package org.broadinstitute.gpinformatics.mercury.control.reagent;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ControlReagentDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ControlReagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Creates ControlReagent entities.
 */
public class ControlReagentFactory {

    @Inject
    private ControlDao controlDao;

    @Inject
    private BarcodedTubeDao barcodedTubeDao;

    @Inject
    private ControlReagentDao controlReagentDao;

    List<BarcodedTube> make(Map<String, ControlReagentProcessor.ControlDto> mapTubeBarcodeToControl,
            MessageCollection messageCollection) {
        List<BarcodedTube> controlTubes = new ArrayList<>();

        // verify controls exist
        List<Control> controls = controlDao.findAllActive();
        Map<String, Control> mapCollabSampleIdToControl = new HashMap<>();
        for (Control control : controls) {
            mapCollabSampleIdToControl.put(control.getCollaboratorSampleId(), control);
        }
        for (ControlReagentProcessor.ControlDto controlDto : mapTubeBarcodeToControl.values()) {
            if (!mapCollabSampleIdToControl.containsKey(controlDto.getControl())) {
                messageCollection.addError("Failed to find control " + controlDto.getControl());
            }
        }

        Map<ControlReagentProcessor.ControlDto, ControlReagent> mapControlDtoToEntity = new HashMap<>();
        // fetch or create control reagent
        Set<ControlReagentProcessor.ControlDto> controlDtoSet = new HashSet<>(mapTubeBarcodeToControl.values());
        for (ControlReagentProcessor.ControlDto controlDto : controlDtoSet) {
            // What's the unique constraint for control reagents?  Control + Lot?
            List<ControlReagent> controlReagents = controlReagentDao.fetchByLot(controlDto.getLot());
            if (controlReagents.isEmpty()) {
                mapControlDtoToEntity.put(controlDto, new ControlReagent(controlDto.getControl(), controlDto.getLot(),
                        controlDto.getExpiration(), mapCollabSampleIdToControl.get(controlDto.getControl())));
            }
        }

        // create tubes, associate with reagents
        Map<String, BarcodedTube> mapBarcodeToTube = barcodedTubeDao.findByBarcodes(mapTubeBarcodeToControl.keySet());
        for (Map.Entry<String, ControlReagentProcessor.ControlDto> stringControlDtoEntry :
                mapTubeBarcodeToControl.entrySet()) {
            String tubeBarcode = stringControlDtoEntry.getKey();
            if (mapBarcodeToTube.get(tubeBarcode) != null) {
                messageCollection.addError("Tube is already in the database: " + tubeBarcode);
                continue;
            }
            BarcodedTube barcodedTube = new BarcodedTube(tubeBarcode);
            ControlReagentProcessor.ControlDto controlDto = stringControlDtoEntry.getValue();
            barcodedTube.addReagent(mapControlDtoToEntity.get(controlDto));
            controlTubes.add(barcodedTube);
        }
        return controlTubes;
    }

}
