/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2009 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. Neither
 * the Broad Institute nor MIT can be responsible for its use, misuse, or functionality.
 */
package org.broadinstitute.gpinformatics.athena.entity.preference;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Preference definitions are responsible for converting data from the stored preference data into a real object or from
 * the object to the stored data.
 *
 * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
 */
public abstract class PreferenceDefinition {

    public String getPreferenceData() throws Exception {
        return convertToXml();
    }

    public abstract void convertPreference(Preference preference) throws JAXBException;

    public String convertToXml() throws JAXBException {
        StringWriter sw = new StringWriter();
        Marshaller marshaller = getContext().createMarshaller();
        marshaller.marshal(this, sw);
        return sw.toString();
    }

    protected abstract JAXBContext getContext() throws JAXBException;

    public Object convertFromXml(String xml) throws JAXBException {
        Unmarshaller um = getContext().createUnmarshaller();
        return um.unmarshal(new StringReader(xml));
    }

    public interface PreferenceDefinitionCreator {
        public PreferenceDefinition create();
    }
}

