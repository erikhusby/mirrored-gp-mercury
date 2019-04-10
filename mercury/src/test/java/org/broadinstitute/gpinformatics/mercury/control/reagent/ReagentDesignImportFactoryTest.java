package org.broadinstitute.gpinformatics.mercury.control.reagent;

import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Test persistence of probes uploaded via CSV.
 */
@Test(groups = TestGroups.STANDARD)
public class ReagentDesignImportFactoryTest extends Arquillian {

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");

    private String REAGENT_DESIGN = "whole_exome_illumina_coding_v1";

    @Inject
    private ReagentDesignImportFactory reagentDesignImportFactory;

    @Inject
    private BarcodedTubeDao barcodedTubeDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test
    public void testBasic() throws Exception {
        int numTubes = 8;
        MessageCollection messageCollection = new MessageCollection();
        List<BarcodedTube> barcodedTubeList = testReagentDesign(REAGENT_DESIGN, numTubes, messageCollection, true);
        Assert.assertEquals(barcodedTubeList.size(), numTubes);
        Assert.assertEquals(messageCollection.getErrors().size(), 0);
    }

    @Test
    public void testUnknownReagentDesign() throws Exception {
        int numTubes = 1;
        MessageCollection messageCollection = new MessageCollection();
        List<BarcodedTube> barcodedTubeList = testReagentDesign("IamUnknownBaitProbe", numTubes, messageCollection, false);
        Assert.assertEquals(barcodedTubeList.size(), 0);
        Assert.assertEquals(messageCollection.getErrors().size(), 1);
    }

    @Test
    public void testMultipleDesignsToSingleID() throws Exception {
        MessageCollection messageCollection = new MessageCollection();
        String timestamp = simpleDateFormat.format(new Date());
        String prefix = "ProbeFail" + timestamp;

        String tubeA = "TubeATest" + prefix;
        String tubeB = "TubeBFails" + prefix;

        String uniqueDesignId = "NoChanceLabUsesThisId" + timestamp;

        // Upload Once to get data, then attempt with another design name to this id to see if it fails correctly
        ReagentDesignImportProcessor.ReagentImportDto dto = createDto(tubeA, "Pancan_396_NO_INTRONS",
                prefix, 0);
        dto.setDesignId(uniqueDesignId);
        testReagentDesign(Collections.singletonList(dto), messageCollection, true);
        Assert.assertEquals(messageCollection.getErrors().size(), 0);

        ReagentDesignImportProcessor.ReagentImportDto dto2 = createDto(tubeB, "broad_custom_exome_v1",
                prefix, 1);
        dto2.setDesignId(uniqueDesignId);
        testReagentDesign(Collections.singletonList(dto2), messageCollection, false);
        Assert.assertEquals(messageCollection.getErrors().size(), 1);
        String expErr = String.format(
                "Can't link Design ID %s to broad_custom_exome_v1 it's already linked to Pancan_396_NO_INTRONS.",
                uniqueDesignId);
        Assert.assertEquals(messageCollection.getErrors().get(0),
                expErr);
    }

    @Test
    public void testDuplicateBarcodeInMercury() throws Exception {
        MessageCollection messageCollection = new MessageCollection();
        String timestamp = simpleDateFormat.format(new Date());
        String prefix = "ProbeFail" + timestamp;
        ReagentDesignImportProcessor.ReagentImportDto dto = createDto("0330395066", "Pancan_396_NO_INTRONS",
                prefix, 0);
        testReagentDesign(Collections.singletonList(dto), messageCollection, false);
        Assert.assertEquals(messageCollection.getErrors().size(), 1);
        Assert.assertEquals(messageCollection.getErrors().get(0), "Barcode \"0330395066\" already exists.");
    }

    private List<BarcodedTube> testReagentDesign(List<ReagentDesignImportProcessor.ReagentImportDto> dtos,
                                                 MessageCollection messageCollection, boolean persist) throws Exception {
        File tempFile = File.createTempFile("Probe Upload Test", ".csv");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            StatefulBeanToCsv<ReagentDesignImportProcessor.ReagentImportDto> beanToCsv =
                    new StatefulBeanToCsvBuilder<ReagentDesignImportProcessor.ReagentImportDto>(writer)
                            .withSeparator(',')
                            .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                            .build();
            beanToCsv.write(dtos);
        }
        try (FileInputStream fis = new FileInputStream(tempFile)) {
            List<BarcodedTube> barcodedTubes =
                    reagentDesignImportFactory.buildTubesFromSpreadsheet(fis, messageCollection);
            if (persist && !messageCollection.hasErrors()) {
                barcodedTubeDao.persistAll(barcodedTubes);
            }
            return barcodedTubes;
        }
    }

    private List<BarcodedTube> testReagentDesign(String reagentDesign, int numTubes,  MessageCollection messageCollection,
                                                 boolean persist) throws Exception {
        String timestamp = simpleDateFormat.format(new Date());
        String prefix = "Probe" + timestamp;
        List<ReagentDesignImportProcessor.ReagentImportDto> dtos = new ArrayList<>();
        for (int i = 0; i < numTubes; i++) {
            String tubeBarcode = "probe" + timestamp + "tubeBarcode_" + i;
            dtos.add(createDto(tubeBarcode, reagentDesign, prefix, i));
        }
        return testReagentDesign(dtos, messageCollection, persist);
    }

    private ReagentDesignImportProcessor.ReagentImportDto createDto(String tubeBarcode, String reagentDesign,
                                                                    String prefix, int index) {
        SimpleDateFormat sdf = new SimpleDateFormat(
                ReagentDesignImportProcessor.ReagentImportDto.DATE_FORMAT);
        String date = sdf.format(Date.from(Instant.now().plusSeconds(86400)));
        ReagentDesignImportProcessor.ReagentImportDto dto = new ReagentDesignImportProcessor.ReagentImportDto();
        dto.setTubeBarcode(tubeBarcode);
        dto.setDesignId(reagentDesign + "_ID");
        dto.setDesignName(reagentDesign);
        dto.setExpirationDateString(date);
        dto.setManufacturingDate(date);
        dto.setSynthesisDateString(date);
        dto.setLotNumber(prefix + "_" + index);
        dto.setVolume(20);
        dto.setMass(40);
        dto.setStorageConditions("-20 (C)");
        return dto;
    }
}