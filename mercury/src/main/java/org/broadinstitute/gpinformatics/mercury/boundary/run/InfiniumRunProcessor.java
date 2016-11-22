package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.InfiniumStarterConfig;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Scans run folder for the finished idat pairs for each sample well in a chip
 */
public class InfiniumRunProcessor {

    private static final Log log = LogFactory.getLog(InfiniumRunProcessor.class);

    @Inject
    private InfiniumStarterConfig infiniumStarterConfig;

    public InfiniumRunProcessor() {
    }

    //For testing purposes
    public InfiniumRunProcessor(InfiniumStarterConfig infiniumStarterConfig) {
        this.infiniumStarterConfig = infiniumStarterConfig;
    }

    public ChipWellResults process(StaticPlate chip) {
        ChipWellResults chipWellResults = new ChipWellResults();
        Map<VesselPosition, Boolean> wellCompleteMap = new HashMap<>();
        String chipBarcode = chip.getLabel();
        File runDirectory = getRunDirectory(chipBarcode);
        boolean hasRunStarted = runDirectory.exists();
        boolean isChipCompleted = true;
        String scannerName = null;
        if (runDirectory.exists()) {
            List<String> idatFiles = listIdatFiles(runDirectory);
            for (VesselPosition vesselPosition: chip.getVesselGeometry().getVesselPositions()) {
                Set<SampleInstanceV2> sampleInstancesAtPositionV2 =
                        chip.getContainerRole().getSampleInstancesAtPositionV2(vesselPosition);
                if (sampleInstancesAtPositionV2 != null && !sampleInstancesAtPositionV2.isEmpty()) {
                    chipWellResults.getPositionWithSampleInstance().add(vesselPosition);
                    String red = String.format("%s_%s_Red.idat", chipBarcode, vesselPosition.name());
                    String green = String.format("%s_%s_Grn.idat", chipBarcode, vesselPosition.name());
                    boolean complete = idatFiles.contains(red) && idatFiles.contains(green);
                    wellCompleteMap.put(vesselPosition, complete);
                    if (!complete) {
                        isChipCompleted = false;
                    }
                    if (scannerName == null) {
                        scannerName = findScannerName(chipBarcode, vesselPosition.name());
                    }
                }
            }
        }

        chipWellResults.setScannerName(scannerName);
        chipWellResults.setHasRunStarted(hasRunStarted);
        chipWellResults.setCompleted(isChipCompleted);
        chipWellResults.setWellCompleteMap(wellCompleteMap);
        return chipWellResults;
    }

    private List<String> listIdatFiles(File runDirectory) {
        File[] files = runDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String name) {
                return name.toLowerCase().endsWith(".idat");
            }
        });

        List<String> filenames = new ArrayList<>();
        for (File f: files) {
            if (f.length() > infiniumStarterConfig.getMinimumIdatFileLength()) {
                filenames.add(f.getName());
            }
        }

        return filenames;
    }

    private File getRunDirectory(String chipBarcode) {
        File rootDir = new File(infiniumStarterConfig.getDataPath());
        return new File(rootDir, chipBarcode);
    }

    private String findScannerName(String chipBarcode, String vesselPosition) {
        try {
            if (infiniumStarterConfig != null) {
                String redXml = String.format("%s_%s_1_Red.xml", chipBarcode, vesselPosition);
                File chipDir = new File(infiniumStarterConfig.getDataPath(), chipBarcode);
                File redXmlFile = new File(chipDir, redXml);
                if (redXmlFile.exists()) {
                    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
                    Document document = documentBuilder.parse(new FileInputStream(redXmlFile));
                    XPath xPath = XPathFactory.newInstance().newXPath();
                    XPathExpression lcsetKeyExpr = xPath.compile("ImageHeader/ScannerID");
                    NodeList scannerIdNodeList = (NodeList) lcsetKeyExpr.evaluate(document,
                            XPathConstants.NODESET);
                    Node elemNode = scannerIdNodeList.item(0);
                    String scannerId = elemNode.getFirstChild().getNodeValue();
                    String scannerName = InfiniumRunResource.mapSerialNumberToMachineName.get(scannerId);
                    if (scannerName == null) {
                        scannerName = "Unknown";
                    }
                    return scannerName;
                }
            }
        } catch (Exception e) {
            log.error("Failed to find scanner name from filesystem for " + chipBarcode);
        }
        return null;
    }

    public class ChipWellResults {
        private boolean completed;
        private Map<VesselPosition, Boolean> wellCompleteMap;
        private List<VesselPosition> positionWithSampleInstance;
        private boolean hasRunStarted;
        private String scannerName;

        public Map<VesselPosition, Boolean> getWellCompleteMap() {
            return wellCompleteMap;
        }

        public void setWellCompleteMap(Map<VesselPosition, Boolean> wellCompleteMap) {
            this.wellCompleteMap = wellCompleteMap;
        }

        public List<VesselPosition> getPositionWithSampleInstance() {
            if (positionWithSampleInstance == null)
                positionWithSampleInstance = new ArrayList<>();
            return positionWithSampleInstance;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }

        public void setHasRunStarted(boolean hasRunStarted) {
            this.hasRunStarted = hasRunStarted;
        }

        public boolean isHasRunStarted() {
            return hasRunStarted;
        }

        public String getScannerName() {
            return scannerName;
        }

        public void setScannerName(String scannerName) {
            this.scannerName = scannerName;
        }
    }

}
