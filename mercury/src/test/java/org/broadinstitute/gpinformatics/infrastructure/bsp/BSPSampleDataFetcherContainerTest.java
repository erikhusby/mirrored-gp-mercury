package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.BSPSampleDataFetcherImpl;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class BSPSampleDataFetcherContainerTest {
    private BSPConfig bspConfig = BSPConfig.produce(Deployment.DEV);
    private BSPSampleDataFetcher bspSampleDataFetcher =
            new BSPSampleDataFetcherImpl(BSPSampleSearchServiceProducer.testInstance(), bspConfig);
    public void testFFPE() {
        BspSampleData ffpe = bspSampleDataFetcher.fetchSingleSampleFromBSP("SM-16BL4");
        BspSampleData paraffin = bspSampleDataFetcher.fetchSingleSampleFromBSP("SM-2UVBU");
        BspSampleData notFFPE = bspSampleDataFetcher.fetchSingleSampleFromBSP("SM-3HM8");

        Assert.assertNotNull(ffpe);
        Assert.assertNotNull(paraffin);
        Assert.assertNotNull(notFFPE);
        List<BspSampleData> sampleDataList = Arrays.asList(ffpe, paraffin, notFFPE);

        bspSampleDataFetcher.fetchFFPEDerived(sampleDataList);

        Assert.assertTrue(ffpe.getFfpeStatus());
        Assert.assertTrue(paraffin.getFfpeStatus());
        Assert.assertFalse(notFFPE.getFfpeStatus());
    }

    public void testSamplePlastic() {
        BspSampleData rootSample = bspSampleDataFetcher.fetchSingleSampleFromBSP("SM-12LY");
        BspSampleData aliquotSample = bspSampleDataFetcher.fetchSingleSampleFromBSP("SM-3HM8");

        Assert.assertNotNull(rootSample);
        Assert.assertNotNull(aliquotSample);
        List<BspSampleData> sampleDataList = Arrays.asList(rootSample, aliquotSample);

        bspSampleDataFetcher.fetchSamplePlastic(sampleDataList);

        Assert.assertNotNull(rootSample.getPlasticBarcodes());
        Assert.assertNotNull(aliquotSample.getPlasticBarcodes());
    }


    public void testGetSampleDetails() {
        String barcode = "0163031423";
        List<String> barcodes =
                Arrays.asList(barcode, "0163031422", "0163031421", "0163031420", "0163031419", "0163031418",
                        "0163031417", "0163031416", "0163031415", "0163031414", "0163031413", "0163031412",
                        "0163031400", "0163031401", "0163031402", "0163031403", "0163031404", "0163031405",
                        "0163031406", "0163031407", "0163031408", "0163031409", "0163031410", "0163031411",
                        "0163031399", "0163031398", "0163031397", "0163031396", "0163031395", "0163031394",
                        "0163031393", "0163031392", "0163031391", "0163031390", "0163031389", "0163031388",
                        "0163031376", "0163031377", "0163031378", "0163031379", "0163031380", "0163031381",
                        "0163031382", "0163031383", "0163031384", "0163031385", "0163031386", "0163031387",
                        "0163031375", "0163031374", "0163031373", "0163031372", "0163031371", "0163031370",
                        "0163031369", "0163031368", "0163031367", "0163031366", "0163031365", "0163031364",
                        "0163031352", "0163031353", "0163031354", "0163031355", "0163031356", "0163031357",
                        "0163031358", "0163031359", "0163031360", "0163031361", "0163031362", "0163031363",
                        "0163031351", "0163031350", "0163031349", "0163031348", "0163031347", "0163031346",
                        "0163031345", "0163031344", "0163031343", "0163031342", "0163031341", "0163031340",
                        "0163026482", "0163031329", "0163031330", "0163031331", "0163031332", "0163031333",
                        "0163031334", "0163031335", "0163031336", "0163031337", "0163031338", "0163031339",
                        "0163031807", "0163031806", "0163031805", "0163031804", "0163031803", "0163031802",
                        "0163031801", "0163031800", "0163031799", "0163031798", "0163031797", "0163031796",
                        "0163031784", "0163031785", "0163031786", "0163031787", "0163031788", "0163031789",
                        "0163031790", "0163031791", "0163031792", "0163031793", "0163031794", "0163031795",
                        "0163031783", "0163031782", "0163031781", "0163031780", "0163031779", "0163031778",
                        "0163031777", "0163031776", "0163031775", "0163031774", "0163031773", "0163031772",
                        "0163031760", "0163031761", "0163031762", "0163031763", "0163031764", "0163031765",
                        "0163031766", "0163031767", "0163031768", "0163031769", "0163031770", "0163031771",
                        "0163031759", "0163031758", "0163031757", "0163031756", "0163031755", "0163031754",
                        "0163031753", "0163031752", "0163031751", "0163031750", "0163031749", "0163031748",
                        "0163031736", "0163031737", "0163031738", "0163031739", "0163031740", "0163031741",
                        "0163031742", "0163031743", "0163031744", "0163031745", "0163031746", "0163031747",
                        "0163031735", "0163031734", "0163031733", "0163031732", "0163031731", "0163031730",
                        "0163031729", "0163031728", "0163031727", "0163031726", "0163031725", "0163031724",
                        "0163026433", "0163031713", "0163031714", "0163031715", "0163031716", "0163031717",
                        "0163031718", "0163031719", "0163031720", "0163031721", "0163031722", "0163031723",
                        "0163031039", "0163031038", "0163031037", "0163031036", "0163031035", "0163031034",
                        "0163031033", "0163031032", "0163031031", "0163031030", "0163031029", "0163031028",
                        "0163031016", "0163031017", "0163031018", "0163031019", "0163031020", "0163031021",
                        "0163031022", "0163031023", "0163031024", "0163031025", "0163031026", "0163031027",
                        "0163031015", "0163031014", "0163031013", "0163026452", "0163031011", "0163031010",
                        "0163031009", "0163031008", "0163031007", "0163031006", "0163031005", "0163031004",
                        "0163030992", "0163030993", "0163030994", "0163030995", "0163030996", "0163030997",
                        "0163030998", "0163030999", "0163031000", "0163031001", "0163031002", "0163031003",
                        "0163030991", "0163030990", "0163030989", "0163030988", "0163030987", "0163030986",
                        "0163030985", "0163030984", "0163030983", "0163030982", "0163030981", "0163030980",
                        "0163030968", "0163030969", "0163030970", "0163030971", "0163030972", "0163030973",
                        "0163030974", "0163030975", "0163030976", "0163030977", "0163030978", "0163030979",
                        "0163030967", "0163030966", "0163030965", "0163030964", "0163030963", "0163030962",
                        "0163030961", "0163030960", "0163030959", "0163030958", "0163030957", "0163030956",
                        "0163026459", "0163030945", "0163030946", "0163030947", "0163030948", "0163030949",
                        "0163030950", "0163030951", "0163030952", "0163030953", "0163030954", "0163030955",
                        "0162972095", "0162972094", "0162972093", "0162972092", "0162972091", "0162972090",
                        "0162972089", "0162972088", "0162972087", "0162972086", "0162972085", "0162972084",
                        "0162972072", "0162972073", "0162972074", "0162972075", "0162972076", "0162972077",
                        "0162972078", "0162972079", "0162972080", "0162972081", "0162972082", "0162972083",
                        "0162972071", "0162972070", "0162972069", "0162972068", "0162972067", "0162972066",
                        "0162972065", "0162972064", "0162972063", "0162972062", "0162972061", "0162972060",
                        "0162972048", "0162972049", "0162972050", "0162972051", "0162972052", "0162972053",
                        "0162972054", "0162972055", "0162972056", "0162972057", "0162972058", "0162972059",
                        "0162972047", "0162972046", "0162972045", "0162972044", "0162972043", "0162972042",
                        "0162972041", "0162972040", "0162972039", "0162972038", "0162972037", "0162972036",
                        "0162972024", "0162972025", "0162972026", "0162972027", "0162972028", "0162972029",
                        "0162972030", "0162972031", "0162972032", "0162972033", "0162972034", "0162972035",
                        "0162972023", "0162972022", "0162972021", "0162972020", "0162972019", "0162972018",
                        "0162972017", "0162972016", "0162972015", "0162972014", "0162972013", "0162972012",
                        "0162972000", "0162972001", "0162972002", "0162972003", "0162972004", "0162972005",
                        "0162972006", "0162972007", "0162972008", "0162972009", "0162972010", "0162972011",
                        "0163047935", "0163047934", "0163047933", "0163047932", "0163047931", "0163047930",
                        "0163047929", "0163047928", "0163047927", "0163047926", "0163047925", "0163047924",
                        "0163047912", "0163047913", "0163047914", "0163047915", "0163047916", "0163047917",
                        "0163047918", "0163047919", "0163047920", "0163047921", "0163047922", "0163047923",
                        "0163047911", "0163047910", "0163047909", "0163047908", "0163047907", "0163047906",
                        "0163047905", "0163047904", "0163047903", "0163047902", "0163047901", "0163047900",
                        "0163047888", "0163047889", "0163047890", "0163047891", "0163047892", "0163047893",
                        "0163047894", "0163047895", "0163047896", "0163047897", "0163047898", "0163047899",
                        "0163047887", "0163047886", "0163047885", "0163047884", "0163047883", "0163047882",
                        "0163047881", "0163047880", "0163047879", "0163047878", "0163047877", "0163047864",
                        "0163047865", "0163047866", "0163047867", "0163047868", "0163047869", "0163047870",
                        "0163047871", "0163047872", "0163047873", "0163047874", "0163047875", "0163047863",
                        "0163047862", "0163047861", "0163047860", "0163047859", "0163047858", "0163047857",
                        "0163047856", "0163047855", "0163047854", "0163047853", "0163047852", "0163047840",
                        "0163047841", "0163047842", "0163047843", "0163047844", "0163047845", "0163047846",
                        "0163047847", "0163047848", "0163047849", "0163047850", "0163047851");
        Map<String, GetSampleDetails.SampleInfo> mapBarcodeToSampleInfo =
                bspSampleDataFetcher.fetchSampleDetailsByBarcode(barcodes);
        GetSampleDetails.SampleInfo sampleInfo = mapBarcodeToSampleInfo.get(barcode);
        Assert.assertEquals(mapBarcodeToSampleInfo.size(), barcodes.size());
        Assert.assertEquals(sampleInfo.getManufacturerBarcode(), barcode);
    }
}
