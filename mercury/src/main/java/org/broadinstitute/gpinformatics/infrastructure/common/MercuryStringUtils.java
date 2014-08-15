/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.common;

import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONMarshaller;
import com.sun.jersey.api.json.JSONUnmarshaller;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionRequestBean;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.UUID;
import java.util.zip.CRC32;

public class MercuryStringUtils {
    /**
     * Splits camel case input string into words for example productOrder would return "product order"
     *
     * @see <a href="http://stackoverflow.com/questions/2559759/how-do-i-convert-camelcase-into-human-readable-names-in-java">How do I convert CamelCase into human-readable names in Java?</a>
     */
    public static String splitCamelCase(String inputString) {
        return inputString.replaceAll(
                String.format("%s|%s|%s",
                        "(?<=[A-Z])(?=[A-Z][a-z])",
                        "(?<=[^A-Z])(?=[A-Z])",
                        "(?<=[A-Za-z])(?=[^A-Za-z])"), " "
        );
    }

    public static StringWriter serializeJsonBean(Object requestedBean) throws JAXBException {
        JSONJAXBContext context =
                new JSONJAXBContext(JSONConfiguration.natural().humanReadableFormatting(true).build(),
                        requestedBean.getClass());
        JSONMarshaller marshaller = context.createJSONMarshaller();

        StringWriter writer = new StringWriter();

        marshaller.marshallToJSON(requestedBean, writer);
        return writer;
    }

    public static <T> T deSerializeJsonBean(String testJson1,
                                            Class<T> submissionRequestBeanClass) throws JAXBException {
        JSONJAXBContext context =
                new JSONJAXBContext(JSONConfiguration.natural().humanReadableFormatting(true).build(),
                        submissionRequestBeanClass);

        JSONUnmarshaller unmarshaller = context.createJSONUnmarshaller();

        InputStream input = new ByteArrayInputStream(testJson1.getBytes());

        return unmarshaller.unmarshalFromJSON(input, submissionRequestBeanClass);
    }

    public String generateGUID(String value, String aVersion) {

        CRC32 cRC32Generator = new CRC32();
        cRC32Generator.update(value.getBytes());
        Long tempApplicationChecksum = cRC32Generator.getValue();
        cRC32Generator.update(aVersion.getBytes());
        Long tempVersionChecksum = cRC32Generator.getValue();
        cRC32Generator.reset();

        Long tempMSBits = tempApplicationChecksum * tempVersionChecksum;
        Long tempLSBits = tempVersionChecksum * tempVersionChecksum;

        StringBuilder tempUUID =
                new StringBuilder(new UUID(tempMSBits, tempLSBits).toString());
        tempUUID.replace(14, 15, "4");
        tempUUID.replace(19, 20,
                convertSecondReservedCharacter(tempUUID.substring(19, 20)));

        return tempUUID.toString();
    }

    private String convertSecondReservedCharacter(String aString) {
        switch (aString.charAt(0) % 4) {
        case 0: return "8";
        case 1: return "9";
        case 2: return "a";
        case 3: return "b";
        default: return aString;
        }
    }
}
