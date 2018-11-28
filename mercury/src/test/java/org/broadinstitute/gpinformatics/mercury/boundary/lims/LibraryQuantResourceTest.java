package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.generated.LibraryBeansType;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.generated.LibraryQuantBeanType;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.generated.LibraryQuantRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.generated.MetricMetadataType;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.generated.QpcrRunBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabMetricRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@Test(groups = TestGroups.STANDARD)
public class LibraryQuantResourceTest extends Arquillian {

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private LibraryQuantResource libraryQuantResource;

    @Inject
    private LabMetricRunDao labMetricRunDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Test
    public void testCreateQpcrRunMercury() throws Exception {
        QpcrRunBean qpcrRunBean = createQpcrRunBean("0177174735");
        Response response = libraryQuantResource.createQpcrRun(qpcrRunBean);
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void testCreateQpcrRunSquid() throws Exception {
        QpcrRunBean qpcrRunBean = createQpcrRunBean("0116403448");
        Response response = libraryQuantResource.createQpcrRun(qpcrRunBean);
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void testCreatePicoRunMercury() throws Exception {
        LibraryQuantRunBean libraryQuantRun = createLibraryQuantRunBean("0177174735");
        Response response = libraryQuantResource.createLibraryQuants(libraryQuantRun);
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void testCreatePicoRunSquid() throws Exception {
        LibraryQuantRunBean libraryQuantRun = createLibraryQuantRunBean("0116403448");
        Response response = libraryQuantResource.createLibraryQuants(libraryQuantRun);
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void testCreateVVPVolumeRun() throws Exception {
        LibraryQuantRunBean libraryQuantRun = createLibraryQuantRunBean("0177175086", "VVP_Run", "VVP Volume",
                "85.4");
        LibraryQuantBeanType libraryQuantBeanType = libraryQuantRun.getLibraryQuantBeans().get(0);
        libraryQuantBeanType.getMetadata().add(createMetadata("Flowrate", "98.1"));
        Response response = libraryQuantResource.createLibraryQuants(libraryQuantRun);
        assertThat(response.getStatus(), is(200));
        LabMetricRun labMetricRun = labMetricRunDao.findByName(libraryQuantRun.getRunName());
        assertThat(labMetricRun, notNullValue());
        assertThat(labMetricRun.getLabMetrics().size(), is(1));
        LabMetric labMetric = labMetricRun.getLabMetrics().iterator().next();
        assertThat(labMetric.getValue(), equalTo(new BigDecimal("85.4")));
        Metadata metadata = labMetric.getMetadataSet().iterator().next();
        assertThat(metadata.getValue(), is("98.1"));
    }

    private static MetricMetadataType createMetadata(String name, String value) {
        MetricMetadataType metricMetadataType = new MetricMetadataType();
        metricMetadataType.setValue(value);
        metricMetadataType.setName(name);
        return metricMetadataType;
    }

    private LibraryQuantRunBean createLibraryQuantRunBean(String tubeBarcode) {
        return createLibraryQuantRunBean(tubeBarcode, "PondPico_Run", "Pond Pico", "24.5");
    }

    private LibraryQuantRunBean createLibraryQuantRunBean(String tubeBarcode, String runName, String quantType,
                                                          String value) {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        LibraryQuantRunBean runBean = new LibraryQuantRunBean();
        runBean.setQuantType(quantType);
        runBean.setRunDate(new Date());
        runBean.setRunName(timestamp + runName);
        runBean.setOperator("jowalsh");
        LibraryQuantBeanType libraryQuantBeanType = new LibraryQuantBeanType();
        libraryQuantBeanType.setRackPositionName("A01");
        libraryQuantBeanType.setTubeBarcode(tubeBarcode);
        libraryQuantBeanType.setValue(new BigDecimal(value));
        runBean.getLibraryQuantBeans().add(libraryQuantBeanType);
        return runBean;
    }

    private QpcrRunBean createQpcrRunBean(String tubeBarcode) {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        QpcrRunBean qpcrRunBean = new QpcrRunBean();
        qpcrRunBean.setRunDate(new Date());
        qpcrRunBean.setRunName(timestamp + "VIIA_7_Run");
        qpcrRunBean.setOperator("jowalsh");

        LibraryBeansType lib = new LibraryBeansType();
        lib.setConcentration(new BigDecimal("12.2"));
        lib.setTubeBarcode(tubeBarcode);
        lib.setPass(true);
        lib.setWell("A01");
        qpcrRunBean.getLibraryBeans().add(lib);
        return qpcrRunBean;
    }
}
