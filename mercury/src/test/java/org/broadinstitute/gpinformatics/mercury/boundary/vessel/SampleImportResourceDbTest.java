package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Database test of web service to import samples from BSP
 */
public class SampleImportResourceDbTest extends ContainerTest {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMddHHmmss");

    @Test(enabled=true, groups= TestGroups.EXTERNAL_INTEGRATION, dataProvider= Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testImportTubes(@ArquillianResource URL baseUrl) {
        Date now = new Date();
        String suffix = dateFormat.format(now);

        List<ChildVesselBean> childVesselBeans = new ArrayList<ChildVesselBean>();
        childVesselBeans.add(new ChildVesselBean(suffix + "1", "SM-" + suffix + "1", "Matrix Tube Screw cap [0.5mL]", "A01"));
        childVesselBeans.add(new ChildVesselBean(suffix + "2", "SM-" + suffix + "2", "Matrix Tube Screw cap [0.5mL]", "A02"));

        List<ParentVesselBean> parentVesselBeans = new ArrayList<ParentVesselBean>();
        parentVesselBeans.add(new ParentVesselBean("CO-" + suffix, null, "2D Matrix 96 Slot Rack [0.5ml SC]", childVesselBeans));
        SampleImportBean sampleImportBeanPost = new SampleImportBean("BSP", "EX-" + suffix, now,
                parentVesselBeans, "jowalsh");

        // POST to the resource
        WebResource resource = Client.create().resource(baseUrl.toExternalForm() + "rest/sampleimport");
        String response= resource.type(MediaType.APPLICATION_XML_TYPE)
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
}
