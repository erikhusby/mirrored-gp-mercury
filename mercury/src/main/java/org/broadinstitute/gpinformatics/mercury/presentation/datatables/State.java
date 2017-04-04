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
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class State implements Serializable {
    private static final long serialVersionUID = 2016092701L;

    private Long time = 0l;
    private Integer start = 0;
    private Integer length = 0;

    private List<Column> columns = new ArrayList<>();

    public State() {
    }

    public State(Long time, Integer start, Integer length, List<Column> columns) {
        this.time = time;
        this.start = start;
        this.length = length;
        this.columns = columns;
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

    public List<Column> getColumns() {
        return columns;
    }

    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }

    public void setStart(Integer start) {
        this.start = start;
    }


    @Override
    public String toString() {
        return new JSONObject(this).toString();
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
                 .append(getColumns(), state.getColumns())
                 .isEquals();
     }

     @Override
     public int hashCode() {
         return new HashCodeBuilder(17, 37)
                 .append(getTime())
                 .append(getStart())
                 .append(getLength())
                 .append(getColumns())
                 .toHashCode();
     }
}
