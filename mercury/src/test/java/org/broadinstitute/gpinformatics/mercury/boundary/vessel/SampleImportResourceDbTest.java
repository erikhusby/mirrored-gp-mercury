package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUtil;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.JerseyUtils;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.ws.rs.core.MediaType;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Database test of web service to import samples from BSP
 */
@Test(groups = TestGroups.STUBBY)
@Dependent
public class SampleImportResourceDbTest extends StubbyContainerTest {

    public SampleImportResourceDbTest(){}

    private static final String MATRIX_TUBE_SCREW_CAP_0_5M_L = "Matrix Tube Screw cap [0.5mL]";
    private static final String A01 = "A01";
    private static final String A02 = "A02";
    private static final String D_MATRIX_96_SLOT_RACK_0_5ML_SC = "2D Matrix 96 Slot Rack [0.5ml SC]";
    private static final String badUserName = "scottMatthewes";
    private static final String goodUserName = "jowalsh";
    private static final String SOURCE_SYSTEM = "BSP";
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMddHHmmss");

    @Test(enabled = true, groups = TestGroups.STUBBY, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testImportTubes(@ArquillianResource URL baseUrl) throws Exception {
        Date now = new Date();
        String suffix = dateFormat.format(now);

        List<ChildVesselBean> childVesselBeans = new ArrayList<>();
        childVesselBeans
                .add(new ChildVesselBean(suffix + "1", generateSampleId(suffix, "1"), MATRIX_TUBE_SCREW_CAP_0_5M_L, A01));
        childVesselBeans
                .add(new ChildVesselBean(suffix + "2",  generateSampleId(suffix, "2"), MATRIX_TUBE_SCREW_CAP_0_5M_L, A02));

        List<ParentVesselBean> parentVesselBeans = new ArrayList<>();
        parentVesselBeans
                .add(new ParentVesselBean("CO-" + suffix, null, D_MATRIX_96_SLOT_RACK_0_5ML_SC, childVesselBeans));
        SampleImportBean sampleImportBeanPost = new SampleImportBean(SOURCE_SYSTEM,
                generateSourceSystemExportId(suffix), now,
                parentVesselBeans, goodUserName);

        ClientConfig clientConfig = JerseyUtils.getClientConfigAcceptCertificate();

        // POST to the resource
        WebResource resource = Client.create(clientConfig)
                .resource(RestServiceContainerTest.convertUrlToSecure(baseUrl) + "rest/sampleimport");
        String response = resource.type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML)
                .entity(sampleImportBeanPost)
                .post(String.class);

        // GET from the resource to verify persistence
        String batchName = response.substring(response.lastIndexOf(": ") + 2);
        SampleImportBean sampleImportBeanGet = resource.path(batchName).get(SampleImportBean.class);
        Assert.assertEquals(sampleImportBeanGet.getParentVesselBeans().iterator().next().getChildVesselBeans().size(),
                sampleImportBeanPost.getParentVesselBeans().iterator().next().getChildVesselBeans().size(),
                "Wrong number of tubes");
    }

    private String generateSourceSystemExportId(String suffix) {
        return "EX-" + suffix;
    }

    private String generateSampleId(String suffix, String increment) {
        return BSPUtil.BSP_SAMPLE_SM_PREFIX +"-" + suffix + increment;
    }

    @Test(enabled = true, groups = TestGroups.STUBBY, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testImportTubesNoUser(@ArquillianResource URL baseUrl) {
        Date now = new Date();
        String suffix = dateFormat.format(now);

        List<ChildVesselBean> childVesselBeans = new ArrayList<>();
        childVesselBeans
                .add(new ChildVesselBean(suffix + "1", generateSampleId(suffix, "1"), MATRIX_TUBE_SCREW_CAP_0_5M_L,
                        A01));
        childVesselBeans
                .add(new ChildVesselBean(suffix + "2", generateSampleId(suffix, "2"), MATRIX_TUBE_SCREW_CAP_0_5M_L,
                        A02));

        List<ParentVesselBean> parentVesselBeans = new ArrayList<>();
        parentVesselBeans
                .add(new ParentVesselBean("CO-" + suffix, null, D_MATRIX_96_SLOT_RACK_0_5ML_SC, childVesselBeans));
        SampleImportBean sampleImportBeanPost = new SampleImportBean("BSP", generateSourceSystemExportId(suffix), now,
                parentVesselBeans, "");

        // POST to the resource
        WebResource resource = Client.create().resource(baseUrl.toExternalForm() + "rest/sampleimport");
        try {
            String response = resource.type(MediaType.APPLICATION_XML_TYPE)
                    .accept(MediaType.APPLICATION_XML)
                    .entity(sampleImportBeanPost)
                    .post(String.class);
            Assert.fail();
        } catch (UniformInterfaceException e) {

        }
    }

    @Test(enabled = true, groups = TestGroups.STUBBY, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testImportTubesNoGoodUser(@ArquillianResource URL baseUrl) {
        Date now = new Date();
        String suffix = dateFormat.format(now);

        List<ChildVesselBean> childVesselBeans = new ArrayList<>();
        childVesselBeans
                .add(new ChildVesselBean(suffix + "1", generateSampleId(suffix, "1"), MATRIX_TUBE_SCREW_CAP_0_5M_L,
                        A01));
        childVesselBeans
                .add(new ChildVesselBean(suffix + "2", generateSampleId(suffix, "2"), MATRIX_TUBE_SCREW_CAP_0_5M_L,
                        A02));

        List<ParentVesselBean> parentVesselBeans = new ArrayList<>();
        parentVesselBeans
                .add(new ParentVesselBean("CO-" + suffix, null, D_MATRIX_96_SLOT_RACK_0_5ML_SC, childVesselBeans));
        SampleImportBean sampleImportBeanPost = new SampleImportBean("BSP", generateSourceSystemExportId(suffix), now,
                parentVesselBeans, badUserName);

        // POST to the resource
        WebResource resource = Client.create().resource(baseUrl.toExternalForm() + "rest/sampleimport");
        try {
            String response = resource.type(MediaType.APPLICATION_XML_TYPE)
                    .accept(MediaType.APPLICATION_XML)
                    .entity(sampleImportBeanPost)
                    .post(String.class);
            Assert.fail();
        } catch (UniformInterfaceException e) {

        }
    }
}
