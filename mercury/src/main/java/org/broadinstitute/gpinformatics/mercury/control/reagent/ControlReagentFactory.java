package org.broadinstitute.gpinformatics.mercury.control.reagent;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ControlReagentDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ControlReagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
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

    List<BarcodedTube> make(InputStream spreadsheetStream,
            MessageCollection messageCollection) {
        try {
            ControlReagentProcessor controlReagentProcessor = new ControlReagentProcessor("Sheet1");
            messageCollection.addErrors(PoiSpreadsheetParser.processSingleWorksheet(spreadsheetStream,
                    controlReagentProcessor));
            Map<String, ControlReagentProcessor.ControlDto> mapTubeBarcodeToControl =
                    controlReagentProcessor.getMapTubeBarcodeToControl();

            List<Control> controls = controlDao.findAllActive();
            Map<String, BarcodedTube> mapBarcodeToTube = barcodedTubeDao.findByBarcodes(mapTubeBarcodeToControl.keySet());
            Set<ControlReagentProcessor.ControlDto> controlDtoSet = new HashSet<>(mapTubeBarcodeToControl.values());
            List<String> lots = new ArrayList<>();
            for (ControlReagentProcessor.ControlDto controlDto : controlDtoSet) {
                lots.add(controlDto.getLot());
            }
            Map<String, ControlReagent> mapLotToControl = controlReagentDao.fetchMapLotToControl(lots);

            return getBarcodedTubes(mapTubeBarcodeToControl, messageCollection, controls, mapBarcodeToTube,
                    mapLotToControl);
        } catch (InvalidFormatException | IOException | ValidationException e) {
            throw new RuntimeException(e);
        }
    }

    @DaoFree
    private List<BarcodedTube> getBarcodedTubes(
            Map<String, ControlReagentProcessor.ControlDto> mapTubeBarcodeToControl,
            MessageCollection messageCollection,
            List<Control> controls,
            Map<String, BarcodedTube> mapBarcodeToTube,
            Map<String, ControlReagent> mapLotToControl) {

        List<BarcodedTube> controlTubes = new ArrayList<>();
        Map<String, Control> mapCollabSampleIdToControl = new HashMap<>();
        for (Control control : controls) {
            mapCollabSampleIdToControl.put(control.getCollaboratorSampleId(), control);
        }
        for (ControlReagentProcessor.ControlDto controlDto : mapTubeBarcodeToControl.values()) {
            if (!mapCollabSampleIdToControl.containsKey(controlDto.getControl())) {
                messageCollection.addError("Failed to find control " + controlDto.getControl());
            }
        }

        // create tubes, associate with reagents
        for (Map.Entry<String, ControlReagentProcessor.ControlDto> stringControlDtoEntry :
                mapTubeBarcodeToControl.entrySet()) {
            String tubeBarcode = stringControlDtoEntry.getKey();
            ControlReagentProcessor.ControlDto controlDto = stringControlDtoEntry.getValue();

            if (mapBarcodeToTube.get(tubeBarcode) != null) {
                messageCollection.addError("Tube is already in the database: " + tubeBarcode);
                continue;
            }
            BarcodedTube barcodedTube = new BarcodedTube(tubeBarcode);
            ControlReagent controlReagent = mapLotToControl.get(controlDto.getLot());
            if (controlReagent == null) {
                controlReagent = new ControlReagent(controlDto.getControl(), controlDto.getLot(),
                        controlDto.getExpiration(), mapCollabSampleIdToControl.get(controlDto.getControl()));
            }
            barcodedTube.addReagent(controlReagent);
            controlTubes.add(barcodedTube);
        }
        return controlTubes;
    }

}
