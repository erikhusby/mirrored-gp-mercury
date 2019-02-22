package org.broadinstitute.gpinformatics.mercury.control.vessel;

import com.opencsv.CSVReader;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.MappingStrategy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.broadinstitute.bsp.client.util.MessageCollection;

import java.beans.IntrospectionException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Processes plate information from a Fluidigm csv.
 */
public class FluidigmChipProcessor {

    private static final Log logger = LogFactory.getLog(FluidigmChipProcessor.class);

    private MessageCollection messageCollection;

    private static final String[] COLUMNS = new String[]{
            "id",
            "assayName",
            "alleleX",
            "alleleY",
            "sampleName",
            "type",
            "autoGenotype",
            "confidence",
            "finalGenotype",
            "converted",
            "alleleXIntensity",
            "alleleYIntensity"
    };

    public FluidigmChipProcessor() {
        this.messageCollection = new MessageCollection();
    }

    public FluidigmRun parse(InputStream inputStream) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            FluidigmRun fluidigmRun = processHeaderRows(reader);
            List<FluidigmDataRow> records = processDataRows(reader, fluidigmRun);
            fluidigmRun.setRecords(records);
            processDoseMeterRows(reader, fluidigmRun);
            processRawData(reader, fluidigmRun);
            processBackgroundData(reader, fluidigmRun);
            return fluidigmRun;
        } catch (ParseException e) {
            logger.error("Failed to parse run date.", e);
            messageCollection.addError("Failed to parse run date.");
        } catch (IOException ex) {
            logger.error("Failed to parse fluidigm file.", ex);
            messageCollection.addError("Failed to parse fluidigm file.");
        } catch (Exception e) {
            logger.error("Error parsing fluidigm file.", e);
            messageCollection.addError("Failed to parse fluidigm file.");
        }

        return null;
    }

    private List<FluidigmDataRow> processDataRows(BufferedReader reader, FluidigmRun fluidigmRun) throws Exception {
        PublicProcessLineCsvToBean<FluidigmDataRow> csvToBean = new PublicProcessLineCsvToBean<>();
        ColumnPositionMappingStrategy<FluidigmDataRow> strat = new ColumnPositionMappingStrategy<>();
        strat.setType(FluidigmDataRow.class);
        strat.setColumnMapping(COLUMNS);
        CSVReader csvReader = new CSVReader(reader);
        List<FluidigmDataRow> list = new ArrayList<>();

        // Skip Headers
        String[] rowData = null;
        while (true) {
            rowData = csvReader.readNext();
            if (rowData[0] != null && rowData[0].equals("ID")) {
                break;
            }
        }

        while (true) {
            rowData = csvReader.readNext();
            if (rowData[0].equals("Dose Meter Reading Data")) {
                break;
            }
            FluidigmDataRow fluidigmDataRow = csvToBean.processLine(strat, rowData);
            if (fluidigmDataRow.getId() == null || fluidigmDataRow.getId().isEmpty()) {
                continue;
            }
            list.add(fluidigmDataRow);
            fluidigmRun.getAssayNamesByAssayKey().put(getAssayKeyFromSampleArray(
                    fluidigmDataRow.getId()), fluidigmDataRow.getAssayName());
            fluidigmRun.getSampleNameBySampleKey().put(getSampleKeyFromSampleArray(
                    fluidigmDataRow.getId()), fluidigmDataRow.getSampleName());
        }
        return list;
    }

    private void processDoseMeterRows(BufferedReader reader, FluidigmRun fluidigmRun) throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new RuntimeException("Data file ended unexpectedly");
        }
        if (!line.startsWith("Cycle Number")) {
            throw new RuntimeException("Unrecognized line: " + line + " In 'Does Meter Reading Data' section");
        }

        fluidigmRun.setControlDyeCount(readIntegerFromFile(reader, fluidigmRun.getControlDye(), "Dose Meter Reading Data"));
        fluidigmRun.setDyeXCount(readIntegerFromFile(reader, fluidigmRun.getDyeX(), "Dose Meter Reading Data"));
        fluidigmRun.setDyeYCount(readIntegerFromFile(reader, fluidigmRun.getDyeY(), "Dose Meter Reading Data"));
    }

    private void processRawData(BufferedReader reader, FluidigmRun fluidigmRun) throws IOException {
        Map<String, DescriptiveStatistics> mapAssayToStatistics = new HashMap<>();
        Map<String, DescriptiveStatistics> mapSampleToStatistics = new HashMap<>();
        processRoxData(reader, fluidigmRun, "Raw Data for Passive Reference ROX", mapAssayToStatistics,
                mapSampleToStatistics);
        fluidigmRun.setMapAssayToRawStatistics(mapAssayToStatistics);
        fluidigmRun.setMapSampleToRawStatistics(mapSampleToStatistics);
    }

    private void processBackgroundData(BufferedReader reader, FluidigmRun fluidigmRun) throws IOException {
        Map<String, DescriptiveStatistics> mapAssayToStatistics = new HashMap<>();
        Map<String, DescriptiveStatistics> mapSampleToStatistics = new HashMap<>();
        processRoxData(reader, fluidigmRun, "Bkgd Data for Passive Reference ROX", mapAssayToStatistics,
                mapSampleToStatistics);
        fluidigmRun.setMapAssayToBackgroundStatistics(mapAssayToStatistics);
        fluidigmRun.setMapSampleToBackgroundStatistics(mapSampleToStatistics);
    }

    private void processRoxData(BufferedReader reader, FluidigmRun fluidigmRun, String sectionName,
                                Map<String, DescriptiveStatistics> mapAssayToStatistics,
                                Map<String, DescriptiveStatistics> mapSampleToStatistics) throws IOException {
        // Skip Headers
        while (true) {
            String line = reader.readLine();
            if (line != null && line.startsWith(sectionName)) {
                break;
            }
        }

        String line = reader.readLine();
        if (!line.startsWith("Chamber ID")) {
            throw new RuntimeException("Unrecognized line: " + line);
        }

        int expectedNumberOfLines = fluidigmRun.getDataPoints();
        int lineCount = 0;
        while (lineCount++ < expectedNumberOfLines) {
            line = reader.readLine();
            if (line == null) {
                throw new RuntimeException("Unexpected end of data in section '");
            }
            String[] values = line.split(",");
            Integer value = validateLine(sectionName, line, values);

            // The first column has the S[Position]-[arrayindex], pull the position as well number.
            String sampleArray = values[0];
            fluidigmRun.getChamberIdToRox().put(sampleArray, value);

            String sampleKeyFromSampleName = getSampleKeyFromSampleArray(sampleArray);
            String assayKeyFromSampleArray = getAssayKeyFromSampleArray(sampleArray);
            handleRoxStatisticsByKey(sampleKeyFromSampleName, mapSampleToStatistics, value);
            handleRoxStatisticsByKey(assayKeyFromSampleArray, mapAssayToStatistics, value);
        }
    }

    private void handleRoxStatisticsByKey(String key, Map<String, DescriptiveStatistics> mapKeyToStats, Integer value) {
        DescriptiveStatistics descriptiveStatistics = mapKeyToStats.get(key);
        if (descriptiveStatistics == null) {
            descriptiveStatistics = new DescriptiveStatistics();
            mapKeyToStats.put(key, descriptiveStatistics);
        }
        descriptiveStatistics.addValue(value);
    }

    private Integer validateLine(String sectionName, String line, String[] values) {
        if (values.length != 2) {
            throw new RuntimeException("Unrecognized line: '" + line + "' in section '" + sectionName + "'");
        }
        Integer value;
        try {
            value = Integer.parseInt(values[1]);
        } catch (NumberFormatException ne) {
            throw new RuntimeException("Invalid Integer in line: '" + line + "' in section '" + sectionName + "'");
        }
        return value;
    }

    private Integer readIntegerFromFile(BufferedReader reader, String tag, String sectionName) throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new RuntimeException("Data file ended unexpectedly");
        }
        String[] values = line.split(",");
        if (values.length != 2) {
            throw new RuntimeException("Unrecognized line: '" + line + "' in '" + sectionName + "' section");
        }
        Integer value = null;
        if (values[0].equals(tag)) {
            try {
                value = Integer.parseInt(values[1]);
            } catch (NumberFormatException ne) {
                throw new RuntimeException("Invalid Integer in line: '" + line + "' in '" + sectionName + "' section");
            }
        }
        return value;
    }

    private String getSampleKeyFromSampleArray(String sampleName) {
        return sampleName.split("-")[0];
    }

    private String getAssayKeyFromSampleArray(String sampleName) {
        return sampleName.split("-")[1];
    }

    private FluidigmRun processHeaderRows(BufferedReader reader) throws IOException, ParseException {
        FluidigmRun fluidigmRun = new FluidigmRun();
        String line = reader.readLine();
        String[] columns = line.split(",");
        fluidigmRun.setChipBarcode(columns[2]);
        fluidigmRun.setControlDye(columns[5]);
        if (!fluidigmRun.getControlDye().equals("ROX")) {
            throw new RuntimeException("Control dye needs to be set to ROX");
        }
        fluidigmRun.setRunDateFromString(columns[7]);
        fluidigmRun.setInstrumentName(columns[9]);
        //read version line
        line = reader.readLine();
        fluidigmRun.setApplicationVersion(line.split(",")[1]);
        //read build line
        line = reader.readLine();
        fluidigmRun.setApplicationBuild(line.split(",")[1]);
        //read export type line
        line = reader.readLine();
        columns = line.split(",");
        fluidigmRun.setExportTypeOne(columns[1]);
        fluidigmRun.setExportTypeTwo(columns[2]);
        //read data points line
        line = reader.readLine();
        columns = line.split(",");
        fluidigmRun.setChipRuns(new Integer(columns[1]));
        fluidigmRun.setDataPoints(new Integer(columns[2]));
        //read confidence threshold line
        line = reader.readLine();
        fluidigmRun.setConfidenceThreshold(new Float(line.split(",")[1]));
        //read norm line
        line = reader.readLine();
        fluidigmRun.setNormMethod(line.split(",")[1]);
        //read dye lines
        line = reader.readLine();
        fluidigmRun.setDyeX(line.split(",")[2]);
        line = reader.readLine();
        fluidigmRun.setDyeY(line.split(",")[2]);
        //read allele axis lines
        line = reader.readLine();
        fluidigmRun.setAxisX(line.split(",")[2]);
        line = reader.readLine();
        fluidigmRun.setAxisY(line.split(",")[2]);

        fluidigmRun.setSectionChipBarcode(null);
        fluidigmRun.setExpPacket(null);
        return fluidigmRun;
    }

    public MessageCollection getMessageCollection() {
        return messageCollection;
    }

    public class FluidigmRun implements Serializable {
        private Date runDate;
        private String chipBarcode;
        private String instrumentName;
        private List<FluidigmDataRow> records;
        private Integer controlDyeCount;
        private Integer dyeXCount;
        private Integer dyeYCount;
        private int dataPoints;
        private Map<String, String> assayNamesByAssayKey;
        private Map<String, String> sampleNameBySampleKey;
        private Map<Long, Integer> mapWellNumToRoxSum;
        private Map<Long, Double> mapWellNumToRoxMean;
        private Map<Long, Double> mapWellNumToRoxStandardDeviation;
        private Map<Long, Double> mapWellNumToRoxMedian;
        private String applicationVersion;
        private String applicationBuild;
        private String exportTypeOne;
        private String exportTypeTwo;
        private Integer chipRuns;
        private Float confidenceThreshold;
        private String normMethod;
        private String dyeX;
        private String dyeY;
        private String axisX;
        private String axisY;
        private Object sectionChipBarcode;
        private Object expPacket;
        private Map<String, Integer> chamberIdToRox;
        private String controlDye;
        private Map<String, DescriptiveStatistics> mapAssayToRawStatistics;
        private Map<String, DescriptiveStatistics> mapSampleToRawStatistics;
        private Map<String, DescriptiveStatistics> mapAssayToBackgroundStatistics;
        private Map<String, DescriptiveStatistics> mapSampleToBackgroundStatistics;

        public FluidigmRun() {
            assayNamesByAssayKey = new HashMap<>(96);
            sampleNameBySampleKey = new HashMap<>(96);
            mapWellNumToRoxSum = new HashMap<>(96);
            mapWellNumToRoxMean = new HashMap<>(96);
            mapWellNumToRoxStandardDeviation = new HashMap<>(96);
            mapWellNumToRoxMedian = new HashMap<>(96);
            chamberIdToRox = new HashMap<>(96);
        }

        public Date getRunDate() {
            return runDate;
        }

        public void setRunDate(Date runDate) {
            this.runDate = runDate;
        }

        public String getChipBarcode() {
            return chipBarcode;
        }

        public void setChipBarcode(String chipBarcode) {
            this.chipBarcode = chipBarcode;
        }

        public String getInstrumentName() {
            return instrumentName;
        }

        public void setInstrumentName(String instrumentName) {
            this.instrumentName = instrumentName;
        }

        public void setRunDateFromString(String dateString) throws ParseException {
            setRunDate(new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").parse(dateString));
        }

        public void setRecords(List<FluidigmDataRow> records) {
            this.records = records;
        }

        public List<FluidigmDataRow> getRecords() {
            return records;
        }

        public void setControlDyeCount(Integer controlDyeCount) {
            this.controlDyeCount = controlDyeCount;
        }

        public Integer getControlDyeCount() {
            return controlDyeCount;
        }

        public void setDyeXCount(Integer dyeXCount) {
            this.dyeXCount = dyeXCount;
        }

        public Integer getDyeXCount() {
            return dyeXCount;
        }

        public void setDyeYCount(Integer dyeYCount) {
            this.dyeYCount = dyeYCount;
        }

        public Integer getDyeYCount() {
            return dyeYCount;
        }

        public int getDataPoints() {
            return dataPoints;
        }

        public void setDataPoints(int dataPoints) {
            this.dataPoints = dataPoints;
        }

        public void setAssayNamesByAssayKey(Map<String,String> assayNamesByAssayKey) {
            this.assayNamesByAssayKey = assayNamesByAssayKey;
        }

        public Map<String, String> getAssayNamesByAssayKey() {
            return assayNamesByAssayKey;
        }

        public void setSampleNameBySampleKey(Map<String,String> sampleNameBySampleKey) {
            this.sampleNameBySampleKey = sampleNameBySampleKey;
        }

        public Map<String, String> getSampleNameBySampleKey() {
            return sampleNameBySampleKey;
        }

        public Map<Long, Integer> getMapWellNumToRoxSum() {
            return mapWellNumToRoxSum;
        }

        public Map<Long, Double> getMapWellNumToRoxMean() {
            return mapWellNumToRoxMean;
        }

        public Map<Long, Double> getMapWellNumToRoxStandardDeviation() {
            return mapWellNumToRoxStandardDeviation;
        }

        public Map<Long, Double> getMapWellNumToRoxMedian() {
            return mapWellNumToRoxMedian;
        }

        public void setApplicationVersion(String applicationVersion) {
            this.applicationVersion = applicationVersion;
        }

        public String getApplicationVersion() {
            return applicationVersion;
        }

        public void setApplicationBuild(String applicationBuild) {
            this.applicationBuild = applicationBuild;
        }

        public String getApplicationBuild() {
            return applicationBuild;
        }

        public void setExportTypeOne(String exportTypeOne) {
            this.exportTypeOne = exportTypeOne;
        }

        public String getExportTypeOne() {
            return exportTypeOne;
        }

        public void setExportTypeTwo(String exportTypeTwo) {
            this.exportTypeTwo = exportTypeTwo;
        }

        public String getExportTypeTwo() {
            return exportTypeTwo;
        }

        public void setChipRuns(Integer chipRuns) {
            this.chipRuns = chipRuns;
        }

        public Integer getChipRuns() {
            return chipRuns;
        }

        public void setConfidenceThreshold(Float confidenceThreshold) {
            this.confidenceThreshold = confidenceThreshold;
        }

        public Float getConfidenceThreshold() {
            return confidenceThreshold;
        }

        public void setNormMethod(String normMethod) {
            this.normMethod = normMethod;
        }

        public String getNormMethod() {
            return normMethod;
        }

        public void setDyeX(String dyeX) {
            this.dyeX = dyeX;
        }

        public String getDyeX() {
            return dyeX;
        }

        public void setDyeY(String dyeY) {
            this.dyeY = dyeY;
        }

        public String getDyeY() {
            return dyeY;
        }

        public void setAxisX(String axisX) {
            this.axisX = axisX;
        }

        public String getAxisX() {
            return axisX;
        }

        public void setAxisY(String axisY) {
            this.axisY = axisY;
        }

        public String getAxisY() {
            return axisY;
        }

        public void setSectionChipBarcode(Object sectionChipBarcode) {
            this.sectionChipBarcode = sectionChipBarcode;
        }

        public Object getSectionChipBarcode() {
            return sectionChipBarcode;
        }

        public void setExpPacket(Object expPacket) {
            this.expPacket = expPacket;
        }

        public Object getExpPacket() {
            return expPacket;
        }

        public Map<String, Integer> getChamberIdToRox() {
            return chamberIdToRox;
        }

        public void setChamberIdToRox(Map<String, Integer> chamberIdToRox) {
            this.chamberIdToRox = chamberIdToRox;
        }

        public void setControlDye(String controlDye) {
            this.controlDye = controlDye;
        }

        public String getControlDye() {
            return controlDye;
        }

        public String buildRunName() {
            return chipBarcode + "_" + new SimpleDateFormat("MM-dd-yyyy HH:mm:ss").format(getRunDate());
        }

        public Set<String> getPolyAssays() {
            Set<String> polyAssays = new HashSet<>();
            for (FluidigmDataRow row: getRecords()) {
                polyAssays.add(row.getAssayName());
            }
            return polyAssays;
        }

        public void setMapAssayToRawStatistics(Map<String,DescriptiveStatistics> mapAssayToRawStatistics) {
            this.mapAssayToRawStatistics = mapAssayToRawStatistics;
        }

        public Map<String, DescriptiveStatistics> getMapAssayToRawStatistics() {
            return mapAssayToRawStatistics;
        }

        public void setMapSampleToRawStatistics(Map<String,DescriptiveStatistics> mapSampleToRawStatistics) {
            this.mapSampleToRawStatistics = mapSampleToRawStatistics;
        }

        public Map<String, DescriptiveStatistics> getMapSampleToRawStatistics() {
            return mapSampleToRawStatistics;
        }

        public void setMapAssayToBackgroundStatistics(
                Map<String,DescriptiveStatistics> mapAssayToBackgroundStatistics) {
            this.mapAssayToBackgroundStatistics = mapAssayToBackgroundStatistics;
        }

        public Map<String, DescriptiveStatistics> getMapAssayToBackgroundStatistics() {
            return mapAssayToBackgroundStatistics;
        }

        public void setMapSampleToBackgroundStatistics(
                Map<String,DescriptiveStatistics> mapSampleToBackgroundStatistics) {
            this.mapSampleToBackgroundStatistics = mapSampleToBackgroundStatistics;
        }

        public Map<String, DescriptiveStatistics> getMapSampleToBackgroundStatistics() {
            return mapSampleToBackgroundStatistics;
        }
    }

    public static class FluidigmDataRow {
        private String id;                  // ID - 'S96-A01'
        private String chipName;            // Chip Name - '1381629205' (optional - only used if combined chip run)
        private String chipBarcode;         // Chip Barcode - '1381629205' (optional - only used if combined chip run)
        private String assayName;           // Assay - 'rs10037858'
        private String alleleX;             // Allele X - 'A'
        private String alleleY;             // Allele Y - 'G'
        private String sampleName;          // Name - 'SM-3NQ9Q'
        private String type;                // Type - 'Unknown'
        private String autoGenotype;        // Auto - 'XY'
        private String confidence;          // Confidence - '99.24'
        private String finalGenotype;       // Final - 'XY'
        private String converted;           // Converted - 'A:G'
        private String alleleXIntensity;    // Allele X - '0.802101458875742'
        private String alleleYIntensity;    // Allele Y - '0.976721029574121'

        public FluidigmDataRow() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getChipName() {
            return chipName;
        }

        public void setChipName(String chipName) {
            this.chipName = chipName;
        }

        public String getChipBarcode() {
            return chipBarcode;
        }

        public void setChipBarcode(String chipBarcode) {
            this.chipBarcode = chipBarcode;
        }

        public String getAssayName() {
            return assayName;
        }

        public void setAssayName(String assayName) {
            this.assayName = assayName;
        }

        public String getAlleleX() {
            return alleleX;
        }

        public void setAlleleX(String alleleX) {
            this.alleleX = alleleX;
        }

        public String getAlleleY() {
            return alleleY;
        }

        public void setAlleleY(String alleleY) {
            this.alleleY = alleleY;
        }

        public String getSampleName() {
            return sampleName;
        }

        public void setSampleName(String sampleName) {
            this.sampleName = sampleName;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getAutoGenotype() {
            return autoGenotype;
        }

        public void setAutoGenotype(String autoGenotype) {
            this.autoGenotype = autoGenotype;
        }

        public String getConfidence() {
            return confidence;
        }

        public void setConfidence(String confidence) {
            this.confidence = confidence;
        }

        public String getFinalGenotype() {
            return finalGenotype;
        }

        public void setFinalGenotype(String finalGenotype) {
            this.finalGenotype = finalGenotype;
        }

        public String getConverted() {
            return converted;
        }

        public void setConverted(String converted) {
            this.converted = converted;
        }

        public String getAlleleXIntensity() {
            return alleleXIntensity;
        }

        public void setAlleleXIntensity(String alleleXIntensity) {
            this.alleleXIntensity = alleleXIntensity;
        }

        public String getAlleleYIntensity() {
            return alleleYIntensity;
        }

        public void setAlleleYIntensity(String alleleYIntensity) {
            this.alleleYIntensity = alleleYIntensity;
        }
    }


    /**
     * Wrapper to make the protected processLine method public in order to better control opencsv reader
     * @param <T> object to be created from line.
     */
    private static class PublicProcessLineCsvToBean<T> extends CsvToBean<T> {

        @Override
        public T processLine(MappingStrategy<T> mapper, String[] line) throws IllegalAccessException,
                InvocationTargetException, InstantiationException, IntrospectionException {
            return super.processLine(mapper, line);
        }
    }
}
