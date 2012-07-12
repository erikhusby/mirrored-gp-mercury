package org.broadinstitute.sequel.boundary.vessel;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.broadinstitute.sequel.nonthrift.jaxb.FlowcellDesignationType;
import org.broadinstitute.sequel.nonthrift.jaxb.LaneType;
import org.broadinstitute.sequel.nonthrift.jaxb.LibraryDataType;
import org.broadinstitute.sequel.nonthrift.jaxb.Response;
import org.broadinstitute.sequel.nonthrift.jaxb.SampleInfoType;

import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * @author breilly
 */
@Path("/nonthrift")
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
@Stateless
public class NonthriftXsdEchoResource {

    public static final String FLOWCELL_DESIGNATION_JSON =
            "{\"booleanValue\":null,\"doubleValue\":null,\"stringValue\":null,\"flowcellDesignation\":" +
                    "{\"lanes\":[" +
                    "{\"laneName\":\"1\",\"libraryData\":[" +
                    "{\"wasFound\":true,\"libraryName\":\"Library-100\",\"libraryType\":\"Test Library\",\"tubeBarcode\":\"00000100\",\"sampleDetails\":[" +
                    "{\"sampleName\":\"SM-5000\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\"}," +
                    "{\"sampleName\":\"SM-5001\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\"}]," +
                    "\"dateCreated\":\"2012-07-12T15:23:45.000+0000\",\"discarded\":false,\"destroyed\":false}," +
                    "{\"wasFound\":true,\"libraryName\":\"Library-101\",\"libraryType\":\"Test Library\",\"tubeBarcode\":\"00000101\",\"sampleDetails\":[" +
                    "{\"sampleName\":\"SM-5002\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\"}," +
                    "{\"sampleName\":\"SM-5003\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\"}]," +
                    "\"dateCreated\":\"2012-07-12T15:23:45.000+0000\",\"discarded\":false,\"destroyed\":false}]," +
                    "\"loadingConcentration\":1.2,\"derivedLibraryData\":[" +
                    "{\"wasFound\":true,\"libraryName\":\"Library-102\",\"libraryType\":\"Test Library\",\"tubeBarcode\":\"00000102\",\"sampleDetails\":[" +
                    "{\"sampleName\":\"SM-5004\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\"}," +
                    "{\"sampleName\":\"SM-5005\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\"}]," +
                    "\"dateCreated\":\"2012-07-12T15:23:45.000+0000\",\"discarded\":false,\"destroyed\":false}]}," +
                    "{\"laneName\":\"2\",\"libraryData\":[" +
                    "{\"wasFound\":true,\"libraryName\":\"Library-103\",\"libraryType\":\"Test Library\",\"tubeBarcode\":\"00000103\",\"sampleDetails\":[" +
                    "{\"sampleName\":\"SM-5006\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\"}," +
                    "{\"sampleName\":\"SM-5007\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\"}]," +
                    "\"dateCreated\":\"2012-07-12T15:23:45.000+0000\",\"discarded\":false,\"destroyed\":false}," +
                    "{\"wasFound\":true,\"libraryName\":\"Library-104\",\"libraryType\":\"Test Library\",\"tubeBarcode\":\"00000104\",\"sampleDetails\":[" +
                    "{\"sampleName\":\"SM-5008\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\"}," +
                    "{\"sampleName\":\"SM-5009\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\"}]," +
                    "\"dateCreated\":\"2012-07-12T15:23:45.000+0000\",\"discarded\":false,\"destroyed\":false}]," +
                    "\"loadingConcentration\":1.2,\"derivedLibraryData\":[" +
                    "{\"wasFound\":true,\"libraryName\":\"Library-105\",\"libraryType\":\"Test Library\",\"tubeBarcode\":\"00000105\",\"sampleDetails\":[" +
                    "{\"sampleName\":\"SM-5010\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\"}," +
                    "{\"sampleName\":\"SM-5011\",\"sampleType\":\"Test Sample\",\"indexLength\":6,\"indexSequence\":\"TACTAG\",\"referenceSequence\":\"Test Reference\"}]," +
                    "\"dateCreated\":\"2012-07-12T15:23:45.000+0000\",\"discarded\":false,\"destroyed\":false}]}]," +
                    "\"designationName\":\"Test Designation\",\"readLength\":101,\"pairedEndRun\":true,\"indexedRun\":true,\"controlLane\":2,\"keepIntensityFiles\":false}}";

    private int libraryNumber = 100;
    private int sampleNumber = 5000;

    @GET
    @Path("/echoBoolean")
    public Response echoBoolean(@QueryParam("value") boolean value) {
        Response response = new Response();
        response.setBooleanValue(value);
        return response;
    }

    @GET
    @Path("/echoDouble")
    public Response echoDouble(@QueryParam("value") double value) {
        Response response = new Response();
        response.setDoubleValue(value);
        return response;
    }

    @GET
    @Path("/echoString")
    public Response echoString(@QueryParam("value") String value) {
        Response response = new Response();
        response.setStringValue(value);
        return response;
    }

    @GET
    @Path("/getFlowcellDesignation")
    public Response getFlowcellDesignation() {
        Response response = new Response();
        FlowcellDesignationType designation = new FlowcellDesignationType();
        for (int i = 1; i <= 2; i++) {
            designation.getLanes().add(makeLane(i));
        }
        designation.setDesignationName("Test Designation");
        designation.setReadLength(101);
        designation.setPairedEndRun(true);
        designation.setIndexedRun(true);
        designation.setControlLane(2);
        designation.setKeepIntensityFiles(false);
        response.setFlowcellDesignation(designation);
        return response;
    }

    private LaneType makeLane(int laneNum) {
        LaneType lane = new LaneType();
        lane.setLaneName(Integer.toString(laneNum));
        for (int i = 0; i < 2; i++) {
            lane.getLibraryData().add(makeLibraryData());
        }
        lane.setLoadingConcentration(1.2);
        lane.getDerivedLibraryData().add(makeLibraryData());
        return lane;
    }

    private LibraryDataType makeLibraryData() {
        LibraryDataType libraryData = new LibraryDataType();
        libraryData.setWasFound(true);
        libraryData.setLibraryName("Library-" + libraryNumber);
        libraryData.setLibraryType("Test Library");
        libraryData.setTubeBarcode("00000" + libraryNumber);
        for (int i = 0; i < 2; i++) {
            libraryData.getSampleDetails().add(makeSampleInfo());
        }
        libraryData.setDateCreated(XMLGregorianCalendarImpl.createDateTime(2012, 7, 12, 11, 23, 45));
        libraryData.setDiscarded(false);
        libraryData.setDestroyed(false);
        libraryNumber++;
        return libraryData;
    }

    private SampleInfoType makeSampleInfo() {
        SampleInfoType sampleInfo = new SampleInfoType();
        sampleInfo.setSampleName("SM-" + sampleNumber);
        sampleInfo.setSampleType("Test Sample");
        sampleInfo.setIndexLength(6);
        sampleInfo.setIndexSequence("TACTAG");
        sampleInfo.setReferenceSequence("Test Reference");
        sampleNumber++;
        return sampleInfo;
    }
}
