package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.Namespaces;

import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.Set;

/**
 * Class to describe Athena's view of a Sample. A Sample is identified by a sample Id and
 * a billableItem and an optionally comment which may be in most cases empty but on
 * occasion can actually have a value to describe "exceptions" that occur for a particular sample.
 *
 * <p/>
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 8/28/12
 * Time: 10:26 AM
 */
@XmlType(namespace = Namespaces.ORDER_NS)
public class  AthenaSample implements Serializable {

    private String sampleId;         // This is the Id of the sample. It could be a BSP or Non-BSP sample Id but it is assume
    private String comment;
    private Set<BillableItem> billableItems;

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(final String name) {
        this.sampleId = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

    public Set<BillableItem> getBillableItems() {
        return billableItems;
    }

    public void setBillableItems(final Set<BillableItem> billableItems) {
        this.billableItems = billableItems;
    }
}
