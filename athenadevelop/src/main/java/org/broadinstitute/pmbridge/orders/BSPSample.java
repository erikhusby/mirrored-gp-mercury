package org.broadinstitute.pmbridge.orders;

import org.broadinstitute.pmbridge.Namespaces;

import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

/**
 * Class contains the identifier of a BSP Sample and an associated
 * sample specific comment.  This text may be in most cases null but on
 * occasion can actually have a value for exceptions that occur in the lims system.
 * <p/>
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 8/28/12
 * Time: 10:26 AM
 */
@XmlType(namespace = Namespaces.ORDER_NS)
public class BSPSample implements Serializable {

    private String name;
    private String comment;
    //TODO Should remaining SAMPLE fields be added in order to send the

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

}
