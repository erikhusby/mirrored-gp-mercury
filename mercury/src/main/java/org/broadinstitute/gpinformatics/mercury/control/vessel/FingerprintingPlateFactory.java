package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.SpreadsheetCreator;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventMetadata;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ControlReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselAndPosition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleDataFetcher;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Control class supporting FingerprintingSpreadsheetActionBean
 */
@Dependent
public class FingerprintingPlateFactory {
    /** String for the FP spreadsheet that identifies a filler tube. */
    public static final String NA12878 = "NA12878";
    /** String for the FP spreadsheet that identifies a negative control tube. */
    public static final String NEGATIVE_CONTROL = "negative control";

    @Inject
    private ControlDao controlDao;

    public FingerprintingPlateFactory() {
    }

    public FingerprintingPlateFactory(ControlDao controlDao) {
        this.controlDao = controlDao;
    }

    /**
     * Makes spreadsheet row dtos from the static plate.
     * Puts any error/info messages in the list of errorMessages.
     */
    public List<FpSpreadsheetRow> makeSampleDtos(StaticPlate plate, List<String> errorMessages) throws Exception {
        List<FpSpreadsheetRow> dtos = new ArrayList<>();

        // Takes the pico value from the nearest upstream tube.
        Set<VesselPosition> positionsFound = new HashSet<>();
        for (VesselAndPosition vesselAndPosition : plate.getNearestTubeAncestors()) {
            LabVessel tube = vesselAndPosition.getVessel();
            VesselPosition position = vesselAndPosition.getPosition();
            if (!positionsFound.add(position)) {
                errorMessages.add(position.name() + " has multiple immediate upstream tubes.");
                continue;
            }
            // Prefers FP pico over Initial pico.
            LabMetric labMetric = tube.findMostRecentLabMetric(LabMetric.MetricType.FINGERPRINT_PICO);
            if (labMetric == null) {
                labMetric = tube.findMostRecentLabMetric(LabMetric.MetricType.INITIAL_PICO);
            }
            // Validates the units of measure.
            if (labMetric != null && labMetric.getUnits() != LabMetric.LabUnit.UG_PER_ML &&
                labMetric.getUnits() != LabMetric.LabUnit.NG_PER_UL) {
                errorMessages.add(position.name() + " has incompatible unit of measure for the concentration.");
                continue;
            }

            int fingerprintingPlateSetupCount = 0;
            boolean isRework = false;
            Set<LabEvent> inPlaceLabEvents = tube.getEvents();
            for (LabEvent labEvent: inPlaceLabEvents) {
                if (labEvent.getLabEventType() == LabEventType.FINGERPRINTING_PLATE_SETUP) {
                    fingerprintingPlateSetupCount++;
                }
            }
            if (fingerprintingPlateSetupCount > 1) {
                List<LabEventType> registrationEventTypes =
                        Collections.singletonList(LabEventType.POND_REGISTRATION);
                TransferTraverserCriteria.VesselForEventTypeCriteria eventTypeCriteria
                        = new TransferTraverserCriteria.VesselForEventTypeCriteria(registrationEventTypes, true);
                tube.evaluateCriteria(eventTypeCriteria,
                        TransferTraverserCriteria.TraversalDirection.Descendants);
                isRework = !eventTypeCriteria.getVesselsForLabEventType().isEmpty();
            }

            // Volume applies to all samples on the plate.
            BigDecimal wellVolume = getFpVolume(plate, position);

            if (CollectionUtils.isEmpty(errorMessages)) {
                FpSpreadsheetRow dto = new FpSpreadsheetRow(position.name());
                for (SampleInstanceV2 sampleInstance : tube.getSampleInstancesV2()) {
                    String sampleName = null;
                    boolean positiveControl = false;
                    boolean negativeControl = false;

                    // Sets the sample name for these cases:
                    // - Reagent-only tubes are labeled as NA12878 filler tubes.
                    // - Negative controls are labeled NEGATIVE_CONTROL.
                    // - Positive controls and samples get the root/earliest sample name.
                    if (sampleInstance.isReagentOnly()) {
                        for (Reagent reagent :sampleInstance.getReagents()) {
                            if (OrmUtil.proxySafeIsInstance(reagent, ControlReagent.class)) {
                                ControlReagent controlReagent = OrmUtil.proxySafeCast(reagent, ControlReagent.class);
                                if (controlReagent.getControl().getType() == Control.ControlType.POSITIVE) {
                                    sampleName = controlReagent.getControl().getCollaboratorParticipantId();
                                    positiveControl = true;
                                } else if (controlReagent.getControl().getType() == Control.ControlType.NEGATIVE) {
                                    sampleName = NEGATIVE_CONTROL;
                                    negativeControl = true;
                                }
                            }
                        }
                        if (sampleName == null) {
                            throw new Exception(position.name() +
                                                " is reagent-only but neither positive nor negative control.");
                        }
                    } else if (isNegativeControl(sampleInstance)) {
                        sampleName = NEGATIVE_CONTROL;
                        negativeControl = true;
                    } else {
                        sampleName = sampleInstance.getRootOrEarliestMercurySampleName();
                        if (StringUtils.isBlank(sampleName)) {
                            sampleName = "[no sample id]";
                        }
                    }

                    dto.setRootSampleId(sampleName);
                    dto.setSampleAliquotId(sampleName);
                    dto.setParticipantId(sampleName);
                    if (positiveControl) {
                        dto.setConcentration(labMetric != null ? labMetric.getValue() : new BigDecimal("20"));
                    } else {
                        dto.setConcentration(labMetric != null ? labMetric.getValue() : BigDecimal.ZERO);
                    }
                    dto.setVolume(wellVolume != null ? wellVolume : BigDecimal.ZERO);
                    dto.setPositiveControl(positiveControl);
                    dto.setNegativeControl(negativeControl);
                    dto.setRework(isRework);
                    dtos.add(dto);
                }
            }
        }
        Collections.sort(dtos, BY_POSITION_THEN_ROOT);
        return dtos;
    }

    /**
     * Returns the fingerprint plate well volume which is found in the lab event metadata
     * of the section transfer event that filled the plate.
     */
    private BigDecimal getFpVolume(StaticPlate staticPlate, VesselPosition position) {
        for (LabEvent event : staticPlate.getTransfersTo()) {
            // Finds the volume metadata and ignores events that don't have it.
            for (LabEventMetadata metadata : event.getLabEventMetadatas()) {
                if (metadata.getLabEventMetadataType().equals(LabEventMetadata.LabEventMetadataType.Volume)) {
                    // If event filled this position, volume is relevant.  Currently a "stamp" is used, i.e.
                    // a full plate section transfer from rack to plate.
                    for (SectionTransfer sectionTransfer : event.getSectionTransfers()) {
                        if (sectionTransfer.getTargetVesselContainer().getPositions().contains(position)) {
                            return new BigDecimal(metadata.getValue());
                        }
                    }
                    for (CherryPickTransfer cherryPick : event.getCherryPickTransfers()) {
                        if (position.name().equals(cherryPick.getTargetPosition().name())) {
                            return new BigDecimal(metadata.getValue());
                        }
                    }
                }
            }
        }
        return null;
    }



    /**
     * Determines whether a FP tube should be treated as a negative control, which also
     * means it's not explicitly part of a PDO (i.e. has no bucket entries).
     */
    private boolean isNegativeControl(SampleInstanceV2 sampleInstance) {
        MercurySample mercurySample = sampleInstance.getRootOrEarliestMercurySample();
        if (mercurySample != null && CollectionUtils.isEmpty(sampleInstance.getAllBucketEntries())) {

            SampleData sampleData = new MercurySampleDataFetcher().fetchSampleData(mercurySample);
            if (sampleData != null) {
                // Control name will be found in sample metadata.
                String controlName = sampleData.getCollaboratorParticipantId();
                if (controlName != null) {
                    Control control = controlDao.findByCollaboratorParticipantId(controlName);
                    if (control != null) {
                        return control.getType() == Control.ControlType.NEGATIVE;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Makes a spreadsheet from the dtos and returns a Stream of the spreadsheet.
     */
    public Workbook makeSpreadsheet(List<FpSpreadsheetRow> dtos) throws IOException {
        Map<String, Object[][]> sheets = new HashMap<>();

        int numParticipantCells = 0;
        for (FpSpreadsheetRow dto : dtos) {
            if (!dto.isNegativeControl() && !dto.isPositiveControl() && !dto.isRework()) {
                numParticipantCells++;
            }
        }
        String[][] participantsCells = new String[numParticipantCells + 1][];
        String[][] plateCells = new String[dtos.size() + 1][];

        int rowIndex = 0;
        int participantRowIndex = 0;

        // Fills in the headers.
        participantsCells[rowIndex] = new String[] {"Participant Id", "Gender", "LSID", "Global Participant Id",
                "Collaborator Participant ID", "Collaborator Participant ID_2", "Collaborator Participant ID_3"};
        plateCells[rowIndex] = new String[] {"Well Position", "Participant Id", "Root Sample Id", "Sample Aliquot Id",
                "Volume (ul)", "Concentration (ng/ul)"};
        ++rowIndex;
        ++participantRowIndex;

        // Fills in the data.
        for (FpSpreadsheetRow dto : dtos) {
            // gender=0 means "unspecified"
            if (!dto.isNegativeControl() && !dto.isPositiveControl() && !dto.isRework()) {
                participantsCells[participantRowIndex] = new String[]{dto.getParticipantId(), "0", "", "", "", "", ""};
                participantRowIndex++;
            }
            plateCells[rowIndex] = new String[] {dto.getPosition(), dto.getParticipantId(), dto.getRootSampleId(),
                    dto.getSampleAliquotId(), String.valueOf(dto.getVolume()), String.valueOf(dto.getConcentration())};
            ++rowIndex;
        }

        String[] sheetNames = {"Participants", "Plate"};

        sheets.put(sheetNames[0], participantsCells);
        sheets.put(sheetNames[1], plateCells);

        return SpreadsheetCreator.createSpreadsheet(sheets);
    }


    /**
     * This dto contains info for one row in the spreadsheet.
     */
    public class FpSpreadsheetRow {
        private String position;          // e.g. A01
        private String participantId;     // value is always the root sample id
        private String rootSampleId;      // e.g. SM-1234 or "negative control" or "NA12878"
        private String sampleAliquotId;   // value is always the root sample id
        private BigDecimal volume;        // in units of uL
        private BigDecimal concentration; // in units of ng/uL
        private boolean isPositiveControl;
        private boolean isNegativeControl;
        private boolean isRework; // if there exists another FingerprintPlateSetup event and a downstream Pond Reg.

        FpSpreadsheetRow(String position) {
            this.position = position;
        }

        public String getPosition() {
            return position;
        }

        public String getParticipantId() {
            return participantId;
        }

        public void setParticipantId(String participantId) {
            this.participantId = participantId;
        }

        public String getRootSampleId() {
            return rootSampleId;
        }

        public void setRootSampleId(String rootSampleId) {
            this.rootSampleId = rootSampleId;
        }

        public String getSampleAliquotId() {
            return sampleAliquotId;
        }

        public void setSampleAliquotId(String sampleAliquotId) {
            this.sampleAliquotId = sampleAliquotId;
        }

        public BigDecimal getVolume() {
            return volume;
        }

        public void setVolume(BigDecimal volume) {
            this.volume = volume;
        }

        public BigDecimal getConcentration() {
            return concentration;
        }

        public void setConcentration(BigDecimal concentration) {
            this.concentration = concentration;
        }

        public boolean isPositiveControl() {
            return isPositiveControl;
        }

        public void setPositiveControl(boolean positiveControl) {
            isPositiveControl = positiveControl;
        }

        public boolean isNegativeControl() {
            return isNegativeControl;
        }

        public void setNegativeControl(boolean negativeControl) {
            isNegativeControl = negativeControl;
        }

        public boolean isRework() {
            return isRework;
        }

        public void setRework(boolean rework) {
            isRework = rework;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof FpSpreadsheetRow)) {
                return false;
            }

            FpSpreadsheetRow that = (FpSpreadsheetRow) o;

            if (!position.equals(that.position)) {
                return false;
            }
            if (rootSampleId != null ? !rootSampleId.equals(that.rootSampleId) : that.rootSampleId != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = position.hashCode();
            result = 31 * result + (rootSampleId != null ? rootSampleId.hashCode() : 0);
            return result;
        }

    }

    public static final Comparator<FpSpreadsheetRow> BY_POSITION_THEN_ROOT = new Comparator<FpSpreadsheetRow>() {
        @Override
        public int compare(FpSpreadsheetRow o1, FpSpreadsheetRow o2) {
            String key1 = o1.position.concat("_").concat(o1.rootSampleId != null ? o1.rootSampleId : "");
            String key2 = o2.position.concat("_").concat(o2.rootSampleId != null ? o2.rootSampleId : "");
            return key1.compareTo(key2);
        }
    };

}
