package org.broadinstitute.gpinformatics.infrastructure;

import org.apache.commons.io.IOUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * This class handles the boilerplate code needed to turn an object into XML and to turn a string as XML into an object.
 */
public class ObjectMarshaller<T> {

    private JAXBContext context;

    public ObjectMarshaller(Class<T> clazz) throws JAXBException {
        context = JAXBContext.newInstance(clazz);
    }

    /**
     * Default marshal that does not format the output.
     *
     * @param object The Object that will be marshalled.
     *
     * @return The xml string representation of the object.
     */
    public String marshal(T object) {
        return marshal(object, false);
    }

    /**
     * Marshal with formatting.
     *
     * @param object The Object that will be marshalled.
     *
     * @return The xml string representation of the object.
     */
    public String marshalFormatted(T object) {
        return marshal(object, true);
    }

    /**
     * Marshal method that takes the formatting as a parameter and does the work.
     *
     * @param object The Object being marshalled.
     * @param formatOutput whether to format the output or not.
     *
     * @return The XML string.
     */
    private String marshal(T object, boolean formatOutput) {

        StringWriter writer = null;
        String result = null;
        try {
            writer = new StringWriter();

            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, formatOutput);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

            marshaller.marshal(object, writer);
            result = writer.toString();
        } catch (JAXBException ex) {
            throw new RuntimeException("Error marshaling object '" + object + "'", ex);
        } finally  {
            IOUtils.closeQuietly(writer);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public T unmarshal(String descr) {
        T object;
        Reader reader = null;

        try {
            reader = new StringReader(descr);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            object = (T) unmarshaller.unmarshal(reader);

        } catch (JAXBException ex) {
            throw new RuntimeException("Error unmarshaling an object as class ", ex);
        } finally {
            IOUtils.closeQuietly(reader);
        }

        return object;
    }
}


