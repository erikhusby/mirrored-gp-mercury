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

@JsonIgnoreProperties(ignoreUnknown = true)
public class Search {
    private String search;
    private boolean smart;
    private boolean regex;
    private boolean caseInsensitive;

    public Search() {
    }

    public Search(String search, boolean smart, boolean regex, boolean caseInsensitive) {
        this.search = search;
        this.smart = smart;
        this.regex = regex;
        this.caseInsensitive = caseInsensitive;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public boolean isSmart() {
        return smart;
    }

    public void setSmart(boolean smart) {
        this.smart = smart;
    }

    public boolean isRegex() {
        return regex;
    }

    public void setRegex(boolean regex) {
        this.regex = regex;
    }

    public boolean isCaseInsensitive() {
        return caseInsensitive;
    }

    public void setCaseInsensitive(boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Search search1 = (Search) o;

        return new EqualsBuilder()
                .append(isSmart(), search1.isSmart())
                .append(isRegex(), search1.isRegex())
                .append(isCaseInsensitive(), search1.isCaseInsensitive())
                .append(getSearch(), search1.getSearch())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getSearch())
                .append(isSmart())
                .append(isRegex())
                .append(isCaseInsensitive())
                .toHashCode();
    }
}
