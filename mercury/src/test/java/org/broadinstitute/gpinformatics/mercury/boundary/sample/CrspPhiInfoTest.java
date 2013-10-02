package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.StringReader;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.CrspPhiDTO;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.CrspPhiInfo;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class CrspPhiInfoTest {

    @Test(groups = TestGroups.DATABASE_FREE, enabled = false)
    public void testMarshalling() throws Exception {

        String rawSource = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                           "<crspPhiInfo>\n" +
                           "<phiData>\n" +
                           "<sampleID>CSM-4OVVR</sampleID>\n" +
                           "<caseId>CRSP-1</caseId>\n" +
                           "<patientFirstName>John</patientFirstName>\n" +
                           "<patientLastName>Doe</patientLastName>\n" +
                           "<patientDob>01/01/1970</patientDob>\n" +
                           "<patientGender>Male</patientGender>\n" +
                           "<clinicianName>Yan</clinicianName>\n" +
                           "<clinicianEmail>a@b.org</clinicianEmail>\n" +
                           "<clinicianInstitution>MGH</clinicianInstitution>\n" +
                           "<stateOfPatientResidence>MA</stateOfPatientResidence>\n" +
                           "<receiptDate>2013-09-27T15:13:11-04:00</receiptDate>\n" +
                           "<clinicianPatientId>P2345</clinicianPatientId>\n" +
                           "<clinicianSampleId>S1234</clinicianSampleId>\n" +
                           "</phiData>\n" +
                           "<phiData>\n" +
                           "<sampleID>CSM-4OVVR</sampleID>\n" +
                           "<caseId>CRSP-1</caseId>\n" +
                           "<patientFirstName>John</patientFirstName>\n" +
                           "<patientLastName>Doe</patientLastName>\n" +
                           "<patientDob>01/01/1970</patientDob>\n" +
                           "<patientGender>Male</patientGender>\n" +
                           "<clinicianName>Yan</clinicianName>\n" +
                           "<clinicianEmail>a@b.org</clinicianEmail>\n" +
                           "<clinicianInstitution>MGH</clinicianInstitution>\n" +
                           "<stateOfPatientResidence>MA</stateOfPatientResidence>\n" +
                           "<receiptDate>2013-09-27T15:13:11-04:00</receiptDate>\n" +
                           "<clinicianPatientId>P2345</clinicianPatientId>\n" +
                           "<clinicianSampleId>S1234</clinicianSampleId>\n" +
                           "</phiData>\n" +
                           "</crspPhiInfo>\n";

        JAXBContext queryContext = JAXBContext.newInstance(CrspPhiInfo.class, CrspPhiDTO.class);
        Unmarshaller unmarshaller = queryContext.createUnmarshaller();

        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = sf.newSchema(new File("/Users/scottmat/Development/mercury/mercury/src/main/webapp/xsd/CrspTypes.xsd"));
        unmarshaller.setSchema(schema);
        unmarshaller.setEventHandler(new ValidationEventHandler() {
            @Override
            public boolean handleEvent(ValidationEvent event) {
                throw new RuntimeException("XSD Validation failure: " +
                                           "SEVERITY: " + event.getSeverity() + "MESSAGE: " + event.getMessage() +
                                           "LINKED EXCEPTION:  " + event.getLinkedException() +
                                           "LINE NUMBER:  " + event.getLocator().getLineNumber() +
                                           "COLUMN NUMBER:  " + event.getLocator().getColumnNumber() +
                                           "OFFSET:  " + event.getLocator().getOffset() +
                                           "OBJECT:  " + event.getLocator().getObject() +
                                           "NODE:  " + event.getLocator().getNode() +
                                           "URL:  " + event.getLocator().getURL());
            }
        });


        XMLReader reader = XMLReaderFactory.createXMLReader();

        //Prepare the input
        InputSource is = new InputSource(new StringReader(rawSource));

        //Create a SAXSource specifying the filter
        SAXSource source = new SAXSource(is);

        CrspPhiInfo queryInfo = (CrspPhiInfo)unmarshaller.unmarshal(source);

        Assert.assertNotNull(queryInfo);
        Assert.assertFalse(queryInfo.getPhiData().isEmpty());
        Assert.assertEquals("CRSP-1",queryInfo.getPhiData().iterator().next().getCaseId());
        Assert.assertEquals("CSM-4OVVR",queryInfo.getPhiData().iterator().next().getSampleID());

    }
}
