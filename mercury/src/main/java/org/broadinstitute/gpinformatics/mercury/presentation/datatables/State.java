/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2016 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.presentation.datatables;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class State implements Serializable {
    private static final long serialVersionUID = 2016092701L;

    private long time;
    private int start;
    private int length;
    private List<Map<Integer, Direction>> order = new ArrayList<>();

    private Search search = new Search();

    private List<Column> columns = new ArrayList<>();

    private List<ColReorder> colReorder;


    public State() {
    }

    public enum Direction {asc, desc}

    public State(long time, int start, int length, List<Map<Integer, Direction>> order, Search search,
                 List<Column> columns, List<ColReorder> colReorder) {
        this.time = time;
        this.start = start;
        this.length = length;
        this.order = order;
        this.search = search;
        this.columns = columns;
        this.colReorder = colReorder;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getStart() {
        return start;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public List<Map<Integer, Direction>> getOrderList() {
        return order;
    }

    public void setOrderList(List<Map<Integer, Direction>> order) {
        this.order = order;
    }

    public Search getSearch() {
        return search;
    }

    public void setSearch(Search search) {
        this.search = search;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public List<ColReorder> getColReorder() {
        return colReorder;
    }

    @JsonProperty("ColReorder")
    public void setColReorder(List<ColReorder> colReorder) {
        this.colReorder = colReorder;
    }

    @Override
     public boolean equals(Object o) {
         if (this == o) {
             return true;
         }

         if (o == null || getClass() != o.getClass()) {
             return false;
         }

         State state = (State) o;

         return new EqualsBuilder()
                 .append(getTime(), state.getTime())
                 .append(getStart(), state.getStart())
                 .append(getLength(), state.getLength())
                 .append(getOrderList(), state.getOrderList())
                 .append(getSearch(), state.getSearch())
                 .append(getColumns(), state.getColumns())
                 .append(getColReorder(), state.getColReorder())
                 .isEquals();
     }

     @Override
     public int hashCode() {
         return new HashCodeBuilder(17, 37)
                 .append(getTime())
                 .append(getStart())
                 .append(getLength())
                 .append(getOrderList())
                 .append(getSearch())
                 .append(getColumns())
                 .append(getColReorder())
                 .toHashCode();
     }

}
