package org.broadinstitute.gpinformatics.infrastructure.jpa;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a collection of entities that span a specified range within the result set of a database query
 */
public class Page<PAGE_ENTITY> implements Serializable {

    private static final long serialVersionUID = -2676745938634077703L;

    List<PAGE_ENTITY> pageEntities = new ArrayList<>();

    int pageNumber;
    int firstIndex;
    int numberOfItems;

    public Page(List<PAGE_ENTITY> pageEntities, int pageNumber, int firstIndex, int numberOfItems) {
        this.pageEntities = pageEntities;
        this.pageNumber = pageNumber;
        this.firstIndex = firstIndex;
        this.numberOfItems = numberOfItems;
    }

    public List<PAGE_ENTITY> getPageEntities() {
        return pageEntities;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getFirstIndex() {
        return firstIndex;
    }

    public int getNumberOfItems() {
        return numberOfItems;
    }

}
