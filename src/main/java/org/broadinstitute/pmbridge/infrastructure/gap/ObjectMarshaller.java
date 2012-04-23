package org.broadinstitute.pmbridge.infrastructure.gap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;


public class ObjectMarshaller {

    private static Log logger = LogFactory.getLog(ObjectMarshaller.class);

    // Suitable for short xml Strings
    public static String marshall(Object object) {
        return marshall(object, false);
    }

    // Suitable for short xml Strings
    public static String marshall(Object object, boolean formatOutput) {

        StringWriter writer = null;
        String result = null;
        try {
            writer = new StringWriter();

            JAXBContext jc = JAXBContext.newInstance(object.getClass());

            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, formatOutput);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

            marshaller.marshal(object, writer);
            result = writer.toString();

        } catch (JAXBException ex) {
            throw new RuntimeException("Error marshaling object '" + object + "'", ex);
        } finally  {
            if ((writer != null )) {
                writer.flush();
                try {
                    writer.close();
                } catch (IOException e) { }
            }
        }

        return result;
    }

    public static <T> T unmarshall(Class<T> clazz, String descr) {

        T object;

        try {

            object = unmarshall(clazz, new StringReader(descr));

        } catch (Exception ex) {

            throw new RuntimeException("Error unmarshaling '" + descr + "' as class " + clazz.getName(), ex);
        }

        return object;
    }

//
//    public static <T> T unmarshall(Class<T> clazz, File file) {
//
//        T object;
//
//        try {
//
//            object = unmarshall(clazz, new FileReader(file));
//
//        } catch (Exception ex) {
//
//            throw new RuntimeException("Error unmarshaling an object from the file " + file.getAbsolutePath() + " as class " + clazz.getName(), ex);
//        }
//
//        return object;
//    }
//
    public static <T> T unmarshall(Class<T> clazz, Reader reader) {

        T object;

        try {

            JAXBContext jc = JAXBContext.newInstance(clazz);

            Unmarshaller unmarshaller = jc.createUnmarshaller();
            object = (T) unmarshaller.unmarshal(reader);

        } catch (JAXBException ex) {

            throw new RuntimeException("Error unmarshaling an object as class " + clazz.getName(), ex);

        } finally {

            try {

                if (reader != null)
                    reader.close();

            } catch (IOException ex) {
                logger.error(ex);
            }
        }

        return object;
    }
}

