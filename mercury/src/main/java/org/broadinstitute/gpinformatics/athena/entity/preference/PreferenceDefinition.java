/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
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
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Preference definitions are responsible for converting data from the stored preference data into a real object or from
 * the object to the stored data.
 *
 * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
 */
public abstract class PreferenceDefinition {

    /**
     * Get the preference data as a string.
     *
     * @return The xml string.
     * @throws Exception Any errors.
     */
    public String getPreferenceData() throws Exception {
        return convertToXml();
    }

    /**
     * Let the subclass do its work to convert the preference data into the appropriate data.
     *
     * @param preference The preference information.
     *
     * @throws Exception Any errors
     */
    public abstract void convertPreference(Preference preference) throws Exception;

    /**
     * Utility function to convert the underlying object (from getContext) into an XML string.
     *
     * @return The xml string.
     * @throws JAXBException Any JAXB errors.
     */
    private String convertToXml() throws JAXBException {
        StringWriter sw = new StringWriter();
        Marshaller marshaller = getContext().createMarshaller();
        marshaller.marshal(this, sw);
        return sw.toString();
    }

    /**
     * The JAXB context. The subclass creates the context based on the objects it wants to stream.
     *
     * @return The context object.
     * @throws JAXBException Any JAXB errors.
     */
    protected abstract JAXBContext getContext() throws JAXBException;

    /**
     * Take the XML and use the context to convert it into a populated object.
     *
     * @param xml The xml string.
     * @return The obecct.
     *
     * @throws JAXBException Any errors processing the XML.
     */
    protected PreferenceDefinition convertFromXml(String xml) throws JAXBException {
        Unmarshaller um = getContext().createUnmarshaller();
        return (PreferenceDefinition) um.unmarshal(new StringReader(xml));
    }

    /**
     * Interface for creating preference definitions.
     */
    public interface PreferenceDefinitionCreator extends Serializable {
        public PreferenceDefinition create() throws Exception;
    }
}

