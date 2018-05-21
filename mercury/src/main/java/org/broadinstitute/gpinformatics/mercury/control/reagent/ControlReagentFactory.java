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

import javax.enterprise.context.Dependent;
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
@Dependent
public class ControlReagentFactory {

    @Inject
    private ControlDao controlDao;

    @Inject
    private BarcodedTubeDao barcodedTubeDao;

    @Inject
    private ControlReagentDao controlReagentDao;

    /**
     * From a spreadsheet, creates tubes and associates them with control reagents.
     * @param spreadsheetStream spreadsheet with tube barcodes
     * @param messageCollection errors are added to this
     * @return list of entities
     */
    public List<BarcodedTube> buildTubesFromSpreadsheet(InputStream spreadsheetStream,
            MessageCollection messageCollection) {
        try {
            // Parse spreadsheet to get DTOs.
            ControlReagentProcessor controlReagentProcessor = new ControlReagentProcessor("Sheet1");
            messageCollection.addErrors(PoiSpreadsheetParser.processSingleWorksheet(spreadsheetStream,
                    controlReagentProcessor));
            Map<String, ControlReagentProcessor.ControlDto> mapTubeBarcodeToControl =
                    controlReagentProcessor.getMapTubeBarcodeToControl();

            // Fetch existing entities from database.
            List<Control> controls = controlDao.findAllActive();
            Map<String, BarcodedTube> mapBarcodeToTube = barcodedTubeDao.findByBarcodes(mapTubeBarcodeToControl.keySet());
            Set<ControlReagentProcessor.ControlDto> controlDtoSet = new HashSet<>(mapTubeBarcodeToControl.values());
            List<String> lots = new ArrayList<>();
            for (ControlReagentProcessor.ControlDto controlDto : controlDtoSet) {
                lots.add(controlDto.getLot());
            }
            Map<String, ControlReagent> mapLotToControl = controlReagentDao.fetchMapLotToControl(lots);

            // Make tube entities.
            List<BarcodedTube> barcodedTubes = buildTubes(mapTubeBarcodeToControl, messageCollection, controls,
                    mapBarcodeToTube, mapLotToControl);
            if (!messageCollection.hasErrors()) {
                barcodedTubeDao.persistAll(barcodedTubes);
            }
            return barcodedTubes;
        } catch (InvalidFormatException | IOException | ValidationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Builds tube entities.
     * @param mapTubeBarcodeToControl DTOs from spreadsheet
     * @param messageCollection errors are added to this
     * @param controls from database, must exist
     * @param mapBarcodeToTube from database, error if they exist
     * @param mapLotToControl from database, created if they don't exist
     * @return list of entities
     */
    @DaoFree
    List<BarcodedTube> buildTubes(
            Map<String, ControlReagentProcessor.ControlDto> mapTubeBarcodeToControl,
            MessageCollection messageCollection,
            List<Control> controls,
            Map<String, BarcodedTube> mapBarcodeToTube,
            Map<String, ControlReagent> mapLotToControl) {

        List<BarcodedTube> controlTubes = new ArrayList<>();
        Map<String, Control> mapCollabSampleIdToControl = new HashMap<>();
        for (Control control : controls) {
            mapCollabSampleIdToControl.put(control.getCollaboratorParticipantId(), control);
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
                mapLotToControl.put(controlDto.getLot(), controlReagent);
            } else if (!controlReagent.getControl().getCollaboratorParticipantId().equals(controlDto.getControl())) {
                messageCollection.addError("Mismatch between lot and control: " + tubeBarcode);
            }
            barcodedTube.addReagent(controlReagent);
            controlTubes.add(barcodedTube);
        }
        return controlTubes;
    }

}
