package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.Namespaces;

import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.List;

/**
 * Class to model the concept of a Order that be be created in PMBridge
 * by the Program PM and subsequently submitted to a lims system.
 * Currently supports the concept of more than one OrderItem per Order but initially it
 * is assumed that this list may only contain one item.
 * For more detail on the purpose of the Order, see the user stories listed on
 *
 * @see <a href="	https://confluence.broadinstitute.org/x/kwPGAg</a>
 *      <p/>
 *      Created by IntelliJ IDEA.
 *      User: mccrory
 *      Date: 8/28/12
 *      Time: 10:25 AM
 */
@XmlType(namespace = Namespaces.ORDER_NS, propOrder = {"name", "barcode", "orderItems", "comments"})
public class Order implements Serializable {

    private String name;       // Unique name/title for the order
    private String barcode;    // Unique barcode for the order. Eg. PDO-ABDR
    private List<OrderItem> orderItems;  // list of items contained in the order. Should just be a list on 1 for now.
    private String comments;  // Additional comments of the order


    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(final String barcode) {
        this.barcode = barcode;
    }

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(final List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(final String comments) {
        this.comments = comments;
    }
}
