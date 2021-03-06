package org.broadinstitute.gpinformatics.mercury.control.reagent;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reads and partially validates Probe CSV File into a list of row objects
 */
public class ReagentDesignImportProcessor {
    private static final Log log = LogFactory.getLog(ReagentDesignImportProcessor.class);
    private List<String> tubeBarcodes;

    public List<ReagentImportDto> parse(InputStream inputStream, MessageCollection messageCollection) {
        try {
            CsvToBean<ReagentImportDto> builder = new CsvToBeanBuilder<ReagentImportDto>(
                    new BufferedReader(new InputStreamReader(inputStream))).
                    withSeparator(',').
                    withQuoteChar('\'').
                    withType(ReagentImportDto.class).
                    build();
            if (builder != null) {
                List<ReagentImportDto> dtos = builder.parse();

                Set<String> allTubes = new HashSet<>();
                tubeBarcodes = dtos.stream().map(ReagentDesignImportProcessor.ReagentImportDto::getTubeBarcode)
                        .collect(Collectors.toList());

                // Find duplicate tubes in upload
                tubeBarcodes.stream()
                        .filter(t -> !allTubes.add(t))
                        .forEach(t -> messageCollection.addError("Barcode repeated in upload: " + t));

                Calendar c = Calendar.getInstance();
                c.set(Calendar.HOUR_OF_DAY, 0);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);

                Map<String, String> mapDesignIdToName = new HashMap<>();

                // Validate other fields
                for (ReagentImportDto dto: dtos) {
                    if (dto.getVolume() <= 0) {
                        messageCollection.addError("Volume can't be less than or equal to 0 for " + dto.getTubeBarcode());
                    }

                    if (dto.getMass() <= 0) {
                        messageCollection.addError("Mass can't be less than or equal to 0 for " + dto.getTubeBarcode());
                    }

                    // Libraries version of date format is lenient, so check after DTO to see if date format is garbage
                    try {
                        if (dto.getExpirationDate().before(c.getTime())) {
                            messageCollection.addError(dto.getTubeBarcode() + " is expired.");
                        }
                    } catch (ParseException ex) {
                        messageCollection.addError(ex.getMessage());
                    }

                    try {
                        dto.getSynthesisDate();
                    } catch (ParseException ex) {
                        messageCollection.addError(ex.getMessage());
                    }

                    try {
                        dto.getManufacturingDate();
                    } catch (ParseException ex) {
                        messageCollection.addError(ex.getMessage());
                    }

                    if (!mapDesignIdToName.containsKey(dto.getDesignId())) {
                        mapDesignIdToName.put(dto.getDesignId(), dto.getDesignName());
                    } else if (!mapDesignIdToName.get(dto.getDesignId()).equals(dto.getDesignName())) {
                        String err = String.format("Can't link Design ID %s it's already linked to %s in upload",
                                dto.getDesignName(), mapDesignIdToName.get(dto.getDesignId()));
                        messageCollection.addError(err);
                    }
                }

                return dtos;
            }
        } catch (Exception e) {
            log.error("Failed to parse file " + e.getCause().getMessage(), e);
            messageCollection.addError("Failed to parse input file: " + e.getCause().getMessage());
        }
        return null;
    }

    public List<String> getTubeBarcodes() {
        return tubeBarcodes;
    }

    public static class ReagentImportDto {
        public static final String DATE_FORMAT = "MM/dd/yy";

        @CsvBindByName(column = "Manufacturer Design ID", required = true)
        private String designId;

        @CsvBindByName(column = "Design Name", required = true)
        private String designName;

        @CsvBindByName(column = "2-D Barcode", required = true)
        private String tubeBarcode;

        @CsvBindByName(column = "Volume in Tube (uL)", required = true)
        private double volume;

        @CsvBindByName(column = "Probe Mass (ng)", required = true)
        private double mass;

        @CsvBindByName(column = "Synthesis Date", required = true)
        private String synthesisDateString;

        @CsvBindByName(column = "Manufacturing Date", required = true)
        private String manufacturingDateString;

        @CsvBindByName(column = "Storage Conditions", required = true)
        private String storageConditions;

        @CsvBindByName(column = "Lot Number", required = true)
        private String lotNumber;

        @CsvBindByName(column = "Expiration Date", required = true)
        private String expirationDateString;

        public ReagentImportDto() {
        }

        public String getDesignId() {
            return designId;
        }

        public void setDesignId(String designId) {
            this.designId = designId;
        }

        public String getDesignName() {
            return designName;
        }

        public void setDesignName(String designName) {
            this.designName = designName;
        }

        public String getTubeBarcode() {
            return tubeBarcode;
        }

        public void setTubeBarcode(String tubeBarcode) {
            this.tubeBarcode = tubeBarcode;
        }

        public double getVolume() {
            return volume;
        }

        public void setVolume(double volume) {
            this.volume = volume;
        }

        public double getMass() {
            return mass;
        }

        public void setMass(double mass) {
            this.mass = mass;
        }

        public Date getSynthesisDate() throws ParseException {
            return parseAsDate(synthesisDateString);
        }

        public String getSynthesisDateString() {
            return synthesisDateString;
        }

        public void setSynthesisDateString(String synthesisDateString) {
            this.synthesisDateString = synthesisDateString;
        }

        public Date getManufacturingDate() throws ParseException {
            return parseAsDate(manufacturingDateString);
        }

        public String getManufacturingDateString() {
            return manufacturingDateString;
        }

        public void setManufacturingDate(String manufacturingDateString) {
            this.manufacturingDateString = manufacturingDateString;
        }

        public String getStorageConditions() {
            return storageConditions;
        }

        public void setStorageConditions(String storageConditions) {
            this.storageConditions = storageConditions;
        }

        public String getLotNumber() {
            return lotNumber;
        }

        public void setLotNumber(String lotNumber) {
            this.lotNumber = lotNumber;
        }

        public Date getExpirationDate() throws ParseException {
            return parseAsDate(this.expirationDateString);
        }

        public String getExpirationDateString() {
            return expirationDateString;
        }

        public void setExpirationDateString(String expirationDate) {
            this.expirationDateString = expirationDate;
        }

        private Date parseAsDate(String str) throws ParseException {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT);
            simpleDateFormat.setLenient(false);
            return simpleDateFormat.parse(str);
        }
    }
}
