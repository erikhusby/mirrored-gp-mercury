package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ConcentrationAndVolumeAndWeightType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.LibraryDataType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: thompson
 * Date: 7/11/13
 * Time: 4:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class LimsQueryResourceTest2 extends Arquillian {
    @Inject
    private LimsQueryResource limsQueryResource;

    @Inject
    private SystemRouter systemRouter;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.PROD,  "prod");
    }

    @Test
    public void testDoesLimsRecognizeAllTubes() {
        boolean b = limsQueryResource.doesLimsRecognizeAllTubes(Arrays.asList(
                "1191422111",
                "1194468504",
                "1194468512",
                "1194468525",
                "1194468528",
                "1194468536",
                "1194468544",
                "1194468552",
                "1194468560",
                "1194468568",
                "1194468576",
                "1194468584",
                "1194468497",
                "1194468520",
                "1194468513",
                "1194468521",
                "1194468529",
                "1194468537",
                "1194468545",
                "1194468553",
                "1194468561",
                "1194468569",
                "1194468577",
                "1194468585",
                "1194468498",
                "1194468506",
                "1194468514",
                "1194468522",
                "1194468530",
                "1194468538",
                "1194468546",
                "1194468554",
                "1194468562",
                "1194468570",
                "1194468578",
                "1194468586",
                "1194468499",
                "1194468507",
                "1194468515",
                "1194468523",
                "1194468531",
                "1194468539",
                "1194468547",
                "1194468555",
                "1194468563",
                "1194468571",
                "1194468579",
                "1194468587",
                "1194468500",
                "1194468508",
                "1194468516",
                "1194468524",
                "1194468532",
                "1194468540",
                "1194468548",
                "1194468556",
                "1194468564",
                "1194468572",
                "1194468589",
                "1194468588",
                "1194468501",
                "1194468505",
                "1194468517",
                "1194468509",
                "1194468533",
                "1194468541",
                "1194468549",
                "1194468557",
                "1194468565",
                "1194468573",
                "1194468581",
                "1194468580",
                "1194468502",
                "1194468510",
                "1194468518",
                "1194468526",
                "1194468534",
                "1194468542",
                "1194468550",
                "1194468558",
                "1194468566",
                "1194468574",
                "1194468582",
                "1194468590",
                "1194468503",
                "1194468511",
                "1194468519",
                "1194468527",
                "1194468535",
                "1194468543",
                "1194468551",
                "1194468559",
                "1194468567",
                "1194468575",
                "1194468583",
                "1194468496"
        ));
        Assert.assertTrue(b);
    }


    @Test
    public void testX() {
        List<String> tubeBarcodes = new ArrayList<>();
        tubeBarcodes.add("0157399356");
        List<LibraryDataType> libraryDataTypes = limsQueryResource.fetchLibraryDetailsByTubeBarcode(tubeBarcodes, true);
        libraryDataTypes.get(0);
    }

    @Test
    public void testRouting() {
        List<String> barcodes = Arrays.asList(
                "1191422111",
                "1194468504",
                "1194468512",
                "1194468525",
                "1194468528",
                "1194468536",
                "1194468544",
                "1194468552",
                "1194468560",
                "1194468568",
                "1194468576",
                "1194468584",
                "1194468497",
                "1194468520",
                "1194468513",
                "1194468521",
                "1194468529",
                "1194468537",
                "1194468545",
                "1194468553",
                "1194468561",
                "1194468569",
                "1194468577",
                "1194468585",
                "1194468498",
                "1194468506",
                "1194468514",
                "1194468522",
                "1194468530",
                "1194468538",
                "1194468546",
                "1194468554",
                "1194468562",
                "1194468570",
                "1194468578",
                "1194468586",
                "1194468499",
                "1194468507",
                "1194468515",
                "1194468523",
                "1194468531",
                "1194468539",
                "1194468547",
                "1194468555",
                "1194468563",
                "1194468571",
                "1194468579",
                "1194468587",
                "1194468500",
                "1194468508",
                "1194468516",
                "1194468524",
                "1194468532",
                "1194468540",
                "1194468548",
                "1194468556",
                "1194468564",
                "1194468572",
                "1194468589",
                "1194468588",
                "1194468501",
                "1194468505",
                "1194468517",
                "1194468509",
                "1194468533",
                "1194468541",
                "1194468549",
                "1194468557",
                "1194468565",
                "1194468573",
                "1194468581",
                "1194468580",
                "1194468502",
                "1194468510",
                "1194468518",
                "1194468526",
                "1194468534",
                "1194468542",
                "1194468550",
                "1194468558",
                "1194468566",
                "1194468574",
                "1194468582",
                "1194468590",
                "1194468503",
                "1194468511",
                "1194468519",
                "1194468527",
                "1194468535",
                "1194468543",
                "1194468551",
                "1194468559",
                "1194468567",
                "1194468575",
                "1194468583",
                "1194468496");
        for (String barcode : barcodes) {
            System.out.println(barcode + " " + systemRouter.routeForVesselBarcodes(Collections.singletonList(barcode)));
        }
    }

    @Test
    public void testY() {
        SequencingTemplateType sequencingTemplateType = limsQueryResource.fetchIlluminaSeqTemplate("FCT-21651",
                SequencingTemplateFactory.QueryVesselType.FLOWCELL_TICKET, false);
        sequencingTemplateType.getBarcode();
    }

    @Test
    public void testZ() {
        List<String> tubeBarcodes = new ArrayList<>();
        tubeBarcodes.add("0243250939");
        tubeBarcodes.add("0224153926");
        tubeBarcodes.add("0224237748");
        tubeBarcodes.add("0243250900");
        tubeBarcodes.add("0243250908");
        tubeBarcodes.add("0243250916");
        tubeBarcodes.add("0243250924");
        tubeBarcodes.add("0243250932");
        tubeBarcodes.add("0243250940");
        tubeBarcodes.add("0243250948");
        tubeBarcodes.add("0243250861");
        tubeBarcodes.add("0243250869");
        tubeBarcodes.add("0243250877");
        tubeBarcodes.add("0243250885");
        tubeBarcodes.add("0243250893");
        tubeBarcodes.add("0243250901");
        tubeBarcodes.add("0243250909");
        tubeBarcodes.add("0243250917");
        tubeBarcodes.add("0243250925");
        tubeBarcodes.add("0243250933");
        tubeBarcodes.add("0243250941");
        tubeBarcodes.add("0243348381");
        tubeBarcodes.add("0243348389");
        tubeBarcodes.add("0243348397");
        tubeBarcodes.add("0243348405");
        tubeBarcodes.add("0243348413");
        tubeBarcodes.add("0243348421");
        tubeBarcodes.add("0243348429");
        tubeBarcodes.add("0243348437");
        tubeBarcodes.add("0243348445");
        tubeBarcodes.add("0243348453");
        tubeBarcodes.add("0243348461");
        tubeBarcodes.add("0243348469");
        tubeBarcodes.add("0243348382");
        tubeBarcodes.add("0243348390");
        tubeBarcodes.add("0224154672");
        tubeBarcodes.add("0224154671");
        Map<String, ConcentrationAndVolumeAndWeightType> stringConcentrationAndVolumeAndWeightTypeMap =
                limsQueryResource.fetchConcentrationAndVolumeAndWeightForTubeBarcodes(tubeBarcodes, false);
        stringConcentrationAndVolumeAndWeightTypeMap.size();
    }
}
