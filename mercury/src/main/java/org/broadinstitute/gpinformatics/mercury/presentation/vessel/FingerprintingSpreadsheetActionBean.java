/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.presentation.vessel;


import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.ValidationErrors;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Workbook;
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.SpreadsheetCreator;
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.StreamCreatedSpreadsheetUtil;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventMetadata;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselAndPosition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This Action Bean takes the barcode of a static plate designated for GAP fingerprinting
 * and streams back a newly created spreadsheet of the well contents.
 */
@UrlBinding(value = FingerprintingSpreadsheetActionBean.ACTION_BEAN_URL)
public class FingerprintingSpreadsheetActionBean extends CoreActionBean {
    private static final Logger logger = Logger.getLogger(FingerprintingSpreadsheetActionBean.class.getName());
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("M-d-yy");
    private static final String NA12878 = "NA12878";

    public static final String ACTION_BEAN_URL = "/vessel/FingerprintingSpreadsheet.action";
    private static final String CREATE_PAGE = "/vessel/create_fingerprint_spreadsheet.jsp";
    private static final String SUBMIT_ACTION = "barcodeSubmit";

    private static final List<Integer> ACCEPTABLE_SAMPLE_COUNTS = Arrays.asList(new Integer[]{48, 96});

    private String plateBarcode;
    StaticPlate staticPlate;
    Workbook workbook;

    @Inject
    private StaticPlateDao staticPlateDao;

    public String getPlateBarcode() {
        return plateBarcode;
    }

    public void setPlateBarcode(String plateBarcode) {
        this.plateBarcode = plateBarcode;
    }

    @DefaultHandler
    public Resolution view() {
        return new ForwardResolution(CREATE_PAGE);
    }

    @HandlesEvent(SUBMIT_ACTION)
    public Resolution barcodeSubmit() {
        workbook = null;
        List<FpSpreadsheetRow> dtos = makeSampleDtos(staticPlate);

        if (dtos == null) {
            // Error occurred, already messaged.
            return new ForwardResolution(CREATE_PAGE);

        } else if (dtos.isEmpty()) {
            addMessage("No samples found.");
            return new ForwardResolution(CREATE_PAGE);

        } else if (!ACCEPTABLE_SAMPLE_COUNTS.contains(dtos.size())) {
            addGlobalValidationError("Plate has a pico'd sample count of " + dtos.size() + " and it must be " +
                                     StringUtils.join(ACCEPTABLE_SAMPLE_COUNTS, " or "));
            return new ForwardResolution(CREATE_PAGE);
        }
        return streamSpreadsheet(dtos);
    }

    @ValidationMethod(on = SUBMIT_ACTION)
    public void validateNoPlate(ValidationErrors errors) {
        if (StringUtils.isBlank(plateBarcode)) {
            errors.add("barcodeTextbox", new SimpleError("Plate barcode is missing."));
        } else {
            staticPlate = staticPlateDao.findByBarcode(plateBarcode);
            if (staticPlate == null) {
                errors.add(plateBarcode, new SimpleError("Plate not found."));
            }
        }
    }

    /**
     * Makes spreadsheet row dtos from the static plate.
     */
    private List<FpSpreadsheetRow> makeSampleDtos(StaticPlate plate) {
        List<FpSpreadsheetRow> dtos = new ArrayList<>();

        // Only keeps on sample tube per position.  Prefer tubes having FP pico or secondarily Initial pico.
        Map<VesselPosition, Pair<LabVessel, LabMetric>> positionMap = new HashMap<>();
        for (VesselAndPosition vesselAndPosition : plate.getNearestTubeAncestors()) {
            LabVessel labVessel = vesselAndPosition.getVessel();
            VesselPosition position = vesselAndPosition.getPosition();

            // Always prefers the latest FP pico.
            LabMetric labMetric = labVessel.findMostRecentLabMetric(LabMetric.MetricType.FINGERPRINT_PICO);
            if (labMetric == null && !positionMap.containsKey(position)) {
                labMetric = labVessel.findMostRecentLabMetric(LabMetric.MetricType.INITIAL_PICO);
            }

            if (labMetric != null) {
                if (labMetric.getUnits() != LabMetric.LabUnit.UG_PER_ML &&
                    labMetric.getUnits() != LabMetric.LabUnit.NG_PER_UL) {
                    addGlobalValidationError("Found incompatible unit of measure for the concentration.");
                    return null;
                }
                positionMap.put(position, Pair.of(labVessel, labMetric));
            } else if (!positionMap.containsKey(position)) {
                positionMap.put(position, Pair.of(labVessel, (LabMetric)null));
            }
        }

        for (VesselPosition position : positionMap.keySet()) {
            LabVessel tube = positionMap.get(position).getLeft();
            LabMetric labMetric = positionMap.get(position).getRight();

            BigDecimal wellVolume = getFpVolume(plate, position);

            FpSpreadsheetRow dto = new FpSpreadsheetRow(position.name());
            for (SampleInstanceV2 sampleInstance : tube.getSampleInstancesV2()) {
                if (!sampleInstance.isReagentOnly()) {
                    String sampleName = sampleInstance.getMercuryRootSampleName();
                    if (StringUtils.isBlank(sampleName)) {
                        sampleName = sampleInstance.getEarliestMercurySampleName();
                    }
                    dto.setRootSampleId(sampleName);
                    dto.setSampleAliquotId(sampleName);
                    dto.setParticipantId(sampleName);
                    dto.setConcentration(labMetric != null ? labMetric.getValue() : BigDecimal.ZERO);
                    dto.setVolume(wellVolume);
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
                    // If event filled this position, volume is relevant.
                    for (CherryPickTransfer cherryPick : event.getCherryPickTransfers()) {
                        if (position.name().equals(cherryPick.getTargetPosition().name())) {
                            return new BigDecimal(metadata.getValue());
                        }
                    }
                    for (SectionTransfer sectionTransfer : event.getSectionTransfers()) {
                        if (sectionTransfer.getTargetVesselContainer().getPositions().contains(position)) {
                            return new BigDecimal(metadata.getValue());
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Makes a spreadsheet from the dtos and returns a Stream of the spreadsheet.
     */
    private Resolution streamSpreadsheet(List<FpSpreadsheetRow> dtos) {
        Map<String, Object[][]> sheets = new HashMap<>();

        String[][] participantsCells = new String[dtos.size() + 1][];
        String[][] plateCells = new String[dtos.size() + 1][];

        int rowIndex = 0;

        // Fills in the headers.
        participantsCells[rowIndex] = new String[] {"Participant Id", "Gender", "LSID", "Global Participant Id",
                "Collaborator Participant ID", "Collaborator Participant ID_2", "Collaborator Participant ID_3"};
        plateCells[rowIndex] = new String[] {"Well Position", "Participant Id", "Root Sample Id", "Sample Aliquot Id",
                "Volume (ul)", "Concentration (ng/ul)"};
        ++rowIndex;

        // Fills in the data.
        for (FpSpreadsheetRow dto : dtos) {
            participantsCells[rowIndex] = new String[] {dto.getParticipantId(), "", "", "", "", "", ""};
            plateCells[rowIndex] = new String[] {dto.getPosition(), dto.getParticipantId(), dto.getRootSampleId(),
                    dto.getSampleAliquotId(), String.valueOf(dto.getVolume()), String.valueOf(dto.getConcentration())};
            ++rowIndex;
        }

        String[] sheetNames = {"Participants", "Plate"};

        sheets.put(sheetNames[0], participantsCells);
        sheets.put(sheetNames[1], plateCells);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            workbook = SpreadsheetCreator.createSpreadsheet(sheets);
            workbook.write(out);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create spreadsheet", e);
            addGlobalValidationError("Failed to create spreadsheet");
            return new ForwardResolution(CREATE_PAGE);
        }

        StreamingResolution stream = new StreamingResolution(StreamCreatedSpreadsheetUtil.XLS_MIME_TYPE,
                new ByteArrayInputStream(out.toByteArray()));
        String filename = DATE_FORMAT.format(new Date()) + "_FP_" + plateBarcode + ".xls";
        stream.setFilename(filename);

        plateBarcode = null;
        staticPlate = null;

        return stream;
    }


    /**
     * This dto contains info for one row in the spreadsheet.
     */
    class FpSpreadsheetRow {
        private String position;          // e.g. A01
        private String participantId;     // value is always the root sample id
        private String rootSampleId;      // e.g. SM-1234 or "Negative Control" or "NA12878"
        private String sampleAliquotId;   // value is always the root sample id
        private BigDecimal volume;        // in units of uL
        private BigDecimal concentration; // in units of ng/uL

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
