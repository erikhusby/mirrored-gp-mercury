package org.broadinstitute.gpinformatics.mercury.control.reagent;

import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
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

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test
    public void testBasic() throws Exception {
        int numTubes = 8;
        MessageCollection messageCollection = new MessageCollection();
        List<BarcodedTube> barcodedTubeList = testReagentDesign(REAGENT_DESIGN, numTubes, messageCollection);
        Assert.assertEquals(barcodedTubeList.size(), numTubes);
        Assert.assertEquals(messageCollection.getErrors().size(), 0);
    }

    @Test
    public void testUnknownReagentDesign() throws Exception {
        int numTubes = 1;
        MessageCollection messageCollection = new MessageCollection();
        List<BarcodedTube> barcodedTubeList = testReagentDesign("IamUnknownBaitProbe", numTubes, messageCollection);
        Assert.assertEquals(barcodedTubeList.size(), 0);
        Assert.assertEquals(messageCollection.getErrors().size(), 1);
    }

    private List<BarcodedTube> testReagentDesign(String reagentDesign, int numTubes,
                                                 MessageCollection messageCollection) throws Exception {
        String timestamp = simpleDateFormat.format(new Date());
        String prefix = "Probe" + timestamp;
        File tempFile = File.createTempFile("Probe Upload Test", ".csv");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            StatefulBeanToCsv<ReagentDesignImportProcessor.ReagentImportDto> beanToCsv =
                    new StatefulBeanToCsvBuilder<ReagentDesignImportProcessor.ReagentImportDto>(writer)
                            .withSeparator(',')
                            .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                            .build();
            List<ReagentDesignImportProcessor.ReagentImportDto> dtos = new ArrayList<>();
            for (int i = 0; i < numTubes; i++) {
                String tubeBarcode = "probe" + timestamp + "tubeBarcode_" + i;
                dtos.add(createDto(tubeBarcode, reagentDesign, prefix, i));
            }
            beanToCsv.write(dtos);
        }

        try (FileInputStream fis = new FileInputStream(tempFile)) {
            return reagentDesignImportFactory.buildTubesFromSpreadsheet(fis, messageCollection);
        }
    }

    private ReagentDesignImportProcessor.ReagentImportDto createDto(String tubeBarcode, String reagentDesign,
                                                                    String prefix, int index) {
        ReagentDesignImportProcessor.ReagentImportDto dto = new ReagentDesignImportProcessor.ReagentImportDto();
        dto.setTubeBarcode(tubeBarcode);
        dto.setDesignId(reagentDesign + "_ID");
        dto.setDesignName(reagentDesign);
        dto.setExpirationDate(Date.from(Instant.now().plusSeconds(86400)));
        dto.setManufacturingDate(Date.from(Instant.now().minusSeconds(86400)));
        dto.setSynthesisDate(Date.from(Instant.now().minusSeconds(86400 * 2)));
        dto.setLotNumber(prefix + "_" + index);
        dto.setVolume(20);
        dto.setMass(40);
        dto.setStorageConditions("-20 (C)");
        return dto;
    }
}